-- this file contains the indexes that do not add any constraints, defined solely 
-- for performance issues (unique indexes are therefore not present in this file, 
-- but in bgeeConstraint.sql)

CREATE INDEX affym_chip_idx ON affymetrixProbeset(bgeeAffymetrixChipId);