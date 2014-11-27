package org.jessies.dalvikexplorer;

import android.hardware.Sensor;
import android.hardware.SensorManager;

import java.util.HashMap;
import java.util.Locale;

public class SensorItem implements BetterArrayAdapter.Subtitleable {
  private final HashMap<Sensor, ImmutableSensorEvent> events;
  private final Sensor sensor;

  public SensorItem(HashMap<Sensor, ImmutableSensorEvent> events, Sensor sensor) {
    this.events = events;
    this.sensor = sensor;
  }

  @Override public String toString() {
    return sensor.getName();
  }

  public String toSubtitle() {
    ImmutableSensorEvent sensorEvent = events.get(sensor);
    if (sensorEvent != null) {
      return toString(sensorEvent);
    }
    return "No data.";
  }

  private String toString(ImmutableSensorEvent e) {
    switch (e.sensor.getType()) {
      case Sensor.TYPE_ACCELEROMETER:
      case Sensor.TYPE_GRAVITY:
      case Sensor.TYPE_LINEAR_ACCELERATION:
        return String.format(Locale.US, "x=%+.1f m/s\u00b2, y=%+.1f m/s\u00b2, z=%+.1f m/s\u00b2", e.values[0], e.values[1], e.values[2]);
      case Sensor.TYPE_MAGNETIC_FIELD:
        return String.format(Locale.US, "x=%.1f \u00b5T, y=%.1f \u00b5T, z=%.1f \u00b5T", e.values[0], e.values[1], e.values[2]);
      case Sensor.TYPE_GYROSCOPE:
        return String.format(Locale.US, "x=%+.1f radian/s, y=%+.1f radian/s, z=%+.1f radian/s", e.values[0], e.values[1], e.values[2]);
      case Sensor.TYPE_LIGHT:
        return String.format(Locale.US, "%.1f lux", e.values[0]);
      case Sensor.TYPE_PRESSURE:
        return String.format(Locale.US, "%.1f hPa", e.values[0]);
      case Sensor.TYPE_PROXIMITY:
        return String.format(Locale.US, "%.1f cm", e.values[0]);
      case Sensor.TYPE_RELATIVE_HUMIDITY:
        return String.format(Locale.US, "%.1f %%", e.values[0]);
      case Sensor.TYPE_AMBIENT_TEMPERATURE:
        return String.format(Locale.US, "%.1f C", e.values[0]);
      case Sensor.TYPE_ROTATION_VECTOR:
        float[] r = new float[16];
        SensorManager.getRotationMatrixFromVector(r, e.values);
        float[] values = new float[3];
        SensorManager.getOrientation(r, values);
        return String.format(Locale.US, "azimuth=%+.1f, pitch=%+.1f, roll=%+.1f", values[0], values[1], values[2]);
      default:
        return e.sensor.getType() + " " + java.util.Arrays.toString(e.values);
    }
  }
}
