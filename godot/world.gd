extends Node3D
const FOOT = 0.3048
var player: CharacterBody3D
var goblin: CharacterBody3D
var camera: Camera3D
var stick := Vector2.ZERO
var touch_id := -1
var attack := 0.0
var running := false
var yaw := 0.0
var status: Label
var rng := RandomNumberGenerator.new()
func mesh_at(parent: Node3D, mesh: Mesh, pos: Vector3, color: Color) -> MeshInstance3D:
	var m := MeshInstance3D.new()
	m.mesh = mesh
	var mat := StandardMaterial3D.new()
	mat.albedo_color = color
	m.material_override = mat
	parent.add_child(m)
	m.position = pos
	return m
func body(pos: Vector3, asset: String, color: Color) -> CharacterBody3D:
	var b := CharacterBody3D.new()
	add_child(b)
	b.position = pos
	var c := CollisionShape3D.new()
	var capsule := CapsuleShape3D.new()
	capsule.radius = 0.3
	capsule.height = 1.7
	c.shape = capsule
	c.position.y = 0.85
	b.add_child(c)
	if ResourceLoader.exists(asset):
		var model = load(asset).instantiate()
		b.add_child(model)
	else:
		var mesh := CapsuleMesh.new()
		mesh.radius = 0.3
		mesh.height = 1.7
		mesh_at(b,mesh,Vector3(0,0.85,0),color)
	return b
func _ready():
	rng.seed = 812
	var ground := StaticBody3D.new()
	add_child(ground)
	var shape := CollisionShape3D.new()
	var box := BoxShape3D.new()
	box.size = Vector3(220,1,220)
	shape.shape = box
	ground.add_child(shape)
	ground.position.y = -0.5
	var floor_mesh := BoxMesh.new()
	floor_mesh.size = box.size
	mesh_at(ground,floor_mesh,Vector3.ZERO,Color("#536b42"))
	for i in range(260):
		var p := Vector3(rng.randf_range(-95,95),0,rng.randf_range(-95,95))
		if p.length() < 8: continue
		var tree := StaticBody3D.new()
		add_child(tree)
		tree.position = p
		var trunk := CylinderMesh.new()
		trunk.top_radius = 0.18
		trunk.bottom_radius = 0.25
		trunk.height = 3
		mesh_at(tree,trunk,Vector3(0,1.5,0),Color("#594332"))
		var col := CollisionShape3D.new()
		var cylinder := CylinderShape3D.new()
		cylinder.radius = 0.25
		cylinder.height = 3
		col.shape = cylinder
		col.position.y = 1.5
		tree.add_child(col)
		var crown := CylinderMesh.new()
		crown.top_radius = 0
		crown.bottom_radius = 1.6
		crown.height = 4
		mesh_at(tree,crown,Vector3(0,3.5,0),Color("#284b32"))
	player = body(Vector3.ZERO,"res://assets/classic_study.glb",Color.BEIGE)
	goblin = body(Vector3(0,0,-12),"res://assets/dagger_goblin.glb",Color("#708238"))
	var sun := DirectionalLight3D.new()
	add_child(sun)
	sun.rotation_degrees = Vector3(-55,-35,0)
	sun.shadow_enabled = true
	var env := WorldEnvironment.new()
	env.environment = Environment.new()
	env.environment.background_mode = Environment.BG_COLOR
	env.environment.background_color = Color("#a5b8c2")
	env.environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.environment.ambient_light_color = Color.WHITE
	env.environment.ambient_light_energy = 0.7
	add_child(env)
	camera = Camera3D.new()
	add_child(camera)
	camera.current = true
	camera.far = 300 * FOOT
	var ui := CanvasLayer.new()
	add_child(ui)
	status = Label.new()
	status.position = Vector2(20,30)
	ui.add_child(status)
	var controls := HBoxContainer.new()
	controls.position = Vector2(365,1080)
	ui.add_child(controls)
	for title in ["Run","Attack","Turn"]:
		var button := Button.new()
		button.text = title
		button.custom_minimum_size = Vector2(105,100)
		controls.add_child(button)
		button.pressed.connect(func(): action(title))
	var hint := Label.new()
	hint.text = "Drag here to move"
	hint.position = Vector2(30,1130)
	ui.add_child(hint)
func action(title: String):
	if title == "Run": running = not running
	elif title == "Turn": yaw += PI/4
	elif attack <= 0:
		attack = 6
		if is_instance_valid(goblin) and player.position.distance_to(goblin.position) <= 5*FOOT:
			goblin.queue_free()
func _input(event):
	if event is InputEventScreenTouch:
		if event.pressed and event.position.x < 350 and event.position.y > 850:
			touch_id = event.index
			stick = Vector2.ZERO
		elif not event.pressed and event.index == touch_id:
			touch_id = -1
			stick = Vector2.ZERO
	elif event is InputEventScreenDrag and event.index == touch_id:
		stick = (stick + event.relative/80).limit_length()
func _physics_process(dt):
	var direction := Vector3(stick.x,0,stick.y).rotated(Vector3.UP,yaw)
	player.velocity = direction * (30*FOOT/6) * (2 if running else 1)
	player.move_and_slide()
	if direction.length() > 0.1: player.rotation.y = atan2(-direction.x,-direction.z)
	camera.position = player.position + Vector3(0,5,8).rotated(Vector3.UP,yaw)
	camera.look_at(player.position + Vector3(0,1,-3).rotated(Vector3.UP,yaw))
	attack = maxf(0,attack-dt)
	var distance := player.position.distance_to(goblin.position)/FOOT if is_instance_valid(goblin) else 0.0
	status.text = "FORESTS OF HELL 3D\nGoblin: %.1f ft | Attack: %.1f" % [distance,attack]
