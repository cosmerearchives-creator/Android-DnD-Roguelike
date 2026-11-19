package com.cosmerearchives.dndroguelike;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.res.Configuration;
import android.util.TypedValue;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** First playable, autosaving tab-like sections for a High Elf Fighter. */
public final class CharacterCreatorActivity extends Activity {
    private PlayerCharacter hero;
    private LinearLayout root, contents;
    private TextView summary;
    private CharacterPreviewView preview;
    private boolean previewGame=false;
    private int previewDirection=0,previewAnimation=0;
    private int tab=0;
    private boolean building=false;
    private final Random random=new Random();
    private final String[] tabs={"Identity","Abilities","Background","Fighter","Equipment","Review"};
    private final int bg=0xFF171F2B,fg=0xFFF0E6D1,accent=0xFFBDA06B;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        hero=PlayerCharacter.load(this);
        if(hero==null||hero.completed){hero=new PlayerCharacter();hero.rolls.add(PlayerCharacter.rollScores(random));hero.base=hero.rolls.get(0).clone();}
        if(hero.rolls.isEmpty())hero.rolls.add(hero.base.clone());
        show();
    }
    private TextView text(String value,int size){
        TextView t=new TextView(this);t.setText(value);t.setTextColor(fg);t.setTextSize(size);
        t.setPadding(8,9,8,9);return t;
    }
    private void heading(String s){
        TextView h=text(s,21);h.setTypeface(null,Typeface.BOLD);h.setTextColor(0xFFF3DCAD);contents.addView(h);
    }
    private void note(String s){TextView t=text(s,13);t.setTextColor(0xFFBFC8D2);contents.addView(t);}
    private void persist(){if(!building){hero.save(this);if(summary!=null)summary.setText(stats());if(preview!=null)preview.character(hero);}}
    private int dp(float n){return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,n,getResources().getDisplayMetrics()));}
    private GradientDrawable panel(int color,float radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));
        d.setStroke(dp(1),0xFF3E5261);return d;
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
        root=vertical();root.setBackgroundColor(bg);root.setPadding(dp(12),dp(12),dp(12),dp(8));
        TextView kicker=text("THE ARCHIVES  /  CHARACTER FORGE",12);
        kicker.setTextColor(accent);kicker.setLetterSpacing(.16f);root.addView(kicker);
        TextView headline=text("Forge your legend",25);headline.setTypeface(null,Typeface.BOLD);
        root.addView(headline);
        // Compact progress with all six sections, navigable by tapping step chips.
        LinearLayout nav=horizontal();
        for(int i=0;i<tabs.length;i++){
            final int step=i;
            Button chip=button((i+1)+(tab==i?"  "+tabs[i]:""),()->{tab=step;show();});
            chip.setTextSize(landscape?11:12);
            chip.setPadding(dp(3),0,dp(3),0);
            chip.setBackground(panel(tab==i?0xFF846F4C:0xFF263643,9));
            chip.setTextColor(0xFFFFF4D9);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(42),tab==i?1.8f:1);
            cp.setMargins(dp(2),dp(3),dp(2),dp(7));nav.addView(chip,cp);
        }
        root.addView(nav);
        LinearLayout work=horizontal();
        work.setGravity(Gravity.TOP);
        root.addView(work,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout stage=vertical();
        stage.setBackground(panel(0xFF172734,16));
        stage.setPadding(dp(6),dp(6),dp(6),dp(6));
        if(landscape) {
            LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,-1,.9f);
            pp.setMargins(0,0,dp(10),0);work.addView(stage,pp);
        }else{
            LinearLayout column=vertical();
            work.addView(column,new LinearLayout.LayoutParams(-1,-1));
            LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(225));
            pp.setMargins(0,0,0,dp(8));column.addView(stage,pp);
        }
        LinearLayout stageLabel=horizontal();
        TextView label=text("LIVE CHARACTER PREVIEW",12);label.setLetterSpacing(.1f);label.setTextColor(accent);
        stageLabel.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        TextView hint=text("High Elf · Fighter",12);hint.setTextColor(0xFFABBDC6);stageLabel.addView(hint);
        stage.addView(stageLabel);
        preview=new CharacterPreviewView(this,hero);
        preview.mode(previewGame);preview.direction(previewDirection);preview.animation(previewAnimation);
        stage.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout previewControls=horizontal();
        Button mode=button(previewGame?"GAMEPLAY":"FULL BODY",()->{previewGame=!previewGame;preview.mode(previewGame);show();});
        previewControls.addView(mode,new LinearLayout.LayoutParams(0,dp(39),1.4f));
        Button left=button("◀",()->{previewDirection=(previewDirection+3)%4;preview.direction(previewDirection);});
        Button right=button("▶",()->{previewDirection=(previewDirection+1)%4;preview.direction(previewDirection);});
        previewControls.addView(left,new LinearLayout.LayoutParams(0,dp(39),.65f));
        previewControls.addView(right,new LinearLayout.LayoutParams(0,dp(39),.65f));
        Button motion=button(new String[]{"IDLE","WALK","ATTACK"}[previewAnimation],()->{
            previewAnimation=(previewAnimation+1)%3;preview.animation(previewAnimation);show();
        });
        previewControls.addView(motion,new LinearLayout.LayoutParams(0,dp(39),1f));
        stage.addView(previewControls);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);
        contents=vertical();contents.setPadding(dp(8),dp(7),dp(8),dp(24));
        scroll.addView(contents);
        if(landscape){
            work.addView(scroll,new LinearLayout.LayoutParams(0,-1,1.1f));
        }else{
            LinearLayout column=(LinearLayout)stage.getParent();
            column.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        }
        heading(tabs[tab]);
        if(tab==0)identity();
        if(tab==1)abilities();
        if(tab==2)background();
        if(tab==3)fighter();
        if(tab==4)equipment();
        if(tab==5)review();
        LinearLayout footer=vertical();footer.setBackground(panel(0xFF233443,13));
        footer.setPadding(dp(8),dp(5),dp(8),dp(5));
        summary=text(stats(),13);summary.setGravity(Gravity.CENTER);summary.setTextColor(0xFFE5D2A9);footer.addView(summary);
        LinearLayout actions=horizontal();
        Button previous=button("Previous",()->{if(tab>0){tab--;show();}});
        previous.setEnabled(tab>0);
        actions.addView(previous,new LinearLayout.LayoutParams(0,dp(45),1));
        Button save=button("Save draft",()->{hero.save(this);new AlertDialog.Builder(this).setMessage("Character draft saved locally.").setPositiveButton("OK",null).show();});
        actions.addView(save,new LinearLayout.LayoutParams(0,dp(45),1));
        Button next=button(tab==tabs.length-1?"START ADVENTURE":"Next step",()->{
            if(tab<tabs.length-1){tab++;show();}else finishCharacter();
        });
        next.setBackground(panel(0xFF9F8554,9));next.setTextColor(Color.WHITE);
        actions.addView(next,new LinearLayout.LayoutParams(0,dp(45),1.2f));
        footer.addView(actions);
        root.addView(footer);
        building=false;
        setContentView(root);
        persist();
    }
    private Button button(String caption,Runnable onClick){
        Button b=new Button(this);b.setText(caption);b.setAllCaps(false);b.setTextColor(Color.WHITE);
        b.setTextSize(13);b.setPadding(dp(5),0,dp(5),0);
        b.setBackground(panel(0xFF304657,9));b.setOnClickListener(v->onClick.run());return b;
    }
    private void select(String label,String[] options,String current,java.util.function.Consumer<String> changed){
        LinearLayout field=vertical();field.setPadding(dp(12),dp(7),dp(12),dp(9));
        field.setBackground(panel(0xFF253747,11));
        LinearLayout.LayoutParams fieldParams=new LinearLayout.LayoutParams(-1,-2);fieldParams.setMargins(0,dp(5),0,dp(5));
        TextView title=text(label.toUpperCase(),12);title.setTextColor(accent);title.setLetterSpacing(.1f);
        field.addView(title);
        Spinner spinner=new Spinner(this);
        ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,options){
            @Override public View getView(int pos,View convert,android.view.ViewGroup parent){
                TextView selected=(TextView)super.getView(pos,convert,parent);
                selected.setTextColor(Color.WHITE);selected.setTextSize(16);return selected;
            }
        };
        spinner.setAdapter(a);int index=Arrays.asList(options).indexOf(current);spinner.setSelection(Math.max(0,index));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                String chosen=options[position];if(chosen.equals(current))return;
                changed.accept(chosen);persist();
            }
        });
        field.addView(spinner);contents.addView(field,fieldParams);
    }
    private void identity(){
        heading("Identity and appearance");
        note("Species: High Elf  •  Class: Fighter  •  Level 1");
        contents.addView(text("Character name",15));
        EditText name=new EditText(this);name.setSingleLine(true);name.setText(hero.name);
        name.setHint("Enter your character's name");name.setTextColor(fg);name.setHintTextColor(0xFF87919D);
        name.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){hero.name=s.toString();persist();}
            public void afterTextChanged(Editable e){}
        });contents.addView(name);
        select("Appearance",new String[]{"Female","Male"},hero.gender,v->hero.gender=v);
        select("Skin",new String[]{"Fair","Light","Warm","Olive","Brown","Deep"},hero.skin,v->hero.skin=v);
        select("Hair",new String[]{"Silver","White","Blonde","Brown","Black","Red"},hero.hair,v->hero.hair=v);
        select("Clothing color",new String[]{"Blue","Green","Red","Purple","Black","White"},hero.clothingColor,v->hero.clothingColor=v);
        select("Alignment",new String[]{"Lawful Good","Neutral Good","Chaotic Good","Lawful Neutral","True Neutral","Chaotic Neutral","Lawful Evil","Neutral Evil","Chaotic Evil"},hero.alignment,v->hero.alignment=v);
        contents.addView(text("Age: "+hero.age+" years",15));
        SeekBar age=new SeekBar(this);age.setMax(650);age.setProgress(hero.age-100);
        age.setOnSeekBarChangeListener(seek(v->{hero.age=100+v;persist();}));contents.addView(age);
        contents.addView(text("Height: "+hero.heightInches+" inches",15));
        SeekBar height=new SeekBar(this);height.setMax(18);height.setProgress(hero.heightInches-56);
        height.setOnSeekBarChangeListener(seek(v->{hero.heightInches=56+v;persist();}));contents.addView(height);
        note("Your appearance and starting equipment update the live preview above. More detailed facial customization and markings are planned for later.");
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
        note("Name: "+hero.name+"  •  "+hero.gender+" High Elf  •  Fighter 1");
        note("Background: "+hero.background+"  •  Style: "+hero.style);
        note("Skills: "+hero.skillOne+", "+hero.skillTwo+"  •  Cantrip: "+hero.cantrip);
        note("Fey Ancestry, Darkvision 60 ft, Trance, Keen Senses, Elven Weapon Training");
        note(stats()+"\n"+hero.loadout().summary());
        contents.addView(button("Finish character and enter game",()->finishCharacter()));
    }
    private void finishCharacter(){
        if(!hero.valid()){
            new AlertDialog.Builder(this).setTitle("Incomplete character")
                .setMessage("Choose a name and two different Fighter skills before starting.")
                .setPositiveButton("OK",null).show();return;
        }
        hero.completed=true;hero.save(this);
        startActivity(new Intent(this,GameActivity.class));
        finish();
    }
}
