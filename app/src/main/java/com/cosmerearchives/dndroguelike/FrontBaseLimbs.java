package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import java.io.IOException;
import java.io.InputStream;

/** Front-facing base artwork registered to the shared rig. */
final class FrontBaseLimbs {
    private static final int WIDTH=96, HEIGHT=320, COLUMNS=2, ROWS=40;
    private static final float PIXELS_TO_RIG=1f/4.3f;
    private final LimbMesh[] meshes = {
        new LimbMesh(WIDTH,HEIGHT,COLUMNS,ROWS,PIXELS_TO_RIG,54,43,41,143,30,231),
        new LimbMesh(WIDTH,HEIGHT,COLUMNS,ROWS,PIXELS_TO_RIG,42,43,56,143,66,231),
        new LimbMesh(WIDTH,HEIGHT,COLUMNS,ROWS,PIXELS_TO_RIG,49,46,54,151,50,267),
        new LimbMesh(WIDTH,HEIGHT,COLUMNS,ROWS,PIXELS_TO_RIG,40,46,34,151,38,267)
    };
    private final Bitmap[] images=new Bitmap[4];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);

    FrontBaseLimbs(Context context) {
        try (InputStream stream=context.getAssets().open("elf_front_bound_limbs.webp")) {
            Bitmap atlas=BitmapFactory.decodeStream(stream);
            if (atlas == null || atlas.getWidth()!=WIDTH*4 || atlas.getHeight()!=HEIGHT) return;
            for (int i=0;i<images.length;i++) {
                images[i]=Bitmap.createBitmap(atlas,i*WIDTH,0,WIDTH,HEIGHT);
            }
        } catch (IOException ignored) {
            // The existing segmented layers also support the front view.
        }
    }

    boolean ready() { return images[0]!=null; }

    void draw(Canvas canvas,int index,float rootX,float rootY,
              float jointX,float jointY,float tipX,float tipY) {
        LimbMesh mesh=meshes[index];
        mesh.pose(rootX,rootY,jointX,jointY,tipX,tipY);
        canvas.drawBitmapMesh(images[index],COLUMNS,ROWS,mesh.vertices,0,null,0,paint);
    }
}
