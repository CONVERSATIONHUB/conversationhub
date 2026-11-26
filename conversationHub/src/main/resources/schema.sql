-- Conversation Hub MySQL schema / migration.
-- Safe to run on a fresh database and on the original release-1 database.
-- Tables are created only when missing; new columns are added only when missing.
-- Hibernate ddl-auto=validate then checks the final structure at startup.

create table if not exists app_role (created_at datetime(6) not null, id bigint not null auto_increment, name varchar(60) not null, description varchar(255), primary key (id)) engine=InnoDB;
create table if not exists app_setting (id bigint not null auto_increment, updated_at datetime(6) not null, setting_key varchar(100) not null, setting_value varchar(255), primary key (id)) engine=InnoDB;
create table if not exists app_user (active bit not null, consent_given bit not null, verified bit not null, booking_restricted_until datetime(6), consent_at datetime(6), created_at datetime(6) not null, id bigint not null auto_increment, institution_id bigint, provider_id bigint, role_id bigint not null, updated_at datetime(6) not null, phone varchar(30) not null, email varchar(150), name varchar(150) not null, password_hash varchar(255) not null, account_type ENUM('GUEST','SEEKER','APPOINTMENT_OFFICER','CENTER_MANAGER','INSTITUTIONAL_PROVIDER','INDEPENDENT_PROVIDER','MAIN_ADMIN') not null, primary key (id)) engine=InnoDB;
create table if not exists audit_log (entity_id bigint, id bigint not null auto_increment, performed_at datetime(6) not null, user_id bigint, entity_type varchar(80) not null, action varchar(100) not null, details JSON, primary key (id)) engine=InnoDB;
create table if not exists availability_slot (blocked bit not null, booked_count integer not null, capacity integer not null, created_at datetime(6) not null, end_time datetime(6) not null, id bigint not null auto_increment, provider_id bigint not null, start_time datetime(6) not null, primary key (id)) engine=InnoDB;
create table if not exists booking (booking_for_other bit not null, capacity_held bit not null, response_deadline_hours integer not null, cancelled_at datetime(6), decided_at datetime(6), expired_at datetime(6), expires_at datetime(6) not null, id bigint not null auto_increment, provider_id bigint not null, seeker_user_id bigint not null, service_id bigint not null, slot_id bigint not null, submitted_at datetime(6) not null, updated_at datetime(6) not null, withdrawn_at datetime(6), relationship varchar(100), attendee_name varchar(150), cancellation_rule varchar(500) not null, decision_reason varchar(500), message TEXT, status ENUM('PENDING','CONFIRMED','REJECTED','EXPIRED','CANCELLED','WITHDRAWN','COMPLETED','NO_SHOW') not null, primary key (id)) engine=InnoDB;
create table if not exists deletion_request (id bigint not null auto_increment, requested_at datetime(6) not null, resolved_at datetime(6), resolved_by bigint, user_id bigint not null, reason varchar(500), status enum ('APPROVED','PENDING','REJECTED') not null, primary key (id)) engine=InnoDB;
create table if not exists institution (created_at datetime(6) not null, id bigint not null auto_increment, manager_user_id bigint, updated_at datetime(6) not null, contact_phone varchar(30), contact_email varchar(150), location varchar(200), name varchar(200) not null, contact_address varchar(255), status ENUM('PENDING','APPROVED','SUSPENDED','REJECTED') not null, type ENUM('GOVERNMENT','EDUCATION','HEALTHCARE','COMPANY','OTHER') not null, primary key (id)) engine=InnoDB;
create table if not exists language_preference (id bigint not null auto_increment, updated_at datetime(6) not null, user_id bigint not null, language_code ENUM('EN','SW') not null, primary key (id)) engine=InnoDB;
create table if not exists notification (is_read bit not null, booking_id bigint, created_at datetime(6) not null, id bigint not null auto_increment, user_id bigint not null, message_key varchar(100) not null, language_code ENUM('EN','SW') not null, primary key (id)) engine=InnoDB;
create table if not exists password_reset_token (attempts integer not null, consumed_at datetime(6), created_at datetime(6) not null, expires_at datetime(6) not null, id bigint not null auto_increment, user_id bigint not null, reference varchar(80) not null, token_hash varchar(255) not null, primary key (id)) engine=InnoDB;
create table if not exists provider (active bit not null, cancellation_cutoff_hours integer not null, min_lead_time_hours integer not null, no_show_restriction_hours integer not null, pending_reverification bit not null, published bit not null, response_deadline_hours integer not null, created_at datetime(6) not null, id bigint not null auto_increment, institution_id bigint, pending_service_id bigint, service_id bigint not null, updated_at datetime(6) not null, user_id bigint, name varchar(150) not null, pending_name varchar(150), location varchar(200), pending_location varchar(200), pending_specialisation varchar(200), specialisation varchar(200), cancellation_rule varchar(500) not null, no_show_rule varchar(500) not null, identity_details varchar(2000), pending_identity_details varchar(2000), pending_qualifications varchar(2000), qualifications varchar(2000), pending_service_mode enum ('BOTH','IN_PERSON','REMOTE'), service_mode ENUM('IN_PERSON','REMOTE','BOTH') not null, verification_status ENUM('UNVERIFIED','PENDING','VERIFIED','REJECTED') not null, primary key (id)) engine=InnoDB;
create table if not exists reschedule_request (capacity_held bit not null, booking_id bigint not null, decided_at datetime(6), id bigint not null auto_increment, new_slot_id bigint not null, requested_at datetime(6) not null, decision_reason varchar(500), reason varchar(500), status ENUM('PENDING','APPROVED','REJECTED') not null, primary key (id)) engine=InnoDB;
create table if not exists service (active bit not null, duration_minutes integer not null, created_at datetime(6) not null, id bigint not null auto_increment, service_type varchar(100) not null, charge_information varchar(255), category ENUM('INSTITUTIONAL','HEALTHCARE','COUNSELLING_COMFORTING','PASTORAL') not null, primary key (id)) engine=InnoDB;
create table if not exists slot_recurrence (capacity integer not null, day_of_week integer not null, end_time time(0) not null, start_time time(0) not null, valid_from date not null, valid_until date not null, created_at datetime(6) not null, id bigint not null auto_increment, provider_id bigint not null, primary key (id)) engine=InnoDB;
create table if not exists verification_challenge (attempts integer not null, consumed_at datetime(6), created_at datetime(6) not null, expires_at datetime(6) not null, id bigint not null auto_increment, user_id bigint not null, target_value varchar(150), code_hash varchar(255) not null, challenge_type enum ('CONTACT_CHANGE','REGISTRATION') not null, channel enum ('EMAIL','PHONE') not null, primary key (id)) engine=InnoDB;
create table if not exists verification_record (created_at datetime(6) not null, decided_at datetime(6), decided_by_user_id bigint, id bigint not null auto_increment, institution_id bigint, provider_id bigint, credential_reference varchar(500), document_reference varchar(500), identity_details varchar(2000), qualifications varchar(2000), decision ENUM('PENDING','APPROVED','REJECTED') not null, primary key (id)) engine=InnoDB;

SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'active'),
  'SELECT 1',
  'ALTER TABLE app_user ADD COLUMN active BIT(1) NOT NULL DEFAULT b''1'''
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'consent_given'),
  'SELECT 1',
  'ALTER TABLE app_user ADD COLUMN consent_given BIT(1) NOT NULL DEFAULT b''0'''
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'consent_at'),
  'SELECT 1',
  'ALTER TABLE app_user ADD COLUMN consent_at DATETIME(6) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'booking_restricted_until'),
  'SELECT 1',
  'ALTER TABLE app_user ADD COLUMN booking_restricted_until DATETIME(6) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'booking' AND COLUMN_NAME = 'capacity_held'),
  'SELECT 1',
  'ALTER TABLE booking ADD COLUMN capacity_held BIT(1) NOT NULL DEFAULT b''1'''
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'booking' AND COLUMN_NAME = 'cancelled_at'),
  'SELECT 1',
  'ALTER TABLE booking ADD COLUMN cancelled_at DATETIME(6) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'booking' AND COLUMN_NAME = 'expired_at'),
  'SELECT 1',
  'ALTER TABLE booking ADD COLUMN expired_at DATETIME(6) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'booking' AND COLUMN_NAME = 'withdrawn_at'),
  'SELECT 1',
  'ALTER TABLE booking ADD COLUMN withdrawn_at DATETIME(6) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'institution' AND COLUMN_NAME = 'contact_phone'),
  'SELECT 1',
  'ALTER TABLE institution ADD COLUMN contact_phone VARCHAR(30) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'institution' AND COLUMN_NAME = 'contact_email'),
  'SELECT 1',
  'ALTER TABLE institution ADD COLUMN contact_email VARCHAR(150) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'institution' AND COLUMN_NAME = 'contact_address'),
  'SELECT 1',
  'ALTER TABLE institution ADD COLUMN contact_address VARCHAR(255) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'published'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN published BIT(1) NOT NULL DEFAULT b''0'''
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'location'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN location VARCHAR(200) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'identity_details'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN identity_details VARCHAR(2000) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'qualifications'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN qualifications VARCHAR(2000) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'min_lead_time_hours'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN min_lead_time_hours INT NOT NULL DEFAULT 0'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'cancellation_cutoff_hours'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN cancellation_cutoff_hours INT NOT NULL DEFAULT 24'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'no_show_rule'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN no_show_rule VARCHAR(500) NOT NULL DEFAULT ''A no-show may lead to a temporary booking restriction.'''
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'no_show_restriction_hours'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN no_show_restriction_hours INT NOT NULL DEFAULT 24'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'pending_reverification'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN pending_reverification BIT(1) NOT NULL DEFAULT b''0'''
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'pending_name'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN pending_name VARCHAR(150) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'pending_specialisation'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN pending_specialisation VARCHAR(200) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'pending_identity_details'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN pending_identity_details VARCHAR(2000) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'pending_qualifications'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN pending_qualifications VARCHAR(2000) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'pending_location'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN pending_location VARCHAR(200) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'pending_service_mode'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN pending_service_mode ENUM(''BOTH'',''IN_PERSON'',''REMOTE'') NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'provider' AND COLUMN_NAME = 'pending_service_id'),
  'SELECT 1',
  'ALTER TABLE provider ADD COLUMN pending_service_id BIGINT NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'verification_record' AND COLUMN_NAME = 'identity_details'),
  'SELECT 1',
  'ALTER TABLE verification_record ADD COLUMN identity_details VARCHAR(2000) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'verification_record' AND COLUMN_NAME = 'qualifications'),
  'SELECT 1',
  'ALTER TABLE verification_record ADD COLUMN qualifications VARCHAR(2000) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'verification_record' AND COLUMN_NAME = 'document_reference'),
  'SELECT 1',
  'ALTER TABLE verification_record ADD COLUMN document_reference VARCHAR(500) NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- Enum values and numeric column types added/changed in this release.
ALTER TABLE app_user MODIFY COLUMN account_type ENUM('GUEST','SEEKER','APPOINTMENT_OFFICER','CENTER_MANAGER','INSTITUTIONAL_PROVIDER','INDEPENDENT_PROVIDER','MAIN_ADMIN') NOT NULL;
ALTER TABLE booking MODIFY COLUMN status ENUM('PENDING','CONFIRMED','REJECTED','EXPIRED','CANCELLED','WITHDRAWN','COMPLETED','NO_SHOW') NOT NULL;
ALTER TABLE availability_slot MODIFY COLUMN capacity INT NOT NULL, MODIFY COLUMN booked_count INT NOT NULL;
ALTER TABLE booking MODIFY COLUMN response_deadline_hours INT NOT NULL;
ALTER TABLE provider MODIFY COLUMN response_deadline_hours INT NOT NULL;
ALTER TABLE service MODIFY COLUMN duration_minutes INT NOT NULL;
ALTER TABLE verification_record MODIFY COLUMN credential_reference VARCHAR(500);

-- Existing verified providers were public before the explicit published flag existed.
UPDATE provider SET published = b'1' WHERE verification_status = 'VERIFIED';

alter table app_role add constraint UKdyf5hu50ddvjoy0pfpleevwfl unique (name);
alter table app_setting add constraint UK64fkipvexnau9oxgfw26q0b9w unique (setting_key);
alter table app_user add constraint UKexslcon9jmfy0xhclbtpf26vo unique (phone);
alter table app_user add constraint UK1j9d9a06i600gd43uu3km82jw unique (email);
alter table language_preference add constraint UKk8jy72uqrnk7cggwkqpfmqh83 unique (user_id);
alter table password_reset_token add constraint UKeddmk9wvcss03umlb92qh3tar unique (reference);
alter table provider add constraint UK8oc1o2uw5cl4vnitihnb5dfi5 unique (user_id);
alter table app_user add constraint FKkmgqo1fpdy1lpmrsgilephxsf foreign key (institution_id) references institution (id);
alter table app_user add constraint FKnj6qpbx6dykrjax2xb3amln3y foreign key (provider_id) references provider (id);
alter table app_user add constraint FKn1w45cvkd2bdpvu78k056mckg foreign key (role_id) references app_role (id);
alter table audit_log add constraint FKqsu5o6fffuhrg94lncceiey3n foreign key (user_id) references app_user (id);
alter table availability_slot add constraint FK83hg2pcm86yv4wbq7x7ebxqqd foreign key (provider_id) references provider (id);
alter table booking add constraint FKnijo6hobccis4ybsd6bfvv1tv foreign key (provider_id) references provider (id);
alter table booking add constraint FKemt1bn1wpw4xexrx92rargulo foreign key (seeker_user_id) references app_user (id);
alter table booking add constraint FKcebnlefwi9r13txu8btclnmsu foreign key (service_id) references service (id);
alter table booking add constraint FKq2nspb0jdxkp4uyryeos05q4m foreign key (slot_id) references availability_slot (id);
alter table deletion_request add constraint FKjmibj2o7lyxhr482aoxth2ci8 foreign key (resolved_by) references app_user (id);
alter table deletion_request add constraint FKh40v4aw3jx30uirg62nca80s6 foreign key (user_id) references app_user (id);
alter table institution add constraint FKsb09dfoyy0g6lgf3l6h880d2b foreign key (manager_user_id) references app_user (id);
alter table language_preference add constraint FKgssk2gxpyq3v573p7cyupyqs foreign key (user_id) references app_user (id);
alter table notification add constraint FK2k589bvvxex5a79rt1bdhnw0r foreign key (booking_id) references booking (id);
alter table notification add constraint FKcxbqr6g1py0j4m9qeq44h1ju6 foreign key (user_id) references app_user (id);
alter table password_reset_token add constraint FKli7wollcmb8tibymo3s94o57h foreign key (user_id) references app_user (id);
alter table provider add constraint FKhj0b5cl3kmhm65wdh6h4hpl9o foreign key (institution_id) references institution (id);
alter table provider add constraint FKatw8dvhvxje438kijwdodwert foreign key (pending_service_id) references service (id);
alter table provider add constraint FK4vob1cjbf7e7tjyytgc9qx95t foreign key (service_id) references service (id);
alter table provider add constraint FKg5koq55rolrp8ibiisy4nm91d foreign key (user_id) references app_user (id);
alter table reschedule_request add constraint FK2y0fwkx04yrq4xe9a1hjdsa4h foreign key (booking_id) references booking (id);
alter table reschedule_request add constraint FKfcfabqfkajw4iqtm24kl6rt8d foreign key (new_slot_id) references availability_slot (id);
alter table slot_recurrence add constraint FK378v2eutnfcapje46ehdtjbej foreign key (provider_id) references provider (id);
alter table verification_challenge add constraint FKs4tg5t3umbxev00e4egb6q1m0 foreign key (user_id) references app_user (id);
alter table verification_record add constraint FKn76kk0tu2jejx84gjyheyn3io foreign key (decided_by_user_id) references app_user (id);
alter table verification_record add constraint FK5x8jwqhluf4s5nsqkmi75uvrk foreign key (institution_id) references institution (id);
alter table verification_record add constraint FK2frsaf79jh7gnke1rwna9ehae foreign key (provider_id) references provider (id);
