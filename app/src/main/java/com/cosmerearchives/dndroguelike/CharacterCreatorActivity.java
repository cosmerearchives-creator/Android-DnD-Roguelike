package com.cosmerearchives.dndroguelike;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Autosaving character creation for a new Wildlands adventure. */
public final class CharacterCreatorActivity extends Activity {
    private PlayerCharacter hero;
    private LinearLayout root, contents;
    private TextView summary, appearanceLabel;
    private CharacterPreviewView preview;
    private boolean previewGame=false;
    private int previewDirection=0,previewAnimation=0;
    private int tab=0;
    private boolean building=false;
    private final Random random=new Random();
    private final String[] tabs={"Identity","Abilities","Background","Class","Equipment","Review"};
    private static final int BG=0xFF13212A, INK=0xFFF3E0B7, BRASS=0xFFC7A96D;
    private static final int PANEL=0xFF1A2D35, SLATE=0xFF30494E, FOREST=0xFF365D50;
    private static final Typeface SERIF=Typeface.create("serif",Typeface.BOLD);
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        hero=PlayerCharacter.loadDraft(this);
        if(saved!=null){
            tab=saved.getInt("step");
            previewDirection=saved.getInt("facing");
            previewAnimation=saved.getInt("motion");
            previewGame=saved.getBoolean("smallPreview");
        }
        if(hero==null){hero=new PlayerCharacter();hero.rolls.add(PlayerCharacter.rollScores(random));hero.base=hero.rolls.get(0).clone();}
        if(hero.rolls.isEmpty())hero.rolls.add(hero.base.clone());
        show();
    }
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        state.putInt("step",tab);
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
        summary.setText(stats());
    }

    private void cycleAppearance(int direction){
        hero.appearance=hero.appearance.cycle(direction);
        preview.appearance(hero.appearance);
        appearanceLabel.setText(appearanceText());
        persist();
    }

    private String appearanceText(){
        int inches=hero.appearance.heightInches;
        return hero.appearance.label+"  ·  "+inches/12+"′"+inches%12+"″";
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
        TextView headline=text("Create your High Elf",26);
        headline.setTypeface(SERIF);
        root.addView(headline);
        View rule=new View(this);
        rule.setBackgroundColor(BRASS);
        root.addView(rule,new LinearLayout.LayoutParams(-1,dp(1)));
        HorizontalScrollView steps=new HorizontalScrollView(this);
        steps.setHorizontalScrollBarEnabled(false);
        steps.setFillViewport(true);
        LinearLayout nav=horizontal();
        Button activeStep=null;
        for(int i=0;i<tabs.length;i++){
            final int step=i;
            Button chip=button(tabs[i],()->{tab=step;show();});
            chip.setBackground(buttonBackground(tab==i?FOREST:PANEL));
            chip.setSelected(tab==i);
            if(tab==i)activeStep=chip;
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,dp(44),1f);
            cp.setMargins(dp(2),dp(7),dp(2),dp(7));nav.addView(chip,cp);
        }
        steps.addView(nav);
        root.addView(steps);
        Button selected=activeStep;
        steps.post(()->steps.scrollTo(selected.getLeft()-dp(8),0));
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
        LinearLayout appearanceControls=horizontal();
        Button previousAppearance=button("‹",()->cycleAppearance(-1));
        previousAppearance.setContentDescription("Previous appearance");
        appearanceControls.addView(previousAppearance,new LinearLayout.LayoutParams(dp(44),dp(40)));
        appearanceLabel=text(appearanceText(),14);
        appearanceLabel.setGravity(Gravity.CENTER);
        appearanceLabel.setTypeface(SERIF);
        appearanceLabel.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        appearanceControls.addView(appearanceLabel,new LinearLayout.LayoutParams(0,-2,1));
        Button nextAppearance=button("›",()->cycleAppearance(1));
        nextAppearance.setContentDescription("Next appearance");
        appearanceControls.addView(nextAppearance,new LinearLayout.LayoutParams(dp(44),dp(40)));
        stage.addView(appearanceControls);
        if(preview==null)preview=new CharacterPreviewView(this,hero.appearance);
        else ((ViewGroup)preview.getParent()).removeView(preview);
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
        heading(tabs[tab]);
        if(tab==0)identity();
        if(tab==1)abilities();
        if(tab==2)background();
        if(tab==3)fighter();
        if(tab==4)equipment();
        if(tab==5)review();
        LinearLayout footer=vertical();footer.setBackground(panel(PANEL,8));
        footer.setPadding(dp(6),dp(4),dp(6),dp(6));
        summary=text(stats(),13);summary.setGravity(Gravity.CENTER);summary.setTextColor(0xFFE5D2A9);footer.addView(summary);
        LinearLayout actions=horizontal();
        Button previous=button("Previous",()->{if(tab>0){tab--;show();}});
        previous.setEnabled(tab>0);
        actions.addView(previous,new LinearLayout.LayoutParams(0,dp(45),1));
        previous.setAlpha(tab==0?.55f:1f);
        Button next=button(tab==tabs.length-1?"Start adventure":"Next step",()->{
            if(tab<tabs.length-1){tab++;show();}else finishCharacter();
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
    private void identity(){
        note("Level 1  ·  Draft autosaves");
        note("Use the arrows above the model to choose your appearance.");
        contents.addView(text("Character name",15));
        EditText name=new EditText(this);name.setSingleLine(true);name.setText(hero.name);
        name.setHint("Enter your character's name");name.setTextColor(INK);name.setHintTextColor(0xFF87919D);
        name.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){hero.name=s.toString();persist();}
            public void afterTextChanged(Editable e){}
        });contents.addView(name);
        select("Alignment",new String[]{"Lawful Good","Neutral Good","Chaotic Good","Lawful Neutral","True Neutral","Chaotic Neutral","Lawful Evil","Neutral Evil","Chaotic Evil"},hero.alignment,v->hero.alignment=v);
        TextView ageLabel=text("Age: "+hero.age+" years",15);
        contents.addView(ageLabel);
        SeekBar age=new SeekBar(this);age.setMax(650);age.setProgress(hero.age-100);
        age.setOnSeekBarChangeListener(seek(v->{
            hero.age=100+v;
            ageLabel.setText("Age: "+hero.age+" years");
            persist();
        }));contents.addView(age);
    }
    private SeekBar.OnSeekBarChangeListener seek(java.util.function.IntConsumer changed){
        return new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int v,boolean fromUser){if(fromUser)changed.accept(v);}
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        };
    }
    private void abilities(){
        heading("Ability rolls: 4d6, drop lowest");
        note("Generate up to four complete sets, choose one, then assign its six scores without duplicates. High Elf +2 DEX and +1 INT apply afterward.");
        for(int j=0;j<hero.rolls.size();j++){
            final int index=j;int[] set=hero.rolls.get(j);
            contents.addView(button("Set "+(j+1)+": "+Arrays.toString(set),()->assignSet(index)));
        }
        if(hero.rolls.size()<4)contents.addView(button("Reroll full set ("+(4-hero.rolls.size())+" remaining)",()->{
            hero.rolls.add(PlayerCharacter.rollScores(random));hero.base=hero.rolls.get(hero.rolls.size()-1).clone();persist();show();
        }));
        note("Currently assigned base scores: "+Arrays.toString(hero.base));
        for(int i=0;i<6;i++){
            final int ability=i;
            String[] opts=new String[6];for(int k=0;k<6;k++)opts[k]=PlayerCharacter.ABILITIES[k]+" = "+hero.base[k];
            // Swap two ability slots to preserve exactly one instance of each rolled score.
            contents.addView(text(PlayerCharacter.ABILITIES[i]+": "+hero.score(i)+" (base "+hero.base[i]+")",15));
            Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,PlayerCharacter.ABILITIES));
            s.setSelection(i);
            s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                boolean initialized=false;
                public void onNothingSelected(AdapterView<?> parent){}
                public void onItemSelected(AdapterView<?> parent,View v,int position,long id){
                    if(!initialized){initialized=true;return;}
                    if(position==ability)return;
                    int tmp=hero.base[ability];hero.base[ability]=hero.base[position];hero.base[position]=tmp;
                    persist();show();
                }
            });contents.addView(s);
        }
    }
    private void assignSet(int index){hero.base=hero.rolls.get(index).clone();persist();show();}
    private void background(){
        heading("Background");
        select("Background",PlayerCharacter.BACKGROUNDS,hero.background,v->{hero.background=v;hero.save(this);show();});
        note("Background skills: "+Arrays.toString(hero.backgroundSkills()));
        select("High Elf bonus language",PlayerCharacter.LANGUAGES,hero.languageOne,v->hero.languageOne=v);
        if(hero.background.equals("Acolyte")||!hero.background.equals("Caravan Guard"))
            select("Background language",PlayerCharacter.LANGUAGES,hero.languageTwo,v->hero.languageTwo=v);
        if(hero.background.equals("Acolyte"))select("Second Acolyte language",PlayerCharacter.LANGUAGES,hero.languageThree,v->hero.languageThree=v);
        if(hero.background.equals("Wilderness Pathfinder")||hero.background.equals("Relic Seeker")){
            select("Background tool",hero.background.equals("Wilderness Pathfinder")?
                new String[]{"cartographer_tools","herbalism_kit"}:
                new String[]{"cartographer_tools","calligrapher_supplies"},hero.toolChoice,v->hero.toolChoice=v);
        }
        if(hero.background.equals("Caravan Guard"))
            select("Gaming set",new String[]{"dice_set","playing_cards","dragonchess","three_dragon_ante"},hero.gamingChoice,v->hero.gamingChoice=v);
        if(hero.background.equals("Guild Apprentice"))
            select("Artisan's tools",new String[]{"smith_tools","carpenter_tools","brewer_supplies","cook_utensils","weaver_tools","tinker_tools","jeweler_tools","mason_tools","painter_supplies","potter_tools","leatherworker_tools","woodcarver_tools","cobbler_tools","glassblower_tools","alchemist_supplies","calligrapher_supplies_artisan","cartographer_tools_artisan"},hero.artisanChoice,v->hero.artisanChoice=v);
    }
    private void fighter(){
        select("Starting class",PlayerCharacter.STARTING_CLASSES,hero.startingClass,v->hero.startingClass=v);
        heading("Fighter choices");
        select("Fighting Style",PlayerCharacter.STYLES,hero.style,v->hero.style=v);
        select("First skill proficiency",PlayerCharacter.FIGHTER_SKILLS,hero.skillOne,v->hero.skillOne=v);
        select("Second skill proficiency",PlayerCharacter.FIGHTER_SKILLS,hero.skillTwo,v->hero.skillTwo=v);
        select("High Elf wizard cantrip (INT)",PlayerCharacter.CANTRIPS,hero.cantrip,v->hero.cantrip=v);
        note("Fighter saving throw proficiencies: Strength and Constitution. Level 1 feature: Second Wind.");
        if(hero.skillOne.equals(hero.skillTwo))note("Choose two different Fighter skills before completing.");
    }
    private void equipment(){
        heading("Fighter starting equipment");
        select("Armor selection",new String[]{"chain_mail","leather_armor"},hero.armorChoice,v->{hero.armorChoice=v;show();});
        note("Leather Armor includes a Longbow with unlimited ordinary arrows.");
        List<String> martial=new ArrayList<>();
        for(Equipment.Item item:Equipment.catalog()){
            if(item.weapon()&&!item.id.equals("club")&&!item.id.equals("dagger")
                 &&!item.id.equals("handaxe")&&!item.id.equals("javelin")&&!item.id.equals("light_hammer")
                 &&!item.id.equals("quarterstaff")&&!item.id.equals("spear")&&!item.id.equals("mace")
                 &&!item.id.equals("sickle")&&!item.id.equals("greatclub")&&!item.id.equals("dart")
                 &&!item.id.equals("shortbow")&&!item.id.equals("sling")&&!item.id.equals("light_crossbow"))martial.add(item.id);
        }
        select("Martial weapon",martial.toArray(new String[0]),hero.martialOne,v->{hero.martialOne=v;show();});
        List<String> other=new ArrayList<>();other.add("shield");other.addAll(martial);
        select("Shield or second martial weapon",other.toArray(new String[0]),hero.martialTwo,v->{hero.martialTwo=v;show();});
        select("Light crossbow or two handaxes",new String[]{"light_crossbow","handaxes"},hero.rangedChoice,v->{hero.rangedChoice=v;show();});
        select("Adventuring pack",new String[]{"explorer","dungeon"},hero.packChoice,v->{hero.packChoice=v;show();});
        Equipment.Bag preview=hero.loadout();
        note(preview.summary()+"\nCoins: "+preview.gold+" gp (weightless)");
        note("Background wardrobe unlocks remain cosmetic and weightless. Equipped armor, shield, and main-hand weapon are reflected in the preview.");
    }
    private String stats(){
        return "STR "+hero.score(0)+"  DEX "+hero.score(1)+"  CON "+hero.score(2)+
            "  INT "+hero.score(3)+"  WIS "+hero.score(4)+"  CHA "+hero.score(5)+
            "\nHP "+hero.maxHp()+"  AC "+hero.loadout().armorClass()+
            "  Speed 30 ft  •  Carry "+hero.maxCarry()+" lb";
    }
    private void review(){
        heading("Final review");
        note("Name: "+hero.name+"  •  High Elf  •  "+hero.startingClass+" 1");
        note("Background: "+hero.background+"  •  Style: "+hero.style);
        note("High Elf language: "+hero.languageOne);
        note("Skills: "+hero.skillOne+", "+hero.skillTwo+"  •  Cantrip: "+hero.cantrip);
        note("Fey Ancestry, Darkvision 60 ft, Trance, Keen Senses, Elven Weapon Training");
        note(stats()+"\n"+hero.loadout().summary());

    }
    private void finishCharacter(){
        if(!hero.valid()){
            new AlertDialog.Builder(this).setTitle("Incomplete character")
                .setMessage("Choose a name and two different Fighter skills before starting.")
                .setPositiveButton("OK",null).show();return;
        }
        if(PlayerCharacter.loadCompleted(this)!=null){
            new AlertDialog.Builder(this).setTitle("Start a new adventure?")
                .setMessage("Starting will replace the active character with "+hero.name+".")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Start adventure",(dialog,which)->startAdventure()).show();
            return;
        }
        startAdventure();
    }

    private void startAdventure(){
        hero.completed=true;hero.save(this);
        startActivity(new Intent(this,OutdoorActivity.class));
        finish();
    }
}
