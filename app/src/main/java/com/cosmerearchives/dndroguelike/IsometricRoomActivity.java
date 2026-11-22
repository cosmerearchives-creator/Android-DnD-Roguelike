package com.cosmerearchives.dndroguelike;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;

/** A small playable baked-art room, launched separately from the saved world. */
public final class IsometricRoomActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xFF19262D);
        TextView heading = new TextView(this);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        heading.setPadding(padding, padding, padding, 0);
        heading.setTextColor(0xFFF2DDB4);
        heading.setTextSize(18);
        heading.setText("Isometric Test Room\nTap a tile to move");
        layout.addView(heading);
        try {
            Room room = new Room(this);
            layout.addView(room, new LinearLayout.LayoutParams(-1, 0, 1));
            LinearLayout controls = new LinearLayout(this);
            layout.addView(controls);
            Button run = new Button(this);
            run.setText("Run off");
            run.setOnClickListener(v -> {
                room.running = !room.running;
                run.setText(room.running ? "Run on" : "Run off");
            });
            controls.addView(run, new LinearLayout.LayoutParams(0, -2, 1));
            controls.addView(room.punchButton, new LinearLayout.LayoutParams(0, -2, 1));
            Button exit = new Button(this);
            exit.setText("Exit");
            exit.setOnClickListener(v -> finish());
            controls.addView(exit, new LinearLayout.LayoutParams(0, -2, 1));
        } catch (IOException error) {
            heading.setText("Could not load the isometric room art. Use Back to return to the menu.");
        }
        setContentView(layout);
    }

    private static final class Room extends View {
        private static final int SIZE = 5;
        private static final int[] DX = {0, 1, 0, -1}, DY = {-1, 0, 1, 0};
        private static final float STEP_X = 70.0036f, STEP_Y = 35.83905f;
        private static final float ORIGIN_X = 384f, ORIGIN_Y = 304.493f, LEDGE_RISE = 28.7339f;
        private static final int TREE_X = 1, TREE_Y = -1;
        private static final int CELL_W = 144, CELL_H = 160;
        private static final long REFILL_MS = 6000L;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Path diamond = new Path();
        private final Rect source = new Rect();
        private final RectF destination = new RectF();
        private final Bitmap floor, tree, ledge, idle, walk, punch;
        private final ArrayDeque<Integer> route = new ArrayDeque<>();
        // Ordered from rear to front by tile X minus Y.
        private final int[] propX = {1, 2, 2, TREE_X};
        private final int[] propY = {1, 2, 1, TREE_Y};
        private final Button punchButton;
        private float x = 1, y = -2, rise, distance, markerX, markerY, markerRise;
        private int facing, cell = index(1, -2), step = cell, goal = -1;
        private long lastFrame, attackStart = -1, actionReadyAt;
        private float scale, offsetX, offsetY;
        private boolean running;

        Room(Context context) throws IOException {
            super(context);
            floor = load("floor");
            tree = load("tree");
            ledge = load("ledge");
            idle = load("idle");
            walk = load("walk");
            punch = load("punch");
            punchButton = new Button(context);
            punchButton.setText("Punch ●");
            punchButton.setOnClickListener(v -> attack());
            setContentDescription("Isometric room. Tap a ground tile to move Coiled Puff.");
            setClickable(true);
        }

        private Bitmap load(String name) throws IOException {
            try (InputStream input = getContext().getAssets().open("isometric_lab/" + name + ".png")) {
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                if (bitmap == null) throw new IOException("Invalid room art " + name);
                return bitmap;
            }
        }

        private static int index(int x, int y) { return (y + 2) * SIZE + x + 2; }
        private static int tileX(int index) { return index % SIZE - 2; }
        private static int tileY(int index) { return index / SIZE - 2; }
        private static boolean inside(int x, int y) { return Math.abs(x) <= 2 && Math.abs(y) <= 2; }
        private static boolean blocked(int x, int y) { return x == TREE_X && y == TREE_Y; }
        private static float elevation(int x, int y) {
            return (y == 1 && x >= 1 || y == 2 && x == 2) ? LEDGE_RISE : 0;
        }
        private static float px(float x, float y) { return ORIGIN_X + (x + y) * STEP_X; }
        private static float py(float x, float y, float rise) {
            return ORIGIN_Y + (x - y) * STEP_Y - rise;
        }

        private void routeTo(int target) {
            int[] previous = new int[SIZE * SIZE];
            Arrays.fill(previous, -1);
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(step);
            previous[step] = step;
            while (!queue.isEmpty() && previous[target] == -1) {
                int at = queue.remove();
                for (int dir = 0; dir < DX.length; dir++) {
                    int xx = tileX(at) + DX[dir], yy = tileY(at) + DY[dir];
                    if (!inside(xx, yy) || blocked(xx, yy)) continue;
                    int neighbor = index(xx, yy);
                    if (previous[neighbor] != -1) continue;
                    previous[neighbor] = at;
                    queue.add(neighbor);
                }
            }
            if (previous[target] == -1) return;
            route.clear();
            for (int at = target; at != step; at = previous[at]) route.addFirst(at);
            goal = target == cell && cell == step ? -1 : target;
        }

        private void attack() {
            long now = SystemClock.uptimeMillis();
            if (now < actionReadyAt) return;
            actionReadyAt = now + REFILL_MS;
            attackStart = now;
            markerX = x + DX[facing];
            markerY = y + DY[facing];
            markerRise = elevation(Math.round(markerX), Math.round(markerY));
            updatePunchButton(false);
            invalidate();
        }

        private void updatePunchButton(boolean ready) {
            if (punchButton.isEnabled() == ready) return;
            punchButton.setEnabled(ready);
            punchButton.setText(ready ? "Punch ●" : "Punch ○");
        }

        private void move(float dt) {
            if (cell == step && !route.isEmpty()) {
                step = route.remove();
                for (int dir = 0; dir < DX.length; dir++) {
                    if (tileX(cell) + DX[dir] == tileX(step)
                        && tileY(cell) + DY[dir] == tileY(step)) facing = dir;
                }
            }
            if (cell == step) return;
            float xx = tileX(step), yy = tileY(step);
            float remaining = Math.abs(xx - x) + Math.abs(yy - y);
            float advance = Math.min(remaining, dt * (running ? 2f : 1f));
            if (remaining > 0) {
                x += (xx - x) * advance / remaining;
                y += (yy - y) * advance / remaining;
            }
            distance += advance;
            float startRise = elevation(tileX(cell), tileY(cell));
            float endRise = elevation((int) xx, (int) yy);
            rise = endRise + (startRise - endRise) * (remaining - advance);
            if (advance >= remaining) {
                cell = step;
                x = xx;
                y = yy;
                if (cell == goal) goal = -1;
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            long now = SystemClock.uptimeMillis();
            float dt = lastFrame == 0 ? 0 : Math.min(.05f, (now - lastFrame) * .001f);
            lastFrame = now;
            float punchPhase = (now - attackStart) / (RigAttackPose.DURATION * 1000f);
            boolean attacking = attackStart >= 0 && punchPhase < 1;
            if (!attacking) move(dt);
            updatePunchButton(now >= actionReadyAt);
            scale = Math.min(getWidth() / 768f, getHeight() / 576f);
            if (scale <= 0) return;
            offsetX = (getWidth() - 768 * scale) * .5f;
            offsetY = (getHeight() - 576 * scale) * .5f;
            canvas.save();
            canvas.translate(offsetX, offsetY);
            canvas.scale(scale, scale);
            drawArt(canvas, floor, 0, 0);
            if (goal >= 0 && elevation(tileX(goal), tileY(goal)) == 0) drawMarker(canvas, tileX(goal), tileY(goal),
                elevation(tileX(goal), tileY(goal)), 0x80B7D6AD, .77f);
            if (attacking && markerRise == 0) drawMarker(canvas, markerX, markerY, markerRise, 0xAAEFC475, .55f);
            boolean drawn = false;
            for (int i = 0; i < propX.length; i++) {
                int xx = propX[i], yy = propY[i];
                if (!drawn && x - y < xx - yy) {
                    drawActor(canvas, now, attacking, punchPhase);
                    drawn = true;
                }
                Bitmap prop = blocked(xx, yy) ? tree : ledge;
                float ground = prop == tree ? 165.8486f : 87.4622f;
                drawArt(canvas, prop, px(xx, yy) - prop.getWidth() * .5f, py(xx, yy, 0) - ground);
                if (prop == ledge && goal == index(xx, yy)) {
                    drawMarker(canvas, xx, yy, LEDGE_RISE, 0x80B7D6AD, .77f);
                }
                if (prop == ledge && attacking && markerRise > 0
                    && Math.round(markerX) == xx && Math.round(markerY) == yy) {
                    drawMarker(canvas, markerX, markerY, markerRise, 0xAAEFC475, .55f);
                }
            }
            if (!drawn) drawActor(canvas, now, attacking, punchPhase);
            canvas.restore();
            postInvalidateOnAnimation();
        }

        private void drawArt(Canvas canvas, Bitmap bitmap, float left, float top) {
            destination.set(left, top, left + bitmap.getWidth(), top + bitmap.getHeight());
            canvas.drawBitmap(bitmap, null, destination, paint);
        }

        private void drawMarker(Canvas canvas, float x, float y, float z, int color, float fraction) {
            float cx = px(x, y), cy = py(x, y, z);
            diamond.reset();
            diamond.moveTo(cx + STEP_X * fraction, cy);
            diamond.lineTo(cx, cy + STEP_Y * fraction);
            diamond.lineTo(cx - STEP_X * fraction, cy);
            diamond.lineTo(cx, cy - STEP_Y * fraction);
            diamond.close();
            paint.setColor(color);
            canvas.drawPath(diamond, paint);
            paint.setColor(0xFFFFFFFF);
        }

        private void drawActor(Canvas canvas, long now, boolean attacking, float punchPhase) {
            Bitmap sheet = idle;
            int column = (int) (now / 500 % 4);
            if (attacking) {
                sheet = punch;
                column = Math.min(6, (int) (7 * punchPhase));
            } else if (cell != step) {
                sheet = walk;
                column = (int) (8 * GaitModel.fractional(distance * 1.55f));
            }
            int left = column * CELL_W, top = facing * CELL_H;
            source.set(left, top, left + CELL_W, top + CELL_H);
            float screenX = px(x, y) - CELL_W * .5f;
            float screenY = py(x, y, rise) - 135.303f;
            destination.set(screenX, screenY, screenX + CELL_W, screenY + CELL_H);
            canvas.drawBitmap(sheet, source, destination, paint);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) return true;
            if (event.getActionMasked() == MotionEvent.ACTION_UP && scale > 0) {
                performClick();
                float sx = (event.getX() - offsetX) / scale;
                float sy = (event.getY() - offsetY) / scale;
                int selected = -1, closestDepth = Integer.MIN_VALUE;
                for (int yy = -2; yy <= 2; yy++) {
                    for (int xx = -2; xx <= 2; xx++) {
                        float u = Math.abs(sx - px(xx, yy)) / STEP_X;
                        float v = Math.abs(sy - py(xx, yy, elevation(xx, yy))) / STEP_Y;
                        if (!blocked(xx, yy) && u + v <= 1 && xx - yy > closestDepth) {
                            closestDepth = xx - yy;
                            selected = index(xx, yy);
                        }
                    }
                }
                if (selected >= 0) routeTo(selected);
            }
            return true;
        }

        @Override public boolean performClick() {
            super.performClick();
            return true;
        }
    }
}
