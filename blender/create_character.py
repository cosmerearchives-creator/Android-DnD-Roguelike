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

if variant == "dagger_goblin":
    root.name = "Dagger Goblin"
    skin.diffuse_color = (.34,.43,.20,1)
    navy.diffuse_color = (.23,.135,.075,1)
    cloth.diffuse_color = (.31,.21,.10,1)
    metal.diffuse_color = (.38,.40,.35,1)
    eye.diffuse_color = (.80,.49,.14,1)
    w = .98
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
    for i in range(3):
        tuft=uv("Crown tuft",((i-1)*.031,.041,2.171),(.029,.044,.043),hair)
        tuft.rotation_euler.x = -.32
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
        surface("Low leather boot", [
            (leg_x,-.050,.035,.060,.105),(leg_x,-.050,.045,.070,.120),
            (leg_x,-.039,.085,.070,.116),(leg_x,-.017,.118,.063,.085),
            (leg_x,.015,.159,.053,.060),(leg_x,.016,.216,.055,.057),
            (leg_x,.016,.245,.057,.059)], leather, leg+"_boot")
        uv("Boot cuff", (leg_x,.014,.211), (.058,.060,.033), metal if classic else cloth, leg+"_boot")
        if classic:
            uv("Small knee plate", (leg_x,-.072,.556), (.062,.028,.066), metal, leg)
        uv("Lower tusk", (.029*side,-.115,1.945), (.009,.012,.022), gold)
        ear.location.x *= 1.16
        ear.scale.x *= 1.48
        ear.rotation_euler.y = -.32*side
    uv("Hooked nose tip",(0,-.147,1.985),(.024,.027,.027),skin)
    uv("Rough leather cross-body panel",(-.052,-.12,1.46),(.112,.063,.217),cloth)
    uv("Belt pouch",(-.172,-.109,1.245),(.069,.054,.070),leather)
    # A short blade in hand. Two spare daggers ride on the belt.
    def dagger(label, x, y, z, angle):
        grip=uv(label+" grip",(x,y,z),(.019,.027,.065),leather)
        guard=uv(label+" guard",(x,y,z-.053),(.054,.023,.013),gold)
        blade=surface(label+" blade", [(x,y,z-.269,.002,.005),(x,y,z-.206,.026,.012),
                      (x,y,z-.073,.037,.015),(x,y,z-.059,.033,.016)],metal)
        pivot=bpy.data.objects.new(label,None)
        bpy.context.collection.objects.link(pivot)
        pivot.parent=root
        pivot.location=(x,y,z)
        bpy.context.view_layer.update()
        for part in (grip,guard,blade):
            part.parent=pivot
            part.matrix_parent_inverse=pivot.matrix_world.inverted()
        pivot.rotation_euler.y=angle
    dagger("Drawn dagger",.329,-.072,1.05,-.32)
    dagger("Belt dagger one",-.16,-.128,1.30,.20)
    dagger("Belt dagger two",-.211,-.093,1.28,.39)
    # Compact goblin proportions with a larger head and longer ears.
    for obj in list(root.children):
        if obj.type!="MESH": continue
        if obj.name.startswith(("Hair cap","Eye","Brow","Face and jaw","Pointed ear","Nose","Hooked","Mouth","Lower tusk")):
            if obj.name.startswith("Face and jaw"):
                for v in obj.data.vertices:
                    v.co.x *= 1.20
            else:
                obj.location.x *= 1.20
                if obj.name.startswith("Hair cap"): obj.scale.x *= 1.15
    root.scale=(.79,.78,.62)
else:
    w = 1.0 if classic else .88
    hip = .279 if classic else .238
    torso_skin = surface("Upper torso skin", [(0,0,z,rx,ry) for z,rx,ry in [
        (1.32,.162*w,.115),(1.47,.198*w,.139),(1.60,.233*w,.158),
        (1.645,.235*w,.146),(1.70,.232*w,.123),(1.76,.191*w,.105),(1.81,.093,.079)]], skin)
    for vertex in torso_skin.data.vertices:
        x,y,z = vertex.co
        if y >= 0:
            continue
        front = min(1.0,-y/.12)**2
        aim = .074*w
        contour = math.exp(-((abs(x)-aim)/(.043*w))**2)
        sternum = math.exp(-(x/(.034*w))**2)
        weight = math.exp(-((z-1.625)/.077)**2)
        vertex.co.y += front*weight*(.014*sternum-(.040 if classic else .044)*contour)
    torso_skin.data.update()

    def fitted_bodice():
        n = 64
        rings = [
            (1.225,.188*w,.141),(1.31,.166*w,.124),(1.40,.181*w,.138),
            (1.48,.203*w,.161),(1.55,.233*w,.184),(1.62,.229*w,.161),
            (1.70,.222*w,.131)]
        vertices = []
        for j,(z,rx,ry) in enumerate(rings):
            for i in range(n):
                angle=math.tau*i/n
                xx,depth=math.cos(angle),math.sin(angle)
                front=max(0,-depth)
                # Keep the original outer depth, but shape two rounded contours
                # and a shallow transition at the sternum.
                fullness = .045 if classic else .055
                bust_weight = math.exp(-((z-1.56)/.085)**2)
                yy = ry*depth - front**2*fullness*math.exp(-((z-1.56)/.09)**2)
                center = math.exp(-(xx/.21)**2)
                side = math.exp(-((abs(xx)-.43)/.23)**2)
                yy += front**2*bust_weight*(.029*center - .024*side)
                yy = max(yy, -ry-fullness*math.exp(-((z-1.56)/.09)**2))
                zz=z
                if j>=5:
                    neckline = (.095 if classic else .125)*front**3
                    zz -= neckline if j==6 else neckline*.5
                    if not classic:
                        zz+=.022*front*math.exp(-((abs(xx)-.42)/.25)**2)
                vertices.append((rx*xx,yy,zz))
        faces=[(j*n+i,j*n+(i+1)%n,(j+1)*n+(i+1)%n,(j+1)*n+i)
               for j in range(len(rings)-1) for i in range(n)]
        mesh=bpy.data.meshes.new("Tailored bodice")
        mesh.from_pydata(vertices,[],faces)
        obj=bpy.data.objects.new("Shaped steel cuirass" if classic else "Violet sweetheart bodice",mesh)
        bpy.context.collection.objects.link(obj)
        finish(obj,obj.name,metal if classic else cloth,"")
        solid=obj.modifiers.new("Garment thickness","SOLIDIFY");solid.thickness=.013
        curve=bpy.data.curves.new("Neckline trim","CURVE")
        curve.dimensions="3D";curve.bevel_depth=.007;curve.bevel_resolution=3
        spline=curve.splines.new("POLY");spline.points.add(n-1)
        for point,(x,y,z) in zip(spline.points,vertices[-n:]):
            point.co=(x*1.012,y*1.015,z,1)
        spline.use_cyclic_u=True
        trim=bpy.data.objects.new("Gold neckline piping",curve)
        bpy.context.collection.objects.link(trim);trim.parent=root;curve.materials.append(gold)
    fitted_bodice()

    surface("Fitted hip panel", [(0,0,z,rx,ry) for z,rx,ry in [
        (1.02,hip*.91,.148),(1.09,hip,.172),(1.16,hip*.93,.161),
        (1.24,.197*w,.141),(1.29,.169*w,.125)]], navy)
    surface("Waist belt", [(0,0,1.23,.199*w,.147),(0,0,1.25,.191*w,.146),
        (0,0,1.28,.180*w,.138)], leather)
    uv("Belt clasp", (0,-.148,1.255), (.042,.021,.028), gold)
    for side in (-1,1):
        if not classic:
            panel=uv("Split violet overskirt",(.116*side,-.110,1.104),(.086,.049,.126),cloth)
            panel.rotation_euler.y=-.23*side
        strap=uv("Fitted shoulder strap",(.159*w*side,-.074,1.706),(.034,.046,.091),metal if classic else cloth)
        strap.rotation_euler.y=.18*side
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
        surface("Fitted boot", [
            (leg_x,-.050,.035,.059,.103),(leg_x,-.050,.045,.069,.121),
            (leg_x,-.045,.081,.069,.120),(leg_x,-.021,.114,.062,.092),
            (leg_x,.015,.151,.052,.059),(leg_x,.016,.216,.052,.054),
            (leg_x,.016,.244,.054,.057)], leather, leg+"_boot")
        uv("Boot cuff", (leg_x,.014,.211), (.058,.060,.033), metal if classic else cloth, leg+"_boot")
        if classic:
            uv("Small knee plate", (leg_x,-.072,.556), (.062,.028,.066), metal, leg)
        else:
            uv("Long side hair", (.114*side,.059,1.94), (.037,.051,.176), hair)
    if classic:
        uv("Short ranger cape", (0,.136,1.465), (.225,.039,.341), cloth)
        uv("Tied hair bun", (0,.145,2.129), (.086,.077,.073), highlight)
        for i in range(5):
            lock = uv("Swept hair lock", (-.103+i*.041,-.049,2.142+i*.007),
                      (.045,.071,.055), hair if i%2 else highlight)
            lock.rotation_euler.y = -.43
    else:
        uv("Sash knot", (.121,-.146,1.26), (.055,.027,.045), gold)
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
# Match Free Roam: screen X is world X + Y and screen Y is world X - Y.
# The ratio of the diamond half-height to its half-width gives the tilt.
ground_ratio = 35.83905 / 70.0036
tilt = math.asin(ground_ratio)
bpy.ops.object.camera_add(location=(5,-5,1.13 + math.sqrt(50)*math.tan(tilt)))
camera=bpy.context.object
direction=Vector((0,0,1.13))-camera.location
camera.rotation_euler=direction.to_track_quat("-Z","Y").to_euler()
camera.data.type="ORTHO";camera.data.ortho_scale=2.69
# Match the renderer's ground anchor at row 160 in each 192-pixel cell.
camera.data.shift_y = (160-96)/192 - (1.13-.037)*math.cos(tilt)/2.69
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
for obj in list(root.children):
    if obj.type == "CURVE":
        bpy.ops.object.select_all(action="DESELECT")
        obj.select_set(True)
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.convert(target="MESH")
bpy.context.view_layer.update()
meshes = []
for obj in bpy.data.objects:
    limb = obj.get("limb")
    if obj.type == "MESH" and obj.parent == root:
        transform = obj.matrix_local.copy()
        meshes.append((obj, limb or "", [transform @ v.co for v in obj.data.vertices],
                       transform.inverted()))

def unit(value):
    return min(1.0, max(0.0, value))

def walk_foot(phase):
    """Return fore-aft offset and lift using the game's stance/swing convention."""
    cycle = (phase / math.tau) % 1.0
    stance = .64
    if cycle < stance:
        return .5 - cycle/stance, 0.0
    t = (cycle - stance)/(1 - stance)
    return -.5 + t*t*(3 - 2*t), math.sin(math.pi*t)**2


def knee_position(foot_y, foot_z):
    """Two fixed-length segments from hip to ankle, bending forward along -Y."""
    hip_z, ankle_z = 1.04, .22 + foot_z
    dy, dz = foot_y, ankle_z - hip_z
    distance = math.hypot(dy, dz)
    upper, lower = .432, .419
    along = (upper**2 - lower**2 + distance**2)/(2*distance)
    bend = math.sqrt(max(0.0, upper**2 - along**2))
    return (dy*along/distance + dz*bend/distance,
            hip_z + dz*along/distance - dy*bend/distance)


def punch_motion(phase):
    # Charge, extension, return. Impact occurs at normalized time .5.
    if phase < .25:
        return -math.sin(phase/.25*math.pi/2)
    if phase < .5:
        return -math.cos((phase-.25)/.25*math.pi)
    return math.cos((phase-.5)*math.pi)


def pose(mode, phase):
    strike = punch_motion(phase) if mode == "punch" else 0
    for obj, limb, neutral, inverse in meshes:
        left = "left" in limb
        swing = phase + (math.pi if left else 0.0)
        along, lift = walk_foot(swing) if mode == "walk" else (0.0, 0.0)
        foot_y, foot_z = -.33*along, .125*lift
        knee_y, knee_z = knee_position(foot_y, foot_z)
        resting_y, resting_z = knee_position(0, 0)
        for vertex, base in zip(obj.data.vertices, neutral):
            point = base.copy()
            if mode == "walk" and limb.startswith("leg"):
                if limb.endswith("boot"):
                    point.y += foot_y
                    point.z += foot_z
                elif base.z >= .56:
                    weight = unit((1.04-base.z)/.48)
                    point.y += (knee_y-resting_y)*weight
                    point.z += (knee_z-resting_z)*weight
                else:
                    weight = unit((base.z-.22)/.34)
                    point.y += foot_y*(1-weight)+(knee_y-resting_y)*weight
                    point.z += foot_z*(1-weight)+(knee_z-resting_z)*weight
            elif mode == "walk" and limb.startswith("arm"):
                point.y += .19*along*unit((1.69-base.z)/.64)
            elif mode == "punch":
                arm_weight = unit((1.69-base.z)/.64)
                if limb == "arm_right":
                    reach = max(0.0,strike)
                    point.y -= (.61*reach + .14*min(0.0,strike))*arm_weight
                    point.z += (.52*reach + .16*abs(strike))*arm_weight
                    point.x -= .13*reach*arm_weight
                elif limb == "arm_left":
                    point.y -= .22*abs(strike)*arm_weight
                    point.z += .36*abs(strike)*arm_weight
                body_weight = unit((base.z-1.05)/.47)
                turn = -.34*strike*body_weight
                xx, yy = point.x, point.y
                point.x = math.cos(turn)*xx - math.sin(turn)*yy
                point.y = math.sin(turn)*xx + math.cos(turn)*yy - .065*max(0.0,strike)*body_weight
                point.z -= .046*abs(strike)*unit(base.z/.92)
            vertex.co = inverse @ point
        obj.data.update()
    if variant == "dagger_goblin":
        dagger = bpy.data.objects["Drawn dagger"]
        reach = max(0.0,strike)
        hand = Vector((.329-.13*reach,-.072-.61*reach-.14*min(0.0,strike),
                       1.05+.52*reach+.16*abs(strike)-.046*abs(strike)))
        dagger.location = hand
        dagger.rotation_euler.x = -1.08*reach

# The editable source contains the neutral continuous-mesh figure.
bpy.ops.wm.save_as_mainfile(filepath=os.path.abspath(os.path.join(out,variant+".blend")))
# Row zero faces world -Y. Rotation follows N, NE, E, SE, S, SW, W, NW.
# Render the actual opposite views to preserve anatomy, lighting and punch handedness.
for facing in range(8):
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
    for frame in range(5):
        pose("punch",frame/4)
        root.location.z=0
        scene.render.filepath=os.path.abspath(os.path.join(out,"punch_f%d_%d.png"%(facing,frame)))
        bpy.ops.render.render(write_still=True)
print("BLENDER_CHARACTER_IDLE_WALK_KNEE_PUNCH_OK",out)
