package org.peno.b4.bikerisk;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.List;


public class PositionManager implements LocationListener, LoginManager.LoginResultListener {
    private static PositionManager instance;

    public static class UIObjects {
        public GoogleMap mMap;
        public TextView streetText;
        public TextView speedText;

        public UIObjects(GoogleMap mMap, TextView st, TextView spt) {
            this.mMap = mMap;
            this.streetText = st;
            this.speedText = spt;
        }
    }

    public boolean started = false;

    private LoginManager mLoginManager;

    private LocationManager locationManager;
    private Location lastLocation;

    private PolylineOptions pOptions;

    public CameraPosition lastCameraPosition;

    private UIObjects UIobjects;
    private Geocoder geocoder;

    private Marker locMarker;
    private Circle locRad;
    private Polyline userRoute;

    private String lastStreet;
    private long msecondsInStreet;
    private long curmSeconds;
    private float curSpeed;

    private Context context;

    private class LookupAddressTask extends AsyncTask<LatLng, Void, String> {

        @Override
        protected String doInBackground(LatLng... params) {
            LatLng pos = params[0];
            if (geocoder != null) {
                try {
                    List<Address> locations = geocoder.getFromLocation(pos.latitude,
                            pos.longitude, 1);
                    if (locations.size() > 0) {
                        Address loc = locations.get(0);
                        if (loc.getMaxAddressLineIndex() >= 2) {
                            String street = Utils.removeNumbers(loc.getAddressLine(0));
                            //String city = removeNumbers(loc.getAddressLine(1));
                            return street;
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d("POS", "error, geocoder is null!");
            }
            return null;
        }

        @Override
        protected void onPostExecute(String curStreet) {
            //TODO: fix for off-road ...
            if (curStreet == null || curStreet.equals("")) {
                if (UIobjects != null)
                    UIobjects.streetText.setText("off-road");
                msecondsInStreet += curmSeconds;
            }else {
                if (UIobjects != null)
                    UIobjects.streetText.setText(curStreet);

                if (curStreet.equals(lastStreet)){
                    msecondsInStreet += curmSeconds;
                } else {
                    //TODO: calc points( fix algo)
                    int points = Math.round((msecondsInStreet / 1000) * curSpeed);
                    Toast.makeText(context, "Points: " + points, Toast.LENGTH_SHORT).show();

                    mLoginManager.addPoints(PositionManager.this, lastStreet, points);

                    msecondsInStreet = curmSeconds;
                    lastStreet = curStreet;
                }
            }
        }
    }

    public PositionManager(Context ctx, UIObjects o) {
        this.locationManager = (LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);
        mLoginManager = LoginManager.getInstance();
        this.UIobjects = o;
        this.context = ctx.getApplicationContext();
        this.geocoder = new Geocoder(ctx);

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
        //TODO: save points
        started = false;
        if (locMarker != null)
            locMarker.remove();
        if (locRad != null)
            locRad.remove();

        if (UIobjects != null && pOptions != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            List<LatLng> points = pOptions.getPoints();
            for (LatLng p : points) {
                builder.include(p);
            }
            UIobjects.mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 5));
        }
        locationManager.removeUpdates(this);
    }

    public void destroy() {
        if (started)
            stop();
        pause(null);
        instance = null;
    }

    public  void pause(CameraPosition pos) {
        this.lastCameraPosition = pos;
        this.UIobjects = null;

        this.locMarker = null;
        this.locRad = null;

        this.mLoginManager = null;
        //this.lastLocation = null;
    }

    public void resume(UIObjects uiobj) {
        this.UIobjects = uiobj;

        mLoginManager = LoginManager.getInstance();

        if (this.started && lastLocation != null) {
            onLocationChanged(lastLocation);
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
        pOptions.add(pos);

        curSpeed = location.getSpeed();
        float speed = curSpeed * Utils.MPS_TO_KMH;

        if (lastLocation != null && speed > 10.0f && speed < 45.0f) {
            curmSeconds = (location.getTime() - lastLocation.getTime());
        } else {
            curmSeconds = 0;
        }
        new LookupAddressTask().execute(pos);

        lastLocation = location;

        // UI code:
        if (UIobjects == null)
            return;

        UIobjects.speedText.setText(String.format("%.2f km/h", speed));
        UIobjects.speedText.setTextColor((speed > 10.0f && speed < 45.0f)? Color.GREEN : Color.RED);

        if (userRoute != null)
            userRoute.remove();

        userRoute = UIobjects.mMap.addPolyline(pOptions);

        if (locMarker != null) {
            locMarker.setPosition(pos);
            locRad.setCenter(pos);
            locRad.setRadius(location.getAccuracy());
        } else {

            locMarker = UIobjects.mMap.addMarker(new MarkerOptions()
                    .title("bike")
                    .snippet(mLoginManager.user + " is here")
                    .position(pos));
            locRad = UIobjects.mMap.addCircle(new CircleOptions()
                    .strokeColor(Color.BLUE)
                    .strokeWidth(1)
                    .fillColor(Color.argb(100 , 0, 0, 200))
                    .center(pos)
                    .radius(location.getAccuracy()));
        }
        UIobjects.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 16.5f));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        //TODO: hide error or something
    }

    @Override
    public void onProviderDisabled(String provider) {
        //TODO: error or something (gps is off)
    }

    @Override
    public void onLoginResult(String req, Boolean result, JSONObject response) {
        if (req.equals("add-points")) {
            if (result) {
                Toast.makeText(context, "saved points", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "error saving points", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLoginError(String error) {
        Intent errorIntent = new Intent(context, ErrorActivity.class);
        errorIntent.putExtra(ErrorActivity.EXTRA_MESSAGE, error);
        context.startActivity(errorIntent);
        stop();
    }
}
