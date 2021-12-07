INSERT INTO role(role_id, name)
VALUES (1, 'ROLE_USER');
INSERT INTO role(role_id, name)
VALUES (2, 'ROLE_ADMIN');

INSERT INTO users(user_id,email,password,username)
VALUES (1,'admin@admin.cz','$2a$10$ppr454lZnf6t1cMbU6YGgulOrzZogcIEqH90nflkHQldFabIvnRuW','admin');

INSERT INTO user_roles(user_id, role_id)
VALUES (1,1);
INSERT INTO user_roles(user_id, role_id)
VALUES (1,2);



INSERT INTO user_group(group_id, group_name)
VALUES(1, 'Test group 1');
INSERT INTO user_group(group_id, group_name)
VALUES(2, 'Test group 2');
INSERT INTO user_group(group_id, group_name)
VALUES(3, 'Test group 3');
INSERT INTO user_group(group_id, group_name)
VALUES(4, 'Test group 4');
INSERT INTO user_group(group_id, group_name)
VALUES(5, 'Test group 5');

INSERT INTO source( source_name, user_id, source_created, source_last_updated)
VALUES ('Test soubor 1', 1, current_timestamp, current_timestamp);
INSERT INTO source(source_name, user_id)
VALUES ('Test soubor 2', 1);
INSERT INTO source(source_name, user_id)
VALUES ('Test soubor 3', 1);
INSERT INTO source(source_name, user_id)
VALUES ('Test soubor 4', 1);
INSERT INTO source(source_name, user_id)
VALUES ('Test soubor 5', 1);