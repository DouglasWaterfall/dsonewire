package waterfall.db.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class WaterHeaterBurnEntity {
  @Id
  @GeneratedValue(strategy= GenerationType.AUTO)
  private Integer id;
  private Long startDts;  // system time millis in UTC
  private Long endDts;    // system time millis in UTC

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getId() {
    return this.id;
  }

  public void setStartDTS(Long startDTS) {
    this.startDts = startDTS;
  }

  public Long getStartDTS() {
    return this.startDts;
  }

  public void setEndDTS(Long endDTS) {
    this.endDts = endDTS;
  }

  public Long getEndDTS() {
    return this.endDts;
  }

}
