package org.peno.b4.roadwars;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;


public class RegisterActivity extends AppCompatActivity
        implements ConnectionManager.ResponseListener, View.OnTouchListener {

    private ConnectionManager connectionManager;

    private ImageView imageView;

    private TextView connectionLostBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setOnTouchListener(this);
        connectionLostBanner = (TextView) findViewById(R.id.connectionLost);
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectionManager = ConnectionManager.getInstance(this, this);
        connectionManager.start();
    }

    @Override
    protected void onPause() {
        connectionManager.stop();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions_register, menu);
        return  super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.register_button:
                registerClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void registerClicked(){
        String name = ((EditText)findViewById(R.id.user_name)).getText().toString().trim();
        String pass = ((EditText)findViewById(R.id.password)).getText().toString();
        String pass2 = ((EditText)findViewById(R.id.password2)).getText().toString();
        String email = ((EditText)findViewById(R.id.email)).getText().toString();

        View color = findViewById(R.id.colorView);
        int colorCode = ((ColorDrawable)color.getBackground()).getColor();

        if (name.equals("")) {Toast.makeText(this, getString(R.string.fill_name), Toast.LENGTH_SHORT).show();}
        else if (name.length() > 15) {Toast.makeText(this, R.string.user_long, Toast.LENGTH_SHORT).show();}
        else if (pass.equals("")) {Toast.makeText(this, getString(R.string.fill_pass), Toast.LENGTH_SHORT).show();}
        else if (!pass.equals(pass2)) {Toast.makeText(this, R.string.match_pass, Toast.LENGTH_SHORT).show();}
        else if (email.equals("") || !email.matches("..*@..*\\...*")) {
            Toast.makeText(this, getString(R.string.fill_email), Toast.LENGTH_SHORT).show();
        }
        else if (colorCode == Color.rgb(0,0,0)) {Toast.makeText(this, getString(R.string.choose_color), Toast.LENGTH_SHORT).show();}
        else connectionManager.createUser(name, pass, email, colorCode);
              //create account with username, password, email and color
    }

    @Override
    public boolean onResponse(String req, Boolean result, JSONObject response) {
        connectionLostBanner.setVisibility(View.GONE);
        if (req.equals("create-user")) {
            if (result) {
                Toast.makeText(this, getString(R.string.register_ok), Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, getString(R.string.register_fail), Toast.LENGTH_SHORT).show();
            }
        }
        // absorb all server requests
        return true;
    }

    @Override
    public void onConnectionLost(String reason) {
        connectionLostBanner.setVisibility(View.VISIBLE);
        //Log.d("REG", "connection lost: " + reason);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_DOWN){
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
