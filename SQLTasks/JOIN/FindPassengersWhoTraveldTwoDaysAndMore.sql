-- Есть таблица поездок с двумя колонками, пассажир и дата поездки. Найти пассажиров, которые ездили два и более дней подряд

-- Решение: lag() считаем отдельно для каждого пассажира

SELECT DISTINCT passenger FROM (
SELECT DISTINCT passenger, trip_date, -- DISTINCT для пассажиров, которые ездили два раза в один день. Убираем такие.
-- LAG() берёт значения из предыдущей строки,
 --  считаем это отдельно для каждого пассажира отсортировав по дате.
 -- В результате у самой первой поездки будет prev = NULL, а у последующих будет prev = trip_date предыдущей поездки
 lag(trip_date) OVER(PARTITION BY PASSENGER ORDER BY trip_date) AS prev
 FROM trips) x
 WHERE trip_date - prev_date = 1;  -- Нам подходят строки, у которых prev_date - trip_date = 1
