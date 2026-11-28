extends Node3D
## Six original full-resolution terrain materials on one true 5-foot grid.
## Square, continuous underlay prevents holes between irregular source edges.
## Biome transitions mix the ACTUAL neighboring texture only at shared edges.
const CELL := 1.524
const KINDS := ["grass", "dirt", "leaves", "moss", "mud", "rocky"]
const MIN_X := -11
const MAX_X := 22
const MIN_Z := -10
const MAX_Z := 10
const STORAGE := "user://terrain_blending.cfg"
var terrain_counts: Dictionary = {}
var blend_inches := 6.0
var material := ShaderMaterial.new()

func _ready() -> void:
	name = "TerrainLayer"
	var settings := ConfigFile.new()
	if settings.load(STORAGE)==OK:
		blend_inches=clampf(float(settings.get_value("terrain","edge_blend_inches",6.0)),0,30)
	var grid := Image.create(MAX_X-MIN_X+1,MAX_Z-MIN_Z+1,false,Image.FORMAT_R8)
	for x in range(MIN_X,MAX_X+1):
		for z in range(MIN_Z,MAX_Z+1):
			if _cell_enabled(x,z):
				var kind := _type_at(x,z)
				grid.set_pixel(x-MIN_X,z-MIN_Z,Color(float(KINDS.find(kind)+1)/255.0,0,0))
				terrain_counts[kind]=int(terrain_counts.get(kind,0))+1
	material.shader=preload("res://terrain_ground.gdshader")
	material.set_shader_parameter("biome_map",ImageTexture.create_from_image(grid))
	material.set_shader_parameter("map_origin",Vector2(MIN_X,MIN_Z))
	material.set_shader_parameter("cell_size",CELL)
	material.set_shader_parameter("blend_inches",blend_inches)
	material.set_shader_parameter("meadow",preload("res://grass_seamless.png"))
	for channel in ["color","normal","orm"]:
		material.set_shader_parameter("patches_"+channel,load("res://assets/terrain/ground_patches_"+channel+".png"))

func set_blend_inches(value: float) -> void:
	blend_inches=clampf(value,0,30)
	material.set_shader_parameter("blend_inches",blend_inches)
	var settings := ConfigFile.new()
	settings.set_value("terrain","edge_blend_inches",blend_inches)
	settings.save(STORAGE)

func _hash(x: int, z: int) -> int:
	var k := x * 73856093 ^ z * 19349663
	return absi(k ^ (k >> 11))

func _type_at(x: int, z: int) -> String:
	if x >= 0 and x <= 10 and absi(z) <= 1:
		return "dirt"
	var field := sin(float(x) * .24 + 1.3) * .74 + cos(float(z) * .30 - .7) * .52 + sin(float(x + z) * .13) * .42
	if field < -1.00: return "mud"
	if field < -.53: return "rocky"
	if field < -.07: return "dirt"
	if field < .37: return "leaves"
	if field < .79: return "moss"
	return "grass"

func _cell_enabled(x: int, z: int) -> bool:
	if x < MIN_X or x > MAX_X or z < MIN_Z or z > MAX_Z: return false
	var center_x := float(x) * CELL
	var center_z := float(z) * CELL
	if center_x > 12.2 and center_x < 30.0 and absf(center_z) < 8.1: return false
	var edge := mini(mini(x - MIN_X, MAX_X - x), mini(z - MIN_Z, MAX_Z - z))
	return not (edge < 2 and _hash(x, z) % 3 == 0)

