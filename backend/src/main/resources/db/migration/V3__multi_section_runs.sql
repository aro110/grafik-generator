CREATE TABLE generation_run_groups (
    id          BIGSERIAL PRIMARY KEY,
    config_id   BIGINT NOT NULL REFERENCES schedule_configs(id) ON DELETE CASCADE,
    seed        BIGINT NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'PARTIAL', 'FAILED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMPTZ
);

CREATE INDEX idx_run_groups_config_id ON generation_run_groups(config_id);

ALTER TABLE generation_runs
    ADD COLUMN group_id BIGINT REFERENCES generation_run_groups(id) ON DELETE CASCADE;

CREATE INDEX idx_generation_runs_group_id ON generation_runs(group_id);
