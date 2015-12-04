package org.peno.b4.roadwars.Minigames;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.peno.b4.roadwars.MiniGameManager;
import org.peno.b4.roadwars.R;
import org.peno.b4.roadwars.Utils;

/**
 * basic race to street game
 *
 * Created by timo on 12/3/15.
 */
public class StreetRaceGame extends Minigame{

    private String targetStreet;
    private LatLng target;
    private String opponent;

    private Marker marker;
    private Circle circle;
    private boolean waitingForFinish = false;

    public StreetRaceGame(Context context, String street, String opponent) {
        super(context, "Race to street", 2000);
        targetStreet = street;
        this.opponent = opponent;
        this.target = Utils.getLatLng(geocoder, street);
    }

    @Override
    public boolean start() {
        if (target == null) {
            target = Utils.getLatLng(geocoder, targetStreet);
        }
        return target != null;
    }

    @Override
    public void finish(boolean won) {
        if (won)
            connectionManager.addPoints(targetStreet, points);
    }

    @Override
    public void stop() {
        connectionManager.stopMinigame(opponent, targetStreet);
        clearUI();
    }

    @Override
    public void onStop() {
        clearUI();
    }

    @Override
    public void onLocationChanged(Location location) {
        float[] distance = new float[3];
        Location.distanceBetween(location.getLatitude(),location.getLongitude(),
                this.target.latitude, this.target.longitude,distance);
        if (distance[0] < 10 && !waitingForFinish){
            connectionManager.finishMinigame(this.opponent, targetStreet);

            //prevent multiple requests to server -> wait unitil response
            waitingForFinish = true;

            Log.d("Mini", "target reached");
        }
    }

    @Override
    public void drawUI(MiniGameManager.UIObjects uiObjects) {
        clearUI();
        marker = uiObjects.mMap.addMarker(new MarkerOptions()
                .title("target")
                .position(target));
        circle = uiObjects.mMap.addCircle(new CircleOptions().center(target).radius(10.0f));

        uiObjects.container.setVisibility(View.VISIBLE);
        uiObjects.textView.setText(context.getString(R.string.race_to, targetStreet)); //with placeholders: context needed
        uiObjects.container.setBackgroundColor(Color.WHITE);
    }

    private void clearUI() {
        if (marker != null)
            marker.remove();
        if (circle != null)
            circle.remove();
    }
}
