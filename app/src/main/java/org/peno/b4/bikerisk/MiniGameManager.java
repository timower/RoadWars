package org.peno.b4.bikerisk;

import android.location.Location;

/**
 * Created by timo on 11/14/15.
 */
public class MiniGameManager {

    private MiniGameManager instance;
    private MiniGameManager() {

    }

    public MiniGameManager getInstance() {
        return instance;
    }

    //TODO implement mini games
    public void startRaceGame() {

    }
    public void onLocationChanged(Location location) {
        //TODO check for running minigames and update/finish
    }
}

