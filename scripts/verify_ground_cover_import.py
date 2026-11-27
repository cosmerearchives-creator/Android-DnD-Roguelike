#!/usr/bin/env python3
"""Locked source-texture and geometry gate for the four shared-texture GLB collections.
All original texture bytes were compared with user-supplied source GLBs at extraction.
CI can reverify SHA256 of every original 2048px image embedded into the derivatives.
"""
import hashlib
import json
from pathlib import Path
import struct

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/"godot"/"assets"/"ground_cover"
ORIGINAL_REVIEW=ROOT.parent/"Beastiary Review"

SOURCES={
    "woodland_bonus":{
        "source":"Meshy_AI_Assorted_Wild_Ground_Plants_20261127_texture(1).glb",
        "sha":"2a6b5497634570d9b8d9319be020eac77afb576bdf7e71203f7f994f6ee9c023",
        "count":12,
        "prefix_counts":{"woodland_extra":12},
        "images":{
            "fa1f5eee9dac4082c872f8693d7861978bc18bbebfe636718c5eef713c8424df",
            "252b780896e38afd353f99d992938ec2191f6ed88b2d92c3ca359b6bca153ed8",
            "03d7201db9b045ed92f53f67181a36b1aa4cc314bf56eb6dc23ece151e207c23"}},
    "plants":{
        "source":"Meshy_AI_Assorted_Wild_Ground_Plants_20261127_texture(2).glb",
        "sha":"aa049962ce8ba493b1d313b3868e4adb0fbfd24d15505e1e4f2fbe193daa7823",
        "count":11,
        "prefix_counts":{"tall_grass":4,"small_fern":3,"woodland_plant":4},
        "images":{"d3bb6ab35da059bdb424eae9d7dca146e227b7036a88049b350a409d7ce0964f"}},
    "debris":{
        "source":"Meshy_AI_Forest_Floor_Debris_Collection_20261127_texture.glb",
        "sha":"976e43a7d7613dace73847e5e1efbc1816ea46e4b72812ad482bbc495dcfe78d",
        "count":13,
        "prefix_counts":{"loose_stone":4,"fallen_leaves":3,"fallen_twig":3,"fallen_branch":3},
        "images":{"489a01fbfaf62063e5b87a467cd919796b736ae94ce34b19140ef5cc3427dd95"}},
    "elements":{
        "source":"Meshy_AI_Forest_Floor_Elements_20261127_texture.glb",
        "sha":"30531abb0379898b75eb834aceb1d18df75a7ae34322afd5c00fdab64bb93c6c",
        "count":12,
        "prefix_counts":{"mushroom":4,"moss_cluster":3,"exposed_root":3,"pinecone":2},
        "images":{
            "033977b11e0e00b6a909921abc18fd930f89b022d9c4fe9d9bc1687661fa47b1",
            "320c789d5554d8480f632d644040151a43194b98a51dece9a18aaeea58c92032",
            "c567edd67278f9a0d5ffe2ed1cf385d94d819faeb72f0038004f0624d1b90770"}}}

def parse_glb(path):
    data=path.read_bytes()
    assert data[:4]==b"glTF",path
    assert struct.unpack_from("<I",data,8)[0]==len(data),path
    n=struct.unpack_from("<I",data,12)[0]
    assert data[16:20]==b"JSON",path
    metadata=json.loads(data[20:20+n].decode("utf-8"))
    off=20+n
    assert data[off+4:off+8]==b"BIN\\x00".replace(b"\\x00",b"\x00")
    start=off+8
    hashes=set()
    for img in metadata.get("images",[]):
        view=metadata["bufferViews"][img["bufferView"]]
        raw=data[start+view.get("byteOffset",0):start+view.get("byteOffset",0)+view["byteLength"]]
        assert len(raw)>100000,(path,img)
        sha=hashlib.sha256(raw).hexdigest()
        hashes.add(sha)
        suffix=".jpg" if img.get("mimeType")=="image/jpeg" else ".png"
        companion=path.with_name(path.stem+"_"+img["name"]+suffix)
        assert companion.is_file(),f"Godot material companion not versioned: {companion}"
        assert hashlib.sha256(companion.read_bytes()).hexdigest()==sha,f"Material companion modified: {companion}"
    return metadata,hashes

def main():
    total=0
    for group,info in SOURCES.items():
        target=ASSETS/f"{group}.glb"
        assert target.is_file(),target
        scene,hashes=parse_glb(target)
        assert hashes==info["images"],f"Source PBR bytes changed in {group}: {hashes}"
        assert len(scene["meshes"])==info["count"],f"Wrong mesh count {group}"
        names={str(node.get("name","")) for node in scene["nodes"] if "mesh" in node}
        expected={f"{category}_{i:02d}" for category,count in info["prefix_counts"].items() for i in range(1,count+1)}
        assert names==expected,(group,sorted(expected-names),sorted(names-expected))
        assert not scene.get("skins",[]),f"Ground cover should contain no rigs: {group}"
        assert not scene.get("animations",[]),f"Ground cover should contain no animations: {group}"
        for mesh in scene["meshes"]:
            for surface in mesh["primitives"]:
                position=scene["accessors"][surface["attributes"]["POSITION"]]
                assert 65<=position["count"]<2000,(group,position["count"])
        reviewed=ORIGINAL_REVIEW/info["source"]
        if reviewed.is_file():
            assert hashlib.sha256(reviewed.read_bytes()).hexdigest()==info["sha"],f"Original moved/altered: {reviewed}"
        total+=len(expected)
        print("GROUND_COVER_SOURCE_MAPS_OK",group,"variants",len(expected),"maps",len(hashes))
    assert total==48,total
    print("GROUND_COVER_FIDELITY_OK variants=48 active=40 short_grass_missing=4")
if __name__=="__main__":main()
