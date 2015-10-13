package org.peno.b4.bikerisk;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class ErrorActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "org.extra_messsage";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error);
        TextView message = (TextView)findViewById(R.id.error_text);
        message.setText(getIntent().getStringExtra(EXTRA_MESSAGE));
    }
}
