import bpy, bmesh, json, math, hashlib, struct, shutil
from pathlib import Path
from mathutils import Vector, Matrix
root=Path(r"G:\ChatGPT Projects\Forests of Hell")
src=root/"Beastiary Review"
review=root/"Ground Cover Review"
game=root/"ForestsOfHellSource"/"godot"/"assets"/"ground_cover"
game.mkdir(parents=True,exist_ok=True)
sources=[
("Meshy_AI_Assorted_Wild_Ground_Plants_20261127_texture(1).glb","woodland_bonus",[
("woodland_extra_01",54,42),("woodland_extra_02",116,33),("woodland_extra_03",179,33),("woodland_extra_04",225,21),
("woodland_extra_05",55,84),("woodland_extra_06",174,80),
("woodland_extra_07",195,130),("woodland_extra_08",126,149),("woodland_extra_09",126,186),("woodland_extra_10",189,185),("woodland_extra_11",244,173),("woodland_extra_12",70,206)]),
("Meshy_AI_Assorted_Wild_Ground_Plants_20261127_texture(2).glb","plants",[
("tall_grass_01",70,20),("tall_grass_02",126,21),("tall_grass_03",178,27),("tall_grass_04",228,22),
("small_fern_01",68,112),("small_fern_02",126,113),("small_fern_03",183,112),
("woodland_plant_01",74,204),("woodland_plant_02",126,203),("woodland_plant_03",179,205),("woodland_plant_04",233,207)]),
("Meshy_AI_Forest_Floor_Debris_Collection_20261127_texture.glb","debris",[
("loose_stone_01",57,19),("loose_stone_02",121,18),("loose_stone_03",177,17),("loose_stone_04",236,17),
("fallen_leaves_01",63,84),("fallen_leaves_02",121,82),("fallen_leaves_03",177,83),
("fallen_twig_01",67,148),("fallen_twig_02",121,150),("fallen_twig_03",177,149),
("fallen_branch_01",64,211),("fallen_branch_02",121,213),("fallen_branch_03",176,208)]),
("Meshy_AI_Forest_Floor_Elements_20261127_texture.glb","elements",[
("mushroom_01",73,17),("mushroom_02",122,21),("mushroom_03",174,26),("mushroom_04",226,23),
("moss_cluster_01",79,85),("moss_cluster_02",130,90),("moss_cluster_03",180,85),
("exposed_root_01",88,152),("exposed_root_02",139,156),("exposed_root_03",189,157),
("pinecone_01",85,215),("pinecone_02",125,211)])]
result=[]
for filename,group,assets in sources:
    p=src/filename
    assert p.is_file(),p
    sha=hashlib.sha256(p.read_bytes()).hexdigest()
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(p))
    obj=next(o for o in bpy.data.objects if o.type=="MESH")
    orig=obj.data
    # The exported sheets were composed as separate miniature objects but
    # flattened into one huge mesh. Preserve every connected component intact.
    nv=len(orig.vertices)
    parent=list(range(nv))
    size=[1]*nv
    def find(a):
        while parent[a]!=a:
            parent[a]=parent[parent[a]];a=parent[a]
        return a
    def join(a,b):
        a=find(a);b=find(b)
        if a==b:return
        if size[a]<size[b]:a,b=b,a
        parent[b]=a;size[a]+=size[b]
    for face in orig.polygons:
        a=face.vertices[0]
        for b in face.vertices[1:]:join(a,b)
    roots=[find(i) for i in range(nv)]
    points=[obj.matrix_world@v.co for v in orig.vertices]
    minx=min(v.x for v in points);maxx=max(v.x for v in points)
    miny=min(v.y for v in points);maxy=max(v.y for v in points)
    centerx=(minx+maxx)/2;centery=(miny+maxy)/2
    component=[]
    for k in set(roots):
        idx=[i for i in range(nv) if roots[i]==k]
        cx=sum(points[i].x for i in idx)/len(idx)
        cy=sum(points[i].y for i in idx)/len(idx)
        # The 300x225 review image used a 1.25m orthographic camera and 4x scale reduction.
        px=150+(cx-centerx)*180
        py=112.5-(cy-centery)*180
        nearest=min(range(len(assets)),key=lambda j:(px-assets[j][1])**2+(py-assets[j][2])**2)
        component.append((k,nearest))
    mapping=dict(component)
    groups={j:[] for j in range(len(assets))}
    for v,rootid in enumerate(roots):groups[mapping[rootid]].append(v)
    # Any slivers are kept in their original nearest spatial cluster.
    source_mats=[m.name for m in orig.materials]
    bpy.data.objects.remove(obj,do_unlink=True)
    details=[]
    for i,(asset,x,y) in enumerate(assets):
        verts=groups[i]
        if len(verts)<65:raise RuntimeError(f"{filename} {asset} insufficient geometry: {len(verts)}")
        mask=set(verts)
        mesh=orig.copy()
        bm=bmesh.new();bm.from_mesh(mesh);bm.verts.ensure_lookup_table()
        delete=[v for v in bm.verts if v.index not in mask]
        bmesh.ops.delete(bm,geom=delete,context='VERTS')
        bm.to_mesh(mesh);bm.free();mesh.update()
        name=asset
        piece=bpy.data.objects.new(name,mesh)
        bpy.context.collection.objects.link(piece)
        # preserve imported materials and rotation, but relocate independently
        pts=[points[k] for k in verts]
        xmid=(min(v.x for v in pts)+max(v.x for v in pts))/2
        ymid=(min(v.y for v in pts)+max(v.y for v in pts))/2
        zlow=min(v.z for v in pts)
        # original obj removed but capture transform prior? for glTF source its
        # matrix was rotation only; copied geometry still in imported local axes.
        piece.matrix_world=Matrix.Translation((-xmid,-ymid,-zlow))
        # MultiMesh instancing uses mesh-local geometry without the GLB node
        # transform. Bake this centering into mesh vertices, preserve UV/material.
        piece.data.transform(piece.matrix_world)
        piece.matrix_world=Matrix.Identity(4)
        # apply glTF source axis correction (typically Blender default imported
        # source mesh object is already the converted axis coordinate system).
        bounds=[round(max(v[j] for v in pts)-min(v[j] for v in pts),3) for j in range(3)]
        details.append({"id":name,"vertices":len(verts),"world_dimensions_m":bounds,"center":[round(xmid,4),round(ymid,4)],"origin_bottom":round(zlow,4)})
    # Original file maps are kept byte-identical in review. Blender exporter
    # produces a new, shared-image, multi-object GLB for efficient game instancing.
    out=game/(group+".glb")
    bpy.ops.object.select_all(action='DESELECT')
    for o in bpy.data.objects:
        if o.type=='MESH':o.select_set(True)
    bpy.context.view_layer.objects.active=next(o for o in bpy.data.objects if o.type=='MESH')
    bpy.ops.export_scene.gltf(filepath=str(out),export_format='GLB',use_selection=True,export_image_format='AUTO',export_animations=False,export_texcoords=True,export_normals=True)
    binary=out.read_bytes();jlen=struct.unpack_from('<I',binary,12)[0];data=json.loads(binary[20:20+jlen])
    meshes=data.get('meshes',[])
    if len(meshes)!=len(assets):raise RuntimeError(f'GLB exported only {len(meshes)}/{len(assets)} meshes for {group}')
    record={"source":filename,"original_sha256":sha,"group":group,"output":str(out),"materials":source_mats,"source_component_count":len(component),"variants":details,"exported_meshes":len(meshes),"exported_images":len(data.get('images',[]))}
    result.append(record)
    print("EXTRACTED",group,"assets",len(assets),"per_variant_verts",[x["vertices"] for x in details],"images",record["exported_images"],flush=True)
(review/"extracted_variants.json").write_text(json.dumps(result,indent=2),encoding='utf-8')
print("GROUP_EXTRACTION_COMPLETE",sum(len(x["variants"]) for x in result))
