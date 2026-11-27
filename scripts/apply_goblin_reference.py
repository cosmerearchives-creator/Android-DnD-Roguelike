from pathlib import Path
import hashlib,shutil
p=Path(__file__).resolve().parents[1]
src=Path.home()/'Downloads/Meshy_AI_goblin_warrior_character_20261127_animated_mixamo_All_animation.glb'
h='29c21a1c5e1e3bdcd49e99e7cafaf63c466dbfec8b029570e8a13d2e06d2bdcc'
assert hashlib.sha256(src.read_bytes()).hexdigest()==h
for folder in ['Master Asset Library/Creature Sources','Beastiary Review']:
 dst=p.parent/folder/src.name
 if not dst.exists(): shutil.copyfile(src,dst)
 assert hashlib.sha256(dst.read_bytes()).hexdigest()==h
shutil.copyfile(src,p/'godot/assets/goblin_warrior.glb')
f=p/'scripts/verify_lean_cast.py'
t=f.read_text().replace('2080e255abfac5d7a87b9f3a8cba31d5e9d83c4ebd58a0f28a2845b26ac039a2',h)
f.write_text(t)
f=p/'godot/humanoid_shared_animations.gd'
t=f.read_text()
t=t.replace('if native != null and native.has_animation("Walking") and native.has_animation("Crossbow"):', 'if native != null and native.has_animation("Walking") and native.has_animation("Crossbow"):')
# Preserve native goblin tracks for matching clips; no speculative finger mapping.
needle='    # Standing pose derived once'
insert='''    # Model-specific native reference clips override unvalidated retargets.
    # Preserve all native tracks and remap only the skeleton node path.
    var reference := find_animator(model)
    if reference != null and reference.has_animation("Axe_Spin_Attack"):
        for name in reference.get_animation_list():
            var exact := reference.get_animation(name).duplicate(true) as Animation
            var reference_root := reference.get_node(reference.root_node)
            for track in exact.get_track_count():
                var path := exact.track_get_path(track)
                var target := reference_root.get_node_or_null(NodePath(String(path.get_concatenated_names())))
                if target != null:
                    var new_path := String(model.get_path_to(target))
                    if path.get_subname_count() > 0:
                        new_path += ":" + String(path.get_concatenated_subnames())
                    exact.track_set_path(track, NodePath(new_path))
            if library.has_animation(name):
                library.remove_animation(name)
            library.add_animation(name, exact)
        player.set_meta("native_reference_clips", reference.get_animation_list())
'''
assert needle in t
t=t.replace(needle,insert+needle)
f.write_text(t)
