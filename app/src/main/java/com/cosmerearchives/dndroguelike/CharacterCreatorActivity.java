package com.cosmerearchives.dndroguelike;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public final class CharacterCreatorActivity extends Activity {
    private PlayerCharacter hero;
    private OriginCreatorView creator;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        hero=PlayerCharacter.loadDraft(this);
        if (hero==null) hero=PlayerCharacter.loadCompleted(this);
        if (hero==null) hero=new PlayerCharacter();
        hero.completed=false;
        creator=new OriginCreatorView(this,hero,()->hero.save(this),this::finish,this::enterHub);
        ViewCompat.setOnApplyWindowInsetsListener(creator,(view,insets)->{
            Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()
                | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        setContentView(creator);
        if (saved!=null) {
            creator.facing(saved.getInt("facing",2));
            creator.showStep(saved.getInt("step"));
        }
        ViewCompat.requestApplyInsets(creator);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("step",creator.step());
        state.putInt("facing",creator.facing());
    }

    @Override public void onBackPressed() {
        creator.goBack();
    }

    private void enterHub() {
        String name=hero.name.trim();
        hero.name=name.isEmpty()?"Avatar":name;
        boolean newCharacter = PlayerCharacter.loadCompleted(this) == null;
        hero.completed=true;
        hero.save(this);
        android.content.SharedPreferences.Editor world = getSharedPreferences("wildlands_world",MODE_PRIVATE).edit()
            .putFloat("town_player_x",TownHub.SPAWN_X)
            .putFloat("town_player_y",TownHub.SPAWN_Y)
            .putBoolean("town_quest_active",false);
        if (newCharacter) world.putLong("clock_nanos",WorldClock.START)
            .putString("weather",WorldLighting.Weather.CLEAR.name());
        world.apply();
        startActivity(new Intent(this,PrototypeWorldActivity.class)
            .putExtra(PrototypeWorldActivity.EXTRA_TOWN,true));
        finish();
    }
}
