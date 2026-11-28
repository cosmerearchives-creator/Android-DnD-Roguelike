extends SceneTree
func _initialize(): call_deferred("run")
func run():
	var lab=load("res://humanoid_animation_lab.tscn").instantiate()
	root.add_child(lab)
	await process_frame
	lab._load_target(lab.TARGETS.size()-1)
	var rig=lab.model.find_children("*","Skeleton3D",true,false)[0]
	var garment=lab.model.find_child("WearableLeather",true,false)
	var coverage=lab.model.find_child("BootCoverage",true,false)
	for clip in ["Walking","Running"]:
		lab._play_clip(lab._index_of_clip(clip))
		lab.shared_player.advance(.2)
		var t=lab.shared_player.current_animation_position
		for enabled in [false,true]:
			lab.equip_button.button_pressed=enabled
			if garment.visible!=enabled or coverage.visible==enabled or garment.get_node(garment.skeleton)!=rig or rig.get_bone_count()!=66 or lab.shared_player.current_animation!=clip or lab.shared_player.current_animation_position!=t:
				push_error("Wearable toggle failed: "+clip)
				quit(1)
				return
	lab._load_target(0)
	if lab.equip_button.visible or lab.clip_picker.item_count<=2:
		quit(1)
		return
	print("LEATHER_TRIAL_PASS")
	quit()
