"""Rig the existing Warden and goblin alternate game-ready meshes.
Keep source GLBs intact; output rigged derivatives with Idle/Walk/Attack clips.
The body weight map is inferred from local anatomy and should receive visual review.
"""
import bpy, math, sys, pathlib, statistics
from mathutils import Vector
ROOT=pathlib.Path(__file__).resolve().parent.parent
ASSETS=ROOT/"godot"/"assets"
CASES=[
    ("emerald_blade_warden",1.42),
    ("goblin_alt_2",1.22),
    ("goblin_alt_3",1.24),
    ("goblin_alt_4",1.34),
]
def clamp(x): return max(0.0,min(1.0,x))
def smooth(x): x=clamp(x);return x*x*(3-2*x)
def make(name,target_height):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    source=ASSETS/(name+".glb")
    bpy.ops.import_scene.gltf(filepath=str(source))
    meshes=[o for o in bpy.context.scene.objects if o.type=="MESH"]
    assert len(meshes)==1,(name,len(meshes))
    obj=meshes[0]
    for mod in list(obj.modifiers):
        if mod.type in {"ARMATURE"}:obj.modifiers.remove(mod)
    bpy.context.view_layer.objects.active=obj
    bpy.ops.object.select_all(action="DESELECT");obj.select_set(True)
    bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
    coords=[v.co.copy() for v in obj.data.vertices]
    lo=min(p.z for p in coords); hi=max(p.z for p in coords)
    assert hi-lo>.3,(name,lo,hi)
    midz=lo+(hi-lo)*.55
    center_candidates=[p for p in coords if abs(p.z-midz)<(hi-lo)*.12]
    cx=statistics.median(p.x for p in center_candidates) if center_candidates else 0
    cy=statistics.median(p.y for p in center_candidates) if center_candidates else 0
    factor=target_height/(hi-lo)
    for vert in obj.data.vertices:
        vert.co=(vert.co-Vector((cx,cy,lo)))*factor
    obj.data.update()
    obj.name=name+"_RiggedMesh"
    height=target_height
    shoulder=height*.72
    pelvis=height*.42
    armscale=height*.19
    legspread=height*.105
    arm_data=bpy.data.armatures.new(name+"_HumanoidSkeleton")
    arm=bpy.data.objects.new(name+"_HumanoidRig",arm_data)
    bpy.context.collection.objects.link(arm)
    bpy.context.view_layer.objects.active=arm
    bpy.ops.object.select_all(action="DESELECT");arm.select_set(True)
    bpy.ops.object.mode_set(mode="EDIT")
    def bone(n,a,b,parent=None):
        eb=arm_data.edit_bones.new(n);eb.head=a;eb.tail=b
        if parent:eb.parent=arm_data.edit_bones[parent];eb.use_connect=False
    bone("Hips",(0,0,pelvis*.8),(0,0,pelvis))
    bone("Spine",(0,0,pelvis),(0,0,shoulder),"Hips")
    bone("Head",(0,0,shoulder),(0,0,height*.96),"Spine")
    for side,sgn in (("L",-1),("R",1)):
        bone(side+"UpperArm",(sgn*armscale*.64,0,shoulder*.97),(sgn*armscale*1.07,-.018*height,shoulder*.71),"Spine")
        bone(side+"Forearm",(sgn*armscale*1.07,-.018*height,shoulder*.71),(sgn*armscale*1.22,-.065*height,shoulder*.52),side+"UpperArm")
        bone(side+"Hand",(sgn*armscale*1.22,-.065*height,shoulder*.52),(sgn*armscale*1.25,-.078*height,shoulder*.44),side+"Forearm")
        bone(side+"Thigh",(sgn*legspread,0,pelvis),(sgn*legspread*1.12,-.02*height,height*.24),"Hips")
        bone(side+"Shin",(sgn*legspread*1.12,-.02*height,height*.24),(sgn*legspread*1.12,-.02*height,height*.065),side+"Thigh")
        bone(side+"Foot",(sgn*legspread*1.12,-.02*height,height*.065),(sgn*legspread*1.12,-.09*height,height*.025),side+"Shin")
    bpy.ops.object.mode_set(mode="OBJECT")
    groups={n:obj.vertex_groups.new(name=n) for n in arm_data.bones.keys()}
    # Shared weights at duplicated UV-seam vertices prevent visible tears when posed.
    coincident={}
    for i,v in enumerate(obj.data.vertices):
        key=tuple(round(k,5) for k in v.co)
        coincident.setdefault(key,[]).append(i)
    def weights(p):
        x,y,z=p
        h=height
        normalized=z/h
        side="L" if x<0 else "R"
        # Far-apart weapons and shields are rigidly attached to their respective hands.
        lateral=abs(x)/h
        if lateral>.38 and normalized>.17 and normalized<.84:
            return {side+"Hand":1.0}
        if normalized < .42:
            leg=smooth((lateral-.014)/.1)
            if normalized<.11:part=side+"Foot"
            elif normalized<.28:part=side+"Shin"
            else:part=side+"Thigh"
            return {"Hips":max(0,1-leg),part:leg}
        if normalized>.79:
            t=smooth((normalized-.74)/.12)
            return {"Spine":1-t,"Head":t}
        reach=smooth((lateral-.06)/.12)*(1-smooth((normalized-.68)/.12))
        hand=(1-smooth((normalized-.38)/.09))
        upper=smooth((normalized-.52)/.08)
        fore=max(0,1-hand-upper)
        hips=1-smooth((normalized-.39)/.13)
        return {"Hips":(1-reach)*hips,"Spine":(1-reach)*(1-hips),
                side+"UpperArm":reach*upper,side+"Forearm":reach*fore,side+"Hand":reach*hand}
    for ids in coincident.values():
        p=obj.data.vertices[ids[0]].co
        found=sorted(weights(p).items(),key=lambda pair:-pair[1])[:4]
        total=sum(max(0,v) for n,v in found)
        if total<=0:continue
        for n,v in found:
            if v>0.00001:groups[n].add(ids,v/total,"REPLACE")
    modifier=obj.modifiers.new("HumanoidSkin","ARMATURE")
    modifier.object=arm
    obj.parent=arm;obj.matrix_parent_inverse=arm.matrix_world.inverted()
    pb=arm.pose.bones
    for b in pb:b.rotation_mode="XYZ"
    def clip(n,frames):
        action=bpy.data.actions.new(n)
        arm.animation_data_create()
        arm.animation_data.action=action
        for frame,bones in frames:
            for b in pb:
                b.rotation_euler=(0,0,0)
            for bn,degrees in bones.items():
                pb[bn].rotation_euler=tuple(math.radians(q) for q in degrees)
            for b in pb:
                b.keyframe_insert(data_path="rotation_euler",frame=frame,group=b.name)
        arm.animation_data.action=None
        track=arm.animation_data.nla_tracks.new()
        track.name=n
        strip=track.strips.new(n,1,action)
        strip.action_frame_start=frames[0][0]
        strip.action_frame_end=frames[-1][0]
        track.mute=True
    clip("Idle",[(1,{}),(13,{"Spine":(1,0,0),"Head":(-1,0,0)}),(25,{})])
    clip("Walk",[(1,{"LThigh":(18,0,0),"RThigh":(-18,0,0),"LShin":(-9,0,0),"RShin":(13,0,0),"LUpperArm":(-11,0,0),"RUpperArm":(11,0,0)}),
                 (7,{"LShin":(12,0,0),"RShin":(12,0,0)}),
                 (13,{"LThigh":(-18,0,0),"RThigh":(18,0,0),"LShin":(13,0,0),"RShin":(-9,0,0),"LUpperArm":(11,0,0),"RUpperArm":(-11,0,0)}),
                 (19,{"LShin":(12,0,0),"RShin":(12,0,0)}),
                 (25,{"LThigh":(18,0,0),"RThigh":(-18,0,0),"LShin":(-9,0,0),"RShin":(13,0,0),"LUpperArm":(-11,0,0),"RUpperArm":(11,0,0)})])
    clip("Attack",[(1,{}),(7,{"LUpperArm":(-20,10,7),"LForearm":(-15,0,0),"Spine":(0,0,-4)}),
                   (12,{"LUpperArm":(24,-18,-12),"LForearm":(25,0,0),"Spine":(0,0,6)}),(22,{})])
    for b in pb:b.rotation_euler=(0,0,0)
    bpy.ops.object.select_all(action="DESELECT")
    arm.select_set(True);obj.select_set(True);bpy.context.view_layer.objects.active=arm
    dest=ASSETS/(name+"_rigged.glb")
    bpy.ops.export_scene.gltf(filepath=str(dest),export_format="GLB",use_selection=True,
        export_animations=True,export_animation_mode="NLA_TRACKS",export_nla_strips=True)
    print("RIGGED",name,"height",height,"vertices",len(obj.data.vertices),
          "bones",len(arm.data.bones),"output",dest.stat().st_size,flush=True)
for name,height in CASES:
    make(name,height)
