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

import org.peno.b4.roadwars.CameraActivity;
import org.peno.b4.roadwars.MiniGameManager;
import org.peno.b4.roadwars.R;

import java.util.Random;

/**
 *
 * basic PhotoMinigame
 *
 * Created by timo on 12/3/15.
 */
public class PhotoGame extends Minigame {
    public enum PhotoLocation {

        ARENBERG("Arenberg Kasteel", 50.863144, 4.683389, 20),
        SCHOUWBURG("Stadsschouwburg", 50.8796686, 4.7048214, 10),
        LADEUZE("Ladeuzeplein",50.87816,	4.7071588, 20),
        KLEINBEGIJNENHOF("Klein Begijnenhof",50.8847899, 4.698877,15),
        GROOTBEGIJNENHOF("Groot Begijnenhof", 50.8713473,4.696988, 15),
        KRUIDTUIN("Kruidtuin",50.8778217,	4.6912699, 20),
        MUSEUMM("Museum-M",50.8783342,4.7049935, 15),
        SINTPIETERSKERK("Sint-Pieterskerk",50.8792963,4.7003878,25),
        UNIVERSITEITSHALLEN("Universiteitshallen",50.877886,4.700564,10),
        STADHUIS("Stadhuis van Leuven",50.8789, 4.7012,15);




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

    private String street;

    private PhotoLocation currentLocation;
    private boolean waitingForFinish = false;

    public PhotoGame(Context context, String street) {
        super(context, "In the picture", 1000);
        this.street = street;
        currentLocation = PhotoLocation.randomLocation();
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public void finish(boolean won) {
        if (won)
            connectionManager.addPoints(street, points);
        clearUI();
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
            intent.putExtra(CameraActivity.EXTRA_TARGETLAT, currentLocation.getLatitude());
            intent.putExtra(CameraActivity.EXTRA_TARGETLONG, currentLocation.getLongitude());
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
