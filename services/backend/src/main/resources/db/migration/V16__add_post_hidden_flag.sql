alter table posts
    add column hidden boolean not null default false;

alter table posts
    add column hidden_at timestamptz;

create index idx_posts_hidden on posts (hidden) where hidden = true;
