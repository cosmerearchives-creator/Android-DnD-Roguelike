package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.LruCache;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;

/** Temporary Free Roam controls and eight-direction weapon animation art. */
final class WeaponTrials {
    enum Motion {
        UNARMED("Unarmed", "unarmed", ""),
        SWING("One-hand swing", "swing", "longsword", "scimitar", "battleaxe", "warhammer", "quarterstaff"),
        THRUST("One-hand thrust", "thrust", "dagger", "rapier", "spear", "trident"),
        HEAVY("Two-hand swing", "heavy", "greatsword", "greataxe", "maul", "longsword", "battleaxe", "warhammer", "quarterstaff", "spear", "trident"),
        BOW("Bow shot", "bow", "shortbow", "longbow");

        final String title, asset;
        final String[] weapons;
        Motion(String title, String asset, String... weapons) {
            this.title=title;
            this.asset=asset;
            this.weapons=weapons;
        }
    }

    private static final int WIDTH=144, HEIGHT=164, FRAMES=5, GRIP_VALUES=5;
    // Relative depth in Blender units. A thin torso margin keeps the weapon
    // behind the body until it actually clears the near side.
    private static final float BODY_DEPTH=.06f;
    private static final float WEAPON_CLIP=256f;
    private static final float ANCHOR=136.67f;
    private static final float BOW_ART_COS=.8910065f, BOW_ART_SIN=.4539905f;
    private final Context context;
    private final PetiteAppearance petite;
    private final Rect source = new Rect();
    private final RectF destination = new RectF();
    private final LruCache<String,Bitmap> poses = new LruCache<>(5);
    private final HashMap<String,Bitmap> icons = new HashMap<>();
    private final HashMap<String,float[]> grips = new HashMap<>();
    private final HashMap<String,float[]> iconGrips = new HashMap<>();
    private final HashMap<String,float[]> bowTips = new HashMap<>();
    private final Paint stringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float[] petiteDrawHand;

    WeaponTrials(Context context,PetiteAppearance petite) {
        this.context=context;
        this.petite=petite;
    }

    private Bitmap load(String file) {
        try (InputStream input=context.getAssets().open("weapon_trials/"+file)) {
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            return null;
        }
    }

    private float[] read(String file, int count) {
        try (InputStream input=context.getAssets().open("weapon_trials/"+file);
             Scanner scanner=new Scanner(input).useLocale(java.util.Locale.US)) {
            float[] values=new float[count];
            for (int i=0;i<count;i++) {
                if (!scanner.hasNextFloat()) return null;
                values[i]=scanner.nextFloat();
            }
            return values;
        } catch (IOException e) {
            return null;
        }
    }

    boolean draw(Canvas canvas, Paint paint, int style, Motion motion, String weapon,
                 int facing, float progress, float x, float y, float scale) {
        return draw(canvas,paint,style,motion,weapon,facing,progress,x,y,scale,false);
    }

    boolean draw(Canvas canvas, Paint paint, int style, Motion motion, String weapon,
                 int facing, float progress, float x, float y, float scale, boolean idleHeld) {
        String sheetName=style+"_"+motion.asset;
        int frame=Math.max(0,Math.min(FRAMES-1,Math.round(progress*(FRAMES-1))));
        if (style==1 && motion==Motion.BOW && !idleHeld)
            frame=petiteBowFrame(progress);
        String bitmapName=sheetName;
        String gripSheet=sheetName;
        int gripFrames=FRAMES;
        if (style==1 && progress<=-1f) {
            bitmapName="1_walk";
            gripSheet=motion==Motion.BOW ? "1_walk_left" : "1_walk_right";
            gripFrames=8;
            frame=Math.min(7,(int)(8*(-progress-1)));
        }
        if (style==4 && progress<0) {
            boolean walking=progress < -1f;
            bitmapName=walking ? "4_walk" : "4_idle";
            frame=walking ? Math.min(7,(int)(8*(-progress-1))) : 0;
        }
        Bitmap sheet=poses.get(bitmapName);
        if (sheet==null) {
            sheet=load(bitmapName+".webp");
            if (sheet==null) return false;
            if (style==1) {
                String module=bitmapName.equals("1_walk") ? "armed_walk" : motion.asset;
                sheet=petite.apply(context,sheet,module);
            }
            poses.put(bitmapName,sheet);
        }
        float top=y-ANCHOR*scale, left=x-WIDTH*.5f*scale;
        source.set(frame*WIDTH,facing*HEIGHT,(frame+1)*WIDTH,(facing+1)*HEIGHT);
        int gripFrame=style==4 && progress<0 ? 0 : frame;
        drawHeld(canvas,paint,gripSheet,weapon,facing,gripFrame,gripFrames,left,top,scale,idleHeld,false);
        destination.set(left,top,left+WIDTH*scale,top+HEIGHT*scale);
        canvas.drawBitmap(sheet,source,destination,paint);
        drawHeld(canvas,paint,gripSheet,weapon,facing,gripFrame,gripFrames,left,top,scale,idleHeld,true);
        return true;
    }

    private static int petiteBowFrame(float progress) {
        if (progress<.13f) return 0;
        if (progress<.28f) return 1;
        if (progress<.5f) return 2;
        if (progress<.76f) return 3;
        return 4;
    }

    private void drawHeld(Canvas canvas, Paint paint, String sheet, String weapon, int facing,
                          int frame,int gripFrames,float left,float top,float scale,boolean idleHeld,boolean foreground) {
        if (weapon.isEmpty()) return;
        if (!grips.containsKey(sheet)) grips.put(sheet,read(sheet+".txt",8*gripFrames*GRIP_VALUES));
        boolean animateString=sheet.equals("1_bow") && !idleHeld;
        String iconKey=animateString ? weapon+"_limbs" : weapon;
        if (!icons.containsKey(iconKey)) icons.put(iconKey,load(iconKey+".webp"));
        if (!iconGrips.containsKey(weapon)) iconGrips.put(weapon,read(weapon+".txt",3));
        float[] anchors=grips.get(sheet), iconAnchor=iconGrips.get(weapon);
        Bitmap icon=icons.get(iconKey);
        if (anchors==null) return;
        if (weapon.equals("quarterstaff") && icon==null) {
            int a=(facing*gripFrames+frame)*GRIP_VALUES;
            if ((anchors[a+3]>BODY_DEPTH)!=foreground)return;
            canvas.save();
            canvas.translate(left+anchors[a]*scale,top+anchors[a+1]*scale);
            canvas.rotate(idleHeld?172f:anchors[a+2]);
            Paint staff=new Paint(Paint.ANTI_ALIAS_FLAG);
            staff.setColor(0xFF503421);staff.setStrokeWidth(4f*scale);staff.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(0,-45*scale,0,45*scale,staff);
            staff.setColor(0xFFAA8050);staff.setStrokeWidth(1.5f*scale);
            canvas.drawLine(-scale,-44*scale,-scale,44*scale,staff);
            canvas.restore();return;
        }
        if (icon==null || iconAnchor==null) return;

        int anchor=(facing*gripFrames+frame)*GRIP_VALUES;
        float gripX=anchors[anchor];
        float angle=idleHeld ? (gripX>=WIDTH*.5f ? 172f : -172f) : anchors[anchor+2];
        float depth=anchors[anchor+3];
        float slope=idleHeld ? 0f : anchors[anchor+4];
        float lower=-WEAPON_CLIP, upper=WEAPON_CLIP;

        if (Math.abs(slope)<.0001f) {
            if ((depth>BODY_DEPTH)!=foreground) return;
        } else {
            // In weapon space negative Y points toward the tip. A crossing
            // swing can have its grip behind the body and its tip in front.
            float split=(depth-BODY_DEPTH)/slope;
            if ((slope>0f)==foreground) upper=Math.min(upper,split);
            else lower=Math.max(lower,split);
            if (upper<=lower) return;
        }

        canvas.save();
        canvas.translate(left+gripX*scale,top+anchors[anchor+1]*scale);
        canvas.rotate(angle);
        canvas.clipRect(-WEAPON_CLIP*scale,lower*scale,WEAPON_CLIP*scale,upper*scale);
        float factor=iconAnchor[2]*scale/Math.max(icon.getWidth(),icon.getHeight());
        float l=-iconAnchor[0]*factor, t=-iconAnchor[1]*factor;
        if (animateString) {
            float projection=drawBowString(canvas,weapon,facing,frame,anchors,anchor,angle,scale,factor,l,t);
            canvas.scale(projection,1f);
        }
        canvas.rotate(-27f); // Inventory art was rendered at a 27-degree tilt.
        destination.set(l,t,l+icon.getWidth()*factor,t+icon.getHeight()*factor);
        canvas.drawBitmap(icon,null,destination,paint);
        canvas.restore();
    }

    private float drawBowString(Canvas canvas, String weapon, int facing, int frame,
                                float[] grips, int gripIndex, float angle, float scale,
                                float factor, float left, float top) {
        if (!bowTips.containsKey(weapon)) bowTips.put(weapon,read(weapon+"_string.txt",4));
        float[] tips=bowTips.get(weapon);
        if (tips==null) return 1f;
        if (petiteDrawHand==null) petiteDrawHand=read("1_bow_draw.txt",8*FRAMES*2);
        double radians=Math.toRadians(angle);
        float cos=(float)Math.cos(radians), sin=(float)Math.sin(radians);
        float artX1=left+tips[0]*factor, artY1=top+tips[1]*factor;
        float artX2=left+tips[2]*factor, artY2=top+tips[3]*factor;
        float x1=BOW_ART_COS*artX1+BOW_ART_SIN*artY1;
        float x2=BOW_ART_COS*artX2+BOW_ART_SIN*artY2;
        float y1=BOW_ART_COS*artY1-BOW_ART_SIN*artX1;
        float y2=BOW_ART_COS*artY2-BOW_ART_SIN*artX2;
        float projection=1f;
        if (petiteDrawHand!=null) {
            int fullDraw=facing*FRAMES+2, fullGrip=fullDraw*GRIP_VALUES;
            float dx=petiteDrawHand[fullDraw*2]-grips[fullGrip];
            float dy=petiteDrawHand[fullDraw*2+1]-grips[fullGrip+1];
            float sideways=(dx*cos+dy*sin)*scale;
            float restOffset=Math.abs((x1+x2)*.5f);
            // The draw hand is behind the bow. A near head-on view narrows
            // the bow's profile and the opposite view mirrors its limbs.
            projection=Math.copySign(Math.min(1f,Math.abs(sideways)/(restOffset+4f*scale)),sideways);
        }
        x1*=projection;
        x2*=projection;
        float midX=(x1+x2)*.5f, midY=(y1+y2)*.5f;
        boolean drawing=(frame==1 || frame==2) && petiteDrawHand!=null;
        if (drawing) {
            int hand=(facing*FRAMES+frame)*2;
            float dx=(petiteDrawHand[hand]-grips[gripIndex])*scale;
            float dy=(petiteDrawHand[hand+1]-grips[gripIndex+1])*scale;
            float projectedX=dx*cos+dy*sin;
            if ((projectedX-midX)*projection>0f) {
                midX=projectedX;
                midY=dy*cos-dx*sin;
            }
        }
        stringPaint.setColor(0xff9bb5c3);
        stringPaint.setStrokeWidth(Math.max(.65f,scale));
        canvas.drawLine(x1,y1,midX,midY,stringPaint);
        canvas.drawLine(midX,midY,x2,y2,stringPaint);
        if (drawing) drawNockedArrow(canvas,midX,midY,scale);
        return projection;
    }

    private void drawNockedArrow(Canvas canvas, float nockX, float nockY, float scale) {
        // The shaft crosses the bow just above the grip and projects beyond it.
        float restY=-4f*scale;
        float dx=-nockX, dy=restY-nockY;
        float distance=(float)Math.hypot(dx,dy);
        if (distance<scale) return;
        float length=36f*scale;
        float tipX=nockX+dx*length/distance, tipY=nockY+dy*length/distance;
        stringPaint.setColor(0xffb89561);
        canvas.drawLine(nockX,nockY,tipX,tipY,stringPaint);
        stringPaint.setColor(0xffd5dce0);
        canvas.drawLine(tipX-dx*4f*scale/distance,tipY-dy*4f*scale/distance,tipX,tipY,stringPaint);
    }
}
