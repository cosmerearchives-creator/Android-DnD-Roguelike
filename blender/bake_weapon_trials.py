"""Bake Free Roam weapon test poses and their screen-space grip registrations."""
import json
import math
from pathlib import Path

import bpy
from bpy_extras.object_utils import world_to_camera_view
from mathutils import Vector

source = Path(__file__).with_name("create_character.py")
# Use the exact character geometry, materials and camera used by Free Roam.
namespace = {"__file__": str(source)}
exec(compile(source.read_text().split("# The editable source", 1)[0], str(source), "exec"), namespace)
scene, camera, root = (namespace[k] for k in ("scene", "camera", "root"))
output = Path(namespace["out"])
scene.display.render_aa = "8"
scene.render.resolution_x = 144
scene.render.resolution_y = 164

# Five equally spaced poses. Peak contacts and the bow release occupy pose two.
# Values are wrist displacements from the neutral position, with torso rotation.
motions = {
    "swing": [(0,0,0,0),(.10,.28,.35,.42),(-.56,-.51,.38,-.36),(-.42,-.22,.22,-.19),(0,0,0,0)],
    "thrust": [(0,0,0,0),(-.07,.24,.30,.24),(-.13,-.62,.57,-.28),(-.09,-.30,.27,-.14),(0,0,0,0)],
    "heavy": [(0,0,0,0),(-.27,.15,.91,.30),(-.34,-.60,.48,-.23),(-.41,-.38,.19,-.25),(0,0,0,0)],
    "bow": [(0,0,0,0),(-.10,.21,.58,.18),(-.14,.31,.60,.22),(-.10,.45,.56,.23),(0,0,0,0)],
}
weapon_vectors = {
    "swing": [(0,0,1),(.65,.30,.76),(-.83,-.58,.02),(-.90,-.28,.36),(0,0,1)],
    "thrust": [(0,0,1),(0,-.95,.21),(0,-1,.06),(0,-.95,.17),(0,0,1)],
    "heavy": [(0,0,1),(0,.27,1),(0,-1,-.35),(-.38,-.62,-.65),(0,0,1)],
    "bow": [(0,0,1)]*5,
}
gloves = sorted(
    ((obj["limb"], obj.location.copy()) for obj in root.children if obj.name.startswith("Glove")),
    key=lambda pair: pair[0])
assert [limb for limb,_ in gloves] == ["arm_left","arm_right"]
left_hand, right_hand = (point for _,point in gloves)

def transform(base, limb, mode, i):
    dx, dy, dz, turn = motions[mode][i]
    body = max(0, min(1, (base.z-1.05)/.47))
    angle = turn*body
    x = base.x*math.cos(angle)-base.y*math.sin(angle)
    y = base.x*math.sin(angle)+base.y*math.cos(angle)
    z = base.z
    weight = max(0, min(1, (1.67-base.z)/.633))
    if limb == "arm_right":
        x += dx*weight
        y += dy*weight
        z += dz*weight
    if limb == "arm_left":
        if mode == "heavy":
            x += (dx+.60 if i in (1,2,3) else 0)*weight
            y += (dy+.09 if i in (1,2,3) else 0)*weight
            z += dz*.88*weight
        if mode == "bow" and i in (1,2,3):
            x += .11*weight
            y -= .56*weight
            z += .57*weight
    return Vector((x,y,z))

def pixel(point):
    q=world_to_camera_view(scene,camera,root.matrix_world @ point)
    return [round(q.x*144,2),round((1-q.y)*164,2)]

if namespace["variant"] == "dagger_goblin":
    for part in bpy.data.objects["Drawn dagger"].children:
        part.hide_render = True
    selected_modes = ("swing", "bow")
else:
    selected_modes = tuple(motions)

for mode in selected_modes:
    registered=[]
    for facing in range(8):
        root.rotation_euler.z=facing*math.pi/4
        for i in range(5):
            for obj,limb,neutral,inverse in namespace["meshes"]:
                for vertex,base in zip(obj.data.vertices,neutral):
                    vertex.co=inverse @ transform(base,limb,mode,i)
                obj.data.update()
            bpy.context.view_layer.update()
            limb, rest = ("arm_left",left_hand) if mode=="bow" else ("arm_right",right_hand)
            hand=transform(rest,limb,mode,i)
            grip=pixel(hand)
            tip=pixel(hand+Vector(weapon_vectors[mode][i]))
            degrees=math.degrees(math.atan2(tip[0]-grip[0],grip[1]-tip[1]))
            registered.append([*grip,round(degrees,2)])
            scene.render.filepath=str((output/f"{mode}_{facing}_{i}.png").resolve())
            bpy.ops.render.render(write_still=True)
    (output/f"{mode}.json").write_text(json.dumps(registered))

if namespace["variant"] == "dagger_goblin":
    for facing in range(8):
        root.rotation_euler.z = facing*math.pi/4
        for mode, count in (("idle", 1), ("walk", 8)):
            for i in range(count):
                namespace["pose"](mode, i*math.tau/count)
                scene.render.filepath = str((output/f"{mode}_{facing}_{i}.png").resolve())
                bpy.ops.render.render(write_still=True)
