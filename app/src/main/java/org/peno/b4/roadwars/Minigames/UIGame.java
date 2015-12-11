package org.peno.b4.roadwars.Minigames;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;

import org.peno.b4.roadwars.MiniGameManager;

/**
 * displays text in minigameUI
 *
 * Created by timo on 12/3/15.
 */
public class UIGame extends Minigame {

    private String text;

    public UIGame(Context context, String text) {
        super(context, "", 0);
        this.text = text;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public void finish(boolean won) {

    }

    @Override
    public void stop() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void drawUI(MiniGameManager.UIObjects uiObjects) {
        uiObjects.container.setVisibility(View.VISIBLE);
        uiObjects.textView.setText(text);
        uiObjects.container.setBackgroundColor(Color.WHITE);
    }

    @Override
    public LatLng getTarget() {
        return null;
    }
}
