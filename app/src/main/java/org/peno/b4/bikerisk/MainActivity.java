package org.peno.b4.bikerisk;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TableLayout;
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

import java.util.HashMap;

public class MainActivity extends AppCompatActivity
        implements ConnectionManager.ResponseListener, OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener {

    private static final String TAG = "MainActivity";
    private static final int notId = 14;

    private LatLng lastCameraPos;

    private ConnectionManager connectionManager;
    private PositionManager positionManager;

    private GoogleMap mMap;
    private Geocoder geocoder;

    private HashMap<String, GroundOverlay> streetMarkers;

    private HashMap<Float, Bitmap> markerCache;

    private Bitmap originalBitmap;

    private TableLayout pointsTable;
    private TextView speedText;
    private ProgressBar progressBar;

    private TextView connectionLostBanner;

    /** Creates the activity, initializes the map and searches for certain text views. (UNFINISHED)
     *
     * @param savedInstanceState:
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // create activity:
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // get map object (calls onMapReady)
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        hideStartedNotification();

        // init markers and bitmap cache
        streetMarkers = new HashMap<>();
        markerCache = new HashMap<>();

        originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ball);

        // find textviews for street and speed
        pointsTable = (TableLayout)findViewById(R.id.streets_table);
        speedText = (TextView)findViewById(R.id.speed_text);
        progressBar = (ProgressBar)findViewById(R.id.main_progressbar);
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
    }

    /** Resumes the activity. Restarts server connection and checks login.
     * If the user is not logged in, it will start the login activity.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // start connection with server
        connectionManager = ConnectionManager.getInstance(this, this);

        // login:
        if (connectionManager.loadFromSharedPrefs()) {
            // check login with saved key & user
            connectionManager.checkLogin();
        } else {
            // start login activity (calls on activity result)
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
        }
    }

    /** Pauses the activity.
     */
    @Override
    protected void onPause() {
        Log.d(TAG, "on pause");
        // stop connection with server
        connectionManager.stop();
        if (positionManager != null) {
            if (mMap != null) {
                positionManager.pause(mMap.getCameraPosition());
            } else {
                positionManager.pause(null);
            }
        }
        super.onPause();
    }

    /** Destroys the activity. Stops server connection and position manager.
     * If the screen is merely rotated, it saves the current camera position
     * and pauses the position manager.
     */
    @Override
    protected void onDestroy() {

        // hide notification (gets shown again later if running)
        hideStartedNotification();

        //check if we are just rotation screens:
        if (isChangingConfigurations()) {
            // we will be restarted later again -> save camera position in positionmanager
            if (mMap != null) {
                positionManager.pause(mMap.getCameraPosition());
            } else {
                positionManager.pause(null);
            }
        } else {
            // we will be destroyed -> kill positionmanager
            positionManager.destroy();
        }
        Log.d(TAG, "on destroy");
        super.onDestroy();
    }

    /** Allows the activity to communicate with other activities. (UNFINISHED)
     *
     * @param requestCode: request send by MainActivity
     * @param resultCode: result received from other activity
     * @param data:
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LoginActivity.REQUEST_LOGIN) {
            if (resultCode == RESULT_OK) {
                // we are logged in (via login)
                hideProgressBar();
            } else {
                // somehow user got back here without logging in -> restart login activity
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
            }
        }
        if (requestCode == UserSearchActivity.GET_USER_REQ) {
            if (resultCode == RESULT_OK) {
                String name = data.getData().getHost();
                Intent UserInfoActivityIntent = new Intent(this, UserInfoActivity.class);
                UserInfoActivityIntent.putExtra("name", name);
                startActivity(UserInfoActivityIntent);
            }
        }
    }

    /** Allows the activity to handle different requests from outside
     * (the user, other activities or managers,...).
     *
     * @param req the original request string
     * @param result if the result succeeded
     * @param response the complete response object
     */
    @Override
    public boolean onResponse(String req, Boolean result, JSONObject response) {
        connectionLostBanner.setVisibility(View.GONE);
        switch (req) {
            case "check-login":
                if (result) {
                    // we are logged in
                    hideProgressBar();
                } else {
                    //login (key not valid anymore):
                    Intent loginIntent = new Intent(this, LoginActivity.class);
                    startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
                }
                return true;
            case "logout":
                // we just logged out -> show login activity
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
                return true;
            case "get-all-streets":
                if (result) {
                    // show received streets & markers
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
                                // maybe owner changed -> update icon
                                streetMarkers.get(name)
                                        .setImage(BitmapDescriptorFactory
                                                .fromBitmap(Utils.getStreetBitmap(markerCache, originalBitmap, HSV[0])));
                            } else {
                                // add marker
                                addMarker(HSV[0], lat, lng, name);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            case "get-street":
                if (result) {
                    // show toast with name of owner
                    try {
                        JSONArray info = response.getJSONArray("info");
                        String street = response.getString("street");
                        Toast.makeText(this, "Owner of " + street + ": " + info.getString(1), Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            case "add-points":
                if (result) {
                    Toast.makeText(this, "saved points", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "error saving points", Toast.LENGTH_SHORT).show();
                }
                return true;
            case "user-info":
                if (result) {
                    try {
                        positionManager.setUserColor(response.getInt("color"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            case "get-first":
                MiniGameManager.getInstance().setFirst(result);
                return true;

        }
        return false;
    }

    /** (UNFINISHED)
     *
     * @param reason reason why the connection was dropped
     */
    @Override
    public void onConnectionLost(String reason) {
        Log.d(TAG, "connection lost: " + reason);
        connectionLostBanner.setVisibility(View.VISIBLE);
    }


    /** Creates the options menu (at the top of the screen). (UNFINISHED)
     *
     * @param menu: the menu object which contains all options.
     * @return ?
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions_main, menu);

        // change icon (if started or not)
        MenuItem item = menu.findItem(R.id.action_start_stop);
        if (positionManager == null || !positionManager.started) {
            item.setIcon(R.drawable.ic_play_dark);
        } else {
            item.setIcon(R.drawable.ic_stop_white_24dp);

        }
        return super.onCreateOptionsMenu(menu);
    }

    /** Executes the selected option of the menu. (UNFINISHED)
     *
     * @param item: the selected option
     * @return ?
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_logout:
                if (positionManager.started) {
                    hideStartedNotification();
                    hideInfoText();
                    hideProgressBar();
                    positionManager.stop();

                    invalidateOptionsMenu();
                }
                this.connectionManager.logout();
                return super.onOptionsItemSelected(item);
            case R.id.action_user_info:
                if(progressBar.getVisibility() == View.VISIBLE){
                    return super.onOptionsItemSelected(item);
                }
                Intent intent = new Intent(this, UserInfoActivity.class);
                intent.putExtra("name", this.connectionManager.user);
                startActivity(intent);
                return super.onOptionsItemSelected(item);
            case R.id.action_start_stop:
                    if (!positionManager.started) {
                        //we have to control each time that the button is clicked if gps is activated
                        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                            showGPSDisabledAlertToUser();
                        }else {
                            Toast.makeText(this, "GPS is Enabled", Toast.LENGTH_SHORT).show();
                            if (progressBar.getVisibility() == View.VISIBLE) {
                                return super.onOptionsItemSelected(item);
                            }
                            showStartedNotification();
                            showInfoText();
                            positionManager.start();
                            showProgressBar();
                            connectionManager.getUserInfo(connectionManager.user);
                        }
                    } else {
                        hideStartedNotification();
                        hideInfoText();
                        positionManager.stop();
                    }
                    // redraw options menu:
                    invalidateOptionsMenu();
                    return super.onOptionsItemSelected(item);
            case R.id.action_userlist:
                Intent userList = new Intent(this, UserSearchActivity.class);
                userList.putExtra(UserSearchActivity.EXTRA_ALL_USERS, true);
                startActivityForResult(userList, UserSearchActivity.GET_USER_REQ);
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    /** Configures the map and camera. Starts/resumes the position manager, sets camera view and
     * listeners.
     *
     * @param googleMap: the map object
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        geocoder = new Geocoder(this);

        positionManager = PositionManager.getInstance(this,
                new PositionManager.UIObjects(mMap, speedText, pointsTable, progressBar, connectionLostBanner));
        if (positionManager.started) {
            // show notification
            showStartedNotification();
            invalidateOptionsMenu();
            showInfoText();
        }

        // set camera position:
        if (positionManager.lastCameraPosition != null) {
            // last position:
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(positionManager.lastCameraPosition));
        } else {
            // camera centered at Sint-Pieterskerk, Leuven
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(50.879549, 4.701242), 14));
        }

        // set up listeners:
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            /** (UNFINISHED)
             *
             * @param cameraPosition:
             */
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (progressBar.getVisibility() == View.VISIBLE) {
                    return;
                }
                Log.d(TAG, "camera changed");
                VisibleRegion reg = mMap.getProjection().getVisibleRegion();
                LatLngBounds bounds = reg.latLngBounds;
                float dist = 0;
                if (lastCameraPos != null) {
                    Point p1 = mMap.getProjection().toScreenLocation(lastCameraPos);
                    dist = p1.x * p1.x + p1.y * p1.y;
                    Log.d(TAG, "dist: " + dist);
                }

                if (lastCameraPos == null || dist > 700 * 700) {
                    Log.d(TAG, "getting street");
                    connectionManager.getAllStreets(bounds);
                    lastCameraPos = bounds.northeast;
                }

            }
        });
    }

    /** Starts the Street Rank Activity for the selected street.
     *
     * @param latLng: coordinates of the point which was clicked
     */
    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.d(TAG, "long-click on map");
        // lookup street and start streetRankActivity:
        if(progressBar.getVisibility() == View.VISIBLE){
            return;
        }
        String street = Utils.lookupStreet(geocoder, latLng);
        Log.d(TAG, "get street rank for " + street);
        if (street != null) {
            Intent intent = new Intent(this, StreetRankActivity.class);
            intent.putExtra(StreetRankActivity.EXTRA_STREET, street);
            //intent.putExtra(StreetRankActivity.EXTRA_CITY, city);
            startActivity(intent);
        }
    }

    /** Shows the street name and owner of the selected street.
     *
     * @param latLng: coordinates of the point which was clicked
     */
    @Override
    public void onMapClick(LatLng latLng) {
        if(progressBar.getVisibility() == View.VISIBLE){
            return;
        }
        pointsTable.setVisibility(View.GONE);
        // lookup street & show toast (in onLoginResult)
        String street = Utils.lookupStreet(geocoder, latLng);
        if (street != null)
            connectionManager.getStreet(street);
    }

    /** Displays a notification on the smartphone notification screen.
     */
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

    /** Hides the notification on the smartphone notification screen.
     *
     */
    private void hideStartedNotification() {
        NotificationManager notificationManager =
                (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(notId);
    }

    /** Shows the info text.(UNFINISHED)
     */
    private void showInfoText() {
        speedText.setVisibility(View.VISIBLE);
    }

    /** Hides the info text. (UNFINISHED)
     */
    private void hideInfoText() {
        speedText.setVisibility(View.GONE);
    }

    /** Hides the progressbar. (UNFINISHED)
     */
    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    /** Shows the progressbar. (UNFINISHED)
     */
    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    /** Adds a marker on a street in the color of the owner. (UNFINISHED)
     *
     * @param hue: color of the owner
     * @param lat: latitude coordinate of the street
     * @param lng: longitude coordinate of the street
     * @param name: name of street/ name of owner?
     */
    private void addMarker(float hue, double lat, double lng, String name) {
        /*
        MarkerOptions markerOptions = new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(hue))
                .position(new LatLng(lat, lng))
                .title(name);
        mMap.addMarker(markerOptions);
        */
        // add ground overlay
        GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(Utils.getStreetBitmap(markerCache, originalBitmap, hue)))
                .position(new LatLng(lat, lng), 40);

        streetMarkers.put(name, mMap.addGroundOverlay(groundOverlayOptions));
    }

    /**
     * Guides the user to activation of the GPS
     */

    private void showGPSDisabledAlertToUser(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes, continue to settings", new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int id){
                                startActivity(new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                        });

        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        dialog.cancel();
                    }
                });

        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }
}
