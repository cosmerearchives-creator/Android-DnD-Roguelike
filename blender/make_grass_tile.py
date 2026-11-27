"""Make a walkable 5-foot grass plane referencing the user's original unmodified PBR image bytes.
Do not use the source 1,896,609-vertex vertical grass mesh as a repeated ground tile.
"""
import json,struct,copy,hashlib
from pathlib import Path
source=Path(r"G:\ChatGPT Projects\Forests of Hell\Background Assets\Meshy_AI_grass_texture(1).glb")
target=Path(r"G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource\godot\assets\terrain\grass.glb")
data=source.read_bytes()
n=struct.unpack_from("<I",data,12)[0];j=json.loads(data[20:20+n])
base=20+n+8
out=bytearray()
views=[]
def add(binary,target_=None):
 pad=(-len(out))%4
 out.extend(b'\0'*pad)
 v={"buffer":0,"byteOffset":len(out),"byteLength":len(binary)}
 if target_ is not None:v["target"]=target_
 views.append(v);out.extend(binary)
 return len(views)-1
images=[]
for im in j["images"]:
 v=j["bufferViews"][im["bufferView"]]
 start=base+v.get("byteOffset",0)
 clone=copy.deepcopy(im);clone["bufferView"]=add(data[start:start+v["byteLength"]])
 images.append(clone)
# x/z plane, glTF Y up. Source top half grass image uses original UVs.
p=[(-.762,.004,-.762),(.762,.004,-.762),(.762,.004,.762),(-.762,.004,.762)]
norm=[(0.,1.,0.)]*4
uv=[(.025,.03),(.975,.03),(.975,.54),(.025,.54)]
ids=(0,2,1,0,3,2)
def acc(vals,fmt,component,kind,target_,minimum=None,maximum=None):
 flat=[c for v in vals for c in v] if isinstance(vals[0],(list,tuple)) else list(vals)
 raw=struct.pack("<"+fmt*len(flat),*flat)
 ix=add(raw,target_)
 a={"bufferView":ix,"byteOffset":0,"componentType":component,"count":len(vals),"type":kind}
 if minimum is not None:a["min"]=minimum
 if maximum is not None:a["max"]=maximum
 return a
accessors=[
 acc(p,"f",5126,"VEC3",34962,[-.762,.004,-.762],[.762,.004,.762]),
 acc(norm,"f",5126,"VEC3",34962),
 acc(uv,"f",5126,"VEC2",34962),
 acc(ids,"H",5123,"SCALAR",34963)
]
mat=copy.deepcopy(j["materials"][0])
mat["doubleSided"]=True
obj={"asset":{"version":"2.0","generator":"Forest of Hell grass material reuse (no texture conversion)"},
     "scenes":[{"nodes":[0]}],"scene":0,"nodes":[{"mesh":0,"name":"Grass 5ft ground"}],
     "meshes":[{"name":"Grass 5ft PBR source","primitives":[{"attributes":{"POSITION":0,"NORMAL":1,"TEXCOORD_0":2},"indices":3,"material":0}]}],
     "materials":[mat],"textures":copy.deepcopy(j["textures"]),"samplers":copy.deepcopy(j.get("samplers",[])),
     "images":images,"bufferViews":views,"accessors":accessors,
     "buffers":[{"byteLength":len(out)}],"extras":{"original_source_sha256":hashlib.sha256(data).hexdigest(),"original_texture_bytes_unchanged":True,"ground_tile_meters":1.524}}
jb=json.dumps(obj,separators=(',',':')).encode('utf8')
jb+=b' '*((-len(jb))%4)
out.extend(b'\0'*((-len(out))%4))
pack=struct.pack('<4sII',b'glTF',2,12+8+len(jb)+8+len(out))+struct.pack('<I4s',len(jb),b'JSON')+jb+struct.pack('<I4s',len(out),b'BIN\x00')+out
target.write_bytes(pack)
print("CREATED",target,target.stat().st_size,"source",hashlib.sha256(data).hexdigest(),"images",[(i.get("name"),i.get("mimeType")) for i in images])
