package org.peno.b4.bikerisk;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.view.View;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Random;

/**
 * TODO: what if connection is lost or something??
 */
public class MiniGameManager {

    // read: https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html
    public enum MiniGame { //TODO: save string id as discription -> use getString to use
        TARGET_RACE("race to target", 2000),
        PHOTO_ROUND("take picture of place", 1000),
        UI("", 0),
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

    public static class UIObjects {
        public GoogleMap mMap;
        public TextView textView;
        public View container;

        public UIObjects(GoogleMap mMap, TextView view, View container) {
            this.mMap = mMap;
            this.textView = view;
            this.container = container;
        }
    }

    private Geocoder geocoder;
    private UIObjects UIobjects;
    private Context context;


    // street to race to:
    private String street;
    private LatLng target; // coordinates of street
    private PhotoLocation currentLocation;

    // name of  opponent:
    private String opponent;

    private MiniGame runningMiniGame;

    private ConnectionManager connectionManager;

    private static MiniGameManager instance = null;

    private Marker marker;
    private Circle circle;

    private String UIstring;

    private MiniGameManager() {
        runningMiniGame = MiniGame.NONE;
        connectionManager = ConnectionManager.getInstance();
    }

    public static MiniGameManager getInstance() {
        if (instance == null)
            instance = new MiniGameManager();
        return instance;
    }



    public void setContext(Context ctx) {
        this.geocoder = new Geocoder(ctx);
        this.context = ctx.getApplicationContext();
    }

    public void setUIObjects(UIObjects newObjects) {
        this.UIobjects = newObjects;
        drawUI();
    }



    public boolean startRaceGame(String street, String user) {
        if (PositionManager.getInstance().start()) {
            this.street = street;
            this.opponent = user;
            this.target = Utils.getLatLng(geocoder, street);

            runningMiniGame = MiniGame.TARGET_RACE;

            drawUI();
            return true;
        }
        return false;
    }

    public boolean startPhotoRound() {
        if (PositionManager.getInstance().start()) {
            this.runningMiniGame = MiniGame.PHOTO_ROUND;
            this.currentLocation = PhotoLocation.randomLocation();
            drawUI();
            return  true;
        }
        return false;
    }


    public void finish(boolean won) {
        if (won) {
            connectionManager.addPoints(street, runningMiniGame.getpoints());
            Toast.makeText(context, "You won!", Toast.LENGTH_SHORT).show();
            UIstring = context.getString(R.string.player_win, runningMiniGame.getpoints());
        } else {
            Toast.makeText(context, "You lost!", Toast.LENGTH_SHORT).show();
            UIstring = context.getString(R.string.player_lose);
        }
        runningMiniGame = MiniGame.UI;
        drawUI();
    }

    public void stop() {
        switch (runningMiniGame) {
            case TARGET_RACE:
                connectionManager.stopMinigame(opponent, street);
                break;
            case PHOTO_ROUND:
                break;
            case UI:
                UIstring = "";
                break;
        }
        runningMiniGame = MiniGame.NONE;
        drawUI();
    }

    public void onStop() {
        Toast.makeText(context, R.string.opponent_quit, Toast.LENGTH_SHORT).show();
        UIstring = context.getString(R.string.opponent_quit);
        runningMiniGame = MiniGame.UI;
        drawUI();
    }


    private void drawUI() {
        if (UIobjects == null)
            return;
        if (circle != null)
            circle.remove();
        if (marker != null)
            marker.remove();
        switch (runningMiniGame) {
            case TARGET_RACE:
                marker = UIobjects.mMap.addMarker(new MarkerOptions()
                        .title("target")
                        .position(target));
                circle = UIobjects.mMap.addCircle(new CircleOptions().center(target).radius(10.0f));

                UIobjects.container.setVisibility(View.VISIBLE);
                UIobjects.textView.setText(context.getString(R.string.race_to, street)); //with placeholders: context needed
                UIobjects.container.setBackgroundColor(Color.WHITE);
                break;
            case PHOTO_ROUND:
                LatLng t = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                circle = UIobjects.mMap.addCircle(new CircleOptions()
                        .center(t)
                        .radius(currentLocation.getDistance()));
                marker = UIobjects.mMap.addMarker(new MarkerOptions().title("photo location").position(t));

                UIobjects.container.setVisibility(View.VISIBLE);
                UIobjects.textView.setText(context.getString(R.string.take_picture_of, currentLocation.getFullName())); //with placeholders: context needed
                UIobjects.container.setBackgroundColor(Color.WHITE);
                break;
            case UI:
                UIobjects.container.setVisibility(View.VISIBLE);
                UIobjects.textView.setText(UIstring);
                UIobjects.container.setBackgroundColor(Color.WHITE);
                break;
            case NONE:
                UIobjects.container.setVisibility(View.GONE);
                break;
        }
    }


    public void onLocationChanged(Location location) {
        switch (runningMiniGame) {
            case TARGET_RACE:
                float[] distance = new float[3];
                Location.distanceBetween(location.getLatitude(),location.getLongitude(),this.target.latitude,this.target.longitude,distance);
                if (distance[0] < 10){
                    connectionManager.finishMinigame(this.opponent, street);
                    Log.d("Mini", "target reached");
                }
                break;
            case PHOTO_ROUND:
                float[] distanceToStart = new float[3];
                Location.distanceBetween(location.getLatitude(), location.getLongitude(), this.currentLocation.latitude, this.currentLocation.longitude, distanceToStart);
                if (distanceToStart[0] < this.currentLocation.distance){
                    Intent intent = new Intent(context, CameraActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    Log.d("Mini", "photo location reached");
                }
            default:
                break;
        }
    }
}

