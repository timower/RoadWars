package org.peno.b4.bikerisk;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;

public class UserSearchActivity extends AppCompatActivity
        implements ConnectionManager.ResponseListener {

    private ConnectionManager connectionManager;
    private TextView connectionLostBanner;

    public static final String EXTRA_ALLOW_NFC = "roadwars.allownfc";

    public static final String TAG = "UserSearchActivity";

    private boolean allowNFC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_search);
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
        Intent intent = getIntent();
        allowNFC = intent.getBooleanExtra(EXTRA_ALLOW_NFC, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectionManager = ConnectionManager.getInstance(this, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions_user_search, menu);

        MenuItem item = menu.findItem(R.id.action_allow_nfc);
        item.setVisible(allowNFC);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onResponse(String req, Boolean result, JSONObject response) {
        connectionLostBanner.setVisibility(View.GONE);
        // Change request!
        if (req.equals("get-all-users")) {
            if (result) {
                // clear layout:
                setContentView(R.layout.activity_street_rank);

                Log.d(TAG, response.toString());
            }
        }
    }



    @Override
    public void onConnectionLost(String reason) {
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
        Log.d("CON", "connection lost: " + reason);
        connectionLostBanner.setVisibility(View.VISIBLE);
    }
}
