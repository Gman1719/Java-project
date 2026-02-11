# Testing Guide - Employee Dashboard & Payroll Officer Dashboard

## Database Setup (CRITICAL FIRST STEP)

Before testing, ensure your database is set up:

```sql
-- Run this in MySQL
mysql -u root -p
source c:\Users\hp\eclipse\employee - Copy\src\sql\sql.sql
```

**Database Connection Settings:**
- File: `src/utils/DBConnection.java`
- URL: `jdbc:mysql://localhost:3306/payroll_system`
- Username: `root`
- Password: `root`

---

## Employee Dashboard Testing

### Login Credentials
```
Username: employee1
Password: emp123
```
Or:
```
Username: employee2
Password: emp123
```

### Buttons & Their Functions

#### 1. **Home Button** ✅
- **Action**: Refreshes all dashboard data
- **Tables Filled**:
  - Attendance Table (from `attendance` table)
  - Leave Requests Table (from `leave_requests` table)
  - Other Requests Table (from `bank_requests`, `salary_advance_requests`, `reimbursements`)
- **Metrics Updated**:
  - Net Salary (from `payroll` table)
  - Leave Balance (calculated from `leave_requests`)
  - Next Pay Day (calculated)
- **Expected Result**: All tables should populate with current employee's data

#### 2. **Attendance Button** ✅
- **Action**: Scrolls to Attendance tab
- **Table**: Shows attendance records for selected month
- **Database Table**: `attendance`
- **Columns**: Date, Time In, Time Out, Status, Remarks
- **Test**:
  1. Select a month from DatePicker
  2. Click "Load" button
  3. Table should show attendance records

#### 3. **Leaves Button** ✅
- **Action**: Opens Leave Request Form
- **Database Table**: Inserts into `leave_requests`
- **Test**:
  1. Click "Leaves"
  2. Modal opens with form
  3. Select: Leave Type, Start Date, End Date
  4. Enter reason
  5. Click "Submit Request"
  6. Check database: `SELECT * FROM leave_requests WHERE emp_id = 3;`
  7. Leave Requests table should refresh automatically

#### 4. **Payslips Button** ✅
- **Action**: Opens Payslip History modal
- **Database Table**: Reads from `payroll`
- **Test**:
  1. Click "Payslips"
  2. Modal shows all payslips for current employee
  3. Can view/download individual payslips
  4. Check: `SELECT * FROM payroll WHERE emp_id = 3;`

#### 5. **New Request Button** ✅ (NEWLY ADDED)
- **Action**: Opens New Request Form
- **Request Types**:
  - **Salary Advance** → `salary_advance_requests` table
  - **Bank Account Change** → `bank_requests` table
  - **Reimbursement** → `reimbursements` table
- **Test for Salary Advance**:
  1. Click "New Request"
  2. Select "Salary Advance"
  3. Amount field appears
  4. Enter amount: 5000
  5. Enter reason
  6. Click "Submit Request"
  7. Check: `SELECT * FROM salary_advance_requests WHERE emp_id = 3;`

- **Test for Bank Change**:
  1. Select "Bank Account Change"
  2. Old Account and New Account fields appear
  3. Enter both account numbers
  4. Enter reason
  5. Submit
  6. Check: `SELECT * FROM bank_requests WHERE emp_id = 3;`

- **Test for Reimbursement**:
  1. Select "Reimbursement"
  2. Amount field appears
  3. Enter amount and reason
  4. Submit
  5. Check: `SELECT * FROM reimbursements WHERE emp_id = 3;`

#### 6. **Requests Button** ✅
- **Action**: Scrolls to "Other" requests tab
- **Table**: Shows latest status of all request types
- **Expected Data**:
  - "Bank Account Change (date): Status"
  - "Salary Advance (date): Status"
  - "Reimbursement (date): Status"

#### 7. **Calendar Button** ✅
- **Action**: Opens Company Calendar modal
- **Database Table**: `calendar_events`
- **Shows**:
  - Company holidays (emp_id = NULL)
  - Personal events (emp_id = current employee)
- **Test**:
  1. Click "Calendar"
  2. See company holidays and personal events
  3. Can add personal events

#### 8. **Notifications Button** ✅
- **Action**: Opens Notifications modal
- **Database Table**: `notifications`
- **Shows**: User-specific notifications
- **Test**:
  1. Click "Notifications"
  2. See list of notifications
  3. Check: `SELECT * FROM notifications WHERE user_id = 3;`

#### 9. **Profile Button** ✅
- **Action**: Opens Edit Profile modal
- **Database Tables**: Updates `users` and `employees`
- **Test**:
  1. Click "Profile"
  2. Modify profile information
  3. Save changes
  4. Check database for updates

#### 10. **Download Payslip Button** ✅
- **Action**: Downloads latest payslip as PDF
- **Database Table**: Reads from `payroll`
- **Test**:
  1. Click "Download PDF" in Net Salary card
  2. File save dialog appears
  3. PDF is generated with:
     - Employee name
     - Base salary, allowances, deductions, tax
     - Net salary
     - Generated date

#### 11. **Logout Button** ✅
- **Action**: Clears session and returns to login
- **Test**: Should return to login screen

---

## Payroll Officer Dashboard Testing

### Login Credentials
```
Username: payroll1
Password: pay123
```

### Buttons & Their Functions

#### 1. **Dashboard Button** ✅
- **Action**: Shows dashboard overview
- **Metrics Loaded** (all from database):
  - Total Personnel: `COUNT(*) FROM employees WHERE status='Active'`
  - Pending Approval: `COUNT(*) FROM payroll WHERE status='Pending'`
  - Attendance Flags: `COUNT(*) FROM attendance WHERE status='Absent'`
  - Total Payout: `SUM(net_salary) FROM payroll`
- **Test**: Click Dashboard, all metrics should show real numbers

#### 2. **Run Payroll Button** ✅
- **Action**: Navigates to payroll tab
- **Tables Filled**: Payroll table from `payroll` table
- **Test**:
  1. Select Month and Year
  2. Click "Fetch Data"
  3. Table fills with employee payroll records
  4. Columns: Employee Name, Base Salary, Allowances, Deductions, Net Salary, Status

#### 3. **Batch Generate Button** ✅
- **Action**: Generates payroll for ALL active employees
- **Database**: Inserts into `payroll` table
- **Test**:
  1. Select month/year (e.g., January 2025)
  2. Click "Batch Generate"
  3. Confirmation dialog appears
  4. Click "Yes"
  5. Check database: `SELECT * FROM payroll WHERE month='January' AND year=2025;`
  6. Should see records for all employees
  7. Net salary is auto-calculated: (base + allowances - deductions - tax)

#### 4. **Fetch Data Button** ✅
- **Action**: Loads payroll for selected month/year
- **Database**: Reads from `payroll` JOIN `employees` JOIN `users`
- **Test**: Select different months and click "Fetch Data"

#### 5. **Lock Month Button** ✅
- **Action**: Security lock for a payroll period
- **Database**: Inserts into `audit_log`
- **Test**:
  1. Click "Lock Month"
  2. Confirmation dialog
  3. Action is logged in audit_log table

#### 6. **Export to Excel Button** ✅
- **Action**: Exports current payroll table to CSV
- **Test**:
  1. Load payroll data
  2. Click "Export As... → Export to Excel"
  3. Save dialog appears
  4. CSV file created with all payroll data
  5. Open CSV to verify

#### 7. **Salary Structures Tab** ✅
- **Tables Filled**:
  - Employee List: Loaded from `employees` table via EmployeeDAO
- **Test**:
  1. Click "Salary Structures"
  2. Employee list loads on left side
  3. Click an employee
  4. Salary details populate
  5. Modify allowances/deductions
  6. Live net salary calculation updates
  7. Click "Calculate & Save"
  8. Check: `SELECT salary FROM employees WHERE emp_id = X;`

#### 8. **Save Structure Button** ✅
- **Action**: Updates employee salary
- **Database**: `UPDATE employees SET salary = ? WHERE emp_id = ?`
- **Also logs**: `INSERT INTO audit_log ...`
- **Test**: Modify salary and verify in database

#### 9. **Tax & Settings Tab** ✅
- **Tables Filled**: Settings loaded from `settings` table
- **Fields**:
  - Tax Rate (from `settings.tax_rate`)
  - Social Rate (from `settings.social_rate`)
  - Currency Symbol (from `settings.currency_symbol`)
- **Test**:
  1. Click "Tax & Settings"
  2. Current values load from database
  3. Modify tax rate
  4. Click "Update Global Settings"
  5. Check: `SELECT * FROM settings;`

#### 10. **Reports & Archive Tab** ✅
- **Table Filled**: Payslip History from `payroll` table
- **Columns**: Month/Year, Employee, Amount
- **Query**: 
  ```sql
  SELECT CONCAT(u.first_name,' ',u.last_name) AS emp,
         CONCAT(p.month,' ',p.year) AS period,
         p.net_salary
  FROM payroll p
  JOIN employees e ON p.emp_id = e.emp_id
  JOIN users u ON e.user_id = u.user_id
  ORDER BY p.generated_on DESC
  ```
- **Test**:
  1. Click "Reports & Archive"
  2. Table shows all historical payroll records
  3. Sorted by most recent first

#### 11. **Notifications Icon (🔔)** ✅
- **Action**: Shows unread notifications
- **Database**: `SELECT * FROM notifications WHERE user_id = ? AND status = 'Unread'`
- **Test**: Click bell icon, see notifications

#### 12. **Logout Button** ✅
- **Action**: Clears session and returns to login
- **Test**: Should return to login screen

---

## Common Issues & Solutions

### Issue 1: Tables are Empty
**Solution**:
```sql
-- Check if data exists
SELECT * FROM attendance WHERE emp_id = 3;
SELECT * FROM payroll WHERE emp_id = 3;
SELECT * FROM leave_requests WHERE emp_id = 3;

-- If no data, the tables will be empty (which is correct!)
-- Add some sample data using the INSERT statements in sql.sql
```

### Issue 2: "Employee session data is missing"
**Solution**: 
- SessionManager.getCurrentEmployeeId() returns 0
- Check that you logged in correctly
- Restart application and login again

### Issue 3: Database connection failed
**Solution**:
- Check MySQL is running: `mysql -u root -p`
- Verify DBConnection.java has correct credentials
- Test connection: 
  ```sql
  USE payroll_system;
  SHOW TABLES;
  ```

### Issue 4: Buttons don't do anything
**Solution**:
- Check console for errors
- Verify FXML onAction methods match controller methods
- Check that all @FXML annotations are present

---

## Verification Queries

### Check Employee Dashboard Data:
```sql
-- Replace emp_id = 3 with your test employee ID

-- Attendance
SELECT * FROM attendance WHERE emp_id = 3;

-- Leave Requests
SELECT * FROM leave_requests WHERE emp_id = 3;

-- Payroll (Payslips)
SELECT * FROM payroll WHERE emp_id = 3;

-- Requests
SELECT * FROM salary_advance_requests WHERE emp_id = 3;
SELECT * FROM bank_requests WHERE emp_id = 3;
SELECT * FROM reimbursements WHERE emp_id = 3;

-- Notifications
SELECT * FROM notifications WHERE user_id = 3;

-- Calendar Events
SELECT * FROM calendar_events WHERE emp_id = 3 OR emp_id IS NULL;
```

### Check Payroll Officer Data:
```sql
-- All Employees
SELECT * FROM employees WHERE status = 'Active';

-- Payroll Records
SELECT * FROM payroll;

-- Settings
SELECT * FROM settings;

-- Audit Log
SELECT * FROM audit_log ORDER BY log_time DESC LIMIT 10;
```

---

## Success Criteria

### Employee Dashboard ✅
- [x] All 11 buttons functional
- [x] Attendance table loads from database
- [x] Leave requests table loads from database
- [x] Other requests table loads from database
- [x] Notifications list loads from database
- [x] Leave request form submits to database
- [x] New request form submits to database (3 types)
- [x] Payslip download generates PDF
- [x] All metrics show real data

### Payroll Officer Dashboard ✅
- [x] All 12 buttons functional
- [x] Payroll table loads from database
- [x] Employee list loads from database
- [x] Dashboard metrics load from database
- [x] Batch generate creates payroll records
- [x] Export to Excel works
- [x] Salary structure saves to database
- [x] Tax settings load and save to database
- [x] Archive table loads historical payroll
- [x] All tabs navigate correctly

---

## Quick Test Sequence

### Employee Dashboard (5 minutes):
1. Login as employee1/emp123
2. Check all metrics have values
3. Click "Leaves" → Submit a leave request
4. Check Leaves tab shows the new request
5. Click "New Request" → Submit salary advance
6. Check Other tab shows the request
7. Click "Calendar" → View events
8. Click "Notifications" → View notifications
9. Download payslip PDF

### Payroll Officer Dashboard (5 minutes):
1. Login as payroll1/pay123
2. Check dashboard metrics have values
3. Select current month/year → Click "Fetch Data"
4. Table should populate
5. Click "Salary Structures" → See employee list
6. Select an employee → See salary details
7. Click "Tax & Settings" → See current settings
8. Click "Reports & Archive" → See payroll history
9. Export to Excel

**All tests should pass with real database data!**
