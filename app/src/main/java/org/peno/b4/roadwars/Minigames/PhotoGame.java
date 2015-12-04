package org.peno.b4.roadwars.Minigames;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Random;

import org.peno.b4.roadwars.CameraActivity;
import org.peno.b4.roadwars.MiniGameManager;
import org.peno.b4.roadwars.R;

/**
 *
 * basic PhotoMinigame
 *
 * Created by timo on 12/3/15.
 */
public class PhotoGame extends Minigame {
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

    private Marker marker;
    private Circle circle;

    private PhotoLocation currentLocation;
    private boolean waitingForFinish = false;

    public PhotoGame(Context context) {
        super(context, "In the picture", 1000);

        currentLocation = PhotoLocation.randomLocation();
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
        clearUI();
    }

    @Override
    public void onStop() {
        clearUI();
    }

    @Override
    public void onLocationChanged(Location location) {
        float[] distanceToStart = new float[3];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                this.currentLocation.latitude, this.currentLocation.longitude, distanceToStart);

        if (distanceToStart[0] < this.currentLocation.distance && !waitingForFinish){

            Intent intent = new Intent(context, CameraActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            //prevent multiple requests to server -> wait unitil response
            waitingForFinish = true;

            //Log.d("Mini", "photo location reached");
        }
    }

    @Override
    public void drawUI(MiniGameManager.UIObjects uiObjects) {
        if (circle != null)
            circle.remove();
        if (marker != null)
            marker.remove();

        LatLng t = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        circle = uiObjects.mMap.addCircle(new CircleOptions()
                .center(t)
                .radius(currentLocation.getDistance()));
        marker = uiObjects.mMap.addMarker(new MarkerOptions().title("photo location").position(t));

        uiObjects.container.setVisibility(View.VISIBLE);
        uiObjects.textView.setText(context.getString(R.string.take_picture_of, currentLocation.getFullName())); //with placeholders: context needed
        uiObjects.container.setBackgroundColor(Color.WHITE);
    }

    private void clearUI() {
        if (marker != null)
            marker.remove();
        if (circle != null)
            circle.remove();
    }
}
