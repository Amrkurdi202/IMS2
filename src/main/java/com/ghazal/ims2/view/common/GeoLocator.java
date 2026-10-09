package com.ghazal.ims2.view.common;

import com.vaadin.flow.component.Component;

import java.util.function.Consumer;

/**
 * Client-side geolocation helper for Vaadin 24.
 * <p>
 * The host component must declare a {@code @ClientCallable} method with the
 * given name and signature {@code void method(String payload)} where payload is
 * {@code "lat,lng"} or an empty string on failure.
 * <p>
 * Requires a secure context (HTTPS or localhost) and user permission.
 */
public final class GeoLocator {

    public record GeoPoint(double latitude, double longitude) {}

    public static final double EARTH_RADIUS_M = 6_371_000.0;

    private GeoLocator() {
    }

    /** Triggers browser geolocation; result is delivered to the host's client-callable method. */
    public static void request(Component host, String serverMethodName) {
        host.getElement().executeJs(
                "if(!navigator.geolocation){$0.$server['" + serverMethodName + "']('');return;}"
                        + "navigator.geolocation.getCurrentPosition("
                        + "  function(p){$0.$server['" + serverMethodName + "']"
                        + "(p.coords.latitude + ',' + p.coords.longitude);},"
                        + "  function(){$0.$server['" + serverMethodName + "']('');},"
                        + "  {enableHighAccuracy:true, timeout:10000, maximumAge:0});",
                host.getElement());
    }

    /** Parses the payload sent by {@link #request}; returns null on failure. */
    public static GeoPoint parse(String payload) {
        if (payload == null) {
            return null;
        }
        String[] parts = payload.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            double lat = Double.parseDouble(parts[0].trim());
            double lng = Double.parseDouble(parts[1].trim());
            if (Double.isNaN(lat) || Double.isNaN(lng)) {
                return null;
            }
            return new GeoPoint(lat, lng);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Haversine distance in meters between two WGS84 points. */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }
}