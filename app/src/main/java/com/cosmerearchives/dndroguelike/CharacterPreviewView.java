package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;

/** Uses the same appearance, facing art and animated rig as the Wildlands. */
final class CharacterPreviewView extends View {
    private final RiggedElf rig;
    private ElfAppearance appearance;
    private boolean worldScale;
    private int direction, animation;
    private long animationStart;

    CharacterPreviewView(Context context, ElfAppearance appearance) {
        super(context);
        this.appearance = appearance;
        rig = new RiggedElf(context, appearance);
        if (Build.VERSION.SDK_INT < 28) setLayerType(View.LAYER_TYPE_SOFTWARE, null);
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
        rig.draw(canvas, getWidth() * .5f, getHeight() * .92f, scale, direction,
            walking ? seconds * 1.55f : 0f, walking ? 1f : 0f, seconds,
            progress, false, getWidth());
        postInvalidateOnAnimation();
    }
}
