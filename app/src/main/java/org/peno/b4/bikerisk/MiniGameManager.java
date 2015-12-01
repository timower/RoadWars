package org.peno.b4.bikerisk;

import android.content.Intent;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.Random;

/**
 * Created by timo on 11/14/15.
 * mmeeerr uuitleg
 */
public class MiniGameManager {

    // read: https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html
    public enum MiniGame {
        TARGET_RACE("race to target",1000),
        PHOTO_ROUND("take picture of place", 1000),
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

    public enum PhotoLocation {

        ARENBERG("Campus Arenberg", 50.863399, 4.678033, 20);


        private String fullName;
        private double latitude;
        private double longitude;
        private int distance;

        PhotoLocation(String fullName, double latitude, double longitude, int distance){
                this.fullName = fullName;
                this.latitude = latitude;
                this.longitude = longitude;
                this.distance = distance;
        }

        public String getFullName() {return this.fullName;}
        public double getLatitude() {return this.latitude;}
        public double getLongitude() {return this.longitude;}
        public int getDistance() {return this.distance;}

        public static final Random Random = new Random();

        public static PhotoLocation randomLocation(){
            PhotoLocation values[] = PhotoLocation.values();
            return values[Random.nextInt(values.length)];
        }
    }

    private String street;
    private Geocoder geocoder;
    private LatLng target;
    private Boolean first;

    private PhotoLocation currentLocation;

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

    public void resume() {
        switch (runningMiniGame) {
            case PHOTO_ROUND:
                //teken ui opnieuw
                break;
        }
    }

    public void startPhotoRound() {
        this.runningMiniGame = MiniGame.PHOTO_ROUND;
        this.currentLocation = PhotoLocation.randomLocation();
    }

    public void setFirst(Boolean result){
        first = result;


    }
    public void onLocationChanged(Location location) {
        switch (runningMiniGame) {
            case TARGET_RACE:
                //TODO: visible if user is finished
                float[] distance = new float[3];
                Location.distanceBetween(location.getLatitude(),location.getLongitude(),this.target.latitude,this.target.longitude,distance);
                if (distance[0]<10){
                    //TODO: game stopped
                    //connectionManager.getFirst();
                }
                break;
            case PHOTO_ROUND:
                float[] distanceToStart = new float[3];
                Location.distanceBetween(location.getLatitude(), location.getLongitude(), this.currentLocation.latitude, this.currentLocation.longitude, distanceToStart);
                if (distanceToStart[0] < this.currentLocation.distance){
                    //Intent intent = new Intent(CameraActivity);
                    Log.d("Mini", "photo location reached");
                }
            default:
                break;
        }
    }
}

