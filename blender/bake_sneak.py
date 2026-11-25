"""Bake the active world avatar's sneak poses and customization masks."""
import math
import sys
from pathlib import Path

from PIL import Image

source = Path(__file__).with_name("create_character.py")
setup = source.read_text().split("# The editable source contains", 1)[0]
exec(compile(setup, str(source), "exec"))

output = Path(out)
assets = Path("app/src/main/assets")
(assets / "petite_modules").mkdir(parents=True, exist_ok=True)
(assets / "blender_world").mkdir(parents=True, exist_ok=True)

colors = {mat: tuple(mat.diffuse_color) for mat in bpy.data.materials}
shading = scene.display.shading
ears = [(obj, obj.location.copy(), obj.scale.copy()) for obj in root.children
        if obj.name.startswith("Pointed ear")]

def crouch(phase, walking):
    for obj, limb, neutral, inverse in meshes:
        shift = math.pi if "left" in limb else 0
        along, lift = walk_foot(phase + shift) if walking else (0, 0)
        foot_y, foot_z = -.22 * along, .070 * lift
        hip_z = .87 + .022 * math.cos(phase * 2)
        hip_y = .035
        # Solve the original leg lengths with the hip lowered and the foot planted.
        dy, dz = foot_y - hip_y, .22 + foot_z - hip_z
        distance = math.hypot(dy, dz)
        upper, lower = .432, .419
        axis = (upper**2 - lower**2 + distance**2) / (2 * distance)
        bend = math.sqrt(max(0, upper**2 - axis**2))
        knee_y = hip_y + (dy * axis + dz * bend) / distance
        knee_z = hip_z + (dz * axis - dy * bend) / distance
        old_y, old_z = knee_position(0, 0)
        for vertex, base in zip(obj.data.vertices, neutral):
            point = base.copy()
            if limb.endswith("boot"):
                point.y += foot_y
                point.z += foot_z
            elif limb.startswith("leg"):
                if base.z >= .56:
                    knee = unit((1.04 - base.z) / .48)
                    point.y += hip_y * (1 - knee) + (knee_y - old_y) * knee
                    point.z += (hip_z - 1.04) * (1 - knee) + (knee_z - old_z) * knee
                else:
                    knee = unit((base.z - .22) / .34)
                    point.y += foot_y * (1 - knee) + (knee_y - old_y) * knee
                    point.z += foot_z * (1 - knee) + (knee_z - old_z) * knee
            else:
                # Rotate around the hip. Head and costume keep their dimensions.
                tilt = .27 + .035 * math.sin(phase)
                height = base.z - 1.04
                point.y = hip_y + math.cos(tilt) * base.y - math.sin(tilt) * height
                point.z = hip_z + math.sin(tilt) * base.y + math.cos(tilt) * height
                if limb.startswith("arm"):
                    weight = unit((1.67 - base.z) / .62)
                    point.y -= (.13 - .10 * along) * weight
                    point.z += .12 * weight
            vertex.co = inverse @ point
        obj.data.update()

def render(name, mode):
    if mode == "beauty":
        shading.light = "STUDIO"
        shading.show_cavity = True
        scene.view_settings.view_transform = "Standard"
        for mat, color in colors.items():
            mat.diffuse_color = color
    else:
        shading.light = "FLAT"
        shading.show_cavity = False
        scene.view_settings.view_transform = "Standard"
        for mat in colors:
            mat.diffuse_color = ((1, 0, 0, 1) if mat == skin else
                                 (0, 1, 0, 1) if mat in (hair, highlight) else (0, 0, 0, 1))
    for obj, location, scale in ears:
        obj.location.x = location.x * (1.18 if mode == "pointed" else .88)
        obj.scale.x = scale.x * (1.42 if mode == "pointed" else .70)
    scene.render.filepath = str((output / (name + ".png")).resolve())
    bpy.ops.render.render(write_still=True)
    with Image.open(scene.render.filepath) as image:
        return image.convert("RGBA")

for walking, frames in ((False, 1), (True, 8)):
    name = "sneak_walk" if walking else "sneak_idle"
    atlas = Image.new("RGBA", (168 * frames, 192 * 8))
    masks = Image.new("RGB", atlas.size)
    for facing in range(8):
        root.rotation_euler.z = facing * math.pi / 4
        for frame in range(frames):
            crouch(frame * math.tau / frames, walking)
            anchor = (168 * frame, 192 * facing)
            image = render("beauty", "beauty")
            control = render("control", "control")
            pointed = render("pointed", "pointed")
            assert image.getchannel("A").getbbox(), (name, facing, frame)
            atlas.paste(image, anchor)
            mask = Image.new("RGB", image.size)
            pixels = []
            for (red, green, _, alpha), (point, _, _, point_alpha) in zip(control.getdata(), pointed.getdata()):
                skin_weight = red * alpha // 255
                pixels.append((skin_weight, green * alpha // 255,
                               max(0, point * point_alpha // 255 - skin_weight)))
            mask.putdata(pixels)
            masks.paste(mask, anchor)
    atlas.save(assets / "blender_world" / (name + ".png"))
    masks.save(assets / "petite_modules" / (name + ".png"))
