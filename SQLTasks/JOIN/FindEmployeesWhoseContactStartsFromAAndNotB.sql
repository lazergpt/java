-- Есть две таблицы - сотрудники и их контакты. Вывести сотрудников у которых есть контакты начинающиеся на А, и нет контактов начинающихся на В.


-- Решение 1 - через EXISTS
SELECT * FROM employees e
WHERE EXISTS (SELECT 1 FROM contacts c where c.e_id = e.e_id and c.c_name LIKE 'A%')
AND NOT EXISTS (SELECT 1 FROM contacts c where c.e_id = e.e_id and c.c_name LIKE 'B%');

-- Решение 2. Через JOIN, GROUP BY, HAVING COUNT
SELECT e.e_id, e.e_name FROM employees e JOIN contacts c ON e.e_id = c.e_id
GROUP BY e.e_id, e.e_name
HAVING COUNT (CASE WHEN c.c_name like 'A%' then 1 end) > 0
AND COUNT(CASE WHEN c.c_name LIKE 'B%' then 1 end) = 0;

-- Решение 3: Через EXCEPT
SELECT * FROM employees e
WHERE e.e_id IN (
SELECT c.e_id FROM contacts c where c.e_id = e.e_id and c.c_name LIKE 'A%'
EXCEPT
SELECT c.e_id FROM contacts c where c.e_id = e.e_id and c.c_name LIKE 'B%'
);

ROLLBACK;