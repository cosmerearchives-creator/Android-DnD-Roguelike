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

/** Uses the same appearance, facing art and animated rig as the Wildlands. */
final class CharacterPreviewView extends View {
    private final RiggedElf rig;
    private final Bitmap terrain;
    private final Rect terrainTile=new Rect();
    private final RectF backdrop=new RectF();
    private final Paint terrainPaint=new Paint(Paint.FILTER_BITMAP_FLAG);
    private ElfAppearance appearance;
    private boolean worldScale;
    private int direction, animation;
    private long animationStart;

    CharacterPreviewView(Context context, ElfAppearance appearance) {
        super(context);
        this.appearance = appearance;
        rig = new RiggedElf(context, appearance);
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

    void appearance(ElfAppearance value) {
        if (value == appearance) return;
        rig.setAppearance(value);
        appearance = value;
        invalidate();
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
        boolean walking = animation == 1;
        float progress = animation == 2 ? (seconds % (RigAttackPose.DURATION + .55f))
            / RigAttackPose.DURATION : 0f;
        if (progress >= 1f) progress = 0f;
        float scale = Math.min(getHeight() / 205f, getWidth() / 172f) * appearance.heightScale;
        if (worldScale) scale *= .40f;
        if (scale <= 0f) return;
        canvas.drawColor(0xFF13222B);
        if (terrain != null) {
            // Crop a square of the same painted ground used in the Wildlands.
            float size=Math.max(getWidth(),getHeight());
            backdrop.set(0f,(getHeight()-size)*.5f,size,(getHeight()+size)*.5f);
            canvas.drawBitmap(terrain,terrainTile,backdrop,terrainPaint);
        }
        rig.draw(canvas, getWidth() * .5f, getHeight() * .92f, scale, direction,
            walking ? seconds * 1.55f : 0f, walking ? 1f : 0f, seconds,
            progress, false, getWidth());
        postInvalidateOnAnimation();
    }
}
