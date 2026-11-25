package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.LruCache;

/** Animated poses for the single Avatar. Gameplay decides when an action starts. */
final class AvatarActions {
    enum Clip {
        JOG("jog",.62f), SPRINT("sprint",.49f), INTERACT("interact",.65f),
        CAST_PREPARE("cast_prepare",.32f), CAST_RELEASE("cast_release",.28f),
        CAST_RECOVER("cast_recover",.27f), JUMP_START("jump_start",.20f),
        JUMP_AIR("jump_air",.33f), JUMP_LAND("jump_land",.22f),
        SIT("sit",.56f), SEATED("seated",1.25f), STAND("stand",.56f),
        ROLL("roll",.62f), DEFEAT("defeat",1.05f);

        final String path;
        final float duration;
        Clip(String path,float duration) { this.path=path; this.duration=duration; }
    }
    private final Context context;
    private final PetiteAppearance appearance;
    private final LruCache<Clip,Bitmap> sheets=new LruCache<>(4);
    private final Rect cell=new Rect();
    private final RectF bounds=new RectF();
    private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
    private Clip playing;
    private float elapsed;

    AvatarActions(Context context,PetiteAppearance appearance) {
        this.context=context;
        this.appearance=appearance;
    }

    void play(Clip clip) {
        playing=clip;
        elapsed=0f;
    }

    boolean active() { return playing!=null; }
    boolean seated() { return playing==Clip.SIT || playing==Clip.SEATED; }
    boolean allowsMotion() {
        return playing==null || playing==Clip.JUMP_START || playing==Clip.JUMP_AIR
            || playing==Clip.JUMP_LAND || playing==Clip.ROLL;
    }

    boolean update(float dt) {
        if (playing==null) return false;
        elapsed+=dt;
        if (elapsed<playing.duration) return false;
        boolean defeated=playing==Clip.DEFEAT;
        switch (playing) {
            case JUMP_START: play(Clip.JUMP_AIR); break;
            case JUMP_AIR: play(Clip.JUMP_LAND); break;
            case SIT:
            case SEATED: play(Clip.SEATED); break;
            default: play(null); break;
        }
        return defeated;
    }

    void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); }

    boolean draw(Canvas canvas,float x,float y,int facing) {
        return playing!=null && drawClip(canvas,playing,x,y,facing,elapsed/playing.duration);
    }

    boolean drawRun(Canvas canvas,float x,float y,int facing,int pace,float phase) {
        return drawClip(canvas,pace==1?Clip.JOG:Clip.SPRINT,x,y,facing,phase);
    }

    boolean drawCast(Canvas canvas,float x,float y,int facing,float remaining) {
        float impact=RigAttackPose.DURATION*.5f;
        if (remaining>impact)
            return drawClip(canvas,Clip.CAST_PREPARE,x,y,facing,1f-(remaining-impact)/impact);
        float elapsed=impact-remaining;
        float release=.16f;
        if (elapsed<release)
            return drawClip(canvas,Clip.CAST_RELEASE,x,y,facing,elapsed/release);
        return drawClip(canvas,Clip.CAST_RECOVER,x,y,facing,(elapsed-release)/(impact-release));
    }

    private boolean drawClip(Canvas canvas,Clip clip,float x,float y,int facing,float progress) {
        Bitmap sheet=sheets.get(clip);
        if (sheet==null) {
            Bitmap raw=PetiteAppearance.load(context,"avatar_actions/"+clip.path+".png");
            if (raw==null) return false;
            sheet=appearance.apply(context,raw,clip.path);
            if (sheet!=raw) raw.recycle();
            sheets.put(clip,sheet);
        }
        int frame=Math.max(0,Math.min(5,(int)(6*progress)));
        cell.set(frame*168,facing*192,(frame+1)*168,(facing+1)*192);
        float scale=clip==Clip.ROLL || clip==Clip.DEFEAT ? 2.78f/2.13f : 1f;
        int footAnchor=clip==Clip.ROLL ? 146 : clip==Clip.DEFEAT ? 132
            : clip==Clip.JUMP_LAND ? 148 : 160;
        bounds.set(x-84*scale,y-footAnchor*scale,x+84*scale,y+(192-footAnchor)*scale);
        canvas.drawBitmap(sheet,cell,bounds,paint);
        return true;
    }
}
