package org.peno.b4.roadwars;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

import java.util.Locale;

public class UserInfoActivity extends AppCompatActivity
        implements ConnectionManager.ResponseListener {

    public static final String EXTRA_NAME = "org.peno.name";

    private ConnectionManager connectionManager;
    private String infoName;

    private TextView connectionLostBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
        infoName = getIntent().getStringExtra(EXTRA_NAME);
    }

    @Override
    protected void onResume() {
        super.onResume();

        connectionManager = ConnectionManager.getInstance(this, this);

        //TODO: if more crashes when app was killed -> add everywhere:
        if (connectionManager.user == null) {
            if (connectionManager.loadFromSharedPrefs()) {
                connectionManager.checkLogin();
            } else {
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
            }
        } else {
            getUserInfo();
        }
    }

    private void getUserInfo() {
        if (infoName == null) {
            infoName = connectionManager.user;
        }
        if (!infoName.equals(connectionManager.user)) {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(getString(R.string.user_info));
        } else {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(getString(R.string.my_profile));
        }
        // clear ui:
        setContentView(R.layout.activity_user_info);
        connectionManager.getUserInfo(infoName);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LoginActivity.REQUEST_LOGIN) {
            if (resultCode == RESULT_OK) {
                getUserInfo();
            } else {
                // somehow user got back here without logging in -> restart login activity
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions_user_info, menu);
        MenuItem friends = menu.findItem(R.id.action_my_friends);
        if (connectionManager.user.equals(infoName)) {
            friends.setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_my_friends:
                Intent intent1 = new Intent(this, FriendsActivity.class);
                startActivity(intent1);
                return super.onOptionsItemSelected(item);
            case R.id.change_info:
                Intent intent = new Intent(this, ChangeUserInfoActivity.class);
                startActivity(intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onResponse(String req, Boolean result, JSONObject response) {
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
        connectionLostBanner.setVisibility(View.GONE);
        switch (req) {
            case "user-info":
                if (result) {
                    try {
                        final String resName = response.getString("user");
                        TextView name = (TextView) findViewById(R.id.user_name_value);
                        name.setText(resName);

                        if (resName.equals(connectionManager.user)) {
                            TextView email = (TextView) findViewById(R.id.user_email_value);
                            email.setText(response.getString("email"));
                        } else {
                            View email_label = findViewById(R.id.email_container);
                            email_label.setVisibility(View.GONE);
                        }
                        TextView totalnumberownedstreets = (TextView) findViewById(R.id.totalnumberownedstreets);
                        totalnumberownedstreets.setText(getString(R.string.integer, response.getInt("n-streets")));

                        View color = findViewById(R.id.user_color_value);
                        color.setBackgroundColor(response.getInt("color"));
                        //friend:
                        boolean friend = response.getBoolean("friend");
                        boolean friend_req = response.getBoolean("friend-req");
                        boolean sent_friend_req = response.getBoolean("sent-friend-req");
                        if (friend) {
                            // add info he's friend?
                            findViewById(R.id.add_friend).setVisibility(View.GONE);
                            findViewById(R.id.accept_req).setVisibility(View.GONE);
                            findViewById(R.id.remove_req).setVisibility(View.GONE);
                            findViewById(R.id.remove_friend).setVisibility(View.VISIBLE);
                            View v = findViewById(R.id.remove_friend);
                            v.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    connectionManager.removeFriend(resName);
                                    Button b = (Button) v;
                                    b.setClickable(false);
                                }
                            });
                        }
                        if (friend_req) {
                            findViewById(R.id.add_friend).setVisibility(View.GONE);
                            findViewById(R.id.accept_req).setVisibility(View.VISIBLE);
                            findViewById(R.id.remove_req).setVisibility(View.VISIBLE);
                            findViewById(R.id.remove_friend).setVisibility(View.GONE);
                            View v = findViewById(R.id.accept_req);
                            v.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    connectionManager.acceptFriend(resName);
                                    Button b = (Button) v;
                                    b.setClickable(false);
                                }
                            });
                            View b = findViewById(R.id.remove_req);
                            b.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    connectionManager.declineFriend(resName);
                                    Button d = (Button) v;
                                    d.setClickable(false);
                                }
                            });
                        }
                        if (sent_friend_req) {
                            findViewById(R.id.add_friend).setVisibility(View.GONE);
                            findViewById(R.id.accept_req).setVisibility(View.GONE);
                            findViewById(R.id.remove_req).setVisibility(View.GONE);
                            findViewById(R.id.remove_friend).setVisibility(View.GONE);
                        }
                        if (!friend && !friend_req && !resName.equals(connectionManager.user) && !sent_friend_req) {
                            View v = findViewById(R.id.add_friend);
                            v.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    connectionManager.addFriend(resName);
                                    Button b = (Button) v;
                                    b.setClickable(false);
                                }
                            });
                            findViewById(R.id.add_friend).setVisibility(View.VISIBLE);
                            findViewById(R.id.accept_req).setVisibility(View.GONE);
                            findViewById(R.id.remove_friend).setVisibility(View.GONE);
                            findViewById(R.id.remove_req).setVisibility(View.GONE);

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    connectionManager.getAllPoints(infoName);
                } else {
                    Toast.makeText(this, getString(R.string.error_info), Toast.LENGTH_SHORT).show();
                    finish();
                }
                return true;
            case "get-all-points":
                if (result) {
                    try {
                        JSONArray points = response.getJSONArray("points");
                        int length = points.length();

                        TableLayout table = (TableLayout) findViewById(R.id.streets_table);
                        TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(
                                TableLayout.LayoutParams.WRAP_CONTENT,
                                TableLayout.LayoutParams.WRAP_CONTENT);
                        TableRow.LayoutParams rowParams = new TableRow.LayoutParams(
                                TableRow.LayoutParams.WRAP_CONTENT,
                                TableRow.LayoutParams.WRAP_CONTENT);

                        for (int i = 0; i < length; i++) {
                            JSONArray subA = points.getJSONArray(i);
                            if (subA.length() != 2)
                                continue;
                            final String street = subA.getString(0);
                            int pointsS = subA.getInt(1);

                            TableRow nRow = new TableRow(this);
                            nRow.setLayoutParams(tableParams);

                            TextView streetView = new TextView(this);
                            streetView.setLayoutParams(rowParams);
                            streetView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(UserInfoActivity.this, StreetRankActivity.class);
                                    intent.putExtra(StreetRankActivity.EXTRA_STREET, street);
                                    startActivity(intent);
                                }
                            });

                            TextView pointsView = new TextView(this);
                            pointsView.setLayoutParams(rowParams);
                            pointsView.setGravity(Gravity.CENTER);

                            String showStreet;
                            if (street.length() > 30) {
                               showStreet = street.substring(0, 27) + "...";
                            }
                            else {
                                showStreet = street;
                            }


                            streetView.setText(showStreet);
                            pointsView.setText(String.format(Locale.getDefault(), "%d", pointsS));

                            nRow.addView(streetView);
                            nRow.addView(pointsView);
                            table.addView(nRow);

                            //Log.d("TEST", street);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            case "add-friend":
                if (result) {
                    //clear ui:
                    setContentView(R.layout.activity_user_info);
                    connectionManager.getUserInfo(infoName);
                } else {
                    Toast.makeText(this, getString(R.string.error_add_friend), Toast.LENGTH_SHORT).show();
                }
                return true;
            case "remove-friend":
                if (result) {
                    //clear ui:
                    setContentView(R.layout.activity_user_info);
                    connectionManager.getUserInfo(infoName);
                } else {
                    Toast.makeText(this, getString(R.string.error_remove_friend), Toast.LENGTH_SHORT).show();
                }
                return true;
            case "remove-friend-req":
                if (result) {
                    //clear ui:
                    setContentView(R.layout.activity_user_info);
                    connectionManager.getUserInfo(infoName);
                } else {
                    Toast.makeText(this, getString(R.string.error_remove_friend_req), Toast.LENGTH_SHORT).show();
                }
                return true;
            case "accept-friend":
                if (result) {
                    //clear ui:
                    setContentView(R.layout.activity_user_info);
                    connectionManager.getUserInfo(infoName);
                } else {
                    Toast.makeText(this, getString(R.string.error_accept_friend_req), Toast.LENGTH_SHORT).show();
                }
                return true;
            case "check-login":
                if (result) {
                    getUserInfo();
                } else {
                    Intent loginIntent = new Intent(this, LoginActivity.class);
                    startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
                }
                return true;
        }
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
}
