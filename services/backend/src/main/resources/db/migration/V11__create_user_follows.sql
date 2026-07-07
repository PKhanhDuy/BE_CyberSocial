create table user_follows (
    id uuid primary key default gen_random_uuid(),
    follower_id uuid not null references users(id) on delete cascade,
    following_id uuid not null references users(id) on delete cascade,
    created_at timestamptz not null default now(),
    constraint chk_user_follows_not_self check (follower_id <> following_id),
    unique (follower_id, following_id)
);

create index idx_user_follows_follower on user_follows (follower_id);
create index idx_user_follows_following on user_follows (following_id);
