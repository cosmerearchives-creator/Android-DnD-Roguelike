from pathlib import Path
p=Path(__file__).resolve().parents[1]
f=p/'godot/humanoid_shared_animations.gd'
s=f.read_text(encoding='utf-8')
# Disable direct early return, preserving base original animation tracks. Shared claw
# is separately built via _create_claw_attack and appended as an additive clip.
needle='        direct.set_meta("mapped_bones", destination.get_bone_count())'
insert='''        var direct_claw := _create_claw_attack(destination, model.get_path_to(destination), in_place)
        if direct_claw != null:
            var direct_library := direct.get_animation_library("").duplicate() as AnimationLibrary
            direct_library.add_animation(CLAW_NAME, direct_claw)
            direct.remove_animation_library("")
            direct.add_animation_library("", direct_library)
'''
assert needle in s
s=s.replace(needle,insert+needle)
start=s.index('    # Additional canonical humanoid attack')
end=s.index('    # Model-specific native reference clips',start)
body=s[start:end]
# Extract and return clip in method, reusing exactly the canonical conversion.
prefix='''static func _create_claw_attack(destination: Skeleton3D, dest_path: NodePath, in_place: bool) -> Animation:
    var dest_skeleton_path := String(dest_path)
    var library := AnimationLibrary.new()
'''
# Replace final library.add with return, ensuring null if unavailable.
body=body.replace('    claw_instance.free()','    claw_instance.free()\n    if library.has_animation(CLAW_NAME):\n        return library.get_animation(CLAW_NAME)\n    return null')
indented='\n'.join('    '+line if line else '' for line in body.splitlines())
# method actual body needs 4 space indentation not 8; original 4-space body valid as-is.
method=prefix+body
# original body comment references variable but no origin source dependency
s=s[:start]+'''    var claw_clip := _create_claw_attack(destination, NodePath(dest_skeleton_path), in_place)
    if claw_clip != null:
        library.add_animation(CLAW_NAME, claw_clip)
'''+s[end:]
s+='\n'+method
f.write_text(s,encoding='utf-8')
