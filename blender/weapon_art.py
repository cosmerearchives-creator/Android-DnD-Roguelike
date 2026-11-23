"""Original fantasy inventory silhouettes in the X/Z plane."""
import math

import bpy
from mathutils import Vector


def render_weapon(item_id, steel, wood, brass, leather, cloth, cylinder, sphere):
    def line(name, points, width, material, loop=False):
        curve = bpy.data.curves.new(name, "CURVE")
        curve.dimensions = "3D"
        curve.bevel_depth = width
        curve.bevel_resolution = 3
        path = curve.splines.new("POLY")
        path.points.add(len(points) - 1)
        for point, (x, z) in zip(path.points, points):
            point.co = (x, 0, z, 1)
        path.use_cyclic_u = loop
        obj = bpy.data.objects.new(name, curve)
        bpy.context.collection.objects.link(obj)
        curve.materials.append(material)

    def plate(name, outline, material=steel, thickness=.065):
        n = len(outline)
        vertices = [(x, y, z) for y in (-thickness, thickness) for x, z in outline]
        faces = [tuple(range(n)), tuple(reversed(range(n, 2*n)))]
        faces += [(i, n+i, n+(i+1)%n, (i+1)%n) for i in range(n)]
        mesh = bpy.data.meshes.new(name)
        mesh.from_pydata(vertices, [], faces)
        mesh.update()
        obj = bpy.data.objects.new(name, mesh)
        bpy.context.collection.objects.link(obj)
        mesh.materials.append(material)
        bevel = obj.modifiers.new("Rounded edge", "BEVEL")
        bevel.width = .016
        bevel.segments = 2
        obj.modifiers.new("Face shading", "WEIGHTED_NORMAL")

    def shaft(top, radius=.063, bottom=.12):
        cylinder("Haft", (0,0,(top+bottom)/2), radius, top-bottom, wood)
        for z in (bottom+.08, top-.10):
            cylinder("Haft binding", (0,0,z), radius*1.17, .072, brass)

    def sword(profile, guard, pommel=.105):
        start = min(z for _,z in profile)
        cylinder("Wrapped grip", (0,0,(start+.14)/2), .072, start-.14, leather)
        for z in (.18,.26,.34,.42):
            if z < start:
                cylinder("Grip ring", (0,0,z), .076, .018, brass)
        sphere("Pommel", (0,0,.13), (pommel,.082,.095), brass)
        line("Crossguard", guard, .041, brass)
        plate("Blade", profile)

    swords = {
        "dagger": ([(-.10,.48),(.09,.48),(.105,.83),(0,1.42),(-.105,.83)], [(-.19,.49),(.19,.49)]),
        "shortsword": ([(-.13,.49),(.13,.49),(.12,1.32),(0,1.63),(-.12,1.32)], [(-.24,.54),(0,.49),(.24,.54)]),
        "longsword": ([(-.116,.61),(.116,.61),(.09,1.76),(0,2.01),(-.09,1.76)], [(-.34,.68),(0,.61),(.34,.68)]),
        "greatsword": ([(-.18,.73),(.18,.73),(.16,1.85),(0,2.14),(-.16,1.85)], [(-.39,.83),(-.19,.76),(0,.72),(.19,.76),(.39,.83)]),
        "rapier": ([(-.042,.54),(.042,.54),(.029,1.68),(0,2.02),(-.031,1.66)], [(-.24,.58),(0,.54),(.24,.58)]),
        "scimitar": ([(-.094,.53),(.10,.53),(.22,1.03),(.39,1.48),(.53,1.87),(.30,1.74),(.15,1.48),(-.029,1.06)], [(-.27,.59),(0,.53),(.24,.65)]),
    }
    axes = {
        "handaxe": (1.19, .075, [(-.12,1.17),(.23,1.22),(.44,1.40),(.48,1.25),(.45,1.07),(.33,.94),(.16,1.01),(-.12,1.01)]),
        "battleaxe": (1.60, .055, [(-.15,1.59),(.18,1.70),(.59,1.83),(.66,1.62),(.65,1.36),(.53,1.16),(.27,1.27),(-.15,1.33)]),
        "greataxe": (1.75, .075, [(-.16,1.65),(.29,1.85),(.61,2.06),(.73,1.80),(.74,1.55),(.62,1.24),(.34,1.35),(-.16,1.43)]),
        "halberd": (1.83, .058, [(-.44,1.69),(-.13,1.67),(-.07,1.91),(0,2.22),(.095,1.92),(.11,1.75),(.40,1.86),(.53,1.54),(.40,1.34),(.10,1.51),(-.10,1.48)]),
        "war_pick": (1.4, .06, [(-.25,1.59),(.12,1.60),(.35,1.50),(.57,1.29),(.55,1.15),(.34,1.33),(.13,1.41),(-.25,1.38)]),
    }
    if item_id in swords:
        sword(*swords[item_id])
        if item_id == "rapier":
            line("Swept knuckle bow", [(-.20,.55),(-.28,.44),(-.22,.24),(-.07,.14)], .03, brass)
            line("Basket loop", [(-.22,.53),(-.15,.37),(0,.36),(.16,.53)], .025, brass)
    elif item_id in axes:
        top, radius, shape = axes[item_id]
        shaft(top, radius)
        plate("Forged axe head", shape)
        if item_id == "greataxe":
            plate("Opposite crescent", [(-x,z) for x,z in reversed(shape)])
    elif item_id in {"spear","pike","javelin","lance","glaive","trident","dart"}:
        heads = {
            "spear": (1.64,.053,[(-.063,1.55),(-.165,1.72),(0,2.10),(.165,1.72),(.063,1.55)]),
            "pike": (1.92,.045,[(-.051,1.83),(-.092,1.96),(0,2.23),(.092,1.96),(.051,1.83)]),
            "javelin": (1.52,.043,[(-.060,1.47),(-.095,1.62),(.011,1.98),(.080,1.62),(.048,1.47)]),
            "lance": (1.73,.083,[(-.11,.67),(-.12,1.09),(-.075,1.83),(0,2.19),(.083,1.83),(.12,1.09),(.11,.67)]),
            "glaive": (1.51,.062,[(-.068,1.45),(-.12,1.73),(-.25,2.02),(-.16,2.17),(.10,2.04),(.17,1.79),(.069,1.45)]),
            "trident": (1.58,.055,[(-.28,2.10),(-.18,1.95),(-.18,1.73),(-.066,1.70),(-.064,2.01),(0,2.22),(.063,2.01),(.063,1.70),(.18,1.73),(.18,1.95),(.28,2.10),(.30,1.79),(.23,1.60),(.066,1.56),(.06,1.43),(-.06,1.43),(-.066,1.56),(-.23,1.60),(-.30,1.79)]),
            "dart": (1.10,.033,[(-.064,1.04),(0,1.48),(.07,1.04)]),
        }
        top,radius,shape=heads[item_id]
        shaft(top,radius)
        plate("Point",shape)
        if item_id == "dart":
            plate("Flight",[(0,.38),(-.14,.19),(-.13,.66),(0,.55),(.13,.66),(.14,.19)],cloth,.043)
        if item_id == "lance":
            sphere("Flared hand guard",(0,0,.64),(.20,.15,.09),brass)
    elif item_id in {"club","greatclub","quarterstaff","blowgun"}:
        outlines = {
            "club":[(-.055,.14),(-.07,.54),(-.16,1.16),(-.11,1.37),(.09,1.42),(.18,1.24),(.14,.98),(.07,.51),(.052,.14)],
            "greatclub":[(-.083,.14),(-.091,.47),(-.21,1.14),(-.26,1.58),(-.187,1.84),(.18,1.87),(.24,1.67),(.22,1.28),(.13,.57),(.066,.14)],
        }
        if item_id in outlines:
            plate("Carved wood",outlines[item_id],wood,.13 if item_id=="greatclub" else .09)
            for height in ((.42,1.35,1.62) if item_id=="greatclub" else (.31,)):
                cylinder("Bindings",(0,0,height),(.262 if height>1 else .094),.062,leather)
        else:
            shaft(2.0 if item_id=="quarterstaff" else 1.68,.067 if item_id=="quarterstaff" else .041)
            if item_id == "quarterstaff":
                cylinder("Center wrap",(0,0,1.02),.077,.33,leather)
            else:
                cylinder("Mouthpiece",(0,0,.17),.061,.14,brass)
                sphere("Dark muzzle",(0,0,1.68),(.036,.036,.011),leather)
    elif item_id in {"flail","maul","morningstar","light_hammer","mace","warhammer"}:
        top = {"flail":1.30,"maul":1.76,"morningstar":1.47,"light_hammer":1.17,"mace":1.43,"warhammer":1.57}[item_id]
        shaft(top,.07)
        if item_id in {"light_hammer","warhammer","maul"}:
            width = {"light_hammer":.31,"warhammer":.50,"maul":.68}[item_id]
            height = .29 if item_id=="maul" else .21
            plate("Hammer head",[(-width*.55,top-height/2),(.16,top-height/2),(width*.55,top-height*.30),(width*.55,top+height*.35),(.16,top+height/2),(-width*.55,top+height/2)],steel,.13)
            if item_id == "warhammer":
                plate("Beaked back",[(.19,top+.08),(.51,top+.045),(.62,top-.14),(.38,top-.054),(.19,top-.07)])
        elif item_id == "mace":
            sphere("Mace core",(0,0,top),(.12,.11,.25),steel)
            for j in range(6):
                a=math.tau*j/6
                fin=sphere("Raised flange",(.115*math.cos(a),.115*math.sin(a),top),(.077,.041,.20),steel)
                fin.rotation_euler.z=a
        else:
            x,z = (.57,.87) if item_id=="flail" else (0,top)
            if item_id=="flail":
                for j in range(6):
                    bpy.ops.mesh.primitive_torus_add(location=(.08+j*.081,0,top+.035-.051*j),
                        major_segments=16,minor_segments=8,major_radius=.063,minor_radius=.014)
                    obj=bpy.context.object
                    obj.rotation_euler.x=math.pi/2 if j%2==0 else 0
                    obj.data.materials.append(steel)
            sphere("Spiked head",(x,0,z),(.157,.147,.153),steel)
            for j in range(8):
                a=math.tau*j/8
                radial=Vector((math.cos(a),0,math.sin(a)))
                bpy.ops.mesh.primitive_cone_add(vertices=8,radius1=.061,depth=.13,
                    location=Vector((x,0,z))+.182*radial)
                obj=bpy.context.object
                obj.rotation_euler=radial.to_track_quat("Z","Y").to_euler()
                obj.data.materials.append(steel)
    elif item_id in {"longbow","shortbow","hand_crossbow","light_crossbow","heavy_crossbow"}:
        if item_id in {"longbow","shortbow"}:
            height=2 if item_id=="longbow" else 1.58
            limb=[(.15,.14),(-.08,.23),(-.22,.47),(-.27,1.0),(-.22,1.53),(-.08,1.77),(.15,1.86)]
            if item_id=="shortbow":
                limb=[(.20,.12),(.065,.27),(-.21,.45),(-.26,.77),(-.12,1),(.11,1.21),(.24,1.40)]
            else:
                limb=[(x,z*height/2) for x,z in limb]
            line("Laminated bow",limb,.047,wood)
            line("Bowstring",[limb[0],limb[-1]],.012,cloth)
        else:
            length,width={"hand_crossbow":(1.06,.45),"light_crossbow":(1.55,.60),"heavy_crossbow":(1.85,.76)}[item_id]
            stock=[(-.12,.16),(.08,.13),(.068,length),(-.068,length),(-.12,.64)]
            plate("Stock",stock,wood,.09)
            upper=length-.28
            line("Crossbow limbs",[(-width,upper+.05),(-width*.59,upper-.06),(0,upper), (width*.59,upper-.06),(width,upper+.05)],.041,steel if item_id=="heavy_crossbow" else wood)
            line("Drawn string",[(-width,upper+.05),(0,upper-.29),(width,upper+.05)],.013,cloth)
            sphere("Bolt housing",(0,0,upper),(.11,.082,.15),brass)
            if item_id=="heavy_crossbow":
                line("Foot loop",[(-.08,length),(-.135,length+.18),(.135,length+.18),(.08,length)],.036,steel)
            if item_id=="hand_crossbow":
                plate("Pistol grip",[(0,.47),(.25,.14),(.11,.09),(-.11,.39)],leather,.075)
    elif item_id=="sickle":
        shaft(.84,.071)
        plate("Hooked blade",[(-.105,.74),(-.10,.97),(-.029,1.20),(.15,1.43),(.38,1.51),(.60,1.44),(.70,1.26),(.65,1.10),(.56,1.00),(.57,1.20),(.49,1.30),(.33,1.30),(.19,1.22),(.09,1.03),(.08,.74)])
    elif item_id=="whip":
        shaft(.58,.077)
        whip=[(0,.55),(-.10,.79),(-.33,.98),(-.51,1.18),(-.47,1.42),(-.25,1.56),(.07,1.55),(.29,1.38),(.34,1.15),(.24,.99),(.085,1.01),(.038,1.15),(.16,1.24)]
        line("Braided lash",whip,.032,leather)
        line("Whip tip",[whip[-1],(.30,1.23),(.44,1.13)],.014,wood)
    elif item_id=="sling":
        line("Release cord",[(-.75,1.65),(-.38,1.48),(.26,.85),(.41,.65)],.026,wood)
        line("Finger loop",[(-.69,1.5),(-.78,1.73),(-.57,1.66),(-.49,1.52),(-.69,1.5)],.027,leather)
        line("Retention cord",[(-.49,1.52),(-.03,1.39),(.53,.99),(.55,.76)],.026,wood)
        sphere("Leather pouch",(.49,0,.70),(.16,.08,.105),leather)
        sphere("Sling stone",(.49,-.052,.73),(.095,.065,.062),steel)
    elif item_id=="net":
        for j in range(6):
            p=j/5
            line("Net strand",[(-.69+1.31*p,.28+.18*p),(-.58+1.21*p,1.01),(-.48+1.28*p,1.72-.22*p)],.019,wood)
            line("Cross strand",[(-.69+.21*p,.28+1.44*p),(.0,.36+1.32*p),(.62+.18*p,.46+1.04*p)],.019,wood)
        for x,z in [(-.69,.28),(-.48,1.72),(.62,.46),(.80,1.50)]:
            sphere("Net weight",(x,0,z),(.070,.058,.072),steel)
    else:
        raise ValueError(f"No weapon artwork for {item_id}")
