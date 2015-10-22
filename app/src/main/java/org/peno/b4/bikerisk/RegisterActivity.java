package org.peno.b4.bikerisk;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.EditText;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.json.JSONObject;


public class RegisterActivity extends AppCompatActivity
        implements LoginManager.LoginResultListener, View.OnTouchListener {

    private LoginManager mLoginManager;

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mLoginManager = LoginManager.getInstance();
        imageView = (ImageView)findViewById(R.id.imageView);
        imageView.setOnTouchListener(this);
    }
    public void registerClicked(View view){
        String name = ((EditText)findViewById(R.id.user_name)).getText().toString();
        String pass = ((EditText)findViewById(R.id.password)).getText().toString();
        String email = ((EditText)findViewById(R.id.email)).getText().toString();
        //RadioGroup color = (RadioGroup)findViewById(R.id.radio_group1);
        //RadioButton button = (RadioButton)findViewById(color.getCheckedRadioButtonId());
        View color = findViewById(R.id.colorView);
        int colorCode = ((ColorDrawable)color.getBackground()).getColor();
        if (name.equals("") || pass.equals("") || email.equals(""))
            return;
        mLoginManager.createUser(this, name, pass, email, colorCode);
        //create account with username, password, email and color
    }

    @Override
    public void onLoginResult(String req, Boolean result, JSONObject response) {
        if (req.equals("create-user")) {
            if (result) {
                Toast.makeText(this, "successfully created user", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error creating user", Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_DOWN){
            float width = imageView.getWidth();
            Log.d("EV", "X: " + event.getX());
            Log.d("EV", "width: " + width);
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
}
