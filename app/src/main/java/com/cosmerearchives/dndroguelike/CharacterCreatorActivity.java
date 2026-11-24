package com.cosmerearchives.dndroguelike;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.util.TypedValue;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.Arrays;

/** Autosaving character creation for a new Wildlands adventure. */
public final class CharacterCreatorActivity extends Activity {
    private PlayerCharacter hero;
    private LinearLayout root, contents;
    private CharacterPreviewView preview;
    private boolean previewGame;
    private int previewDirection=0,previewAnimation=0;
    private int tab=0;
    private boolean building;
    private static final String[] TABS={"Appearance","Review"};
    private static final int BG=0xFF13212A, INK=0xFFF3E0B7, BRASS=0xFFC7A96D;
    private static final int PANEL=0xFF1A2D35, SLATE=0xFF30494E, FOREST=0xFF365D50;
    private static final Typeface SERIF=Typeface.create("serif",Typeface.BOLD);
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        hero=PlayerCharacter.loadDraft(this);
        if (hero==null) hero=PlayerCharacter.loadCompleted(this);
        if (hero==null) hero=new PlayerCharacter();
        hero.completed=false;
        if(saved!=null){
            tab=saved.getBoolean("avatarReview") ? 1 : 0;
            previewDirection=saved.getInt("facing");
            previewAnimation=saved.getInt("motion");
            previewGame=saved.getBoolean("smallPreview");
        }
        show();
    }
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        state.putBoolean("avatarReview",tab==1);
        state.putInt("facing",previewDirection);
        state.putInt("motion",previewAnimation);
        state.putBoolean("smallPreview",previewGame);
    }

    private TextView text(String value,int size){
        TextView t=new TextView(this);t.setText(value);t.setTextColor(INK);t.setTextSize(size);
        t.setPadding(dp(4),dp(5),dp(4),dp(5));return t;
    }
    private void heading(String s){
        TextView h=text(s,21);h.setTypeface(SERIF);h.setTextColor(INK);contents.addView(h);
    }
    private void note(String s){TextView t=text(s,13);t.setTextColor(0xFFC1CEC4);contents.addView(t);}
    private void persist(){
        if(building)return;
        hero.save(this);
    }

    private int dp(float n){return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,n,getResources().getDisplayMetrics()));}
    private GradientDrawable panel(int color,float radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));
        d.setStroke(dp(1),0xFF796849);return d;
    }
    private RippleDrawable buttonBackground(int color){
        return new RippleDrawable(ColorStateList.valueOf(0x36F4E2B7),panel(color,6),null);
    }
    private LinearLayout vertical(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;
    }
    private LinearLayout horizontal(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;
    }
    private void show(){
        building=true;
        boolean landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        root=vertical();
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF20333A,BG,0xFF16282C}));
        root.setFocusableInTouchMode(true);
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
            Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()
                |WindowInsetsCompat.Type.displayCutout()|WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left+dp(12),safe.top+dp(6),safe.right+dp(12),safe.bottom+dp(8));
            return WindowInsetsCompat.CONSUMED;
        });
        TextView headline=text("Create your Avatar",26);
        headline.setTypeface(SERIF);
        root.addView(headline);
        View rule=new View(this);
        rule.setBackgroundColor(BRASS);
        root.addView(rule,new LinearLayout.LayoutParams(-1,dp(1)));
        LinearLayout nav=horizontal();
        for (int i=0;i<TABS.length;i++) {
            final int step=i;
            Button chip=button(TABS[i],()->{tab=step;show();});
            chip.setBackground(buttonBackground(tab==i?FOREST:PANEL));
            chip.setSelected(tab==i);
            LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,dp(44),1);
            size.setMargins(dp(2),dp(7),dp(2),dp(7));
            nav.addView(chip,size);
        }
        root.addView(nav);
        LinearLayout work=horizontal();
        work.setGravity(Gravity.TOP);
        root.addView(work,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(this);
        LinearLayout column=vertical();
        scroll.addView(column);
        LinearLayout stage=vertical();
        stage.setBackground(panel(PANEL,8));
        stage.setPadding(dp(6),dp(6),dp(6),dp(6));
        if(landscape) {
            LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,-1,.9f);
            pp.setMargins(0,0,dp(10),0);work.addView(stage,pp);
        }else{
            LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(258));
            pp.setMargins(0,0,0,dp(8));column.addView(stage,pp);
        }
        if(preview==null)preview=new CharacterPreviewView(this);
        else ((ViewGroup)preview.getParent()).removeView(preview);
        preview.petite(hero.petite);
        preview.mode(previewGame);preview.direction(previewDirection);preview.animation(previewAnimation);
        stage.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout previewControls=horizontal();
        Button mode=button(previewGame?"SMALL":"FULL BODY",null);
        mode.setOnClickListener(view->{
            previewGame=!previewGame;
            preview.mode(previewGame);
            mode.setText(previewGame?"SMALL":"FULL BODY");
        });
        previewControls.addView(mode,new LinearLayout.LayoutParams(0,dp(39),1.4f));
        Button left=button("◀",()->{previewDirection=(previewDirection+3)%4;preview.direction(previewDirection);});
        Button right=button("▶",()->{previewDirection=(previewDirection+1)%4;preview.direction(previewDirection);});
        left.setContentDescription("Rotate left");
        right.setContentDescription("Rotate right");
        previewControls.addView(left,new LinearLayout.LayoutParams(0,dp(39),.65f));
        previewControls.addView(right,new LinearLayout.LayoutParams(0,dp(39),.65f));
        String[] motions={"IDLE","WALK","ATTACK"};
        Button motion=button(motions[previewAnimation],null);
        motion.setOnClickListener(view->{
            previewAnimation=(previewAnimation+1)%motions.length;
            preview.animation(previewAnimation);
            motion.setText(motions[previewAnimation]);
        });
        previewControls.addView(motion,new LinearLayout.LayoutParams(0,dp(39),1f));
        stage.addView(previewControls);
        contents=vertical();contents.setPadding(dp(8),dp(8),dp(8),dp(16));
        contents.setBackground(panel(PANEL,8));
        column.addView(contents,new LinearLayout.LayoutParams(-1,-2));
        work.addView(scroll,landscape
            ? new LinearLayout.LayoutParams(0,-1,1.1f)
            : new LinearLayout.LayoutParams(-1,-1));
        if (tab==0) appearance();
        else review();
        LinearLayout footer=vertical();footer.setBackground(panel(PANEL,8));
        footer.setPadding(dp(6),dp(4),dp(6),dp(6));
        LinearLayout actions=horizontal();
        Button next=button(tab==0?"Review":"Enter Hub",()->{
            if (tab==0) {
                tab=1;
                show();
            } else {
                startAdventure();
            }
        });
        next.setBackground(buttonBackground(FOREST));
        LinearLayout.LayoutParams nextParams=new LinearLayout.LayoutParams(0,dp(45),1.2f);
        nextParams.setMarginStart(dp(6));actions.addView(next,nextParams);
        footer.addView(actions);
        LinearLayout.LayoutParams footerParams=new LinearLayout.LayoutParams(-1,-2);
        footerParams.topMargin=dp(6);root.addView(footer,footerParams);
        building=false;
        setContentView(root);
        root.requestFocus();
        ViewCompat.requestApplyInsets(root);
        persist();
    }
    private Button button(String caption,Runnable onClick){
        Button b=new Button(this);b.setText(caption);b.setAllCaps(false);b.setTextColor(INK);
        b.setTextSize(14);b.setPadding(dp(9),0,dp(9),0);
        b.setMinWidth(0);b.setMinimumWidth(0);
        b.setBackground(buttonBackground(SLATE));
        if(onClick!=null)b.setOnClickListener(v->onClick.run());
        return b;
    }
    private void select(String label,String[] options,String current,java.util.function.Consumer<String> changed){
        LinearLayout field=vertical();field.setPadding(dp(12),dp(7),dp(12),dp(9));
        field.setBackground(panel(0xFF22383D,6));
        LinearLayout.LayoutParams fieldParams=new LinearLayout.LayoutParams(-1,-2);fieldParams.setMargins(0,dp(5),0,dp(5));
        TextView title=text(label.toUpperCase(),12);title.setTextColor(BRASS);title.setLetterSpacing(.1f);
        field.addView(title);
        Spinner spinner=new Spinner(this);
        ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,options){
            @Override public View getView(int pos,View convert,android.view.ViewGroup parent){
                TextView selected=(TextView)super.getView(pos,convert,parent);
                selected.setTextColor(INK);selected.setTextSize(16);return selected;
            }
        };
        spinner.setAdapter(a);int index=Arrays.asList(options).indexOf(current);spinner.setSelection(Math.max(0,index));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            private int selected=index;
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                if(position==selected)return;
                selected=position;
                changed.accept(options[position]);persist();
            }
        });
        field.addView(spinner);contents.addView(field,fieldParams);
    }
    private void appearance() {
        heading("Appearance");
        contents.addView(text("Avatar name",15));
        EditText name=new EditText(this);
        name.setSingleLine(true);name.setText(hero.name);name.setHint("Avatar");
        name.setTextColor(INK);name.setHintTextColor(0xFF87919D);
        name.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){
                hero.name=s.toString();
                persist();
            }
            public void afterTextChanged(Editable e){}
        });
        contents.addView(name);
        select("Ear shape",PetiteAppearance.EARS,hero.petite.ears,
            value->setPetite(new PetiteAppearance(value,hero.petite.hair,hero.petite.skin)));
        select("Hair color",PetiteAppearance.HAIR,hero.petite.hair,
            value->setPetite(new PetiteAppearance(hero.petite.ears,value,hero.petite.skin)));
        select("Skin tone",PetiteAppearance.SKIN,hero.petite.skin,
            value->setPetite(new PetiteAppearance(hero.petite.ears,hero.petite.hair,value)));
    }

    private void setPetite(PetiteAppearance value) {
        hero.petite=value;
        preview.petite(value);
    }

    private void review() {
        heading("Review");
        note(avatarName());
        note(hero.petite.ears+" ears  •  "+hero.petite.hair+" hair  •  "+hero.petite.skin+" skin");
    }

    private String avatarName() {
        String name=hero.name.trim();
        return name.isEmpty() ? "Avatar" : name;
    }

    private void startAdventure() {
        hero.name=avatarName();
        hero.completed=true;
        hero.save(this);
        getSharedPreferences("wildlands_world",MODE_PRIVATE).edit()
            .putFloat("town_player_x",TownHub.SPAWN_X)
            .putFloat("town_player_y",TownHub.SPAWN_Y)
            .putBoolean("town_quest_active",false).apply();
        startActivity(new Intent(this,PrototypeWorldActivity.class)
            .putExtra(PrototypeWorldActivity.EXTRA_TOWN,true));
        finish();
    }
}
