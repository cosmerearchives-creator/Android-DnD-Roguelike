package com.cosmerearchives.dndroguelike;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.function.IntConsumer;

final class WildlandsUi {
    static final int INK=0xFFF3DFB8, GOLD=0xFFCFB576, TEAL=0xF0132E35;
    static final Typeface SERIF=Typeface.create("serif",Typeface.NORMAL);

    private WildlandsUi() {}

    static void plate(Canvas canvas, Paint paint, Path path, RectF bounds,
                      float cut, int fill, int edge, float stroke) {
        float l=bounds.left,t=bounds.top,r=bounds.right,b=bounds.bottom;
        path.reset();
        path.moveTo(l+cut,t);path.lineTo(r-cut,t);path.lineTo(r,t+cut);
        path.lineTo(r,b-cut);path.lineTo(r-cut,b);path.lineTo(l+cut,b);
        path.lineTo(l,b-cut);path.lineTo(l,t+cut);path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        canvas.drawPath(path,paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setColor(edge);
        canvas.drawPath(path,paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private static Drawable frame(float density, boolean selected, boolean outer) {
        return new Drawable() {
            private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Path path=new Path();
            private final RectF rect=new RectF();
            @Override public void draw(Canvas canvas) {
                Rect bounds=getBounds();
                rect.set(bounds);
                rect.inset(2*density,2*density);
                plate(canvas,paint,path,rect,10*density,selected?0xFF364744:TEAL,
                    selected || outer?GOLD:0xFF658184,selected?2*density:density);
                if (outer || selected) {
                    rect.inset(4*density,4*density);
                    plate(canvas,paint,path,rect,7*density,Color.TRANSPARENT,
                        selected?0x80EED28A:0x609F925C,density);
                }
            }
            @Override public void setAlpha(int alpha) {}
            @Override public void setColorFilter(android.graphics.ColorFilter filter) {}
            @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        };
    }

    static void choose(Context context,String title,String message,String[] choices,
                       int selected,IntConsumer accept) {
        float density=context.getResources().getDisplayMetrics().density;
        Dialog dialog=new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel=new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding((int)(16*density),(int)(14*density),(int)(16*density),(int)(12*density));
        panel.setBackground(frame(density,false,true));
        panel.addView(label(context,"✦  "+title,22,density));
        if (message!=null) panel.addView(label(context,message,15,density));
        ScrollView scroll=new ScrollView(context);
        LinearLayout list=new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        int screenHeight=context.getResources().getDisplayMetrics().heightPixels;
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,
            Math.min((int)(choices.length*55*density),screenHeight/2)));
        for (int i=0;i<choices.length;i++) {
            final int index=i;
            TextView row=label(context,(i==selected?"◆  ":"◇  ")+choices[i],17,density);
            row.setBackground(frame(density,i==selected,false));
            row.setSelected(i==selected);
            row.setOnClickListener(v->{dialog.dismiss();accept.accept(index);});
            list.addView(row,new LinearLayout.LayoutParams(-1,(int)(53*density)));
        }
        TextView cancel=label(context,"‹  Back",16,density);
        cancel.setGravity(Gravity.CENTER);
        cancel.setOnClickListener(v->dialog.dismiss());
        panel.addView(cancel,new LinearLayout.LayoutParams(-1,(int)(46*density)));
        dialog.setContentView(panel);
        dialog.show();
        if (dialog.getWindow()!=null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(Math.min((int)(390*density),
                context.getResources().getDisplayMetrics().widthPixels-(int)(30*density)),-2);
        }
    }

    static void waitAtBed(Context context,long time,IntConsumer confirm,Runnable closed) {
        float density=context.getResources().getDisplayMetrics().density;
        Dialog dialog=new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel=new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding((int)(16*density),(int)(12*density),(int)(16*density),(int)(10*density));
        panel.setBackground(frame(density,false,true));
        panel.addView(label(context,"✦  Wait at the bed",22,density));
        panel.addView(label(context,"Now  ·  Day "+(WorldClock.day(time)+1)
            +"  ·  "+WorldClock.format(time,true),16,density));

        TextView preview=label(context,"",16,density);
        preview.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
        panel.addView(preview);
        ScrollView scroll=new ScrollView(context);
        LinearLayout hours=new LinearLayout(context);
        hours.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(hours);
        int screenHeight=context.getResources().getDisplayMetrics().heightPixels;
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,screenHeight/3));
        TextView[] buttons=new TextView[24];
        int[] selected={-1};
        IntConsumer select=hour->{
            if (selected[0]>=0) {
                buttons[selected[0]].setBackground(frame(density,false,false));
                buttons[selected[0]].setSelected(false);
            }
            selected[0]=hour;
            buttons[hour].setBackground(frame(density,true,false));
            buttons[hour].setSelected(true);
            long destination=WorldClock.nextHour(time,hour);
            String day=WorldClock.day(time)==WorldClock.day(destination)?"Today":"Tomorrow";
            preview.setText(day+"  ·  "+WorldClock.format(destination,false)
                +"\nWait "+WorldClock.duration(destination-time)
                +"\nOutdoor light  ·  "+WorldLighting.Ambient.at(destination).label);
        };
        for (int h=0;h<24;h+=3) {
            LinearLayout row=new LinearLayout(context);
            for (int column=0;column<3;column++) {
                final int hour=h+column;
                TextView option=label(context,WorldClock.format(hour*WorldClock.HOUR,false),15,density);
                option.setGravity(Gravity.CENTER);
                option.setBackground(frame(density,false,false));
                option.setOnClickListener(v->select.accept(hour));
                buttons[hour]=option;
                row.addView(option,new LinearLayout.LayoutParams(0,(int)(49*density),1));
            }
            hours.addView(row);
        }
        select.accept((int)((time/WorldClock.HOUR+1)%24));
        TextView wait=label(context,"Wait until selected hour",17,density);
        wait.setGravity(Gravity.CENTER);
        wait.setBackground(frame(density,true,false));
        wait.setOnClickListener(v->{
            confirm.accept(selected[0]);
            dialog.dismiss();
        });
        panel.addView(wait,new LinearLayout.LayoutParams(-1,(int)(50*density)));
        TextView cancel=label(context,"Cancel",16,density);
        cancel.setGravity(Gravity.CENTER);
        cancel.setOnClickListener(v->dialog.dismiss());
        panel.addView(cancel);
        dialog.setOnDismissListener(d->closed.run());
        dialog.setContentView(panel);
        dialog.show();
        if (dialog.getWindow()!=null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(Math.min((int)(420*density),
                context.getResources().getDisplayMetrics().widthPixels-(int)(24*density)),-2);
        }
        scroll.post(()->scroll.scrollTo(0,((android.view.View)buttons[selected[0]].getParent()).getTop()));
    }

    private static TextView label(Context context,String text,int sp,float density) {
        TextView view=new TextView(context);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(INK);
        view.setTypeface(SERIF);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding((int)(10*density),(int)(6*density),(int)(10*density),(int)(6*density));
        return view;
    }
}
