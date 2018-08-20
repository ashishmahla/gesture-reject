package com.gesturecaller.models;

import android.content.Context;
import android.location.Location;

import com.gesturecaller.utils.GPSTracker;

/**
 * +Created by Ashish on 2/15/2018.
 */

public class MyLocation {

    private static final int THRESHOLD_DISTANCE = 200;
    private int id;
    private String name;
    private double latitude;
    private double longitude;
    private String message;
    private boolean enabled;

    public MyLocation() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Location getLocation() {
        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        return location;
    }

    public String getStringLocation() {
        return "(" + latitude + ", " + longitude + ")";
    }

    public String getDistance(Context context) {
        GPSTracker gpsTracker = new GPSTracker(context);
        Location currLoc = gpsTracker.getLocation();
        gpsTracker.stopUsingGPS();

        float distance = -1;
        if (currLoc != null) {
            try {
                distance = currLoc.distanceTo(getLocation());
            } catch (Exception ignored) {
            }
        }

        return distance >= 0 ? String.valueOf(distance) : "";
    }

    public String getFormattedDistance(Context context) {
        String d = getDistance(context);
        if (d.isEmpty()) {
            return d;
        } else {
            return (int)Float.parseFloat(d) / (float)1000 + " km";
        }
    }

    public boolean isActive(Context context) {
        String distance = getDistance(context);
        return !distance.isEmpty() && Float.parseFloat(distance) <= THRESHOLD_DISTANCE;
    }
}