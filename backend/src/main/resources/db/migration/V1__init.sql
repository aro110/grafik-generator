CREATE TABLE sections (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    surname VARCHAR(100) NOT NULL,
    section_id BIGINT NOT NULL REFERENCES sections(id) ON DELETE RESTRICT,
    total_hours INTEGER NOT NULL,
    total_days INTEGER NOT NULL,
    days_off JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_employees_section_id ON employees(section_id);

CREATE TABLE schedule_configs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED')),
    version INTEGER NOT NULL DEFAULT 1,
    year_month DATE NOT NULL,
    store_hours JSONB NOT NULL,
    staffing_targets JSONB NOT NULL,
    calendar JSONB NOT NULL,
    shift_rules JSONB NOT NULL,
    ga_parameters JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_schedule_configs_status ON schedule_configs(status);
CREATE INDEX idx_schedule_configs_year_month ON schedule_configs(year_month);

CREATE TABLE generation_runs (
    id BIGSERIAL PRIMARY KEY,
    config_id BIGINT NOT NULL REFERENCES schedule_configs(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED')),
    seed BIGINT NOT NULL,
    progress INTEGER NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_generation_runs_config_id ON generation_runs(config_id);
CREATE INDEX idx_generation_runs_status ON generation_runs(status);

CREATE TABLE schedules (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES generation_runs(id) ON DELETE CASCADE,
    fitness DOUBLE PRECISION NOT NULL,
    genes JSONB NOT NULL,
    shift_starts JSONB,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_schedules_run_id ON schedules(run_id);
CREATE INDEX idx_schedules_published ON schedules(published);
