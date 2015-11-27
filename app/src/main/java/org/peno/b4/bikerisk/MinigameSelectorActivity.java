package org.peno.b4.bikerisk;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

//TODO: dynamicly load minigames from minigamemanger.minigame.values()

//TODO: rename
public class MinigameSelectorActivity extends AppCompatActivity
        implements ConnectionManager.ResponseListener {


    public static final String EXTRA_STREET = "org.peno.b4.bikerisk.STREET";
    public static final String EXTRA_CITY = "org.peno.b4.bikerisk.CITY";

    private ConnectionManager connectionManager;
    private String street;
    public static final String TAG = "MinigameSelectorActivity";

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
        //getSupport²nBar().setTitle("Minigame");
        connectionLostBanner = (TextView) findViewById(R.id.connectionLost);
    }

    @Override
    protected void onResume() {
        super.onResume();

        connectionManager = ConnectionManager.getInstance(this, this);
    }

    @Override
    public boolean onResponse(String req, Boolean result, JSONObject response) {
        connectionLostBanner = (TextView) findViewById(R.id.connectionLost);
        connectionLostBanner.setVisibility(View.GONE);
        if (req.equals("start-minigame")) {
            if (result) {
                //TODO: minigameManager.startRaceGame();
                Toast.makeText(this, "Minigame started!", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "Other player is not online", Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return false;
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
        intent.putExtra(UserSearchActivity.EXTRA_ALL_FRIENDS, true);
        intent.putExtra(UserSearchActivity.EXTRA_NFC_INTENT, Utils.MINIGAME_NFC_INTENT + ":" + street);

        startActivityForResult(intent, UserSearchActivity.GET_USER_REQ);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UserSearchActivity.GET_USER_REQ) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "user selected: " + data.getData().getHost(), Toast.LENGTH_SHORT).show();
                connectionManager.startMinigame(data.getData().getHost(), street);
            } else {
                Toast.makeText(this, "canceled", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void FotorondeClicked(View view) {
        Toast.makeText(this, "This minigame is not available yet!", Toast.LENGTH_LONG).show();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}


