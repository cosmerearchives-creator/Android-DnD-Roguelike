extends StaticBody3D
## Imported open-roof tavern hall, kept at original full material/mesh fidelity.
## Source has a small door but a full walkable interior; scale architecture
## independently from playable rigs so even a future eight-foot race fits.
const TAVERN_PATH := "res://assets/terrain/tavern_room.glb"
const FOOT := 0.3048
const DOOR_WIDTH_M := 2.10
const DOOR_HEIGHT_M := 2.90
const TALL_PLAYABLE_REFERENCE_M := 8.0 * FOOT
const MODEL_FOOTPRINT_SCALE := 9.0
const MODEL_HEIGHT_SCALE := 13.0
var source_visual: Node3D

func _ready() -> void:
    name = "Tavern"
    collision_layer = 1
    collision_mask = 1
    source_visual = (load(TAVERN_PATH) as PackedScene).instantiate() as Node3D
    source_visual.name = "ImportedTavernRoom"
    add_child(source_visual)
    source_visual.scale = Vector3(MODEL_FOOTPRINT_SCALE, MODEL_HEIGHT_SCALE, MODEL_FOOTPRINT_SCALE)
    # The imported hardwood surface is at source model Y=-0.247m.
    # Sinking the bottom stone footings keeps the floor level with the hub.
    source_visual.position.y = .247 * MODEL_HEIGHT_SCALE + .020
    # Explicit wall colliders instead of a 415k-vertex triangle collider.
    # Local +Z faces the real model's door; root is turned to face the spawn.
    _box("RearWall", Vector3(0, 3.10, -7.12), Vector3(16.4, 6.2, .60))
    _box("WestWall", Vector3(-8.30, 3.10, 0), Vector3(.60, 6.2, 14.6))
    _box("EastWall", Vector3(8.30, 3.10, 0), Vector3(.60, 6.2, 14.6))
    # Leave a 2.1m physical gap around the original doorway, wider than the
    # reference 8-ft character capsule. Do not cover it with a trigger/collider.
    _box("EntranceWestWall", Vector3(-4.55, 3.10, 7.12), Vector3(7.50, 6.2, .60))
    _box("EntranceEastWall", Vector3(4.80, 3.10, 7.12), Vector3(7.00, 6.2, .60))
    _box("CentralHearth", Vector3(0, 1.00, 0), Vector3(2.60, 2.0, 2.45))

func _box(label: String, at: Vector3, size: Vector3) -> void:
    var shape := BoxShape3D.new()
    shape.size = size
    var collider := CollisionShape3D.new()
    collider.name = label
    collider.shape = shape
    collider.position = at
    add_child(collider)
