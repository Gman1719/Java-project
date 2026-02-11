-- =====================================================
-- FIXED SQL SCRIPT: Full Payroll System Schema Setup
-- =====================================================

-- 1. DROP DATABASE IF EXISTS
DROP DATABASE IF EXISTS payroll_system;

-- 2. CREATE DATABASE
CREATE DATABASE payroll_system;
USE payroll_system;

-- 3. ROLES TABLE
CREATE TABLE roles (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL
);

-- Insert default roles
INSERT INTO roles (role_name) VALUES 
('Admin'), ('HR'), ('Employee'), ('Payroll Officer');

-- 4. DEPARTMENTS TABLE
CREATE TABLE departments (
    dept_id INT AUTO_INCREMENT PRIMARY KEY,
    dept_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Insert sample departments
INSERT INTO departments (dept_name, description) VALUES
('Management', 'Executive and senior leadership.'),
('HR', 'Manages employee relations and hiring.'),
('IT', 'Handles technology and software.'),
('Finance', 'Handles payroll and company accounts.');


-- 5. USERS TABLE
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    dept_id INT, 
    designation VARCHAR(50),
    date_of_joining DATE,
    status ENUM('Active','Inactive') DEFAULT 'Active',
    role_id INT, 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(role_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    FOREIGN KEY (dept_id) REFERENCES departments(dept_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- 6. EMPLOYEES TABLE
CREATE TABLE employees (
    emp_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    role_id INT, 
    gender ENUM('Male','Female','Other'),
    phone VARCHAR(20), 
    email VARCHAR(100), 
    dept_id INT, 
    position VARCHAR(100),
    salary DECIMAL(10,2),
    bank_account VARCHAR(50),
    date_joined DATE,
    address VARCHAR(255),
    profile_picture_path VARCHAR(255) NULL, 
    status ENUM('Active','Inactive') DEFAULT 'Active',
    FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(role_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    FOREIGN KEY (dept_id) REFERENCES departments(dept_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- 7. SETTINGS TABLE
CREATE TABLE settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(255),
    tax_rate DECIMAL(5,2) DEFAULT 5.00,
    working_hours INT DEFAULT 8
);

INSERT INTO settings (company_name) VALUES ('Payroll Management System');

-- 8. LEAVE_REQUESTS TABLE
CREATE TABLE leave_requests (
    leave_id INT AUTO_INCREMENT PRIMARY KEY,
    emp_id INT NOT NULL,
    leave_type VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days DECIMAL(4, 1) NOT NULL,
    reason TEXT,
    status ENUM('Pending','Approved','Rejected') DEFAULT 'Pending',
    requested_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (emp_id) REFERENCES employees(emp_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- 9. ATTENDANCE TABLE
CREATE TABLE attendance (
    attend_id INT AUTO_INCREMENT PRIMARY KEY,
    emp_id INT NOT NULL,
    date DATE NOT NULL,
    time_in TIME,
    time_out TIME,
    status ENUM('Present','Absent','Leave') DEFAULT 'Present',
    leave_id INT NULL,
    remarks VARCHAR(255) NULL, 
    FOREIGN KEY (emp_id) REFERENCES employees(emp_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (leave_id) REFERENCES leave_requests(leave_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    UNIQUE (emp_id, date)
);

-- 10. PAYROLL TABLE
CREATE TABLE payroll (
    payroll_id INT AUTO_INCREMENT PRIMARY KEY,
    emp_id INT NOT NULL,
    month VARCHAR(20) NOT NULL,
    year INT NOT NULL,
    base_salary DECIMAL(10,2) NOT NULL,
    allowances DECIMAL(10,2) DEFAULT 0,
    deductions DECIMAL(10,2) DEFAULT 0,
    tax DECIMAL(10,2) DEFAULT 0,
    net_salary DECIMAL(10,2) AS (base_salary + allowances - deductions - tax) STORED, 
    generated_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (emp_id) REFERENCES employees(emp_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    UNIQUE (emp_id, month, year)
);

-- 11. CALENDAR EVENTS TABLE
CREATE TABLE calendar_events (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    emp_id INT NULL,
    event_date DATE NOT NULL,
    event_time TIME,
    description VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (emp_id) REFERENCES employees(emp_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- 12. BANK REQUESTS TABLE
CREATE TABLE bank_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    emp_id INT NOT NULL,
    old_account VARCHAR(50),
    new_account VARCHAR(50),
    status ENUM('Pending','Approved','Rejected') DEFAULT 'Pending',
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (emp_id) REFERENCES employees(emp_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- 13. SALARY ADVANCE REQUESTS TABLE
CREATE TABLE salary_advance_requests (
    advance_id INT AUTO_INCREMENT PRIMARY KEY,
    emp_id INT NOT NULL,
    amount DECIMAL(10,2),
    reason TEXT,
    status ENUM('Pending','Approved','Rejected') DEFAULT 'Pending',
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (emp_id) REFERENCES employees(emp_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- 14. REIMBURSEMENTS TABLE
CREATE TABLE reimbursements (
    reimb_id INT AUTO_INCREMENT PRIMARY KEY,
    emp_id INT NOT NULL,
    amount DECIMAL(10,2),
    reason TEXT,
    status ENUM('Pending','Approved','Rejected') DEFAULT 'Pending',
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (emp_id) REFERENCES employees(emp_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- 15. NOTIFICATIONS TABLE
CREATE TABLE notifications (
    notify_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    message VARCHAR(255),
    status ENUM('Unread','Read') DEFAULT 'Unread',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);



-- 16. AUDIT LOG TABLE
CREATE TABLE audit_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    action VARCHAR(255),
    log_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- =====================================================
-- 17. SAMPLE DATA INSERTIONS
-- =====================================================

-- Insert initial admin user FIRST (to avoid foreign key issues)
INSERT INTO users (username, password, first_name, last_name, email, phone, designation, date_of_joining, status) VALUES
('admin', 'admin1234', 'Admin', 'User', 'admin@example.com', '0000000000', 'Administrator', '2024-01-01', 'Active');

-- Get role IDs AFTER they exist
SET @admin_role = (SELECT role_id FROM roles WHERE role_name = 'Admin');
SET @hr_role = (SELECT role_id FROM roles WHERE role_name = 'HR');
SET @emp_role = (SELECT role_id FROM roles WHERE role_name = 'Employee');
SET @payroll_role = (SELECT role_id FROM roles WHERE role_name = 'Payroll Officer');

-- Get department IDs
SET @mgmt_dept = (SELECT dept_id FROM departments WHERE dept_name = 'Management');
SET @hr_dept = (SELECT dept_id FROM departments WHERE dept_name = 'HR');
SET @it_dept = (SELECT dept_id FROM departments WHERE dept_name = 'IT');
SET @finance_dept = (SELECT dept_id FROM departments WHERE dept_name = 'Finance');

-- Update the admin user with proper role and dept
UPDATE users SET role_id = @admin_role, dept_id = @mgmt_dept WHERE username = 'admin';

-- Insert remaining Users
INSERT INTO users (username, password, role_id, first_name, last_name, email, phone, dept_id, designation, date_of_joining, status) VALUES
('hr1', 'hr123', @hr_role, 'John', 'Doe', 'hr1@example.com', '1111111111', @hr_dept, 'HR Manager', '2024-01-02', 'Active'),
('employee1', 'emp123', @emp_role, 'Alice', 'Smith', 'alice@example.com', '0912345679', @it_dept, 'Developer', '2024-01-05', 'Active'),
('employee2', 'emp123', @emp_role, 'Bob', 'Johnson', 'bob@example.com', '0912345680', @finance_dept, 'Accountant', '2024-02-01', 'Active'),
('employee3', 'emp123', @emp_role, 'Carol', 'Williams', 'carol@example.com', '0912345681', @it_dept, 'Tester', '2024-03-01', 'Active'),
('payroll1', 'pay123', @payroll_role, 'Peter', 'Pay', 'payroll@example.com', '0912345682', @finance_dept, 'Payroll Officer', '2024-04-01', 'Active');

-- Insert Employees
INSERT INTO employees (user_id, role_id, gender, phone, email, dept_id, position, salary, bank_account, date_joined, address, profile_picture_path) VALUES
(1, @admin_role, 'Male', '0000000000', 'admin@example.com', @mgmt_dept, 'Administrator', 50000.00, '0000000000', '2024-01-01', '1 Main HQ', NULL),
(2, @hr_role, 'Male', '1111111111', 'hr1@example.com', @hr_dept, 'HR Manager', 30000.00, '1111111111', '2024-01-02', '3 HR Office', NULL),
(3, @emp_role, 'Female', '0912345679', 'alice@example.com', @it_dept, 'Developer', 15000.00, '1234567890', '2024-01-05', '456 Tech Lane', NULL),
(4, @emp_role, 'Male', '0912345680', 'bob@example.com', @finance_dept, 'Accountant', 12000.00, '9876543210', '2024-02-01', '101 Finance Blvd', NULL),
(5, @emp_role, 'Female', '0912345681', 'carol@example.com', @it_dept, 'Tester', 10000.00, '1122334455', '2024-03-01', '789 QA Circle', NULL),
(6, @payroll_role, 'Male', '0912345682', 'payroll@example.com', @finance_dept, 'Payroll Officer', 20000.00, '5566778899', '2024-04-01', '202 Payroll St', NULL);

-- =====================================================
-- 18. ADDITIONAL SAMPLE DATA
-- =====================================================

-- Sample Leave Requests
INSERT INTO leave_requests (emp_id, leave_type, start_date, end_date, total_days, reason, status) VALUES
(6, 'Sick Leave', '2024-12-01', '2024-12-02', 2.0, 'Fever and cold', 'Approved'),
(4, 'Casual Leave', '2024-12-10', '2024-12-12', 3.0, 'Family event', 'Pending');

-- Sample Attendance Records
INSERT INTO attendance (emp_id, date, time_in, time_out, status, leave_id, remarks) VALUES
(3, '2024-12-01', '09:00:00', '17:00:00', 'Present', NULL, 'On time.'),
(4, '2024-12-01', '09:15:00', '17:05:00', 'Present', NULL, 'Slightly late in.'),
(5, '2024-12-01', '09:05:00', '16:55:00', 'Present', NULL, 'Left 5 mins early.'),
(6, '2024-12-01', NULL, NULL, 'Leave', 1, 'Sick leave approved.');

-- Sample Payroll Entries
INSERT INTO payroll (emp_id, month, year, base_salary, allowances, deductions, tax) VALUES
(3, 'December', 2024, 15000, 2000, 500, 1000),
(4, 'December', 2024, 12000, 1500, 300, 800),
(5, 'December', 2024, 10000, 1000, 200, 500),
(6, 'December', 2024, 20000, 2500, 700, 1500),
(3, 'November', 2024, 15000.00, 2000.00, 500.00, 1000.00);
-- Other Sample Data
INSERT INTO calendar_events (emp_id, event_date, event_time, description, type, details) VALUES
(NULL, '2025-12-25', '00:00:00', 'Christmas Day', 'Holiday', 'Company-wide holiday.'),
(NULL, CURDATE(), '10:00:00', 'Quarterly Review Meeting', 'Company', 'All staff mandatory attendance.'),
(3, CURDATE(), '14:30:00', '1:1 with Manager', 'Personal', 'Discuss Q4 goals.'), 
(4, '2025-12-11', '09:00:00', 'Urgent Project Deadline', 'Personal', 'Must finish report.'),
(NULL, '2025-12-15', '14:30:00', 'Quarterly All-Hands', 'Company', 'Mandatory attendance via Zoom.');

INSERT INTO bank_requests (emp_id, old_account, new_account, status) VALUES
(3, '1234567890', '0987654321', 'Pending'),
(5, '1122334455', '5544332211', 'Approved');

INSERT INTO salary_advance_requests (emp_id, amount, reason, status) VALUES
(4, 5000, 'Medical expenses', 'Approved'),
(5, 2000, 'Travel expenses', 'Pending');

INSERT INTO reimbursements (emp_id, amount, reason, status) VALUES
(3, 300, 'Stationery purchase', 'Approved'),
(6, 1000, 'Conference fees', 'Pending');

INSERT INTO notifications (user_id, message, status) VALUES
(1, 'Payroll generated for December 2024', 'Unread'),
(2, 'Leave request pending approval', 'Unread'),
(4, 'Salary advance approved', 'Read');

INSERT INTO audit_log (user_id, action) VALUES
(1, 'Created new employee record for Alice Smith'),
(2, 'Approved leave request for Bob Johnson'),
(4, 'Requested salary advance of 5000');

COMMIT;

-- =====================================================
-- ADDITIONAL USER CREATION (Optional)
-- =====================================================

-- Insert New Admin User (Optional - uncomment if needed)
/*
SET @admin_role = (SELECT role_id FROM roles WHERE role_name = 'Admin');
SET @mgmt_dept = (SELECT dept_id FROM departments WHERE dept_name = 'Management');

INSERT INTO users (username, password, role_id, first_name, last_name, email, phone, dept_id, designation, date_of_joining, status) VALUES
('superadmin', 'super123', @admin_role, 'Super', 'Admin', 'superadmin@example.com', '0000000007', @mgmt_dept, 'Chief Administrator', '2024-05-01', 'Active');
SET @superadmin_user_id = LAST_INSERT_ID();

INSERT INTO employees (user_id, role_id, gender, phone, email, dept_id, position, salary, bank_account, date_joined, address, profile_picture_path) VALUES
(@superadmin_user_id, @admin_role, 'Female', '0000000007', 'superadmin@example.com', @mgmt_dept, 'Chief Administrator', 75000.00, '7777777777', '2024-05-01', 'Executive Suite', NULL);
*/

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

SELECT 'Users Table:' as '';
SELECT * FROM users;

SELECT 'Employees Table:' as '';
SELECT emp_id, user_id, position, salary FROM employees;

SELECT 'Leave Requests Table:' as '';
SELECT * FROM leave_requests;

SELECT 'Database setup completed successfully!' as '';

-- Select the database first


ALTER TABLE notifications ADD COLUMN title VARCHAR(100) AFTER user_id;

ALTER TABLE notifications ADD COLUMN target VARCHAR(50) AFTER message;

USE payroll_system;

-- Security Alert
INSERT INTO notifications (user_id, title, message, target, status) 
VALUES (1, 'Security Alert', 'Multiple failed login attempts detected from IP 192.168.1.50.', 'Security', 'Unread');

-- Payroll Notification
INSERT INTO notifications (user_id, title, message, target, status) 
VALUES (1, 'Payroll Processed', 'Monthly payroll for December 2024 has been successfully generated.', 'Payroll', 'Read');

-- System Maintenance
INSERT INTO notifications (user_id, title, message, target, status) 
VALUES (1, 'Maintenance Notice', 'Database backup scheduled for tonight at 12:00 AM.', 'System', 'Unread');




-- Security Alert
INSERT INTO notifications (user_id, title, message, target, status) 
VALUES (1, 'Security Alert', 'Multiple failed login attempts detected from IP 192.168.1.50.', 'Security', 'Unread');

-- Payroll Notification
INSERT INTO notifications (user_id, title, message, target, status) 
VALUES (1, 'Payroll Processed', 'Monthly payroll for December 2024 has been successfully generated.', 'Payroll', 'Read');

-- System Maintenance
INSERT INTO notifications (user_id, title, message, target, status) 
VALUES (1, 'Maintenance Notice', 'Database backup scheduled for tonight at 12:00 AM.', 'System', 'Unread');

-- HR Notification
-- System Maintenance
INSERT INTO notifications (user_id, title, message, target, status) 
VALUES (1, 'Maintenance Notice', 'Database backup scheduled for tonight at 12:00 AM.', 'System', 'Unread');


SET SQL_SAFE_UPDATES = 0;



UPDATE notifications SET target = 'General' WHERE target IS NULL;
UPDATE notifications SET title = 'System Notification' WHERE title IS NULL;

SET SQL_SAFE_UPDATES = 1;

CREATE TABLE leave_balance (
    emp_id INT PRIMARY KEY,
    total_days INT DEFAULT 30,
    used_days INT DEFAULT 0,
    remaining_days INT GENERATED ALWAYS AS (total_days - used_days) STORED,
    FOREIGN KEY (emp_id) REFERENCES employees(emp_id)
);


INSERT INTO notifications (user_id, title, message, created_at)
VALUES (3, 'Test Alert', 'This is a test notification', NOW());

UPDATE notifications
SET target = 'ALL'
WHERE target IS NULL;


USE payroll_system;
-- Rename 'date' to 'attendance_date' to match your Chart logic
ALTER TABLE attendance CHANGE COLUMN date attendance_date DATE NOT NULL;

ALTER TABLE attendance ADD COLUMN hours_worked DECIMAL(5,2) DEFAULT 0.00;

DESCRIBE payroll;

ALTER TABLE payroll ADD COLUMN status VARCHAR(20) DEFAULT 'Pending';

USE payroll_system;

DELIMITER //

CREATE TRIGGER after_leave_request_insert
AFTER INSERT ON leave_requests
FOR EACH ROW
BEGIN
    -- We find the user_id for the HR manager (role_id = 2)
    -- You can adjust this to target a specific HR user or all HR users
    DECLARE hr_user_id INT;
    SELECT user_id INTO hr_user_id FROM users WHERE role_id = 2 LIMIT 1;

    INSERT INTO notifications (user_id, title, message, target, status, created_at)
    VALUES (
        hr_user_id, 
        'New Leave Request', 
        CONCAT('Employee #', NEW.emp_id, ' requested ', NEW.total_days, ' day(s) for: ', NEW.leave_type),
        'HR',
        'Unread',
        NOW()
    );
END //

DELIMITER ;


USE payroll_system;

DELIMITER //
CREATE TRIGGER after_employee_insert
AFTER INSERT ON employees
FOR EACH ROW
BEGIN
    INSERT INTO leave_balance (emp_id, total_days, used_days)
    VALUES (NEW.emp_id, 30, 0);
END //
DELIMITER ;

-- This allows the remarks to stay clean while type holds the category
ALTER TABLE attendance MODIFY COLUMN remarks TEXT;

-- This is required for the "Entry Type" dropdown (Regular, Overtime, etc.) in your UI
ALTER TABLE attendance ADD COLUMN attendance_type VARCHAR(50) DEFAULT 'Regular' AFTER status;

SELECT * FROM attendance WHERE emp_id = 3;

USE payroll_system;

-- Add missing columns to the settings table
ALTER TABLE settings 
ADD COLUMN payroll_locked BOOLEAN DEFAULT FALSE,
ADD COLUMN overtime_multiplier DECIMAL(5,2) DEFAULT 1.5,
ADD COLUMN payroll_cycle VARCHAR(50) DEFAULT 'Monthly',
ADD COLUMN late_threshold INT DEFAULT 15,
ADD COLUMN annual_leave_limit INT DEFAULT 30,
ADD COLUMN auto_approval BOOLEAN DEFAULT FALSE,
ADD COLUMN min_password_length INT DEFAULT 8,
ADD COLUMN max_login_attempts INT DEFAULT 5;
