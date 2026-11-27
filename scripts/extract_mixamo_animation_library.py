"""Extract a glTF 2.0 animation library with a minimal skinned marker mesh.

Preserves source skeleton, skin matrices, all animation channels, and keyframe data.
Removes the original character mesh, textures and materials to prevent duplicates.
Usage: python extract_mixamo_animation_library.py input.glb output.glb
"""
import copy
import json
import struct
import sys
from pathlib import Path


def read_glb(path):
    blob = Path(path).read_bytes()
    magic, version, declared_size = struct.unpack_from('<4sII', blob)
    if magic != b'glTF' or version != 2 or declared_size != len(blob):
        raise ValueError('Expected valid glTF 2.0 .glb input')
    offset = 12
    json_data = None
    binary_data = b''
    while offset < len(blob):
        chunk_length, chunk_type = struct.unpack_from('<I4s', blob, offset)
        offset += 8
        chunk = blob[offset:offset + chunk_length]
        offset += chunk_length
        if chunk_type == b'JSON':
            json_data = json.loads(chunk)
        elif chunk_type == b'BIN\x00':
            binary_data = chunk
    if json_data is None or not binary_data:
        raise ValueError('Expected JSON and embedded BIN glTF chunks')
    return json_data, binary_data


def extract(source_path, target_path):
    src, blob = read_glb(source_path)
    if len(src.get('skins', [])) != 1 or not src.get('animations'):
        raise ValueError('Expected one humanoid skin and one or more animation clips')
    if len(src.get('meshes', [])) != 1:
        raise ValueError('This extractor expects one source skinned mesh')
    used_accessors = {src['skins'][0]['inverseBindMatrices']}
    for animation in src['animations']:
        for sampler in animation['samplers']:
            used_accessors.update((sampler['input'], sampler['output']))
    output = {key: copy.deepcopy(src[key]) for key in
              ('asset', 'scene', 'scenes', 'nodes', 'skins', 'animations')}
    output['asset']['generator'] = 'Forests of Hell shared humanoid animation extractor'
    output['buffers'] = [{'byteLength': 0}]
    output['bufferViews'] = []
    output['accessors'] = []
    output['meshes'] = [{'name': 'MinimalRigMarker', 'primitives': []}]
    new_bin = bytearray()
    accessor_map = {}
    view_map = {}

    def align4():
        new_bin.extend(b'\x00' * (-len(new_bin) % 4))

    def copy_accessor(old_idx):
        if old_idx in accessor_map:
            return accessor_map[old_idx]
        accessor = copy.deepcopy(src['accessors'][old_idx])
        if 'sparse' in accessor:
            raise ValueError('Sparse accessors require explicit support')
        old_view = accessor['bufferView']
        if old_view not in view_map:
            view = copy.deepcopy(src['bufferViews'][old_view])
            if view.get('buffer', 0) != 0:
                raise ValueError('Expected single embedded buffer')
            start = view.get('byteOffset', 0)
            stop = start + view['byteLength']
            align4()
            view['byteOffset'] = len(new_bin)
            view['buffer'] = 0
            new_bin.extend(blob[start:stop])
            view_map[old_view] = len(output['bufferViews'])
            output['bufferViews'].append(view)
        accessor['bufferView'] = view_map[old_view]
        accessor_map[old_idx] = len(output['accessors'])
        output['accessors'].append(accessor)
        return accessor_map[old_idx]

    for i in sorted(used_accessors):
        copy_accessor(i)
    output['skins'][0]['inverseBindMatrices'] = accessor_map[src['skins'][0]['inverseBindMatrices']]
    for animation in output['animations']:
        for sampler in animation['samplers']:
            sampler['input'] = accessor_map[sampler['input']]
            sampler['output'] = accessor_map[sampler['output']]

    def new_accessor(packed, typ, component, count, target=None, minimum=None, maximum=None):
        align4()
        view = {'buffer': 0, 'byteOffset': len(new_bin), 'byteLength': len(packed)}
        if target:
            view['target'] = target
        view_idx = len(output['bufferViews'])
        output['bufferViews'].append(view)
        new_bin.extend(packed)
        accessor = {'bufferView': view_idx, 'componentType': component,
                    'count': count, 'type': typ}
        if minimum is not None:
            accessor['min'] = minimum
        if maximum is not None:
            accessor['max'] = maximum
        idx = len(output['accessors'])
        output['accessors'].append(accessor)
        return idx

    # Keep tiny skinned geometry so Godot imports the bone hierarchy as Skeleton3D.
    tiny_positions = [(-0.0005, 0, 0), (0.0005, 0, 0), (0, 0.0005, 0)]
    positions = new_accessor(b''.join(struct.pack('<3f', *v) for v in tiny_positions),
                             'VEC3', 5126, 3, 34962,
                             [-0.0005, 0, 0], [0.0005, 0.0005, 0])
    joints = new_accessor(struct.pack('<12H', *([0, 0, 0, 0] * 3)), 'VEC4',
                          5123, 3, 34962)
    weights = new_accessor(struct.pack('<12f', *([1, 0, 0, 0] * 3)), 'VEC4',
                           5126, 3, 34962)
    indices = new_accessor(struct.pack('<3H', 0, 1, 2), 'SCALAR', 5123, 3, 34963,
                           [0], [2])
    output['meshes'][0]['primitives'] = [
        {'attributes': {'POSITION': positions, 'JOINTS_0': joints, 'WEIGHTS_0': weights},
         'indices': indices, 'mode': 4}]
    output['buffers'][0]['byteLength'] = len(new_bin)
    align4()
    json_bin = json.dumps(output, ensure_ascii=False, separators=(',', ':')).encode('utf-8')
    json_bin += b' ' * (-len(json_bin) % 4)
    overall_size = 12 + 8 + len(json_bin) + 8 + len(new_bin)
    output_blob = struct.pack('<4sII', b'glTF', 2, overall_size)
    output_blob += struct.pack('<I4s', len(json_bin), b'JSON') + json_bin
    output_blob += struct.pack('<I4s', len(new_bin), b'BIN\x00') + new_bin
    Path(target_path).parent.mkdir(parents=True, exist_ok=True)
    Path(target_path).write_bytes(output_blob)
    verification, _ = read_glb(target_path)
    if [a['name'] for a in verification['animations']] != [a['name'] for a in src['animations']]:
        raise AssertionError('Animation names changed during extraction')
    print('SOURCE_BYTES', Path(source_path).stat().st_size)
    print('LIBRARY_BYTES', len(output_blob))
    print('CLIPS', len(output['animations']))
    print('CHANNELS', sum(len(a['channels']) for a in output['animations']))
    print('JOINTS', len(output['skins'][0]['joints']))
    print('NAMES', ', '.join(a['name'] for a in output['animations']))


if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise SystemExit('Usage: extract_mixamo_animation_library.py input.glb output.glb')
    extract(sys.argv[1], sys.argv[2])
