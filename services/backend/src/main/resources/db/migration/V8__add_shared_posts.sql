alter table posts
    add column shared_post_id uuid references posts(id) on delete set null;

create index idx_posts_shared_post_created_at on posts (shared_post_id, created_at desc);
