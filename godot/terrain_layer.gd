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
var _next_kind := 0
var _source_albedos: Dictionary = {}
var _materials: Array[ShaderMaterial] = []
var _underlays: Dictionary = {}
var _fill_shader: Shader

func _ready() -> void:
	name = "TerrainLayer"
	var settings := ConfigFile.new()
	if settings.load(STORAGE) == OK:
		blend_inches = clampf(float(settings.get_value("terrain", "edge_blend_inches", 6.0)), 0.0, 30.0)
	set_process(true)

func set_blend_inches(value: float) -> void:
	blend_inches = clampf(value, 0.0, 30.0)
	for material in _materials:
		material.set_shader_parameter("blend_inches", blend_inches)
	var settings := ConfigFile.new()
	settings.set_value("terrain", "edge_blend_inches", blend_inches)
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

func _tile_cells(kind: String) -> Array[Vector2i]:
	var cells: Array[Vector2i] = []
	for x in range(MIN_X, MAX_X + 1):
		for z in range(MIN_Z, MAX_Z + 1):
			if _cell_enabled(x,z) and _type_at(x,z) == kind:
				cells.append(Vector2i(x,z))
	return cells

func _shader() -> Shader:
	var shader := Shader.new()
	shader.code = """shader_type spatial;
render_mode depth_draw_opaque, cull_back;
uniform sampler2D tex_color : source_color, repeat_enable, filter_linear_mipmap;
uniform sampler2D tex_normal : hint_normal, filter_linear_mipmap;
uniform sampler2D tex_orm : filter_linear_mipmap;
uniform sampler2D biome_grass : source_color, filter_linear_mipmap;
uniform sampler2D biome_dirt : source_color, filter_linear_mipmap;
uniform sampler2D biome_leaves : source_color, filter_linear_mipmap;
uniform sampler2D biome_moss : source_color, filter_linear_mipmap;
uniform sampler2D biome_mud : source_color, filter_linear_mipmap;
uniform sampler2D biome_rocky : source_color, filter_linear_mipmap;
uniform vec3 tint : source_color = vec3(1.0);
uniform vec2 raw_min = vec2(-0.5);
uniform vec2 raw_size = vec2(1.0);
uniform float edge_flat_y = 0.0;
uniform float blend_inches : hint_range(0.0,30.0,1.0) = 6.0;
varying vec2 ground_coordinate;
varying vec4 neighbor_ids;
varying vec2 ground_world;
void vertex() {
    ground_coordinate = (VERTEX.xz - raw_min) / raw_size;
    // Source models have small extruded perimeter faces. Flatten only their
    // outer rim to the shared 5ft top elevation to hide black side-wall lines.
    float dist_to_edge = min(min(ground_coordinate.x,1.0-ground_coordinate.x),
        min(ground_coordinate.y,1.0-ground_coordinate.y));
    float flatten_rim = 1.0-smoothstep(0.0,.065,dist_to_edge);
    VERTEX.y=mix(VERTEX.y,edge_flat_y,flatten_rim);
    neighbor_ids = INSTANCE_CUSTOM;
    ground_world = (MODEL_MATRIX * vec4(VERTEX,1.0)).xz;
}
float seam_hash(vec2 p) {
    return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);
}
vec3 neighbor_color(float id, vec2 uv) {
    if (id < 1.5) return texture(biome_grass,uv).rgb * vec3(.87,.87,.83);
    if (id < 2.5) return texture(biome_dirt,uv).rgb * vec3(.69,.59,.48);
    if (id < 3.5) return texture(biome_leaves,uv).rgb * vec3(.72,.65,.52);
    if (id < 4.5) return texture(biome_moss,uv).rgb * vec3(.72,.78,.66);
    if (id < 5.5) return texture(biome_mud,uv).rgb * vec3(.67,.58,.52);
    return texture(biome_rocky,uv).rgb * vec3(.79,.77,.70);
}
void fragment() {
    vec4 base = texture(tex_color,UV);
    vec3 mr = texture(tex_orm,UV).rgb;
    ALBEDO = base.rgb * tint;
    ROUGHNESS = clamp(mr.g,0.08,1.0);
    METALLIC = clamp(mr.b,0.0,1.0);
    NORMAL_MAP = texture(tex_normal,UV).rgb;
    NORMAL_MAP_DEPTH = 1.0;
    // Matching terrain tiles use a small amount of world-space color at
    // their shared border: both sides sample the SAME map coordinates.
    // The tile interior retains its original source UV, texture and normals.
    float match_edge = 1.0;
    if (neighbor_ids.r < .01) match_edge = min(match_edge,ground_coordinate.x);
    if (neighbor_ids.g < .01) match_edge = min(match_edge,1.0-ground_coordinate.x);
    if (neighbor_ids.b < .01) match_edge = min(match_edge,ground_coordinate.y);
    if (neighbor_ids.a < .01) match_edge = min(match_edge,1.0-ground_coordinate.y);
    float align = 1.0-smoothstep(0.0,.10,match_edge);
    vec2 shared_uv=ground_world*.145+vec2(.271,.163);
    vec3 shared_sample=texture(tex_color,shared_uv).rgb*tint;
    ALBEDO=mix(ALBEDO,shared_sample,align*.79);
    // Edge-only 0..30 inches inward, zero mixing in the tile interior.
    float width = min(0.5,blend_inches*0.0254/1.524);
    if (width > 0.00001) {
        // Low-amplitude noise prevents a mechanically straight fade band.
        float irregular = 0.82 + 0.35*seam_hash(floor(ground_world*5.3));
        width *= irregular;
        if (neighbor_ids.r > 0.01) {
            float influence = 0.5*(1.0-smoothstep(0.0,width,ground_coordinate.x));
            ALBEDO = mix(ALBEDO,neighbor_color(round(neighbor_ids.r*8.0),UV),influence);
        }
        if (neighbor_ids.g > 0.01) {
            float influence = 0.5*(1.0-smoothstep(0.0,width,1.0-ground_coordinate.x));
            ALBEDO = mix(ALBEDO,neighbor_color(round(neighbor_ids.g*8.0),UV),influence);
        }
        if (neighbor_ids.b > 0.01) {
            float influence = 0.5*(1.0-smoothstep(0.0,width,ground_coordinate.y));
            ALBEDO = mix(ALBEDO,neighbor_color(round(neighbor_ids.b*8.0),UV),influence);
        }
        if (neighbor_ids.a > 0.01) {
            float influence = 0.5*(1.0-smoothstep(0.0,width,1.0-ground_coordinate.y));
            ALBEDO = mix(ALBEDO,neighbor_color(round(neighbor_ids.a*8.0),UV),influence);
        }
    }
}"""
	return shader

func _material(kind: String, source: StandardMaterial3D, box: AABB, shade: Shader) -> ShaderMaterial:
	var material := ShaderMaterial.new()
	material.shader = shade
	material.set_shader_parameter("tex_color",source.albedo_texture)
	material.set_shader_parameter("tex_normal",source.normal_texture)
	material.set_shader_parameter("tex_orm",source.metallic_texture)
	var tints := {
		"grass": Vector3(.87,.87,.83),
		"dirt": Vector3(.69,.59,.48),
		"leaves": Vector3(.72,.65,.52),
		"moss": Vector3(.72,.78,.66),
		"mud": Vector3(.67,.58,.52),
		"rocky": Vector3(.79,.77,.70)
	}
	material.set_shader_parameter("tint",tints.get(kind,Vector3.ONE))
	material.set_shader_parameter("raw_min",Vector2(box.position.x,box.position.z))
	material.set_shader_parameter("raw_size",Vector2(box.size.x,box.size.z))
	material.set_shader_parameter("edge_flat_y",box.position.y+box.size.y)
	material.set_shader_parameter("blend_inches",blend_inches)
	_materials.append(material)
	return material

func _apply_neighbor_materials() -> void:
	# Bind after all six stages are loaded, preserving staged loading.
	for material in _materials:
		for kind in KINDS:
			material.set_shader_parameter("biome_"+kind,_source_albedos[kind])

func _process(_delta: float) -> void:
	if _next_kind >= KINDS.size():
		_apply_neighbor_materials()
		set_process(false)
		return
	var kind: String = KINDS[_next_kind]
	_next_kind += 1
	var path := "res://assets/terrain/%s.glb" % kind
	var resource := load(path) as PackedScene
	if resource == null:
		push_error("Missing terrain source: " + path)
		return
	var source := resource.instantiate() as Node3D
	add_child(source)
	var found := source.find_children("*","MeshInstance3D",true,false)
	if found.size() != 1:
		push_error("Terrain model requires exactly one mesh: " + kind)
		source.queue_free()
		return
	var original := found[0] as MeshInstance3D
	var box := original.mesh.get_aabb()
	var sx := CELL / box.size.x
	var sz := CELL / box.size.z
	var sy := minf(1.0,.012/maxf(box.size.y,.00001))
	var source_material := original.mesh.surface_get_material(0) as StandardMaterial3D
	if source_material == null or source_material.albedo_texture == null or source_material.normal_texture == null:
		push_error("Incomplete source PBR materials: "+kind)
		source.queue_free()
		return
	_source_albedos[kind] = source_material.albedo_texture
	var shader := _shader()
	var surface_material := _material(kind,source_material,box,shader)
	# The source mesh may contain deeply inset holes, but the underlay should
	# remain a clean uninterrupted earth color rather than stretching source UV.
	var fill_material := ShaderMaterial.new()
	if _fill_shader == null:
		_fill_shader = Shader.new()
		_fill_shader.code = """shader_type spatial;
render_mode cull_back, depth_draw_opaque;
uniform vec3 earth_tint : source_color = vec3(0.20,0.19,0.13);
void fragment() {
	ALBEDO = earth_tint;
	ROUGHNESS = 0.99;
}"""
	fill_material.shader = _fill_shader
	var earthy := {
		"grass":Color(.18,.21,.12),"dirt":Color(.24,.18,.13),
		"leaves":Color(.25,.18,.10),"moss":Color(.19,.24,.13),
		"mud":Color(.17,.14,.11),"rocky":Color(.21,.21,.19)}
	fill_material.set_shader_parameter("earth_tint",Vector3(earthy[kind].r,earthy[kind].g,earthy[kind].b))
	var positions := _tile_cells(kind)
	var multimesh := MultiMesh.new()
	multimesh.transform_format = MultiMesh.TRANSFORM_3D
	multimesh.use_custom_data = true
	multimesh.mesh = original.mesh
	multimesh.instance_count = positions.size()
	var fill_mesh := PlaneMesh.new()
	fill_mesh.size = Vector2(CELL,CELL)
	var filler := MultiMesh.new()
	filler.transform_format = MultiMesh.TRANSFORM_3D
	filler.use_custom_data = true
	filler.mesh = fill_mesh
	filler.instance_count = positions.size()
	var base_scale := Basis.from_scale(Vector3(sx,sy,sz))
	var local_directions: Array[Vector3] = [
		Vector3(-1,0,0),Vector3(1,0,0),Vector3(0,0,-1),Vector3(0,0,1)]
	for index in positions.size():
		var cell := positions[index]
		var yaw := float(_hash(cell.x,cell.y)%4)*PI*.5
		var turn := Basis(Vector3.UP,yaw)
		var basis := turn*base_scale
		var ground_y := .008-box.position.y*sy
		var origin := Vector3(float(cell.x)*CELL,ground_y,float(cell.y)*CELL)
		origin -= basis*Vector3(box.get_center().x,0,box.get_center().z)
		multimesh.set_instance_transform(index,Transform3D(basis,origin))
		filler.set_instance_transform(index,Transform3D(turn,Vector3(float(cell.x)*CELL,.003,float(cell.y)*CELL)))
		var ids: Array[float] = []
		for direction in local_directions:
			var neighbor := turn*direction
			var nx := cell.x+roundi(neighbor.x)
			var nz := cell.y+roundi(neighbor.z)
			var other_kind := _type_at(nx,nz) if _cell_enabled(nx,nz) else kind
			ids.append(float(KINDS.find(other_kind)+1)/8.0 if other_kind!=kind else 0.0)
		var flags := Color(ids[0],ids[1],ids[2],ids[3])
		multimesh.set_instance_custom_data(index,flags)
		filler.set_instance_custom_data(index,flags)
	var fill_batch := MultiMeshInstance3D.new()
	fill_batch.name = kind.capitalize()+"GroundFill"
	fill_batch.multimesh = filler
	fill_batch.material_override = fill_material
	add_child(fill_batch)
	_underlays[kind] = fill_batch
	var batch := MultiMeshInstance3D.new()
	batch.name = kind.capitalize()+"Ground"
	batch.multimesh = multimesh
	batch.material_override = surface_material
	add_child(batch)
	terrain_counts[kind] = positions.size()
	source.queue_free()
