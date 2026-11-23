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

    private static final int WIDTH=144, HEIGHT=164, FRAMES=5;
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
        destination.set(left,top,left+WIDTH*scale,top+HEIGHT*scale);
        canvas.drawBitmap(sheet,source,destination,paint);
        drawHeld(canvas,paint,sheetName,weapon,facing,progress<0 ? 0 : frame,left,top,scale);
        return true;
    }

    private void drawHeld(Canvas canvas, Paint paint, String sheet, String weapon, int facing,
                          int frame,float left,float top,float scale) {
        if (weapon.isEmpty()) return;
        if (!grips.containsKey(sheet)) grips.put(sheet,read(sheet+".txt",8*FRAMES*3));
        if (!icons.containsKey(weapon)) icons.put(weapon,load(weapon+".webp"));
        if (!iconGrips.containsKey(weapon)) iconGrips.put(weapon,read(weapon+".txt",3));
        float[] anchors=grips.get(sheet), iconAnchor=iconGrips.get(weapon);
        Bitmap icon=icons.get(weapon);
        if (anchors==null || icon==null || iconAnchor==null) return;
        int anchor=(facing*FRAMES+frame)*3;
        float size=iconAnchor[2]*scale;
        canvas.save();
        canvas.translate(left+anchors[anchor]*scale,top+anchors[anchor+1]*scale);
        canvas.rotate(anchors[anchor+2]-27f);
        float factor=size/Math.max(icon.getWidth(),icon.getHeight());
        float l=-iconAnchor[0]*factor, t=-iconAnchor[1]*factor;
        destination.set(l,t,l+icon.getWidth()*factor,t+icon.getHeight()*factor);
        canvas.drawBitmap(icon,null,destination,paint);
        canvas.restore();
    }
}
