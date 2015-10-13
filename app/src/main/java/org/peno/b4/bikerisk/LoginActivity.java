package org.peno.b4.bikerisk;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    public void regClick(View view) {
        Context context = getApplicationContext();
        CharSequence text = "register clicked";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public void loginClick(View view) {

        EditText result1 = (EditText) findViewById(R.id.editText);
        EditText result2 = (EditText) findViewById(R.id.editText2);
        String res1 = result1.getText().toString();
        String res2 = result2.getText().toString();

        String res = "login: " + res1 + ", pass: " + res2;

        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, res, duration);
        toast.show();


    }

}
