"""Repack glTF embedded images while preserving mesh, armature, animation and materials.

No geometry or skeleton data are altered. Requires Pillow.
Example: python scripts/optimize_glb_textures.py model.glb 2048
"""
import io
import json
import pathlib
import struct
import sys
from PIL import Image


def chunks(path):
    blob = path.read_bytes()
    magic, version, size = struct.unpack_from("<4sII", blob, 0)
    assert magic == b"glTF" and version == 2 and size == len(blob)
    offset = 12
    gltf, data = None, None
    while offset < len(blob):
        length, kind = struct.unpack_from("<I4s", blob, offset)
        offset += 8
        content = blob[offset:offset+length]
        if kind == b"JSON":
            gltf = json.loads(content)
        elif kind == b"BIN\x00":
            data = content
        offset += length
    assert gltf and data is not None
    assert len(gltf.get("buffers", [])) == 1
    return gltf, data


def optimize(path, limit):
    doc, binary = chunks(path)
    updated = {}
    images = doc.get("images", [])
    for image in images:
        if "bufferView" not in image:
            continue
        index = image["bufferView"]
        view = doc["bufferViews"][index]
        base = view.get("byteOffset", 0)
        original = binary[base:base + view["byteLength"]]
        pic = Image.open(io.BytesIO(original))
        width, height = pic.size
        name = image.get("name", "").lower()
        target = 1024 if "metallic" in name or "roughness" in name else limit
        ratio = min(1, target/max(width, height))
        if ratio < 1:
            pic = pic.resize((round(width*ratio), round(height*ratio)), Image.Resampling.LANCZOS)
        if image["mimeType"] == "image/jpeg" or "texture_0" == name or "base" in name:
            if pic.mode not in ("RGB", "L"):
                pic = pic.convert("RGB")
            fmt, mime, kwargs = "JPEG", "image/jpeg", {"quality": 92, "optimize": True, "subsampling": 0}
        else:
            fmt, mime, kwargs = "PNG", "image/png", {"optimize": True, "compress_level": 8}
        result_io = io.BytesIO()
        pic.save(result_io, fmt, **kwargs)
        result = result_io.getvalue()
        updated[index] = result
        image["mimeType"] = mime
        print("IMAGE", image.get("name"), f"{width}x{height}",
              f"->{pic.width}x{pic.height}", len(original), "->", len(result), flush=True)
    new_binary = bytearray()
    for i, view in enumerate(doc["bufferViews"]):
        old_start = view.get("byteOffset", 0)
        payload = updated.get(i)
        if payload is None:
            payload = binary[old_start:old_start + view["byteLength"]]
        new_binary.extend(b"\x00" * (-len(new_binary) % 4))
        view["byteOffset"] = len(new_binary)
        view["byteLength"] = len(payload)
        new_binary.extend(payload)
    doc["buffers"][0]["byteLength"] = len(new_binary)
    new_binary.extend(b"\x00" * (-len(new_binary) % 4))
    json_bytes = json.dumps(doc, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    json_bytes += b" " * (-len(json_bytes) % 4)
    header_length = 12 + 8 + len(json_bytes) + 8 + len(new_binary)
    target_path = path.with_suffix(".optimized.glb")
    with open(target_path, "wb") as out:
        out.write(struct.pack("<4sII", b"glTF", 2, header_length))
        out.write(struct.pack("<I4s", len(json_bytes), b"JSON"))
        out.write(json_bytes)
        out.write(struct.pack("<I4s", len(new_binary), b"BIN\x00"))
        out.write(new_binary)
    new_doc, _ = chunks(target_path)
    assert len(new_doc.get("skins", [])) == len(doc.get("skins", []))
    assert [a.get("name") for a in new_doc.get("animations", [])] == [a.get("name") for a in doc.get("animations", [])]
    assert len(new_doc.get("meshes", [])) == len(doc.get("meshes", []))
    print("COMPACT", path.name, path.stat().st_size, "->", target_path.stat().st_size)
    return target_path


if __name__ == "__main__":
    path = pathlib.Path(sys.argv[1])
    resized = optimize(path, int(sys.argv[2]) if len(sys.argv) > 2 else 2048)
    resized.replace(path)
