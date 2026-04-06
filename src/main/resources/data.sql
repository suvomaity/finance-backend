-- Seed Data for Finance Backend
-- Passwords: admin123, analyst123, viewer123

INSERT INTO users (id, name, email, password, role, status, created_at, updated_at)
SELECT '11111111-1111-1111-1111-111111111111', 'Admin User', 'admin@zorvyn.com',
       '$2a$10$N2Xs1ofEzm625LrBp.7mBenwoibTS/k26iZCUTgzuMjAAlgeHcKcm',
       'ADMIN', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@zorvyn.com');

INSERT INTO users (id, name, email, password, role, status, created_at, updated_at)
SELECT '22222222-2222-2222-2222-222222222222', 'Analyst User', 'analyst@zorvyn.com',
       '$2a$10$UNroSS9DnX8pK87JhGCnquQD27tXdG0i0.RiMWPQbR8Y9tJ.fvF0.',
       'ANALYST', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'analyst@zorvyn.com');

INSERT INTO users (id, name, email, password, role, status, created_at, updated_at)
SELECT '33333333-3333-3333-3333-333333333333', 'Viewer User', 'viewer@zorvyn.com',
       '$2a$10$CLw4QD6SLe/OjGUJsiHP8ePzTrvHYiZJ2pL4JR.AopjF00zP1lTVm',
       'VIEWER', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'viewer@zorvyn.com');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000001-0000-0000-0000-000000000001', 75000.00, 'INCOME', 'Salary', '2026-01-05', 'January salary credit', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000001-0000-0000-0000-000000000001');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000002-0000-0000-0000-000000000002', 12000.00, 'EXPENSE', 'Rent', '2026-01-07', 'Office rent for January', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000002-0000-0000-0000-000000000002');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000003-0000-0000-0000-000000000003', 2500.00, 'EXPENSE', 'Utilities', '2026-01-10', 'Electricity and internet bill', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000003-0000-0000-0000-000000000003');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000004-0000-0000-0000-000000000004', 15000.00, 'INCOME', 'Freelance', '2026-01-15', 'Freelance project payment', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000004-0000-0000-0000-000000000004');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000005-0000-0000-0000-000000000005', 3200.00, 'EXPENSE', 'Food', '2026-01-20', 'Team lunch and groceries', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000005-0000-0000-0000-000000000005');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000006-0000-0000-0000-000000000006', 75000.00, 'INCOME', 'Salary', '2026-02-05', 'February salary credit', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000006-0000-0000-0000-000000000006');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000007-0000-0000-0000-000000000007', 12000.00, 'EXPENSE', 'Rent', '2026-02-07', 'Office rent for February', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000007-0000-0000-0000-000000000007');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000008-0000-0000-0000-000000000008', 8500.00, 'EXPENSE', 'Marketing', '2026-02-12', 'Google Ads campaign', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000008-0000-0000-0000-000000000008');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000009-0000-0000-0000-000000000009', 4500.00, 'EXPENSE', 'Travel', '2026-02-18', 'Client meeting travel expenses', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000009-0000-0000-0000-000000000009');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000010-0000-0000-0000-000000000010', 20000.00, 'INCOME', 'Freelance', '2026-02-22', 'API integration project', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000010-0000-0000-0000-000000000010');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000011-0000-0000-0000-000000000011', 75000.00, 'INCOME', 'Salary', '2026-03-05', 'March salary credit', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000011-0000-0000-0000-000000000011');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000012-0000-0000-0000-000000000012', 12000.00, 'EXPENSE', 'Rent', '2026-03-07', 'Office rent for March', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000012-0000-0000-0000-000000000012');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000013-0000-0000-0000-000000000013', 6000.00, 'EXPENSE', 'Software', '2026-03-10', 'Annual SaaS subscriptions', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000013-0000-0000-0000-000000000013');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000014-0000-0000-0000-000000000014', 1800.00, 'EXPENSE', 'Food', '2026-03-14', 'Team dinner', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000014-0000-0000-0000-000000000014');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000015-0000-0000-0000-000000000015', 2800.00, 'EXPENSE', 'Utilities', '2026-03-18', 'Phone and cloud hosting bills', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000015-0000-0000-0000-000000000015');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000016-0000-0000-0000-000000000016', 30000.00, 'INCOME', 'Consulting', '2026-03-22', 'FinTech consulting engagement', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000016-0000-0000-0000-000000000016');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000017-0000-0000-0000-000000000017', 75000.00, 'INCOME', 'Salary', '2026-04-05', 'April salary credit', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000017-0000-0000-0000-000000000017');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000018-0000-0000-0000-000000000018', 12000.00, 'EXPENSE', 'Rent', '2026-04-07', 'Office rent for April', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000018-0000-0000-0000-000000000018');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000019-0000-0000-0000-000000000019', 5000.00, 'EXPENSE', 'Marketing', '2026-04-10', 'Social media promotions', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000019-0000-0000-0000-000000000019');

INSERT INTO transactions (id, amount, type, category, date, description, created_by, is_deleted, created_at, updated_at)
SELECT 'a0000020-0000-0000-0000-000000000020', 10000.00, 'INCOME', 'Freelance', '2026-04-01', 'Mobile app backend project', '11111111-1111-1111-1111-111111111111', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 'a0000020-0000-0000-0000-000000000020');
