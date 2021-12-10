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



INSERT INTO user_group( group_name)
VALUES('Test group 1');
INSERT INTO user_group( group_name)
VALUES('Test group 2');
INSERT INTO user_group( group_name)
VALUES('Test group 3');
INSERT INTO user_group( group_name)
VALUES('Test group 4');

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


INSERT INTO template( template_name, template_created, template_last_updated, user_id)
VALUES ('Template 1', current_timestamp, current_timestamp, 1);
INSERT INTO template( template_name, template_created, template_last_updated, user_id)
VALUES ('Template 2', current_timestamp, current_timestamp, 1);
INSERT INTO template( template_name, template_created, template_last_updated, user_id)
VALUES ('Template 3', current_timestamp, current_timestamp, 1);
INSERT INTO template( template_name, template_created, template_last_updated, user_id)
VALUES ('Template 4', current_timestamp, current_timestamp, 1);
INSERT INTO template( template_name, template_created, template_last_updated, user_id)
VALUES ('Template 5', current_timestamp, current_timestamp, 1);

INSERT INTO report( report_name, report_created, report_last_updated, user_id)
VALUES ('Report 1', current_timestamp, current_timestamp, 1);
INSERT INTO report( report_name, report_created, report_last_updated, user_id)
VALUES ('Report 2', current_timestamp, current_timestamp, 1);
INSERT INTO report( report_name, report_created, report_last_updated, user_id)
VALUES ('Report 3', current_timestamp, current_timestamp, 1);
INSERT INTO report( report_name, report_created, report_last_updated, user_id)
VALUES ('Report 4', current_timestamp, current_timestamp, 1);