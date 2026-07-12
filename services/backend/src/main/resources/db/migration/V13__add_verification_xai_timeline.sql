alter table post_verifications
    add column event_attributions jsonb,
    add column propagation_timeline jsonb;
