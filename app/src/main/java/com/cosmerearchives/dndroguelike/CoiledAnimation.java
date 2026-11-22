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

/** Baked Coiled Puff poses. The world and preview share the same frame selection. */
final class CoiledAnimation {
    private static final int WIDTH = 168, HEIGHT = 192;
    private static final int[] FRAME_COUNTS = {4, 8, 7};
    private static final String[] MOTIONS = {"idle", "walk", "punch"};
    // The bake orbits the model in the opposite order from the game's world facings.
    private static final int[] FACING_ROWS = {0, 3, 2, 1};
    private static final int[] GROUND_ROWS = {180, 181, 179, 181};
    private static final float RIG_UNITS_PER_PIXEL = 156f / 168f;
    private final Bitmap[] sheets;
    private final Rect source = new Rect();
    private final RectF destination = new RectF();

    private CoiledAnimation(Bitmap[] sheets) {
        this.sheets = sheets;
    }

    static CoiledAnimation load(Context context) {
        Bitmap[] sheets = new Bitmap[MOTIONS.length];
        for (int i = 0; i < MOTIONS.length; i++) {
            try (InputStream input = context.getAssets().open("coiled_animation/" + MOTIONS[i] + ".png")) {
                Bitmap sheet = BitmapFactory.decodeStream(input);
                if (sheet == null || sheet.getWidth() != WIDTH * FRAME_COUNTS[i]
                    || sheet.getHeight() != HEIGHT * FACING_ROWS.length) return null;
                sheets[i] = sheet;
            } catch (IOException ignored) {
                return null;
            }
        }
        return new CoiledAnimation(sheets);
    }

    void draw(Canvas canvas, Paint paint, float x, float y, float scale, int facing,
              float walkCycles, float movementBlend, float idleElapsed, float attackProgress) {
        int motion = 0;
        float phase = GaitModel.fractional(idleElapsed * .5f);
        if (attackProgress > 0f) {
            motion = 2;
            phase = attackProgress;
        } else if (movementBlend > .15f) {
            motion = 1;
            phase = GaitModel.fractional(walkCycles);
        }
        int frame = Math.min(FRAME_COUNTS[motion] - 1, (int) (phase * FRAME_COUNTS[motion]));
        int row = FACING_ROWS[facing];
        source.set(frame * WIDTH, row * HEIGHT, (frame + 1) * WIDTH, (row + 1) * HEIGHT);
        float pixel = scale * RIG_UNITS_PER_PIXEL;
        float halfWidth = WIDTH * .5f * pixel;
        float top = y - GROUND_ROWS[row] * pixel;
        destination.set(x - halfWidth, top, x + halfWidth, top + HEIGHT * pixel);
        canvas.drawBitmap(sheets[motion], source, destination, paint);
    }
}
