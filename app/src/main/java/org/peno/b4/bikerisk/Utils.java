package org.peno.b4.bikerisk;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by timo on 10/26/15.
 */
public class Utils {
    public static final float MPS_TO_KMH = 3.6f;
    public static final String HOST = "128.199.52.178";
    public static final int PORT = 4444;

    public static String removeNumbers(String orig) {
        String street = "";
        for (String sub : orig.split(" ")) {
            if (!sub.matches("[0-9][0-9]*[a-zA-Z]?-?[0-9]*[a-zA-Z]?")) {
                street += sub + " ";
            }
        }

        return street.trim();
    }

    public static String lookupStreet(Geocoder geocoder, LatLng pos) {
        //TODO: support diffrent cities
        if (geocoder != null) {
            try {
                List<Address> locations = geocoder.getFromLocation(pos.latitude,
                        pos.longitude, 1);
                if (locations.size() > 0) {
                    Address loc = locations.get(0);
                    if (loc.getMaxAddressLineIndex() >= 2) {
                        String street = Utils.removeNumbers(loc.getAddressLine(0));
                        //String city = removeNumbers(loc.getAddressLine(1));
                        return street;
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("POS", "error, geocoder is null!");
        }
        return null;
    }

    /**
     * Creates a new bitmap with the hue of the user
     * @param hue The users color(hue)
     * @return the created bitmap
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
}
