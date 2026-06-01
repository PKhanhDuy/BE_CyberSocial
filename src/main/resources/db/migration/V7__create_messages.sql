create table message_conversations (
    id uuid primary key default gen_random_uuid(),
    user_one_id uuid not null references users(id) on delete cascade,
    user_two_id uuid not null references users(id) on delete cascade,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_message_conversations_not_self check (user_one_id <> user_two_id),
    unique (user_one_id, user_two_id)
);

create table messages (
    id uuid primary key default gen_random_uuid(),
    conversation_id uuid not null references message_conversations(id) on delete cascade,
    sender_id uuid not null references users(id) on delete cascade,
    message_type varchar(20) not null,
    content text,
    media_url varchar(2048),
    link_url varchar(2048),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_messages_type check (message_type in ('TEXT', 'IMAGE', 'VIDEO', 'LINK')),
    constraint chk_messages_payload check (
        (message_type = 'TEXT' and content is not null and length(trim(content)) > 0)
        or (message_type in ('IMAGE', 'VIDEO') and media_url is not null and length(trim(media_url)) > 0)
        or (message_type = 'LINK' and link_url is not null and length(trim(link_url)) > 0)
    )
);

create table message_reactions (
    id uuid primary key default gen_random_uuid(),
    message_id uuid not null references messages(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    emoji varchar(40) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (message_id, user_id)
);

create index idx_message_conversations_user_one_updated_at on message_conversations (user_one_id, updated_at desc);
create index idx_message_conversations_user_two_updated_at on message_conversations (user_two_id, updated_at desc);
create index idx_messages_conversation_created_at on messages (conversation_id, created_at desc);
create index idx_messages_sender_created_at on messages (sender_id, created_at desc);
create index idx_message_reactions_message_created_at on message_reactions (message_id, created_at desc);
