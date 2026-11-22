"""Bake the Classic Ranger or Petite Scout for the Android isometric renderer."""
import bpy, math, sys, os
from mathutils import Vector

args = sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else []
out = args[args.index("--output")+1] if "--output" in args else "build/blender-preview"
variant = args[args.index("--variant")+1] if "--variant" in args else "petite_pale"
os.makedirs(out, exist_ok=True)
bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)

def material(name, color):
    m=bpy.data.materials.new(name); m.diffuse_color=(*color,1)
    m.use_nodes=True
    bs=m.node_tree.nodes.get("Principled BSDF")
    bs.inputs["Base Color"].default_value=(*color,1)
    bs.inputs["Roughness"].default_value=.84
    return m

classic = variant == "classic_study"
skin = material("Skin", (.65,.39,.26) if classic else (.88,.73,.70))
hair = material("Hair", (.075,.043,.023) if classic else (.032,.026,.041))
highlight = material("Hair ribbons", (.14,.075,.039) if classic else (.070,.049,.085))
navy = material("Fitted tunic", (.09,.18,.164) if classic else (.105,.083,.235))
cloth = material("Cape and panels", (.072,.22,.16) if classic else (.26,.092,.29))
metal = material("Armor", (.36,.43,.41) if classic else (.49,.36,.60))
gold = material("Gold trim", (.69,.42,.15))
leather = material("Leather", (.20,.097,.060) if classic else (.11,.079,.13))
eye = material("Eyes", (.08,.14,.14))
lips = material("Lips", (.34,.13,.12) if classic else (.45,.20,.23))

root = bpy.data.objects.new("Classic Ranger" if classic else "Petite Scout", None)
bpy.context.collection.objects.link(root)

def finish(obj, name, mat, limb):
    obj.name = name
    obj.data.materials.append(mat)
    obj.parent = root
    if limb:
        obj["limb"] = limb
    for polygon in obj.data.polygons:
        polygon.use_smooth = True
    return obj

def uv(name, loc, scale, mat, limb=""):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=20, ring_count=12, location=loc)
    obj = bpy.context.object
    obj.scale = scale
    return finish(obj, name, mat, limb)

def surface(name, rings, mat, limb=""):
    """Build a smooth surface from ascending cross sections."""
    count = 24
    vertices = [(x + rx*math.cos(i*math.tau/count), y + ry*math.sin(i*math.tau/count), z)
                for x,y,z,rx,ry in rings for i in range(count)]
    faces = [tuple(reversed(range(count)))]
    for j in range(len(rings)-1):
        for i in range(count):
            a, b = j*count+i, j*count+(i+1)%count
            faces.append((a,b,b+count,a+count))
    faces.append(tuple(range(len(vertices)-count,len(vertices))))
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(vertices, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    subdivision = obj.modifiers.new("Smooth cross sections", "SUBSURF")
    subdivision.levels = subdivision.render_levels = 1
    return finish(obj, name, mat, limb)

w = 1.0 if classic else .88
hip = .279 if classic else .238
surface("Shaped tunic", [(0,0,z,rx,ry) for z,rx,ry in [
    (.98,hip*.89,.14),(1.09,hip,.173),(1.20,hip*.91,.165),
    (1.33,.174*w,.127),(1.44,.199*w,.147),(1.56,.251*w,.178),
    (1.68,.246*w,.151),(1.76,.208*w,.125),(1.81,.100,.087)]], navy)
surface("Waist belt", [(0,0,1.26,.189*w,.140),(0,0,1.28,.186*w,.143),
    (0,0,1.32,.183*w,.138),(0,0,1.34,.180*w,.136)], leather)
uv("Belt clasp", (0,-.141,1.30), (.044,.022,.034), gold)
uv("Neck", (0,0,1.83), (.079,.074,.139), skin)
surface("Face and jaw", [(0,y,z,rx,ry) for z,y,rx,ry in [
    (1.856,-.046,.041,.058),(1.884,-.038,.073,.075),(1.927,-.020,.102,.094),
    (1.987,-.013,.125 if classic else .116,.109),(2.058,-.002,.130 if classic else .119,.110),
    (2.118,.010,.121,.099),(2.163,.022,.086,.078),(2.186,.024,.046,.041)]], skin)
uv("Nose", (0,-.119,1.988), (.020,.029,.030), skin)
uv("Mouth", (0,-.115,1.938), (.037,.011,.009), lips)
uv("Hair cap", (0,.043,2.110), (.146,.127,.132), hair)
for side in (-1,1):
    arm = "arm_left" if side < 0 else "arm_right"
    leg = "leg_left" if side < 0 else "leg_right"
    uv("Eye", (.053*side,-.107,2.035), (.023,.013,.010), eye)
    uv("Brow", (.056*side,-.108,2.062), (.026,.013,.006), highlight)
    ear = uv("Pointed ear", (.145*side,.011,2.042), (.061,.025,.021), skin)
    ear.rotation_euler.y = -.42*side
    shoulder = .235*w*side
    wrist = .339*w*side
    surface("Continuous arm", [
        (wrist,-.031,1.056,.040,.034),(wrist,-.027,1.159,.045,.043),
        (.314*w*side,-.008,1.25,.061,.053),(.294*w*side,.007,1.340,.050,.049),
        (.269*w*side,.008,1.49,.062,.056),(shoulder,0,1.603,.078,.073),
        (shoulder,0,1.685,.060,.060),(.211*w*side,0,1.711,.021,.039)], skin, arm)
    uv("Glove", (wrist,-.038,1.037), (.043,.037,.066), leather, arm)
    uv("Bracer", (.322*w*side,-.024,1.151), (.053,.050,.070), metal, arm)
    uv("Shoulder armor" if classic else "Mantle clasp", (shoulder,.012,1.662),
       (.108,.110,.061) if classic else (.073,.079,.040), metal if classic else cloth, arm)
    leg_x = .133*w*side
    surface("Continuous trouser leg", [
        (leg_x,.019,.17,.049,.051),(leg_x,.017,.254,.055,.060),
        (leg_x,.026,.368,.076,.075),(leg_x,-.015,.541,.062,.065),
        (leg_x,-.018,.62,.070,.070),(leg_x,.001,.78,.089,.094),
        (leg_x,.008,.92,.109,.107),(leg_x,.011,1.05,.107,.106)], leather, leg)
    uv("Boot", (leg_x,-.043,.107), (.066,.123,.070), leather, leg+"_boot")
    uv("Boot cuff", (leg_x,.014,.211), (.058,.060,.033), metal if classic else cloth, leg+"_boot")
    if classic:
        uv("Small knee plate", (leg_x,-.072,.556), (.062,.028,.066), metal, leg)
    else:
        uv("Long side hair", (.114*side,.059,1.94), (.037,.051,.176), hair)
if classic:
    uv("Shaped cuirass", (0,-.127,1.565), (.203,.070,.164), metal)
    uv("Breastplate ridge", (0,-.193,1.553), (.013,.013,.118), gold)
    uv("Short ranger cape", (0,.136,1.465), (.225,.039,.341), cloth)
    uv("Tied hair bun", (0,.145,2.129), (.086,.077,.073), highlight)
    for i in range(5):
        lock = uv("Swept hair lock", (-.103+i*.041,-.049,2.142+i*.007),
                  (.045,.071,.055), hair if i%2 else highlight)
        lock.rotation_euler.y = -.43
else:
    uv("Scout front panel", (0,-.121,1.49), (.099,.065,.244), cloth)
    uv("Sash knot", (.121,-.144,1.29), (.055,.027,.045), gold)
    uv("Straight hair back", (0,.125,1.923), (.136,.057,.251), hair)
    for side in (-1,1):
        swept = uv("Parted fringe", (.055*side,-.040,2.131), (.080,.067,.063), hair)
        swept.rotation_euler.y = .53*side
    for i in range(5):
        uv("Hair sheen", ((i-2)*.042,.168,1.901), (.010,.014,.176), highlight)

world=bpy.context.scene.world
world.color=(.32,.32,.32)
bpy.ops.object.light_add(type="AREA",location=(2,-4,6))
bpy.context.object.data.energy=600;bpy.context.object.data.shape="DISK";bpy.context.object.data.size=5
bpy.ops.object.camera_add(location=(3.5,-6,3.2))
camera=bpy.context.object
direction=Vector((0,0,1.13))-camera.location
camera.rotation_euler=direction.to_track_quat("-Z","Y").to_euler()
camera.data.type="ORTHO";camera.data.ortho_scale=2.69
# Match the renderer's ground anchor at row 160 in each 192-pixel cell.
camera.data.shift_y = -.05625
scene=bpy.context.scene;scene.camera=camera
# Workbench spatial supersampling uses fresh samples for each still.
scene.render.engine = "BLENDER_WORKBENCH"
scene.display.render_aa = "16"
scene.display.shading.light = "STUDIO"
scene.display.shading.color_type = "MATERIAL"
scene.display.shading.show_cavity = True
scene.display.shading.cavity_type = "WORLD"
scene.render.image_settings.color_mode = "RGBA"
scene.render.resolution_x=168;scene.render.resolution_y=192;scene.render.resolution_percentage=100
scene.render.image_settings.file_format="PNG";scene.render.film_transparent=True
scene.view_settings.view_transform="Standard"
# Bake each pose from neutral coordinates. Every boot vertex shares a
# single transform; the neighboring leg uses a continuous displacement.
bpy.context.view_layer.update()
meshes = []
for obj in bpy.data.objects:
    limb = obj.get("limb")
    if obj.type == "MESH" and limb:
        transform = obj.matrix_local.copy()
        meshes.append((obj, limb, [transform @ v.co for v in obj.data.vertices],
                       transform.inverted()))

def unit(value):
    return min(1.0, max(0.0, value))

def pose(mode, phase):
    for obj, limb, neutral, inverse in meshes:
        left = "left" in limb
        swing = phase + (math.pi if left else 0.0)
        for vertex, base in zip(obj.data.vertices, neutral):
            point = base.copy()
            if mode == "walk" and limb.startswith("leg"):
                weight = 1.0 if limb.endswith("boot") else unit((1.04-base.z)/.80)
                point.y -= .165*math.cos(swing)*weight
                point.z += .095*max(0.0, math.sin(swing))*weight
            elif mode == "walk" and limb.startswith("arm"):
                weight = unit((1.69-base.z)/.64)
                point.y += .095*math.cos(swing)*weight
            elif mode == "punch" and limb == "arm_right":
                reach = math.sin(math.pi*phase)*unit((1.69-base.z)/.64)
                point.y -= .43*reach
                point.z += .33*reach
            vertex.co = inverse @ point
        obj.data.update()

# The editable source contains the neutral continuous-mesh figure.
bpy.ops.wm.save_as_mainfile(filepath=os.path.abspath(os.path.join(out,variant+".blend")))
for facing in (0,1,2,3,4):
    root.rotation_euler.z=facing*math.pi/4
    for frame in range(1):
        pose("idle",0)
        root.location.z=.012*math.sin(frame*math.pi/2)
        scene.render.filepath=os.path.abspath(os.path.join(out,"idle_f%d_%d.png"%(facing,frame)))
        bpy.ops.render.render(write_still=True)
    for frame in range(8):
        phase=frame*math.tau/8
        pose("walk",phase)
        root.location.z=0
        scene.render.filepath=os.path.abspath(os.path.join(out,"walk_f%d_%d.png"%(facing,frame)))
        bpy.ops.render.render(write_still=True)
    for frame in range(3):
        pose("punch",frame/4)
        root.location.z=0
        scene.render.filepath=os.path.abspath(os.path.join(out,"punch_f%d_%d.png"%(facing,frame)))
        bpy.ops.render.render(write_still=True)
print("BLENDER_CHARACTER_IDLE_WALK_KNEE_PUNCH_OK",out)
