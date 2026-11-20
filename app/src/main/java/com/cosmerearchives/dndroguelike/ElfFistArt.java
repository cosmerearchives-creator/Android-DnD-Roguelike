package com.cosmerearchives.dndroguelike;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

/** Closed fingerless glove drawn from the wrist toward the knuckles. */
final class ElfFistArt {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape = new Path();
    private final Shader leather = new LinearGradient(0f, -4f, 0f, 4f,
        new int[]{0xFF55566A, 0xFF383943, 0xFF2D292F}, null, Shader.TileMode.CLAMP);
    private final Shader skin;

    ElfFistArt(int skinTone) {
        int[] colors = skinTone == 0 ? new int[]{0xFFF0C49B, 0xFFD9A079, 0xFF95624E}
            : new int[]{shade(skinTone, 27), skinTone, shade(skinTone, -34)};
        skin = new LinearGradient(0f, -4f, 2f, 4f, colors, null, Shader.TileMode.CLAMP);
    }

    private static int shade(int color, int amount) {
        return android.graphics.Color.rgb(
            channel((color >>> 16) & 255, amount),
            channel((color >>> 8) & 255, amount),
            channel(color & 255, amount));
    }

    private static int channel(int value, int amount) {
        return Math.max(0, Math.min(255, value + amount));
    }

    void draw(Canvas canvas, int facing, float elbowX, float elbowY, float wristX, float wristY,
              float closure, float impact) {
        int alpha = Math.round(255f * closure);
        boolean rear = facing == 2;
        boolean towardCamera = facing == 0;
        canvas.save();
        canvas.translate(wristX, wristY);
        canvas.rotate((float) Math.toDegrees(Math.atan2(wristY - elbowY, wristX - elbowX)));
        canvas.scale(1f - (towardCamera ? .16f * impact : 0f),
            1f + (towardCamera ? .16f : .04f) * impact);

        // The glove overlaps the illustrated cuff and grows wider toward the palm.
        shape.reset();
        shape.moveTo(-3.4f, -2.25f);
        shape.cubicTo(-1f, -2.45f, .1f, -2.7f, 1.5f, -3.35f);
        shape.cubicTo(3.3f, -4.4f, 5.6f, -4.15f, 6.9f, -3.2f);
        shape.cubicTo(8.5f, -2.5f, 8.35f, -.1f, 7.65f, 1.3f);
        shape.cubicTo(7.1f, 2.85f, 5.3f, 3.15f, 3.2f, 3.7f);
        shape.cubicTo(1.65f, 3.9f, .3f, 2.55f, -3.4f, 2.25f);
        shape.close();
        fill(canvas, 0xFF3D3A48, leather, alpha);

        if (facing == 1) {
            // The camera is on the palm side of the striking arm in this profile.
            drawPalm(canvas, alpha);
            canvas.restore();
            return;
        }

        // The fingerless glove exposes the knuckles. Short arcs divide the
        // curled fingers without drawing over the wrist connection.
        shape.reset();
        shape.moveTo(rear ? 5f : 3.5f, -3.45f);
        shape.quadTo(5.7f, -4.1f, 6.9f, -3.1f);
        shape.quadTo(8.15f, -2.7f, 7.85f, -1.15f);
        shape.quadTo(8.15f, -.5f, 7.5f, .45f);
        shape.quadTo(7.3f, 1.45f, 6.4f, 2.05f);
        shape.quadTo(5.05f, 2.85f, rear ? 4.7f : 3.65f, 1.7f);
        shape.cubicTo(4.9f, .1f, 4.2f, -1.5f, rear ? 5f : 3.5f, -3.45f);
        shape.close();
        fill(canvas, 0xFFD4A07D, skin, alpha);
        stroke(canvas, 0x886B483D, alpha, .34f, 5.95f, -1.45f, 7.76f, -1.16f);
        stroke(canvas, 0x996D4C3D, alpha, .32f, 5.5f, -.15f, 7.48f, .15f);

        // Tucked thumb and leather seam distinguish the fist from a capped tube.
        shape.reset();
        shape.moveTo(1.05f, 2.1f);
        shape.quadTo(2.2f, .72f, 3.85f, .95f);
        shape.quadTo(4.85f, 1f, 5.1f, 1.85f);
        shape.quadTo(4.2f, 3.2f, 2.73f, 3.25f);
        shape.quadTo(1.55f, 3.1f, 1.05f, 2.1f);
        shape.close();
        fill(canvas, 0xFFD4A07D, skin, alpha);
        stroke(canvas, 0xC596744B, alpha, .47f, -1.3f, -2.35f, -.95f, 2.36f);
        canvas.restore();
    }

    private void drawPalm(Canvas canvas, int alpha) {
        // The curled finger pads face inward toward the heel of the palm.
        shape.reset();
        shape.moveTo(3.25f, -3.35f);
        shape.cubicTo(5.25f, -4.1f, 7.55f, -3.8f, 7.95f, -2.04f);
        shape.cubicTo(8.35f, -.32f, 7.15f, 1.96f, 5.9f, 2.55f);
        shape.quadTo(4.8f, 3.05f, 3.9f, 2.38f);
        shape.quadTo(4.85f, .53f, 3.65f, -.76f);
        shape.quadTo(2.85f, -1.78f, 3.25f, -3.35f);
        shape.close();
        fill(canvas, 0xFFD4A07D, skin, alpha);
        stroke(canvas, 0xAA704637, alpha, .43f, 5.4f, -2.03f, 7.94f, -1.58f);
        stroke(canvas, 0xAA704637, alpha, .43f, 5.28f, -.56f, 7.68f, .12f);
        stroke(canvas, 0x99704637, alpha, .38f, 4.98f, .88f, 6.65f, 1.68f);

        // The thumb lies across the curled fingers, visible on the palm side.
        shape.reset();
        shape.moveTo(1.1f, 2.9f);
        shape.cubicTo(1.55f, .79f, 2.85f, -.27f, 4.65f, -1.43f);
        shape.quadTo(5.65f, -2.04f, 6.02f, -1.14f);
        shape.quadTo(6.08f, -.4f, 5.19f, .27f);
        shape.quadTo(3.5f, 1.78f, 3.45f, 3.17f);
        shape.quadTo(2.18f, 3.67f, 1.1f, 2.9f);
        shape.close();
        fill(canvas, 0xFFD4A07D, skin, alpha);
        stroke(canvas, 0x9E754B3D, alpha, .4f, 3.65f, .63f, 5.16f, -.49f);
    }

    private void fill(Canvas canvas, int color, Shader shader, int alpha) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setAlpha(alpha);
        paint.setShader(shader);
        canvas.drawPath(shape, paint);
        paint.setShader(null);
    }

    private void stroke(Canvas canvas, int color, int alpha, float width,
                        float fromX, float fromY, float toX, float toY) {
        paint.setColor(color);
        paint.setAlpha(Math.round((color >>> 24) * alpha / 255f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(width);
        canvas.drawLine(fromX, fromY, toX, toY, paint);
    }
}
