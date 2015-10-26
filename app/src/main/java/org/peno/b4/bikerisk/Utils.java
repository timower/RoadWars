package org.peno.b4.bikerisk;

/**
 * Created by timo on 10/26/15.
 */
public class Utils {
    public static final float MPS_TO_KMH = 3.6f;

    public static String removeNumbers(String orig) {
        String street = "";
        for (String sub : orig.split(" ")) {
            if (!sub.matches("[0-9][0-9]*[A-Z]?-?[0-9]*[A-Z]?")) {
                street += sub + " ";
            }
        }

        return street.trim();
    }
}
