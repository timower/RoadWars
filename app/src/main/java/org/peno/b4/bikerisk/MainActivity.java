package org.peno.b4.bikerisk;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.VisibleRegion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements LoginManager.LoginResultListener, OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener {
    private static final String TAG = "MainActivity";
    private static final int notId = 14;

    private LatLng lastCameraPos;

    private LoginManager mLoginManager;
    private PositionManager positionManager;

    private GoogleMap mMap;
    private Geocoder geocoder;

    private HashMap<String, GroundOverlay> streetMarkers;

    private HashMap<Float, Bitmap> markerCache;

    private Bitmap originalBitmap;

    private TextView streetNameText;
    private TextView speedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLoginManager = new LoginManager(this);
        mLoginManager.start(new LoginManager.LoginConnectListener() {
            @Override
            public void onLoginConnect() {
                if (mLoginManager.loadFromSharedPrefs()) {
                    mLoginManager.checkLogin(MainActivity.this);
                } else {
                    Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        streetMarkers = new HashMap<>();
        markerCache = new HashMap<>();

        originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ball);

        streetNameText = (TextView)findViewById(R.id.street_text);
        speedText = (TextView)findViewById(R.id.speed_text);
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
        if (mMap != null) {
            positionManager.pause(mMap.getCameraPosition());
        } else {
            positionManager.pause(null);
        }
        Log.d(TAG, "on destroy");
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LoginActivity.REQUEST_LOGIN) {
            if (resultCode == RESULT_OK) {
                hideProgressBar();
            } else {
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
            }
        }
    }
    @Override
    public void onLoginResult(String req, Boolean result, JSONObject response) {
        switch (req) {
            case "check-login":
                if (result) {
                    hideProgressBar();
                } else {
                    //login:
                    Intent loginIntent = new Intent(this, LoginActivity.class);
                    startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
                }
                break;
            case "logout":
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
                break;
            case "get-all-streets":
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
                            if (streetMarkers.containsKey(name)) {
                                streetMarkers.get(name)
                                        .setImage(BitmapDescriptorFactory
                                                .fromBitmap(getStreetBitmap(HSV[0])));
                            } else {
                                addMarker(HSV[0], lat, lng, name);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case "get-street":
                if (result) {
                    try {
                        JSONArray info = response.getJSONArray("info");
                        String street = response.getString("street");
                        Toast.makeText(this, "Owner of " + street + ": " + info.getString(1), Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
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
                .image(BitmapDescriptorFactory.fromBitmap(getStreetBitmap(hue)))
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
                    showInfoText();
                    positionManager.start();
                } else {
                    hideStartedNotification();
                    hideInfoText();
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
        geocoder = new Geocoder(this);

        // resume gps:
        if (PositionManager.getInstance() == null) {
            positionManager = new PositionManager(this, new PositionManager.UIObjects(mMap, streetNameText, speedText));
        } else {
            positionManager = PositionManager.getInstance();
            if (positionManager.started) {
                positionManager.resume(new PositionManager.UIObjects(mMap, streetNameText, speedText));
                showStartedNotification();
                invalidateOptionsMenu();
                showInfoText();
            }
        }

        if (positionManager != null && positionManager.lastCameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(positionManager.lastCameraPosition));
        } else {
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

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (geocoder != null) {
            try {
                List<Address> locations = geocoder.getFromLocation(latLng.latitude,
                        latLng.longitude, 1);
                if (locations.size() > 0) {
                    Address loc = locations.get(0);
                    if (loc.getMaxAddressLineIndex() >= 2) {
                        String street = Utils.removeNumbers(loc.getAddressLine(0));
                        String city = Utils.removeNumbers(loc.getAddressLine(1));

                        Intent intent = new Intent(this, StreetRankActivity.class);
                        intent.putExtra(StreetRankActivity.EXTRA_STREET, street);
                        intent.putExtra(StreetRankActivity.EXTRA_CITY, city);
                        startActivity(intent);
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "error, geocoder is null!");
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
                        String street = Utils.removeNumbers(loc.getAddressLine(0));
                        mLoginManager.getStreet(this, street);

                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "error, geocoder is null!");
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

    private void showInfoText() {
        streetNameText.setVisibility(View.VISIBLE);
        speedText.setVisibility(View.VISIBLE);
    }

    private void hideInfoText() {
        streetNameText.setVisibility(View.GONE);
        speedText.setVisibility(View.GONE);
    }

    private Bitmap getStreetBitmap(float hue) {
        if (markerCache.containsKey(hue)) {
            //Log.d(TAG, "cache hit");
            return markerCache.get(hue);
        }
        //Log.d(TAG, "cache miss");
        int w = originalBitmap.getWidth();
        int h = originalBitmap.getHeight();
        int[] pixels = new int[w*h];
        originalBitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        float[] HSV = new float[3];

        int len = w*h;
        for (int i = 0; i < len; i++) {
            Color.colorToHSV(pixels[i], HSV);
            HSV[0] = hue;
            pixels[i] = Color.HSVToColor(Color.alpha(pixels[i]), HSV);
        }
        Bitmap ret = Bitmap.createBitmap(pixels, w, h, originalBitmap.getConfig());
        markerCache.put(hue, ret);
        return ret;
    }

}
