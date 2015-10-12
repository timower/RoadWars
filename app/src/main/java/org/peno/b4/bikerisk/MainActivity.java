package org.peno.b4.bikerisk;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //LoginManager mLoginManager;

        super.onCreate(savedInstanceState);
//
//         if (!mLoginManager.checkLogin()) {
//              create loginActivity intent
//              start intent for result
//              get key from intent
//              if still not logged in -> stop / error
//          }

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
