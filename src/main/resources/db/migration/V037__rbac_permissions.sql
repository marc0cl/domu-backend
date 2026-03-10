ALTER TABLE roles
    ADD COLUMN permissions_json TEXT NULL;

UPDATE roles SET permissions_json = '["ALL"]'
WHERE name = 'admin';

UPDATE roles SET permissions_json = '["VOTES_VIEW","DOCUMENTS_VIEW"]'
WHERE name = 'resident';

UPDATE roles SET permissions_json = '["VOTES_CREATE","VOTES_VIEW","DOCUMENTS_VIEW"]'
WHERE name = 'concierge';

UPDATE roles SET permissions_json = '["VOTES_VIEW","DOCUMENTS_VIEW"]'
WHERE name = 'staff';

UPDATE roles SET permissions_json = '["FINANCE_VIEW","VOTES_CREATE","VOTES_VIEW","DOCUMENTS_UPLOAD","DOCUMENTS_VIEW"]'
WHERE name = 'committee';
