package com.zkry.map.util;

import com.zkry.map.dto.MapPoint;

/** 中国大陆高德 GCJ-02 与地图常用 WGS84 坐标互转。 */
public final class MapCoordinates {

    private static final double PI = Math.PI;
    private static final double SEMI_MAJOR_AXIS = 6378245.0;
    private static final double ECCENTRICITY_SQUARED = 0.006693421622965943;

    private MapCoordinates() {
    }

    public static MapPoint wgs84ToGcj02(MapPoint point) {
        if (!convertible(point)) return point;
        double longitude = point.longitude();
        double latitude = point.latitude();
        double[] delta = delta(longitude, latitude);
        return new MapPoint(longitude + delta[0], latitude + delta[1]);
    }

    public static MapPoint gcj02ToWgs84(MapPoint point) {
        if (!convertible(point)) return point;
        MapPoint shifted = wgs84ToGcj02(point);
        return new MapPoint(
            point.longitude() * 2 - shifted.longitude(),
            point.latitude() * 2 - shifted.latitude()
        );
    }

    public static boolean outOfChina(double longitude, double latitude) {
        return longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271;
    }

    private static boolean convertible(MapPoint point) {
        return point != null && point.available()
            && Double.isFinite(point.longitude()) && Double.isFinite(point.latitude())
            && !outOfChina(point.longitude(), point.latitude());
    }

    private static double[] delta(double longitude, double latitude) {
        double latitudeDelta = transformLatitude(longitude - 105.0, latitude - 35.0);
        double longitudeDelta = transformLongitude(longitude - 105.0, latitude - 35.0);
        double radianLatitude = latitude / 180.0 * PI;
        double sinLatitude = Math.sin(radianLatitude);
        double magic = 1 - ECCENTRICITY_SQUARED * sinLatitude * sinLatitude;
        double rootMagic = Math.sqrt(magic);
        latitudeDelta = latitudeDelta * 180.0
            / ((SEMI_MAJOR_AXIS * (1 - ECCENTRICITY_SQUARED)) / (magic * rootMagic) * PI);
        longitudeDelta = longitudeDelta * 180.0
            / (SEMI_MAJOR_AXIS / rootMagic * Math.cos(radianLatitude) * PI);
        return new double[]{longitudeDelta, latitudeDelta};
    }

    private static double transformLatitude(double x, double y) {
        double result = -100 + 2 * x + 3 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        result += (20 * Math.sin(6 * x * PI) + 20 * Math.sin(2 * x * PI)) * 2 / 3;
        result += (20 * Math.sin(y * PI) + 40 * Math.sin(y / 3 * PI)) * 2 / 3;
        return result + (160 * Math.sin(y / 12 * PI) + 320 * Math.sin(y * PI / 30)) * 2 / 3;
    }

    private static double transformLongitude(double x, double y) {
        double result = 300 + x + 2 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        result += (20 * Math.sin(6 * x * PI) + 20 * Math.sin(2 * x * PI)) * 2 / 3;
        result += (20 * Math.sin(x * PI) + 40 * Math.sin(x / 3 * PI)) * 2 / 3;
        return result + (150 * Math.sin(x / 12 * PI) + 300 * Math.sin(x / 30 * PI)) * 2 / 3;
    }
}
