package org.peno.b4.bikerisk;

import android.content.Intent;
import android.graphics.Color;
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

public class FriendsActivity extends AppCompatActivity implements ConnectionManager.ResponseListener {
    private TextView connectionLostBanner;
    private ConnectionManager connectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectionManager = ConnectionManager.getInstance(this, this);
        resetUI();
    }

    private void resetUI(){
        setContentView(R.layout.activity_friends);
        connectionManager.getFriends();
        connectionManager.getFriendRequests();
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
                Intent SearchFriendsIntent = new Intent(this, UserSearchActivity.class);
                //startActivityForResult(SearchFriendsIntent, UserSearchActivity.SEARCH_FRIEND);
                //TODO: fix intent
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        //if (requestCode == UserSearchActivity.SEARCH_FRIEND){
            //connectionManager.addFriend(data.getDataString());
        //}
        // TODO: fix userSearch
    }

    @Override
    public void onResponse(String req, Boolean result, JSONObject response) {
        connectionLostBanner = (TextView) findViewById(R.id.connectionLost);
        connectionLostBanner.setVisibility(View.GONE);

        TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        TableRow.LayoutParams rowParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT);
        TableRow.LayoutParams filledRowParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);

        if (req.equals("get-friends")){
            if (result) {
                try {
                    JSONArray friends = response.getJSONArray("friends");
                    Log.d("FRIENDS", friends.toString());
                    int length = friends.length();

                    TableLayout table = (TableLayout)findViewById(R.id.friends_list);

                    for (int i = 0; i < length; i++) {
                        JSONArray sub = friends.getJSONArray(i);
                        final String name = sub.getString(0);
                        final int userHSV = sub.getInt(1);

                        Log.d("tag", "for-loop started" + name);

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
                        remove.setLayoutParams(rowParams);
                        remove.setText(R.string.remove);
                        remove.setTextColor(Color.rgb(0, 50, 250));
                        remove.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                connectionManager.removeFriend(name);
                                Toast.makeText(FriendsActivity.this, "Friend " + name + " removed!", Toast.LENGTH_SHORT).show();
                                resetUI();
                            }
                        });

                        View playerColor = new View(this);
                        playerColor.setLayoutParams(filledRowParams);
                        playerColor.setBackgroundColor(userHSV);

                        nRow.addView(playerColor);
                        nRow.addView(username);
                        nRow.addView(remove);

                        table.addView(nRow);
                        Log.d("TABLE", "added row to table");
                    }
                }catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        if (req.equals("get-friend-reqs")) {
            if (result) {
                try{
                    JSONArray reqs = response.getJSONArray("friend-reqs");
                    int length = reqs.length();

                    if (length != 0) {
                        Toast.makeText(this, "Got friend requests!", Toast.LENGTH_SHORT).show();
                        findViewById(R.id.request_list).setVisibility(View.VISIBLE);
                    }

                    TableLayout table = (TableLayout) findViewById(R.id.request_list_table);

                    for (int i = 0; i < length; i++) {
                        JSONArray sub = reqs.getJSONArray(i);
                        final String name = sub.getString(0);
                        final int userHSV = sub.getInt(1);

                        TableRow nRow = new TableRow(this);
                        nRow.setLayoutParams(tableParams);

                        View playerColor = new View(this);
                        playerColor.setLayoutParams(filledRowParams);
                        playerColor.setBackgroundColor(userHSV);

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

                        TextView accept = new TextView(this);
                        accept.setText(R.string.accept);
                        accept.setTextColor(Color.rgb(50, 200, 0));
                        accept.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(FriendsActivity.this, "Request from " + name + " accepted!", Toast.LENGTH_SHORT).show();
                                connectionManager.acceptFriend(name);
                                resetUI();
                            }
                        });

                        TextView decline = new TextView(this);
                        decline.setText(R.string.decline);
                        decline.setTextColor(Color.rgb(250, 0, 0));
                        decline.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(FriendsActivity.this, "Request from " + name + " declined!", Toast.LENGTH_SHORT).show();
                                connectionManager.declineFriend(name);
                                resetUI();
                            }
                        });

                        nRow.addView(playerColor);
                        nRow.addView(username);
                        nRow.addView(accept);
                        nRow.addView(decline);

                        table.addView(nRow);

                    }

                }catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if (req.equals("accept-friend") || req.equals("remove-friend") || req.equals("remove-friend-req")){
            if (!result) {Toast.makeText(FriendsActivity.this, "Action Failed", Toast.LENGTH_SHORT).show();}
        }
    }

    @Override
    public void onConnectionLost(String reason) {
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
        connectionLostBanner.setVisibility(View.VISIBLE);
    }
}