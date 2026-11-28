extends SceneTree
const APPEARANCE=preload("res://character_appearance.gd")
func _initialize():call_deferred("_run")
func _run():
	var pack:=load("res://assets/female_no_race_base.glb") as PackedScene
	if pack==null:quit(1);return
	var character:=pack.instantiate() as Node3D
	root.add_child(character)
	await process_frame
	var skeleton:Skeleton3D=APPEARANCE._skeleton(character)
	assert(skeleton!=null)
	assert(skeleton.get_bone_count()==66)
	var bones:PackedStringArray=PackedStringArray()
	var parents:PackedInt32Array=PackedInt32Array()
	for i in skeleton.get_bone_count():
		bones.append(skeleton.get_bone_name(i))
		parents.append(skeleton.get_bone_parent(i))
	var idx:int=APPEARANCE._head_bone(skeleton)
	assert(idx==5)
	assert(skeleton.get_bone_name(idx)=="mixamorig_Head")
	for h in range(1,APPEARANCE.HAIR.size()):
		var data:Dictionary={"hair":h,"beard":1,"hair_scale":1.0,"hair_offset":Vector3.ZERO,"beard_offset":Vector3.ZERO}
		APPEARANCE.apply(character,data)
		await process_frame
		var mount:=character.find_child("ModularAppearance",true,false) as BoneAttachment3D
		assert(mount!=null and mount.bone_idx==idx)
		assert(mount.get_child_count()==2)
		var hair:=mount.get_node("Hair") as Node3D
		var beard:=mount.get_node("Beard") as Node3D
		var hair_factor:float=APPEARANCE.mesh_factor("hair",h,data)
		var beard_factor:float=APPEARANCE.mesh_factor("beard",1,data)
		assert(hair.position.distance_to(APPEARANCE.base_position("hair",h,hair_factor))<0.001)
		assert(beard.position.distance_to(APPEARANCE.base_position("beard",1,beard_factor))<0.001)
		var hair_top:float=hair.position.y+float(APPEARANCE.MESH_TOP["hair"][h])*hair.scale.y
		var beard_top:float=beard.position.y+float(APPEARANCE.MESH_TOP["beard"][1])*beard.scale.y
		assert(absf(hair_top-APPEARANCE.SCALP_TOP)<0.001)
		assert(absf(beard_top-APPEARANCE.BEARD_TOP)<0.001)
		data["hair_offset"]=Vector3(0.03,-0.02,0.04)
		data["beard_offset"]=Vector3(-0.015,0.01,-0.02)
		APPEARANCE.update_placement(character,data)
		assert(hair.position.distance_to(APPEARANCE.base_position("hair",h,hair_factor)+data["hair_offset"])<0.001)
		assert(beard.position.distance_to(APPEARANCE.base_position("beard",1,beard_factor)+data["beard_offset"])<0.001)
		var old_head_position:Vector3=mount.global_position
		var old_child_position:Vector3=hair.global_position
		skeleton.set_bone_pose_rotation(idx,Quaternion(Vector3.UP,0.24))
		await process_frame
		# Bone rotation must affect the attachment and mesh, not mutate skeleton source.
		assert(hair.global_position.distance_to(old_child_position)>0.001)
		skeleton.reset_bone_pose(idx)
		await process_frame
		print("HEAD_ATTACHMENT_OK ",APPEARANCE.HAIR[h]," head=",idx," crown=",hair_top," beardtop=",beard_top)
		for i in skeleton.get_bone_count():
			assert(bones[i]==skeleton.get_bone_name(i))
			assert(parents[i]==skeleton.get_bone_parent(i))
	print("HAIR_BEARD_FIT_SMOKE_OK all three hair variants and beard, 66 original joints preserved")
	quit(0)
