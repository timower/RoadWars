package org.peno.b4.bikerisk;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements LoginManager.LoginResultListener {

    public LoginManager mLoginManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//         if (!mLoginManager.checkLogin()) {
//              create loginActivity intent
//              start intent for result
//              get key from intent
//              if still not logged in -> stop / error
//          }
        setContentView(R.layout.activity_main);
        mLoginManager = new LoginManager(this);
        mLoginManager.checkLogin(this, "test", "bb8e01a8b75f8ad87f7d401908cc1d570d02616aa1d9308859d82c73436938e8");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == LOGIN_REQUEST) {
//            if (resultCode == RESULT_OK) {
//                // get key from data
//                // done
//            } else {
//
//            }
//        }
    }

    @Override
    public void loginResult(Boolean result) {
        if (result) {
            Toast.makeText(this, "you logged in", Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(this, "login failed", Toast.LENGTH_SHORT).show();
        }
    }
}
