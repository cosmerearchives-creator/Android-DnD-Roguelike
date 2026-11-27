from pathlib import Path
import shutil,hashlib
p=Path(__file__).resolve().parents[1]
a=p/'godot/assets'
src=p.parent/'Master Asset Library/Creature Sources'
for n,f in [('human_adventurer.glb','Meshy_AI_Female_Character_Model_20261127_animated_mixamo_All_animation.glb'),('goblin_warrior.glb','Meshy_AI_goblin_warrior_character_20261127_animated_mixamo_Axe_Spin_Attack.glb')]:
 shutil.copyfile(src/f,a/n)
 assert hashlib.sha256((src/f).read_bytes()).digest()==hashlib.sha256((a/n).read_bytes()).digest()
 print(n,(a/n).stat().st_size)
f=p/'godot/humanoid_shared_animations.gd'
t=f.read_text()
needle='    var source_instance := SOURCE.instantiate()'
insert='''    # The canonical human already contains the original animation tracks.
    # Copy the native library and preserve its root path, keys and track types.
    var native := find_animator(model)
    if native != null and native.has_animation("Walking") and native.has_animation("Crossbow"):
        var direct := AnimationPlayer.new()
        direct.name = "SharedHumanoidAnimations"
        model.add_child(direct)
        direct.root_node = direct.get_path_to(native.get_node(native.root_node))
        for lib_name in native.get_animation_library_list():
            direct.add_animation_library(lib_name, native.get_animation_library(lib_name))
        var idle_library := AnimationLibrary.new()
        var idle := Animation.new()
        idle.length = 1.5
        idle.loop_mode = Animation.LOOP_LINEAR
        var walk := native.get_animation("Walking")
        for i in walk.get_track_count():
            if walk.track_get_key_count(i) == 0: continue
            var idx := idle.add_track(walk.track_get_type(i))
            idle.track_set_path(idx, walk.track_get_path(i))
            idle.track_insert_key(idx, 0.0, walk.track_get_key_value(i, 0))
        # Keep native library untouched by adding the synthetic idle to a copy.
        var base := direct.get_animation_library("").duplicate() as AnimationLibrary
        base.add_animation("Idle", idle)
        direct.remove_animation_library("")
        direct.add_animation_library("", base)
        direct.set_meta("mapped_bones", destination.get_bone_count())
        direct.set_meta("native_reference", true)
        native.stop()
        return direct
'''
assert needle in t
f.write_text(t.replace(needle,insert+needle,1))
f=p/'scripts/verify_lean_cast.py'
t=f.read_text().replace('len(human.get("animations", [])) == 0','len(human.get("animations", [])) == 20').replace('assert asset_total < 45_000_000, f"Asset directory exceeds 45 MB: {asset_total}"','import hashlib\nassert hashlib.sha256((assets/"human_adventurer.glb").read_bytes()).hexdigest() == "119496a7536e41fca167b8f1ce6162883cc37bf8d4db775b4322eb59362aac68"\nassert hashlib.sha256((assets/"goblin_warrior.glb").read_bytes()).hexdigest() == "2080e255abfac5d7a87b9f3a8cba31d5e9d83c4ebd58a0f28a2845b26ac039a2"')
f.write_text(t)
f=p/'godot/humanoid_animation_lab.gd'
t=f.read_text().replace('All motions play in place','Human uses original motion')
f.write_text(t)
