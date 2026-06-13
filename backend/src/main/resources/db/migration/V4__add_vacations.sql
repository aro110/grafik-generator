ALTER TABLE employees ADD COLUMN vacations JSONB NOT NULL DEFAULT '[]'::jsonb;
