# Spring Security — вопросы для Senior Java Developer (Т-Банк и финтех)

> Выборка реальных вопросов с собеседований и детальных разборов тем, которые задают на Senior-уровне.

## Базовые вопросы (фундамент)

### 1. Архитектура Spring Security и поток запроса (Filter Chain)
Рассказать про `DelegatingFilterProxy`, `FilterChainProxy`, `SecurityFilterChain` и порядок фильтров (`SecurityContextPersistence`, `UsernamePassword`, `ExceptionTranslation`, `AuthorizationFilter`). Показать, что Spring Security — это цепочка фильтров, а не магия.

### 2. Разница между `SecurityContext`, `SecurityContextHolder` и `Authentication`
- `Authentication` — объект с данными пользователя (principal, credentials, authorities).
- `SecurityContext` — контейнер для него.
- `SecurityContextHolder` — место хранения контекста (по умолчанию ThreadLocal).
Обсудить работу с асинхронностью (WebFlux / CompletableFuture): ThreadLocal там не поможет.

### 3. `@PreAuthorize` vs `@PostAuthorize`
- `@PreAuthorize` — проверка прав *до* входа в метод (гейт).
- `@PostAuthorize` — *после* выполнения, есть доступ к `returnObject`.
Обсудить SpEL, использование бинов (`@beanName.method()`) и то, что `@PostAuthorize` не спасает от выполнения дорогой операции, если доступ будет закрыт.

### 4. Почему BCrypt или Argon2, а не MD5/SHA-256?
MD5/SHA быстрые — перебираются на GPU (миллиарды хешей/сек). BCrypt/Argon2 — адаптивные (медленные) и используют соль. Argon2 (memory-hard) — современный стандарт для новых систем. Спросить про `PasswordEncoder` и `DelegatingPasswordEncoder` для миграции.

---

## Senior / Продакшн вопросы

### 5. Аутентификация по JWT в Spring Security (stateless)
Классика для Т-Банка (микросервисы). Кастомный фильтр, наследующий `OncePerRequestFilter`: извлечь токен из `Authorization: Bearer`, проверить подпись/срок, загрузить `UserDetails`, положить `Authentication` в `SecurityContextHolder`. **Важно:** всегда вызывать `filterChain.doFilter()` в `finally` или в конце метода, иначе запрос «зависнет».

### 6. Обработка истечения JWT и Refresh Tokens
Stateless-архитектура не хранит сессии. Обсудить Refresh Token (хранится в БД/Redis, больший TTL), rotation (смена refresh-токена при каждом использовании) и sliding sessions. В Т-Банке ценят понимание безопасности механизмов (отзыв токена при компрометации).

### 7. Настройка CORS и CSRF для REST API
Для stateless REST API на JWT **CSRF обычно отключают** (`http.csrf().disable()`), так как токен передается в заголовке, а не в куке. CORS настраивается через `CorsConfigurationSource`; `CorsFilter` должен стоять *перед* фильтрами аутентификации, иначе preflight-запросы (OPTIONS) будут падать с 401.

### 8. Безопасность в микросервисной архитектуре (OAuth2 / API Gateway)
Архитектурный вопрос. Паттерн API Gateway, который валидирует токен и передает данные пользователя во внутренние сервисы (например, через заголовки). Либо OAuth2 Resource Server, где каждый сервис сам валидирует JWT (публичный ключ). Обсудить хранение секретов и mTLS между сервисами.

### 9. Исключение в кастомном JWT-фильтре: как вернуть 401, а не 500?
Нужно знать про `AuthenticationEntryPoint` и `AccessDeniedHandler`. Если просто бросить `RuntimeException` в фильтре, оно пролетит мимо `ExceptionTranslationFilter` (если тот стоит дальше по цепочке) и вернется 500. Либо ловить исключение внутри фильтра и писать в response, либо делегировать обработку `AuthenticationEntryPoint`.

### 10. Тонкая авторизация (fine-grained access control) на уровне методов/данных
Понимание SpEL и кастомных бинов. Пример: `@PreAuthorize("@tenantService.isMember(#tenantId, authentication.name)")`. Параметры метода должны компилироваться с флагом `-parameters`, чтобы SpEL видел их имена. Часто спрашивают в контексте мультитенантности (банковские сервисы).

---

## «Красные флаги» и специфика Т-Банка

### Задача с кодом (Live Coding / Code Review)
Дают кусок Spring-кода (например, `PaymentController`) и просят найти баги. Часто среди них:
- **Проблемы транзакций:** `@Transactional` на методе, который вызывает внешний сервис; self-invocation, когда `this.method()` игнорирует `@Transactional`.
- **Использование `double` для денег** — сразу говорить про `BigDecimal`.
- **Статический `HashMap` для кэша** — баг (потокобезопасность, утечка памяти). В контексте Security это может быть кэш пользователей или токенов.

### Virtual Threads (Project Loom) и `ThreadLocal`
Взаимодействие с `SecurityContextHolder`, построенным на ThreadLocal. Вопрос «на подумать»: если у вас миллион виртуальных потоков, и каждый хранит контекст безопасности, не будет ли это проблемой с памятью?

### OAuth2 и внешние провайдеры
Authorization Code Flow, интеграция с Keycloak, Okta.

---

## Итог
Готовьте не просто определения, а **сценарии использования** (JWT + Refresh, обработка ошибок в фильтрах, CORS/CSRF для REST) и **умение читать код** на предмет security- и transaction-ошибок. Именно это проверяют на Senior-позициях в финтехе.