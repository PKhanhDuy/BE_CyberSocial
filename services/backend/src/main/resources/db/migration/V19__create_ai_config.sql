create table ai_config (
    id int primary key default 1,
    analysis_enabled boolean not null default true,
    tier_thresholds varchar(100) not null default '5,15,30',
    debounce_minutes int not null default 3,
    fake_threshold_percent int not null default 59,
    updated_by uuid references users(id) on delete set null,
    updated_at timestamptz not null default now(),
    constraint ai_config_singleton check (id = 1)
);

insert into ai_config (id) values (1) on conflict (id) do nothing;
