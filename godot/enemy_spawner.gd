extends Node3D
## Lightweight roaming enemy population for the first forest.
## Uses the same imported animation clips as the Bestiary.
const FOOT := 0.3048
const GOBLINS = preload("res://goblin_variants.gd")
const SHARED = preload("res://humanoid_shared_animations.gd")
const COMBAT = preload("res://creature_combat_stats.gd")
const BEETLE = "res://assets/fire_beetle.glb"
const TWIG_BLIGHT = "res://assets/twig_blight.glb"
const SPAWNTEST_KINDS := ["Fire Beetle"]
const MAX_ENEMIES := 18
const SPAWN_INTERVAL := 4.0
const STARTING_POPULATION := 9
const INITIAL_SPAWN_INTERVAL := 0.18
# Native GLBs face their mesh's +Z axis, whereas the world movement
# code used to turn them as though their forward axis were -Z.
# Apply the correction only to their *visuals*, not collision bodies.
const VISUAL_FORWARD_OFFSETS := {
    "Goblin Warrior": PI,
    "Fire Beetle": PI
}
const MAX_RANGE := 37.0
const SPAWN_MIN := 14.0
const SPAWN_MAX := 30.0
var player: CharacterBody3D
var rng := RandomNumberGenerator.new()
var mobs: Array[Dictionary] = []
var scenes: Dictionary = {}
var spawn_clock := 0.0
var ai_clock := 0.0
var initial_population := false
var initial_remaining := 0
var initial_index := 0
var initial_timer := 0.0
var starting_scenes: Array[String] = []
var cleared_goblins := 0
var on_kill: Callable
var on_player_hit: Callable
var attack_events := 0

func _ready() -> void:
	rng.randomize()

func setup(hero: CharacterBody3D) -> void:
	player = hero
	call_deferred("_initial_spawn")

func _initial_spawn() -> void:
	if not is_instance_valid(player): return
	# The previous startup instantiated all nine skinned enemies in one
	# deferred frame. Thread-load the three distinct source scenes, then
	# introduce one mob every few frames without changing total population.
	starting_scenes = [BEETLE,TWIG_BLIGHT,str(GOBLINS.ENTRIES[0].asset)]
	for path in starting_scenes:
		if scenes.has(path) or not ResourceLoader.exists(path): continue
		var error: Error = ResourceLoader.load_threaded_request(path,"PackedScene",true)
		if error != OK:
			push_warning("Could not background-load enemy %s (code %d)" % [path,error])
	initial_remaining = STARTING_POPULATION
	initial_index = 0
	initial_timer = INITIAL_SPAWN_INTERVAL
	initial_population = true
	spawn_clock = 0.0

func _startup_assets_ready() -> bool:
	var ready := true
	for path in starting_scenes:
		if scenes.has(path): continue
		var state: ResourceLoader.ThreadLoadStatus = ResourceLoader.load_threaded_get_status(path)
		if state == ResourceLoader.THREAD_LOAD_IN_PROGRESS:
			ready = false
		elif state == ResourceLoader.THREAD_LOAD_LOADED:
			scenes[path] = ResourceLoader.load_threaded_get(path)
		else:
			# On an unsupported device, retain the existing load behavior.
			scenes[path] = load(path)
	return ready

func _spawn_random() -> void:
	var kinds: Array = GOBLINS.ENTRIES
	var roll := rng.randf()
	if roll < 0.23:
		_spawn(BEETLE, "Fire Beetle")
	elif roll < 0.43:
		_spawn(TWIG_BLIGHT, "Twig Blight")
	else:
		var entry: Dictionary = kinds[rng.randi_range(0, kinds.size()-1)]
		_spawn(str(entry.asset), str(entry.name))

func _bounds(node: Node3D, parent_transform: Transform3D) -> Vector2:
	var t := parent_transform * node.transform
	var low := INF
	var high := -INF
	if node is MeshInstance3D and node.mesh != null:
		var aabb: AABB = (node as MeshInstance3D).get_aabb()
		for i in range(8):
			var y: float = (t * aabb.get_endpoint(i)).y
			low = minf(low,y)
			high = maxf(high,y)
	for child in node.get_children():
		if child is Node3D:
			var b := _bounds(child,t)
			low = minf(low,b.x)
			high = maxf(high,b.y)
	return Vector2(low,high)

# Animated rest-pose bone extents are authoritative for models with
# unrealistically tiny mesh AABBs, such as the imported Fire Beetle.
func _skeletal_bounds(node: Node3D, relative: Transform3D) -> Vector2:
	var transformed := relative * node.transform
	var low := INF
	var high := -INF
	if node is Skeleton3D:
		var skeleton := node as Skeleton3D
		for index in skeleton.get_bone_count():
			var point: Vector3 = transformed * skeleton.get_bone_global_rest(index).origin
			low = minf(low, point.y)
			high = maxf(high, point.y)
	for child in node.get_children():
		if child is Node3D:
			var result := _skeletal_bounds(child, transformed)
			low = minf(low, result.x)
			high = maxf(high, result.y)
	return Vector2(low, high)

func _spawn(path: String, kind: String) -> void:
	if mobs.size() >= MAX_ENEMIES or not is_instance_valid(player): return
	if not scenes.has(path):
		if not ResourceLoader.exists(path):
			push_warning("Missing enemy resource: " + path)
			return
		scenes[path] = load(path)
	var scene: PackedScene = scenes[path]
	if scene == null: return
	var body := CharacterBody3D.new()
	body.name = "Enemy_" + kind.replace(" ","_")
	body.collision_layer = 2
	body.collision_mask = 1
	# Spawn out of sight from the start, inside the 110m forest floor,
	# and separated from existing enemy bodies.
	var spawn_point := Vector3.ZERO
	var found := false
	for attempt in range(28):
		var angle := rng.randf_range(0.0,TAU)
		var distance := rng.randf_range(SPAWN_MIN,SPAWN_MAX)
		spawn_point = player.global_position + Vector3(cos(angle),0,sin(angle))*distance
		spawn_point.y = 0.0
		if absf(spawn_point.x) > 93.0 or absf(spawn_point.z) > 93.0:
			continue
		var open := true
		for mob in mobs:
			var other: CharacterBody3D = mob.body
			if is_instance_valid(other) and other.global_position.distance_to(spawn_point) < 7.5:
				open = false
				break
		if open:
			found = true
			break
	if not found:
		body.free()
		return
	body.position = spawn_point
	add_child(body)
	var mesh := scene.instantiate() as Node3D
	mesh.name = "CharacterVisual"
	body.add_child(mesh)
	mesh.rotation.y = VISUAL_FORWARD_OFFSETS.get(kind, 0.0)
	var bounds := _skeletal_bounds(mesh,Transform3D.IDENTITY) if kind == "Fire Beetle" else _bounds(mesh,Transform3D.IDENTITY)
	var desired_height := 0.52 if kind == "Fire Beetle" else 1.15 if kind == "Twig Blight" else 1.22
	var minimum_span := 0.0000001 if kind == "Fire Beetle" else 0.01
	if is_finite(bounds.x) and is_finite(bounds.y) and bounds.y > bounds.x + minimum_span:
		mesh.scale *= desired_height/(bounds.y-bounds.x)
		mesh.position.y = -bounds.x*mesh.scale.y
	var shape := CollisionShape3D.new()
	var capsule := CapsuleShape3D.new()
	capsule.radius = 0.23 if kind == "Fire Beetle" else 0.25
	capsule.height = maxf(desired_height,capsule.radius*2.05)
	shape.shape = capsule
	shape.position.y = capsule.height*0.5
	body.add_child(shape)
	if kind == "Fire Beetle":
		var glow := OmniLight3D.new()
		glow.light_color = Color("#ffc45a")
		glow.light_energy = 0.42
		glow.omni_range = 6.1 # 10 feet bright + 10 feet dim, visual approximation.
		glow.shadow_enabled = false
		glow.position.y = 0.3
		body.add_child(glow)
	if kind in SPAWNTEST_KINDS:
		body.add_to_group("spawntest")
		var tag := Label3D.new()
		tag.name = "SpawnTestTag"
		tag.text = "SPAWNTEST"
		tag.font_size = 44
		tag.pixel_size = 0.004
		tag.outline_size = 8
		tag.modulate = Color("#ffe27a")
		tag.billboard = BaseMaterial3D.BILLBOARD_ENABLED
		tag.no_depth_test = true
		tag.position.y = desired_height + 0.22
		body.add_child(tag)
	var animator: AnimationPlayer = _animator(mesh) if kind == "Fire Beetle" else SHARED.attach(mesh)
	var stats: Dictionary = COMBAT.get_monster(kind)
	if stats.is_empty():
		push_error("Unregistered enemy combat stats: " + kind)
		body.queue_free()
		return
	mobs.append({
		"body":body, "animator":animator, "kind":kind,
		"spawntest":kind in SPAWNTEST_KINDS,
		"stats":stats, "hp":int(stats.hp), "max_hp":int(stats.hp),
		"provoked":false, "attack_cooldown":rng.randf_range(0.4,1.1),
		"attack_anim":0.0,
		"visual_rest_position":mesh.position,
		"direction":Vector3.ZERO,"timer":rng.randf_range(0.2,2.0),
		"speed":float(stats.speed_ft)*FOOT/6.0*rng.randf_range(0.9,1.0),
	})
	_set_anim(mobs.back(), "idle")

func _animator(node: Node) -> AnimationPlayer:
	if node is AnimationPlayer: return node as AnimationPlayer
	for child in node.get_children():
		var result := _animator(child)
		if result != null: return result
	return null

func _set_anim(mob: Dictionary, state: String) -> void:
	var anim: AnimationPlayer = mob.animator
	if not is_instance_valid(anim): return
	var kind: String = mob.kind
	var wanted := ""
	if state == "attack":
		wanted = "Attack_Claw_01_BackSwipe" if kind == "Twig Blight" else "Axe_Spin_Attack" if kind == "Goblin Warrior" else "Walk"
	elif state == "walk":
		wanted = "Walk" if kind == "Fire Beetle" else "Movement_WalkMale_01_Standard"
	else:
		wanted = "Walk" if kind == "Fire Beetle" else "Idle"
	if not anim.has_animation(wanted):
		for clip in anim.get_animation_list():
			if clip.to_lower().contains(state):
				wanted = clip
				break
	if not anim.has_animation(wanted): return
	if anim.current_animation != wanted or not anim.is_playing():
		var resource := anim.get_animation(wanted)
		resource.loop_mode = Animation.LOOP_NONE if state == "attack" and kind != "Fire Beetle" else Animation.LOOP_LINEAR
		anim.play(wanted)

func flat_distance(a: Vector3, b: Vector3) -> float:
	return Vector2(a.x-b.x, a.z-b.z).length()

func nearest_target(origin: Vector3, max_distance: float) -> CharacterBody3D:
	var closest: CharacterBody3D = null
	var closest_distance := max_distance
	for mob in mobs:
		var enemy: CharacterBody3D = mob.body
		if not is_instance_valid(enemy) or int(mob.hp) <= 0: continue
		var distance := flat_distance(origin, enemy.global_position)
		if distance < closest_distance:
			closest = enemy
			closest_distance = distance
	return closest

func target_is_alive(target: CharacterBody3D) -> bool:
	if not is_instance_valid(target): return false
	for mob in mobs:
		if mob.body == target and int(mob.hp) > 0:
			return true
	return false

func get_target_stats(target: CharacterBody3D) -> Dictionary:
	if not is_instance_valid(target): return {}
	for mob in mobs:
		if mob.body == target:
			return {"kind":str(mob.kind),"hp":int(mob.hp),
				"max_hp":int(mob.max_hp),"ac":int(mob.stats.ac)}
	return {}

func damage_target(target: CharacterBody3D, attack_stats: Dictionary) -> Dictionary:
	var outcome := {"valid":false, "hit":false, "damage":0, "defeated":false}
	if not target_is_alive(target): return outcome
	for index in mobs.size():
		var mob: Dictionary = mobs[index]
		if mob.body != target: continue
		outcome.valid = true
		outcome.kind = str(mob.kind)
		mob.provoked = true
		var roll: Dictionary = COMBAT.attack_roll(rng, int(attack_stats.attack_bonus), int(mob.stats.ac))
		outcome.hit = bool(roll.hit)
		outcome.critical = bool(roll.critical)
		if bool(roll.hit):
			var damage := COMBAT.damage_roll(rng, attack_stats, bool(roll.critical))
			mob.hp = maxi(0, int(mob.hp)-damage)
			outcome.damage = damage
			outcome.remaining_hp = int(mob.hp)
			outcome.defeated = int(mob.hp) == 0
			if outcome.defeated:
				_defeat_index(index)
		return outcome
	return outcome

func _defeat_index(index: int) -> void:
	var mob: Dictionary = mobs[index]
	if str(mob.kind) == "Goblin Warrior":
		cleared_goblins += 1
		if on_kill.is_valid(): on_kill.call(cleared_goblins)
	var body: CharacterBody3D = mob.body
	mobs.remove_at(index)
	if is_instance_valid(body): body.queue_free()

func _enemy_attack(mob: Dictionary) -> void:
	var stats: Dictionary = mob.stats
	var roll: Dictionary = COMBAT.attack_roll(rng, int(stats.attack_bonus), COMBAT.TEST_PLAYER.ac)
	var hit: bool = bool(roll.hit)
	var damage := COMBAT.damage_roll(rng, stats, bool(roll.critical)) if hit else 0
	mob.attack_cooldown = float(stats.attack_interval)
	mob.attack_anim = 0.48
	attack_events += 1
	_set_anim(mob, "attack")
	if on_player_hit.is_valid():
		on_player_hit.call(str(mob.kind), hit, damage, str(stats.attack_label))

# Separate scheduling from movement so the spawn-distance regression checks
# initial positions, not positions after one or more AI movement ticks.
func _advance_initial_spawn(delta: float) -> void:
	if initial_remaining <= 0: return
	initial_timer -= delta
	if initial_timer > 0.0 or not _startup_assets_ready(): return
	if initial_index == 0:
		_spawn(BEETLE,"Fire Beetle")
	elif initial_index == 1:
		_spawn(TWIG_BLIGHT,"Twig Blight")
	else:
		_spawn_random()
	initial_index += 1
	initial_remaining -= 1
	initial_timer = INITIAL_SPAWN_INTERVAL
	if initial_remaining == 0:
		spawn_clock = 0.0

func _physics_process(delta: float) -> void:
	if not initial_population or not is_instance_valid(player): return
	ai_clock += delta
	if initial_remaining > 0:
		_advance_initial_spawn(delta)
	else:
		spawn_clock += delta
		if spawn_clock >= SPAWN_INTERVAL:
			spawn_clock = 0.0
			if mobs.size() < MAX_ENEMIES:
				_spawn_random()
	for index in range(mobs.size()-1,-1,-1):
		var mob: Dictionary = mobs[index]
		var body: CharacterBody3D = mob.body
		if not is_instance_valid(body):
			mobs.remove_at(index)
			continue
		var distance := flat_distance(body.global_position,player.global_position)
		if distance > MAX_RANGE:
			body.queue_free()
			mobs.remove_at(index)
			continue
		mob.attack_cooldown = maxf(0.0, float(mob.attack_cooldown)-delta)
		mob.attack_anim = maxf(0.0, float(mob.attack_anim)-delta)
		mob.timer = float(mob.timer)-delta
		if float(mob.timer)<=0.0:
			mob.timer = rng.randf_range(1.5,4.0)
			var move_angle := rng.randf_range(0.0,TAU)
			mob.direction = Vector3(cos(move_angle),0,sin(move_angle)) if rng.randf()>0.18 else Vector3.ZERO
		var stats: Dictionary = mob.stats
		var hostile := (bool(stats.aggressive) or bool(mob.provoked)) and distance <= float(stats.notice_m)
		var direction: Vector3 = mob.direction
		if hostile:
			direction = player.global_position-body.global_position
			direction.y=0.0
			direction = direction.normalized()
			if distance <= float(stats.reach_ft)*FOOT*0.90:
				direction=Vector3.ZERO
				if float(mob.attack_cooldown)<=0.0:
					# The attack callback applies the actual creature stat block.
					_enemy_attack(mob)
		elif distance > 24.0:
			direction = player.global_position-body.global_position
			direction.y=0.0
			direction=direction.normalized()
		body.velocity.x = direction.x*float(mob.speed)
		body.velocity.z = direction.z*float(mob.speed)
		body.velocity.y = -0.5 if body.is_on_floor() else body.velocity.y-9.8*delta
		body.move_and_slide()
		var facing: Vector3 = direction
		if hostile and facing.length_squared()<0.05:
			facing=player.global_position-body.global_position
			facing.y=0.0
		if facing.length_squared()>0.05:
			body.rotation.y=atan2(-facing.x,-facing.z)
		if str(mob.kind) == "Fire Beetle":
			# No native Bite clip exists; a short physical lunge clearly marks
			# its attack without modifying its 27-bone skeleton or Walk clip.
			var beetle_visual := body.get_node_or_null("CharacterVisual") as Node3D
			if beetle_visual != null:
				var phase := (0.48-float(mob.attack_anim))/0.48
				var distance_lunge := 0.15*sin(clampf(phase,0.0,1.0)*PI) if float(mob.attack_anim)>0.0 else 0.0
				beetle_visual.position = mob.visual_rest_position + Vector3(0,0,-distance_lunge)
		if float(mob.attack_anim)<=0.0:
			_set_anim(mob,"walk" if direction.length_squared()>0.05 else "idle")

# Backward-compatible manual attack entrypoint, now doing real hit and damage
# rolls rather than instantly deleting a creature on every button press.
func defeat_nearest(range_meters: float) -> bool:
	if not is_instance_valid(player): return false
	var nearest := nearest_target(player.global_position, range_meters)
	if nearest == null: return false
	var result := damage_target(nearest, COMBAT.TEST_PLAYER)
	return bool(result.get("defeated",false))
