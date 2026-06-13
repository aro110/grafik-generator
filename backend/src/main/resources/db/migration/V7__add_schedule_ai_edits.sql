ALTER TABLE schedules ADD COLUMN protected_overrides JSONB;

CREATE TABLE schedule_ai_edits (
    id BIGSERIAL PRIMARY KEY,
    source_schedule_id BIGINT NOT NULL REFERENCES schedules(id) ON DELETE CASCADE,
    accepted_schedule_id BIGINT REFERENCES schedules(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PROPOSED', 'APPLIED', 'REJECTED', 'FAILED')),
    instruction TEXT NOT NULL,
    model VARCHAR(120) NOT NULL,
    allow_protected_date_changes BOOLEAN NOT NULL DEFAULT FALSE,
    changes JSONB NOT NULL DEFAULT '[]'::jsonb,
    diff JSONB NOT NULL DEFAULT '[]'::jsonb,
    warnings JSONB NOT NULL DEFAULT '[]'::jsonb,
    errors JSONB NOT NULL DEFAULT '[]'::jsonb,
    proposed_genes JSONB,
    proposed_shift_starts JSONB,
    protected_overrides JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    applied_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_schedule_ai_edits_source_schedule_id ON schedule_ai_edits(source_schedule_id);
CREATE INDEX idx_schedule_ai_edits_status ON schedule_ai_edits(status);
