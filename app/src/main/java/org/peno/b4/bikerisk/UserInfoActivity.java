package org.peno.b4.bikerisk;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class UserInfoActivity extends AppCompatActivity implements LoginManager.LoginResultListener {

    private LoginManager mLoginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        mLoginManager = LoginManager.getInstance();
        mLoginManager.getUserInfo(this);
    }

    @Override
    public void onLoginResult(String req, Boolean result, JSONObject response) {
        if (req.equals("user-info")) {
            if (result) {
                try {
                    TextView name = (TextView) findViewById(R.id.user_name_value);
                    name.setText(response.getString("user"));

                    TextView email = (TextView) findViewById(R.id.user_email_value);
                    email.setText(response.getString("email"));

                    View color = findViewById(R.id.user_color_value);
                    color.setBackgroundColor(response.getInt("color"));
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
