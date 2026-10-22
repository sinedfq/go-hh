-- +goose Up
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS terms JSONB NOT NULL DEFAULT '{}';
ALTER TABLE companies ADD COLUMN IF NOT EXISTS profile JSONB NOT NULL DEFAULT '{}';
CREATE TABLE saved_searches (
 id BIGSERIAL PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 name TEXT NOT NULL, filters JSONB NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX saved_searches_user_idx ON saved_searches(user_id);
CREATE TABLE saved_search_matches (
 search_id BIGINT NOT NULL REFERENCES saved_searches(id) ON DELETE CASCADE,
 vacancy_id INTEGER NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
 PRIMARY KEY(search_id,vacancy_id)
);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS feedback TEXT NOT NULL DEFAULT '';
ALTER TABLE applications ADD COLUMN IF NOT EXISTS interview_at TIMESTAMPTZ;
CREATE TABLE application_events (
 id BIGSERIAL PRIMARY KEY, application_id INTEGER NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
 status TEXT NOT NULL, feedback TEXT NOT NULL DEFAULT '', interview_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX application_events_app_idx ON application_events(application_id,id);
INSERT INTO application_events(application_id,status,created_at) SELECT id,'new',created_at FROM applications;
INSERT INTO application_events(application_id,status,created_at) SELECT id,'viewed',viewed_at FROM applications WHERE viewed_at IS NOT NULL;
INSERT INTO application_events(application_id,status,created_at) SELECT id,status,NULL FROM applications WHERE status NOT IN ('new','viewed');
-- +goose StatementBegin
CREATE FUNCTION record_application_event() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='INSERT' THEN
  INSERT INTO application_events(application_id,status,feedback,interview_at) VALUES(NEW.id,NEW.status,NEW.feedback,NEW.interview_at);
 ELSIF (NEW.status,NEW.feedback,NEW.interview_at) IS DISTINCT FROM (OLD.status,OLD.feedback,OLD.interview_at) THEN
  INSERT INTO application_events(application_id,status,feedback,interview_at) VALUES(NEW.id,NEW.status,NEW.feedback,NEW.interview_at);
 END IF;
 RETURN NEW;
END $$;
-- +goose StatementEnd
CREATE TRIGGER application_history AFTER INSERT OR UPDATE OF status,feedback,interview_at ON applications FOR EACH ROW EXECUTE FUNCTION record_application_event();

-- +goose Down
DROP TRIGGER IF EXISTS application_history ON applications;
DROP FUNCTION IF EXISTS record_application_event();
DROP TABLE IF EXISTS application_events;
ALTER TABLE applications DROP COLUMN IF EXISTS feedback, DROP COLUMN IF EXISTS interview_at;
DROP TABLE IF EXISTS saved_search_matches;
DROP TABLE IF EXISTS saved_searches;
ALTER TABLE companies DROP COLUMN IF EXISTS profile;
ALTER TABLE vacancies DROP COLUMN IF EXISTS terms;
