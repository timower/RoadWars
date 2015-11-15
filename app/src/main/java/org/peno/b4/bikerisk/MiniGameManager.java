package org.peno.b4.bikerisk;

import android.location.Location;

/**
 * Created by timo on 11/14/15.
 */
public class MiniGameManager {

    //TODO: add properties to each minigame (full name, points, ...)
    // read: https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html
    public enum MiniGame {
        STREET_RACE, // ("race to street", 1000, ...)
        NONE;

        private String fullName;

        //TODO: create initalizer
        //private MiniGame(String fullName) { ... }

        //private String getFullName() {...}
    }

    private MiniGame runningMiniGame;

    private MiniGameManager instance = null;
    private MiniGameManager() {
        runningMiniGame = MiniGame.NONE;
    }

    public MiniGameManager getInstance() {
        if (instance == null)
            instance = new MiniGameManager();
        return instance;
    }

    //TODO implement mini games
    public void startRaceGame() {
        runningMiniGame = MiniGame.STREET_RACE;
    }
    public void onLocationChanged(Location location) {
        switch (runningMiniGame) {
            case STREET_RACE:
                //TODO: check if user is finished
                break;
            default:
                break;
        }
    }
}

