package org.peno.b4.bikerisk;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements LoginManager.LoginResultListener, OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener, LocationListener {

    private LoginManager mLoginManager;
    private LocationManager locationManager;

    private GoogleMap mMap;
    private Geocoder geocoder;

    private  boolean started = false;

    private Marker locMarker;

    private static final String TAG = "MainActivity";

    private static final int notId = 14;

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

        this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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
        stopLocation();
        Log.d(TAG, "on destroy");
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LoginActivity.REQUEST_LOGIN) {
            if (resultCode == RESULT_OK) {
                hideProgressBar();
            } else {
                // error?
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

                    mMap.clear();

                    JSONArray streets = response.getJSONArray("streets");
                    float[] HSV = new float[3];
                    for (int i = 0; i < streets.length(); i++) {
                        JSONArray street = streets.getJSONArray(i);
                        Color.colorToHSV(street.getInt(3), HSV);
                        addMarker(HSV[0], street.getDouble(1),
                                street.getDouble(2), street.getString(0));
                    }
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
                .image(BitmapDescriptorFactory.defaultMarker(hue))
                .position(new LatLng(lat, lng), 20);

        mMap.addGroundOverlay(groundOverlayOptions);
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
                startActivity(intent);
                return super.onOptionsItemSelected(item);
            case R.id.action_start_stop:
                started = !started;
                if (started) {
                    item.setIcon(R.drawable.ic_pause_dark);
                    showStartedNotification();
                    startLocation();
                } else {
                    item.setIcon(R.drawable.ic_play_dark);
                    hideStartedNotification();
                    stopLocation();
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
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
        mMap.setOnMapLongClickListener(this);
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                //TODO: don't lookup every street again
                Log.d(TAG, "camera changed");
                VisibleRegion reg = mMap.getProjection().getVisibleRegion();
                LatLngBounds bounds = reg.latLngBounds;
                mLoginManager.getAllStreets(MainActivity.this, bounds);
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

    private void startLocation() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10 * 1000, 2, this);
    }

    private void stopLocation() {
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (locMarker != null) {
            locMarker.remove();
            locMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
        } else {

            locMarker = mMap.addMarker(new MarkerOptions()
                                .title("bike")
                                .position(new LatLng(location.getLatitude(),
                                        location.getLongitude())));
        }
        Log.d(TAG, "Acc: " + location.getAccuracy());

    }

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
}
