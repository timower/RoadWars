package org.peno.b4.bikerisk;

import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

/**
 * Created by timo on 10/26/15.
 */
public class Utils {
    public static final float MPS_TO_KMH = 3.6f;

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
}
