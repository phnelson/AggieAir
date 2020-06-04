package edu.ucdavis.corbin.airdavis;

import android.location.Location;

public class dataSet {
    private String lat;
    private String lon;
    private int pm25;
    private Location loc;
    dataSet(Location l, int p) {
        setAll(l,p);
    }

    public String getLat() {
        return lat;
    }

    public String getLon() {
        return lon;
    }

    public int getPm25() {
        return pm25;
    }

    public void setAll(Location l, int p) {
        if(l != null) {
            lat = Location.convert(l.getLatitude(), Location.FORMAT_DEGREES);
            lon = Location.convert(l.getLongitude(), Location.FORMAT_DEGREES);
        }
        loc = l;
        pm25 = p;
    }
    public String getLocationString() {
//        char vertDir = Integer.parseInt(lat) > 0 ? 'N' : 'S';
//        char horzDir = Integer.parseInt(lon) > 0 ? 'E' : 'W';
        if(loc == null) return null;
        return getLat() + ',' + getLon();
    }

    public Location getLoc() {
        return loc;
    }
}
