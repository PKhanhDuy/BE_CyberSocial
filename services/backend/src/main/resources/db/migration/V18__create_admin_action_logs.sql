create table admin_action_logs (
    id uuid primary key default gen_random_uuid(),
    admin_user_id uuid references users(id) on delete set null,
    action_type varchar(40) not null,
    target_type varchar(20) not null,
    target_id uuid,
    reason varchar(500),
    note varchar(500),
    created_at timestamptz not null default now()
);

create index idx_admin_action_logs_admin on admin_action_logs (admin_user_id);
create index idx_admin_action_logs_target on admin_action_logs (target_type, target_id);
create index idx_admin_action_logs_created_at on admin_action_logs (created_at desc);
