create table music_tracks (
    id uuid primary key default gen_random_uuid(),
    title varchar(180) not null,
    artist varchar(180) not null,
    audio_url varchar(2048) not null,
    cover_url varchar(2048),
    duration_seconds integer not null,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_music_tracks_duration_positive check (duration_seconds > 0)
);

create table stories (
    id uuid primary key default gen_random_uuid(),
    author_id uuid not null references users(id) on delete cascade,
    caption text,
    visibility varchar(20) not null default 'FRIENDS',
    music_track_id uuid references music_tracks(id) on delete set null,
    music_start_ms integer,
    music_duration_ms integer,
    expires_at timestamptz not null default (now() + interval '24 hours'),
    archived_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_stories_visibility check (visibility in ('PUBLIC', 'FRIENDS', 'PRIVATE')),
    constraint chk_stories_music_start_non_negative check (music_start_ms is null or music_start_ms >= 0),
    constraint chk_stories_music_duration_positive check (music_duration_ms is null or music_duration_ms > 0)
);

create table story_media (
    id uuid primary key default gen_random_uuid(),
    story_id uuid not null unique references stories(id) on delete cascade,
    media_type varchar(20) not null,
    media_url varchar(2048) not null,
    thumbnail_url varchar(2048),
    width integer,
    height integer,
    duration_ms integer,
    created_at timestamptz not null default now(),
    constraint chk_story_media_type check (media_type in ('IMAGE', 'VIDEO')),
    constraint chk_story_media_width_positive check (width is null or width > 0),
    constraint chk_story_media_height_positive check (height is null or height > 0),
    constraint chk_story_media_duration_positive check (duration_ms is null or duration_ms > 0)
);

create table story_views (
    id uuid primary key default gen_random_uuid(),
    story_id uuid not null references stories(id) on delete cascade,
    viewer_id uuid not null references users(id) on delete cascade,
    viewed_at timestamptz not null default now(),
    unique (story_id, viewer_id)
);

create table story_reactions (
    id uuid primary key default gen_random_uuid(),
    story_id uuid not null references stories(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    reaction_type varchar(40) not null,
    created_at timestamptz not null default now(),
    unique (story_id, user_id)
);

create index idx_music_tracks_active_title on music_tracks (is_active, title);
create index idx_stories_author_created_at on stories (author_id, created_at desc);
create index idx_stories_expires_at on stories (expires_at);
create index idx_stories_active_created_at on stories (created_at desc) where archived_at is null;
create index idx_story_media_story on story_media (story_id);
create index idx_story_views_viewer_viewed_at on story_views (viewer_id, viewed_at desc);
create index idx_story_views_story_viewed_at on story_views (story_id, viewed_at desc);
create index idx_story_reactions_story_created_at on story_reactions (story_id, created_at desc);
