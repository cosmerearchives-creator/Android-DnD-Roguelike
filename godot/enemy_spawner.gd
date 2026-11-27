extends Node3D
## Lightweight roaming enemy population for the first forest.
## Uses the same imported animation clips as the Bestiary.
const FOOT := 0.3048
const GOBLINS = preload("res://goblin_variants.gd")
const SHARED = preload("res://humanoid_shared_animations.gd")
const BEETLE = "res://assets/fire_beetle.glb"
const TWIG_BLIGHT = "res://assets/twig_blight.glb"
const SPAWNTEST_KINDS := ["Fire Beetle"]
const MAX_ENEMIES := 18
const SPAWN_INTERVAL := 4.0
const STARTING_POPULATION := 9
# Native GLBs face their mesh's +Z axis, whereas the world movement
# code used to turn them as though their forward axis were -Z.
# Apply the correction only to their *visuals*, not collision bodies.
const VISUAL_FORWARD_OFFSETS := {
    "Goblin Warrior": PI
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
var cleared_goblins := 0
var on_kill: Callable

func _ready() -> void:
	rng.randomize()

func setup(hero: CharacterBody3D) -> void:
	player = hero
	call_deferred("_initial_spawn")

func _initial_spawn() -> void:
	if not is_instance_valid(player): return
	# Populate the forest progressively, rather than packing every enemy
	# around the starting location at scene load.
	# Guarantee a visible population to test both imports on every fresh map.
	_spawn(BEETLE, "Fire Beetle")
	_spawn(TWIG_BLIGHT, "Twig Blight")
	for index in STARTING_POPULATION - 2:
		_spawn_random()
	initial_population = true
	spawn_clock = 0.0

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
	mobs.append({
		"body":body, "animator":animator, "kind":kind,
		"spawntest":kind in SPAWNTEST_KINDS,
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
	var defeated_name := ""
	for mob in mobs:
		if mob.body == nearest:
			defeated_name = str(mob.kind)
			break
	if defeated_name == "Goblin Warrior":
		cleared_goblins += 1
		if on_kill.is_valid(): on_kill.call(cleared_goblins)
	for i in range(mobs.size()-1,-1,-1):
		if mobs[i].body == nearest:
			mobs.remove_at(i)
			break
	nearest.queue_free()
	return true

