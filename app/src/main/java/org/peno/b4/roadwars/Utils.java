package org.peno.b4.roadwars;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;
import org.peno.b4.roadwars.Minigames.StreetRaceGame;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Static constants and functions used across different activities;
 *
 * Created by timo on 10/26/15.
 */
public class Utils {
    public static final float MPS_TO_KMH = 3.6f;
    public static final String HOST = "128.199.52.178";
    public static final int PORT = 4444;
    public static final float MIN_ACCURACY = 18.0f;

    public static final String FRIEND_NFC_INTENT = "add_friend";
    public static final String MINIGAME_NFC_INTENT = "join_minigame";

    public static String removeNumbers(String orig) {
        String street = "";
        /*
            for (String sub : orig.split(" |-")) {
                if (!sub.matches("[0-9]+"))  {  
         */
        for (String sub : orig.split(" ")) {
            if (!sub.matches("[0-9][0-9]*[a-zA-Z]?(?:-[0-9][0-9]*[a-zA-Z]?)?")) {
                street += sub + " ";
            }
        }

        return street.trim();
    }

    public static String lookupStreet(Geocoder geocoder, LatLng pos) {
        if (geocoder != null) {
            try {
                List<Address> locations = geocoder.getFromLocation(pos.latitude,
                        pos.longitude, 1);
                if (locations.size() > 0) {
                    Address loc = locations.get(0);
                    if (loc.getMaxAddressLineIndex() >= 2) {
                        String street = Utils.removeNumbers(loc.getAddressLine(0));
                        String city = removeNumbers(loc.getAddressLine(1));
                        return street + ", " + city;
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //Log.d("POS", "error, geocoder is null!");
        }
        return null;
    }

    public static LatLng getLatLng(Geocoder geocoder, String street){
        if(geocoder != null){
            try{
                List<Address> locations = geocoder.getFromLocationName(street, 1);
                if (locations.size() != 1)
                    return null;
                return new LatLng(locations.get(0).getLatitude(), locations.get(0).getLongitude());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            //Log.d("POS", "error, geocoder is null!");
        }
        return null;
    }

    /**
     * Returns a bitmap of a picture colored in the user's color.
     * If it already exists in the marker cache, it will be reused.
     * Otherwise, a new bitmap is created and added to the marker cache.
     * @param hue: the color(hue) of the user
     * @return The created bitmap
     */
    public static Bitmap getStreetBitmap(HashMap<Float, Bitmap> markerCache,
                                   Bitmap originalBitmap, float hue) {
        if (markerCache != null && markerCache.containsKey(hue)) {
            //Log.d(TAG, "cache hit");
            return markerCache.get(hue);
        }
        //Log.d(TAG, "cache miss");
        int w = originalBitmap.getWidth();
        int h = originalBitmap.getHeight();
        int[] pixels = new int[w*h];
        originalBitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        float[] HSV = new float[3];

        int len = w*h;
        for (int i = 0; i < len; i++) {
            Color.colorToHSV(pixels[i], HSV);
            HSV[0] = hue;
            pixels[i] = Color.HSVToColor(Color.alpha(pixels[i]), HSV);
        }
        Bitmap ret = Bitmap.createBitmap(pixels, w, h, originalBitmap.getConfig());
        if (markerCache != null)
            markerCache.put(hue, ret);
        return ret;
    }

    public static void onResponse(String req, boolean result, JSONObject response) {
        MiniGameManager minigameInstance = MiniGameManager.getInstance();
        if (result) {
            try {
                switch (req) {
                    case "started-minigame":
                        // response.getString("minigame").equals("race") &&
                        //Log.d("IMP", "started-minigame");
                        if (minigameInstance != null) {
                            //TODO: check result of startRaceGame
                            String street = response.getString("street");
                            minigameInstance.startGame(new StreetRaceGame(minigameInstance.context,
                                    street, response.getString("name")));
                        } else {
                            throw new RuntimeException("what????");
                        }
                        break;
                    case "finish-minigame":
                        //you won
                        String street = response.getString("street");
                        minigameInstance.finish(true);
                        break;
                    case "stopped-minigame":
                        minigameInstance.onStop();
                        break;
                    case "stop-minigame":
                        //Log.d("Mini", "successfully stopped minigame");
                        break;
                }
            }  catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            if (req.equals("finish-minigame")) {
                //you lost
                minigameInstance.finish(false);
            }
        }
    }
}
