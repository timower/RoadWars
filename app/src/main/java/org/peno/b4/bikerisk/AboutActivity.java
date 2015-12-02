package org.peno.b4.bikerisk;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            TextView text = (TextView)findViewById(R.id.AboutTextVersion);
            text.setText(getString(R.string.version_str, version));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            finish();
        }
    }
}


