package org.peno.b4.bikerisk;

import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

/**
 * Created by timo on 12/1/15.
 */
public class ProgressTracker {
    public final static int REASON_CALCULATING = 1 << 0;
    public final static int REASON_GPS = 1 << 1;
    public final static int REASON_LOGIN = 1 << 2;
    public final static int REASON_GPS_DISABLED = 1 << 3;
    private ProgressBar progressBar;
    private int reasons = 0;

    public ProgressTracker() {
    }

    public void setProgressBar(ProgressBar pb) {
        progressBar = pb;
        updateProgressBar();
    }

    public void showProgressBar(int reason) {
        reasons |= reason;
        updateProgressBar();
    }

    public void hideProgressBar(int reason) {
        reasons &= ~reason; //TODO: fix
        updateProgressBar();
    }

    public void updateProgressBar() {
        if (progressBar == null)
            return;
        Log.d("PROG", "reasons: " + reasons);
        if (reasons == 0)
            progressBar.setVisibility(View.GONE);
        else
            progressBar.setVisibility(View.VISIBLE);
    }

    public boolean visible() {
        return reasons != 0;
    }
}
