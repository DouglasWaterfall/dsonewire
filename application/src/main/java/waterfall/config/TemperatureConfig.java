package waterfall.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import waterfall.onewire.DSAddress;
import waterfall.onewire.device.DS18B20Scratchpad;

@Component
@ConfigurationProperties("temperature")
public class TemperatureConfig {

  private List<Temperature> list;

  public List<Temperature> getList() {
    return list;
  }

  public void setList(List<Temperature> list) {
    this.list = list;
  }

  public static class Temperature {

    private String location;
    private DSAddress dsAddress;
    private byte resolution;

    public String getLocation() {
      return location;
    }

    public void setLocation(String location) {
      this.location = location;
    }

    public DSAddress getDsAddress() {
      return dsAddress;
    }

    public void setDsAddress(String dsAddress) {
      this.dsAddress = DSAddress.fromUncheckedHex(dsAddress);
    }

    public byte getResolution() {
      return resolution;
    }

    public void setResolution(String v) throws Exception {
      String[] s = v.split("\\.");
      if ((s.length != 2) || (!"DS18B20Scratchpad".equals(s[0]))) {
        throw new IllegalArgumentException("Resolution must be of the form DS18B20Scratchpad.RESOLUTION_*");
      }
      resolution = DS18B20Scratchpad.class.getDeclaredField(s[1]).getByte(null);
    }

  }

}
