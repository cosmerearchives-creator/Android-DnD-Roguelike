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
        SWING("One-hand swing", "swing", "longsword", "scimitar", "battleaxe", "warhammer"),
        THRUST("One-hand thrust", "thrust", "dagger", "rapier", "spear"),
        HEAVY("Two-hand swing", "heavy", "greatsword", "greataxe", "maul"),
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
    private final Context context;
    private final Rect source = new Rect();
    private final RectF destination = new RectF();
    private final LruCache<String,Bitmap> poses = new LruCache<>(5);
    private final HashMap<String,Bitmap> icons = new HashMap<>();
    private final HashMap<String,float[]> grips = new HashMap<>();
    private final HashMap<String,float[]> iconGrips = new HashMap<>();

    WeaponTrials(Context context) { this.context=context; }

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
        String bitmapName=sheetName;
        if (style==4 && progress<0) {
            boolean walking=progress < -1f;
            bitmapName=walking ? "4_walk" : "4_idle";
            frame=walking ? Math.min(7,(int)(8*(-progress-1))) : 0;
        }
        Bitmap sheet=poses.get(bitmapName);
        if (sheet==null) {
            sheet=load(bitmapName+".webp");
            if (sheet==null) return false;
            poses.put(bitmapName,sheet);
        }
        float top=y-ANCHOR*scale, left=x-WIDTH*.5f*scale;
        source.set(frame*WIDTH,facing*HEIGHT,(frame+1)*WIDTH,(facing+1)*HEIGHT);
        int gripFrame=progress<0 ? 0 : frame;
        drawHeld(canvas,paint,sheetName,weapon,facing,gripFrame,left,top,scale,idleHeld,false);
        destination.set(left,top,left+WIDTH*scale,top+HEIGHT*scale);
        canvas.drawBitmap(sheet,source,destination,paint);
        drawHeld(canvas,paint,sheetName,weapon,facing,gripFrame,left,top,scale,idleHeld,true);
        return true;
    }

    private void drawHeld(Canvas canvas, Paint paint, String sheet, String weapon, int facing,
                          int frame,float left,float top,float scale,boolean idleHeld,boolean foreground) {
        if (weapon.isEmpty()) return;
        if (!grips.containsKey(sheet)) grips.put(sheet,read(sheet+".txt",8*FRAMES*GRIP_VALUES));
        if (!icons.containsKey(weapon)) icons.put(weapon,load(weapon+".webp"));
        if (!iconGrips.containsKey(weapon)) iconGrips.put(weapon,read(weapon+".txt",3));
        float[] anchors=grips.get(sheet), iconAnchor=iconGrips.get(weapon);
        Bitmap icon=icons.get(weapon);
        if (anchors==null || icon==null || iconAnchor==null) return;

        int anchor=(facing*FRAMES+frame)*GRIP_VALUES;
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
        canvas.rotate(-27f); // Inventory art was rendered at a 27-degree tilt.
        float factor=iconAnchor[2]*scale/Math.max(icon.getWidth(),icon.getHeight());
        float l=-iconAnchor[0]*factor, t=-iconAnchor[1]*factor;
        destination.set(l,t,l+icon.getWidth()*factor,t+icon.getHeight()*factor);
        canvas.drawBitmap(icon,null,destination,paint);
        canvas.restore();
    }
}
