package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

final class ForestMenu extends ScrollView {
    private static final int FOREST = 0xFF103032;
    private static final int GOLD = 0xFFE9C981;
    private static final int PARCHMENT = 0xFFF6E8C6;
    private static final float ART_WIDTH = 781;
    private static final float ART_HEIGHT = 1536;

    final Button adventure;
    final Button avatar;
    final Button updates;
    final TextView status;

    ForestMenu(Context context) {
        super(context);
        setFillViewport(true);
        setVerticalScrollBarEnabled(false);
        setBackgroundColor(FOREST);
        ViewCompat.setOnApplyWindowInsetsListener(this, (view, windowInsets) -> {
            Insets safe = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        MenuScene scene = new MenuScene(context);
        addView(scene, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.menu_tree);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        scene.place(logo, 254, 70, 273, 142, 0);

        TextView title = label(context, context.getString(R.string.app_name));
        title.setAccessibilityHeading(true);
        scene.place(title, 38, 205, 705, 83, 76);
        scene.place(label(context, context.getString(R.string.menu_subtitle)), 188, 294, 405, 53, 30);
        scene.place(label(context, "v" + BuildConfig.VERSION_NAME), 314, 361, 153, 48, 28);

        adventure = button(context, R.string.menu_continue, MenuIcon.PLAY, true);
        avatar = button(context, 0, MenuIcon.AVATAR, false);
        updates = button(context, R.string.menu_updates, MenuIcon.UPDATE, false);
        scene.place(adventure, 98, 473, 585, 118, 41);
        scene.place(avatar, 102, 602, 577, 103, 40);
        scene.place(updates, 102, 722, 577, 104, 40);

        status = label(context, "");
        status.setSingleLine(false);
        status.setMaxLines(3);
        status.setCompoundDrawablePadding(Math.round(8 * getResources().getDisplayMetrics().density));
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        scene.place(status, 90, 884, 601, 92, 29);
    }

    private static TextView label(Context context, String value) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextColor(PARCHMENT);
        text.setTypeface(WildlandsUi.SERIF);
        text.setGravity(Gravity.CENTER);
        text.setSingleLine(true);
        text.setIncludeFontPadding(false);
        return text;
    }

    private static Button button(Context context, int label, MenuIcon icon, boolean primary) {
        Button button = new Button(context);
        if (label != 0) button.setText(label);
        button.setTypeface(WildlandsUi.SERIF);
        button.setAllCaps(false);
        button.setSingleLine(true);
        button.setIncludeFontPadding(false);
        button.setTextColor(new ColorStateList(
            new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}},
            new int[]{0xFFADAD96, PARCHMENT}));
        button.setStateListAnimator(null);
        button.setBackground(new MenuButton(icon, primary));
        return button;
    }

    private static final class Slot extends ViewGroup.LayoutParams {
        final float x, y, span, height, font;

        Slot(float x, float y, float span, float height, float font) {
            super(0, 0);
            this.x = x;
            this.y = y;
            this.span = span;
            this.height = height;
            this.font = font;
        }
    }

    private final class MenuScene extends ViewGroup {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final RectF rect = new RectF();
        private final Path path = new Path();
        private final Bitmap backdrop;
        private float scale;
        private float left;

        MenuScene(Context context) {
            super(context);
            setWillNotDraw(false);
            backdrop = BitmapFactory.decodeResource(getResources(), R.drawable.menu_camp);
        }

        void place(View view, float x, float y, float width, float height, float font) {
            addView(view, new Slot(x, y, width, height, font));
        }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            float density = getResources().getDisplayMetrics().density;
            float artWidth = Math.min(width, 480 * density);
            scale = artWidth / ART_WIDTH;
            left = (width - artWidth) / 2;
            setMeasuredDimension(width, resolveSize(Math.round(ART_HEIGHT * scale), heightSpec));
            boolean hasAdventure = adventure.getVisibility() != GONE;
            for (int index = 0; index < getChildCount(); index++) {
                View child = getChildAt(index);
                if (child.getVisibility() == GONE) continue;
                Slot slot = (Slot) child.getLayoutParams();
                if (child instanceof TextView) {
                    TextView text = (TextView) child;
                    float fontScale = Math.min(1.3f, getResources().getConfiguration().fontScale);
                    text.setTextSize(TypedValue.COMPLEX_UNIT_PX, slot.font * scale * fontScale);
                    if (child != status) {
                        text.setAutoSizeTextTypeUniformWithConfiguration(1, Math.max(2,
                            Math.round(slot.font * scale * fontScale)), 1, TypedValue.COMPLEX_UNIT_PX);
                    }
                    if (child instanceof Button) {
                        int iconInset = Math.round(126 * scale);
                        int rightInset = Math.round(25 * scale);
                        text.setPadding(iconInset, 0, rightInset, 0);
                        text.setGravity(Gravity.CENTER);
                        child.setActivated(child == adventure || (child == avatar && !hasAdventure));
                    }
                }
                int mode = child == status ? MeasureSpec.AT_MOST : MeasureSpec.EXACTLY;
                child.measure(MeasureSpec.makeMeasureSpec(Math.round(slot.span * scale), mode),
                    MeasureSpec.makeMeasureSpec(Math.round(slot.height * scale), MeasureSpec.EXACTLY));
            }
        }

        @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
            boolean hasAdventure = adventure.getVisibility() != GONE;
            for (int index = 0; index < getChildCount(); index++) {
                View child = getChildAt(index);
                if (child.getVisibility() == GONE) continue;
                Slot slot = (Slot) child.getLayoutParams();
                float y = slot.y;
                if (!hasAdventure && child == avatar) y = 473;
                if (!hasAdventure && child == updates) y = 602;
                if (!hasAdventure && child == status) y -= 120;
                int x = Math.round(left + (slot.x + slot.span / 2) * scale - child.getMeasuredWidth() / 2f);
                int top = Math.round(y * scale);
                child.layout(x, top, x + child.getMeasuredWidth(), top + child.getMeasuredHeight());
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (scale <= 0) return;
            canvas.save();
            canvas.translate(left, 0);
            canvas.scale(scale, scale);
            rect.set(0, 0, ART_WIDTH, getHeight() / scale);
            canvas.drawBitmap(backdrop, null, rect, paint);
            ornament(canvas, 39, true);
            ornament(canvas, adventure.getVisibility() == GONE ? 750 : 870, false);
            paint.setColor(GOLD);
            paint.setStrokeWidth(1.8f);
            canvas.drawLine(129, 320, 192, 320, paint);
            canvas.drawLine(589, 320, 652, 320, paint);
            rect.set(314, 362, 467, 407);
            WildlandsUi.plate(canvas, paint, path, rect, 19, 0xC0103032, GOLD, 2);
            canvas.restore();
        }

        private void ornament(Canvas canvas, float y, boolean crown) {
            paint.setColor(GOLD);
            paint.setStrokeWidth(1.8f);
            canvas.drawLine(207, y, 336, y, paint);
            canvas.drawLine(445, y, 574, y, paint);
            diamond(canvas, path, paint, 357, y, crown ? 11 : 6);
            diamond(canvas, path, paint, 390.5f, y, crown ? 5 : 9);
            diamond(canvas, path, paint, 424, y, crown ? 11 : 6);
        }
    }

    private enum MenuIcon { PLAY, AVATAR, UPDATE }

    private static void diamond(Canvas canvas, Path path, Paint paint, float x, float y, float radius) {
        path.reset();
        path.moveTo(x, y - radius);
        path.lineTo(x + radius, y);
        path.lineTo(x, y + radius);
        path.lineTo(x - radius, y);
        path.close();
        canvas.drawPath(path, paint);
    }

    private static final class MenuButton extends Drawable {
        private final MenuIcon icon;
        private final boolean primary;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();
        private final LinearGradient highlight = new LinearGradient(
            0, 0, 0, 102, 0xFF2C4F48, 0xFF153738, Shader.TileMode.CLAMP);

        MenuButton(MenuIcon icon, boolean primary) {
            this.icon = icon;
            this.primary = primary;
        }

        @Override public void draw(Canvas canvas) {
            boolean enabled = false;
            boolean pressed = false;
            boolean featured = primary;
            for (int state : getState()) {
                if (state == android.R.attr.state_enabled) enabled = true;
                if (state == android.R.attr.state_pressed || state == android.R.attr.state_focused) pressed = true;
                if (state == android.R.attr.state_activated) featured = true;
            }
            canvas.save();
            canvas.translate(getBounds().left, getBounds().top);
            canvas.scale(getBounds().width() / 577f, getBounds().height() / 103f);
            rect.set(5, 6, 572, 97);
            int edge = enabled ? GOLD : 0xFF9E9774;
            if (featured || pressed) {
                WildlandsUi.plate(canvas, paint, path, rect, 23, Color.TRANSPARENT, 0x24F5C66C, 13);
                WildlandsUi.plate(canvas, paint, path, rect, 23, Color.TRANSPARENT, 0x55F5C66C, 8);
            }
            WildlandsUi.plate(canvas, paint, path, rect, 23,
                pressed ? 0xFF39544A : 0xD9123437, edge, featured ? 3.5f : 2.2f);
            if (featured && !pressed) {
                paint.setShader(highlight);
                canvas.drawPath(path, paint);
                paint.setShader(null);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(edge);
                canvas.drawPath(path, paint);
                paint.setStyle(Paint.Style.FILL);
            }
            paint.setColor(edge);
            drawIcon(canvas);
            canvas.restore();
        }

        private void drawIcon(Canvas canvas) {
            path.reset();
            switch (icon) {
                case PLAY:
                    path.moveTo(70, 30);
                    path.lineTo(109, 51.5f);
                    path.lineTo(70, 73);
                    path.close();
                    canvas.drawPath(path, paint);
                    break;
                case AVATAR:
                    canvas.drawOval(81, 27, 101, 51, paint);
                    canvas.drawRoundRect(70, 50, 112, 76, 12, 12, paint);
                    canvas.drawRect(70, 65, 112, 76, paint);
                    break;
                case UPDATE:
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5);
                    canvas.drawArc(69, 30, 112, 73, 199, 155, false, paint);
                    canvas.drawArc(69, 30, 112, 73, 19, 155, false, paint);
                    paint.setStyle(Paint.Style.FILL);
                    path.moveTo(115, 34);
                    path.lineTo(115, 51);
                    path.lineTo(99, 47);
                    path.close();
                    path.moveTo(66, 69);
                    path.lineTo(66, 52);
                    path.lineTo(82, 56);
                    path.close();
                    canvas.drawPath(path, paint);
                    break;
            }
        }

        @Override public boolean isStateful() { return true; }
        @Override protected boolean onStateChange(int[] state) { invalidateSelf(); return true; }
        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(android.graphics.ColorFilter filter) {}
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }
}
