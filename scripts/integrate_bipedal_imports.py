from pathlib import Path
import json,struct,hashlib,shutil
p=Path(__file__).resolve().parents[1]
root=p.parent
incoming=root/'Beastiary Imports'
assets=p/'godot/assets'
items={'Male Human.glb':'male_human.glb','Twig Blight.glb':'twig_blight.glb'}
def read_glb(f):
 b=f.read_bytes()
 assert b[:4]==b'glTF'
 length=struct.unpack_from('<I',b,12)[0]
 return json.loads(b[20:20+length])
for original,production in items.items():
 f=incoming/original
 assert f.is_file(),f
 hash=hashlib.sha256(f.read_bytes()).hexdigest()
 for folder in ['Master Asset Library/Creature Sources','Beastiary Review']:
  output=root/folder/original
  output.parent.mkdir(parents=True,exist_ok=True)
  if not output.exists():shutil.copyfile(f,output)
  assert hashlib.sha256(output.read_bytes()).hexdigest()==hash
 shutil.copyfile(f,assets/production)
 assert hashlib.sha256((assets/production).read_bytes()).hexdigest()==hash
 info=read_glb(f)
 print('IMPORTED',original,production,'sha256',hash,'bones',len(info['skins'][0]['joints']),'animations',[a['name'] for a in info['animations']])
 f.unlink()
def edit(path,old,new):
 f=p/path
 t=f.read_text(encoding='utf-8')
 assert old in t,(path,old)
 f.write_text(t.replace(old,new),encoding='utf-8')
edit('godot/front_menu.gd','const MODELS = ["human_adventurer"]','const MODELS = ["human_adventurer", "male_human"]')
edit('godot/front_menu.gd','const NAMES = ["Human Adventurer"]','const NAMES = ["Human Adventurer", "Male Human"]')
edit('godot/world.gd','const MODELS = ["human_adventurer"]','const MODELS = ["human_adventurer", "male_human"]')
edit('godot/world.gd','const MODEL_NAMES = ["Human Adventurer"]','const MODEL_NAMES = ["Human Adventurer", "Male Human"]')
edit('godot/inventory_screen.gd','const PROFILE := ["human_adventurer"]','const PROFILE := ["human_adventurer", "male_human"]')
edit('godot/humanoid_animation_lab.gd','{"name": "Goblin Warrior (Mixamo)", "asset": "res://assets/goblin_warrior.glb"}','{"name": "Goblin Warrior (Mixamo)", "asset": "res://assets/goblin_warrior.glb"},\n    {"name": "Male Human", "asset": "res://assets/male_human.glb"},\n    {"name": "Twig Blight", "asset": "res://assets/twig_blight.glb"}')
edit('scripts/verify_lean_cast.py','"human_adventurer.glb", "goblin_warrior.glb", "fire_beetle.glb",','"human_adventurer.glb", "goblin_warrior.glb", "fire_beetle.glb",\n    "male_human.glb", "twig_blight.glb",')
edit('scripts/verify_lean_cast.py','assert len(shared.get("animations", [])) == 20','assert len(shared.get("animations", [])) == 20\nassert len(gltf_info("male_human.glb").get("skins", [])) == 1\nassert len(gltf_info("twig_blight.glb").get("skins", [])) == 1\nassert "Shield_Push_Left" in [a["name"] for a in gltf_info("twig_blight.glb")["animations"]]')
edit('godot/enemy_spawner.gd','const BEETLE = "res://assets/fire_beetle.glb"','const BEETLE = "res://assets/fire_beetle.glb"\nconst TWIG_BLIGHT = "res://assets/twig_blight.glb"')
edit('godot/enemy_spawner.gd','if rng.randf() < 0.23:\n\t\t_spawn(BEETLE, "Fire Beetle")\n\telse:', 'var roll := rng.randf()\n\tif roll < 0.23:\n\t\t_spawn(BEETLE, "Fire Beetle")\n\telif roll < 0.43:\n\t\t_spawn(TWIG_BLIGHT, "Twig Blight")\n\telse:')
edit('godot/enemy_spawner.gd','var desired_height := 0.52 if kind == "Fire Beetle" else 1.22','var desired_height := 0.52 if kind == "Fire Beetle" else 1.15 if kind == "Twig Blight" else 1.22')
edit('godot/enemy_spawner.gd','if defeated_name != "Fire Beetle":','if defeated_name == "Goblin Warrior":')
# Native clip stays on Twig Blight, universal shared clip added to each shared humanoid rig from the native Twig animation.
f=p/'godot/humanoid_shared_animations.gd'
t=f.read_text(encoding='utf-8')
t=t.replace('const SOURCE := preload("res://assets/humanoid_mixamo_animation_library.glb")','const SOURCE := preload("res://assets/humanoid_mixamo_animation_library.glb")\nconst CLAW_SOURCE := preload("res://assets/twig_blight.glb")\nconst CLAW_NAME := "Attack_Claw_01_BackSwipe"')
# External claw retarget helper: separate source without changing the canonical base source. Use canonical bone mapping for both paths.
needle='    # Model-specific native reference clips override unvalidated retargets.'
insert='''    # Additional canonical humanoid attack, sourced from the Twig Blight's
    # original Shield_Push_Left. It is reusable on any compatible humanoid rig.
    var claw_instance := CLAW_SOURCE.instantiate()
    var claw_skeleton := find_skeleton(claw_instance)
    var claw_player := find_animator(claw_instance)
    if claw_skeleton != null and claw_player != null and claw_player.has_animation("Shield_Push_Left"):
        var claw_native := claw_player.get_animation("Shield_Push_Left")
        var claw_clip := Animation.new()
        claw_clip.length = claw_native.length
        claw_clip.loop_mode = Animation.LOOP_NONE
        claw_clip.step = claw_native.step
        for track_idx in claw_native.get_track_count():
            var kind := claw_native.track_get_type(track_idx)
            if kind != Animation.TYPE_ROTATION_3D and kind != Animation.TYPE_POSITION_3D:
                continue
            var track_path := claw_native.track_get_path(track_idx)
            if track_path.get_subname_count() == 0:
                continue
            var source_name := String(track_path.get_subname(0))
            var canonical := _source_bone_name(source_name)
            var src_bone := claw_skeleton.find_bone(source_name)
            var dst_bone := _target_bone(canonical, destination)
            if src_bone < 0 or dst_bone < 0 or (kind == Animation.TYPE_POSITION_3D and canonical != "Hips"):
                continue
            var converted_idx := claw_clip.add_track(kind)
            claw_clip.track_set_path(converted_idx, NodePath(dest_skeleton_path + ":" + destination.get_bone_name(dst_bone)))
            claw_clip.track_set_interpolation_type(converted_idx, claw_native.track_get_interpolation_type(track_idx))
            var src_local := claw_skeleton.get_bone_rest(src_bone)
            var dst_local := destination.get_bone_rest(dst_bone)
            var src_global_q := claw_skeleton.get_bone_global_rest(src_bone).basis.get_rotation_quaternion().normalized()
            var dst_global_q := destination.get_bone_global_rest(dst_bone).basis.get_rotation_quaternion().normalized()
            var src_rest_q := src_local.basis.get_rotation_quaternion().normalized()
            var dst_rest_q := dst_local.basis.get_rotation_quaternion().normalized()
            var claw_scale_ratio := clampf(_rest_height(destination) / _rest_height(claw_skeleton), 0.3, 3.0)
            for key_idx in claw_native.track_get_key_count(track_idx):
                var value: Variant = claw_native.track_get_key_value(track_idx, key_idx)
                if kind == Animation.TYPE_ROTATION_3D:
                    var relative: Quaternion = src_rest_q.inverse() * (value as Quaternion)
                    var world_delta: Quaternion = src_global_q * relative * src_global_q.inverse()
                    var local_delta: Quaternion = dst_global_q.inverse() * world_delta * dst_global_q
                    value = (dst_rest_q * local_delta).normalized()
                else:
                    var offset: Vector3 = ((value as Vector3) - src_local.origin) * claw_scale_ratio
                    if in_place:
                        offset.x = 0.0
                        offset.z = 0.0
                    value = dst_local.origin + offset
                claw_clip.track_insert_key(converted_idx, claw_native.track_get_key_time(track_idx, key_idx),
                    value, claw_native.track_get_key_transition(track_idx, key_idx))
        library.add_animation(CLAW_NAME, claw_clip)
    claw_instance.free()
'''
assert needle in t
t=t.replace(needle,insert+needle)
# Important: avoid cached library including wrong or missing attack. Cache key needs source invariant but already includes rest and path.
# Native human path uses exact original; append an independent shared attack overlay to native via separate future work if needed.
f.write_text(t,encoding='utf-8')
# Twig and male share clip; twig included as bestiary entry.
f=p/'godot/bestiary.gd'
t=f.read_text(encoding='utf-8')
anchor='const SHARED = preload("res://humanoid_shared_animations.gd")'
twig='''const TWIG_BLIGHT = {
    "name": "Twig Blight", "asset": "res://assets/twig_blight.glb",
    "subtitle": "Small plant, neutral evil • Bipedal",
    "details": [
        ["Size", "Small"], ["Creature type", "Plant"], ["Alignment", "Neutral evil"],
        ["Armor class", "13 (natural armor)"], ["Hit points", "4 (1d6 + 1)"],
        ["Speed", "20 ft."], ["STR / DEX / CON", "6 (-2) / 13 (+1) / 12 (+1)"],
        ["INT / WIS / CHA", "4 (-3) / 8 (-1) / 3 (-4)"],
        ["Senses", "Blindsight 60 ft. (blind beyond this radius), passive Perception 9"],
        ["Languages", "Understands Common but can't speak"], ["Challenge rating", "1/8 (25 XP)"],
        ["Damage vulnerability", "Fire"], ["Condition immunity", "Blinded, deafened"]
    ],
    "abilities": [["False Appearance", "While motionless, indistinguishable from a dead shrub."]],
    "attacks": [["Claws", "Melee Weapon Attack: +3 to hit, reach 5 ft., one target. Hit: 3 (1d4 + 1) piercing damage. Animation: Attack_Claw_01_BackSwipe."]]
}
'''
assert anchor in t
t=t.replace(anchor,twig+anchor)
t=t.replace('[["Goblins",0],["Fire Beetle",1]]','[["Goblins",0],["Fire Beetle",1],["Twig Blight",2]]')
t=t.replace('creature_picker.add_item("Fire Beetle")','creature_picker.add_item("Fire Beetle")\n\tcreature_picker.add_item("Twig Blight")')
t=t.replace('"GOBLIN ATTRIBUTES" if active_creature == "Goblin" else "FIRE BEETLE ATTRIBUTES"','active_creature.to_upper() + " ATTRIBUTES"')
t=t.replace('\telse:\n\t\tvar variant: Dictionary = GOBLINS.ENTRIES[variant_index]','''\telif index == 2:
        if not _load_creature(TWIG_BLIGHT.asset, "Attack_Claw_01_BackSwipe"):
            creature_picker.select(0)
            return
        active_creature = "Twig Blight"
        preview_title.text = TWIG_BLIGHT.name
        preview_subtitle.text = TWIG_BLIGHT.subtitle
        preview_controls.visible = preview_animator != null
        _render_stats(TWIG_BLIGHT)
    else:
        var variant: Dictionary = GOBLINS.ENTRIES[variant_index]''')
t=t.replace('preview_animator = SHARED.attach(instance) if path == GOBLINS.ENTRIES[0].asset else _find_animator(instance)','preview_animator = _find_animator(instance) if path == FIRE_BEETLE.asset else SHARED.attach(instance)')
t=t.replace('if state == "Axe Spin" and local_anim != null', 'if state == "Axe Spin" and local_anim != null')
t=t.replace('for clip in ["Walk", "Attack", "Axe Spin"]:', 'for clip in ["Walk", "Attack", "Axe Spin", "Claw"]:')
t=t.replace('var wanted := "Walking" if state == "Walk" and preview_animator.name == "SharedHumanoidAnimations" else "Running" if state == "Run" else state','var wanted := "Attack_Claw_01_BackSwipe" if state == "Claw" else "Walking" if state == "Walk" and preview_animator.name == "SharedHumanoidAnimations" else "Running" if state == "Run" else state')
f.write_text(t,encoding='utf-8')
# Include claw test and new models in smoke.
f=p/'godot/tests/lean_cast_smoke.gd'
t=f.read_text(encoding='utf-8')
t=t.replace('{"label":"Goblin","path":"res://assets/goblin_warrior.glb","min_bones":18}','{"label":"Goblin","path":"res://assets/goblin_warrior.glb","min_bones":18},\n        {"label":"Male Human","path":"res://assets/male_human.glb","min_bones":20},\n        {"label":"Twig Blight","path":"res://assets/twig_blight.glb","min_bones":20}')
t=t.replace('for clip in ["Idle","Walking","Running","Attack","Push","Crossbow"]:', 'for clip in ["Idle","Walking","Running","Attack","Push","Crossbow","Attack_Claw_01_BackSwipe"]:')
# Female direct native player needs to get claw separately: decide after tests
t=t.replace('world.toggle_inventory()','''world.enemies._spawn("res://assets/twig_blight.glb", "Twig Blight")
    var twig_found := false
    for mob in world.enemies.mobs:
        if mob.kind == "Twig Blight" and is_instance_valid(mob.animator):
            twig_found = mob.animator.has_animation("Attack_Claw_01_BackSwipe")
    if not twig_found:
        _fail("Twig Blight not using shared claw attack")
        return
    world.toggle_inventory()''')
f.write_text(t,encoding='utf-8')
print('EDITED')
