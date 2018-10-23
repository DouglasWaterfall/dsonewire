using home;

CREATE TABLE `water_heater_burn` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `start_dts` BIGINT(20) NOT NULL,
  `end_dts` BIGINT(20) NOT NULL,
  PRIMARY KEY (`id`)
);