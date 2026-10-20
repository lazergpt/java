# Вопросы по Spring Security для Senior Java Developer

## Архитектура и фильтры (глубокое понимание)

- **Как устроена цепочка фильтров (Filter Chain) и в чем разница между `SecurityContextPersistenceFilter` и `UsernamePasswordAuthenticationFilter`?**
  — Проверяется понимание того, как запрос проходит через фильтры, где создается `SecurityContext` (в `PersistenceFilter`) и где именно происходит попытка аутентификации по логину/паролю (в `UsernamePasswordFilter`).
- **Как работает `DelegatingFilterProxy` и почему он необходим?**
  — Вопрос о мосте между сервлетным контейнером и контекстом Spring: фильтр регистрируется в `web.xml`, но делегирует выполнение бинам Spring.
- **В чем разница между `OncePerRequestFilter` и обычным `Filter`?**
  — `OncePerRequestFilter` гарантирует, что логика выполнится ровно один раз даже при использовании форвардов (`forward`) или включений (`include`) внутри одного HTTP-запроса.
- **Как работает `ExceptionTranslationFilter`?**
  — Это последний фильтр в цепочке, который ловит `AuthenticationException` и `AccessDeniedException`, после чего перенаправляет пользователя на страницу логина или выдает ошибку 403.

## Аутентификация и управление сессиями

- **Как реализовать Stateless-аутентификацию (JWT) и какие фильтры для этого кастомизируются?**
  — Ожидается рассказ о создании собственного фильтра (наследника `OncePerRequestFilter`), который будет парсить токен из заголовка, валидировать его и вручную класть `Authentication` в `SecurityContextHolder`.
- **Как работает `SessionManagementFilter` и стратегия `SessionFixationProtection`?**
  — Проверка знаний о защите от фиксации сессии: как Spring Security создает новую сессию (`migrateSession`) или полностью новую идентичность (`newSession`) после успешного входа.
- **В чем разница между `AuthenticationManager`, `ProviderManager` и `AuthenticationProvider`?**
  — `AuthenticationManager` — это интерфейс. `ProviderManager` — его стандартная реализация, которая итерируется по списку `AuthenticationProvider` (например, `DaoAuthenticationProvider` для БД и `LdapAuthenticationProvider` для LDAP).
- **Как работает `AuthenticationEntryPoint`?**
  — Компонент, который отвечает за реакцию на неаутентифицированный запрос (например, отправка статуса 401 для API вместо редиректа на HTML-страницу логина).

## Авторизация и защита методов

- **Как работает `SecurityMetadataSource` и `AccessDecisionManager`?**
  — Вопрос на понимание того, как Spring Security сопоставляет URL (или метод) со списком требуемых ролей (`ConfigAttribute`) и как принимается финальное решение (голосованием или жестко).
- **В чем разница между `@PreAuthorize` и `@PostAuthorize`?**
  — `@PreAuthorize` проверяет права *до* выполнения метода (доступ к аргументам), `@PostAuthorize` — *после* (позволяет проверить результат выполнения, например, что `returnObject.owner == principal.username`).
- **Как работает SpEL (Spring Expression Language) в аннотациях безопасности?**
  — Ожидается знание встроенных переменных (`authentication`, `principal`, `hasRole`, `hasAuthority`) и возможности вызывать методы бинов прямо из аннотации (например, `@PreAuthorize("@customService.canEdit(authentication, #id)")`).

## Хранение учетных данных и криптография

- **Как правильно настроить `PasswordEncoder` и почему нельзя использовать `NoOpPasswordEncoder`?**
  — Проверка понимания того, что `PasswordEncoder` — это фабрика, и для совместимости со старыми хешами используется `DelegatingPasswordEncoder`. `NoOpPasswordEncoder` допустим только для тестов.
- **Как работает `BCryptPasswordEncoder` (соль, раунды, фактор стоимости)?**
  — Вопрос о том, что соль генерируется автоматически и хранится внутри строки хеша, а параметр `strength` определяет количество итераций ($2^{strength}$).
- **Как Spring Security защищает от CSRF и почему для stateless API его часто отключают?**
  — Объяснение механизма токенов синхронизатора (Synchronizer Token Pattern) и причин отключения (у stateless API нет состояния сессии, которое можно украсть через подделку запроса, а токены JWT обычно хранятся в `localStorage` или заголовках).

## Интеграция и сложные сценарии

- **Как подружить Spring Security с Keycloak или другим Identity Provider (OAuth2 / OIDC)?**
  — Ожидается рассказ о `spring-boot-starter-oauth2-resource-server`, работе с `JwtDecoder`, извлечении ролей из `realm_access` или `resource_access` и настройке мульти-тенантности.
- **Как реализовать собственный `UserDetailsService` и какие подводные камни есть при ленивой загрузке (Lazy Initialization) ролей?**
  — Классическая проблема: если `UserDetails` загружается в одном транзакционном контексте, а роли дергаются позже (уже после закрытия сессии), упадет `LazyInitializationException`. Решается `JOIN FETCH` или `EntityGraph`.
- **Как работает `SecurityContextHolder` и какие стратегии хранения контекста (MODE_THREADLOCAL, MODE_INHERITABLETHREADLOCAL) существуют?**
  — Проверка понимания того, что по умолчанию контекст живет в `ThreadLocal` и теряется при переключении потоков в `CompletableFuture`, если не использовать `ExecutorService` с кастомной фабрикой потоков.

## Вопросы из других компаний (высокая сложность)

- **Как реализовать мульти-тенантность (multi-tenancy) в Spring Security, где у каждого арендатора своя база данных и свои пользователи?**
  — Сложная задача, требующая комбинации `AbstractRoutingDataSource` для переключения БД и динамической загрузки `UserDetailsService` в зависимости от тенанта, извлеченного из поддомена или JWT.
- **Как работает реактивный стек (WebFlux) и чем `ReactiveSecurityContextHolder` отличается от обычного?**
  — В реактивном стеке нет `ThreadLocal`, контекст пробрасывается через цепочку `Mono` и `Context` (Reactor Context).
- **Как написать собственный `AccessDecisionVoter` для сложной бизнес-логики?**
  — Например, разрешить редактирование документа только в рабочее время или если автор документа находится в той же гео-зоне, что и текущий пользователь.
- **Как Spring Security интегрируется с проприетарными SSO-решениями (например, на базе SAML 2.0)?**
  — Настройка `RelyingPartyRegistration`, работа с `Saml2WebSsoAuthenticationFilter` и валидация подписей XML-сообщений.

---
*Источники данных: открытые обсуждения, технические блоги и отчеты о собеседованиях (включая специфику финтех-сектора РФ).*