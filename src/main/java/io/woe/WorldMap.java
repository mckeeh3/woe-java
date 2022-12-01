package io.woe;

interface WorldMap {
  static final int earthRadiusKm = 6371;

  static LatLng latLng(double lat, double lng) {
    return new LatLng(lat, lng);
  }

  record LatLng(double lat, double lng) {
    static WorldMap.LatLng fromRadians(double lat, double lng) {
      return new WorldMap.LatLng(Math.toDegrees(lat), Math.toDegrees(lng));
    }
  }
}
