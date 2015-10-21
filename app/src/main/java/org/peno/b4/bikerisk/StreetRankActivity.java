package org.peno.b4.bikerisk;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class StreetRankActivity extends AppCompatActivity
        implements LoginManager.LoginResultListener {
    public static final String EXTRA_STREET = "org.peno.b4.bikerisk.STREET";
    public static final String EXTRA_CITY  ="org.peno.b4.bikerisk.CITY";

    public static final String TAG = "StreetRankActivity";

    private LoginManager mLoginManager;
    private String street;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_street_rank);

        mLoginManager = LoginManager.getInstance();

        Intent intent = getIntent();
        street = intent.getStringExtra(EXTRA_STREET);
        String city = intent.getStringExtra(EXTRA_CITY);

        getSupportActionBar().setTitle(street);

        mLoginManager.getStreetRank(this, street);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions_rank_test, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_points:
                mLoginManager.addPoints(this, street , 5);
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    @Override
    public void onLoginResult(String req, Boolean result, JSONObject response) {
        if (req.equals("street-rank")) {
            if (result) {
                Log.d(TAG, response.toString());
                try {
                    JSONArray rank = response.getJSONArray("rank");
                    int length = rank.length();

                    int userPoints = 0;

                    TableLayout table = (TableLayout)findViewById(R.id.rank_table);
                    TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(
                            TableLayout.LayoutParams.WRAP_CONTENT,
                            TableLayout.LayoutParams.WRAP_CONTENT);
                    TableRow.LayoutParams rowParams = new TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.WRAP_CONTENT);

                    for (int i = 0; i < length; i++) {
                        JSONArray subA = rank.getJSONArray(i);
                        if (subA.length() != 2)
                            continue;
                        String name = subA.getString(0);
                        int points = subA.getInt(1);

                        if (name.equals(mLoginManager.user)) {
                            userPoints = points;
                        }

                        TableRow nRow = new TableRow(this);
                        nRow.setLayoutParams(tableParams);

                        TextView rankView = new TextView(this);
                        rankView.setLayoutParams(rowParams);

                        TextView streetView = new TextView(this);
                        streetView.setLayoutParams(rowParams);
                        streetView.setGravity(Gravity.CENTER);

                        TextView pointsView = new TextView(this);
                        pointsView.setLayoutParams(rowParams);
                        //pointsView.setGravity(Gravity.CENTER);

                        rankView.setText(String.format(Locale.getDefault(), "%d", i + 1));
                        streetView.setText(name);
                        pointsView.setText(String.format(Locale.getDefault(), "%d", points));

                        nRow.addView(rankView);
                        nRow.addView(streetView);
                        nRow.addView(pointsView);
                        table.addView(nRow);

                        Log.d(TAG, name);
                    }

                    TextView pText = (TextView)findViewById(R.id.points_text);
                    pText.setText(String.format(Locale.getDefault(), "%d", userPoints));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "Error getting street rank", Toast.LENGTH_SHORT).show();
            }
        } else if (req.equals("add-points")) {
            if (result) {
                mLoginManager.getStreetRank(this, street);
                //TODO: clear ...
            } else {
                Toast.makeText(this, "Error adding points", Toast.LENGTH_SHORT).show();

            }
        }
    }

    @Override
    public void onLoginError(String error) {
        Intent errorIntent = new Intent(this, ErrorActivity.class);
        errorIntent.putExtra(ErrorActivity.EXTRA_MESSAGE, error);
        startActivity(errorIntent);
        finish();
    }
}
