package org.peno.b4.roadwars;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ChangeUserInfoActivity extends AppCompatActivity
        implements ConnectionManager.ResponseListener, View.OnTouchListener {


    private ConnectionManager connectionManager;

    private ImageView imageView;

    private TextView connectionLostBanner;

    public String user;
    public String email;
    public int color;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_user_info);

        imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setOnTouchListener(this);
        connectionLostBanner = (TextView) findViewById(R.id.connectionLost);
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectionManager = ConnectionManager.getInstance(this, this);
        connectionManager.start();

        //TODO: check login or something

        connectionManager.getUserInfo(connectionManager.user);
    }

    @Override
    protected void onPause() {
        connectionManager.stop();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions_change_user_info, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_info_button:
                changeInfoClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void changeInfoClicked() {
        String name = ((EditText) findViewById(R.id.user_name)).getText().toString();
        String pass = ((EditText) findViewById(R.id.password)).getText().toString();
        String email = ((EditText) findViewById(R.id.email)).getText().toString();

        View color = findViewById(R.id.colorView);
        int colorCode = ((ColorDrawable) color.getBackground()).getColor();

        if (name.equals("")) {
            Toast.makeText(this, getString(R.string.fill_name), Toast.LENGTH_SHORT).show();
        } else if (email.equals("") || !email.matches("..*@..*\\...*")) {
            Toast.makeText(this, getString(R.string.fill_email), Toast.LENGTH_SHORT).show();
        } else if (colorCode == Color.rgb(0, 0, 0)) {
            Toast.makeText(this, getString(R.string.choose_color), Toast.LENGTH_SHORT).show();
        } else connectionManager.changeUserInfo(name, pass, email, colorCode);
    }

    @Override
    public boolean onResponse(String req, Boolean result, JSONObject response) {
        connectionLostBanner.setVisibility(View.GONE);
        if (req.equals("change-user-info")) {
            if (result) {
                Toast.makeText(this, getString(R.string.change_ok), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, UserInfoActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, getString(R.string.change_failed), Toast.LENGTH_SHORT).show();
            }
        } else if (req.equals("user-info")) {
            if (result) {
                try {
                    user = connectionManager.user;
                    email = response.getString("email");
                    color = response.getInt("color");

                    EditText editName = (EditText) findViewById(R.id.user_name);
                    editName.setText(user, TextView.BufferType.EDITABLE);

                    EditText editMail = (EditText) findViewById(R.id.email);
                    editMail.setText(email, TextView.BufferType.EDITABLE);

                    View colorView = findViewById(R.id.colorView);
                    colorView.setBackgroundColor(color);
                } catch (JSONException error) {
                    error.printStackTrace();
                }
            }
            // absorb all server requests
            return true;
        }
        return false;
    }

    @Override
    public void onConnectionLost (String reason){
        connectionLostBanner.setVisibility(View.VISIBLE);
        //Log.d("REG", "connection lost: " + reason);
    }

    @Override
    public boolean onTouch (View v, MotionEvent event){
        int action = event.getAction();
        if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_DOWN) {
            float width = imageView.getWidth();
            //Log.d("EV", "X: " + event.getX());
            //Log.d("EV", "width: " + width);
            float hue = (event.getX() / width) * 360f;
            //Toast.makeText(this, String.valueOf(hue), Toast.LENGTH_SHORT).show();
            View colorView = findViewById(R.id.colorView);
            float[] HSV = new float[3];
            HSV[0] = hue;
            HSV[1] = 1;
            HSV[2] = 1;
            colorView.setBackgroundColor(Color.HSVToColor(HSV));
        }
        return true;
    }

    public void pingClick(View view) {
        connectionManager.ping();
    }
}
