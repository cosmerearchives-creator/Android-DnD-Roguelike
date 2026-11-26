package com.cosmerearchives.dndroguelike;

import android.graphics.Canvas;
import android.graphics.Paint;

final class WorldGrid {
    private WorldGrid() {}

    // Both world views draw on the main thread.
    private static final Paint LINES = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        LINES.setColor(0x85512921);
        LINES.setStrokeWidth(1.25f);
    }

    static void draw(Canvas canvas,float x,float y,float halfWidth,float halfHeight) {
        canvas.drawLine(x-halfWidth,y,x,y-halfHeight,LINES);
        canvas.drawLine(x,y-halfHeight,x+halfWidth,y,LINES);
    }
}
