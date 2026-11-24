package com.cosmerearchives.dndroguelike;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Immutable 2014 racial rules, independent of avatar artwork. */
final class RaceDefinition {
    static final RaceDefinition HIGH_ELF=new RaceDefinition("high_elf","elf",new int[]{0,2,0,1,0,0},30,
        Arrays.asList("perception"),Arrays.asList("common","elvish"),
        Arrays.asList("darkvision","keen_senses","fey_ancestry","trance","elf_weapon_training","high_elf_cantrip","extra_language"));
    final String id, family;
    private final int[] bonuses;
    final int speedFeet;
    final List<String> skills,languages,traits;
    private RaceDefinition(String id,String family,int[] bonuses,int speedFeet,List<String> skills,List<String> languages,List<String> traits){
        this.id=id;this.family=family;this.bonuses=bonuses.clone();this.speedFeet=speedFeet;
        this.skills=Collections.unmodifiableList(new java.util.ArrayList<>(skills));
        this.languages=Collections.unmodifiableList(new java.util.ArrayList<>(languages));
        this.traits=Collections.unmodifiableList(new java.util.ArrayList<>(traits));
    }
    final int bonusLanguages=1, darkvisionFeet=60;
    final String size="medium", cantripAbility="intelligence";
    final List<String> weaponProficiencies=Collections.unmodifiableList(Arrays.asList("longsword","shortsword","shortbow","longbow"));
    float movementFactor(){return speedFeet/30f;}
    int bonus(int index){return bonuses[index];}
}
