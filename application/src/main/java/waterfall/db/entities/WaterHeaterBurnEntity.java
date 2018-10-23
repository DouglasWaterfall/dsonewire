package waterfall.db.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "water_heater_burn")
public class WaterHeaterBurnEntity {

  @Id
  @GeneratedValue(strategy= GenerationType.IDENTITY)
  private Integer id;
  private long startDts;  // system time millis in UTC
  private long endDts;    // system time millis in UTC

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getId() {
    return this.id;
  }

  public void setStartDTS(long startDTS) {
    this.startDts = startDTS;
  }

  public long getStartDTS() {
    return this.startDts;
  }

  public void setEndDTS(long endDTS) {
    this.endDts = endDTS;
  }

  public long getEndDTS() {
    return this.endDts;
  }

}
