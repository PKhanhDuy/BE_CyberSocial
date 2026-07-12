alter table post_shares
    add column parent_share_id uuid references post_shares(id) on delete set null,
    add column repost_post_id uuid references posts(id) on delete set null;

create index idx_post_shares_parent_share_id on post_shares (parent_share_id);
create index idx_post_shares_repost_post_id on post_shares (repost_post_id);
