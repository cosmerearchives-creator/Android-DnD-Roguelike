extends SceneTree
## Lightweight integration check for the single-human/single-goblin cast.
const Shared = preload("res://humanoid_shared_animations.gd")
const Goblins = preload("res://goblin_variants.gd")
const Quad = preload("res://quadruped_shared_animations.gd")
const Combat = preload("res://creature_combat_stats.gd")

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
        {"label":"Male Human","path":"res://assets/male_human.glb","min_bones":60},
        {"label":"Twig Blight","path":"res://assets/twig_blight.glb","min_bones":28}
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
        # Import acceptance is now anatomy-based, not a low bone count.
        if item.label != "Human":
            var source_scene: Node3D = load("res://assets/humanoid_mixamo_animation_library.glb").instantiate()
            var origin := Shared.find_skeleton(source_scene)
            var mapping := Shared._build_mapping(origin, skeleton)
            var validation := Shared.validate_humanoid(origin, skeleton, mapping)
            if not validation.valid:
                _fail("Import gate failed for " + str(item.label) + ": " + str(validation.missing))
                return
            if item.label == "Male Human":
                if int(validation.mapped) < 60 or int(validation.fingers) < 30:
                    _fail("Male custom articulated fingers were dropped")
                    return
                for side in ["Left", "Right"]:
                    var actual := Shared._actual_hand_root(skeleton, side)
                    var nominal := Shared._named_bone(skeleton, side + "Hand")
                    if actual == nominal or Shared._custom_finger_mapping(skeleton, side).size() < 15:
                        _fail("Male humanoid still targets dummy hands")
                        return
            if item.label == "Twig Blight":
                if skeleton.get_bone_count() != 28 or int(validation.mapped) != 28:
                    _fail("Twig Blight 28-bone skeleton was modified or lost its coverage")
                    return
            source_scene.free()
        for clip in ["Idle","Walking","Running","Attack","Push","Crossbow",
                "Attack_BowTwoHanded_01_Standard",
                "Attack_MeleeTwoHanded_01_OverheadSmash",
                "Unused_Attack_FirearmOneHanded_01_QuickDraw",
                "Attack_CrossbowTwoHanded_01_Standard",
                "Attack_Claw_01_BackSwipe"]:
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
    # Beetle was previously normalized from a 0.00007-unit mesh AABB,
    # making a single leg fill the entire preview viewport.
    var beetle_preview_bounds: AABB = bestiary._skeleton_bounds_for(bestiary.preview_model, Transform3D.IDENTITY)
    if absf(beetle_preview_bounds.size.y - 0.65) > 0.08:
        _fail("Fire Beetle preview still scales from tiny mesh AABB")
        return
    if bestiary.camera.position.z < 0.8 or bestiary.camera.position.z > 10.0:
        _fail("Fire Beetle initial framing is off-screen")
        return
    print("BEETLE_PREVIEW_SCALE_OK ", bestiary.preview_model.scale.y,
        " camera_distance=", bestiary.camera.position.z)
    bestiary._select_creature(2)
    if bestiary.active_creature != "Twig Blight":
        _fail("Twig Blight inaccessible in Bestiary")
        return
    bestiary._select_creature(3)
    if bestiary.active_creature != "Wolf" or bestiary.preview_animator == null:
        _fail("Wolf missing from Bestiary")
        return
    if not bestiary.preview_animator.has_animation("Attack_Pounce_01_Standard"):
        _fail("Wolf lacks canonical quadruped preview")
        return
    bestiary._play_preview_animation("Movement_Run_01_Standard")
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
    if lab.shared_player == null or lab.clip_picker.item_count != 21:
        _fail("Human animation lab missing shared library")
        return
    lab._load_target(1)
    if lab.shared_player == null or not lab.shared_player.has_animation("Idle"):
        _fail("Goblin animation lab missing shared Idle")
        return
    if lab._index_of_clip("Attack_CrossbowTwoHanded_01_Standard") == 0:
        _fail("Canonical crossbow animation missing from humanoid lab")
        return
    # Alphabetical order; Left Jab is deliberately listed twice.
    var labels: Array[String] = []
    for index in lab.clip_picker.item_count:
        labels.append(str(lab.clip_picker.get_item_metadata(index)))
    var sorted_labels := labels.duplicate()
    sorted_labels.sort()
    if labels != sorted_labels:
        _fail("Humanoid animation picker not alphabetized")
        return
    for required_clip in [
            "Action_Sleep_01_Standard", "Attack_Claw_02_Stab",
            "Attack_Unarmed_01_LeftJab", "Movement_Jump_01_Standard",
            "Movement_Sneak_01_Standard", "Attack_Unarmed_02_HighKick",
            "Attack_MeleeOneHanded_01_Standard", "Cast_Self_01_Standard",
            "Cast_RangedWeakOneHanded_01_Standard",
            "Pending_Action_Talk_01_HandOnHip", "Pending_Attack_Throw_01_Heavy",
            "Pending_Action_PickUp_01_Standard",
            "Movement_WalkMale_01_Standard", "Movement_WalkFemale_01_Standard"]:
        if required_clip not in labels or not lab.shared_player.has_animation(required_clip):
            _fail("Renamed animation missing: " + required_clip)
            return
    if Shared.gameplay_walk_clip("human_adventurer") != "Movement_WalkFemale_01_Standard" or Shared.gameplay_walk_clip("male_human") != "Movement_WalkMale_01_Standard":
        _fail("Walk animation model selection broken")
        return
    lab._play_clip(lab._index_of_clip("Attack_CrossbowTwoHanded_01_Standard"))
    lab.queue_free()
    print("LEAN_ANIMATION_LAB_OK")

    var wolf_scene: Node3D = load("res://assets/wolf_quadruped.glb").instantiate()
    get_root().add_child(wolf_scene)
    var wolf_skeleton := Quad.find_skeleton(wolf_scene)
    var wolf_native := Quad.find_animator(wolf_scene)
    var wolf_check := Quad.validate(wolf_scene)
    var wolf_animator := Quad.attach(wolf_scene)
    if wolf_skeleton == null or wolf_skeleton.get_bone_count() != 46 or not wolf_check.valid:
        _fail("Wolf 46-bone quadruped foundation rejected")
        return
    if wolf_native == null or wolf_animator == null or int(wolf_animator.get_meta("mapped_bones")) != 46:
        _fail("Wolf lost native quadruped animations")
        return
    for name in ["dog_idle1", "dog_run", "dog_dog_over", "leopard_skill_feipu"]:
        if not wolf_animator.has_animation(name):
            _fail("Wolf original clip was dropped: " + name)
            return
    for name in ["Idle_Standing_01_Standard","Movement_Run_01_Standard",
            "Action_RollOver_01_Standard","Attack_Pounce_01_Standard"]:
        if not wolf_animator.has_animation(name):
            _fail("Wolf canonical animation missing: " + name)
            return
        wolf_native.stop()
        wolf_animator.play(name)
        wolf_animator.advance(0.15)
    var beetle_scene: Node3D = load("res://assets/fire_beetle.glb").instantiate()
    get_root().add_child(beetle_scene)
    if Quad.validate(beetle_scene).valid:
        _fail("Beetle was incorrectly accepted as 46-bone quadruped")
        return
    beetle_scene.queue_free()
    wolf_scene.queue_free()
    var wolf_lab: Control = load("res://quadruped_animation_lab.tscn").instantiate()
    get_root().add_child(wolf_lab)
    await process_frame
    if wolf_lab.shared_player == null or wolf_lab.clip_picker.item_count != 4:
        _fail("Separate quadruped animation lab failed")
        return
    wolf_lab._play_clip(wolf_lab._index_of_clip("Movement_Run_01_Standard"))
    wolf_lab.queue_free()
    print("QUADRUPED_REFERENCE_OK bones=46 clips=4")

    var world: Node3D = load("res://world.tscn").instantiate()
    get_root().add_child(world)
    await process_frame
    if world.player == null or world.player.get_node_or_null("CharacterVisual/SharedHumanoidAnimations") == null:
        _fail("Player not using the shared animation framework")
        return
    for state in ["idle","walk","run","attack"]:
        world.play_imported_animation(state)
    if world.TEST_JUMP_COOLDOWN_SECONDS != 1.0 or world.RUN_STICK_THRESHOLD <= 0.85:
        _fail("Jump or running stick threshold missing")
        return
    if world.enemies.MAX_ENEMIES < 16 or world.enemies.SPAWN_INTERVAL > 5.0:
        _fail("Increased spawn frequency not applied")
        return
    world.hud_action("Crouch")
    if not world.sneaking or not world.hud.toggles.get("Crouch", false):
        _fail("Sneak toggle does not engage")
        return
    world.hud_action("Crouch")
    if world.sneaking:
        _fail("Sneak toggle does not disengage")
        return
    # Confirm real ground contact and jump action; failing to find the floor
    # is a test setup error, not permission to skip the jump regression.
    world.player.global_position = Vector3(0, 0.95, 0)
    world.player.velocity = Vector3(0, -0.1, 0)
    for tick in 20:
        world.player.velocity.y -= 9.8 / 60.0
        world.player.move_and_slide()
        if world.player.is_on_floor():
            break
    if not world.player.is_on_floor():
        _fail("Jump test could not establish ground contact")
        return
    world.hud_action("Jump")
    if absf(world.jump_cooldown - 1.0) > 0.0001 or world.player.velocity.y <= 0.0:
        _fail("Jump input missing upward impulse/cooldown")
        return
    var jump_y: float = world.player.velocity.y
    world.hud_action("Jump")
    if absf(jump_y - world.player.velocity.y) > 0.0001:
        _fail("Jump cooldown bypassed")
        return
    world.action("Attack")
    if absf(world.attack - 1.0) > 0.0001:
        _fail("Attack cooldown must be exactly 1 second during testing")
        return
    world.action("Attack")
    if absf(world.attack - 1.0) > 0.0001:
        _fail("Repeated attack bypasses testing cooldown")
        return
    world.enemies._spawn("res://assets/goblin_warrior.glb", "Goblin Warrior")
    var found := false
    for mob in world.enemies.mobs:
        if mob.kind == "Goblin Warrior" and is_instance_valid(mob.animator):
            found = mob.animator.has_animation("Walking")
    if not found:
        _fail("Enemy goblin not using shared walk")
        return
    var goblin_offset_ok := false
    for mob in world.enemies.mobs:
        if mob.kind == "Goblin Warrior":
            var goblin_visual := mob.body.get_node_or_null("CharacterVisual") as Node3D
            goblin_offset_ok = goblin_visual != null and absf(absf(goblin_visual.rotation.y) - PI) < 0.001
            if goblin_visual != null:
                for direction in [Vector3.FORWARD, Vector3.BACK, Vector3.LEFT, Vector3.RIGHT]:
                    mob.body.rotation.y = atan2(-direction.x,-direction.z)
                    var facing := -goblin_visual.global_transform.basis.z
                    if facing.normalized().dot(direction) > -0.99:
                        goblin_offset_ok = false
            break
    if not goblin_offset_ok:
        _fail("Goblin imported mesh still facing backward during movement")
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
    world.enemies._spawn("res://assets/fire_beetle.glb", "Fire Beetle")
    var beetle_found := false
    for mob in world.enemies.mobs:
        if mob.kind == "Fire Beetle":
            var marker := mob.body.get_node_or_null("SpawnTestTag") as Label3D
            var visual := mob.body.get_node_or_null("CharacterVisual") as Node3D
            var beetle_world_bounds: Vector2 = world.enemies._skeletal_bounds(visual, Transform3D.IDENTITY) if visual else Vector2.ZERO
            beetle_found = marker != null and marker.text == "SPAWNTEST" and (
                visual != null and absf(beetle_world_bounds.y - beetle_world_bounds.x - 0.52) < 0.08)
        elif mob.kind == "Twig Blight":
            if mob.body.get_node_or_null("SpawnTestTag") != null:
                _fail("Forest-native Twig Blight should not be tagged SPAWNTEST")
                return
    if not beetle_found:
        _fail("Fire Beetle did not spawn with SPAWNTEST marker and corrected scale")
        return
    # Every imported monster has an authoritative 2014 5e combat definition.
    for profile in [
            {"kind":"Goblin Warrior","ac":15,"hp":7,"bonus":4,"sides":6,"damage_bonus":2},
            {"kind":"Twig Blight","ac":13,"hp":4,"bonus":3,"sides":4,"damage_bonus":1},
            {"kind":"Fire Beetle","ac":13,"hp":4,"bonus":1,"sides":6,"damage_bonus":-1},
            {"kind":"Wolf","ac":13,"hp":11,"bonus":4,"sides":4,"damage_bonus":2}]:
        var stats: Dictionary = Combat.get_monster(profile.kind)
        if stats.is_empty() or int(stats.ac)!=int(profile.ac) or int(stats.hp)!=int(profile.hp) or int(stats.attack_bonus)!=int(profile.bonus) or int(stats.damage_sides)!=int(profile.sides) or int(stats.damage_bonus)!=int(profile.damage_bonus):
            _fail("Creature combat stats diverge from Bestiary: " + str(profile.kind))
            return
    print("COMBAT_MONSTER_STATS_OK 4 creatures")
    # Lock-on takes the nearest living enemy and holds it while valid.
    var nearby_goblin: CharacterBody3D = null
    for mob in world.enemies.mobs:
        if mob.kind == "Goblin Warrior":
            nearby_goblin = mob.body
            break
    if nearby_goblin == null:
        _fail("No Goblin Warrior available for target lock test")
        return
    nearby_goblin.global_position = world.player.global_position+Vector3(0,0,1.2)
    # Keep the test goblin alive during the randomized automatic strike; a
    # critical or high roll must not invalidate the subsequent HP test.
    var test_goblin_index := -1
    for index in world.enemies.mobs.size():
        if world.enemies.mobs[index].body == nearby_goblin:
            test_goblin_index = index
            break
    if test_goblin_index < 0:
        _fail("Target goblin absent from combat state")
        return
    world.enemies.mobs[test_goblin_index].hp = 100
    world.hud_action("Lock")
    if not world.lock_enabled or not world.hud.toggles.get("Lock",false) or world.locked_target != nearby_goblin or not world.target_marker.visible:
        _fail("Automatic lock-on failed to acquire nearest enemy")
        return
    world.hud_action("A")
    if not world.auto_attack_enabled or not world.hud.toggles.get("A",false) or not world.hud.toggles.get("Attack Mode",false):
        _fail("Automatic attack toggle not synchronized across controls")
        return
    world.attack=0.0
    world._physics_process(0.0)
    if absf(world.attack-world.TEST_ATTACK_COOLDOWN_SECONDS)>0.0001 or world.punch_left<=0.0:
        _fail("Automatic attack did not fire when target entered melee reach")
        return
    var attack_timer: float = world.attack
    world._physics_process(0.0)
    if absf(world.attack-attack_timer)>0.0001:
        _fail("Automatic attack bypassed its one-second cooldown")
        return
    print("AUTO_ATTACK_AND_LOCK_OK")
    world.enemies.mobs[test_goblin_index].hp = 7
    # Combat has actual dice and HP loss, not the previous one-hit deletion.
    var durable_goblin: CharacterBody3D = nearby_goblin
    var strike_profile := {"attack_bonus":100, "damage_bonus":0,
        "damage_dice":1, "damage_sides":1}
    var successful_hit := false
    for attempt in 15:
        var outcome: Dictionary = world.enemies.damage_target(durable_goblin,strike_profile)
        if bool(outcome.get("hit",false)):
            successful_hit=true
            if not world.enemies.target_is_alive(durable_goblin):
                _fail("Goblin destroyed after a 1-damage attack")
                return
            if int(world.enemies.get_target_stats(durable_goblin).hp)>=7:
                _fail("Goblin HP failed to decrease")
                return
            break
    if not successful_hit:
        _fail("Dice test produced 15 consecutive natural ones")
        return
    # Enemy aggression must produce real attack callbacks when in range.
    var mob_index := -1
    for i in world.enemies.mobs.size():
        if world.enemies.mobs[i].body == durable_goblin:
            mob_index=i
            break
    if mob_index<0:
        _fail("Damaged goblin not present in enemy combat state")
        return
    world.enemies.mobs[mob_index].attack_cooldown=0.0
    var count_before: int = world.enemies.attack_events
    world.enemies._physics_process(0.016)
    if world.enemies.attack_events<=count_before:
        _fail("Aggressive goblin did not attempt an attack in melee range")
        return
    var hp_before: int = world.player_hp
    world._on_enemy_attack("Goblin Warrior",true,3,"Scimitar")
    if world.player_hp != hp_before-3 or not world.hud.combat_status.contains(str(world.player_hp)):
        _fail("Enemy weapon damage failed to update player HP and HUD")
        return
    print("MONSTER_DAMAGE_AND_AGGRESSION_OK")
    # Beetle is explicitly facing-corrected and can bite independently of
    # humanoid skeletons; it retains its visible SPAWNTEST marker.
    var enemy_beetle: CharacterBody3D = null
    for mob in world.enemies.mobs:
        if mob.kind == "Fire Beetle":
            enemy_beetle=mob.body
            break
    if enemy_beetle == null:
        _fail("No beetle available to test orientation and aggression")
        return
    var beetle_visual := enemy_beetle.get_node_or_null("CharacterVisual") as Node3D
    if beetle_visual==null or absf(absf(beetle_visual.rotation.y)-PI)>0.001:
        _fail("Beetle visual forward correction is missing")
        return
    for direction in [Vector3.FORWARD, Vector3.BACK, Vector3.LEFT, Vector3.RIGHT]:
        enemy_beetle.rotation.y=atan2(-direction.x,-direction.z)
        if (-beetle_visual.global_transform.basis.z).normalized().dot(direction)>-0.99:
            _fail("Beetle imported model rotates against its movement direction")
            return
    enemy_beetle.global_position=world.player.global_position+Vector3(0,0,1.1)
    for i in world.enemies.mobs.size():
        if world.enemies.mobs[i].body==enemy_beetle:
            world.enemies.mobs[i].attack_cooldown=0.0
            break
    var previous_events: int = world.enemies.attack_events
    world.enemies._physics_process(0.016)
    if world.enemies.attack_events<=previous_events:
        _fail("Fire Beetle did not perform an attack in its detection radius")
        return
    world.hud_action("A")
    world.hud_action("Lock")
    if world.auto_attack_enabled or world.lock_enabled or world.target_marker.visible:
        _fail("Auto combat toggles failed to release")
        return
    print("BEETLE_FACING_AND_ATTACK_OK")

    world.toggle_inventory()
    if world.inventory_panel == null or world.inventory_panel.hero_model not in ["human_adventurer", "male_human"]:
        _fail("Inventory portrait is not using a registered playable human")
        return
    print("LEAN_WORLD_AND_INVENTORY_OK")
    world.queue_free()
    print("LEAN_CAST_SMOKE_OK")
    quit(0)
