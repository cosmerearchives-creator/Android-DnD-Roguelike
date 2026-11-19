package com.cosmerearchives.dndroguelike;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
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
    private void heading(String s){contents.addView(text(s,19));}
    private void note(String s){TextView t=text(s,13);t.setTextColor(0xFFBFC8D2);contents.addView(t);}
    private void persist(){if(!building){hero.save(this);if(summary!=null)summary.setText(stats());}}
    private void show(){
        building=true;
        ScrollView sc=new ScrollView(this);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(14,12,14,26);root.setBackgroundColor(bg);
        sc.addView(root);
        root.addView(text("CREATE CHARACTER  •  HIGH ELF FIGHTER",20));
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);
        Button prev=button("◀",()->{if(tab>0){tab--;show();}});
        Button next=button("▶",()->{if(tab<tabs.length-1){tab++;show();}});
        nav.addView(prev,new LinearLayout.LayoutParams(0,-2,1));nav.addView(text(tabs[tab],17),new LinearLayout.LayoutParams(0,-2,3));nav.addView(next,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(nav);
        contents=new LinearLayout(this);contents.setOrientation(LinearLayout.VERTICAL);
        root.addView(contents);
        if(tab==0)identity();
        if(tab==1)abilities();
        if(tab==2)background();
        if(tab==3)fighter();
        if(tab==4)equipment();
        if(tab==5)review();
        summary=text(stats(),14);root.addView(summary);
        root.addView(button("Save draft",()->{hero.save(this);new AlertDialog.Builder(this).setMessage("Character draft saved locally.").setPositiveButton("OK",null).show();}));
        building=false;
        setContentView(sc);
        persist();
    }
    private Button button(String caption,Runnable onClick){
        Button b=new Button(this);b.setText(caption);b.setAllCaps(false);b.setOnClickListener(v->onClick.run());return b;
    }
    private void select(String label,String[] options,String current,java.util.function.Consumer<String> changed){
        contents.addView(text(label,15));
        Spinner spinner=new Spinner(this);
        ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options);
        spinner.setAdapter(a);int index=Arrays.asList(options).indexOf(current);spinner.setSelection(Math.max(0,index));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                String chosen=options[position];if(chosen.equals(current))return;
                changed.accept(chosen);persist();
            }
        });
        contents.addView(spinner);
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
        note("Advanced face, build, markings, and live character preview are reserved for the later appearance editor.");
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
        if(hero.background.equals("Acolyte"))note("Acolyte also chooses a second background language; language support will be expanded.");
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
            if(item.weapon()&&!item.id.equals("net")&&!item.id.equals("club")&&!item.id.equals("dagger")
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
        note("Background wardrobe unlocks are cosmetic and weightless. Basic equipment statistics and weight appear here; detailed model previews are a later pass.");
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
        contents.addView(button("Finish character and enter game",()->finish()));
    }
    private void finish(){
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
