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
    // Illustration cells contain transparent margins. Use one measured envelope
    // for both base and armor so their pixels remain registered on the same rig.
    private final Rect[][] visibleCells=new Rect[4][5];

    ElfLayerSprites(Context context) {
        base=decode(context,"elf_base_layers.webp");
        armor=decode(context,"elf_armor_layers.webp");
        for(int facing=0;facing<4;facing++){
            for(int part=0;part<5;part++)
                visibleCells[facing][part]=measureCell(facing,part);
        }
    }

    private static Bitmap decode(Context ctx,String name) {
        try(java.io.InputStream stream=ctx.getAssets().open(name)){
            Bitmap result=BitmapFactory.decodeStream(stream);
            return result!=null && result.getWidth()==CELL*4
                && result.getHeight()==CELL*5 ? result : null;
        }catch(java.io.IOException ignored){return null;}
    }

    /** Scan alpha only at initialization, not during the animation loop. */
    private Rect measureCell(int facing,int part){
        int column=facing==3?1:facing;
        int ox=column*CELL,oy=part*CELL;
        int minX=CELL,minY=CELL,maxX=-1,maxY=-1;
        int[] pixels=new int[CELL*CELL];
        for(Bitmap atlas:new Bitmap[]{base,armor}){
            if(atlas==null)continue;
            atlas.getPixels(pixels,0,CELL,ox,oy,CELL,CELL);
            for(int y=0;y<CELL;y++){
                for(int x=0;x<CELL;x++){
                    if((pixels[y*CELL+x]>>>24)<24)continue;
                    if(x<minX)minX=x;
                    if(y<minY)minY=y;
                    if(x>maxX)maxX=x;
                    if(y>maxY)maxY=y;
                }
            }
        }
        if(maxX<minX||maxY<minY)
            return new Rect(ox,oy,ox+CELL,oy+CELL);
        return new Rect(ox+Math.max(0,minX-2),oy+Math.max(0,minY-2),
            ox+Math.min(CELL,maxX+3),oy+Math.min(CELL,maxY+3));
    }

    boolean ready(){return base!=null;}
    boolean armorReady(){return armor!=null;}

    private void paintOne(Canvas canvas,Bitmap atlas,int facing,int part,
                          float left,float top,float right,float bottom) {
        if(atlas==null)return;
        // Art is mirrored from the right-facing source into left-facing space.
        src.set(visibleCells[Math.max(0,Math.min(3,facing))][part]);
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
        // Bone endpoints are joints, not art-cell boundaries.
        float seam=part==THIGH||part==SHIN?2.7f:1.8f;
        paintOne(canvas,base,facing,part,-half,-seam,half,length+seam);
        if(armored)paintOne(canvas,armor,facing,part,
                           -half-1f,-seam,half+1f,length+seam);
        canvas.restore();
        return true;
    }
}
