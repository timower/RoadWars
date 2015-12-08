package org.peno.b4.roadwars;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity
        implements ConnectionManager.ResponseListener, SensorEventListener {

    public static final String EXTRA_TARGETLAT  = "org.peno.b4.roadwars.targetlat";
    public static final String EXTRA_TARGETLONG  = "org.peno.b4.roadwars.targetlong";

    public static final int REQUEST_PICTURE = 1;

    private ConnectionManager connectionManager;
    private TextView connectionLostBanner;

    private SensorManager sensorManager;
    private Sensor orientation;

    private LatLng target;

    ImageView capturedImage;

    String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Intent intent = getIntent();
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);

        //cameraButton = (Button) findViewById(R.id.camerabutton);
        capturedImage = (ImageView) findViewById(R.id.imageView);

        target = new LatLng(intent.getDoubleExtra(EXTRA_TARGETLAT, 0),
                intent.getDoubleExtra(EXTRA_TARGETLONG, 0));

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //TODO: (minor) use: https://stackoverflow.com/questions/10291322/what-is-the-alternative-to-android-orientation-sensor
        orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        capturedImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // Ensure that there's a camera activity to handle the intent
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        throw new RuntimeException("error saving file");
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                        startActivityForResult(takePictureIntent, REQUEST_PICTURE);
                    }
                }
            }
        });

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(500);
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectionManager = ConnectionManager.getInstance(this, this);
        sensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            MiniGameManager.getInstance().finish(true);
        } else {
            MiniGameManager.getInstance().finish(false);
        }
        finish();
    }

    @Override
    public final void onAccuracyChanged(Sensor orientation, int accuracy) {
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        LatLng position = PositionManager.getInstance().getLastPosition();
        if (position == null)
            return;

        double latPosition = position.latitude;
        double longPosition = position.longitude;
        double latTarget = target.latitude;
        double longTarget = target.longitude;

        float result[] = new float[2];

        Location.distanceBetween(latTarget, longTarget, latPosition, longPosition, result);
        double target_angle = result[1] + 180;
        double position_angle = event.values[0];
        double angle_difference = Math.abs(target_angle - position_angle);
        angle_difference = (angle_difference + 180) % 360 - 180;

        if (Math.abs(angle_difference) < Utils.PICTURE_RADIUS) {
            capturedImage.setVisibility(View.VISIBLE);
        } else {
            capturedImage.setVisibility(View.GONE);
        }
    }

    public boolean onResponse(String req, Boolean result, JSONObject response) {
        return false;
    }

    @Override
    public void onConnectionLost(String reason) {
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
        //Log.d("CON", "connection lost: " + reason);
        connectionLostBanner.setVisibility(View.VISIBLE);
    }

    public void pingClick(View view) {
        connectionManager.ping();
    }

    @Override
    public void onBackPressed() {
        MiniGameManager.getInstance().finish(false);
        super.onBackPressed();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "RoadWars_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }
}