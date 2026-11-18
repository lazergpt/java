CREATE TABLE department (
    d_id integer,
    d_name varchar(20),
    PRIMARY KEY (d_id)
);

CREATE TABLE employee2 (
    e2_id integer,
    d_id integer,
    e2_name varchar(20),
    e2_position varchar(20),
    e2_salary INTEGER,
    PRIMARY KEY (e2_id)
);

-- Тестовые данные (без NULL)
INSERT INTO department (d_id, d_name) VALUES
    (1, 'IT'),
    (2, 'HR'),
    (3, 'Accounting'),
    (4, 'Sales'),
    (5, 'Marketing');

INSERT INTO employee2 (e2_id, d_id, e2_name, e2_position, e2_salary) VALUES
    (1, 1, 'Alice', 'Developer', 1500),
    (2, 1, 'Bob', 'QA Engineer', 1200),
    (3, 1, 'Charlie', 'DevOps', 1400),
    (4, 2, 'Diana', 'HR Manager', 1100),
    (5, 2, 'Eve', 'Recruiter', 900),
    (6, 3, 'Frank', 'Accountant', 1000),
    (7, 3, 'Grace', 'Auditor', 1050),
    (8, 4, 'Henry', 'Sales Manager', 1300),
    (9, 4, 'Ivy', 'Sales Rep', 800),
    (10, 5, 'Jack', 'Marketing Manager', 1250),
    (11, 5, 'Kate', 'SMM Specialist', 850),
    (12, 4, 'Leo', 'Account Manager', 950);

-- 1. Имя и должность сотрудников с ЗП больше 100
SELECT e2_name, e2_position
FROM employee2
WHERE e2_salary > 100;

-- 3. По каждому отделу вывести имя отдела и к-во сотрудников, у который salary > 100 and salary < 1000.
-- Вывести только те отделы, в которых таких сотрудников не меньше 2
WITH middle_employees_by_department AS (
SELECT e2.d_id, count(1) AS number FROM employee2 e2
WHERE e2.e2_salary > 100 AND e2.e2_salary < 1000
GROUP BY e2.d_id
HAVING count(1) >= 2 )
 SELECT d.d_name, mebd.number  FROM middle_employees_by_department mebd join department d ON d.d_id = mebd.d_id;







-- По каждому отделу вывести имя отдела и к-во сотрудников с salary > 100 и salary < 1000.
-- Вывести отделы, в которых таких сотрудников не меньше двух. Есил меньше, то вывести 0

EXPLAIN WITH two_employee_departments AS (SELECT e.d_id, COUNT(e.e2_id) AS employees_number FROM employee2 e where e.e2_salary > 100 and e.e2_salary < 1000
                                  GROUP BY e.d_id
                                  HAVING count(e.e2_id) >= 2)
 SELECT d.d_name, (CASE WHEN ted.employees_number IS NULL THEN 0 ELSE ted.employees_number END) AS number_column
 FROM department AS d LEFT JOIN two_employee_departments AS ted ON d.d_id = ted.d_id
 ORDER BY number_column DESC;


ROLLBACK;















ROLLBACK ;