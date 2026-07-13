alter table post_verifications add column public_label boolean not null default false;
alter table post_verifications add column admin_decision varchar(20);
alter table post_verifications add column admin_note varchar(500);
alter table post_verifications add column reviewed_at timestamptz;
alter table post_verifications add column reviewed_by uuid references users(id) on delete set null;

create index idx_post_verifications_public_label on post_verifications (public_label) where public_label = true;
