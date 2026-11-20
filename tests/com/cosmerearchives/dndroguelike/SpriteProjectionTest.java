package com.cosmerearchives.dndroguelike;

/** Regression guard: cropped old-art portrait must use ORIGINAL's projection. */
public final class SpriteProjectionTest {
    private static void eq(float expected,float actual,String message){
        if(Math.abs(expected-actual)>.0001f)
            throw new AssertionError(message+": "+expected+" != "+actual);
    }

    public static void main(String[] args){
        // Full frame corners must exactly match the legacy ORIGINAL renderer.
        eq(-62.5f,SpriteProjection.x(0,360),"full-frame left");
        eq(62.5f,SpriteProjection.x(360,360),"full-frame right");
        eq(-166f,SpriteProjection.y(0,480),"full-frame top");
        eq(0f,SpriteProjection.y(480,480),"full-frame foot");

        // A cropped head must preserve full-frame pixel-to-world ratios.
        int left=360*27/100,right=360*73/100,bottom=480*28/100;
        eq((right-left)*125f/360f,
           SpriteProjection.x(right,360)-SpriteProjection.x(left,360),
           "cropped head width");
        eq(bottom*166f/480f,
           SpriteProjection.y(bottom,480)-SpriteProjection.y(0,480),
           "cropped head height");
        eq(-166f,SpriteProjection.y(0,480),"cropped head Y origin");

        // The old custom -32..32/-167..-115 rectangle enlarged the
        // front-facing head by ~12%. That scale is no longer permitted.
        float oldHeight=52f;
        float correctedHeight=SpriteProjection.y(bottom,480)-SpriteProjection.y(0,480);
        if(!(oldHeight/correctedHeight>1.1f))
            throw new AssertionError("Expected prior portrait overscale");
        System.out.println("SpriteProjectionTest passed");
    }
}
