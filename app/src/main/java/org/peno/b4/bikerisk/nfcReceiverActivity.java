package org.peno.b4.bikerisk;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class nfcReceiverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);

            NdefMessage message = (NdefMessage) rawMessages[0]; // only one message transferred
            String data = new String(message.getRecords()[0].getPayload());
            String delen[] = data.split(":");
            switch (delen[0]) {
                case Utils.FRIEND_NFC_INTENT:
                    String naam = delen[1];
                    ConnectionManager.getInstance().nfcFriend(naam);
                    break;
                case Utils.MINIGAME_NFC_INTENT:

                    break;

            }

            Toast.makeText(this,data, Toast.LENGTH_LONG).show();
        }
        //Intent intent2 = new Intent(this, MainActivity.class);
        //startActivity(intent2);
        finish();
    }
}
