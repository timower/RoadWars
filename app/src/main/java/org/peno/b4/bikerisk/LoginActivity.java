package org.peno.b4.bikerisk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity implements LoginManager.LoginResultListener {
    public static final int REQUEST_LOGIN = 5;
    private LoginManager mLoginManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mLoginManager = LoginManager.getInstance();
    }

    public void regClick(View view) {
        Intent registerIntent = new Intent(getApplicationContext(), RegisterActivity.class);
        startActivity(registerIntent);
    }

    public void loginClick(View view) {

        EditText result1 = (EditText) findViewById(R.id.editText);
        EditText result2 = (EditText) findViewById(R.id.editText2);
        String username = result1.getText().toString();
        String password = result2.getText().toString();
        Toast.makeText(this, "login pressed", Toast.LENGTH_SHORT).show();
        mLoginManager.login(this, username, password);
    }

    @Override
    public void onLoginResult(String req, Boolean result) {
        if (req.equals("login")) {
            if (result) {
                setResult(Activity.RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to login", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLoginError(String req, String error) {
        Intent errorIntent = new Intent(this, ErrorActivity.class);
        errorIntent.putExtra(ErrorActivity.EXTRA_MESSAGE, error);
        startActivity(errorIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
