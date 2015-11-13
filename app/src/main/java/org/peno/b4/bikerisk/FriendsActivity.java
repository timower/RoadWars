package org.peno.b4.bikerisk;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FriendsActivity extends AppCompatActivity implements ConnectionManager.ResponseListener {
    private TextView connectionLostBanner;
    private ConnectionManager connectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
        connectionManager = ConnectionManager.getInstance(this, this);
        connectionManager.getFriends();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)  {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions_friends, menu);
        return  super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_new_friends:
                Toast.makeText(FriendsActivity.this, "search for friends", Toast.LENGTH_SHORT).show();
                // TODO: intent
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResponse(String req, Boolean result, JSONObject response) {
        connectionLostBanner = (TextView) findViewById(R.id.connectionLost);
        connectionLostBanner.setVisibility(View.GONE);

        if (req.equals("get-friends")){

            if (result) {
                try {
                    JSONArray friends = response.getJSONArray("friends");
                    int length = friends.length();

                    setContentView(R.layout.activity_friends);

                    TableLayout table = (TableLayout) findViewById(R.id.friends_list);
                    TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(
                            TableLayout.LayoutParams.WRAP_CONTENT,
                            TableLayout.LayoutParams.WRAP_CONTENT);
                    TableRow.LayoutParams rowParams = new TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.WRAP_CONTENT);
                    TableRow.LayoutParams filledRowParams = new TableRow.LayoutParams(
                            TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);

                    for (int i = 0; i < length; i++) {
                        JSONArray sub = friends.getJSONArray(i);
                        final String name = sub.getString(0);
                        final int userHSV = sub.getInt(1);

                        TableRow nRow = new TableRow(this);
                        nRow.setLayoutParams(tableParams);

                        TextView username = new TextView(this);
                        username.setLayoutParams(rowParams);
                        username.setText(name);
                        username.setGravity(Gravity.CENTER);
                        username.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent UserInfoActivityIntent = new Intent(getApplicationContext(), UserInfoActivity.class);
                                UserInfoActivityIntent.putExtra("name", name);
                                startActivity(UserInfoActivityIntent);
                            }
                        });

                        TextView remove = new TextView(this);
                        remove.setText(R.string.remove);
                        remove.setTextColor(Color.rgb(0, 50, 250));
                        remove.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // TODO: remove friend
                                Toast.makeText(FriendsActivity.this, "Friend " + name + " removed!", Toast.LENGTH_SHORT).show();
                            }
                        });

                        View playerColor = new View(this);
                        playerColor.setLayoutParams(filledRowParams);
                        playerColor.setBackgroundColor(userHSV);


                        nRow.addView(playerColor);
                        nRow.addView(username);
                        nRow.addView(remove);

                        table.addView(nRow);

                    }
                }catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onConnectionLost(String reason) {
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
        connectionLostBanner.setVisibility(View.VISIBLE);
    }
}
