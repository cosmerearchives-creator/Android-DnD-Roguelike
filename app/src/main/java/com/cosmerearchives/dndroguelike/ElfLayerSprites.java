package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * The visual character's rig-independent, illustrated layers. Both the base
 * undergarment and armor assets share 4 direction columns and 5 body-part rows:
 * torso, upper arm, forearm, thigh, shin/boot.
 *
 * Bones, stride speed, knee positions and attack transforms are never changed by
 * the armor toggle. Base art always renders FIRST, armor over it at the same
 * anchors. Original approved portrait is handled separately by RiggedElf.
 */
final class ElfLayerSprites {
    static final int TORSO=0, UPPER_ARM=1, FOREARM=2, THIGH=3, SHIN=4;
    private static final int CELL=192;
    private final Bitmap base,armor;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Rect src=new Rect();
    private final RectF dest=new RectF();

    ElfLayerSprites(Context context) {
        base=decode(context,"elf_base_layers.webp");
        armor=decode(context,"elf_armor_layers.webp");
    }

    private static Bitmap decode(Context ctx,String name) {
        try(java.io.InputStream stream=ctx.getAssets().open(name)){
            Bitmap result=BitmapFactory.decodeStream(stream);
            return result!=null && result.getWidth()==CELL*4
                && result.getHeight()==CELL*5 ? result : null;
        }catch(java.io.IOException ignored){return null;}
    }

    boolean ready(){return base!=null;}
    boolean armorReady(){return armor!=null;}

    private void paintOne(Canvas canvas,Bitmap atlas,int facing,int part,
                          float left,float top,float right,float bottom) {
        if(atlas==null)return;
        // Art is mirrored from the right-facing source into left-facing space.
        int column=facing==3?1:Math.max(0,Math.min(3,facing));
        src.set(column*CELL,part*CELL,(column+1)*CELL,(part+1)*CELL);
        dest.set(left,top,right,bottom);
        paint.setAlpha(255);
        canvas.drawBitmap(atlas,src,dest,paint);
    }

    boolean torso(Canvas canvas,int facing,float halfWidth,boolean armored) {
        if(base==null)return false;
        float width=halfWidth*2f+4f;
        paintOne(canvas,base,facing,TORSO,-width*.5f,-122f,width*.5f,-66f);
        if(armored)paintOne(canvas,armor,facing,TORSO,-width*.55f,-124f,
                           width*.55f,-65f);
        return true;
    }

    boolean limb(Canvas canvas,int facing,int part,int index,
                 float x1,float y1,float x2,float y2,
                 float width,boolean armored) {
        if(base==null)return false;
        float dx=x2-x1,dy=y2-y1;
        float length=(float)Math.hypot(dx,dy);
        if(length<2f)return true;
        canvas.save();
        canvas.translate(x1,y1);
        canvas.rotate((float)Math.toDegrees(Math.atan2(dy,dx))-90f);
        // Two source-side limbs share one asset, independently attached to the rig.
        // Horizontal mirroring prevents a duplicate same-sided glove/knee pattern.
        if(index==1&&!RigViewProfile.isSide(facing))canvas.scale(-1f,1f);
        float half=width*.5f;
        paintOne(canvas,base,facing,part,-half,0f,half,length);
        if(armored)paintOne(canvas,armor,facing,part,-half-1f,-1f,half+1f,length+1f);
        canvas.restore();
        return true;
    }
}
