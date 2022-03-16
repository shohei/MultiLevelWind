package net.sf.openrocket.example;

import java.util.ArrayList;
import java.util.List;
import net.sf.openrocket.models.wind.PinkNoiseWindModel;
import net.sf.openrocket.models.wind.WindModel;
import net.sf.openrocket.util.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiLevelWindModel implements WindModel {
  private static final Logger log = LoggerFactory.getLogger(MultiLevelWindModel.class);
  
  public static class WindSpecs {
    public final double altitude;
    
    public final double speed;
    
    public final double direction;
    
    public WindSpecs(double a, double s, double d) {
      this.altitude = a;
      this.speed = s;
      this.direction = d;
    }
  }
  
  private PinkNoiseWindModel mainWindModel = new PinkNoiseWindModel(10);
  
  private List<WindSpecs> windSpecs = null;
  
  public MultiLevelWindModel(List<WindSpecs> winds) {
    this.windSpecs = new ArrayList<WindSpecs>();
    for (WindSpecs windSpec : winds)
      this.windSpecs.add(new WindSpecs(windSpec.altitude, windSpec.speed, windSpec.direction)); 
    this.mainWindModel.setAverage(((WindSpecs)this.windSpecs.get(0)).speed);
    this.mainWindModel.setDirection(((WindSpecs)this.windSpecs.get(0)).direction);
  }
  
  public int getModID() {
    return 0;
  }
  
  public void setStandardDeviation(double standardDeviation) {
    this.mainWindModel.setStandardDeviation(standardDeviation);
  }
  
  public void setTurbulenceIntensity(double intensity) {
    this.mainWindModel.setTurbulenceIntensity(intensity);
  }
  
  public Coordinate getWindVelocity(double time, double altitude) {
    WindSpecs last_m = null;
    for (WindSpecs m : this.windSpecs) {
      if (m.altitude >= altitude)
        return interpolateWindVelocity(time, altitude, last_m, m); 
      last_m = m;
    } 
    Coordinate ret = interpolateWindVelocity(time, altitude, last_m, null);
    return ret;
  }
  
  private double interpolate(double v1, double v2, double w) {
    return v1 + (v2 - v1) * w;
  }
  
  private String modelToString(WindSpecs m) {
    if (m == null)
      return "(null)"; 
    return String.format("%dft %ddegrees %dspeed", new Object[] { Long.valueOf(Math.round(m.altitude)), Long.valueOf(Math.round(m.direction * 180.0D / Math.PI)), 
          Long.valueOf(Math.round(m.speed)) });
  }
  
  private Coordinate interpolateWindVelocity(double time, double altitude, WindSpecs m1, WindSpecs m2) {
    double speed = 0.0D;
    double direction = 0.0D;
    if (m1 == null) {
      speed = m2.speed;
      direction = m2.direction;
    } else if (m2 == null) {
      speed = m1.speed;
      direction = m1.direction;
    } else {
      double a = (altitude - m1.altitude) / (m2.altitude - m1.altitude);
      speed = interpolate(m1.speed, m2.speed, a);
      double sinSum = (1.0D - a) * Math.sin(m1.direction) + a * Math.sin(m2.direction);
      double cosSum = (1.0D - a) * Math.cos(m1.direction) + a * Math.cos(m2.direction);
      direction = Math.atan2(sinSum, cosSum);
    } 
    this.mainWindModel.setAverage(speed);
    this.mainWindModel.setDirection(direction);
    Coordinate ret = this.mainWindModel.getWindVelocity(time, altitude);
    log.debug(String.format("interpolating alt %f between layers %s and %s returning %s", new Object[] { Double.valueOf(altitude), modelToString(m1), modelToString(m2), ret.toString() }));
    return ret;
  }
}

