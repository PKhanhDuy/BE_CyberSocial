insert into post_verifications (id, post_id, verification_status, analysis_tier, created_at, updated_at)
select gen_random_uuid(), post.id, 'PENDING', 0, post.created_at, now()
from posts post
where post.is_synthetic = false
  and not exists (
      select 1
      from post_verifications verification
      where verification.post_id = post.id
  );
