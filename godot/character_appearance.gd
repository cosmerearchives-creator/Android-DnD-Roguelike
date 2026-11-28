extends RefCounted
## One persisted appearance for creator, world character and inventory viewport.
const HAIR := ["None", "Wavy", "Short", "Braid"]
const BEARDS := ["None", "Full Beard"]
const FILES := {
    "hair": ["", "res://assets/appearance/hair_wavy.glb", "res://assets/appearance/hair_short.glb", "res://assets/appearance/hair_braid.glb"],
    "beard": ["", "res://assets/appearance/beard_full.glb"]
}
const DIMENSIONS := {
    "hair": [Vector3.ZERO, Vector3(0.903253,1.898732,1.034521), Vector3(1.501402,1.573253,1.903303), Vector3(1.437447,1.896282,1.600468)],
    "beard": [Vector3.ZERO, Vector3(1.473436,1.906936,0.674502)]
}
const SAVE := "user://appearance.cfg"
static func settings() -> Dictionary:
    var cfg := ConfigFile.new()
    cfg.load(SAVE)
    return {"hair":clampi(int(cfg.get_value("appearance","hair",0)),0,HAIR.size()-1),
        "beard":clampi(int(cfg.get_value("appearance","beard",0)),0,BEARDS.size()-1),
        "height_scale":clampf(float(cfg.get_value("appearance","height_scale",1.0)),0.7,1.3),
        "frame_scale":clampf(float(cfg.get_value("appearance","frame_scale",1.0)),0.5,1.6),
        "weight_scale":clampf(float(cfg.get_value("appearance","weight_scale",1.0)),0.5,1.3),
        "hair_scale":clampf(float(cfg.get_value("appearance","hair_scale",1.0)),0.5,3.0),
        "hair_offset":_read_offset(cfg,"hair_offset"),
        "beard_offset":_read_offset(cfg,"beard_offset")}
static func _read_offset(cfg: ConfigFile,key: String) -> Vector3:
    var raw: Variant=cfg.get_value("appearance",key,Vector3.ZERO)
    if raw is Vector3:return raw
    return Vector3.ZERO
static func save(data: Dictionary) -> void:
    var cfg := ConfigFile.new()
    for key in ["hair","beard","hair_offset","beard_offset"]:
        cfg.set_value("appearance",key,data.get(key,Vector3.ZERO if key.ends_with("offset") else 0))
    cfg.set_value("appearance","hair_scale",data.get("hair_scale",1.0))
    for key in ["height_scale","frame_scale","weight_scale"]:
        cfg.set_value("appearance",key,data.get(key,1.0))
    cfg.save(SAVE)
static func body_scale(data: Dictionary) -> Vector3:
    var height:=clampf(float(data.get("height_scale",1.0)),0.7,1.3)
    return Vector3(height*clampf(float(data.get("frame_scale",1.0)),0.5,1.6),height,height*clampf(float(data.get("weight_scale",1.0)),0.5,1.3))
static func _skeleton(parent: Node) -> Skeleton3D:
    if parent is Skeleton3D:return parent
    for child in parent.get_children():
        var found:=_skeleton(child)
        if found!=null:return found
    return null
static func _head_bone(skeleton: Skeleton3D) -> int:
    for i in range(skeleton.get_bone_count()):
        var name:=skeleton.get_bone_name(i).to_lower()
        if name=="head" or name.ends_with(":head") or name.ends_with("_head"):return i
    if skeleton.get_bone_count()==93:return skeleton.find_bone("Bone_011")
    return -1
static func clear(character: Node3D) -> void:
    var existing:=character.find_child("ModularAppearance",true,false)
    if existing!=null:
        existing.get_parent().remove_child(existing)
        existing.queue_free()
## Mesh asset coordinates are centered at their own bounding boxes, not at
## the player's scalp. These measured source-AABB Y maxima (meters) give us
## a stable crown/jaw fit without changing a single vertex or skeleton bone.
const MESH_TOP := {
    "hair": [0.0,0.9471683,0.7889832,0.9483418],
    "beard": [0.0,0.9541904]
}
## The head bone on the supplied 66-joint model is below the scalp crown.
## Face-forward is +Z (verified by the original "headfront" bone).
const SCALP_TOP := 0.19
const BEARD_TOP := 0.03
const OFFSET_LIMIT := 0.25

static func base_position(kind: String,index: int,factor: float) -> Vector3:
    var top_y:float=SCALP_TOP if kind=="hair" else BEARD_TOP
    var forward:float=-0.055 if kind=="hair" else 0.120
    return Vector3(0.0,top_y-float(MESH_TOP[kind][index])*factor,forward)

static func mesh_factor(kind: String,index: int,data: Dictionary) -> float:
    var dims:Vector3=DIMENSIONS[kind][index]
    var width:float=0.27 if kind=="hair" else 0.18
    var factor:float=width/maxf(dims.x,0.01)
    if kind=="hair":factor*=clampf(float(data.get("hair_scale",1.0)),0.5,3.0)
    return factor

static func update_placement(character: Node3D,data: Dictionary) -> void:
    var attach:=character.find_child("ModularAppearance",true,false) as BoneAttachment3D
    if attach==null:return
    for kind in ["hair","beard"]:
        var instance:=attach.get_node_or_null(kind.capitalize()) as Node3D
        if instance==null:continue
        var index:=clampi(int(data.get(kind,0)),0,FILES[kind].size()-1)
        if index==0:continue
        var factor:=mesh_factor(kind,index,data)
        var raw_offset:Variant=data.get(kind+"_offset",Vector3.ZERO)
        var user_offset:Vector3=raw_offset if raw_offset is Vector3 else Vector3.ZERO
        user_offset.x=clampf(user_offset.x,-OFFSET_LIMIT,OFFSET_LIMIT)
        user_offset.y=clampf(user_offset.y,-OFFSET_LIMIT,OFFSET_LIMIT)
        user_offset.z=clampf(user_offset.z,-OFFSET_LIMIT,OFFSET_LIMIT)
        instance.scale=Vector3.ONE*factor
        instance.position=base_position(kind,index,factor)+user_offset

static func apply(character: Node3D,data: Dictionary={}) -> void:
    if data.is_empty():data=settings()
    clear(character)
    var skeleton:=_skeleton(character)
    if skeleton==null:return
    var bone:=_head_bone(skeleton)
    if bone<0:return
    var attachment:=BoneAttachment3D.new()
    attachment.name="ModularAppearance"
    skeleton.add_child(attachment)
    attachment.bone_idx=bone
    for kind in ["hair","beard"]:
        var index:=clampi(int(data.get(kind,0)),0,FILES[kind].size()-1)
        if index==0:continue
        var path: String=FILES[kind][index]
        if not ResourceLoader.exists(path):continue
        var instance:Node3D=(load(path) as PackedScene).instantiate()
        instance.name=kind.capitalize()
        attachment.add_child(instance)
        # Position is applied from the verified source mesh bounds below.
    update_placement(character,data)
