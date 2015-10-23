package org.peno.b4.bikerisk;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.Locale;

public class UserInfoActivity extends AppCompatActivity implements LoginManager.LoginResultListener {

    private LoginManager mLoginManager;
    private String infoName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        infoName = getIntent().getStringExtra("name");
        setContentView(R.layout.activity_user_info);
        mLoginManager = LoginManager.getInstance();
        mLoginManager.getUserInfo(this, infoName);
    }

    @Override
    public void onLoginResult(String req, Boolean result, JSONObject response) {
        if (req.equals("user-info")) {
            if (result) {
                try {
                    String resName = response.getString("user");
                    TextView name = (TextView) findViewById(R.id.user_name_value);
                    name.setText(resName);

                    if (resName.equals(mLoginManager.user)) {
                        TextView email = (TextView) findViewById(R.id.user_email_value);
                        email.setText(response.getString("email"));
                    } else  {
                        TextView email_label = (TextView)findViewById(R.id.email_id);
                        email_label.setVisibility(View.GONE);
                    }

                    View color = findViewById(R.id.user_color_value);
                    color.setBackgroundColor(response.getInt("color"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mLoginManager.getAllPoints(this, infoName);
            } else {
                //TODO: error?
            }
        } else if (req.equals("get-all-points")) {
            if (result) {
                try {
                    JSONArray points = response.getJSONArray("points");
                    int length = points.length();

                    TableLayout table = (TableLayout)findViewById(R.id.streets_table);
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
                        String street = subA.getString(0);
                        int pointsS = subA.getInt(1);

                        TableRow nRow = new TableRow(this);
                        nRow.setLayoutParams(tableParams);

                        TextView streetView = new TextView(this);
                        streetView.setLayoutParams(rowParams);

                        TextView pointsView = new TextView(this);
                        pointsView.setLayoutParams(rowParams);
                        pointsView.setGravity(Gravity.CENTER);

                        streetView.setText(street);
                        pointsView.setText(String.format(Locale.getDefault(), "%d", pointsS));

                        nRow.addView(streetView);
                        nRow.addView(pointsView);
                        table.addView(nRow);

                        Log.d("TEST", street);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

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
