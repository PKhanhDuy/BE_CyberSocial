create table friendships (
    id uuid primary key default gen_random_uuid(),
    requester_id uuid not null references users(id) on delete cascade,
    addressee_id uuid not null references users(id) on delete cascade,
    status varchar(20) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_friendships_not_self check (requester_id <> addressee_id),
    unique (requester_id, addressee_id)
);

create index idx_friendships_requester_status on friendships (requester_id, status);
create index idx_friendships_addressee_status on friendships (addressee_id, status);
