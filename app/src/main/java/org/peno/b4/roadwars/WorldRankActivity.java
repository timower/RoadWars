package org.peno.b4.roadwars;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class WorldRankActivity extends AppCompatActivity
        implements ConnectionManager.ResponseListener {


    private ConnectionManager connectionManager;
    private TextView connectionLostBanner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_world_rank);
    }

    @Override
    protected void onResume() {
        super.onResume();

        connectionManager = ConnectionManager.getInstance(this, this);
        connectionManager.getWorldRank();
    }

    @Override
    public boolean onResponse(String req, Boolean result, JSONObject response) {
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
        connectionLostBanner.setVisibility(View.GONE);

        if (req.equals("get-world-ranking")) {
            if (result) {
                // clear layout:
                setContentView(R.layout.activity_world_rank);

                //Log.d(TAG, response.toString());
                try {
                    JSONArray rank = response.getJSONArray("rank");
                    int length = rank.length();

                    TableLayout table = (TableLayout)findViewById(R.id.rank_table);
                    TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(
                            TableLayout.LayoutParams.WRAP_CONTENT,
                            TableLayout.LayoutParams.WRAP_CONTENT);
                    TableRow.LayoutParams rowParams = new TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.WRAP_CONTENT);
                    TableRow.LayoutParams filledRowParams = new TableRow.LayoutParams(
                            TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);

                    for (int i = 0; i < length; i++) {
                        JSONArray subA = rank.getJSONArray(i);
                        if (subA.length() != 3)
                            continue;
                        final String name = subA.getString(1);
                        int streets = subA.getInt(0);
                        int color = subA.getInt(2);

                        /*
                        if (name.equals(connectionManager.user)) {
                            userPoints = points;
                        }
                        */

                        TableRow nRow = new TableRow(this);
                        nRow.setLayoutParams(tableParams);

                        View playerColor = new View(this);
                        playerColor.setLayoutParams(filledRowParams);
                        playerColor.setBackgroundColor(color);

                        TextView userView = new TextView(this);
                        userView.setLayoutParams(rowParams);
                        userView.setGravity(Gravity.CENTER);
                        userView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent UserInfoActivityIntent = new Intent(getApplicationContext(), UserInfoActivity.class);
                                UserInfoActivityIntent.putExtra(UserInfoActivity.EXTRA_NAME, name);
                                startActivity(UserInfoActivityIntent);
                            }
                        });

                        TextView streetsView = new TextView(this);
                        streetsView.setLayoutParams(rowParams);
                        //pointsView.setGravity(Gravity.CENTER);

                        userView.setText(name);
                        streetsView.setText(String.format(Locale.getDefault(), "%d", streets));

                        nRow.addView(playerColor);
                        nRow.addView(userView);
                        nRow.addView(streetsView);
                        table.addView(nRow);

                        //Log.d(TAG, name);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, getString(R.string.error_info), Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onConnectionLost(String reason) {
        connectionLostBanner = (TextView)findViewById(R.id.connectionLost);
        //Log.d(TAG, "connection lost: " + reason);
        connectionLostBanner.setVisibility(View.VISIBLE);
    }

    public void pingClick(View view) {
        connectionManager.ping();
    }
}
