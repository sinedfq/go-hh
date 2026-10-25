-- +goose Up
CREATE TABLE company_subscriptions (
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id INTEGER NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, company_id)
);
CREATE INDEX company_subscriptions_company ON company_subscriptions(company_id);
CREATE TABLE social_posts (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id INTEGER REFERENCES companies(id) ON DELETE CASCADE,
    body TEXT NOT NULL CHECK (char_length(body)<=5000),
    media_url TEXT NOT NULL DEFAULT '',
    media_type TEXT NOT NULL DEFAULT '' CHECK (media_type IN ('','image','video')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (length(trim(body))>0 OR media_url<>'')
);
CREATE INDEX social_posts_company ON social_posts(company_id,id DESC);
CREATE INDEX social_posts_user ON social_posts(user_id,id DESC);
CREATE TABLE social_likes (
    post_id BIGINT NOT NULL REFERENCES social_posts(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY(post_id,user_id)
);
CREATE TABLE social_comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES social_posts(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body TEXT NOT NULL CHECK (char_length(body) BETWEEN 1 AND 2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX social_comments_post ON social_comments(post_id,id DESC);
CREATE TABLE company_stories (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    caption TEXT NOT NULL DEFAULT '' CHECK (char_length(caption)<=500),
    media_url TEXT NOT NULL,
    media_type TEXT NOT NULL CHECK (media_type IN ('image','video')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL DEFAULT NOW()+INTERVAL '24 hours',
    CHECK (expires_at=created_at+INTERVAL '24 hours')
);
CREATE INDEX company_stories_company ON company_stories(company_id,expires_at);
CREATE INDEX company_stories_expiry ON company_stories(expires_at);

-- +goose Down
DROP TABLE company_stories;
DROP TABLE social_comments;
DROP TABLE social_likes;
DROP TABLE social_posts;
DROP TABLE company_subscriptions;
