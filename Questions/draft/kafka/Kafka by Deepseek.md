# Вопросы по Kafka, Spring Kafka и Kafka Streams на собеседовании Senior Java Developer

> Выборка ориентирована на Т-Банк и близкие по стеку компании.  
> Формат: вопрос + очень краткое пояснение, что именно проверяют.

---

## 1. Вопросы, которые встречаются непосредственно в Т-Банке и близких компаниях

1. **Есть ли ситуации, когда Kafka Producer ждёт Consumer?**  
   Проверяют понимание асинхронной природы Kafka: Producer не блокируется на Consumer’е напрямую; ожидание может возникать косвенно через `max.block.ms`, переполнение `buffer.memory` или заполненный топик.

2. **Если я пишу сообщение в топик Kafka, в сколько партиций оно попадёт?**  
   Сообщение попадает ровно в одну партицию: по ключу, round-robin или sticky-стратегии при отсутствии ключа.

3. **Расскажи, как ты оптимизировал обработку Kafka-сообщений и синхронные RPC-вызовы.**  
   Ожидают практики: батчинг, `fetch.min.bytes`, вынос RPC за пределы consumer-loop, асинхронный acknowledgment, тюнинг `max.poll.records`.

4. **Почему именно Transactional Outbox, а не просто коммит в Kafka после успешной отправки?**  
   Проверяют понимание dual-write problem: запись в БД и отправка в Kafka — две разные системы, поэтому нужна атомарная фиксация события в Outbox.

5. **Как перехватывать все запросы в Kafka и отправлять их в систему аудита/телеметрии?**  
   Проверяют знание `ProducerInterceptor` / `ConsumerInterceptor`, их подключение через `props` и ограничения: интерсепторы не должны делать блокирующих операций.

6. **Назови гарантии доставки сообщений в Kafka и что они означают.**  
   Ожидают не только `at-most-once`, `at-least-once`, `exactly-once`, но и привязку к `acks`, `enable.idempotence`, `isolation.level`, ручному/автоматическому commit.

7. **Как оптимизировать обработку Kafka-сообщений, если Consumer Group отстаёт на миллионы сообщений?**  
   Проверяют диагностику lag: всплеск или устойчивое отставание, параллелизм, блокирующая обработка, внешние вызовы, БД, `max.poll.records`, `fetch.min.bytes`.

8. **Где в Kafka выбирают ключ, и по какому принципу?**  
   Ключ выбирают по сущности, для которой нужен порядок событий, например `orderId`; при этом нужно помнить о риске hot partition при неравномерном ключе.

9. **Consumer переобрабатывает старые сообщения после рестарта — что делать?**  
   Проверяют понимание offset management: commit до завершения обработки, `auto-commit`, неверный `ack-mode`; нужно диагностировать причину, а не просто увеличивать timeout.

10. **Один Consumer перегружен, остальные простаивают — почему и как исправить?**  
    Проверяют понимание партиционирования и rebalance: неравномерные ключи, hot partition, `max.poll.interval.ms`; решения — пересмотреть ключ, кастомный partitioner, изменить число партиций.

---

## 2. Kafka Core / Брокер

11. **Как гарантировать exactly-once в Kafka и какие у этого trade-off’ы по сравнению с at-least-once?**  
    Exactly-once требует идемпотентного producer’а, транзакций и `read_committed` на consumer’е; цена — падение пропускной способности и усложнение конфигурации.

12. **Consumer Group отстаёт на 10 миллионов сообщений. Как триажить и восстановить без потери данных?**  
    Проверяют системное мышление: выяснить причину lag, проверить конкретные партиции, масштабировать consumer’ы не более числа партиций, не делать `reset offset` без понимания последствий.

13. **Как работает Debezium CDC и почему его часто используют вместе с Outbox Pattern?**  
    Debezium читает binlog БД и публикует изменения в Kafka; в связке с Outbox это даёт надёжную публикацию событий без dual-write.

14. **Как вы обрабатываете эволюцию схем в Kafka-топиках, не ломая существующих consumer’ов в большой организации?**  
    Проверяют знание Schema Registry и стратегий совместимости: backward/forward/full compatibility, добавление только опциональных полей с default’ами, запрет на изменение типов.

15. **Когда вы не стали бы использовать Kafka? Опишите over-engineered кейс.**  
    Senior-вопрос на трезвость суждений: Kafka плохо подходит для request/reply с низкой задержкой, простых очередей задач со сложной маршрутизацией и сценариев, где не нужен replay и retention.

16. **В чём разница между eager и cooperative rebalancing?**  
    Eager — stop-the-world: все consumer’ы останавливаются, все партиции отзываются. Cooperative отзывает только затронутые партиции; в Spring Kafka задаётся через `partition.assignment.strategy=CooperativeStickyAssignor`.

17. **Auto-commit vs manual commit: в чём риск auto-commit в production?**  
    Auto-commit коммитит offset каждые 5 секунд независимо от факта обработки; при падении можно потерять сообщение или обработать повторно. Для бизнес-обработки обычно нужен manual commit после успешной обработки.

18. **Как тюнить throughput Producer’а и Consumer’а?**  
    Producer: `linger.ms`, `batch.size`, `compression.type`, `buffer.memory`. Consumer: `fetch.min.bytes`, `fetch.max.wait.ms`, `max.poll.records`. Проверяют понимание trade-off между latency и throughput.

19. **Чем KStream отличается от KTable?**  
    KStream — бесконечный append-only поток событий; KTable — changelog, хранящий последнее значение по ключу, то есть materialized view. `GlobalKTable` реплицируется на все инстансы и используется для enrichment joins.

---

## 3. Spring Kafka

20. **`@KafkaListener` создаёт один consumer на аннотацию?**  
    Нет: аннотация создаёт `MessageListenerContainer`, который может управлять несколькими consumer’ами в зависимости от `concurrency`. Это частая ловушка на собеседовании.

21. **Что произойдёт, если никогда не вызывать `ack.acknowledge()`?**  
    Offset не будет закоммичен; сообщение будет перечитано после рестарта или rebalance. Проверка понимания ручного commit’а в Spring Kafka.

22. **Как настроить несколько типов сообщений в одном топике и десериализовать их в `@KafkaListener`?**  
    Варианты: `@KafkaHandler` в классе-листенере, кастомный `Deserializer`, заголовок с типом, `ErrorHandlingDeserializer`. Проверяют опыт с полиморфными сообщениями.

23. **Почему Kafka использует батчи, и как это связано с `linger.ms`?**  
    Батчи снижают сетевые накладные расходы; `linger.ms` заставляет producer ждать N мс, чтобы накопить больший батч. Больше `linger.ms` — выше throughput, но выше latency.

24. **Как в Spring Kafka настроить обработку ошибок и retry?**  
    `DefaultErrorHandler` / `SeekToCurrentErrorHandler`, `FixedBackOff` / `ExponentialBackOff`, `DeadLetterPublishingRecoverer`. Senior-ответ включает понимание, что retry без идемпотентности может привести к дублям.

---

## 4. Kafka Streams

25. **Почему `suppress(untilWindowCloses)` может ничего не эмитить на idle-партиции?**  
    Потому что `suppress` использует stream-time, то есть максимальный timestamp обработанной записи, а не wall-clock. Если новые записи не приходят, stream-time «замерзает», и окно не закрывается.

26. **Как предотвратить OOMKilled (exit code 137) из-за RocksDB в Kubernetes?**  
    RocksDB аллоцирует off-heap память, невидимую для JVM GC. При множестве state store’ов и потоков native-память может превысить cgroup limit. Решение — общий `RocksDBConfigSetter` с shared cache/write buffer manager.

27. **Как гарантируется exactly-once в Kafka Streams и какие есть ограничения?**  
    Через `processing.guarantee=exactly_once_v2`, транзакции producer’а и `read_committed` consumer’а. Ограничения: транзакции работают только в пределах Kafka; внешние side-effect’ы не становятся идемпотентными автоматически.

28. **Что такое co-partitioning и почему это важно для joins?**  
    Для join’а двух stream’ов/table’ов данные по одному ключу должны попадать в одну и ту же партицию в обоих топиках: одинаковое число партиций и одинаковый partitioner. Иначе Kafka Streams создаст repartition-топик с дополнительным overhead.

29. **Чем KStream отличается от KTable в контексте state store’ов?**  
    Stateful-операторы над KStream требуют state store и changelog-топик; KTable уже представляет state и может быть materialized. Проверяют, понимает ли кандидат, какие операторы stateful и как они восстанавливаются после failover’а.

30. **Как обрабатывать late-arriving events в Kafka Streams?**  
    Через `window.grace()`, `suppress()` и понимание разницы между event-time и processing-time. Senior-ответ включает риск того, что `suppress` может «застрять» без heartbeat’ов.

---

## 5. Как это выглядит на собеседовании в Т-Банке

Судя по отзывам, Kafka в Т-Банке редко спрашивают как «теорию ради теории». Вопросы строятся вокруг реальных сценариев:

- «как ты оптимизировал»;
- «что делать, если лаг»;
- «почему выбрал Outbox»;
- «как гарантировать exactly-once и чем заплатил».

При подготовке имеет смысл отрабатывать не определения, а диагностические цепочки и trade-off’ы — именно это отличает Senior-ответ от Middle.