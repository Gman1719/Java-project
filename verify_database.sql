-- DATABASE VERIFICATION SCRIPT
-- Run this script to verify all tables have data and are ready for testing

USE payroll_system;

-- ============================================
-- 1. CHECK DATABASE STRUCTURE
-- ============================================
SHOW TABLES;

-- ============================================
-- 2. VERIFY EMPLOYEE DATA EXISTS
-- ============================================
SELECT 'Checking Employees...' AS status;
SELECT COUNT(*) AS employee_count FROM employees WHERE status = 'Active';
SELECT emp_id, user_id, dept_id, salary, status FROM employees LIMIT 5;

-- ============================================
-- 3. VERIFY USER ACCOUNTS FOR LOGIN
-- ============================================
SELECT 'Checking User Accounts...' AS status;
SELECT u.user_id, u.username, u.first_name, u.last_name, r.role_name 
FROM users u 
JOIN roles r ON u.role_id = r.role_id 
WHERE u.username IN ('employee1', 'employee2', 'payroll1');

-- ============================================
-- 4. CHECK ATTENDANCE DATA
-- ============================================
SELECT 'Checking Attendance Records...' AS status;
SELECT COUNT(*) AS attendance_count FROM attendance;
SELECT * FROM attendance ORDER BY date DESC LIMIT 5;

-- ============================================
-- 5. CHECK LEAVE REQUESTS
-- ============================================
SELECT 'Checking Leave Requests...' AS status;
SELECT COUNT(*) AS leave_requests_count FROM leave_requests;
SELECT lr.leave_id, lr.emp_id, lr.leave_type, lr.status, lr.requested_on 
FROM leave_requests lr LIMIT 5;

-- ============================================
-- 6. CHECK PAYROLL DATA
-- ============================================
SELECT 'Checking Payroll Records...' AS status;
SELECT COUNT(*) AS payroll_count FROM payroll;
SELECT p.payroll_id, p.emp_id, p.month, p.year, p.base_salary, p.net_salary, p.status 
FROM payroll p LIMIT 5;

-- ============================================
-- 7. CHECK OTHER REQUEST TYPES
-- ============================================
SELECT 'Checking Salary Advance Requests...' AS status;
SELECT COUNT(*) AS salary_advance_count FROM salary_advance_requests;
SELECT * FROM salary_advance_requests LIMIT 3;

SELECT 'Checking Bank Requests...' AS status;
SELECT COUNT(*) AS bank_requests_count FROM bank_requests;
SELECT * FROM bank_requests LIMIT 3;

SELECT 'Checking Reimbursements...' AS status;
SELECT COUNT(*) AS reimbursements_count FROM reimbursements;
SELECT * FROM reimbursements LIMIT 3;

-- ============================================
-- 8. CHECK NOTIFICATIONS
-- ============================================
SELECT 'Checking Notifications...' AS status;
SELECT COUNT(*) AS notifications_count FROM notifications;
SELECT n.notif_id, n.user_id, n.title, n.status, n.created_at 
FROM notifications n LIMIT 5;

-- ============================================
-- 9. CHECK CALENDAR EVENTS
-- ============================================
SELECT 'Checking Calendar Events...' AS status;
SELECT COUNT(*) AS calendar_events_count FROM calendar_events;
SELECT * FROM calendar_events LIMIT 5;

-- ============================================
-- 10. CHECK SETTINGS
-- ============================================
SELECT 'Checking Global Settings...' AS status;
SELECT * FROM settings;

-- ============================================
-- 11. VERIFY COMPLETE DATA FOR EMPLOYEE ID 3
-- ============================================
SELECT 'Verifying Complete Data for Employee ID 3...' AS status;

-- User Info
SELECT 'User Info:' AS data_type;
SELECT u.user_id, u.username, u.first_name, u.last_name, u.email 
FROM users u 
JOIN employees e ON u.user_id = e.user_id 
WHERE e.emp_id = 3;

-- Attendance
SELECT 'Attendance Records:' AS data_type;
SELECT COUNT(*) AS count FROM attendance WHERE emp_id = 3;

-- Leave Requests
SELECT 'Leave Requests:' AS data_type;
SELECT COUNT(*) AS count FROM leave_requests WHERE emp_id = 3;

-- Payroll
SELECT 'Payroll Records:' AS data_type;
SELECT COUNT(*) AS count FROM payroll WHERE emp_id = 3;

-- Salary Advance Requests
SELECT 'Salary Advance Requests:' AS data_type;
SELECT COUNT(*) AS count FROM salary_advance_requests WHERE emp_id = 3;

-- Bank Requests
SELECT 'Bank Requests:' AS data_type;
SELECT COUNT(*) AS count FROM bank_requests WHERE emp_id = 3;

-- Reimbursements
SELECT 'Reimbursement Requests:' AS data_type;
SELECT COUNT(*) AS count FROM reimbursements WHERE emp_id = 3;

-- Notifications
SELECT 'Notifications:' AS data_type;
SELECT COUNT(*) AS count FROM notifications WHERE user_id = 3;

-- ============================================
-- 12. INSERT SAMPLE DATA IF MISSING
-- ============================================

-- If no attendance data exists, add some
INSERT IGNORE INTO attendance (emp_id, date, time_in, time_out, status, remarks) VALUES
(3, '2025-12-20', '08:00:00', '17:00:00', 'Present', 'On time'),
(3, '2025-12-21', '08:15:00', '17:05:00', 'Present', 'Slightly late'),
(3, '2025-12-22', '08:00:00', '17:00:00', 'Present', 'On time'),
(3, '2025-12-23', NULL, NULL, 'Leave', 'Sick leave'),
(3, '2025-12-24', '08:00:00', '17:00:00', 'Present', 'On time');

-- If no leave requests exist, add some
INSERT IGNORE INTO leave_requests (leave_id, emp_id, leave_type, start_date, end_date, reason, status, requested_on) VALUES
(1, 3, 'Sick Leave', '2025-12-23', '2025-12-23', 'Feeling unwell', 'Approved', NOW()),
(2, 3, 'Annual Leave', '2026-01-10', '2026-01-15', 'Family vacation', 'Pending', NOW());

-- If no payroll records exist, add some
INSERT IGNORE INTO payroll (payroll_id, emp_id, month, year, base_salary, allowances, deductions, tax, net_salary, status, generated_on) VALUES
(1, 3, 'December', 2025, 5000.00, 500.00, 200.00, 750.00, 4550.00, 'Approved', NOW()),
(2, 3, 'November', 2025, 5000.00, 500.00, 200.00, 750.00, 4550.00, 'Approved', NOW());

-- If no notifications exist, add some
INSERT IGNORE INTO notifications (notif_id, user_id, title, message, status, created_at) VALUES
(1, 3, 'Leave Approved', 'Your sick leave request has been approved', 'Unread', NOW()),
(2, 3, 'Payroll Generated', 'Your December payslip is ready', 'Unread', NOW()),
(3, 3, 'Company Event', 'Year-end party on December 31st', 'Unread', NOW());

-- If no salary advance requests exist, add sample
INSERT IGNORE INTO salary_advance_requests (adv_id, emp_id, amount, reason, status, requested_on) VALUES
(1, 3, 2000.00, 'Emergency medical expenses', 'Pending', NOW());

-- If no bank requests exist, add sample
INSERT IGNORE INTO bank_requests (bank_req_id, emp_id, old_account, new_account, status, requested_on) VALUES
(1, 3, '1234567890', '0987654321', 'Pending', NOW());

-- If no reimbursements exist, add sample
INSERT IGNORE INTO reimbursements (reimb_id, emp_id, amount, reason, status, requested_on) VALUES
(1, 3, 150.00, 'Travel expenses for client meeting', 'Pending', NOW());

-- If no calendar events exist, add some
INSERT IGNORE INTO calendar_events (event_id, title, description, event_date, emp_id, created_at) VALUES
(1, 'Company Holiday', 'New Year Day', '2026-01-01', NULL, NOW()),
(2, 'Team Meeting', 'Monthly team sync', '2026-01-05', 3, NOW());

-- If no settings exist, add default
INSERT IGNORE INTO settings (setting_id, tax_rate, social_rate, currency_symbol) VALUES
(1, 15.00, 5.00, '$');

-- ============================================
-- 13. FINAL VERIFICATION
-- ============================================
SELECT 'FINAL VERIFICATION - All Data Counts' AS status;

SELECT 
    'employees' AS table_name, 
    COUNT(*) AS record_count,
    CASE WHEN COUNT(*) > 0 THEN '✅ HAS DATA' ELSE '❌ EMPTY' END AS status
FROM employees WHERE status = 'Active'
UNION ALL
SELECT 
    'attendance' AS table_name, 
    COUNT(*) AS record_count,
    CASE WHEN COUNT(*) > 0 THEN '✅ HAS DATA' ELSE '❌ EMPTY' END AS status
FROM attendance
UNION ALL
SELECT 
    'leave_requests' AS table_name, 
    COUNT(*) AS record_count,
    CASE WHEN COUNT(*) > 0 THEN '✅ HAS DATA' ELSE '❌ EMPTY' END AS status
FROM leave_requests
UNION ALL
SELECT 
    'payroll' AS table_name, 
    COUNT(*) AS record_count,
    CASE WHEN COUNT(*) > 0 THEN '✅ HAS DATA' ELSE '❌ EMPTY' END AS status
FROM payroll
UNION ALL
SELECT 
    'notifications' AS table_name, 
    COUNT(*) AS record_count,
    CASE WHEN COUNT(*) > 0 THEN '✅ HAS DATA' ELSE '❌ EMPTY' END AS status
FROM notifications
UNION ALL
SELECT 
    'salary_advance_requests' AS table_name, 
    COUNT(*) AS record_count,
    CASE WHEN COUNT(*) > 0 THEN '✅ HAS DATA' ELSE '❌ EMPTY' END AS status
FROM salary_advance_requests
UNION ALL
SELECT 
    'bank_requests' AS table_name, 
    COUNT(*) AS record_count,
    CASE WHEN COUNT(*) > 0 THEN '✅ HAS DATA' ELSE '❌ EMPTY' END AS status
FROM bank_requests
UNION ALL
SELECT 
    'reimbursements' AS table_name, 
    COUNT(*) AS record_count,
    CASE WHEN COUNT(*) > 0 THEN '✅ HAS DATA' ELSE '❌ EMPTY' END AS status
FROM reimbursements
UNION ALL
SELECT 
    'calendar_events' AS table_name, 
    COUNT(*) AS record_count,
    CASE WHEN COUNT(*) > 0 THEN '✅ HAS DATA' ELSE '❌ EMPTY' END AS status
FROM calendar_events
UNION ALL
SELECT 
    'settings' AS table_name, 
    COUNT(*) AS record_count,
    CASE WHEN COUNT(*) > 0 THEN '✅ HAS DATA' ELSE '❌ EMPTY' END AS status
FROM settings;

SELECT '========================================' AS result;
SELECT 'DATABASE VERIFICATION COMPLETE!' AS result;
SELECT 'All tables should show ✅ HAS DATA' AS result;
SELECT 'If any show ❌ EMPTY, sample data was inserted' AS result;
SELECT '========================================' AS result;
