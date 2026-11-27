extends Node3D
## Lightweight roaming enemy population for the first forest.
## Uses the same imported animation clips as the Bestiary.
const FOOT := 0.3048
const GOBLINS = preload("res://goblin_variants.gd")
const BEETLE = "res://assets/fire_beetle.glb"
const MAX_ENEMIES := 8
const SPAWN_INTERVAL := 6.0
const MAX_RANGE := 37.0
const SPAWN_MIN := 5.5
const SPAWN_MAX := 19.0
var player: CharacterBody3D
var rng := RandomNumberGenerator.new()
var mobs: Array[Dictionary] = []
var scenes: Dictionary = {}
var spawn_clock := 0.0
var ai_clock := 0.0
var initial_population := false

func _ready() -> void:
	rng.randomize()

func setup(hero: CharacterBody3D) -> void:
	player = hero
	call_deferred("_initial_spawn")

func _initial_spawn() -> void:
	if not is_instance_valid(player): return
	# Ensure every goblin variant and the beetle appear at least once.
	for entry in GOBLINS.ENTRIES:
		_spawn(str(entry.asset), str(entry.name))
	_spawn(BEETLE, "Fire Beetle")
	for i in range(2):
		_spawn_random()
	initial_population = true

func _spawn_random() -> void:
	var kinds: Array = GOBLINS.ENTRIES
	if rng.randf() < 0.23:
		_spawn(BEETLE, "Fire Beetle")
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
	var angle := rng.randf_range(0.0,TAU)
	var distance := rng.randf_range(SPAWN_MIN,SPAWN_MAX)
	body.global_position = player.global_position + Vector3(cos(angle),0,sin(angle))*distance
	body.global_position.y = 0
	add_child(body)
	var mesh := scene.instantiate() as Node3D
	mesh.name = "CharacterVisual"
	body.add_child(mesh)
	var bounds := _bounds(mesh,Transform3D.IDENTITY)
	var desired_height := 0.52 if kind == "Fire Beetle" else 1.42 if kind == "Emerald Blade Warden" else 1.22
	if is_finite(bounds.x) and is_finite(bounds.y) and bounds.y > bounds.x + 0.01:
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
	var animator := _animator(mesh)
	mobs.append({
		"body":body, "animator":animator, "kind":kind,
		"direction":Vector3.ZERO,"timer":rng.randf_range(0.2,2.0),
		"speed":rng.randf_range(0.65,1.0) if kind != "Fire Beetle" else 0.75,
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
	var chosen := ""
	for clip in anim.get_animation_list():
		if clip.to_lower().contains(state):
			chosen=clip
			break
	if chosen.is_empty() and state == "idle":
		for clip in anim.get_animation_list():
			if clip.to_lower().contains("walk"):
				chosen=clip
				break
	if chosen.is_empty(): return
	if anim.current_animation != chosen or not anim.is_playing():
		var resource := anim.get_animation(chosen)
		if resource != null: resource.loop_mode=Animation.LOOP_LINEAR
		anim.play(chosen)

func _physics_process(delta: float) -> void:
	if not initial_population or not is_instance_valid(player): return
	spawn_clock += delta
	ai_clock += delta
	if spawn_clock >= SPAWN_INTERVAL:
		spawn_clock = 0
		if mobs.size() < MAX_ENEMIES:
			_spawn_random()
	for i in range(mobs.size()-1,-1,-1):
		var mob: Dictionary = mobs[i]
		var body: CharacterBody3D = mob.body
		if not is_instance_valid(body):
			mobs.remove_at(i)
			continue
		if body.global_position.distance_to(player.global_position)>MAX_RANGE:
			body.queue_free()
			mobs.remove_at(i)
			continue
		mob.timer = float(mob.timer)-delta
		if float(mob.timer)<=0.0:
			mob.timer=rng.randf_range(1.5,4.0)
			var move_angle := rng.randf_range(0.0,TAU)
			mob.direction=Vector3(cos(move_angle),0,sin(move_angle)) if rng.randf() > 0.18 else Vector3.ZERO
		var direction: Vector3=mob.direction
		# Keep wandering creatures in the active area, without chasing the player.
		if body.global_position.distance_to(player.global_position)>24.0:
			direction=(player.global_position-body.global_position)
			direction.y=0.0
			direction=direction.normalized()
		var speed: float = mob.speed
		body.velocity.x=direction.x*speed
		body.velocity.z=direction.z*speed
		body.velocity.y=-0.5 if body.is_on_floor() else body.velocity.y-9.8*delta
		body.move_and_slide()
		if direction.length_squared()>0.05:
			body.rotation.y=atan2(-direction.x,-direction.z)
		_set_anim(mob,"walk" if direction.length_squared()>0.05 else "idle")

func defeat_nearest(range_meters: float) -> bool:
	if not is_instance_valid(player): return false
	var nearest: CharacterBody3D = null
	var nearest_dist := range_meters
	for mob in mobs:
		var body: CharacterBody3D=mob.body
		if not is_instance_valid(body): continue
		var dist := player.global_position.distance_to(body.global_position)
		if dist <= nearest_dist:
			nearest=body
			nearest_dist=dist
	if nearest==null: return false
	for i in range(mobs.size()-1,-1,-1):
		if mobs[i].body == nearest:
			mobs.remove_at(i)
			break
	nearest.queue_free()
	return true

