extends SceneTree
## Lightweight integration check for the single-human/single-goblin cast.
const Shared = preload("res://humanoid_shared_animations.gd")
const Goblins = preload("res://goblin_variants.gd")

func _initialize() -> void:
    call_deferred("_run")

func _fail(why: String) -> void:
    printerr("LEAN_CAST_SMOKE_FAILED: " + why)
    quit(1)

func _run() -> void:
    if Goblins.ENTRIES.size() != 1:
        _fail("Only the new Goblin Warrior should be registered")
        return
    for item in [
        {"label":"Human","path":"res://assets/human_adventurer.glb","min_bones":55},
        {"label":"Goblin","path":"res://assets/goblin_warrior.glb","min_bones":18},
        {"label":"Male Human","path":"res://assets/male_human.glb","min_bones":20},
        {"label":"Twig Blight","path":"res://assets/twig_blight.glb","min_bones":20}
    ]:
        var source: PackedScene = load(item.path)
        if source == null:
            _fail("Cannot load " + str(item.path))
            return
        var model: Node3D = source.instantiate()
        get_root().add_child(model)
        var skeleton := Shared.find_skeleton(model)
        var player := Shared.attach(model)
        if skeleton == null or player == null or int(player.get_meta("mapped_bones")) < int(item.min_bones):
            _fail("No compatible humanoid retargeting for " + str(item.label))
            return
        for clip in ["Idle","Walking","Running","Attack","Push","Crossbow","Attack_Claw_01_BackSwipe"]:
            if not player.has_animation(clip):
                _fail(str(item.label) + " lacks " + clip)
                return
            player.play(clip)
            player.advance(0.1)
        if item.label == "Goblin":
            var native: AnimationPlayer = null
            for node in model.find_children("*","AnimationPlayer",true,false):
                if (node as AnimationPlayer).has_animation("Axe_Spin_Attack"):
                    native = node
            if native == null:
                _fail("Goblin lost Axe Spin Attack")
                return
        print("LEAN_CHARACTER_OK ",item.label," mapped=",player.get_meta("mapped_bones"))
        model.queue_free()

    var bestiary: Control = load("res://bestiary.tscn").instantiate()
    get_root().add_child(bestiary)
    await process_frame
    if bestiary.preview_model == null:
        _fail("Goblin missing from Bestiary")
        return
    bestiary._select_creature(1)
    if bestiary.active_creature != "Fire Beetle":
        _fail("Fire Beetle inaccessible in Bestiary")
        return
    bestiary._select_creature(0)
    if bestiary.active_creature != "Goblin":
        _fail("Goblin inaccessible after switching from beetle")
        return
    bestiary._play_preview_animation("Axe Spin")
    var axe_player: AnimationPlayer = bestiary._find_animator(bestiary.preview_model)
    if axe_player == null or axe_player.current_animation != "Axe_Spin_Attack":
        _fail("Goblin's unique axe animation is inaccessible")
        return
    bestiary.queue_free()
    print("LEAN_BESTIARY_OK")

    var lab: Control = load("res://humanoid_animation_lab.tscn").instantiate()
    get_root().add_child(lab)
    await process_frame
    if lab.shared_player == null or lab.clip_picker.item_count != 20:
        _fail("Human animation lab missing shared library")
        return
    lab._load_target(1)
    if lab.shared_player == null or not lab.shared_player.has_animation("Idle"):
        _fail("Goblin animation lab missing shared Idle")
        return
    lab._play_clip(lab._index_of_clip("Crossbow"))
    lab.queue_free()
    print("LEAN_ANIMATION_LAB_OK")

    var world: Node3D = load("res://world.tscn").instantiate()
    get_root().add_child(world)
    await process_frame
    if world.player == null or world.player.get_node_or_null("CharacterVisual/SharedHumanoidAnimations") == null:
        _fail("Player not using the shared animation framework")
        return
    for state in ["idle","walk","run","attack"]:
        world.play_imported_animation(state)
    world.enemies._spawn("res://assets/goblin_warrior.glb", "Goblin Warrior")
    var found := false
    for mob in world.enemies.mobs:
        if mob.kind == "Goblin Warrior" and is_instance_valid(mob.animator):
            found = mob.animator.has_animation("Walking")
    if not found:
        _fail("Enemy goblin not using shared walk")
        return
    world.enemies._spawn("res://assets/twig_blight.glb", "Twig Blight")
    var twig_found := false
    for mob in world.enemies.mobs:
        if mob.kind == "Twig Blight" and is_instance_valid(mob.animator):
            twig_found = mob.animator.has_animation("Attack_Claw_01_BackSwipe")
    if not twig_found:
        _fail("Twig Blight not using shared claw attack")
        return
    # Character selection must support both full-resolution humanoids.
    var front: Control = load("res://front_menu.tscn").instantiate()
    get_root().add_child(front)
    if front.MODELS.size() != 2 or front.MODELS[1] != "male_human":
        _fail("Male character missing from creator selection")
        return
    front.queue_free()
    world.toggle_inventory()
    if world.inventory_panel == null or world.inventory_panel.hero_model not in ["human_adventurer", "male_human"]:
        _fail("Inventory portrait is not using a registered playable human")
        return
    print("LEAN_WORLD_AND_INVENTORY_OK")
    world.queue_free()
    print("LEAN_CAST_SMOKE_OK")
    quit(0)
