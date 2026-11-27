"""CI gate for the full-resolution terrain and tavern texture pipeline.
All five ground GLBs and tavern room are original, unmodified imports.
The grass GLB is a two-triangle derivative retaining the source PBR images byte-for-byte.
"""
from pathlib import Path
import hashlib
import json
import struct

ASSETS = Path(__file__).resolve().parents[1]/"godot"/"assets"/"terrain"
DIGESTS = {
 "dirt": "71ef90de60df179e55d952bb8e1f53ebf70a39e7066cab69c511f7e76cd8c9cd",
 "leaves": "57e18e3e2f6df57effa1efe440b1db3ca773c1487f5dba32f62bc494bc98ba43",
 "moss": "22b5d22eb8a324a76e59cc2398c47ae7e971bafd89d96de08ab29b678c7ad0a7",
 "mud": "f8c63d843112688d8c206f8928315f7de484623998f2b75b9838380444ab8441",
 "rocky": "65d415aaf702ad1db21cd7053f135cd8131405b9206641de418ef05ff53fe03d",
 "tavern_room": "fa9f590598c1a33b4805b560be5ae2da32727a20909994f20121616f62bf295f",
}
GRASS_ORIGINAL_SHA = "ebe40701ac73d59c264f6d8c22c7ed4223f4520039de6be9b93749066126c0d2"

def inspect(key):
    path=ASSETS/(key+".glb")
    blob=path.read_bytes()
    assert blob[:4]==b"glTF" and struct.unpack_from("<I",blob,8)[0]==len(blob),key
    size,typ=struct.unpack_from("<I4s",blob,12)
    assert typ==b"JSON",key
    info=json.loads(blob[20:20+size])
    bbase=20+size+8
    assert blob[bbase-4:bbase]==b"BIN\x00",key
    if key in DIGESTS:
        assert hashlib.sha256(blob).hexdigest()==DIGESTS[key],key+" source GLB altered"
    else:
        # New grass is the exact unmodified imported source (3028 vertices).
        assert hashlib.sha256(blob).hexdigest()==GRASS_ORIGINAL_SHA,"grass source GLB altered"
        positions=info["accessors"][0]
        assert positions["count"]==3028
        assert 0.94 < positions["max"][0]-positions["min"][0] < 1.0
        assert 0.94 < positions["max"][2]-positions["min"][2] < 1.0
    images=info["images"]
    assert len(images)==(4 if key=="tavern_room" else 3),key
    for index,image in enumerate(images):
        view=info["bufferViews"][image["bufferView"]]
        begin=bbase+view.get("byteOffset",0)
        raw=blob[begin:begin+view["byteLength"]]
        ext=".jpg" if image.get("mimeType")=="image/jpeg" else ".png"
        suffix=image.get("name",str(index))
        companion=ASSETS/(key+"_"+suffix+ext)
        assert companion.is_file(),str(companion)
        assert companion.read_bytes()==raw,key+" source texture changed or omitted: "+companion.name
    assert info["materials"] and info["meshes"],key
    print("TERRAIN_SOURCE_AND_PBR_BYTES_OK",key,"textures",len(images),"vertices",info["accessors"][0]["count"])

def main():
    for key in ("grass","dirt","leaves","moss","mud","rocky","tavern_room"):
        inspect(key)
    print("TERRAIN_AND_TAVERN_MATERIAL_GATE_OK")

if __name__=="__main__":
    main()
