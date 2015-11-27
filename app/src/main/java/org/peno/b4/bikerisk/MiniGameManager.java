package org.peno.b4.bikerisk;

import android.location.Geocoder;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;


/**
 * Created by timo on 11/14/15.
 * mmeeerr uuitleg
 */
public class MiniGameManager {

    // read: https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html
    public enum MiniGame {
        TARGET_RACE("race to target",1000),
        NONE("",0);

        private String fullName;
        private int points;


         MiniGame(String fullName, int points) {
             this.fullName = fullName;
             this.points = points;

         }

        public String getFullName() {return this.fullName;}

        public int getpoints() {return this.points;}

    }

    private String street;
    private LatLng target;
    private Geocoder geocoder;
    private Boolean first;

    private MiniGame runningMiniGame;
    private ConnectionManager connectionManager;

    private static MiniGameManager instance = null;
    private MiniGameManager() {
        runningMiniGame = MiniGame.NONE;
        connectionManager = ConnectionManager.getInstance();
    }

    public static MiniGameManager getInstance() {
        if (instance == null)
            instance = new MiniGameManager();
        return instance;
    }

    //TODO implement mini games
    public void startRaceGame(String street, String user) {
        this.street = street;
        this.target = Utils.getLatLng(geocoder, street);
        //TODO to server: game started (boolean)
        // tooon finish marker
        runningMiniGame = MiniGame.TARGET_RACE;

    }



    public void setFirst(Boolean result){
        first = result;


    }
    public void onLocationChanged(Location location) {
        switch (runningMiniGame) {
            case TARGET_RACE:
                //TODO: check if user is finished
                float[] distance = new float[3];
                Location.distanceBetween(location.getLatitude(),location.getLongitude(),this.target.latitude,this.target.longitude,distance);
                if (distance[0]<10){
                    //TODO: game stopped (boolean)
                    //connectionManager.getFirst();


                }

                break;
            default:
                break;
        }
    }
}

