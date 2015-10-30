package org.peno.b4.bikerisk;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity implements ConnectionManager.ResponseListener {
    public static final int REQUEST_LOGIN = 5;
    private static final String TAG = "LoginActivity";
    private ConnectionManager connectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        connectionManager = ConnectionManager.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectionManager = ConnectionManager.getInstance(this);
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
        connectionManager.login(username, password);
    }

    @Override
    public void onResponse(String req, Boolean result, JSONObject response) {
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
    public void onConnectionLost(String reason) {
        Log.d(TAG, "connection lost: " + reason);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
