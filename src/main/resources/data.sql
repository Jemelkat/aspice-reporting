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