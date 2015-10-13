package org.peno.b4.bikerisk;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

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
        mLoginManager = new LoginManager(this);
        if (!mLoginManager.checkLogin("test", "lol")) {
            Toast.makeText(this, "not logged in", Toast.LENGTH_SHORT).show();
        }
        setContentView(R.layout.activity_main);
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


}
