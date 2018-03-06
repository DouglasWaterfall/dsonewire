package waterfall.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("waterheater")
public class WaterHeaterConfig {
  private float triggerTemp;
  private int sampleTimeSec;
  private int windowSize;
  private String dsAddress;

  public float getTriggerTemp() {
    return triggerTemp;
  }

  public void setTriggerTemp(float triggerTemp) {
    this.triggerTemp = triggerTemp;
  }

  public int getSampleTimeSec() {
    return sampleTimeSec;
  }

  public void setSampleTimeSec(int sampleTimeSec) {
    this.sampleTimeSec = sampleTimeSec;
  }

  public int getWindowSize() {
    return windowSize;
  }

  public void setWindowSize(int windowSize) {
    this.windowSize = windowSize;
  }

  public String getDsAddress() {
    return dsAddress;
  }

  public void setDsAddress(String dsAddress) {
    this.dsAddress = dsAddress;
  }

}
