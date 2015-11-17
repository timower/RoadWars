package org.peno.b4.bikerisk;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

//TODO: set result when users clicks another user.

public class UserSearchActivity extends AppCompatActivity
        implements ConnectionManager.ResponseListener {

    private ConnectionManager connectionManager;
    private TextView connectionLostBanner;

    public static final String EXTRA_ALLOW_NFC = "roadwars.allownfc";
    //public ... String EXTRA_ALL_USERS = ...;

    public static final String TAG = "UserSearchActivity";

    private boolean allowNFC;
    //private boolean allUsers; // if false -> only friends

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_search);
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
        Intent intent = getIntent();
        allowNFC = intent.getBooleanExtra(EXTRA_ALLOW_NFC, false);
        //allUsers = ...
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectionManager = ConnectionManager.getInstance(this, this);
        connectionManager.getAllUsers();
        //if (...) {
        //connectionManager.getAllUsers()
        //or
        //connectionManager.getFriends()
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
        connectionLostBanner = (TextView) findViewById(R.id.connectionLost);
        connectionLostBanner.setVisibility(View.GONE);
        // Change request!
        if (req.equals("get-all-users")) {
            if (!result) {
                // clear layout:
                setContentView(R.layout.activity_street_rank);

                Log.d(TAG, response.toString());
                try {

                    // Error: No value for user?
                    JSONArray user = response.getJSONArray("user");
                    int length = user.length();

                    TableLayout table = (TableLayout) findViewById(R.id.user_table);
                    TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(
                            TableLayout.LayoutParams.WRAP_CONTENT,
                            TableLayout.LayoutParams.WRAP_CONTENT);
                    TableRow.LayoutParams rowParams = new TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.WRAP_CONTENT);

                    for (int i = 0; i < length; i++) {
                        JSONArray subA = user.getJSONArray(i);
                        // if (subA.length() != 2)
                        // continue;
                        final String name = subA.getString(0);

                        int color = subA.getInt(1);

                        TableRow nRow = new TableRow(this);
                        nRow.setLayoutParams(tableParams);

                        TextView userView = new TextView(this);
                        userView.setLayoutParams(rowParams);
                        userView.setGravity(Gravity.CENTER);

                        View colorView = new View(this);
                        colorView.setLayoutParams(rowParams);

                        // colorView.setGravity(Gravity.CENTER);
                        userView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(UserSearchActivity.this, "user clicked", Toast.LENGTH_LONG).show();
                            }
                        });

                        userView.setText(name);
                        colorView.setBackgroundColor(color);

                        nRow.addView(colorView);
                        nRow.addView(userView);

                        table.addView(nRow);

                        Log.d(TAG, name);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "Error getting user data", Toast.LENGTH_SHORT).show();
            }
        }

        // else if (req.equals("get-friends") {
        // ...
        // }

    }



    @Override
    public void onConnectionLost(String reason) {
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
        Log.d("CON", "connection lost: " + reason);
        connectionLostBanner.setVisibility(View.VISIBLE);
    }
}
