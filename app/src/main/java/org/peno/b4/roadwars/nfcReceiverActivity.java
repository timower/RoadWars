package org.peno.b4.roadwars;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.json.JSONObject;
import org.peno.b4.roadwars.Minigames.StreetRaceGame;

public class nfcReceiverActivity extends AppCompatActivity implements ConnectionManager.ResponseListener {

    private String opponent;
    private String street;

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
                    finish();
                    break;
                case Utils.MINIGAME_NFC_INTENT:
                    if (delen.length != 3)
                        finish();
                    street = delen[1];
                    opponent = delen[2];
                    ConnectionManager.getInstance(this, this).startMinigame(opponent, street);
                    break;

            }

        }
        //Intent intent2 = new Intent(this, MainActivity.class);
        //startActivity(intent2);
        //finish();
    }

    @Override
    public boolean onResponse(String req, Boolean result, JSONObject response) {
        if (req.equals("start-minigame")) {
            if (result) {
                //MiniGameManager.getInstance().startRaceGame(street, opponent);
                if (MiniGameManager.getInstance().startGame(new StreetRaceGame(this, street, opponent))) {
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, R.string.error_start_minigame, Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(this, getString(R.string.other_offline), Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onConnectionLost(String reason) {

    }
}
