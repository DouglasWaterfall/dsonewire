package waterfall.db.repositories;

import org.springframework.data.repository.CrudRepository;
import waterfall.db.entities.WaterHeaterBurnEntity;

public interface WaterHeaterRepository extends CrudRepository<WaterHeaterBurnEntity, Long> {

}
