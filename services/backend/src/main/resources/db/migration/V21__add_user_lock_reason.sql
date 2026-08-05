alter table users
    add column if not exists lock_reason varchar(500);

-- Backfill lý do khóa gần nhất từ nhật ký admin (nếu có).
update users u
set lock_reason = sub.reason
from (
    select distinct on (target_id)
        target_id,
        reason
    from admin_action_logs
    where target_type = 'USER'
      and action_type = 'LOCK_USER'
      and reason is not null
      and btrim(reason) <> ''
    order by target_id, created_at desc
) sub
where u.id = sub.target_id
  and u.enabled = false
  and (u.lock_reason is null or btrim(u.lock_reason) = '');
