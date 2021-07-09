package com.btc.ep.plugins.embeddedplatform.util;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {

    private static Logger logger = LoggerFactory.getLogger(Util.class);

    private static final int ONE_DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int ONE_HOUR_IN_MILLIS = 1000 * 60 * 60;
    private static final int ONE_MINUTE_IN_MILLIS = 1000 * 60;
    private static final int ONE_SECOND_IN_MILLIS = 1000;

    public static final DateFormat DATE_FORMAT =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, Locale.getDefault());

    public static Map<Object, Object> toMap(Object... objects) {
        if (objects.length % 2 != 0) {
            throw new IllegalArgumentException(
                "Inconsistent number of key-value pairs (received " + objects.length + " items).");
        }
        Map<Object, Object> map = new HashMap<>();
        for (int i = 0; i < (objects.length - 1); i = i + 2) {
            map.put(objects[i], objects[i + 1]);
        }
        return map;
    }

    /**
     * 
     * Returns the time difference as a String of days, hours, minutes and seconds
     * (00d 00:00:00)
     *
     * @param d1
     * @param d2
     * @return the difference as a string
     */
    public static String getTimeDiffAsString(Date d1, Date d2) {
        long duration = Math.abs(d1.getTime() - d2.getTime());
        return getDurationAsString(duration);
    }

    public static String getDurationAsString(long duration) {
        int days = (int)duration / (ONE_DAY_IN_MILLIS);
        int hours = (int)(duration / (ONE_HOUR_IN_MILLIS)) % 24;
        int minutes = (int)(duration / (ONE_MINUTE_IN_MILLIS)) % 60;
        int seconds = (int)(duration / (ONE_SECOND_IN_MILLIS)) % 60;
        String durationString = (days > 0 ? days + "d" : "");
        durationString += (hours < 10 ? "0" + hours : hours) + ":";
        durationString += (minutes < 10 ? "0" + minutes : minutes) + ":";
        durationString += (seconds < 10 ? "0" + seconds : seconds);
        return durationString;
    }

    public static long durationToMilliseconds(String durationString) {
        long duration = 0;
        try {
            String[] split = durationString.split(":");
            if (split.length == 4) {
                String daysString = split[0].trim().replaceAll("(\\d+)d", "$1");
                int days = Integer.parseInt(daysString);
                duration += days * ONE_DAY_IN_MILLIS;

                int hours = Integer.parseInt(split[1].trim());
                duration += hours * ONE_HOUR_IN_MILLIS;

                int minutes = Integer.parseInt(split[2].trim());
                duration += minutes * ONE_MINUTE_IN_MILLIS;

                int seconds = Integer.parseInt(split[3].trim());
                duration += seconds * ONE_SECOND_IN_MILLIS;
            } else { //if (split.length == 3)
                int hours = Integer.parseInt(split[0].trim());
                duration += hours * ONE_HOUR_IN_MILLIS;

                int minutes = Integer.parseInt(split[1].trim());
                duration += minutes * ONE_MINUTE_IN_MILLIS;

                int seconds = Integer.parseInt(split[2].trim());
                duration += seconds * ONE_SECOND_IN_MILLIS;
            }
        } catch (Exception e) {
            logger.error("Could not transform duration string", e);
        }
        return duration;
    }

    public static File getResourceAsFile(Class<?> referenceClass, String resourcePath) {
        ClassLoader classLoader = referenceClass.getClassLoader();
        return new File(classLoader.getResource(resourcePath).getFile().replace("%20", " "));
    }

}
