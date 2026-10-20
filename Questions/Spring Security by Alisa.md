# Вопросы по Spring Security для собеседований (Senior Java Developer)

## Базовый блок — спрашивают почти всем

### **Чем аутентификация отличается от авторизации?**
1. **Аутентификация** — «кто ты», проверка логина и пароля, токена. `AuthenticationManager` -> `UserDetailsService`, создание объекта `Authentication` с `principals` и `credentials` и помещение его в `SecurityContextHolder`.
2. **Авторизация** — «что тебе можно». `AuthorizationManager`, `FilterSecurityInterceptor` / `AuthorizationFilter`, аннотации `@PreAuthorize`/`@PostAuthorize`, конфигурация `authorizeHttpRequests`.

Аутентификация бывает один раз при логине, а авторизация при каждом доступе к защищённому ресурсу.

### **В чём разница между 401 и 403?**
**401 Unauthorized** — не аутентифицирован: сервер не знает, кто ты. Возникает когда пользователь не предоставил валидные учётные данные или токен \
**403 Forbidden** - аутентификация пройдена успешно, но недостаточно прав для доступа к ресурсу.

**Нюансы:**
- если пользователь анонимный, запрос на защищенный ресурс сначала получит 401. Чтобы аноним сразу получал 403, нужно явно разрешить анонимный доступ (permitAll или anonymous()), тогда AccessDeniedHandler перехватит отсутствие прав.
- для микросервисов `AuthenticationEntryPoint` и `AccessDeniedHandler` почти всегда переопределяются. Первый — чтобы вернуть `JSON` вместо страницы логина браузера, второй — чтобы прокинуть в ответ специфический код ошибки (например, `INSUFFICIENT_SCOPE`) для фронтенда.

### **Что такое `SecurityContextHolder` и как он работает?**
`SecurityContextHolder` — это статический _holder_ для объекта `SecurityContext`, который содержит `Authentication` (информацию об аутентифицированном пользователе).\
Контекст привязывается к текущему потоку. Любой код внутри этого потока (сервис, репозиторий) может получить пользователя через `SecurityContextHolder.getContext().getAuthentication()` без передачи параметров.\
Стратегии хранения (задаются через `SecurityContextHolder.setStrategyName()` или системное свойство `spring.security.strategy`:
- **MODE_THREADLOCAL** по дефолту.
- **MODE_INHERITABLETHREADLOCAL** — дочерние потоки наследуют контекст родителя, но для `@Async`
- **MODE_GLOBAL** — один контекст на всё **JVM** (для _standalone_-приложений, в вебе не используется)

**Нюансы:**
- Если вызывается `@Async` или используете `ExecutorService`, `ThreadLocal` не передаст контекст в новый поток автоматически.\
Для этого **Spring Security** предоставляет обертки вроде `DelegatingSecurityContextRunnable` или `DelegatingSecurityContextExecutor`, которые копируют контекст из текущего потока в целевой и очищают его после выполнения.
- контекст автоматически чистится после завершения запроса
- `SecurityContextHolder.MODE_INHERITABLETHREADLOCAL` не спасает при работе с пулами потоков, потому что пул переиспользует потоки, созданные раньше, наследование срабатывает только в момент создания потока.
- Прямой доступ к **principal** лучше получать через `@AuthenticationPrincipal` или `SecurityContextHolderStrategy` это удобнее для тестов, чем статический вызов.
### **Как устроен фильтр-чейн Spring Security?**
`FilterChain` в `Spring Security` — это цепочка сервлет-фильтров, которые обрабатывают каждый HTTP-запрос до попадания в контроллер.\
Порядок работы:
- Контейнер вызывает `DelegatingFilterProxy`. 
- `DelegatingFilterProxy` передаёт управление `FilterChainProxy`. 
- `FilterChainProxy` перебирает свой список `SecurityFilterChain` (в порядке, заданном аннотацией @Order или порядком объявления) и находит первую, чей RequestMatcher совпал с запросом.
- Далее запрос проходит через фильтры, определённые в этой конкретной `SecurityFilterChain`.

Ключевой компонент — `DelegatingFilterProxy` (единственный фильтр, регистрируемый в `Servlet Container`), который делегирует обработку внутреннему `FilterChainProxy`.\
`FilterChainProxy` - точка входа для всех запросов и содержит в себе список цепочек `SecurityFilterChain`\
**Основные фильтры, имеют строгий порядок:**
- `ChannelProcessingFilter` — редирект **HTTP→HTTPS**
- `CsrfFilter` — проверка **CSRF**-токена
- `LogoutFilter` — обработка logout
- `UsernamePasswordAuthenticationFilter` — аутентификация по логину/паролю
- `BearerTokenAuthenticationFilter` — аутентификация по **JWT**
- `DefaultLoginPageGeneratingFilter` — генерация страницы логина
- `ExceptionTranslationFilter` — перевод исключений безопасности в HTTP-ответы
- `AuthorizationFilter` — проверка прав доступа
- `SecurityContextHolderFilter` — сохранение/очистка **_SecurityContext_**

**Нюансы:**
- Порядок фильтров определяется через `.addFilterBefore()`, `.addFilterAfter()` или кастомный `SecurityFilterChain` bean.\
  Место вставки критично. Например, кастомный фильтр для проверки JWT-токена логично ставить до `UsernamePasswordAuthenticationFilter`, чтобы он успел заполнить контекст.
- Фильтры выполняются для ВСЕХ запросов, даже если endpoint публичный
- Можно отключать фильтры: `.csrf().disable()`, `.cors().disable()`
- Кастомные фильтры: наследовать от `OncePerRequestFilter` или `GenericFilterBean`
- `FilterChainProxy` находит только первое совпадение. Если запрос совпал с цепочкой, он не будет проверять остальные, даже если они тоже подходят.\
Поэтому более специфичные цепочки (например, для `/api/**`) должны иметь более высокий приоритет (меньший `@Order`), чем общие (например, для `/**`)
- В приложении можно (и нужно) определять несколько бинов `SecurityFilterChain`
- Чтобы увидеть, какие именно фильтры активны в вашей цепочке, включите логирование на уровне **DEBUG** для `org.springframework.security.web.DefaultSecurityFilterChain`.\
В логах при старте приложения будет выведен полный список фильтров в порядке их выполнения.
- Фильтры работают до `DispatcherServlet` — значит, контроллеры, AOP и `@ExceptionHandler` из **MVC** не видят исключений фильтров. 
- Обработку ошибок в фильтре делают через `authenticationEntryPoint` и `accessDeniedHandler`.

### **Чем `Role` отличается от `Authority`?**
Технически разницы в типе нет — и роль, и _authority_ это `GrantedAuthority`(интерфейс, представляющий одно право). Разница — в семантике и в префиксе.\
1. **Authority** — атомарное право (_permission_): _user:read_, _order:cancel_, _product:write_. Тонкий уровень доступа.
2. **Role** —  это частный случай _Authority_ с префиксом `ROLE_`, группа _authority_, логический «ярлык» пользователя: _ROLE_ADMIN_, _ROLE_USER_.

**Проверка:**
- `.hasAuthority("READ_ORDERS")` — проверяет точное совпадение.
- `.hasRole("ADMIN")` — автоматически добавляет префикс `ROLE_` и проверяет `ROLE_ADMIN`

**Нюансы:**
- Префикс настраивается: через `GrantedAuthorityDefaults` можно изменить префикс ролей (по умолчанию _ROLE_)
- В токенах **JWT** роли часто хранятся как **claims**, которые маппятся в `GrantedAuthority` через `JwtAuthenticationConverter`
- пользователь может иметь и роли, и отдельные `authority` одновременно

### **Session vs JWT — что выбрать и почему?**
1. Session-based
**Плюсы:**
- Проще отозвать доступ (удалил сессию из хранилища — пользователь разлогинен)
- Меньший размер токена (только session ID в cookie)
- Просто реализовать мгновенный logout на всех устройствах

**Минусы:**
- Требует sticky sessions или shared session store (_Redis_) в кластере
- Не подходит для mobile/native apps (проблемы с cookies)
- Сервер хранит состояние → хуже масштабируемость

2. **JWT**
**Плюсы:**
- stateless, но если нужен logout то уже и нет
- Работает из коробки для mobile, SPA, API
- Токен содержит claims → меньше запросов к БД за информацией о пользователе

**Минусы:**
- Невозможно отозвать до истечения TTL (без blacklist/whitelist, что убивает stateless)
- Большой размер токена (особенно с множеством claims)

**Нюансы:**
- Spring Security 6+: `BearerTokenAuthenticationFilter` для JWT, `SessionManagementFilter` для сессий
- Для Logout в JWT нужен blacklist (Redis с TTL = время жизни токена) или versioning токенов
- Для JWT обязательно `SessionCreationPolicy.STATELESS` и отключение **CSRF** (так как нет cookie-аутентификации)
- Может быть Две цепочки `SecurityFilterChain` с разными `@Order`. Одна — для `/ui/**` (formLogin + Session), другая — для `/api/**` (JWT + Stateless)
- JWT не шифрует данные (только подпись). Не кладите туда чувствительные данные.
- Где хранить **JWT** на фронте: `localStorage` уязвим к **XSS**. **HttpOnly** cookie безопаснее от **XSS**, но возвращает проблему **CSRF** (нужен токен). Для browser-based **SPA** часто выбирают **cookie** + **CSRF**, для мобилок — header.

**В итоге:**
- JWT для микросервисов и REST API которым пользуется множество разных UI
- Для односервисного монолита лучше сессии.

### **Что делает `@PreAuthorize` и чем отличается от `@Secured`?**
1. `@Secured` 
- устаревшая
- поддерживает только роли и только в виде строк в виде строк. Аналог `@PreAuthorize("hasRole('...')")`
- нет SpEL выражений
- нет доступа к аргументам метода 
- Включается через `@EnableGlobalMethodSecurity(securedEnabled = true)`
2. `@PreAuthorize`
- Полная поддержка _SpEL_ (Spring Expression Language)
- Доступ к аргументам метода, return value, authentication object
- Кастомные expression handlers
- Включается через `@EnableMethodSecurity` (**_Spring Security 6+_**) или `@EnableGlobalMethodSecurity(prePostEnabled = true)`

**Нюансы:**
- `@PreAuthorize` парсит SpEL при каждом вызове - может влиять на производительность
- Proxy-based: работает только через Spring AOP proxy → не работает при вызове методов внутри того же класса (this.method())
- `@PreAuthorize` проверяется до входа в метод, `@PostAuthorize` — после (может использовать return value)
- могут спросить про `@RolesAllowed`это _**Jakarta EE**_ стандарт, по сути тот же механизм, что `@Secured`

## JWT и токены — обязательный блок для REST/микросервисов

### **Опишите жизненный цикл JWT-аутентификации.**
1. Получение токена (Login)
```-POST /auth/login {username, password}
   → AuthenticationManager.authenticate()
   → UserDetailsService.loadUserByUsername()
   → Проверка credentials (PasswordEncoder)
   → Генерация JWT (JwtEncoder / JwtGenerator)
   ← Response: {accessToken, refreshToken}
```
2. Доступ к защищённому ресурсу (Request) - Клиент отправляет запрос к защищённому эндпоинту, добавляя заголовок `Authorization: Bearer <access_token>` .
```
GET /api/orders (Header: Authorization: Bearer <jwt>)
→ BearerTokenAuthenticationFilter.extractToken()
→ JwtDecoder.decode() ← парсинг + проверка подписи + claims
→ JwtAuthenticationConverter.convert() ← маппинг claims → GrantedAuthority
→ Создание JwtAuthenticationToken (authenticated=true)
→ SecurityContextHolder.setContext()
→ AuthorizationFilter.checkAccess() ← @PreAuthorize / .hasRole()
→ Controller
```
3. Обновление токена (Refresh)
```
POST /auth/refresh {refreshToken}
→ Валидация refresh token (отдельный JwtDecoder или opaque token store)
→ Проверка: не отозван? не истёк?
→ Генерация НОВОГО access + refresh (rotation)
← Response: {newAccessToken, newRefreshToken}
```  
4. Logout - Поскольку Access Token stateless, «мгновенный» logout невозможен. Обходится через blacklist
- при разлогине добавить токен в blacklist (Redis с TTL = оставшееся время жизни)
- в кастомном `JwtDecoder` или `BearerTokenResolver` проверять _blacklist_ перед валидацией

**Нюансы:**
- Старый Refresh Token инвалидируется, и выдаётся новый. Если злоумышленник украл старый Refresh Token и попытается его использовать после того,\
как легитимный клиент уже обновил токен, сервер обнаружит повторное использование отозванного токена и сможет заблокировать всю сессию злоумышленника.
- `JwtDecoder` vs `JwtEncoder`: _**Decoder**_ — валидация (симметричный/асимметричный ключ), _**Encoder**_ — генерация. В микросервисах: Auth Service подписывает (private key), Resource Services только проверяют (public key)

**В Spring Security существует два подхода к реализации JWT цикла:**
 - Ручной (кастомный фильтр): Вы пишете `JwtAuthenticationFilter`, сами парсите и валидируете токен (например, через библиотеку `jjwt`). Это даёт полный контроль, но требует написания большего количества кода.
 - Встроенный (OAuth2 Resource Server): Если вы используете стандартный **_OAuth2/OIDC_**, **_Spring Security_** предоставляет готовый `BearerTokenAuthenticationFilter` и `JwtAuthenticationConverter`.\
Вы только настраиваете `JwtDecoder` (через JWKS URI или публичный ключ), а фреймворк сам делает всю тяжёлую работу по валидации и маппингу claims в authorities . Для нового проекта это предпочтительный путь.
### **Как отозвать JWT до истечения срока жизни?**
1. Token Blacklist (наиболее распространённый) - при логауте помещаем JTI или токен в Redis а в кастомном `JwtDecoder` проверяем наличие, если есть значит логаученый токен и ничего рефрешить не нужно.\
   ✅ - точный контроль, TTL автоматически истекает\
   ❌ - убивает pure stateless\
   ❌ - зависимость от Redis
2. **_Token Versioning / Generation Counter_** - в токене храним версию, при генерации версию инкрементируем и кладём в токен и БД, когда нужно отозвать токен инкрементируем версию в БД.\
Когда приходит запрос проверяем версию в токене, если не совпадет, тогда токен устарел и инвалидирован.
   ✅ - один ключ на пользователя вместо blacklist per token\
   ❌ - отзыв всех токенов сразу (нет гранулярности)/
3. Короткоживущий Access + ротация Refresh Token - ограничение урона при утечке токена
4. JWK Key Rotation - Сменить signing key и все ранее выданные токены становятся невалидными. Используется при утечке приватного ключа.

**Нюансы:**
- Не используйте blacklist для каждого запроса в high-load → кешируйте проверки локально (Caffeine + Redis)

### **Как устроен refresh-токен и зачем он нужен?**
Это обычный JWT (или opaque-строка), но с другим набором claims: type: "refresh", subject, jti, exp — обычно без ролей, потому что роли должны браться заново при выпуске нового access.\
Access token делают короткоживущим для безопастности, но заставлять пользователя логинится каждые 15 минут неприемлемо.\
Для этого применяют `Refresh token` который живёт 24 и более часа. Его задача - получать новые Access token.\
На клиенте можно хранить:
- **Web:** HttpOnly + Secure + SameSite=Strict cookie (не localStorage!)
- **SPA:** BFF pattern (backend-for-frontend хранит refresh, фронтенд получает только short-lived access)


**Нюансы:**
- Каждое использование refresh → выдача нового refresh. Старый инвалидируется. Это предотвращает replay-атаки.
- Хранится на серверной стороне хотя бы частично: либо сам токен (или его хеш) в БД/Redis со статусом, либо одна запись на пользователя (версионирование). Именно хранение даёт возможность отзыва.
- **reuse detection** - если «отозванный» **refresh** внезапно предъявлен повторно, это почти наверняка кража: легитимный клиент пользуется новым токеном, а старым пользуется вор, как результат инвалидируется вся семья токенов
- race condition. Если у пользователя плохой интернет и запрос на обновление токена «потерялся» после того, как сервер уже ротировал токен, клиент попытается использовать старый refresh-токен.\
Сработает **reuse detection**, и пользователя выкинет. Поэтому в продакшене часто добавляют «льготный период» (grace period) в 30–60 секунд, в течение которого старый токен еще принимается, чтобы покрыть сетевые сбои.
-  токен связывают с device id или fingerprint, чтобы refresh, украденный с одного устройства, не работал с другого.

### **Где хранить JWT на клиенте и почему?**
    localStorage удобен, но уязвим к XSS; httpOnly-cookie защищает от XSS, но открывает CSRF. Ожидают осознанный выбор, а не «так принято».

12. **Что за нагрузка `aud`, `iss`, `jti` в JWT?**
    Провайдер (iss), для кого токен (aud), идентификатор токена (jti — для отзыва/защиты от replay). Проверка `aud` — типичный пропуск, про который спрашивают на senior-собесах.

## OAuth2 и микросервисы

13. **Объясните flow Authorization Code Grant.**
    Почему нельзя просто password grant на фронтенде, зачем нужен PKCE для публичных клиентов, где живёт client secret. Любимый вопрос в командах с внешними интеграциями.

14. **Resource Server vs Authorization Server — кто что делает?**
    Resource server валидирует access-токен (`BearerTokenAuthenticationFilter`, JWKS-проверка), authorization server выпускает токены. Ожидают понимания, что JWT сам по себе «не безопасность», а формат.

15. **Как Spring Security настроен в реактивном стеке (WebFlux)?**
    `SecurityContextHolder` с ThreadLocal не работает — вместо него `ReactiveSecurityContextHolder` с Reactor Context. Хороший маркер того, что человек видел не только servlet-стек.

## Практические ловушки и «интересные» вопросы

16. **Как работает CSRF и зачем он нужен в SPA?**
    Спрашивают не «что это», а «когда можно отключить и когда нельзя»: если токен в заголовке `Authorization`, а не в cookie — CSRF не применим. Плюс `CsrfFilter` и `CookieCsrfTokenRepository`.

17. **Как защититься от brute-force?**
    Ожидают не «каптча», а layered-ответ: `AccountStatusUserDetailsChecker` (lock/unlock), rate limiting на уровне filter, экспоненциальная задержка, мониторинг failed auth через events (`AuthenticationFailureHandler`).

18. **Как безопасно мигрировать пароли с одного хеша на другой?**
    Через `DelegatingPasswordEncoder`: каждый хеш помечается `{id}` (например, `{bcrypt}`), при логине пользователь перекодируется на новый алгоритм — так переносят legacy-пароли без сброса паролей.

19. **Зачем соль и почему нельзя просто SHA-256?**
    Соль против радужных таблиц; медленные алгоритмы (bcrypt, PBKDF2, Argon2) — против брутфорса. Классика для senior-вопросов про хранение паролей.

20. **Как добавить свой фильтр в цепочку?**
    Не «создать класс с `Filter`», а правильно: наследоваться от `OncePerRequestFilter` (гарантия одного вызова на запрос), зарегистрировать через `addFilterBefore/After` с указанием позиции относительно существующих фильтров. Ловушка — добавление бина `Filter` автоматически цепляет его на все URL.

21. **Что произойдёт с `SecurityContext` в асинхронной задаче или `CompletableFuture`?**
    По умолчанию — потеряется. Обсуждают `DelegatingSecurityContextAsyncTaskExecutor`, `DelegatingSecurityContextCallable` и то, что `MODE_INHERITABLETHREADLOCAL` не спасает при пуле потоков. Это «мостик» между Security и многопоточностью — Т-Банк любит такие связки.

22. **Как отдавать единый формат ошибок 401/403?**
    Кастомные `AuthenticationEntryPoint` и `AccessDeniedHandler` вместо дефолтных HTML-страниц. Проверяют, понимаете ли вы, что Security-ошибки не попадают в обычный `@ControllerAdvice`.

23. **Что проверит интервьюер через code review?**
    Типовой финтех-кейс: в сервисе есть `@Transactional` с сохранением в БД и вызовом внешнего API/антифрода внутри — ожидают, что вы заметите connection starvation и риск «данные закоммичены, а уведомление ушло/не ушло», и предложите outbox pattern или идемпотентность.

---

## Рекомендация для подготовки к Т-Банку

Сосредоточиться на связках: **JWT + stateless-масштабирование**, **`SecurityContext` + асинхронность**, **code review-задачи на атомарность БД + внешние вызовы** — это то, что реально просачивается в отзывы о собесах.
