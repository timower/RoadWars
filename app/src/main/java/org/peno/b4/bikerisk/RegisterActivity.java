package org.peno.b4.bikerisk;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.view.View;
import android.widget.Toast;


public class RegisterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
    }
    public void registerClicked(View view){
        EditText result1 = (EditText) findViewById(R.id.editText);
        EditText result2 = (EditText) findViewById(R.id.editText2);
        EditText result3 = (EditText) findViewById(R.id.editText3);
        String username = result1.getText().toString();
        String password = result2.getText().toString();
        String email = result3.getText().toString();
        //get color
        //create account with username, password, email and color
    }
}
