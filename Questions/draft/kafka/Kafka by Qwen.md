# Kafka / Spring Kafka / Kafka Streams — вопросы на Senior Java Developer (Т‑Банк)

> Подборка вопросов, которыми делятся кандидаты на Habr Career, в Telegram-каналах
> и профильных чатах подготовки. Основной фокус — internals, гарантии доставки,
> отказоустойчивость и edge cases, а не «как создать продюсер».

---

## Содержание

- [Apache Kafka — Core & Internals](#apache-kafka--core--internals)
- [Spring Kafka](#spring-kafka)
- [Kafka Streams](#kafka-streams)
- [Архитектурные и edge case вопросы](#архитектурные-и-edge-case-вопросы)
- [Как готовиться](#как-готовиться)

---

## Apache Kafka — Core & Internals

1. **Как Kafka гарантирует порядок сообщений (Ordering)?**
   Порядок гарантирован **только внутри одного партишена**. Ожидается рассказ о выборе ключа партиционирования и о том, что происходит при изменении числа партишенов.

2. **Что такое Exactly-Once Semantics и как это устроено под капотом?**
   Идемпотентный продюсер (`PID` + `sequence number`) + транзакции + консьюмер с `isolation.level=read_committed`. Важно: EOS внутри Kafka ≠ EOS вместе с внешней БД.

3. **Как устроен Rebalance в Consumer Group? Как сделать его безболезненным?**
   Классический eager-ребаланс — это `stop-the-world`. Ответ включает `CooperativeStickyAssignor` (инкрементальный ребаланс), `group.instance.id` (static membership), `session.timeout.ms`, `heartbeat.interval.ms`.

4. **Как Kafka хранит данные на диске и почему читает так быстро?**
   Логи, сегменты, `.index` / `.timeindex`, `Page Cache` ОС, `Zero-Copy` (`sendfile`) без копирования в user space.

5. **Что такое ISR и когда реплика вылетает из него?**
   Задержка по времени/оффсету (`replica.lag.time.max.ms`). Что происходит, если лидер упал, а ISR пуст: роль `unclean.leader.election.enable` и риск потери данных.

6. **Как работают транзакции в Kafka?**
   `transactional.id`, `transactional.id`-fencing, Transaction Coordinator, запись в `__transaction_state`, коммит маркеров `COMMIT`/`ABORT` и поведение read-committed консьюмера.

7. **Что такое Tombstone и как работает Log Compaction?**
   Сообщение с ключом и `null`-значением; compacted topic как хранилище актуальных состояний, время жизни tombstone (`delete.retention.ms`), ограничения compacted-топиков.

8. **Продюсер получил `acks=1`, лидер упал до репликации. Как избежать потери?**
   `acks=all` + `min.insync.replicas >= 2` + `RF >= 3`, `enable.idempotence`, `max.in.flight.requests.per.connection`, семантика delivery.

9. **Чем `max.poll.records`, `max.poll.interval.ms` и `session.timeout.ms` отличаются?**
   Три разных лимита: размер батча, время на обработку батча, время жизни члена группы без heartbeat. Типичная причина «вечных» ребалансов.

---

## Spring Kafka

10. **Как обработать Poison Pill (ошибку десериализации)?**
    `ErrorHandlingDeserializer` + `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` (DLQ), заголовки `kafka_exception` / исходные байты, что делать с «невозможным» восстановлением.

11. **Как работает `@KafkaListener(concurrency = "N")`?**
    `N` потоков/контейнеров в пределах одного пода; избыточная параллельность не даёт прироста, если партишенов меньше; разница concurrency и числа инстансов.

12. **Как атомарно обновить БД и отправить сообщение в Kafka?**
    Правильный ответ: **Transaction Outbox** + relay / CDC. В Spring есть `KafkaAwareTransactionManager` для синхронизации локальной и Kafka-транзакции, но это не распределённая атомарность.
    > ⚠️ `ChainedTransactionManager` устарел/удалён в современных версиях Spring — его использование на собеседовании будет минусом.

13. **В чём разница `AckMode.RECORD`, `BATCH`, `MANUAL`, `MANUAL_IMMEDIATE`?**
    Момент коммита оффсета, влияние на throughput и на риск потери/дублирования при падении между обработкой и коммитом.

14. **Как настроить Retry с backoff и уходом в DLQ?**
    `DefaultErrorHandler` + `FixedBackOff` / `ExponentialBackOff`, `seekAfterError`, ретраи в том же потоке vs отдельный retry-topic с TTL, неблокирующие ретраи.

15. **Чем `pause/resume` контейнера отличается от ребаланса?**
    Backpressure на уровне консьюмера: остановка `poll()`, отсутствие новых оффсетов, влияние на `max.poll.interval.ms` и lag.

---

## Kafka Streams

16. **Как устроен Stream-Table Join и зачем нужен co-partitioning?**
    Ключ стрима ищется в локальном State Store (RocksDB). Без одинаковой партиционии и числа партишенов join будет давать неполные результаты.

17. **Чем Stream-Stream Join отличается от Stream-Table Join?**
    Stream-Stream требует окна (`JoinWindows`) и хранит обе стороны; Stream-Table — lookup по материализованному состоянию. Также: inner / left / outer семантика.

18. **Зачем нужен `Punctuator`?**
    Реакция на время, а не на новые данные: закрытие окон, выгрузка агрегатов, таймауты. Разница event-time и processing-time (`Supervisor` / wall-clock).

19. **Как Kafka Streams восстанавливает State Store после падения инстанса?**
    Внутренние changelog-топики, replay состояния, `replication.factor` для сторов, **standby replicas**, `processing.guarantee=exactly_once_v2`.

20. **Что такое Interactive Queries?**
    Доступ к материализованному состоянию приложения через REST/gRPC с чтением из локального State Store; `QueryableStoreType`, `KeyQueryMetadata`, перенаправление запросов.

21. **Как масштабится Kafka Streams-приложение?**
    `StreamTask` привязан к набору партишенов, `StandbyTask` — горячая копия; перераспределение задач через тот же групповой протокол, ограничение «подов не больше партишенов».

---

## Архитектурные и edge case вопросы

22. **10 партишенов, 3 пода, один под завис в GC на 10 минут. Что произойдёт?**
    `session.timeout.ms` → исключение из группы → ребаланс; `max.poll.interval.ms` → защита от медленной обработки; почему heartbeat не спасает зависший processing thread.

23. **Как обеспечить идемпотентность консьюмера, если бизнес-логика не идемпотентна?**
    Дедупликация по `message_id` / `producer_id + sequence` в той же транзакции с бизнес-операцией; уникальные индексы, Redis как кэш (не как источник истины), риски дублей на стороне продюсера.

24. **Schema Registry: Backward, Forward, Full, None compatibility — в чём разница?**
    Backward: новая схема читает старые данные. Forward: старая читает новые. Full: оба направления. Примеры ломающих изменений в Avro/Protobuf (`required`, смена типа, переименование).

25. **Как безопасно обновить Avro-схему при 50 потребителях в проде?**
    Расширение схемы → раскатка потребителей → раскатка продюсеров → повышение compatibility в Registry; rollout по контрактам и версионирование, а не «one big bang».

26. **RocksDB в Kafka Streams «раздулся». Что делать?**
    Причинное дерево: неограниченные state/окна без retention, hot keys, память под block cache, I/O. Решения: retention, `caching`, компакция, репартиционирование, мониторинг.

27. **Как спроектировать доставку «ровно один раз» через Kafka + PostgreSQL + внешний платёжный API?**
    Вопрос-комбинезон: Outbox, дедупликация на приёмнике, идемпотентные ключи внешнему API, retry/DLQ, что именно достижимо без 2PC во внешнем мире.

---

## Как готовиться

- Отвечайте через сценарии отказа: **упал брокер / отвалилась сеть / закончился диск / пришёл битый JSON / начался долгий GC**.
- Называйте конкретные параметры и их значения, а не только названия.
- Умейте отличать гарантию **producer → Kafka**, **Kafka → consumer** и **end-to-end вместе с БД**.
- Готовьте реальный кейс: lag, ребалансы, потерянные/задвоенные сообщения, тюнинг через метрики (`records-lag-max`, `request-rate`, `time-between-poll-avg`).

---

### Примечания к формулировкам

1. Атомарность «локальная БД + Kafka» не решается обычным `@Transactional`: используйте Outbox/CDC.
2. `ChainedTransactionManager` не является современным решением и удалён из новых версий Spring.
3. EOS в Kafka Streams (`exactly_once_v2`) не распространяется автоматически на внешние системы — для них нужна своя идемпотентность.