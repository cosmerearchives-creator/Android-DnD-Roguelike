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
const DEFAULT_HAIR_COLOR := Color("#ffffff")
static func hair_color(data: Dictionary) -> Color:
    var value:Variant=data.get("hair_color",DEFAULT_HAIR_COLOR)
    return value if value is Color else DEFAULT_HAIR_COLOR

static func settings() -> Dictionary:
    var cfg := ConfigFile.new()
    cfg.load(SAVE)
    var selected:=clampi(int(cfg.get_value("appearance","hair",0)),0,HAIR.size()-1)
    var old_offset:=_read_offset(cfg,"hair_offset")
    var old_size:=clampf(float(cfg.get_value("appearance","hair_scale",1.0)),0.5,3.0)
    var offsets:Array=[]
    var scales:Array=[]
    for i in range(HAIR.size()):
        var offset_key:="hair_offset_%d" % i
        var scale_key:="hair_scale_%d" % i
        offsets.append(_read_offset(cfg,offset_key) if cfg.has_section_key("appearance",offset_key) else (old_offset if i==selected else Vector3.ZERO))
        scales.append(clampf(float(cfg.get_value("appearance",scale_key,old_size if i==selected else 1.0)),0.5,3.0))
    return {"hair":selected,
        "beard":clampi(int(cfg.get_value("appearance","beard",0)),0,BEARDS.size()-1),
        "height_scale":clampf(float(cfg.get_value("appearance","height_scale",1.0)),0.7,1.3),
        "frame_scale":clampf(float(cfg.get_value("appearance","frame_scale",1.0)),0.5,1.6),
        "weight_scale":clampf(float(cfg.get_value("appearance","weight_scale",1.0)),0.5,1.3),
        "hair_offset":offsets[selected],
        "hair_scale":scales[selected],
        "hair_offsets":offsets,
        "hair_scales":scales,
        "hair_color":cfg.get_value("appearance","hair_color",DEFAULT_HAIR_COLOR),
        "beard_offset":_read_offset(cfg,"beard_offset")}

static func _read_offset(cfg: ConfigFile,key: String) -> Vector3:
    var raw: Variant=cfg.get_value("appearance",key,Vector3.ZERO)
    if raw is Vector3:return raw
    return Vector3.ZERO

## Preserve each hairstyle's manual XYZ trim and size separately. Legacy saves
## are assigned only to the hairstyle that was actually selected.
static func remember_current_hair(data: Dictionary) -> void:
    var selected:=clampi(int(data.get("hair",0)),0,HAIR.size()-1)
    var offsets:Array=data.get("hair_offsets",[])
    if offsets.size()!=HAIR.size():
        offsets=[]
        for i in range(HAIR.size()):offsets.append(Vector3.ZERO)
    var sizes:Array=data.get("hair_scales",[])
    if sizes.size()!=HAIR.size():
        sizes=[]
        for i in range(HAIR.size()):sizes.append(1.0)
    var raw_offset:Variant=data.get("hair_offset",Vector3.ZERO)
    offsets[selected]=raw_offset if raw_offset is Vector3 else Vector3.ZERO
    sizes[selected]=clampf(float(data.get("hair_scale",1.0)),0.5,3.0)
    data["hair_offsets"]=offsets
    data["hair_scales"]=sizes

static func select_hair(data: Dictionary,index: int) -> void:
    remember_current_hair(data)
    var next_index:=clampi(index,0,HAIR.size()-1)
    data["hair"]=next_index
    data["hair_offset"]=data["hair_offsets"][next_index]
    data["hair_scale"]=data["hair_scales"][next_index]

static func save(data: Dictionary) -> void:
    remember_current_hair(data)
    var cfg := ConfigFile.new()
    for key in ["hair","beard","hair_offset","beard_offset"]:
        cfg.set_value("appearance",key,data.get(key,Vector3.ZERO if key.ends_with("offset") else 0))
    cfg.set_value("appearance","hair_scale",data.get("hair_scale",1.0))
    cfg.set_value("appearance","hair_color",hair_color(data))
    for i in range(HAIR.size()):
        cfg.set_value("appearance","hair_offset_%d" % i,data["hair_offsets"][i])
        cfg.set_value("appearance","hair_scale_%d" % i,data["hair_scales"][i])
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
## Some source hairstyles use an entirely different mesh orientation and shape.
## Keep each style's calibrated attachment independent from the user's XYZ trim.
const HAIR_FIT_POSITION := [
    Vector3.ZERO,
    Vector3(0.0,0.0,-0.055),
    Vector3(0.0,0.0,-0.090),
    Vector3(0.0,0.0,-0.095),
]
## Manual offsets are applied after these mesh-specific defaults.
## The supplied meshes and their original orientations remain unchanged.
static func base_position(kind: String,index: int,factor: float) -> Vector3:
    var target_y:float=SCALP_TOP if kind=="hair" else BEARD_TOP
    var forward:float=0.120 if kind=="beard" else 0.0
    if kind=="hair":
        var calibration:Vector3=HAIR_FIT_POSITION[index]
        return Vector3(calibration.x,target_y-float(MESH_TOP[kind][index])*factor+calibration.y,calibration.z)
    return Vector3(0,target_y-float(MESH_TOP[kind][index])*factor,forward)

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

## Unique runtime overrides keep the original imported materials, maps and
## texture resolution intact; never recolor or replace source textures.
static func _tint_hair_meshes(node: Node, color: Color) -> void:
    if node is MeshInstance3D:
        var mesh_node:=node as MeshInstance3D
        if mesh_node.mesh!=null:
            for i in range(mesh_node.mesh.get_surface_count()):
                var source:Material=mesh_node.mesh.surface_get_material(i)
                var override:Material=mesh_node.get_surface_override_material(i)
                if override==null:
                    if source!=null:
                        override=source.duplicate()
                    else:
                        override=StandardMaterial3D.new()
                    mesh_node.set_surface_override_material(i,override)
                if override is BaseMaterial3D:
                    (override as BaseMaterial3D).albedo_color=color
    for child in node.get_children():
        _tint_hair_meshes(child,color)

static func update_hair_color(character: Node3D,data: Dictionary) -> void:
    var attach:=character.find_child("ModularAppearance",true,false) as BoneAttachment3D
    if attach==null:return
    var hair:=attach.get_node_or_null("Hair")
    if hair!=null:_tint_hair_meshes(hair,hair_color(data))

static func apply(character: Node3D,data: Dictionary={}) -> void:
    if data.is_empty():data=settings()
    var skeleton:=_skeleton(character)
    if skeleton==null:return
    var bone:=_head_bone(skeleton)
    if bone<0:return
    var attachment:=character.find_child("ModularAppearance",true,false) as BoneAttachment3D
    if attachment==null or attachment.get_parent()!=skeleton or attachment.bone_idx!=bone:
        clear(character)
        attachment=BoneAttachment3D.new()
        attachment.name="ModularAppearance"
        skeleton.add_child(attachment)
        attachment.bone_idx=bone
    for kind in ["hair","beard"]:
        var index:=clampi(int(data.get(kind,0)),0,FILES[kind].size()-1)
        var path:String=FILES[kind][index]
        var name:String=kind.capitalize()
        var existing:=attachment.get_node_or_null(name) as Node3D
        if index==0 or not ResourceLoader.exists(path):
            if existing!=null:
                attachment.remove_child(existing)
                existing.queue_free()
            continue
        if existing!=null and str(existing.get_meta("appearance_asset_path",""))==path:
            continue
        if existing!=null:
            attachment.remove_child(existing)
            existing.queue_free()
        var instance:Node3D=(load(path) as PackedScene).instantiate()
        instance.name=name
        instance.set_meta("appearance_asset_path",path)
        attachment.add_child(instance)
    update_placement(character,data)
    update_hair_color(character,data)
