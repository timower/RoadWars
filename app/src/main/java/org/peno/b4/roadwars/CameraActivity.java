package org.peno.b4.roadwars;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends AppCompatActivity
        implements ConnectionManager.ResponseListener, SensorEventListener {

    public static final String EXTRA_TARGETLAT  = "org.peno.b4.roadwars.targetlat";
    public static final String EXTRA_TARGETLONG  = "org.peno.b4.roadwars.targetlong";

    private ConnectionManager connectionManager;
    private TextView connectionLostBanner;

    private SensorManager sensorManager;
    private Sensor orientation;

    private LatLng target;

    Button cameraButton;
    ImageView CapturedImage;

    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Intent intent = getIntent();
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);

        cameraButton = (Button) findViewById(R.id.camerabutton);
        CapturedImage = (ImageView) findViewById(R.id.imageView);

        target = new LatLng(intent.getDoubleExtra(EXTRA_TARGETLAT, 0),
                intent.getDoubleExtra(EXTRA_TARGETLONG, 0));

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        orientation = sensorManager.getDefaultSensor(orientation.TYPE_ORIENTATION);

        tv = (TextView) findViewById(R.id.textView3);


            cameraButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, 0);
                }
            });
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

        Bitmap bitmap = (Bitmap) data.getExtras().get("data");
        CapturedImage.setImageBitmap(bitmap);
        FileOutputStream out = null;
        //TODO: just send tumbnail -> less storage and bandwidth
        // compress bitmap to byteArray -> encode in base64 -> send to server
        try {
            out = new FileOutputStream("RoadWarsPicture");
            if (bitmap != null  )
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

        Location.distanceBetween(latPosition, longPosition, latTarget, longTarget, result);
        double target_angle = result[1] + 180;
        double position_angle = event.values[0];
        double angle_difference = Math.abs(target_angle - position_angle);
        angle_difference = (angle_difference + 180) % 360 - 180;
        tv.setText(target_angle + "\n" + position_angle +"\n" + angle_difference );

        if (Math.abs(angle_difference) < 30) {
            cameraButton.setVisibility(View.VISIBLE);
        } else {
            cameraButton.setVisibility(View.GONE);
        }
    }

//    private double angleFromCoordinate(double lat1, double long1, double lat2,
//                                       double long2) {
//
//        double dLon = (long2 - long1);
//
//        double y = Math.sin(dLon) * Math.cos(lat2);
//        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
//                * Math.cos(lat2) * Math.cos(dLon);
//
//        double brng = Math.atan2(y, x);
//
//        brng = Math.toDegrees(brng);
//        brng = (brng + 360) % 360;
//        brng = 360 - brng;
//
//        return brng;
//    }

    public boolean onResponse(String req, Boolean result, JSONObject response) {
        return false;
    }

    @Override
    public void onConnectionLost(String reason) {
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
        Log.d("CON", "connection lost: " + reason);
        connectionLostBanner.setVisibility(View.VISIBLE);
    }
}