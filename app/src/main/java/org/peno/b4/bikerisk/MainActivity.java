package org.peno.b4.bikerisk;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements LoginManager.LoginResultListener {

    private LoginManager mLoginManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent myIntent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(myIntent);


        mLoginManager = new LoginManager(this);
        if (mLoginManager.loadFromSharedPrefs()) {
            mLoginManager.checkLogin(this);
        } else {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LoginActivity.REQUEST_LOGIN) {
            if (resultCode == RESULT_OK) {
                hideProgressBar();
            } else {
                // error?
            }
        }
    }
    @Override
    public void onLoginResult(Boolean result) {
        if (result) {
            hideProgressBar();
        } else {
            //login:
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
        }
    }

    @Override
    public void onLoginError(String error) {
        Intent errorIntent = new Intent(this, ErrorActivity.class);
        errorIntent.putExtra(ErrorActivity.EXTRA_MESSAGE, error);
        startActivity(errorIntent);
        finish();
    }

    private void hideProgressBar() {
        ProgressBar main = (ProgressBar)findViewById(R.id.main_progressbar);
        main.setVisibility(View.GONE);
    }
}
