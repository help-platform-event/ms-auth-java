-- Per-user availability and notification settings (1:1 with users, sharing its PK).
-- A user without a row simply has the defaults (everything TRUE); the row is created on first update.

CREATE TABLE user_settings (
    user_id               BINARY(16) NOT NULL,
    -- Availability (days of the week)
    available_monday      BIT(1)     NOT NULL DEFAULT b'1',
    available_tuesday     BIT(1)     NOT NULL DEFAULT b'1',
    available_wednesday   BIT(1)     NOT NULL DEFAULT b'1',
    available_thursday    BIT(1)     NOT NULL DEFAULT b'1',
    available_friday      BIT(1)     NOT NULL DEFAULT b'1',
    available_saturday    BIT(1)     NOT NULL DEFAULT b'1',
    available_sunday      BIT(1)     NOT NULL DEFAULT b'1',
    -- Notifications (master switch + categories)
    notifications_enabled BIT(1)     NOT NULL DEFAULT b'1',
    notify_event_activity BIT(1)     NOT NULL DEFAULT b'1',
    notify_event_messages BIT(1)     NOT NULL DEFAULT b'1',
    notify_documents      BIT(1)     NOT NULL DEFAULT b'1',
    notify_deadlines      BIT(1)     NOT NULL DEFAULT b'1',
    notify_nearby_events  BIT(1)     NOT NULL DEFAULT b'1',
    notify_judgments      BIT(1)     NOT NULL DEFAULT b'1',
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;
