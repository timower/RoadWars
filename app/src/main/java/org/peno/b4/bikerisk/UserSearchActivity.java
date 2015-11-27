package org.peno.b4.bikerisk;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

//TODO: set result when users clicks another user.

public class UserSearchActivity extends AppCompatActivity
        implements ConnectionManager.ResponseListener, NfcAdapter.CreateNdefMessageCallback {

    private ConnectionManager connectionManager;
    private TextView connectionLostBanner;

    public static final int GET_USER_REQ = 1;
    public static final String EXTRA_ALLOW_NFC = "roadwars.allownfc";
    public static final String EXTRA_ALL_USERS = "roadwars.all_users";
    public static final String EXTRA_ALL_FRIENDS = "roadwars.all_friends";
    public static final String EXTRA_UNKNOWN_USERS = "roadwars.unknown_users";

    public static final String EXTRA_NFC_INTENT = "roadwars.nfc_intent";

    public static final String TAG = "UserSearchActivity";

    private boolean allowNFC;
    private boolean allUsers; // if false -> only friends
    private boolean allFriends;
    private boolean unknownUsers;
    private String nfcIntent;

    private ArrayList<Pair<String, Integer>> users;
    private ArrayList<Pair<String, Integer>> filteredUsers;
    private EditText searchBar;

    private NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_search);
        resetLayout();

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
        Intent intent = getIntent();
        allowNFC = intent.getBooleanExtra(EXTRA_ALLOW_NFC, false);
        allUsers = intent.getBooleanExtra(EXTRA_ALL_USERS, false);
        allFriends = intent.getBooleanExtra(EXTRA_ALL_FRIENDS, false);
        unknownUsers = intent.getBooleanExtra(EXTRA_UNKNOWN_USERS, false);
        nfcIntent = intent.getStringExtra(EXTRA_NFC_INTENT);

        users = new ArrayList<>();
        filteredUsers = new ArrayList<>();

        searchBar = (EditText)findViewById(R.id.search_text_bar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // filter lijst met searchBar.getText()
                String text = searchBar.getText().toString();
                // voer displayArray uit
                filteredUsers.clear();
                int length = users.size();
                for (int i = 0; i < length; i++) {
                    final String name = users.get(i).first;
                    if (name.startsWith(text)) {
                        filteredUsers.add(users.get(i));
                    }
                }
                displayArray(filteredUsers);
                Log.d(TAG, "text changed");
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        //allUsers = ...

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectionManager = ConnectionManager.getInstance(this, this);
        if (allUsers) {
            connectionManager.getAllUsers();
        } else if (allFriends) {
            connectionManager.getFriends();
        } else if (unknownUsers) {
            connectionManager.getUnknownUsers();
        } else {
            throw new RuntimeException("Fix uw intent!!!!!! voor userSearch (1 optie moet op true staan");
        }
        //if (...) {
        //connectionManager.getAllUsers()
        //or
        //connectionManager.getFriends()
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_allow_nfc:
                if (nfcAdapter == null) {
                    Toast.makeText(this, "Sorry this device does not have NFC.", Toast.LENGTH_LONG).show();
                    return true;
                }

                if (!nfcAdapter.isEnabled()) {
                    Toast.makeText(this, "Please enable NFC via Settings.", Toast.LENGTH_LONG).show();
                    return true;
                }
                Toast.makeText(this, "zet uw apparaten tegen elkaar!!!", Toast.LENGTH_LONG).show();
                nfcAdapter.setNdefPushMessageCallback(this, this);
                return true;
        }

        return super.onOptionsItemSelected(item);
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
        if (result) {
            users.clear();
            Log.d(TAG, response.toString());
            try {
                if (req.equals("get-all-users") || req.equals("get-unknown-users")) {
                        JSONArray user = response.getJSONArray("users");
                        makeArray(user);
                } else if (req.equals("get-friends")) {
                        JSONArray user = response.getJSONArray("friends");
                        makeArray(user);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Error getting user data", Toast.LENGTH_SHORT).show();
        }
    }

    public void makeArray(JSONArray user) {
        try {
            int length = user.length();
            for (int i = 0; i < length; i++) {
                JSONArray subA = user.getJSONArray(i);
                if (subA.length() != 2)
                    continue;
                String username = subA.getString(0);
                if (!username.equals(connectionManager.user))
                    users.add(new Pair<>(username, subA.getInt(1)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        displayArray(users);
    }

    public void displayArray(List<Pair<String, Integer>> list) {
        resetLayout();

        TableLayout table = (TableLayout) findViewById(R.id.user_table);
        TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        TableRow.LayoutParams rowParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT);
        TableRow.LayoutParams colorRowParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT);

        for (Pair<String, Integer> pair : list) {
            final String name = pair.first;

            int color = pair.second;

            TableRow nRow = new TableRow(this);
            nRow.setLayoutParams(tableParams);

            TextView userView = new TextView(this);
            userView.setLayoutParams(rowParams);
            userView.setGravity(Gravity.CENTER);

            View colorView = new View(this);
            colorView.setLayoutParams(colorRowParams);

            // colorView.setGravity(Gravity.CENTER);
            userView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent result = new Intent("org.peno.b4.bikerisk.RESULT_ACTION", Uri.parse("username://" + name));
                    setResult(RESULT_OK, result);
                    finish();
                }
            });

            userView.setText(name);
            colorView.setBackgroundColor(color);

            nRow.addView(colorView);
            nRow.addView(userView);

            table.addView(nRow);
        }
    }

    private void resetLayout() {
        TableLayout table = (TableLayout) findViewById(R.id.user_table);
        table.removeAllViews();
        TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        TableRow.LayoutParams rowParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT);
        TableRow nRow = new TableRow(this);
        nRow.setLayoutParams(tableParams);

        // color:
        TextView colorView = new TextView(this);
        colorView.setLayoutParams(rowParams);
        colorView.setText(R.string.color);
        colorView.setTextSize(18);
        colorView.setTypeface(null, Typeface.BOLD);
        //username
        TextView userView = new TextView(this);
        userView.setLayoutParams(rowParams);
        userView.setText(R.string.Username);
        userView.setTextSize(18);
        userView.setTypeface(null, Typeface.BOLD);
        userView.setGravity(Gravity.CENTER);

        nRow.addView(colorView);
        nRow.addView(userView);

        table.addView(nRow);
    }

    @Override
    public void onConnectionLost(String reason) {
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
        Log.d("CON", "connection lost: " + reason);
        connectionLostBanner.setVisibility(View.VISIBLE);
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String message = nfcIntent + ":" + connectionManager.user;
        NdefRecord ndefRecord = NdefRecord.createMime("text/plain", message.getBytes());
        NdefMessage ndefMessage = new NdefMessage(ndefRecord);
        return ndefMessage;
    }
}
