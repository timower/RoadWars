package org.peno.b4.bikerisk;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


public class PositionManager implements LocationListener{
    private static PositionManager instance;

    public boolean started = false;

    private LoginManager mLoginManager;

    private LocationManager locationManager;
    private Location lastLocation;

    private PolylineOptions pOptions;

    public CameraPosition lastCameraPosition;

    private GoogleMap mMap;

    private Marker locMarker;
    private Circle locRad;
    private Polyline userRoute;

    public PositionManager(Context ctx, GoogleMap mMap) {
        this.locationManager = (LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);
        mLoginManager = LoginManager.getInstance();
        this.mMap = mMap;

        instance = this;
    }

    public static PositionManager getInstance() {
        return instance;
    }

    public void start() {
        started = true;
        pOptions = new PolylineOptions().color(Color.BLUE).width(5);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2 * 1000, 1, this);
    }

    public void stop() {
        started = false;
        locationManager.removeUpdates(this);
    }

    public  void pause(CameraPosition pos) {
        this.lastCameraPosition = pos;
        this.mMap = null;
        this.locMarker = null;
        this.locRad = null;
        //this.lastLocation = null;
    }

    public void resume(GoogleMap mMap) {
        this.mMap = mMap;
        if (lastLocation != null) {
            onLocationChanged(lastLocation);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
        pOptions.add(pos);

        if (mMap == null)
            return;

        if (userRoute != null)
            userRoute.remove();

        userRoute = mMap.addPolyline(pOptions);

        if (locMarker != null) {
            locMarker.setPosition(pos);
            locRad.setCenter(pos);
            locRad.setRadius(location.getAccuracy());
        } else {

            locMarker = mMap.addMarker(new MarkerOptions()
                    .title("bike")
                    .snippet(mLoginManager.user + " is here")
                    .position(pos));
            locRad = mMap.addCircle(new CircleOptions()
                    .strokeColor(Color.BLUE)
                    .strokeWidth(1)
                    .fillColor(Color.argb(100 , 0, 0, 200))
                    .center(pos)
                    .radius(location.getAccuracy()));
        }

        //TODO: lookup street -> display
        //TODO: display speed somewhere -> color if in limits
        //TODO: remember last street
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
