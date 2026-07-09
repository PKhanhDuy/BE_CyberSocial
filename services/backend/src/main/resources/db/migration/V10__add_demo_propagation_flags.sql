alter table users
    add column if not exists is_demo_user boolean not null default false;

alter table posts
    add column if not exists is_synthetic boolean not null default false;

alter table post_likes
    add column if not exists is_synthetic boolean not null default false;

alter table post_shares
    add column if not exists is_synthetic boolean not null default false;

alter table post_comments
    add column if not exists is_synthetic boolean not null default false;

create index if not exists idx_users_demo_user on users (is_demo_user);
create index if not exists idx_posts_synthetic_created_at on posts (is_synthetic, created_at desc);
create index if not exists idx_post_likes_synthetic_created_at on post_likes (is_synthetic, created_at desc);
create index if not exists idx_post_shares_synthetic_created_at on post_shares (is_synthetic, created_at desc);
create index if not exists idx_post_comments_synthetic_created_at on post_comments (is_synthetic, created_at desc);
