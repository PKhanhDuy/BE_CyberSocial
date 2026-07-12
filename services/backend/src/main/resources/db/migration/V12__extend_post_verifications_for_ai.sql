alter table post_verifications
    add column fake_probability numeric(7, 6),
    add column label varchar(10),
    add column risk_level varchar(10),
    add column threshold numeric(7, 6),
    add column mode varchar(30),
    add column analysis_tier integer not null default 0,
    add column interaction_count_at_analysis integer,
    add column last_analyzed_at timestamptz;

create index idx_post_verifications_last_analyzed_at on post_verifications (last_analyzed_at);
