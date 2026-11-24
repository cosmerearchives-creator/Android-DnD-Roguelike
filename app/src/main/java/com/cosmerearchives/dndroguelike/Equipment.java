package com.cosmerearchives.dndroguelike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** First playable equipment foundation. All weights are pounds; coins and cosmetic clothing are weightless. */
final class Equipment {
    enum Kind { WEAPON, ARMOR, SHIELD, CONTAINER, SUPPLY, TOOL, KEEPSAKE, MISC }
    enum Slot { MAIN_HAND, OFF_HAND, BODY }
    static final class Item {
        final String id, name, properties, damage, damageType;
        final Kind kind;
        final float weight, capacity;
        final int ac, rangeNormal, rangeLong;
        final boolean twoHanded, versatile, light, finesse, thrown, ranged, protectedItem, stackable;
        Item(String id, String name, Kind kind, float weight, String damage, String damageType,
             String properties, int ac, int normal, int longRange, float capacity, boolean protectedItem, boolean stackable) {
            this.id=id;this.name=name;this.kind=kind;this.weight=weight;this.damage=damage;
            this.damageType=damageType;this.properties=properties;this.ac=ac;
            this.rangeNormal=normal;this.rangeLong=longRange;this.capacity=capacity;
            this.protectedItem=protectedItem;this.stackable=stackable;
            this.twoHanded=properties.contains("Two-handed");
            this.versatile=properties.contains("Versatile");
            this.light=properties.contains("Light");
            this.finesse=properties.contains("Finesse");
            this.thrown=properties.contains("Thrown");
            this.ranged=properties.contains("Ammunition");
        }
        String damageDice(boolean twoHands){
            if(versatile && twoHands){
                int start=properties.indexOf("Versatile (")+11;
                int end=properties.indexOf(')',start);
                if(end>start)return properties.substring(start,end);
            }
            return damage;
        }
        boolean weapon(){return kind==Kind.WEAPON;}
        String detail(){return name+"  •  "+weight+" lb"+(damage.isEmpty()?"":"  •  "+damage+" "+damageType)+(properties.isEmpty()?"":"  •  "+properties);}
    }
    static final class Stack {
        final Item item;
        int count;
        float remainingSeconds=-1;
        int filledPints=0;
        final List<Stack> contents=new ArrayList<>();
        Stack(Item item,int count){this.item=item;this.count=count;}
        float weight(){
            float sum=item.weight*count;
            for(Stack s:contents)sum+=s.weight();
            if(item.id.equals("waterskin"))sum+=filledPints*1.043f;
            return sum;
        }
        float contentsWeight(){float sum=0;for(Stack s:contents)sum+=s.weight();return sum;}
    }
    static final class Bag {
        final List<Stack> roots=new ArrayList<>();
        final String[] slots=new String[3];
        boolean versatileTwoHanded;
        int strength=10, dexterity=10, constitution=10;
        int copper,silver,electrum,gold,platinum;
        Stack find(String id){return findIn(roots,id);}
        private Stack findIn(List<Stack> list,String id){
            for(Stack s:list){if(s.item.id.equals(id))return s;Stack child=findIn(s.contents,id);if(child!=null)return child;}
            return null;
        }
        void add(String id,int qty){
            if(qty<=0)return;
            Item item=get(id);
            if(item==null)throw new IllegalArgumentException("Unknown item "+id);
            if(item.stackable){Stack found=find(id);if(found!=null){found.count+=qty;return;}}
            for(int i=0;i<(item.stackable?1:qty);i++)roots.add(new Stack(item,item.stackable?qty:1));
        }
        float carriedWeight(){float sum=0;for(Stack s:roots)sum+=s.weight();return sum;}
        float capacity(){return Math.max(0,strength)*15f;}
        float movementFactor(){
            float cap=capacity(),weight=carriedWeight();
            if(cap<=0)return weight>0?0:1;
            if(weight<=cap)return 1;
            return Math.max(0,1-((weight/cap-1)/0.5f));
        }
        boolean moveToContainer(String id,String containerId){
            Stack s=find(id),container=find(containerId);
            if(s==null||container==null||s==container||container.item.kind!=Kind.CONTAINER)return false;
            if(!s.contents.isEmpty())return false;
            if(container.contentsWeight()+s.weight()>container.item.capacity)return false;
            if(removeReference(roots,s)){container.contents.add(s);return true;}return false;
        }
        private boolean removeReference(List<Stack> list,Stack wanted){
            if(list.remove(wanted))return true;
            for(Stack s:list)if(removeReference(s.contents,wanted))return true;
            return false;
        }
        boolean consume(String id){
            Stack s=find(id);if(s==null||s.count<1)return false;
            if(s.count>1)s.count--;else removeReference(roots,s);
            if(id.equals(slots[0]))slots[0]=null;
            if(id.equals(slots[1]))slots[1]=null;
            return true;
        }
        boolean equip(String id,Slot slot){
            Item item=get(id);
            if(item==null||find(id)==null)return false;
            int i=slot.ordinal();
            if(slot==Slot.BODY){if(item.kind!=Kind.ARMOR)return false;slots[i]=id;return true;}
            if(slot==Slot.MAIN_HAND){
                if(item.kind!=Kind.WEAPON)return false;
                if(item.twoHanded){slots[Slot.OFF_HAND.ordinal()]=null;versatileTwoHanded=false;}
                if(!id.equals(slots[i]))versatileTwoHanded=false;
                slots[i]=id;return true;
            }
            if(item.kind!=Kind.SHIELD&&item.kind!=Kind.WEAPON&&!id.equals("torch"))return false;
            Item main=get(slots[0]);
            if(main!=null&&(main.twoHanded||(main.versatile&&versatileTwoHanded)))return false;
            if(item.kind==Kind.WEAPON&&item.twoHanded)return false;
            slots[i]=id;return true;
        }
        boolean setTwoHanded(boolean value){
            Item main=get(slots[0]);
            if(main==null||!main.versatile)return false;
            if(value&&slots[1]!=null)return false;
            versatileTwoHanded=value;return true;
        }
        int armorClass(){
            Item armor=get(slots[2]),shield=get(slots[1]);
            int dex=(dexterity-10)/2;if(dexterity<10)dex=(int)Math.floor((dexterity-10)/2.0);
            int ac=10+dex;
            if(armor!=null){
                if(armor.id.equals("chain_mail"))ac=16;
                else if(armor.id.equals("leather_armor"))ac=11+dex;
                else if(armor.id.equals("hide_armor"))ac=12+Math.min(2,dex);
                else if(armor.id.equals("scale_mail"))ac=14+Math.min(2,dex);
                else if(armor.id.equals("chain_shirt"))ac=13+Math.min(2,dex);
                else if(armor.id.equals("breastplate"))ac=14+Math.min(2,dex);
                else if(armor.id.equals("half_plate"))ac=15+Math.min(2,dex);
                else if(armor.id.equals("ring_mail"))ac=14;
                else if(armor.id.equals("splint"))ac=17;
                else if(armor.id.equals("plate"))ac=18;
                else if(armor.id.equals("studded_leather"))ac=12+dex;
                else if(armor.id.equals("padded"))ac=11+dex;
            }
            if(shield!=null&&shield.kind==Kind.SHIELD)ac+=2;
            return ac;
        }
        List<Stack> all(){List<Stack> out=new ArrayList<>();collect(roots,out);return Collections.unmodifiableList(out);}
        private void collect(List<Stack> source,List<Stack> out){for(Stack s:source){out.add(s);collect(s.contents,out);}}
        String summary(){
            String main=slots[0]==null?"None":get(slots[0]).name;
            String off=slots[1]==null?"None":get(slots[1]).name;
            return String.format(Locale.US,"AC %d • Load %.1f / %.0f lb • Speed %d%%\nMain: %s • Off: %s",
                armorClass(),carriedWeight(),capacity(),Math.round(movementFactor()*100),main,off);
        }
    }
    private static final List<Item> CATALOG=new ArrayList<>();
    private static Item make(String id,String name,Kind kind,float lb,String damage,String type,String properties,int ac,int near,int far,float capacity,boolean protectedItem,boolean stackable){
        Item item=new Item(id,name,kind,lb,damage,type,properties,ac,near,far,capacity,protectedItem,stackable);
        CATALOG.add(item);return item;
    }
    private static void weapon(String id,String name,float lb,String dice,String type,String properties,int near,int far){
        make(id,name,Kind.WEAPON,lb,dice,type,properties,0,near,far,0,false,false);
    }
    private static void basic(String id,String name,Kind kind,float lb,boolean stack){
        make(id,name,kind,lb,"","","",0,0,0,0,false,stack);
    }
    private static void container(String id,String name,float lb,float cap){make(id,name,Kind.CONTAINER,lb,"","","",0,0,0,cap,false,false);}
    static {
        // 2014 martial melee weapon table
        weapon("battleaxe","Battleaxe",4,"1d8","slashing","Versatile (1d10)",0,0);
        weapon("flail","Flail",2,"1d8","bludgeoning","",0,0);
        weapon("glaive","Glaive",6,"1d10","slashing","Heavy, Reach, Two-handed",0,0);
        weapon("greataxe","Greataxe",7,"1d12","slashing","Heavy, Two-handed",0,0);
        weapon("greatsword","Greatsword",6,"2d6","slashing","Heavy, Two-handed",0,0);
        weapon("halberd","Halberd",6,"1d10","slashing","Heavy, Reach, Two-handed",0,0);
        weapon("lance","Lance",6,"1d12","piercing","Reach, Special",0,0);
        weapon("longsword","Longsword",3,"1d8","slashing","Versatile (1d10)",0,0);
        weapon("maul","Maul",10,"2d6","bludgeoning","Heavy, Two-handed",0,0);
        weapon("morningstar","Morningstar",4,"1d8","piercing","",0,0);
        weapon("pike","Pike",18,"1d10","piercing","Heavy, Reach, Two-handed",0,0);
        weapon("rapier","Rapier",2,"1d8","piercing","Finesse",0,0);
        weapon("scimitar","Scimitar",3,"1d6","slashing","Finesse, Light",0,0);
        weapon("shortsword","Shortsword",2,"1d6","piercing","Finesse, Light",0,0);
        weapon("trident","Trident",4,"1d6","piercing","Thrown (20/60), Versatile (1d8)",20,60);
        weapon("war_pick","War Pick",2,"1d8","piercing","",0,0);
        weapon("warhammer","Warhammer",2,"1d8","bludgeoning","Versatile (1d10)",0,0);
        weapon("whip","Whip",3,"1d4","slashing","Finesse, Reach",0,0);
        // Martial ranged
        weapon("blowgun","Blowgun",1,"1","piercing","Ammunition, Loading",25,100);
        weapon("hand_crossbow","Hand Crossbow",3,"1d6","piercing","Ammunition, Light, Loading",30,120);
        weapon("heavy_crossbow","Heavy Crossbow",18,"1d10","piercing","Ammunition, Heavy, Loading, Two-handed",100,400);
        weapon("longbow","Longbow",2,"1d8","piercing","Ammunition, Heavy, Two-handed",150,600);
        weapon("net","Net",3,"—","","Special, Thrown (5/15)",5,15);
        // Simple weapon options included in Fighter's equipment choices
        weapon("club","Club",2,"1d4","bludgeoning","Light",0,0);
        weapon("dagger","Dagger",1,"1d4","piercing","Finesse, Light, Thrown (20/60)",20,60);
        weapon("greatclub","Greatclub",10,"1d8","bludgeoning","Two-handed",0,0);
        weapon("handaxe","Handaxe",2,"1d6","slashing","Light, Thrown (20/60)",20,60);
        weapon("javelin","Javelin",2,"1d6","piercing","Thrown (30/120)",30,120);
        weapon("light_hammer","Light Hammer",2,"1d4","bludgeoning","Light, Thrown (20/60)",20,60);
        weapon("mace","Mace",4,"1d6","bludgeoning","",0,0);
        weapon("quarterstaff","Quarterstaff",4,"1d6","bludgeoning","Versatile (1d8)",0,0);
        weapon("sickle","Sickle",2,"1d4","slashing","Light",0,0);
        weapon("spear","Spear",3,"1d6","piercing","Thrown (20/60), Versatile (1d8)",20,60);
        weapon("light_crossbow","Light Crossbow",5,"1d8","piercing","Ammunition, Loading, Two-handed",80,320);
        weapon("dart","Dart",0.25f,"1d4","piercing","Finesse, Thrown (20/60)",20,60);
        weapon("shortbow","Shortbow",2,"1d6","piercing","Ammunition, Two-handed",80,320);
        weapon("sling","Sling",0,"1d4","bludgeoning","Ammunition",30,120);
        make("padded","Padded Armor",Kind.ARMOR,8,"","","Stealth disadvantage",11,0,0,0,false,false);
        make("leather_armor","Leather Armor",Kind.ARMOR,10,"","","",11,0,0,0,false,false);
        make("studded_leather","Studded Leather",Kind.ARMOR,13,"","","",12,0,0,0,false,false);
        make("hide_armor","Hide Armor",Kind.ARMOR,12,"","","",12,0,0,0,false,false);
        make("chain_shirt","Chain Shirt",Kind.ARMOR,20,"","","",13,0,0,0,false,false);
        make("scale_mail","Scale Mail",Kind.ARMOR,45,"","","Stealth disadvantage",14,0,0,0,false,false);
        make("breastplate","Breastplate",Kind.ARMOR,20,"","","",14,0,0,0,false,false);
        make("half_plate","Half Plate",Kind.ARMOR,40,"","","Stealth disadvantage",15,0,0,0,false,false);
        make("ring_mail","Ring Mail",Kind.ARMOR,40,"","","Stealth disadvantage",14,0,0,0,false,false);
        make("chain_mail","Chain Mail",Kind.ARMOR,55,"","","Strength 13, Stealth disadvantage",16,0,0,0,false,false);
        make("splint","Splint",Kind.ARMOR,60,"","","Strength 15, Stealth disadvantage",17,0,0,0,false,false);
        make("plate","Plate",Kind.ARMOR,65,"","","Strength 15, Stealth disadvantage",18,0,0,0,false,false);
        make("shield","Shield",Kind.SHIELD,6,"","","+2 AC",2,0,0,0,false,false);
        container("backpack","Backpack",5,30);
        container("pouch","Pouch",1,6);
        container("waterskin","Waterskin",0,4.18f);
        container("chest","Chest",25,300);
        basic("bedroll","Bedroll",Kind.SUPPLY,7,false);
        basic("mess_kit","Mess Kit",Kind.SUPPLY,1,false);
        basic("tinderbox","Tinderbox",Kind.TOOL,1,false);
        basic("torch","Torch",Kind.SUPPLY,1,false);
        basic("ration","Ration (1 day)",Kind.SUPPLY,2,true);
        basic("rope_hemp","Hempen Rope (50 ft)",Kind.SUPPLY,10,false);
        basic("rope_silk","Silk Rope (50 ft)",Kind.SUPPLY,5,false);
        basic("piton","Piton",Kind.TOOL,0.25f,true);
        basic("hammer","Hammer",Kind.TOOL,3,false);
        basic("crowbar","Crowbar",Kind.TOOL,5,false);
        basic("oil","Oil Flask",Kind.SUPPLY,1,true);
        basic("candle","Candle",Kind.SUPPLY,0,true);
        basic("incense","Incense Stick",Kind.SUPPLY,0,true);
        basic("prayer_book","Prayer Book",Kind.MISC,1,false);
        basic("prayer_wheel","Prayer Wheel",Kind.MISC,1,false);
        basic("holy_symbol","Holy Symbol",Kind.TOOL,1,false);
        basic("vestments","Vestments",Kind.MISC,4,false);
        basic("common_clothes","Common Clothes",Kind.MISC,3,false);
        basic("travelers_clothes","Traveler's Clothes",Kind.MISC,4,false);
        basic("animal_trophy","Animal Trophy",Kind.KEEPSAKE,0,false);
        basic("rank_insignia","Insignia of Rank",Kind.KEEPSAKE,0,false);
        basic("enemy_trophy","Fallen Enemy Trophy",Kind.KEEPSAKE,0,false);
        basic("guild_letter","Guild Introduction Letter",Kind.MISC,0,false);
        basic("map_case","Wooden Map Case with Map",Kind.MISC,1,false);
        basic("bullseye_lantern","Bullseye Lantern",Kind.SUPPLY,2,false);
        basic("miners_pick","Miner's Pick",Kind.TOOL,10,false);
        basic("shovel","Shovel",Kind.TOOL,5,false);
        basic("two_person_tent","Two-person Tent",Kind.SUPPLY,20,false);
        basic("excavation_trinket","Excavation Trinket",Kind.KEEPSAKE,0,false);
        basic("navigator_tools","Navigator's Tools",Kind.TOOL,2,false);
        basic("walking_staff","Walking Staff",Kind.MISC,4,false);
        basic("hunting_trap","Hunting Trap",Kind.TOOL,25,false);
        basic("wilderness_map","Wilderness Map",Kind.MISC,0,false);
        basic("travel_documents","Travel Documents",Kind.MISC,0,false);
        basic("sketchbook","Sketchbook",Kind.MISC,1,false);
        basic("charcoal","Charcoal",Kind.SUPPLY,0,true);
        basic("architect_tools","Architect's Tools",Kind.TOOL,5,false);
        basic("herbalism_kit","Herbalism Kit",Kind.TOOL,3,false);
        basic("cartographer_tools","Cartographer's Tools",Kind.TOOL,6,false);
        basic("calligrapher_supplies","Calligrapher's Supplies",Kind.TOOL,5,false);
        basic("archaeological_kit","Archaeological Kit",Kind.TOOL,5,false);
        basic("dice_set","Dice Set",Kind.TOOL,0,false);
        basic("playing_cards","Playing Card Set",Kind.TOOL,0,false);
        basic("dragonchess","Dragonchess Set",Kind.TOOL,0.5f,false);
        basic("three_dragon_ante","Three-Dragon Ante Set",Kind.TOOL,0,false);
        String[] artisan={"alchemist_supplies","brewer_supplies","calligrapher_supplies_artisan","carpenter_tools",
            "cartographer_tools_artisan","cobbler_tools","cook_utensils","glassblower_tools","jeweler_tools",
            "leatherworker_tools","mason_tools","painter_supplies","potter_tools","smith_tools",
            "tinker_tools","weaver_tools","woodcarver_tools"};
        String[] names={"Alchemist's Supplies","Brewer's Supplies","Calligrapher's Supplies",
            "Carpenter's Tools","Cartographer's Tools","Cobbler's Tools","Cook's Utensils",
            "Glassblower's Tools","Jeweler's Tools","Leatherworker's Tools","Mason's Tools",
            "Painter's Supplies","Potter's Tools","Smith's Tools","Tinker's Tools","Weaver's Tools","Woodcarver's Tools"};
        int[] lbs={8,9,5,6,6,5,8,5,2,5,8,5,3,8,10,5,5};
        for(int i=0;i<artisan.length;i++)basic(artisan[i],names[i],Kind.TOOL,lbs[i],false);
        for(String[] entry:new String[][]{
            {"artifact_fragment","Mysterious Artifact Fragment"},{"guild_id","Guild Identification"},
            {"employer_insignia","Employer Insignia"}})
            make(entry[0],entry[1],Kind.KEEPSAKE,0.1f,"","","Protected keepsake",0,0,0,0,true,false);
    }
    static Item get(String id){if(id==null)return null;for(Item item:CATALOG)if(item.id.equals(id))return item;return null;}
    static List<Item> catalog(){return Collections.unmodifiableList(CATALOG);}
    static void explorerPack(Bag bag){
        bag.add("backpack",1);bag.add("bedroll",1);bag.add("mess_kit",1);
        bag.add("tinderbox",1);bag.add("torch",10);bag.add("ration",10);
        bag.add("waterskin",1);bag.add("rope_hemp",1);
        Stack skin=bag.find("waterskin");if(skin!=null)skin.filledPints=4;
        // Standard supplies fit inside the backpack; bedroll and rope are carried externally.
        bag.moveToContainer("ration","backpack");
        bag.moveToContainer("tinderbox","backpack");
        bag.moveToContainer("mess_kit","backpack");
    }
    static void dungeonPack(Bag bag){
        bag.add("backpack",1);bag.add("crowbar",1);bag.add("hammer",1);
        bag.add("piton",10);bag.add("torch",10);bag.add("tinderbox",1);
        bag.add("ration",10);bag.add("waterskin",1);bag.add("rope_hemp",1);
        Stack skin=bag.find("waterskin");if(skin!=null)skin.filledPints=4;
        bag.moveToContainer("crowbar","backpack");
        bag.moveToContainer("hammer","backpack");
        bag.moveToContainer("piton","backpack");
        bag.moveToContainer("torch","backpack");
        bag.moveToContainer("tinderbox","backpack");
    }
    static Bag fighterDemo(){
        Bag bag=new Bag();
        bag.add("chain_mail",1);bag.add("longsword",1);bag.add("shield",1);
        bag.add("handaxe",2);explorerPack(bag);
        bag.equip("chain_mail",Slot.BODY);bag.equip("longsword",Slot.MAIN_HAND);
        bag.equip("shield",Slot.OFF_HAND);bag.gold=10;
        return bag;
    }
    private Equipment(){}
}
