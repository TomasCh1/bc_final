# MBK MTF AŠK Slávia Trnava – Webový informačný systém
#Github URL: https://github.com/TomasCh1/bc_final.git
Webová aplikácia pre správu basketbalového klubu.

---

## Požiadavky

- [Docker](https://www.docker.com/) a Docker Compose (v2+)
- Git

---

## Spustenie

```bash
git clone https://github.com/TomasCh1/bc_final.git
cd bc_final

cp .env.example .env
```

Spustíme aplikáciu:

```bash
docker compose up --build
```

Aplikácia beží na [http://localhost:8080](http://localhost:8080).(Ak port nieje obsadený)

---

## Zastavenie

```bash
docker compose down
```

Pre zmazanie aj databázových dát:

```bash
docker compose down -v
```

---

## Konfigurácia

Hlavné premenné v `.env` (úplný zoznam pozri v `.env.example`).

| Premenná | Popis |
|---|---|
| `POSTGRES_DB` | Názov databázy |
| `POSTGRES_USER` | Používateľ databázy |
| `POSTGRES_PASSWORD` | Heslo databázy |
| `JWT_SECRET` | Tajný kľúč pre JWT (odporúčaných 32+ znakov) |
| `JWT_EXPIRATION` | Platnosť tokenu v ms (default: 86400000 = 24h, v prod 43200000 = 12h) |
| `MAIL_HOST` | SMTP server (voliteľné) |
| `PASSWORD_RESET_EMAIL_DISABLED` | `true` = reset linky sa len logujú, maily sa neposielajú |
| `ALLOW_TEST_ENDPOINTS` | `true` = sprístupní testovacie endpointy |

---

## Technológie

- **Backend:** Java 17, Spring Boot 3.2
- **Databáza:** PostgreSQL 17
- **Frontend:** HTML, CSS, JavaScript
- **Infraštruktúra:** Docker, Docker Compose
