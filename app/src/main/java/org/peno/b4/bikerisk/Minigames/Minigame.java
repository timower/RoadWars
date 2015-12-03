package org.peno.b4.bikerisk.Minigames;

import android.content.Context;
import android.location.Geocoder;
import android.location.Location;

import org.peno.b4.bikerisk.ConnectionManager;
import org.peno.b4.bikerisk.MiniGameManager;

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

    public abstract void finish(boolean won);
    public abstract void stop();
    public abstract void onStop();

    public abstract void onLocationChanged(Location location);
    public abstract void drawUI(MiniGameManager.UIObjects uiObjects);

}
