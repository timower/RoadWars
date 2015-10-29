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
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class PositionManager implements LocationListener, LoginManager.LoginResultListener {
    private static PositionManager instance;

    public static class UIObjects {
        public GoogleMap mMap;
        public TextView speedText;
        public TableLayout table;
        public UIObjects(GoogleMap mMap, TextView spt, TableLayout table) {
            this.mMap = mMap;
            this.speedText = spt;
            this.table = table;
        }
    }

    public static class LocationInfo {
        public LatLng pos;
        public float speed;
        public LocationInfo(LatLng pos, float speed) {
            this.pos = pos;
            this.speed = speed;
        }
    }

    public static class StreetPoints {
        public String street;
        public int points;
        public StreetPoints(String street, int points) {
            this.street = street;
            this.points = points;
        }
    }

    public boolean started = false;

    private LoginManager mLoginManager;

    private LocationManager locationManager;

    private Location lastLocation;

    private ArrayList<LocationInfo> routePoints;

    private PolylineOptions pOptions;

    public CameraPosition lastCameraPosition;

    private UIObjects UIobjects;
    private Geocoder geocoder;

    private Marker locMarker;
    private Circle locRad;
    private Polyline userRoute;

    private Context context;

    private class LookupAddressTask extends AsyncTask<LocationInfo, StreetPoints, Void> {

        @Override
        protected Void doInBackground(LocationInfo... params) {
            String lastStreet = "";
            int points = 0;
            LatLng lastPos = null;

            float[] results = new float[3];

            for (LocationInfo point : params) {
                String street = Utils.lookupStreet(geocoder, point.pos);
                if (street != null) {
                    if (!street.equals("") && lastStreet.equals("")) {
                        lastStreet = street;
                    } else if (street.equals(lastStreet) && lastPos != null) {
                        if (point.speed > (10.0f / Utils.MPS_TO_KMH) &&
                                point.speed < (45.0f / Utils.MPS_TO_KMH)) {
                            Location.distanceBetween(lastPos.latitude, lastPos.longitude,
                                    point.pos.latitude, point.pos.longitude, results);
                            points += results[0] * point.speed;
                        }
                    } else if (!street.equals(lastStreet) && !lastStreet.equals("")) {
                        if (points != 0)
                            publishProgress(new StreetPoints(lastStreet, points));
                        points = 0;
                        lastStreet = street;
                    }
                }
                lastPos = point.pos;
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(StreetPoints... params) {
            String street = params[0].street;
            int points = params[0].points;

            mLoginManager.addPoints(PositionManager.this, street, points);
            Toast.makeText(context, "Street: " + street + " points: " + points, Toast.LENGTH_SHORT).show();

            TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.WRAP_CONTENT,
                    TableLayout.LayoutParams.WRAP_CONTENT);
            TableRow.LayoutParams rowParams = new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT);

            TableRow nRow = new TableRow(context);
            nRow.setLayoutParams(tableParams);

            TextView streetView = new TextView(context);
            streetView.setLayoutParams(rowParams);

            TextView pointsView = new TextView(context);
            pointsView.setLayoutParams(rowParams);
            pointsView.setGravity(Gravity.CENTER);

            streetView.setText(street);
            pointsView.setText(String.format(Locale.getDefault(), "%d", points));

            nRow.addView(streetView);
            nRow.addView(pointsView);
            UIobjects.table.addView(nRow);
            UIobjects.table.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Void t) {
            Toast.makeText(context, "finished adding points", Toast.LENGTH_SHORT).show();
        }
    }

    public PositionManager(Context ctx, UIObjects o) {
        this.locationManager = (LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);
        mLoginManager = LoginManager.getInstance();
        this.UIobjects = o;
        this.context = ctx.getApplicationContext();
        this.geocoder = new Geocoder(ctx);
        this.routePoints = new ArrayList<>();

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
        //TODO: save points here and not during ride
        started = false;
        locationManager.removeUpdates(this);
        if (locMarker != null)
            locMarker.remove();
        locMarker = null;
        if (locRad != null)
            locRad.remove();
        locRad = null;

        if (UIobjects != null && pOptions != null) {
            List<LatLng> points = pOptions.getPoints();
            if (points.size() > 1) {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (LatLng p : points) {
                    builder.include(p);
                }
                UIobjects.mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 5));
            }
        }

        if (routePoints.size() > 1) {
            //TODO: snap to road
            // for each points: seek distance and calc points -> add points
            lastLocation = null;
            LocationInfo[] routeArray = routePoints
                    .toArray(new LocationInfo[routePoints.size()]);
            new LookupAddressTask().execute(routeArray);
        }
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
        float speed = location.getSpeed();
        LocationInfo locInfo = new LocationInfo(pos, speed);

        pOptions.add(pos);
        routePoints.add(locInfo);

        lastLocation = location;

        // UI code:
        if (UIobjects == null)
            return;

        // convert to km/h
        speed *= Utils.MPS_TO_KMH;

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
        //UIobjects.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 16.5f));
        UIobjects.mMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
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
