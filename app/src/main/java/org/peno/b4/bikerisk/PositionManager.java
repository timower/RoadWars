package org.peno.b4.bikerisk;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;


public class PositionManager implements LocationListener{
    private static PositionManager instance;

    public interface PositionListener {
        void onLocationChanged(Location location, LatLng pos, PolylineOptions pOpts);
    }

    public boolean started = false;

    private LocationManager locationManager;

    private PositionListener listener;

    private Location lastLocation;

    private PolylineOptions pOptions;

    public CameraPosition lastCameraPosition;

    public PositionManager(Context ctx) {
        this.locationManager = (LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);
        this.instance = this;
    }

    public static PositionManager getInstance() {
        return instance;
    }

    public void start(PositionListener listener) {
        started = true;
        pOptions = new PolylineOptions().color(Color.BLUE).width(5);
        this.listener = listener;
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2 * 1000, 1, this);
    }

    public void stop() {
        started = false;
        this.listener = null;
        locationManager.removeUpdates(this);
    }

    public  void pause(CameraPosition pos) {
        this.listener = null;
        this.lastCameraPosition = pos;
    }

    public void resume(PositionListener listener) {
        this.listener = listener;
        if (lastLocation != null) {
            LatLng pos = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            listener.onLocationChanged(lastLocation, pos, pOptions);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
        pOptions.add(pos);
        listener.onLocationChanged(location, pos, pOptions);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
