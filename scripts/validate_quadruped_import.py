"""Production import gate for the first 46-bone quadruped reference.

Never accept a candidate by bone count alone: validate hierarchy, skin,
weighted mesh, all original tracks, canonical names, and original bytes.
Future quadruped sources must be audited against their own explicit contract.
"""
import hashlib
import json
from pathlib import Path
import struct
import sys

ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT / "godot/assets/wolf_quadruped.glb"
MANIFEST = ROOT / "docs/quadruped-wolf-reference.json"
SOURCE_SHA256 = "137525d12db941491ba5b7b254ffc0920ce3adbaff15f9d2a2a6dc4e873481c4"
ALIASES = {
    "dog_idle1": "Idle_Standing_01_Standard",
    "dog_run": "Movement_Run_01_Standard",
    "dog_dog_over": "Action_RollOver_01_Standard",
    "leopard_skill_feipu": "Attack_Pounce_01_Standard",
}
REQUIRED = {
    "root", "hips", "chest", "head", "jaw",
    "frontUpr.L", "frontLwr.L", "frontFoot.L", "frontToe.L",
    "frontUpr.R", "frontLwr.R", "frontFoot.R", "frontToe.R",
    "rearUpr.L", "rearLwr.L", "rearFoot.L", "rearToe.L",
    "rearUpr.R", "rearLwr.R", "rearFoot.R", "rearToe.R",
}

def inspect():
    blob = ASSET.read_bytes()
    assert hashlib.sha256(blob).hexdigest() == SOURCE_SHA256, "Wolf original bytes were changed"
    magic, version, length = struct.unpack_from("<4sII", blob)
    assert magic == b"glTF" and version == 2 and length == len(blob)
    json_length, json_type = struct.unpack_from("<I4s", blob, 12)
    assert json_type == b"JSON"
    content = json.loads(blob[20:20+json_length])
    assert len(content.get("skins", [])) == 1, "Expected one quadruped skin"
    assert len(content.get("meshes", [])) == 1, "Wolf mesh count changed"
    skin = content["skins"][0]
    joint_nodes = skin["joints"]
    assert len(joint_nodes) == 46, f"Wolf bone count changed: {len(joint_nodes)}"
    nodes = content["nodes"]
    names = [nodes[index].get("name", "") for index in joint_nodes]
    assert len(set(names)) == 46 and REQUIRED.issubset(set(names)), "Missing or duplicate quadruped bones"
    joint_set = set(joint_nodes)
    parents = {child: parent for parent,node in enumerate(nodes) for child in node.get("children", [])}
    entries = []
    for index in joint_nodes:
        parent = parents.get(index)
        if parent is not None and parent not in joint_set:
            assert nodes[index]["name"] == "root", "Bone outside quadruped skeleton: " + nodes[index]["name"]
            parent = None  # the root is parented to the glTF armature transform
        entries.append({
            "name": nodes[index]["name"],
            "parent": nodes[parent]["name"] if parent is not None else None,
            "translation": nodes[index].get("translation", [0, 0, 0]),
            "rotation": nodes[index].get("rotation", [0, 0, 0, 1]),
            "scale": nodes[index].get("scale", [1, 1, 1]),
        })
    ibm = content["accessors"][skin["inverseBindMatrices"]]
    assert ibm["count"] == 46 and ibm["type"] == "MAT4", "Inverse bind matrices incomplete"
    primitive = content["meshes"][0]["primitives"][0]
    attributes = primitive["attributes"]
    assert {"POSITION", "JOINTS_0", "WEIGHTS_0"}.issubset(attributes)
    vertex_count = content["accessors"][attributes["POSITION"]]["count"]
    assert vertex_count > 1000, "Wolf mesh is empty"
    assert content["accessors"][attributes["JOINTS_0"]]["count"] == vertex_count
    assert content["accessors"][attributes["WEIGHTS_0"]]["count"] == vertex_count
    animations = content["animations"]
    assert len(animations) == 4
    clip_info = []
    for animation in animations:
        source = animation["name"]
        assert source in ALIASES, f"Unrecognized quadruped animation: {source}"
        tracks = animation["channels"]
        targets = {}
        for track in tracks:
            target = track["target"]
            node = target["node"]
            assert node in joint_set, f"{source} animates non-skeleton node"
            key = (node, target["path"])
            assert key not in targets, f"Duplicate track: {source} {key}"
            targets[key] = True
        for joint in joint_nodes:
            for kind in ("translation", "rotation", "scale"):
                assert (joint, kind) in targets, f"{source} missing {nodes[joint]['name']}:{kind}"
        assert len(tracks) == 46*3, f"{source} lost animation channels"
        duration = max(content["accessors"][s["input"]].get("max",[0])[0] for s in animation["samplers"])
        assert duration > 0
        clip_info.append({
            "original": source,
            "canonical": ALIASES[source],
            "channels": len(tracks),
            "duration_seconds": round(duration,6),
        })
    manifest = {
        "family": "Quadruped",
        "reference": "Wolf",
        "source_name": "Wolf - Complete With Quadraped Animation Library.glb",
        "sha256": SOURCE_SHA256,
        "bones": 46,
        "vertex_count": vertex_count,
        "skin": entries,
        "animations": sorted(clip_info, key=lambda item:item["original"]),
        "note": "Reference is exact original; distinct rest rigs require verified retargeting before admission.",
    }
    if "--write-reference" in sys.argv:
        MANIFEST.parent.mkdir(parents=True, exist_ok=True)
        MANIFEST.write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf-8")
    else:
        assert MANIFEST.exists(), "Missing committed wolf reference manifest"
        assert json.loads(MANIFEST.read_text(encoding="utf-8")) == manifest, "Wolf skeleton/animation reference changed"
    print("QUADRUPED_IMPORT_OK wolf 46/46 bones, 4/4 original clips, 138 tracks per clip",
          "vertices", vertex_count, "sha256", SOURCE_SHA256)

if __name__ == "__main__":
    inspect()
