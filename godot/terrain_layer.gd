extends Node3D
## First ground-material layer: six full-resolution user assets, placed on a true
## 5-ft / 1.524-m grid. The existing world floor remains the collision surface.
## Multimesh batching prevents one node/draw call per repeated source tile.
const CELL := 1.524
const KINDS := ["grass", "dirt", "leaves", "moss", "mud", "rocky"]
const MIN_X := -11
const MAX_X := 22
const MIN_Z := -10
const MAX_Z := 10
var terrain_counts: Dictionary = {}
var _next_kind := 0

func _ready() -> void:
    name = "TerrainLayer"
    set_process(true)

func _hash(x: int, z: int) -> int:
    var k := x * 73856093 ^ z * 19349663
    return absi(k ^ (k >> 11))

func _type_at(x: int, z: int) -> String:
    # An obvious path to the tavern, without locking the rest of the terrain to a grid pattern.
    if x >= 0 and x <= 10 and absi(z) <= 1:
        return "dirt"
    var field := sin(float(x) * .24 + 1.3) * .74 + cos(float(z) * .30 - .7) * .52 + sin(float(x + z) * .13) * .42
    if field < -1.00:
        return "mud"
    if field < -.53:
        return "rocky"
    if field < -.07:
        return "dirt"
    if field < .37:
        return "leaves"
    if field < .79:
        return "moss"
    return "grass"

func _cell_enabled(x: int, z: int) -> bool:
    if x < MIN_X or x > MAX_X or z < MIN_Z or z > MAX_Z:
        return false
    # Preserve the wooden tavern floor and remove invisible ground draws.
    var center_x := float(x) * CELL
    var center_z := float(z) * CELL
    if center_x > 12.2 and center_x < 30.0 and absf(center_z) < 8.1:
        return false
    var edge := mini(mini(x - MIN_X, MAX_X - x), mini(z - MIN_Z, MAX_Z - z))
    return not (edge < 2 and _hash(x, z) % 3 == 0)

func _tile_cells(kind: String) -> Array[Vector2i]:
    var cells: Array[Vector2i] = []
    for x in range(MIN_X, MAX_X + 1):
        for z in range(MIN_Z, MAX_Z + 1):
            if _cell_enabled(x, z) and _type_at(x, z) == kind:
                cells.append(Vector2i(x, z))
    return cells

func _feathered_material(kind: String, source_material: StandardMaterial3D, limits: AABB) -> ShaderMaterial:
    # The original 2048px albedo, normal and metallic/roughness maps are reused
    # directly; runtime shading creates a soft perimeter without editing them.
    # Original grass mesh carries an emissive map that makes the ground neon;
    # this ground-specific material deliberately does not apply emission.
    var shader := Shader.new()
    shader.code = """shader_type spatial;
render_mode blend_mix, depth_draw_opaque, cull_back;
uniform sampler2D tex_color : source_color, filter_linear_mipmap;
uniform sampler2D tex_normal : hint_normal, filter_linear_mipmap;
uniform sampler2D tex_orm : filter_linear_mipmap;
uniform vec3 tint : source_color = vec3(1.0);
uniform vec2 raw_min = vec2(-0.5);
uniform vec2 raw_size = vec2(1.0);
varying vec2 ground_coordinate;
varying vec4 open_sides;
void vertex() {
    ground_coordinate = (VERTEX.xz - raw_min) / raw_size;
    open_sides = INSTANCE_CUSTOM;
}
void fragment() {
    vec4 ground = texture(tex_color, UV);
    vec3 mr = texture(tex_orm, UV).rgb;
    ALBEDO = ground.rgb * tint;
    ROUGHNESS = clamp(mr.g, 0.08, 1.0);
    METALLIC = clamp(mr.b, 0.0, 1.0);
    NORMAL_MAP = texture(tex_normal, UV).rgb;
    NORMAL_MAP_DEPTH = 1.0;
    // Only feather exposed perimeter edges; interior neighbors remain opaque.
    float alpha = 1.0;
    alpha *= mix(1.0, smoothstep(0.0, 0.36, ground_coordinate.x), open_sides.r);
    alpha *= mix(1.0, smoothstep(0.0, 0.36, 1.0-ground_coordinate.x), open_sides.g);
    alpha *= mix(1.0, smoothstep(0.0, 0.36, ground_coordinate.y), open_sides.b);
    alpha *= mix(1.0, smoothstep(0.0, 0.36, 1.0-ground_coordinate.y), open_sides.a);
    ALPHA = clamp(alpha, 0.0, 1.0);
}"""
    var mat := ShaderMaterial.new()
    mat.shader = shader
    mat.set_shader_parameter("tex_color", source_material.albedo_texture)
    mat.set_shader_parameter("tex_normal", source_material.normal_texture)
    mat.set_shader_parameter("tex_orm", source_material.metallic_texture)
    var colors := {
        "grass": Vector3(.73, .59, .41),
        "dirt": Vector3(.69, .59, .48),
        "leaves": Vector3(.72, .65, .52),
        "moss": Vector3(.72, .78, .66),
        "mud": Vector3(.67, .58, .52),
        "rocky": Vector3(.79, .77, .70)
    }
    mat.set_shader_parameter("tint", colors.get(kind, Vector3.ONE))
    mat.set_shader_parameter("raw_min", Vector2(limits.position.x, limits.position.z))
    mat.set_shader_parameter("raw_size", Vector2(limits.size.x, limits.size.z))
    return mat

func _process(_delta: float) -> void:
    if _next_kind >= KINDS.size():
        set_process(false)
        return
    var kind: String = KINDS[_next_kind]
    _next_kind += 1
    var path := "res://assets/terrain/%s.glb" % kind
    var resource := load(path) as PackedScene
    if resource == null:
        push_error("Missing terrain source: " + path)
        return
    var source: Node3D = resource.instantiate()
    add_child(source)
    var found := source.find_children("*", "MeshInstance3D", true, false)
    if found.size() != 1:
        push_error("Terrain model requires exactly one mesh: " + kind)
        source.queue_free()
        return
    var original := found[0] as MeshInstance3D
    var box := original.mesh.get_aabb()
    var sx := CELL / box.size.x
    var sz := CELL / box.size.z
    # The imported terrain relief ranges from 2 mm to 77 mm at source scale;
    # compress ONLY the vertical mesh scale to keep this walkable, under 12 mm.
    var sy := minf(1.0, .012 / maxf(box.size.y, .00001))
    var positions := _tile_cells(kind)
    var multimesh := MultiMesh.new()
    multimesh.transform_format = MultiMesh.TRANSFORM_3D
    multimesh.use_custom_data = true
    multimesh.mesh = original.mesh
    multimesh.instance_count = positions.size()
    var base_scale := Basis.from_scale(Vector3(sx, sy, sz))
    for index in positions.size():
        var cell := positions[index]
        var yaw := float(_hash(cell.x, cell.y) % 4) * PI * .5
        var turn := Basis(Vector3.UP, yaw)
        var basis := turn * base_scale
        var ground_y := .008 - box.position.y * sy
        var origin := Vector3(float(cell.x) * CELL, ground_y, float(cell.y) * CELL)
        origin -= basis * Vector3(box.get_center().x, 0, box.get_center().z)
        multimesh.set_instance_transform(index, Transform3D(basis, origin))
        # Rotate the neighbor test with the actual per-instance mesh orientation.
        var local_directions: Array[Vector3] = [Vector3(-1,0,0), Vector3(1,0,0),
                                 Vector3(0,0,-1), Vector3(0,0,1)]
        var flags: Array[float] = []
        for local_dir in local_directions:
            var neighbor: Vector3 = turn * local_dir
            var nx := cell.x + roundi(neighbor.x)
            var nz := cell.y + roundi(neighbor.z)
            flags.append(0.0 if _cell_enabled(nx, nz) else 1.0)
        multimesh.set_instance_custom_data(index, Color(flags[0], flags[1], flags[2], flags[3]))
    var batch := MultiMeshInstance3D.new()
    batch.name = kind.capitalize() + "Ground"
    batch.multimesh = multimesh
    var source_material := original.mesh.surface_get_material(0) as StandardMaterial3D
    if source_material != null:
        batch.material_override = _feathered_material(kind, source_material, box)
    add_child(batch)
    terrain_counts[kind] = positions.size()
    source.queue_free()
