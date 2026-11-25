package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

final class InventoryIcons {
    private static final int CELL = 128;
    private final Map<String, Rect> icons = new HashMap<>();
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private final Bitmap atlas;

    InventoryIcons(Context context) {
        Bitmap loaded = null;
        try (InputStream image = context.getAssets().open("inventory_icons.webp");
             BufferedReader index = new BufferedReader(new InputStreamReader(
                 context.getAssets().open("inventory_icons.txt"), StandardCharsets.UTF_8))) {
            loaded = BitmapFactory.decodeStream(image);
            if (loaded != null) {
                int columns = loaded.getWidth() / CELL;
                int capacity = columns * (loaded.getHeight() / CELL);
                String line;
                while ((line = index.readLine()) != null) {
                    String[] entry = line.split(" ", 2);
                    int cell = Integer.parseInt(entry[1]);
                    if (cell < 0 || cell >= capacity) throw new IOException("Invalid icon cell");
                    int x = cell % columns * CELL, y = cell / columns * CELL;
                    icons.put(entry[0], new Rect(x, y, x + CELL, y + CELL));
                }
            }
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException e) {
            Log.w("InventoryIcons", "Could not load item artwork", e);
        }
        atlas = loaded;
    }

    boolean draw(Canvas canvas, Equipment.Item item, float x, float y, float size) {
        if (atlas == null || item == null) return false;
        Rect source = icons.get(item.id);
        if (source == null) return false;
        float half = size / 2;
        destination.set(x - half, y - half, x + half, y + half);
        canvas.drawBitmap(atlas, source, destination, paint);
        return true;
    }
}
