"""Build Katie as a playable full-body character from the supplied legacy portrait.

The original portrait is face and bust only. Preserve its facial mesh and embedded
colour map, replace legacy particle hair with geometry, and reuse Forests of Hell's
existing rigless procedural body and baked walk/punch pose system.

Run from repo root with Blender 4+:
blender -b -t 2 --python-exit-code 1 --python blender/build_katie.py -- SOURCE_ZIP
"""
import bpy, math, sys, os, pathlib, tempfile, zipfile
from mathutils import Vector

base = pathlib.Path(__file__).resolve().parent.parent
args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
source = pathlib.Path(args[0]) if args else pathlib.Path(os.environ.get("USERPROFILE","")) / "Downloads" / "Katie-drive.zip"
if not source.exists():
    raise FileNotFoundError(source)
with zipfile.ZipFile(source) as archive:
    names = [n for n in archive.namelist() if n.lower().endswith(".blend")]
    if not names: raise RuntimeError("Katie ZIP contains no Blender model")
    original = pathlib.Path(tempfile.gettempdir()) / "forests_katie_source.blend"
    original.write_bytes(archive.read(max(names, key=lambda n: archive.getinfo(n).file_size)))
bpy.ops.wm.open_mainfile(filepath=str(original), load_ui=False)
portrait = [bpy.data.objects[n] for n in ("katie","eye.l","eye.r","eyebrows","eyelashes","scalp")]
for o in portrait:
    o.name = "Imported Katie " + o.name
# The existing body builder clears the scene; preserve the imported meshes
# before calling it and restore them afterward.
portrait_copies=[]
for o in portrait:
    mesh=o.data.copy()
    mats=[slot.material for slot in o.material_slots]
    portrait_copies.append((o.name,mesh,o.matrix_world.copy(),mats))
# Images are packed in the original file, so preserve their pixels before
# the procedural character builder clears unused datablocks.
image_copies={}
for nm in ("Katie_Color_4K.png","Katie-eyes.png"):
    im=bpy.data.images.get(nm)
    if im:
        # Packed legacy images need an explicit load before their has_data flag.
        if not im.has_data:
            im.reload()
        im.scale(max(1,round(im.size[0]*min(1,1024/max(im.size)))), max(1,round(im.size[1]*min(1,1024/max(im.size)))))
        im.pack()
        image_path=pathlib.Path(tempfile.gettempdir())/("forests_"+nm.replace(".png","_mobile.png"))
        im.filepath_raw=str(image_path)
        im.file_format="PNG"
        im.save()
        image_copies[nm]=str(image_path)

# Existing pose source is the vetted Forests of Hell procedural character mesh.
# Execute only its character-building prelude; it does not render or save images.
sys.argv = ["blender","--","--variant","petite_pale","--output",str(base/"godot"/"assets")]
body_source = (base / "blender" / "create_character.py").read_text(encoding="utf-8")
body_namespace = {"__name__": "forests_katie_body"}
character_prelude=body_source.split("# The editable source contains the neutral continuous-mesh figure.")[0]
exec(compile(character_prelude,str(base/"blender"/"create_character.py"),"exec"),body_namespace)
root = body_namespace["root"]
root.name = "Katie - playable"
root.rotation_euler = (0,0,0)
portrait=[]
for name, mesh, world, mats in portrait_copies:
    obj=bpy.data.objects.new(name,mesh)
    bpy.context.collection.objects.link(obj)
    obj.matrix_world=world
    portrait.append(obj)

# Remove all the procedural head, face and hair. Retain torso, arms, legs,
# boots and outfit, including existing per-limb pose deformation handles.
remove_names = ("Neck", "Face and jaw", "Nose", "Mouth", "Hair cap",
                "Pointed ear", "Eye", "Brow", "Long side hair", "Straight hair back",
                "Parted fringe", "Hair sheen", "Tied hair bun", "Swept hair lock")
for obj in list(root.children):
    if obj.name.startswith(remove_names):
        bpy.data.objects.remove(obj, do_unlink=True)

# Preserve original face and facial features, with mobile-compatible materials.
# Legacy portrait Y is up and negative Z is forward, after the original armature
# coordinate transform. Its physical geometry is stored in object-local Z.
restored_images={}
for nm,img_path in image_copies.items():
    img=bpy.data.images.load(img_path)
    img.pack()
    restored_images[nm]=img
skin_image=restored_images.get("Katie_Color_4K.png")
eye_image=restored_images.get("Katie-eyes.png")

def pbr(name, color, image=None, alpha=1):
    mat=bpy.data.materials.new("Mobile Katie "+name)
    mat.diffuse_color=(*color[:3],alpha)
    mat.use_nodes=True
    nodes=mat.node_tree.nodes;nodes.clear()
    out=nodes.new("ShaderNodeOutputMaterial")
    bs=nodes.new("ShaderNodeBsdfPrincipled")
    bs.inputs["Base Color"].default_value=(*color[:3],alpha)
    bs.inputs["Roughness"].default_value=.76
    if image:
        tex=nodes.new("ShaderNodeTexImage");tex.image=image
        mat.node_tree.links.new(tex.outputs["Color"],bs.inputs["Base Color"])
    mat.node_tree.links.new(bs.outputs["BSDF"],out.inputs["Surface"])
    return mat
face_mat=pbr("skin",(0.83,0.63,0.54),skin_image)
body_skin_mat=pbr("body skin",(0.78,0.63,0.54))
eyeball_mat=pbr("eyes",(0.92,0.92,0.90),eye_image)
lash_mat=pbr("lashes",(0.10,0.07,0.055))
hair_mat=pbr("hair",(0.16,0.075,0.045))
teeth_mat=pbr("teeth",(0.90,0.87,0.83))

# Face detail is located in the imported head's own local reference frame.
# Scale the bust face down to match the playable figure; don't include the
# old blouse/shoulders or the 6-bone portrait rig.
# Centre the head based on its world-space bounds, and match chin to neck.
head = portrait[0]
# The bust includes shoulder skin; keep it under the outfit collar.
face_scale=0.43
for o in portrait:
    for modifier in list(o.modifiers):
        if modifier.type in {"ARMATURE","PARTICLE_SYSTEM","SUBSURF"}:
            o.modifiers.remove(modifier)
    world=o.matrix_world.copy()
    o.parent=None
    o.matrix_world=world
    for slot in o.material_slots:
        if o == portrait[0] or o == portrait[-1]:slot.material=face_mat if o == portrait[0] else hair_mat
        elif o in portrait[1:3]:slot.material=eyeball_mat
        else:slot.material=lash_mat
    # The original portrait is a stylized standing bust facing negative Y;
    # zero its parent frame and resize around the centre of the source head.
    bpy.context.view_layer.update()
# Original face centre is ~Z 0.55; place its crown at ~2.22m.
for o in portrait:
    o.location = ((o.location.x)*face_scale, (o.location.y-.026)*face_scale-.001, (o.location.z-.42)*face_scale+2.02)
    o.scale *= face_scale
    o.parent=root
    # Remove skin/hair parts that rely entirely on legacy particle rendering.
    # Rebuild long hair as curves instead.

# Realistic hair silhouette using lightweight tapered mesh strips / curves:
# mesh ribbons are exported reliably to glTF unlike particle systems.
def add_hair_strand(index,x,z0,length):
    curve=bpy.data.curves.new("Katie hair strand %02d"%index,"CURVE")
    curve.dimensions="3D"
    curve.resolution_u=2
    curve.bevel_depth=.009 if index%3 else .014
    curve.bevel_resolution=2
    spline=curve.splines.new("BEZIER");spline.bezier_points.add(3)
    for j,pt in enumerate(spline.bezier_points):
        t=j/3
        pt.co=(x*(1+.18*t)+.025*math.sin(index*1.7+t*2.4)*t,
               .075+(.045*math.sin(index*2.1))*t,
               z0-length*t)
        pt.handle_left_type="AUTO";pt.handle_right_type="AUTO"
    ob=bpy.data.objects.new(curve.name,curve);bpy.context.collection.objects.link(ob)
    curve.materials.append(hair_mat); ob.parent=root
    bpy.ops.object.select_all(action="DESELECT");ob.select_set(True)
    bpy.context.view_layer.objects.active=ob
    bpy.ops.object.convert(target="MESH")
for i in range(46):
    t=i/46*math.tau
    x=.118*math.cos(t)
    if math.sin(t)<-.6: continue # keep face visible
    add_hair_strand(i,x,2.17,.29+.10*(i%5)/5)

# Swap skin tint on exposed arms and neck to complement the imported face.
for obj in root.children:
    if obj.type=="MESH" and (obj.name.startswith("Continuous arm") or obj.name.startswith("Neck")):
        for slot in obj.material_slots: slot.material=body_skin_mat

# Pose maps should include all body mesh parts except the imported face.
bpy.context.view_layer.update()
body_parts=[]
for obj in root.children:
    if obj.type!="MESH" or obj in portrait or obj.name.startswith("Katie hair strand"): continue
    if obj.name not in [o.name for o in bpy.context.scene.objects]:continue
    local=obj.matrix_local.copy()
    body_parts.append((obj,obj.get("limb") or "",[local@v.co for v in obj.data.vertices],local.inverted()))
body_namespace["meshes"]=body_parts
pose=body_namespace["pose"]
# Ensure stored source transforms for hair/face are retained across frame exports.
outdir=base/"godot"/"assets"
outdir.mkdir(parents=True,exist_ok=True)
for name,mode,phase in [("katie","idle",0)]+[("katie_walk_%d"%i,"walk",i*math.tau/8) for i in range(8)]+[("katie_punch_%d"%i,"punch",i/4) for i in range(5)]:
    pose(mode,phase)
    bpy.context.view_layer.update()
    bpy.ops.object.select_all(action="DESELECT")
    for ob in root.children:
        if ob.type=="MESH":
            ob.select_set(True)
    bpy.context.view_layer.objects.active=next(o for o in root.children if o.type=="MESH")
    bpy.ops.export_scene.gltf(filepath=str(outdir/(name+".glb")),export_format="GLB",use_selection=True,
        export_animations=False,export_skins=False,export_materials="EXPORT")
    print("KATIE_FRAME",name,(outdir/(name+".glb")).stat().st_size)
print("KATIE_COMPLETE 14 playable frames")
