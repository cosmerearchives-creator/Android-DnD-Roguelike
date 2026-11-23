package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.PixelFormat;
import android.graphics.ColorFilter;
import android.graphics.BlurMaskFilter;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;

/** The Origin step of the portrait creator, built from native Android views. */
final class OriginCreatorView extends FrameLayout {
    private static final int INK=0xFFF6E3BE, GOLD=0xFFD9B678, MUTED=0xFFC5D1CB;
    private static final int PANEL=0xF01B3035, CARD=0xF4284243;
    private static final String[] STEP_ICONS={"♟","⚔","✦","⬡","✥","✶","✓"};
    private static final String[] CARD_ICONS={"✦","♣","⚔","◆","⚒"};
    private static final String[] STEPS={"Origin","Race","Class","Abilities","Skills","Appearance","Review"};
    private static final String[] DESCRIPTIONS={
        "A devotee trained in faith and tradition.",
        "A guide who knows how to survive far from town.",
        "A watchful defender of travelers and trade routes.",
        "A curious explorer of old ruins and forgotten lore.",
        "A craftsperson trained by a trade guild."
    };
    private final PlayerCharacter hero;
    private final Runnable onChange;
    private final TextView feature;
    private final LinearLayout[] cards=new LinearLayout[PlayerCharacter.BACKGROUNDS.length];
    private final TextView[] stepViews=new TextView[STEPS.length];
    private final Scene scene;
    private int facing=2;

    OriginCreatorView(Context context,PlayerCharacter hero,Runnable onChange,Runnable back,Runnable next) {
        super(context);
        this.hero=hero;this.onChange=onChange;
        setBackgroundColor(0xFF14292E);
        scene=new Scene(context,hero.appearance==ElfAppearance.PETITE ? "petite" : "classic");
        addView(scene,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout layout=column();
        layout.setPadding(dp(8),dp(18),dp(8),dp(9));
        addView(layout,new FrameLayout.LayoutParams(-1,-1));
        TextView crest=text("────── ✦ ⚜ ✦ ──────",15,true);
        crest.setGravity(Gravity.CENTER);
        crest.setTextColor(GOLD);
        layout.addView(crest,new LinearLayout.LayoutParams(-1,dp(25)));
        TextView heading=text("Character Creation",25,true);
        heading.setGravity(Gravity.CENTER);
        heading.setTextSize(27);
        layout.addView(heading,new LinearLayout.LayoutParams(-1,dp(47)));
        TextView subtitle=text("Create your adventurer",13,false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextColor(INK);
        subtitle.setTypeface(Typeface.create("serif",Typeface.NORMAL));
        layout.addView(subtitle,new LinearLayout.LayoutParams(-1,dp(34)));
        View titleRule=new View(context);
        titleRule.setBackgroundColor(GOLD);
        LinearLayout.LayoutParams ruleSize=new LinearLayout.LayoutParams(dp(210),dp(1));
        ruleSize.gravity=Gravity.CENTER_HORIZONTAL;
        ruleSize.bottomMargin=dp(18);
        layout.addView(titleRule,ruleSize);

        LinearLayout body=new LinearLayout(context);
        layout.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout steps=column();
        steps.setGravity(Gravity.CENTER_VERTICAL);
        body.addView(steps,new LinearLayout.LayoutParams(dp(91),-1));
        for(int i=0;i<STEPS.length;i++){
            TextView label=text(STEP_ICONS[i]+"  "+STEPS[i],i==5?11:13,true);
            label.setGravity(Gravity.CENTER);
            label.setBackground(ornate(i==0?0xF03D4C49:PANEL,i==0?0xFFFFD88C:0xFF577074,i==0?4:1));
            label.setSelected(i==0);
            stepViews[i]=label;
            LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(-1,dp(35));
            size.bottomMargin=dp(12);
            steps.addView(label,size);
            if(i<STEPS.length-1){
                TextView connector=text("⌘",10,true);
                connector.setGravity(Gravity.CENTER);
                connector.setTextColor(GOLD);
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(12));
                cp.topMargin=-dp(12);cp.bottomMargin=dp(1);
                steps.addView(connector,cp);
            }
        }

        FrameLayout stage=new FrameLayout(context);
        body.addView(stage,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout rotation=new LinearLayout(context);
        rotation.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams turnPosition=new FrameLayout.LayoutParams(-1,dp(48),Gravity.BOTTOM);
        turnPosition.bottomMargin=dp(47);
        stage.addView(rotation,turnPosition);
        TextView left=action("↶",()->rotate(-1)),right=action("↷",()->rotate(1));
        left.setContentDescription("Rotate left");
        right.setContentDescription("Rotate right");
        left.setBackgroundColor(0x00000000);
        rotation.addView(left,new LinearLayout.LayoutParams(0,-1,1));
        right.setBackgroundColor(0x00000000);
        rotation.addView(right,new LinearLayout.LayoutParams(0,-1,1));

        LinearLayout options=column();
        options.setPadding(dp(8),dp(9),dp(8),dp(9));
        options.setBackground(ornate(PANEL,GOLD,2));
        LinearLayout.LayoutParams panel=new LinearLayout.LayoutParams(0,-1,1.65f);
        panel.setMargins(dp(3),dp(23),0,dp(70));
        body.addView(options,panel);
        LinearLayout panelHeader=new LinearLayout(context);
        panelHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView panelTitle=text("Origin",19,true);
        panelTitle.setPadding(dp(5),0,0,dp(8));
        panelHeader.addView(panelTitle,new LinearLayout.LayoutParams(0,-2,1));
        TextView dice=text("⚄",20,true);
        dice.setGravity(Gravity.CENTER);
        panelHeader.addView(dice,new LinearLayout.LayoutParams(dp(29),dp(30)));
        options.addView(panelHeader);
        View separator=new View(context);separator.setBackgroundColor(0x997F8C80);
        options.addView(separator,new LinearLayout.LayoutParams(-1,dp(1)));
        if (hero.background==null) hero.background=PlayerCharacter.BACKGROUNDS[0];
        TextView prompt=text("Choose a Background",13,true);
        prompt.setSingleLine(true);
        prompt.setPadding(dp(5),dp(10),0,dp(9));
        options.addView(prompt);
        ScrollView scroll=new ScrollView(context);
        LinearLayout list=column();
        scroll.addView(list);
        options.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<cards.length;i++) {
            final int index=i;
            LinearLayout item=new LinearLayout(context);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setPadding(dp(6),dp(4),dp(5),dp(4));
            TextView icon=text(CARD_ICONS[i],23,true);
            icon.setGravity(Gravity.CENTER);
            icon.setTextColor(GOLD);
            item.addView(icon,new LinearLayout.LayoutParams(dp(32),-1));
            LinearLayout copy=column();
            TextView name=text(PlayerCharacter.BACKGROUNDS[i],12,true);
            name.setTypeface(Typeface.create("serif",Typeface.NORMAL));
            name.setMaxLines(2);
            name.setEllipsize(null);
            copy.addView(name);
            TextView description=text(DESCRIPTIONS[i],10,false);
            description.setTextColor(INK);
            description.setMaxLines(3);
            copy.addView(description);
            item.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
            item.setMinimumHeight(dp(48));
            item.setOnClickListener(view->{
                hero.background=PlayerCharacter.BACKGROUNDS[index];
                refresh();
                onChange.run();
            });
            cards[i]=item;
            LinearLayout.LayoutParams card=new LinearLayout.LayoutParams(-1,-2);
            card.bottomMargin=dp(4);
            list.addView(item,card);
        }
        LinearLayout featureBox=column();
        featureBox.setPadding(dp(8),dp(6),dp(8),dp(8));
        featureBox.setBackground(ornate(CARD,0xFF668183,2));
        TextView featureHeading=text("Background Feature",13,true);
        featureBox.addView(featureHeading);
        View featureRule=new View(context);
        featureRule.setBackgroundColor(0x776F8B85);
        LinearLayout.LayoutParams fr=new LinearLayout.LayoutParams(-1,dp(1));
        fr.bottomMargin=dp(6);
        featureBox.addView(featureRule,fr);
        feature=text("",11,false);
        feature.setPadding(dp(4),dp(3),dp(2),dp(3));
        featureBox.addView(feature);
        options.addView(featureBox);
        LinearLayout footer=new LinearLayout(context);
        footer.setPadding(dp(6),dp(6),dp(6),dp(6));
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setBackground(ornate(PANEL,GOLD,2));
        layout.addView(footer,new LinearLayout.LayoutParams(-1,dp(58)));
        TextView backButton=action("❮  Back",back);
        backButton.setBackground(ornate(CARD,GOLD,2));
        footer.addView(backButton,new LinearLayout.LayoutParams(dp(78),-1));
        TextView progress=text("◉━━━━○━━━━○━━━━○━━━━○",12,false);
        progress.setGravity(Gravity.CENTER);
        progress.setContentDescription("Step 1 of 7");
        footer.addView(progress,new LinearLayout.LayoutParams(0,-1,1));
        TextView nextButton=action("Next  ❯",next);
        nextButton.setBackground(ornate(CARD,GOLD,2));
        footer.addView(nextButton,new LinearLayout.LayoutParams(dp(78),-1));
        refresh();
    }

    private void rotate(int delta) {
        facing=Math.floorMod(facing+delta,8);
        scene.invalidate();
    }

    private void refresh() {
        for (int i=0;i<cards.length;i++) {
            boolean selected=PlayerCharacter.BACKGROUNDS[i].equals(hero.background);
            cards[i].setSelected(selected);
            cards[i].setBackground(ornate(selected?0xFF454C43:CARD,
                selected?0xFFFFD68A:0xFF4E6C70,selected?3:2));
        }
        feature.setText("⚔  "+hero.background+"\n     "+String.join(" and ",hero.backgroundSkills()));
    }

    private int dp(float value) {
        return Math.round(value*getResources().getDisplayMetrics().density);
    }
    private LinearLayout column() {
        LinearLayout l=new LinearLayout(getContext());
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    private TextView text(String value,int size,boolean serif) {
        TextView view=new TextView(getContext());
        view.setText(value);view.setTextColor(INK);view.setTextSize(size);
        view.setIncludeFontPadding(false);
        if (serif) view.setTypeface(Typeface.create("serif",Typeface.NORMAL));
        return view;
    }
    private TextView action(String label,Runnable action) {
        TextView view=text(label,15,true);
        view.setGravity(Gravity.CENTER);
        view.setBackground(ornate(CARD,GOLD,1));
        view.setOnClickListener(v->action.run());
        return view;
    }

    /** Drawn polygon frames stay native, clickable and density-independent. */
    private Drawable ornate(int fill,int edge,int shape) {
        return new Drawable() {
            final Paint brush=new Paint(Paint.ANTI_ALIAS_FLAG);
            final Path outline=new Path();
            @Override public void draw(Canvas canvas) {
                Rect b=getBounds();
                float l=b.left+dp(1),t=b.top+dp(1),r=b.right-dp(1),bottom=b.bottom-dp(1);
                float cut=Math.min(dp(shape==1||shape==3?12:9),(bottom-t)*.24f);
                outline.reset();
                if(shape==1 || shape==4){
                    outline.moveTo(l+cut,t);outline.lineTo(r-cut,t);
                    outline.lineTo(r,t+(bottom-t)*.5f);
                    outline.lineTo(r-cut,bottom);outline.lineTo(l+cut,bottom);
                    outline.lineTo(l+dp(2),bottom-cut);
                    outline.lineTo(l,t+(bottom-t)*.5f);
                    outline.lineTo(l+dp(2),t+cut);outline.close();
                }else{
                    outline.moveTo(l+cut,t);outline.lineTo(r-cut,t);
                    outline.lineTo(r,t+cut);outline.lineTo(r,bottom-cut);
                    outline.lineTo(r-cut,bottom);outline.lineTo(l+cut,bottom);
                    outline.lineTo(l,bottom-cut);outline.lineTo(l,t+cut);outline.close();
                }
                brush.setStyle(Paint.Style.FILL);brush.setColor(fill);canvas.drawPath(outline,brush);
                brush.setStyle(Paint.Style.STROKE);
                if(shape==3 || shape==4){
                    brush.setStyle(Paint.Style.STROKE);
                    brush.setStrokeWidth(dp(7));brush.setColor(0x35FFCE62);
                    canvas.drawPath(outline,brush);
                    brush.setStrokeWidth(dp(4));brush.setColor(0x88FFD478);
                    canvas.drawPath(outline,brush);
                }
                brush.setStrokeWidth(dp(shape==3||shape==4?1.8f:1.2f));brush.setColor(edge);
                canvas.drawPath(outline,brush);brush.setStyle(Paint.Style.FILL);
            }
            @Override public void setAlpha(int alpha) {}
            @Override public void setColorFilter(ColorFilter filter) {}
            @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
        };
    }

    private GradientDrawable frame(int fill,int edge) {
        GradientDrawable shape=new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(6));
        shape.setStroke(dp(1),edge);
        return shape;
    }

    private final class Scene extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private final Bitmap trees,trees2,bush,rock,person;
        private final Rect sprite=new Rect();
        private final RectF box=new RectF();
        private final Path path=new Path();
        Scene(Context context,String style) {
            super(context);
            trees=asset("factory_world/trees_00.png");
            trees2=asset("factory_world/trees_02.png");
            bush=asset("factory_world/bushes_01.png");
            rock=asset("factory_world/rocks_00.png");
            person=asset("blender_world/"+style+"_idle.png");
        }
        private Bitmap asset(String file) {
            try (InputStream in=getContext().getAssets().open(file)) {
                return BitmapFactory.decodeStream(in);
            } catch (IOException e) { return null; }
        }
        @Override protected void onDraw(Canvas c) {
            float w=getWidth(),h=getHeight(),x=w*.37f,y=h*.68f;
            p.setShader(new android.graphics.LinearGradient(0,0,0,h,
                new int[]{0xFF13272E,0xFF294337,0xFF415A3D,0xFF21352D},
                null,android.graphics.Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p);
            p.setShader(null);
            art(c,trees,-.17f*w,-.06f*h,.62f*w,.65f*h);
            art(c,trees2,.24f*w,-.16f*h,.78f*w,.62f*h);
            art(c,trees2,.59f*w,.12f*h,.98f*w,.75f*h);
            art(c,bush,-.12f*w,.52f*h,.36f*w,.76f*h);
            art(c,rock,.04f*w,.72f*h,.38f*w,.91f*h);
            art(c,bush,.47f*w,.62f*h,.83f*w,.89f*h);
            polygon(c,0xFF5C6070,w*.43f,h*.59f,w*.53f,h*.41f,w*.66f,h*.58f);
            polygon(c,0xFF393E4A,w*.53f,h*.42f,w*.52f,h*.59f,w*.60f,h*.59f);
            p.setColor(0xFF563A28);
            c.drawRect(w*.218f,h*.30f,w*.232f,h*.64f,p);
            c.drawRect(w*.18f,h*.328f,w*.325f,h*.338f,p);
            polygon(c,0xFF624064,w*.232f,h*.34f,w*.317f,h*.34f,w*.317f,h*.47f,w*.275f,h*.50f,w*.232f,h*.47f);
            p.setColor(GOLD);
            c.drawCircle(w*.273f,h*.405f,w*.012f,p);
            p.setColor(0xFF65452C);
            c.drawRect(w*.212f,h*.60f,w*.292f,h*.665f,p);
            p.setColor(0xFFE5B969);
            c.drawRect(w*.238f,h*.574f,w*.266f,h*.609f,p);
            p.setColor(0xFF3E473F);
            c.drawOval(x-w*.146f,y-w*.047f,x+w*.146f,y+w*.09f,p);
            p.setColor(0xFF777C70);
            c.drawOval(x-w*.147f,y-w*.068f,x+w*.147f,y+w*.049f,p);
            p.setColor(0xFF9A9B87);
            c.drawOval(x-w*.115f,y-w*.054f,x+w*.115f,y+w*.032f,p);
            if (person!=null) {
                float size=Math.min(w*.54f,h*.34f);
                sprite.set(0,facing*192,168,(facing+1)*192);
                box.set(x-size*.5f,y-size*160/168,x+size*.5f,y+size*32/168);
                p.setColor(0xFFFFFFFF);
                c.drawBitmap(person,sprite,box,p);
            }
        }
        private void art(Canvas c,Bitmap b,float l,float t,float r,float bottom) {
            if (b==null) return;
            float size=r-l;
            float y=(t+bottom-size)*.5f;
            p.setColor(0xFFFFFFFF);
            box.set(l,y,r,y+size);
            c.drawBitmap(b,null,box,p);
        }
        private void polygon(Canvas c,int color,float... points) {
            p.setColor(color);
            path.reset();path.moveTo(points[0],points[1]);
            for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);
            path.close();
            c.drawPath(path,p);
        }
    }
}
