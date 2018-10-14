CREATE TABLE `waterheater` (
  `id` char(32) NOT NULL,
  `startDts` BIGINT(20) NOT NULL,
  `endDts` BIGINT(20),
  `state` INT(1) NOT NULL,
  PRIMARY KEY (`id`)
);