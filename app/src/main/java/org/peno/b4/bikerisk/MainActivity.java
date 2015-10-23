package org.peno.b4.bikerisk;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements LoginManager.LoginResultListener, OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener,
        PositionManager.PositionListener {
    private static final String TAG = "MainActivity";
    private static final int notId = 14;

    private LatLng lastCameraPos;

    private LoginManager mLoginManager;
    private PositionManager positionManager;

    private GoogleMap mMap;
    private Geocoder geocoder;

    private Marker locMarker;
    private Circle locRad;
    private Polyline userRoute;

    private HashMap<String, GroundOverlay> streetMarkers;

    private Bitmap testBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLoginManager = new LoginManager(this);
        if (mLoginManager.loadFromSharedPrefs()) {
            mLoginManager.checkLogin(this);
        } else {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
        }
        if (PositionManager.getInstance() == null) {
            positionManager = new PositionManager(this);
        } else {
            positionManager = PositionManager.getInstance();
            if (positionManager.started) {
                positionManager.resume(this);
                showStartedNotification();
                invalidateOptionsMenu();
            }
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        streetMarkers = new HashMap<>();

        testBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.white_circle);
        testBitmap = testBitmap.copy(testBitmap.getConfig(), true);
        Log.d(TAG, "mutable: " + testBitmap.isMutable());

        for (int x = 0; x < testBitmap.getWidth(); x++) {
            for (int  y = 0; y < testBitmap.getHeight(); y++) {
                testBitmap.setPixel(x, y, Color.BLUE);
                //TODO: change color, move to function, add hashmap
            }
        }

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "on pause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "on resume");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mLoginManager.pause();
        hideStartedNotification();
        //TODO: fix if turn before mMap exists
        positionManager.pause(mMap.getCameraPosition());
        Log.d(TAG, "on destroy");
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LoginActivity.REQUEST_LOGIN) {
            if (resultCode == RESULT_OK) {
                hideProgressBar();
            } else {
                // TODO: error/retry?
            }
        }
    }
    @Override
    public void onLoginResult(String req, Boolean result, JSONObject response) {
        if (req.equals("check-login")) {
            if (result) {
                hideProgressBar();
            } else {
                //login:
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
            }
        } else if (req.equals("logout")) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
        } else if (req.equals("get-all-streets")) {
            if (result) {
                try {

                    JSONArray streets = response.getJSONArray("streets");
                    float[] HSV = new float[3];
                    for (int i = 0; i < streets.length(); i++) {
                        JSONArray street = streets.getJSONArray(i);
                        Color.colorToHSV(street.getInt(3), HSV);
                        String name = street.getString(0);
                        double lat = street.getDouble(1);
                        double lng = street.getDouble(2);
                        if (streetMarkers.containsKey(name)){
                            streetMarkers.get(name)
                                    .setImage(BitmapDescriptorFactory.fromBitmap(testBitmap));
                        } else {
                            addMarker(HSV[0], lat, lng, name);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else if (req.equals("get-street")) {
            if (result) {
                try {
                    JSONArray info = response.getJSONArray("info");
                    String street = response.getString("street");
                    Toast.makeText(this, "Owner of " + street + ": " + info.getString(1), Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addMarker(float hue, double lat, double lng, String name) {
        /*
        MarkerOptions markerOptions = new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(hue))
                .position(new LatLng(lat, lng))
                .title(name);
        mMap.addMarker(markerOptions);
        */

        GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(testBitmap))
                .position(new LatLng(lat, lng), 40);

        streetMarkers.put(name, mMap.addGroundOverlay(groundOverlayOptions));
    }

    @Override
    public void onLoginError(String error) {

        Intent errorIntent = new Intent(this, ErrorActivity.class);
        errorIntent.putExtra(ErrorActivity.EXTRA_MESSAGE, error);
        startActivity(errorIntent);
        finish();
    }

    private void hideProgressBar() {
        ProgressBar main = (ProgressBar)findViewById(R.id.main_progressbar);
        main.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions_main, menu);
        MenuItem item = menu.findItem(R.id.action_start_stop);
        if (positionManager == null || !positionManager.started) {
            item.setIcon(R.drawable.ic_play_dark);
        } else {
            item.setIcon(R.drawable.ic_pause_dark);

        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_logout:
                this.mLoginManager.logout(this);
                return super.onOptionsItemSelected(item);
            case R.id.action_user_info:
                Intent intent = new Intent(this, UserInfoActivity.class);
                intent.putExtra("name", this.mLoginManager.user);
                startActivity(intent);
                return super.onOptionsItemSelected(item);
            case R.id.action_start_stop:
                if (!positionManager.started) {
                    showStartedNotification();
                    positionManager.start(this);
                } else {
                    hideStartedNotification();
                    positionManager.stop();
                }
                invalidateOptionsMenu();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (positionManager != null && positionManager.lastCameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(positionManager.lastCameraPosition));
        } else {
            geocoder = new Geocoder(this);
            try {
                List<Address> leuven = geocoder.getFromLocationName("Leuven", 1);
                if (leuven.size() > 0) {
                    Address L = leuven.get(0);
                    Log.d(TAG, "got location");
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(L.getLatitude(), L.getLongitude()), 14));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        mMap.setOnMapLongClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                Log.d(TAG, "camera changed");

                VisibleRegion reg = mMap.getProjection().getVisibleRegion();
                LatLngBounds bounds = reg.latLngBounds;
                float dist = 0;
                if (lastCameraPos != null) {
                    Point p1 = mMap.getProjection().toScreenLocation(lastCameraPos);
                    dist = p1.x*p1.x + p1.y*p1.y;
                    Log.d(TAG, "dist: " + dist);
                }

                if (lastCameraPos == null || dist > 700*700){
                    Log.d(TAG, "getting street");
                    mLoginManager.getAllStreets(MainActivity.this, bounds);
                    lastCameraPos = bounds.northeast;
                }

            }
        });
    }

    private String removeNumbers(String orig) {
        String street = "";
        for (String sub : orig.split(" ")) {
            if (!sub.matches("[0-9][0-9]*-?[0-9]*")) {
                street += sub + " ";
            }
        }

        return street.trim();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (geocoder != null) {
            try {
                List<Address> locations = geocoder.getFromLocation(latLng.latitude,
                        latLng.longitude, 1);
                if (locations.size() > 0) {
                    Address loc = locations.get(0);
                    if (loc.getMaxAddressLineIndex() >= 2) {
                        String street = removeNumbers(loc.getAddressLine(0));
                        String city = removeNumbers(loc.getAddressLine(1));

                        Intent intent = new Intent(this, StreetRankActivity.class);
                        intent.putExtra(StreetRankActivity.EXTRA_STREET, street);
                        intent.putExtra(StreetRankActivity.EXTRA_CITY, city);
                        startActivity(intent);
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (geocoder != null) {
            try {
                List<Address> locations = geocoder.getFromLocation(latLng.latitude,
                        latLng.longitude, 1);
                if (locations.size() > 0) {
                    Address loc = locations.get(0);
                    if (loc.getMaxAddressLineIndex() >= 2) {
                        String street = removeNumbers(loc.getAddressLine(0));
                        mLoginManager.getStreet(this, street);

                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void showStartedNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("RoadWars is running")
                .setContentText("click to open")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);


        Intent intent = new Intent(this, this.getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent Pintent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        builder.setContentIntent(Pintent);
        NotificationManager notificationManager =
                (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(notId, builder.build());
    }

    private void hideStartedNotification() {
        NotificationManager notificationManager =
                (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(notId);
    }

    @Override
    public void onLocationChanged(Location location, LatLng pos, PolylineOptions pOps) {
        if (mMap == null)
            return;

        if (userRoute != null)
            userRoute.remove();

        userRoute = mMap.addPolyline(pOps);
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
                                .center(pos)
                                .radius(location.getAccuracy()));
        }
        Log.d(TAG, "Acc: " + location.getAccuracy());

    }
    /*
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "status changed: " + status + ", sats: " + extras.getInt("satellites"));
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "gps enabled");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "gps disabled");
    }
    */
}
