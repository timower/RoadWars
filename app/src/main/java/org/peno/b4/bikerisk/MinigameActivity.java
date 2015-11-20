package org.peno.b4.bikerisk;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

//TODO: dynamicly load minigames from minigamemanger.minigame.values()

//TODO: rename
public class MinigameActivity extends AppCompatActivity
        implements ConnectionManager.ResponseListener {


    public static final String EXTRA_STREET = "org.peno.b4.bikerisk.STREET";
    public static final String EXTRA_CITY = "org.peno.b4.bikerisk.CITY";

    private ConnectionManager connectionManager;
    private String street;
    public static final String TAG = "MinigameActivity";

    // VERWIJDEREN
    private String allow_nfc;

    private TextView connectionLostBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minigame);

        Intent intent = getIntent();
        street = intent.getStringExtra(EXTRA_STREET);
        TextView StreetName = (TextView) findViewById(R.id.street_name_value);
        StreetName.setText(street);
        //String city = intent.getStringExtra(EXTRA_CITY);
        //getSupportActionBar().setTitle("Minigame");
        connectionLostBanner = (TextView) findViewById(R.id.connectionLost);
    }

    @Override
    protected void onResume() {
        super.onResume();

        connectionManager = ConnectionManager.getInstance(this, this);
    }

    @Override
    public void onResponse(String req, Boolean result, JSONObject response) {
        connectionLostBanner = (TextView) findViewById(R.id.connectionLost);
        connectionLostBanner.setVisibility(View.GONE);
    }

    @Override
    public void onConnectionLost(String reason) {
        connectionLostBanner = (TextView) findViewById(R.id.connectionLost);
        Log.d(TAG, "connection lost: " + reason);
        connectionLostBanner.setVisibility(View.VISIBLE);
    }

    public void LiveRaceClicked(View view) {
        Intent intent = new Intent(this, UserSearchActivity.class);
        intent.putExtra(UserSearchActivity.EXTRA_ALLOW_NFC, allow_nfc);
        //TODO: change to startActivityForResult()
        //read: https://developer.android.com/training/basics/intents/result.html
        startActivityForResult(intent, UserSearchActivity.GET_USER_REQ);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UserSearchActivity.GET_USER_REQ) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "user selected: " + data.getData().getHost(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "canceled", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void FotorondeClicked(View view) {
    }
}


