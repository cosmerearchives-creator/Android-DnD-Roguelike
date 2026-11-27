from pathlib import Path
p=Path(__file__).resolve().parents[1]
f=p/'godot/humanoid_shared_animations.gd'
s=f.read_text()
s=s.replace('var candidates: Array = ALIASES.get(source_name, [source_name])','var candidates: Array = [source_name]\n    candidates.append_array(ALIASES.get(source_name, []))')
# Map custom hand roots and finger chains using anatomical ordering of the reference rig.
needle='    var candidates: Array = [source_name]'
profile={'LeftHand':'Bone_018','RightHand':'Bone_023'}
for side,chains in [('Left',{'Thumb':31,'Index':43,'Middle':39,'Ring':35,'Pinky':47}),('Right',{'Thumb':69,'Index':65,'Middle':61,'Ring':53,'Pinky':57})]:
 for finger,start in chains.items():
  for i in range(4): profile[f'{side}Hand{finger}{i+1}']=f'Bone_{start-i:03}'
import json
s=s.replace(needle,'    if destination.find_bone("Bone_018") >= 0 and destination.find_bone("Bone_023") >= 0:\n        var custom: Dictionary = '+json.dumps(profile)+'\n        if custom.has(source_name):\n            return destination.find_bone(custom[source_name])\n'+needle)
# Retain shared versions for direct comparison, even where native clips exist.
s=s.replace('library.remove_animation(name)','library.rename_animation(name, "Shared_" + name)')
# Require the six previously omitted joints and both custom hands.
needle='    if mappings.size() < 8:'
s=s.replace(needle,'''    if destination.find_bone("Bone_018") >= 0:
        for required in ["LeftForeArm", "RightForeArm", "LeftUpLeg", "RightUpLeg", "LeftLeg", "RightLeg", "LeftHand", "RightHand"]:
            var found := false
            for mapped_name in mappings:
                if _source_bone_name(mapped_name) == required:
                    found = true
                    break
            if not found:
                push_error("Missing required goblin bone: " + required)
                source_instance.free()
                return null
        if mappings.size() < 60:
            push_error("Incomplete goblin mapping")
            source_instance.free()
            return null
'''+needle)
f.write_text(s)
