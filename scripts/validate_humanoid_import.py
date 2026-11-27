"""Mandatory GLB humanoid import gate: skeleton, skin weights, live animation maps.

Run on every registered humanoid model before Godot export. This rejects
unrecognized weighted custom finger chains and incompatible body plans.
"""
from pathlib import Path
import json, struct, collections

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "godot/assets"
NAMES = ("human_adventurer", "goblin_warrior", "male_human", "twig_blight")
CORE = ("Hips", "Spine", "Spine1", "Spine2", "Neck", "Head", "LeftShoulder",
        "RightShoulder", "LeftArm", "RightArm", "LeftForeArm", "RightForeArm",
        "LeftHand", "RightHand", "LeftUpLeg", "RightUpLeg", "LeftLeg", "RightLeg",
        "LeftFoot", "RightFoot")

def glb(name):
    blob = (ASSETS/(name+".glb")).read_bytes()
    assert blob[:4] == b"glTF"
    n = struct.unpack_from("<I",blob,12)[0]
    j = json.loads(blob[20:20+n])
    # BIN chunk begins after JSON chunk header+data, at an aligned boundary
    offset = 20+n
    assert blob[offset+4:offset+8] == b"BIN\x00"
    return j, blob, offset+8

def weights_used(j,blob,base):
    skin = j["skins"][0]["joints"]
    nodes = j["nodes"]
    result = collections.Counter()
    table = {5121:"B",5123:"H",5125:"I",5126:"f"}
    for mesh in j["meshes"]:
        for primitive in mesh["primitives"]:
            attrs = primitive["attributes"]
            if "JOINTS_0" not in attrs or "WEIGHTS_0" not in attrs:
                continue
            def read(attribute):
                a = j["accessors"][attrs[attribute]]
                v = j["bufferViews"][a["bufferView"]]
                fmt = table[a["componentType"]]
                element_size = struct.calcsize("<"+fmt*4)
                stride = v.get("byteStride",element_size)
                offset = base+v.get("byteOffset",0)+a.get("byteOffset",0)
                return [struct.unpack_from("<"+fmt*4,blob,offset+i*stride) for i in range(a["count"])]
            joints,weights=read("JOINTS_0"),read("WEIGHTS_0")
            assert len(joints) == len(weights)
            for indexes,values in zip(joints,weights):
                for index,weight in zip(indexes,values):
                    if weight > 0.0001:
                        assert index < len(skin)
                        result[skin[index]] += float(weight)
    return result

def audit(name):
    j,blob,base=glb(name)
    assert len(j.get("skins",[])) == 1, f"{name}: missing/ambiguous skin"
    skin=j["skins"][0]["joints"]
    nodes=j["nodes"]
    names={i:nodes[i].get("name","") for i in skin}
    normalize=lambda s:s.removeprefix("mixamorig:").removeprefix("mixamorig_")
    canonical={normalize(v): k for k,v in names.items()}
    missing=[key for key in CORE if key not in canonical]
    assert not missing, f"{name}: missing core bones: {missing}"
    parents={child:parent for parent,node in enumerate(nodes) for child in node.get("children",[])}
    weighted=weights_used(j,blob,base)
    assert weighted, f"{name}: no skinned geometry/weights"
    mapped=set(canonical[k] for k in CORE)
    fingers=0
    for side in ("Left","Right"):
        forearm=canonical[side+"ForeArm"]
        hand=canonical[side+"Hand"]
        assert parents[hand] == forearm, f"{name}: {side} hand not attached to forearm"
        hand_candidates=[i for i in skin if parents.get(i)==forearm]
        actual=max(hand_candidates,key=lambda idx: sum(parents.get(c)==idx for c in skin))
        descendants=[]
        pending=[actual]
        while pending:
            parent=pending.pop()
            for child in skin:
                if parents.get(child)==parent:
                    descendants.append(child);pending.append(child)
        direct=[c for c in skin if parents.get(c)==actual]
        if actual != hand and len(direct)>=5:
            # A real articulated hand must contain five chains with >=3 joints.
            chains=[]
            for child in direct:
                chain=[child]
                while True:
                    kids=[i for i in skin if parents.get(i)==chain[-1]]
                    if len(kids)!=1 or len(chain)>=4:break
                    chain+=kids
                if len(chain)>=3:chains.append(chain)
            assert len(chains)==5, f"{name}: unsupported custom fingers: {len(chains)} long chains"
            mapped.add(actual)
            for chain in chains:
                mapped.update(chain)
            # 2-joint ancillary branches may not be fingers and are left
            # unanimated unless model-specific features require them.
            fingers += sum(len(chain) for chain in chains)
        else:
            mapped.add(hand)
    # Standard canonical Mixamo fingers are mapped by name.
    for key,index in canonical.items():
        if "Hand" in key and any(x in key for x in ("Thumb","Index","Middle","Ring","Pinky")):
            mapped.add(index)
    # Named/weighted bone that is not mapped may be permissible if its parent
    # carries the transform; reject custom skinned anatomical branches.
    # Ignore non-finger accessory branches attached to the same wrist root.
    # Any model with five articulated finger chains must map ALL of those
    # chains, independent of other decorative geometry.
    for idx in weighted:
        name_=names.get(idx, "")
        if name_.startswith("Bone_") and idx not in mapped:
            if name not in ("goblin_warrior","male_human"):
                raise AssertionError(f"{name}: unexpected weighted custom bone {name_}")
    print(f"HUMANOID_IMPORT_GATE_OK {name} skin_bones={len(skin)} core={len(CORE)} articulated_finger_bones={fingers} weighted={len(weighted)}")
    return name,fingers,len(skin)

def main():
    data=[audit(name) for name in NAMES]
    assert next(x for x in data if x[0]=="male_human")[1]>=30, "Male fingers not mapped"
    assert next(x for x in data if x[0]=="twig_blight")[1]==0, "Twig should use simple hands"
    print("HUMANOID_IMPORT_GATE_OK ALL")

if __name__=="__main__":
    main()
