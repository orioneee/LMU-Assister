# План: профиль игрока LMU — бэкенд и мобилка

## Архитектура

```
Мобилка (Steam-логон, минтит ticket) ──ticket──▶ НАШ БЭКЕНД (Render) ──Nakama API──▶ Nakama
                                       ◀─profile──                    ◀──data──
```

Бэкенд держит всю Nakama-кухню; мобилка видит только наш стабильный REST.
Nakama меняется → деплоим бэк, апку не трогаем.

---

## Что делает бэкенд (наша сторона)

**Эндпоинты для мобилки:**
- `POST /api/auth/steam { ticket }` → обмен тикета на Nakama-сессию, выдаёт наш JWT (внутри uid).
- `GET /api/profile` → профиль + Driver/Safety Rating + бейджи (поверх `cs_user_get`).
- `GET /api/profile/stats` → карьера: гонки/победы/подиумы/поулы по классам и маркам (поверх `profile_get_stats`).
- `GET /api/profile/races` → история гонок с позициями и Δ SR/DR (поверх `results_for_user`).

**Внутри:**
- Обмен ticket → Nakama `/v2/account/authenticate/steam` (Basic = server key, `create=true`).
- Хранилище токенов per-uid (session + refresh) — не синглтон.
- Redis-кэш `uid → {session, refresh, exp}`, общий для всех инстансов.
- Lazy-refresh (без фоновых таймеров): session жив → юзаем; протух, refresh жив → рефрешим инлайн; refresh мёртв → 401 reauth.
  - Реагировать не только на свой TTL, но и когда **сам Nakama** вернул unauthenticated в середине запроса (skew/ранний отзыв) → тоже refresh, потом reauth.
- Разделение ошибок: `401 reauth` (тикет поможет) vs `403 auth_failed` (тикет отклонён / нет аккаунта — не ретраить).
- Маппинг Nakama-ответов в свой стабильный формат + наш подписанный JWT.

---

## Что требуется от мобилки

1. Steam-логон у себя (residential IP, против анти-фрода): первичный вход (пароль + Guard), хранить Steam refresh (~210 дн) + guard в Keychain/Keystore.
2. Минт тикета: silent LogOn(refresh) → **webApiTicket** (appId 2399420), НЕ классический sessionTicket — это блокер #1.
3. Логин в наш бэк: `POST /api/auth/steam { ticket }` → получить и хранить наш JWT.
4. Запросы профиля с нашим JWT (Bearer).
5. Обработка ошибок:
   - `401 reauth` → молча перевыпустить тикет и повторить запрос 1 раз (cap!).
   - `403 auth_failed` → не ретраить, показать юзеру.
6. Полный релогин (пароль+2FA) — только когда умрёт Steam refresh (~раз в полгода).

---

## Блокеры до кода (проверить на живом)

| # | Вопрос | От кого |
|---|--------|---------|
| 1 | webApiTicket vs sessionTicket + правильный identity | мобилка минтит оба → бэк проверяет на Nakama |
| 2 | реальный TTL Nakama refresh (наш код: ~60 мин; решает выгоду lazy-refresh) | замер на любом валидном тикете |
| 3 | create=true для юзера, не заходившего в игру | тест на чистом аккаунте |

---

## Порядок

1. **Сейчас:** мобилка — Steam-логон + webApiTicket + хендлер 401/403.
2. **Параллельно:** мобилка даёт валидный тикет → закрываем блокеры #1 (тип) и #2 (TTL).
3. **После блокеров + OK на репозиторий:** бэкенд — per-uid токены, Redis, 4 эндпоинта, lazy-refresh.
4. **Финал:** проверка create (#3).

> Репозиторий бэка не трогаем до явного «погнали».

---

## Статус мобилки (готово)

- Steam-логон на Android/JVM через JavaSteam (`SteamAuthClient`): credential login + Steam Guard, refresh/access токены, **оба** типа тикета (`sessionTicket` + `webApiTicket`).
- Персистентная сессия: EncryptedSharedPreferences (Android) / JSON-файл (JVM); авто-восстановление при старте.
- Профиль минтит и показывает **оба** тикета для appId 2399420 — для закрытия блокера #1.
- `SteamBackendApi`: `POST /api/v2/auth/steam` (webApiTicket → наш токен), `GET /profile|stats|races` с Bearer.
- VM-оркестрация: после логина auth/steam → profile; 401 `reauth` → перевыпуск тикета + повтор 1 раз; 403 `auth_failed` → терминально; ошибки видны на экране профиля (uid + сырой JSON профиля).
- Наш backend-токен держится in-memory (на рестарте перевыпускается из persisted Steam refresh). `WEBAPI_IDENTITY = ""` пока (меняется одной константой).
- Требуется: `backend.url` в `local.properties` указывает на живой Render-бэк (…/api/v2).
