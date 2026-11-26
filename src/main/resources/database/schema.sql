CREATE DATABASE IF NOT EXISTS campus_lost_found;
USE campus_lost_found;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('STUDENT', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

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

CREATE TABLE IF NOT EXISTS items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL,
    report_type ENUM('LOST', 'FOUND') NOT NULL,
    title VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(60) NOT NULL,
    location VARCHAR(160) NOT NULL,
    event_date DATE NOT NULL,
    identifying_marks TEXT NOT NULL,
    contact_details VARCHAR(255) NOT NULL,
    status ENUM('OPEN', 'CLAIM_PENDING', 'CLAIM_APPROVED', 'RETURNED', 'CLOSED', 'ARCHIVED') NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_items_reporter FOREIGN KEY (reporter_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS item_images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_item_images_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS claims (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    claimant_id BIGINT NOT NULL,
    ownership_explanation TEXT NOT NULL,
    identifying_details TEXT NOT NULL,
    possession_information TEXT NOT NULL,
    additional_notes TEXT NOT NULL,
    evidence_path VARCHAR(500),
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN') NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT,
    review_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL,
    CONSTRAINT fk_claims_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_claims_claimant FOREIGN KEY (claimant_id) REFERENCES users(id),
    CONSTRAINT fk_claims_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

CREATE INDEX idx_items_status ON items(status);
CREATE INDEX idx_items_category ON items(category);
CREATE INDEX idx_items_location ON items(location);
CREATE INDEX idx_claims_item_status ON claims(item_id, status);
