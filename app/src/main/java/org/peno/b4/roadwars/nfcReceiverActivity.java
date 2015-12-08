package org.peno.b4.roadwars;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.peno.b4.roadwars.Minigames.StreetRaceGame;

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
            if (delen.length > 1)
                finish();
            switch (delen[0]) {
                case Utils.FRIEND_NFC_INTENT:
                    if (delen.length != 2)
                        finish();
                    String naam = delen[1];
                    ConnectionManager.getInstance().nfcFriend(naam);
                    Toast.makeText(this, getString(R.string.nfc_friend), Toast.LENGTH_LONG).show();
                    break;
                case Utils.MINIGAME_NFC_INTENT:
                    if (delen.length != 3)
                        finish();
                    MiniGameManager instance = MiniGameManager.getInstance();
                    instance.startGame(new StreetRaceGame(instance.context, delen[1], delen[2]));
                    break;

            }

        }
        //Intent intent2 = new Intent(this, MainActivity.class);
        //startActivity(intent2);
        finish();
    }
}
