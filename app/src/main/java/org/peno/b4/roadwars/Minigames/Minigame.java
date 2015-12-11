package org.peno.b4.roadwars.Minigames;

import android.content.Context;
import android.location.Geocoder;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import org.peno.b4.roadwars.ConnectionManager;
import org.peno.b4.roadwars.MiniGameManager;

/**
 *
 * Created by timo on 12/3/15.
 */
public abstract class Minigame {

    protected String fullName;
    protected int points;

    protected Geocoder geocoder;
    protected Context context;
    protected ConnectionManager connectionManager;

    public Minigame(Context context, String name, int points) {
        this.fullName = name;
        this.points = points;
        this.context = context.getApplicationContext();
        geocoder = new Geocoder(this.context);
        this.connectionManager = ConnectionManager.getInstance();
    }

    public String getFullName() {
        return fullName;
    }

    public int getPoints() {
        return points;
    }

    public abstract boolean start();
    public abstract void finish(boolean won);
    public abstract void stop();
    public abstract void onStop();

    public abstract void onLocationChanged(Location location);
    public abstract void drawUI(MiniGameManager.UIObjects uiObjects);

    public abstract LatLng getTarget();

}
