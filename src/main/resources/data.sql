INSERT INTO roles(role_id, name)
VALUES (1, 'ROLE_USER');
INSERT INTO roles(role_id, name)
VALUES (2, 'ROLE_ADMIN');

INSERT INTO users(email,password,username)
VALUES ('admin@admin.cz','$2a$10$ppr454lZnf6t1cMbU6YGgulOrzZogcIEqH90nflkHQldFabIvnRuW','admin');

INSERT INTO user_roles(user_id, role_id)
VALUES (1,1);
INSERT INTO user_roles(user_id, role_id)
VALUES (1,2);

INSERT INTO user_group( group_name)
VALUES('Example group 1');
INSERT INTO user_group( group_name)
VALUES('Example group 2');
INSERT INTO user_group( group_name)
VALUES('Example group 3');