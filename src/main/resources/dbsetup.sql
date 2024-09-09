CREATE TABLE IF NOT EXISTS global_pool_cooldowns
(
    pool_id VARCHAR(16)  NOT NULL,
    time_available BIGINT NOT NULL,
    PRIMARY KEY (pool_id)
);

CREATE TABLE IF NOT EXISTS global_entry_cooldowns
(
    pool_id VARCHAR(16)  NOT NULL,
    entry_id VARCHAR(16) NOT NULL,
    time_available BIGINT NOT NULL,
    PRIMARY KEY (pool_id, entry_id)
);

CREATE TABLE IF NOT EXISTS player_pool_cooldowns
(
    uuid CHAR(36)        NOT NULL,
    pool_id VARCHAR(16)  NOT NULL,
    time_available BIGINT NOT NULL,
    PRIMARY KEY (uuid, pool_id)
);

CREATE TABLE IF NOT EXISTS player_entry_cooldowns
(
    uuid CHAR(36)        NOT NULL,
    pool_id VARCHAR(16)  NOT NULL,
    entry_id VARCHAR(16) NOT NULL,
    time_available BIGINT NOT NULL,
    PRIMARY KEY (uuid, pool_id, entry_id)
);