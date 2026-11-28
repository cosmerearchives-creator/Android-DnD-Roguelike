extends RefCounted
## One persisted appearance for creator, world character and inventory viewport.
const HAIR := ["None", "Wavy", "Short", "Braid"]
const BEARDS := ["None", "Full Beard"]
const FILES := {
    "hair": ["", "res://assets/appearance/hair_wavy.glb", "res://assets/appearance/hair_short.glb", "res://assets/appearance/hair_braid.glb"],
    "beard": ["", "res://assets/appearance/beard_full.glb"]
}
const DIMENSIONS := {
    "hair": [Vector3.ZERO, Vector3(0.9068,1.0322,1.8982), Vector3(1.5112,1.8989,1.5717), Vector3(1.4305,1.5976,1.8988)],
    "beard": [Vector3.ZERO, Vector3(1.4741,0.6722,1.9026)]
}
const SAVE := "user://appearance.cfg"
static func settings() -> Dictionary:
    var cfg := ConfigFile.new()
    cfg.load(SAVE)
    return {"hair":clampi(int(cfg.get_value("appearance","hair",0)),0,HAIR.size()-1),
        "beard":clampi(int(cfg.get_value("appearance","beard",0)),0,BEARDS.size()-1),
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
    cfg.save(SAVE)
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
        # Assets are normalized around their own mesh center; use physical dimensions
        # to scale for a typical adult head. The slider offsets are measured in meters.
        var dims:Vector3=DIMENSIONS[kind][index]
        var target_width:=0.27 if kind=="hair" else 0.23
        var factor:=target_width/maxf(dims.x,0.01)
        if kind=="hair":factor*=float(data.get("hair_scale",1.0))
        instance.scale=Vector3.ONE*factor
        var default_pos:=Vector3(0,0.09,0) if kind=="hair" else Vector3(0,-0.105,-0.055)
        instance.position=default_pos+Vector3(data.get(kind+"_offset",Vector3.ZERO))
