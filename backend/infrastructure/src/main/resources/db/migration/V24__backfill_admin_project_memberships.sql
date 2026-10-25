-- Org ADMIN can already bypass project membership in the API, but the members
-- list and non-admin code paths expect a row. Projects created before creator
-- membership existed (or by a build that skipped it) have none. There is no
-- created_by column, so backfill every org ADMIN as OPERATOR on every project
-- they are missing from. Non-admin creators are not recoverable here.

INSERT INTO project_memberships (id, project_id, user_id, role, created_at, updated_at)
SELECT gen_random_uuid(), p.id, u.id, 'OPERATOR', NOW(), NOW()
FROM projects p
CROSS JOIN users u
WHERE u.role = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM project_memberships m
      WHERE m.project_id = p.id
        AND m.user_id = u.id
  );
