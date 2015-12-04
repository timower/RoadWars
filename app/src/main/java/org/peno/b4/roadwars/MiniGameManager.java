package org.peno.b4.roadwars;

import android.content.Context;
import android.location.Location;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;

import org.peno.b4.roadwars.Minigames.Minigame;
import org.peno.b4.roadwars.Minigames.UIGame;

/**
 *
 */
public class MiniGameManager {

    public static class UIObjects {
        public GoogleMap mMap;
        public TextView textView;
        public View container;

        public UIObjects(GoogleMap mMap, TextView view, View container) {
            this.mMap = mMap;
            this.textView = view;
            this.container = container;
        }
    }

    private UIObjects UIobjects;
    public Context context;

    private Minigame runningMiniGame;

    private static MiniGameManager instance = null;


    private MiniGameManager() {
        runningMiniGame = null;
    }

    public static MiniGameManager getInstance() {
        if (instance == null)
            instance = new MiniGameManager();
        return instance;
    }

    public void setContext(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    public void setUIObjects(UIObjects newObjects) {
        this.UIobjects = newObjects;
        drawUI();
    }

    public boolean startGame(Minigame minigame) {
        if (PositionManager.getInstance().start()) {
            if (minigame.start()) {
                runningMiniGame = minigame;
                drawUI();
                //TODO: find better way:
                ProgressTracker.getInstance().invalidateOptionsMenu();
                return true;
            } else {
                PositionManager.getInstance().stop();
            }
        }
        return false;
    }


    public void finish(boolean won) {
        if (runningMiniGame == null)
            return;
        runningMiniGame.finish(won);
        String UIstring;
        if (won) {
            Toast.makeText(context, R.string.you_won, Toast.LENGTH_SHORT).show();
            UIstring = context.getString(R.string.player_win, runningMiniGame.getPoints());
        } else {
            Toast.makeText(context, R.string.player_lose, Toast.LENGTH_SHORT).show();
            UIstring = context.getString(R.string.player_lose);
        }
        runningMiniGame = new UIGame(context, UIstring);
        drawUI();
    }

    public void stop() {
        if (runningMiniGame != null)
            runningMiniGame.stop();
        runningMiniGame = null;
        drawUI();
    }

    public void onStop() {
        if (runningMiniGame != null)
            runningMiniGame.onStop();
        Toast.makeText(context, R.string.opponent_quit, Toast.LENGTH_SHORT).show();
        runningMiniGame = new UIGame(context, context.getString(R.string.opponent_quit));
        drawUI();
    }


    private void drawUI() {
        if (UIobjects == null)
            return;

        if (runningMiniGame == null) {
            UIobjects.container.setVisibility(View.GONE);
        } else {
            runningMiniGame.drawUI(UIobjects);
        }
    }


    public void onLocationChanged(Location location) {
        if (runningMiniGame != null) {
            runningMiniGame.onLocationChanged(location);
        }
    }
}

