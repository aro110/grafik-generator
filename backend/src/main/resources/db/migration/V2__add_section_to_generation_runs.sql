ALTER TABLE generation_runs
    ADD COLUMN section_id BIGINT REFERENCES sections(id) ON DELETE RESTRICT;

CREATE INDEX idx_generation_runs_section_id ON generation_runs(section_id);
