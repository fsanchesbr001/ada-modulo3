INSERT INTO auth_user (id, username, password_hash, enabled)
VALUES (1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', TRUE)
ON DUPLICATE KEY UPDATE username = VALUES(username), password_hash = VALUES(password_hash), enabled = VALUES(enabled);
