INSERT INTO auth_user (id, username, password_hash, enabled)
VALUES (1, 'admin', '$2a$10$6osrAkxMq0jbHnGj9ddEkOjHHVLNJLc7syd4eL7aMshr54w6RZWaa', TRUE)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password_hash = VALUES(password_hash),
    enabled = VALUES(enabled);
