extends SceneTree
func _initialize(): call_deferred("run")
func run():
	var world=load("res://world.tscn").instantiate()
	root.add_child(world)
	await process_frame
	var visual=world.player.get_node("CharacterVisual")
	var animator=visual.get_node("SharedHumanoidAnimations")
	var skeleton=world.SHARED.find_skeleton(visual)
	if skeleton.get_bone_count()!=28:
		printerr("Wrong character rig")
		quit(1)
		return
	for pair in [["walk","Movement_WalkMale_01_Standard"],["run","Running"],["attack","Attack_MeleeTwoHanded_01_OverheadSmash"],["idle","Idle"]]:
		world.play_imported_animation(pair[0])
		if animator.current_animation!=pair[1]:
			printerr("Clip mismatch: ",pair)
			quit(1)
			return
		animator.advance(0.15)
	print("GOBLIN_MOTION_PASS ",animator.get_meta("native_reference_clips"))
	quit()
