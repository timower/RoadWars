package org.peno.b4.bikerisk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.GeoApiContext;
import com.google.maps.RoadsApi;
import com.google.maps.model.SnappedPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PositionManager implements LocationListener {
    private static PositionManager instance;

    public static class UIObjects {
        public GoogleMap mMap;
        public TextView speedText;
        public TableLayout table;
        public ProgressTracker progressTracker;
        public TextView connectionLostBanner;

        public UIObjects(GoogleMap mMap, TextView spt, TableLayout table, ProgressTracker p, TextView connectionLostBanner) {
            this.mMap = mMap;
            this.speedText = spt;
            this.table = table;
            this.progressTracker = p;
            this.connectionLostBanner = connectionLostBanner;
        }
    }

    private static class StreetPoints {
        public String street;
        public int points;

        public StreetPoints(String street, int points) {
            this.street = street;
            this.points = points;
        }
    }

    private static class RouteInfo {
        private ArrayList<com.google.maps.model.LatLng> routePoints;
        private ArrayList<Float> routeSpeeds;

        public RouteInfo() {
            routePoints = new ArrayList<>();
            routeSpeeds = new ArrayList<>();
        }

        public void add(LatLng pos, float speed) {
            routePoints.add(new com.google.maps.model.LatLng(pos.latitude, pos.longitude));
            routeSpeeds.add(speed);
        }
    }

    public boolean started = false;

    private ConnectionManager connectionManager;

    private LocationManager locationManager;

    private Location lastLocation;

    private RouteInfo routeInfo;

    private PolylineOptions pOptions;

    public CameraPosition lastCameraPosition;

    private UIObjects UIobjects;
    private Geocoder geocoder;

    private Marker locMarker;
    private Bitmap bicycleBitmap;
    private HashMap<Float, Bitmap> bicycleCache = new HashMap<>();
    private Circle locRad;
    private Polyline userRoute;
    private MiniGameManager minigameManager;

    private Context context;
    private GeoApiContext geoApiContext;

    private boolean gotFirstLocation = false;
    private boolean canStart = true;

    private class SnapToRoadTask extends AsyncTask<Void, Void, PolylineOptions> {
        @Override
        protected PolylineOptions doInBackground(Void... params) {
            try {
                PolylineOptions snappedRoute = new PolylineOptions()
                        .color(Color.RED)
                        .width(8);
                int idx = 0;
                while (idx < routeInfo.routePoints.size()) {
                    if (idx > 4)
                        idx -= 5; // overlay between requests

                    int lower = idx;
                    int upper = Math.min(idx + 100, routeInfo.routePoints.size());
                    int length = upper - lower;

                    com.google.maps.model.LatLng[] points =
                            routeInfo.routePoints.subList(lower, upper)
                                    .toArray(new com.google.maps.model.LatLng[length]);

                    SnappedPoint[] result = RoadsApi.snapToRoads(geoApiContext, false, points).await();

                    for (SnappedPoint p : result) {
                        if (idx == 0 || p.originalIndex > 4) { // account for overlap (idx = 5 is index 0)
                            routeInfo.routePoints.set(p.originalIndex + idx, p.location);
                            snappedRoute.add(new LatLng(p.location.lat, p.location.lng));
                        }
                    }

                    idx = upper;
                }
                return snappedRoute;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(PolylineOptions res) {
            if (res != null && UIobjects != null) {
                userRoute.remove();
                userRoute = UIobjects.mMap.addPolyline(res);
            }
            new LookupAddressTask().execute(routeInfo);
        }
    }

    private class LookupAddressTask extends AsyncTask<RouteInfo, StreetPoints, Void> {

        @Override
        protected Void doInBackground(RouteInfo... params) {
            RouteInfo mRouteInfo = params[0];

            String lastStreet = "";
            int points = 0;
            com.google.maps.model.LatLng lastPos = null;

            float[] results = new float[3];

            for (int i = 0; i < mRouteInfo.routePoints.size(); i++) {
                com.google.maps.model.LatLng pos = mRouteInfo.routePoints.get(i);
                float speed = mRouteInfo.routeSpeeds.get(i);

                String street = Utils.lookupStreet(geocoder, new LatLng(pos.lat, pos.lng));
                //TODO: fix ifs
                if (street != null) {
                    if (!street.equals("") && lastStreet.equals("")) {
                        lastStreet = street;
                    } else if (!lastStreet.equals("") && street.equals(lastStreet) && lastPos != null) {
                        if (speed > (10.0f / Utils.MPS_TO_KMH) &&
                                speed < (45.0f / Utils.MPS_TO_KMH)) {
                            Location.distanceBetween(lastPos.lat, lastPos.lng,
                                    pos.lat, pos.lng, results);
                            points += results[0] * speed;
                        }
                    } else if (!street.equals(lastStreet) && !lastStreet.equals("")) {
                        if (points != 0)
                            publishProgress(new StreetPoints(lastStreet, points));
                        points = 0;
                        lastStreet = street;
                    }
                }
                lastPos = pos;
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(StreetPoints... params) {
            String street = params[0].street;
            int points = params[0].points;

            connectionManager.addPoints(street, points);

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
            if (UIobjects != null)
                UIobjects.progressTracker.hideProgressBar(ProgressTracker.REASON_CALCULATING);
            routeInfo.routePoints.clear();
            routeInfo.routeSpeeds.clear();
            canStart = true;
        }
    }

    private PositionManager(Context ctx, UIObjects o) {
        this.locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);

        connectionManager = ConnectionManager.getInstance();
        minigameManager = MiniGameManager.getInstance();

        this.UIobjects = o;
        this.context = ctx.getApplicationContext();
        this.geocoder = new Geocoder(ctx);
        this.routeInfo = new RouteInfo();
        this.geoApiContext = new GeoApiContext().setApiKey(
                context.getString(R.string.google_maps_key));
        this.bicycleBitmap = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.usericon);

        this.pOptions = new PolylineOptions().color(Color.BLUE).width(5);

        instance = this;
    }

    public static PositionManager getInstance(Context ctx, UIObjects objects) {
        if (instance == null) {
            instance = new PositionManager(ctx, objects);
        } else {
            instance.UIobjects = objects;
            instance.connectionManager = ConnectionManager.getInstance();
            instance.minigameManager = MiniGameManager.getInstance();
            instance.locMarker = null;
            instance.locRad = null;
            instance.userRoute = null;
            instance.drawRoute();
            if (instance.lastCameraPosition != null) {
                objects.mMap.moveCamera(CameraUpdateFactory.newCameraPosition(instance.lastCameraPosition));
            }
            //TODO: call minigameManager -> resume()
        }
        return instance;

    }

    public void setUserColor(int color){
        float HSV[] = new float[3];
        Color.colorToHSV(color, HSV);
        bicycleBitmap=Utils.getStreetBitmap(bicycleCache, bicycleBitmap, HSV[0]);
    }

    public boolean start() {
        if (!canStart)
            return false;
        if (userRoute != null)
            userRoute.remove();
        started = true;
        pOptions = new PolylineOptions().color(Color.BLUE).width(5);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2 * 1000, 1, this);
        return true;
    }

    public void stop() {
        started = false;
        locationManager.removeUpdates(this);

        if (UIobjects != null)
            UIobjects.progressTracker.hideProgressBar(ProgressTracker.REASON_GPS_DISABLED);

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

        if (routeInfo.routePoints.size() > 1) {
            lastLocation = null;
            if (UIobjects != null) {
                UIobjects.progressTracker.showProgressBar(ProgressTracker.REASON_CALCULATING);
                UIobjects.progressTracker.hideProgressBar(ProgressTracker.REASON_GPS);
            }
            canStart = false;
            new SnapToRoadTask().execute();
        } else {
            routeInfo.routePoints.clear();
            routeInfo.routeSpeeds.clear();
            if (UIobjects != null)
                UIobjects.progressTracker.hideProgressBar(ProgressTracker.REASON_GPS);
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

        if (locMarker != null)
            locMarker.remove();
        if (locRad != null)
            locRad.remove();
        this.locMarker = null;
        this.locRad = null;

        this.connectionManager = null;
        //this.lastLocation = null;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("LOC", "accuracy: " + location.getAccuracy() + " gotFirstLocation: " + gotFirstLocation);
        if (!gotFirstLocation && location.getAccuracy() < Utils.MIN_ACCURACY) {
            gotFirstLocation = true;
            if (UIobjects != null)
                UIobjects.progressTracker.hideProgressBar(ProgressTracker.REASON_GPS);
        }
        if (!gotFirstLocation){
            return;
        }

        if (minigameManager != null)
            minigameManager.onLocationChanged(location);

        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
        float speed = location.getSpeed();

        pOptions.add(pos);
        routeInfo.add(pos, speed);

        lastLocation = location;

        // UI code:
        if (UIobjects == null) {
            Log.d("LOC", "no UIobjects in locationUpdate");
            return;
        }
        Log.d("LOC", "showing UI");
        // convert to km/h
        speed *= Utils.MPS_TO_KMH;

        UIobjects.speedText.setText(String.format("%.2f km/h", speed));
        UIobjects.speedText.setTextColor((speed > 10.0f && speed < 45.0f)? Color.GREEN : Color.RED);

        drawRoute();

        if (locMarker != null) {
            locMarker.setPosition(pos);
            locRad.setCenter(pos);
            locRad.setRadius(location.getAccuracy());
        } else {

            locMarker = UIobjects.mMap.addMarker(new MarkerOptions()
                    .title("bike")
                    .snippet(connectionManager.user + " is here")
                    .position(pos)
                    .icon(BitmapDescriptorFactory.fromBitmap(bicycleBitmap)));
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

    private void drawRoute() {
        if (userRoute != null)
            userRoute.remove();
        if (pOptions == null)
            throw new RuntimeException("wtf? pOptions is null!!!!!!!!!");

        userRoute = UIobjects.mMap.addPolyline(pOptions);
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        UIobjects.progressTracker.hideProgressBar(ProgressTracker.REASON_GPS_DISABLED);
        UIobjects.connectionLostBanner.setVisibility(View.GONE);
        UIobjects.connectionLostBanner.setText(R.string.connection_lost);
        UIobjects.connectionLostBanner.setTextSize(25);
    }

    @Override
    public void onProviderDisabled(String provider) {
        UIobjects.progressTracker.showProgressBar(ProgressTracker.REASON_GPS_DISABLED);
        UIobjects.connectionLostBanner.setVisibility(View.VISIBLE);
        UIobjects.connectionLostBanner.setText(R.string.gps_service_not_available);
        UIobjects.connectionLostBanner.setTextSize(20);
    }
}
