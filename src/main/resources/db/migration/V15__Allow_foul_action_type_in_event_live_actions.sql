ALTER TABLE event_live_actions
    DROP CONSTRAINT IF EXISTS event_live_actions_action_type_check;

ALTER TABLE event_live_actions
    ADD CONSTRAINT event_live_actions_action_type_check
    CHECK (action_type IN ('SCORE', 'FOUL', 'SUB_IN', 'SUB_OUT'));
