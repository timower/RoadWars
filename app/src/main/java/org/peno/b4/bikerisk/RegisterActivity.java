package org.peno.b4.bikerisk;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.json.JSONObject;


public class RegisterActivity extends AppCompatActivity implements LoginManager.LoginResultListener {

    private LoginManager mLoginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mLoginManager = LoginManager.getInstance();
    }
    public void registerClicked(View view){
        String name = ((EditText)findViewById(R.id.user_name)).getText().toString();
        String pass = ((EditText)findViewById(R.id.password)).getText().toString();
        String email = ((EditText)findViewById(R.id.email)).getText().toString();
        RadioGroup color = (RadioGroup)findViewById(R.id.radio_group1);
        RadioButton button = (RadioButton)findViewById(color.getCheckedRadioButtonId());
        int colorCode = ((ColorDrawable)button.getBackground()).getColor();
        //TODO: check if ""
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
}
