create extension if not exists pgcrypto;

create table users (
    id uuid primary key default gen_random_uuid(),
    email varchar(320) not null unique,
    display_name varchar(120) not null,
    password_hash varchar(255) not null,
    role varchar(20) not null,
    theme_preference varchar(20) not null,
    enabled boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table user_profiles (
    user_id uuid primary key references users(id) on delete cascade,
    gender varchar(30),
    date_of_birth date,
    language varchar(80),
    nationality varchar(120),
    living_place varchar(180),
    bio text,
    interests text,
    current_job varchar(180),
    education_level varchar(120),
    school varchar(180),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table posts (
    id uuid primary key default gen_random_uuid(),
    author_id uuid not null references users(id) on delete cascade,
    content text not null,
    media jsonb,
    visibility varchar(20) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table post_verifications (
    id uuid primary key default gen_random_uuid(),
    post_id uuid not null unique references posts(id) on delete cascade,
    verification_status varchar(30) not null,
    confidence_score numeric(5,2),
    interpretation text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table post_likes (
    id uuid primary key default gen_random_uuid(),
    post_id uuid not null references posts(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    created_at timestamptz not null default now(),
    unique (post_id, user_id)
);

create table post_shares (
    id uuid primary key default gen_random_uuid(),
    post_id uuid not null references posts(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    content text,
    created_at timestamptz not null default now()
);

create table post_comments (
    id uuid primary key default gen_random_uuid(),
    post_id uuid not null references posts(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    content text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table notifications (
    id uuid primary key default gen_random_uuid(),
    recipient_id uuid not null references users(id) on delete cascade,
    type varchar(30) not null,
    title varchar(180) not null,
    message text not null,
    is_read boolean not null default false,
    read_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table graph_nodes (
    id uuid primary key default gen_random_uuid(),
    label varchar(120) not null,
    type varchar(30) not null,
    metadata jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table graph_edges (
    id uuid primary key default gen_random_uuid(),
    source_node_id uuid not null references graph_nodes(id) on delete cascade,
    target_node_id uuid not null references graph_nodes(id) on delete cascade,
    type varchar(30) not null,
    weight double precision not null default 1.0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table refresh_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    token_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_users_email on users (email);
create index idx_user_profiles_nationality on user_profiles (nationality);
create index idx_posts_author_created_at on posts (author_id, created_at desc);
create index idx_posts_created_at on posts (created_at desc);
create index idx_post_verifications_status on post_verifications (verification_status);
create index idx_post_likes_post_created_at on post_likes (post_id, created_at desc);
create index idx_post_likes_user_created_at on post_likes (user_id, created_at desc);
create index idx_post_shares_post_created_at on post_shares (post_id, created_at desc);
create index idx_post_shares_user_created_at on post_shares (user_id, created_at desc);
create index idx_post_comments_post_created_at on post_comments (post_id, created_at desc);
create index idx_post_comments_user_created_at on post_comments (user_id, created_at desc);
create index idx_notifications_recipient_created_at on notifications (recipient_id, created_at desc);
create index idx_notifications_recipient_read on notifications (recipient_id, is_read);
create index idx_graph_nodes_type on graph_nodes (type);
create index idx_graph_edges_source on graph_edges (source_node_id);
create index idx_graph_edges_target on graph_edges (target_node_id);
create index idx_refresh_tokens_user on refresh_tokens (user_id);
create index idx_refresh_tokens_expires_at on refresh_tokens (expires_at);
