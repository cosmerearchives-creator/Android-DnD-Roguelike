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
    private final TextView[] cards=new TextView[PlayerCharacter.BACKGROUNDS.length];
    private final Scene scene;
    private int facing=2;

    OriginCreatorView(Context context,PlayerCharacter hero,Runnable onChange,Runnable back,Runnable next) {
        super(context);
        this.hero=hero;this.onChange=onChange;
        setBackgroundColor(0xFF14292E);
        scene=new Scene(context,hero.appearance==ElfAppearance.PETITE ? "petite" : "classic");
        addView(scene,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout layout=column();
        layout.setPadding(dp(6),dp(14),dp(6),dp(8));
        addView(layout,new FrameLayout.LayoutParams(-1,-1));
        TextView heading=text("Character Creation",25,true);
        heading.setGravity(Gravity.CENTER);
        layout.addView(heading,new LinearLayout.LayoutParams(-1,dp(40)));
        TextView subtitle=text("Create your adventurer",13,false);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle,new LinearLayout.LayoutParams(-1,dp(28)));

        LinearLayout body=new LinearLayout(context);
        layout.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout steps=column();
        steps.setGravity(Gravity.CENTER_VERTICAL);
        body.addView(steps,new LinearLayout.LayoutParams(dp(75),-1));
        for(int i=0;i<STEPS.length;i++){
            TextView label=text(STEPS[i],i==5?10:12,true);
            label.setGravity(Gravity.CENTER);
            label.setBackground(frame(i==0?0xF03D4C49:PANEL,i==0?GOLD:0xFF577074));
            LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(-1,dp(39));
            size.bottomMargin=dp(9);
            steps.addView(label,size);
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
        rotation.addView(left,new LinearLayout.LayoutParams(0,-1,1));
        rotation.addView(right,new LinearLayout.LayoutParams(0,-1,1));

        LinearLayout options=column();
        options.setPadding(dp(7),dp(8),dp(7),dp(7));
        options.setBackground(frame(PANEL,GOLD));
        LinearLayout.LayoutParams panel=new LinearLayout.LayoutParams(0,-1,1.40f);
        panel.setMargins(dp(3),dp(14),0,dp(46));
        body.addView(options,panel);
        options.addView(text("Origin",19,true));
        if (hero.background==null) hero.background=PlayerCharacter.BACKGROUNDS[0];
        options.addView(text("Choose a background",12,true));
        ScrollView scroll=new ScrollView(context);
        LinearLayout list=column();
        scroll.addView(list);
        options.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<cards.length;i++) {
            final int index=i;
            TextView item=text("",12,false);
            item.setMinHeight(dp(54));
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(6),dp(7),dp(5),dp(7));
            item.setOnClickListener(view->{
                hero.background=PlayerCharacter.BACKGROUNDS[index];
                refresh();
                onChange.run();
            });
            cards[i]=item;
            LinearLayout.LayoutParams card=new LinearLayout.LayoutParams(-1,-2);
            card.bottomMargin=dp(5);
            list.addView(item,card);
        }
        feature=text("",12,false);
        feature.setPadding(dp(5),dp(7),dp(5),dp(6));
        feature.setBackground(frame(CARD,0xFF668183));
        options.addView(feature);
        LinearLayout footer=new LinearLayout(context);
        footer.setPadding(dp(6),dp(6),dp(6),dp(6));
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setBackground(frame(PANEL,GOLD));
        layout.addView(footer,new LinearLayout.LayoutParams(-1,dp(58)));
        footer.addView(action("‹ Back",back),new LinearLayout.LayoutParams(dp(68),-1));
        TextView progress=text("●  ○  ○  ○  ○  ○  ○",11,false);
        progress.setGravity(Gravity.CENTER);
        progress.setContentDescription("Step 1 of 7");
        footer.addView(progress,new LinearLayout.LayoutParams(0,-1,1));
        footer.addView(action("Next ›",next),new LinearLayout.LayoutParams(dp(68),-1));
        refresh();
    }

    private void rotate(int delta) {
        facing=Math.floorMod(facing+delta,8);
        scene.invalidate();
    }

    private void refresh() {
        for (int i=0;i<cards.length;i++) {
            String name=PlayerCharacter.BACKGROUNDS[i];
            boolean selected=name.equals(hero.background);
            android.text.SpannableString copy=new android.text.SpannableString(name+"\n"+DESCRIPTIONS[i]);
            copy.setSpan(new android.text.style.StyleSpan(Typeface.BOLD),0,name.length(),0);
            cards[i].setText(copy);
            cards[i].setSelected(selected);
            cards[i].setBackground(frame(selected?0xFF4A5449:CARD,selected?GOLD:0xFF4E6C70));
        }
        feature.setText("Background skills\n"+String.join(" and ",hero.backgroundSkills()));
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
        if (serif) view.setTypeface(Typeface.create("serif",Typeface.NORMAL));
        return view;
    }
    private TextView action(String label,Runnable action) {
        TextView view=text(label,15,true);
        view.setGravity(Gravity.CENTER);
        view.setBackground(frame(CARD,GOLD));
        view.setOnClickListener(v->action.run());
        return view;
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
