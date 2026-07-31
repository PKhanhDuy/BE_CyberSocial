alter table post_verifications
    add column headline text,
    add column narrative text,
    add column context_hints jsonb;
