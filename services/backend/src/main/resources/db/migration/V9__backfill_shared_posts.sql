insert into posts (author_id, content, media, visibility, shared_post_id, created_at, updated_at)
select share.user_id, coalesce(share.content, ''), '[]'::jsonb, 'PUBLIC', share.post_id, share.created_at, share.created_at
from post_shares share
where not exists (
    select 1
    from posts post
    where post.author_id = share.user_id
      and post.shared_post_id = share.post_id
      and post.created_at = share.created_at
);
