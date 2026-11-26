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
var camera_angle := 60.0
var angle_button: Button
var model_index := 0
var model_button: Button
const MODELS = ["classic_study", "petite_pale", "classic_male", "petite_male"]
const MODEL_NAMES = ["Classic Ranger", "Petite Scout", "Classic Male", "Petite Male"]
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
		model.name = "CharacterVisual"
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
	var floor_visual := mesh_at(ground,floor_mesh,Vector3.ZERO,Color("#526b40"))
	var terrain_shader := Shader.new()
	terrain_shader.code = """shader_type spatial;
varying vec3 world_pos;
void vertex() { world_pos = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz; }
float hash(vec2 p) { return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453); }
float noise2(vec2 p) {
	vec2 i=floor(p), f=fract(p);
	f=f*f*(3.0-2.0*f);
	return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);
}
void fragment() {
	vec2 p=world_pos.xz;
	float n=noise2(p*0.32)*0.65+noise2(p*2.5)*0.25+noise2(p*18.0)*0.10;
	ALBEDO=mix(vec3(0.18,0.30,0.16),vec3(0.24,0.39,0.19),n);
	ALBEDO=mix(ALBEDO,vec3(0.31,0.23,0.15),smoothstep(0.72,0.88,n)*0.6);
	ROUGHNESS=0.93;
}"""
	var terrain_material := ShaderMaterial.new()
	terrain_material.shader = terrain_shader
	floor_visual.material_override = terrain_material
	for i in range(260):
		var p := Vector3(rng.randf_range(-95,95),0,rng.randf_range(-95,95))
		if p.length() < 8: continue
		var tree := StaticBody3D.new()
		add_child(tree)
		tree.position = p
		var scale_factor := rng.randf_range(1.6,3.6)
		if rng.randf() < 0.08: scale_factor *= 1.7
		var path := "res://assets/pine.glb" if rng.randf() < 0.55 else "res://assets/broadleaf.glb"
		var model := (load(path) as PackedScene).instantiate() as Node3D
		tree.add_child(model)
		model.scale = Vector3.ONE * scale_factor
		model.rotation.y = rng.randf_range(0,TAU)
		var col := CollisionShape3D.new()
		var cylinder := CylinderShape3D.new()
		cylinder.radius = 0.095 * scale_factor
		cylinder.height = 1.1 * scale_factor
		col.shape = cylinder
		col.position.y = cylinder.height / 2
		tree.add_child(col)
	for i in range(160):
		var p := Vector3(rng.randf_range(-95,95),0,rng.randf_range(-95,95))
		if p.length() < 3: continue
		var rock := (preload("res://assets/boulder.glb") as PackedScene).instantiate() as Node3D
		add_child(rock)
		rock.position = p
		rock.scale = Vector3.ONE * rng.randf_range(0.5,2.0)
		rock.rotation.y = rng.randf_range(0,TAU)
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
	camera.projection = Camera3D.PROJECTION_ORTHOGONAL
	camera.size = 18.0
	camera.rotation = Vector3(-deg_to_rad(camera_angle), yaw, 0)
	camera.far = 300 * FOOT
	var ui := CanvasLayer.new()
	add_child(ui)
	status = Label.new()
	status.position = Vector2(20,30)
	ui.add_child(status)
	var releases := preload("res://release_updates.gd").new()
	releases.position = Vector2(20,110)
	ui.add_child(releases)
	angle_button = Button.new()
	angle_button.text = "Camera: 60°"
	angle_button.position = Vector2(500,110)
	ui.add_child(angle_button)
	angle_button.pressed.connect(toggle_angle)
	model_button = Button.new()
	model_button.text = MODEL_NAMES[0]
	model_button.position = Vector2(20,190)
	ui.add_child(model_button)
	model_button.pressed.connect(cycle_model)
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
func cycle_model():
	var next_index := (model_index + 1) % MODELS.size()
	var asset := "res://assets/%s.glb" % MODELS[next_index]
	if not ResourceLoader.exists(asset): return
	var old := player.get_node_or_null("CharacterVisual")
	if old != null:
		player.remove_child(old)
		old.queue_free()
	var visual := (load(asset) as PackedScene).instantiate()
	visual.name = "CharacterVisual"
	player.add_child(visual)
	model_index = next_index
	model_button.text = MODEL_NAMES[model_index]
func toggle_angle():
	camera_angle = 75.0 if camera_angle == 60.0 else 60.0
	angle_button.text = "Camera: %d°" % int(camera_angle)
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
	var pitch := deg_to_rad(camera_angle)
	camera.position = player.position + Vector3(0,20*sin(pitch),20*cos(pitch)).rotated(Vector3.UP,yaw)
	camera.rotation = Vector3(-pitch, yaw, 0)
	attack = maxf(0,attack-dt)
	var distance := player.position.distance_to(goblin.position)/FOOT if is_instance_valid(goblin) else 0.0
	status.text = "FORESTS OF HELL 3D\nGoblin: %.1f ft | Attack: %.1f" % [distance,attack]
