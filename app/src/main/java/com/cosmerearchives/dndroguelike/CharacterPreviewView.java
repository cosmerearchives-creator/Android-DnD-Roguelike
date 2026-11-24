package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import java.io.IOException;
import java.io.InputStream;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;

/** Previews the same Avatar sprites used in Free Roam. */
final class CharacterPreviewView extends View {
    private final Bitmap terrain;
    private final Rect terrainTile=new Rect();
    private final RectF backdrop=new RectF();
    private final Paint terrainPaint=new Paint(Paint.FILTER_BITMAP_FLAG);
    private PetiteAppearance petite=PetiteAppearance.DEFAULT;
    private final Bitmap[] petiteSheets=new Bitmap[3];
    private final Rect animationCell=new Rect();
    private final RectF animationBounds=new RectF();
    private final Paint animationPaint=new Paint(Paint.FILTER_BITMAP_FLAG);
    private boolean worldScale;
    private int direction, animation;
    private long animationStart;

    CharacterPreviewView(Context context) {
        super(context);
        terrain = loadTerrain(context);
        if (terrain != null) {
            int tile = terrain.getWidth() / 4;
            terrainTile.set(0, 0, tile * 2, tile * 2);
        }
        terrainPaint.setAlpha(94);
        if (Build.VERSION.SDK_INT < 28) setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private static Bitmap loadTerrain(Context context) {
        try (InputStream input=context.getAssets().open("wildlands_terrain_v031.png")) {
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize=2;
            return BitmapFactory.decodeStream(input,null,options);
        } catch(IOException ignored) {
            return null;
        }
    }

    void petite(PetiteAppearance value) {
        if (petite==value) return;
        petite=value;
        java.util.Arrays.fill(petiteSheets,null);
        invalidate();
    }

    private void drawPetite(Canvas canvas,float seconds,float progress,float scale) {
        if (petiteSheets[animation]==null) {
            String motion=new String[]{"idle","walk","punch"}[animation];
            Bitmap source=PetiteAppearance.load(getContext(),"blender_world/petite_detail_"+motion+".png");
            petiteSheets[animation]=petite.apply(getContext(),source,motion);
        }
        Bitmap sheet=petiteSheets[animation];
        if (sheet==null) return;
        int frame=animation==1 ? Math.floorMod((int)(seconds*12.4f),8)
            : animation==2 ? Math.min(4,(int)(progress*5)) : 0;
        int facing=(direction*2+1)%8;
        animationCell.set(frame*168,facing*192,(frame+1)*168,(facing+1)*192);
        float x=getWidth()*.5f,y=getHeight()*.92f;
        animationBounds.set(x-84*scale,y-160*scale,x+84*scale,y+32*scale);
        canvas.drawBitmap(sheet,animationCell,animationBounds,animationPaint);
    }

    void mode(boolean value) {
        worldScale = value;
        invalidate();
    }

    void direction(int value) {
        direction = value;
        invalidate();
    }

    void animation(int value) {
        animation = value;
        animationStart = SystemClock.uptimeMillis();
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        float seconds = (SystemClock.uptimeMillis() - animationStart) / 1000f;
        float progress = animation == 2 ? (seconds % (RigAttackPose.DURATION + .55f))
            / RigAttackPose.DURATION : 0f;
        if (progress >= 1f) progress = 0f;
        float scale = Math.min(getHeight() / 205f, getWidth() / 172f);
        if (worldScale) scale *= .40f;
        if (scale <= 0f) return;
        canvas.drawColor(0xFF13222B);
        if (terrain != null) {
            // Crop a square of the same painted ground used in the Wildlands.
            float size=Math.max(getWidth(),getHeight());
            backdrop.set(0f,(getHeight()-size)*.5f,size,(getHeight()+size)*.5f);
            canvas.drawBitmap(terrain,terrainTile,backdrop,terrainPaint);
        }
        drawPetite(canvas,seconds,progress,scale);
        postInvalidateOnAnimation();
    }
}
