package org.jessies.dalvikexplorer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

/**
 * An immutable copy of a SensorEvent's data. A SensorEvent is valid only for the lifetime of the callback.
 * If you want to store a SensorEvent indefinitely, as we do, you need something like this.
 */
public class ImmutableSensorEvent {
  public final Sensor sensor;
  public final float[] values;

  public ImmutableSensorEvent(SensorEvent e) {
    sensor = e.sensor;
    values = e.values.clone();
  }
}
