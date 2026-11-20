package com.cosmerearchives.dndroguelike;

/**
 * Pixel-to-character-space projection shared by the unchanged ORIGINAL sprite
 * renderer and RIG TEST's legacy-derived head illustration. Both renderers
 * already inherit the same outdoor camera and requested character scale.
 *
 * A region cut out of the original atlas must keep this exact projection;
 * stretching that region to an independent RectF creates an apparent second
 * zoom and moves the jaw relative to the modular torso.
 */
final class SpriteProjection {
    static final float FULL_WIDTH=125f;
    static final float FULL_HEIGHT=166f;

    private SpriteProjection(){}

    static float x(int pixelX,int sourceFrameWidth){
        if(sourceFrameWidth<=0)throw new IllegalArgumentException("frameWidth");
        return -FULL_WIDTH*.5f + pixelX*FULL_WIDTH/sourceFrameWidth;
    }

    static float y(int pixelY,int sourceFrameHeight){
        if(sourceFrameHeight<=0)throw new IllegalArgumentException("frameHeight");
        return -FULL_HEIGHT + pixelY*FULL_HEIGHT/sourceFrameHeight;
    }
}
