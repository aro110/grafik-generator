ALTER TABLE schedule_ai_edits
    ADD COLUMN source_group_id BIGINT REFERENCES generation_run_groups(id) ON DELETE CASCADE,
    ADD COLUMN accepted_group_id BIGINT REFERENCES generation_run_groups(id) ON DELETE SET NULL,
    ADD COLUMN accepted_schedule_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN proposed_schedules JSONB;

CREATE INDEX idx_schedule_ai_edits_source_group_id ON schedule_ai_edits(source_group_id);
