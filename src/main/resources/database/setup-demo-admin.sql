USE campus_lost_found;

-- Register this address through the JavaFX UI first, then run this script.
UPDATE users
SET role = 'ADMIN'
WHERE email = 'admin@srmap.edu.in';

SELECT id, email, role, active
FROM users
WHERE email = 'admin@srmap.edu.in';
