-- Body parts are all user-defined now: a new account starts without any and adds what it wants to measure.
-- The former built-in waist of existing accounts stays as an ordinary (renamable, deletable) body part.
ALTER TABLE body_part DROP COLUMN code;
