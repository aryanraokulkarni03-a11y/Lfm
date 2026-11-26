USE campus_lost_found;

CREATE TABLE IF NOT EXISTS user_profiles (
    user_id BIGINT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    degree VARCHAR(80) NOT NULL DEFAULT '',
    branch VARCHAR(120) NOT NULL DEFAULT '',
    registration_number VARCHAR(40) NOT NULL DEFAULT '',
    semester VARCHAR(20) NOT NULL DEFAULT '',
    batch VARCHAR(20) NOT NULL DEFAULT '',
    section VARCHAR(20) NOT NULL DEFAULT '',
    hostel_name VARCHAR(120) NOT NULL DEFAULT '',
    room_number VARCHAR(40) NOT NULL DEFAULT '',
    phone VARCHAR(30) NOT NULL,
    gender VARCHAR(30) NOT NULL DEFAULT '',
    university_details VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- MySQL 8.4 does not support ADD COLUMN IF NOT EXISTS. Check information_schema
-- so this migration is safe for both old and fresh local databases.
DELIMITER //
CREATE PROCEDURE migrate_user_profiles()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_profiles' AND column_name = 'degree') THEN
        ALTER TABLE user_profiles ADD COLUMN degree VARCHAR(80) NOT NULL DEFAULT '';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_profiles' AND column_name = 'branch') THEN
        ALTER TABLE user_profiles ADD COLUMN branch VARCHAR(120) NOT NULL DEFAULT '';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_profiles' AND column_name = 'registration_number') THEN
        ALTER TABLE user_profiles ADD COLUMN registration_number VARCHAR(40) NOT NULL DEFAULT '';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_profiles' AND column_name = 'semester') THEN
        ALTER TABLE user_profiles ADD COLUMN semester VARCHAR(20) NOT NULL DEFAULT '';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_profiles' AND column_name = 'batch') THEN
        ALTER TABLE user_profiles ADD COLUMN batch VARCHAR(20) NOT NULL DEFAULT '';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_profiles' AND column_name = 'section') THEN
        ALTER TABLE user_profiles ADD COLUMN section VARCHAR(20) NOT NULL DEFAULT '';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_profiles' AND column_name = 'hostel_name') THEN
        ALTER TABLE user_profiles ADD COLUMN hostel_name VARCHAR(120) NOT NULL DEFAULT '';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_profiles' AND column_name = 'room_number') THEN
        ALTER TABLE user_profiles ADD COLUMN room_number VARCHAR(40) NOT NULL DEFAULT '';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_profiles' AND column_name = 'gender') THEN
        ALTER TABLE user_profiles ADD COLUMN gender VARCHAR(30) NOT NULL DEFAULT '';
    END IF;
END//
DELIMITER ;

CALL migrate_user_profiles();
DROP PROCEDURE migrate_user_profiles;
