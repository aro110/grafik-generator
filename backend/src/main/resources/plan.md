# Plan Migracji GrafikGenerator do Aplikacji Webowej (v2)

## 1. Cel i Podejście
Celem projektu jest konwersja aplikacji konsolowej Java na skalowalną aplikację webową. 
**Główne założenia:**
- Backend: **Spring Boot 4 / Java 25**.
- Frontend: **Jednolity stos Vue 3 (Composition API)** – rezygnujemy z Reacta dla uproszczenia architektury i spójności UI.
- Trwałość danych: **PostgreSQL** w kontenerze Docker.
- Architektura: Clean Architecture z podziałem na moduły `domain`, `application`, `infrastructure` i `api`.

## 2. Etap 0: Zabezpieczenie Logiki (Golden Master)
Zanim zaczniemy modyfikować stare klasy (usuwanie Singletonów `Config` i `ShiftPool`), musimy zagwarantować, że silnik algorytmu genetycznego (GA) nadal działa identycznie.
- **Zadanie:** Implementacja testów typu "Golden Master". Dla ustalonego ziarna (seed) i danych wejściowych, algorytm musi generować identyczny wynik (snapshot). Każda refaktoryzacja musi przechodzić ten test porównawczy.

## 3. Architektura Docelowa

### 3.1 Backend (Spring Boot)
- **Modułowość:**
  - `domain`: Czysta logika GA, bez zależności od frameworków.
  - `application`: Use-case'y (start generacji, walidacja, zarządzanie konfiguracją).
  - `infrastructure`: JPA/SQL (PostgreSQL), mechanizmy asynchroniczne.
  - `api`: Kontrolery REST, DTO (Recordy), dokumentacja OpenAPI.
- **Asynchroniczność:** Generowanie grafiku uruchamiane jako zadanie `@Async`. Status (PENDING/RUNNING/SUCCESS) oraz postęp zapisywany w bazie danych.

### 3.2 SQL i Docker
- **Baza:** PostgreSQL.
- **Docker:** Plik `docker-compose.yml` definiujący bazę danych oraz ewentualne narzędzia pomocnicze (np. pgAdmin).
- **Tabele MVP:** `employees`, `sections`, `schedule_configs` (z obsługą wersji roboczych - DRAFTS), `generation_runs`, `schedules`.

### 3.3 Frontend (Vue 3)
- **Framework:** Vue 3 + Vite + Tailwind CSS.
- **Komponenty:** Shadcn Vue dla zachowania profesjonalnego i minimalistycznego wyglądu.
- **Routing:** Vue Router do obsługi Dashboardu, Kreatora i Widoku Wyników.

## 4. Model GUI i UX

### 4.1 Kreator Konfiguracji (Stepper)
- Siedmioetapowy proces (Kalendarz -> Pracownicy -> Cele -> Reguły -> Wagi -> Parametry GA -> Review).
- **Zasada Draftów:** Automatyczne zapisywanie stanu każdego kroku w `localStorage` oraz (opcjonalnie) w bazie danych, aby zapobiec utracie danych przy odświeżeniu strony.

### 4.2 Główne Ekrany
1. **Dashboard:** Status ostatnich generacji i szybki dostęp do aktywnych grafików.
2. **Kreator (Studio):** Zaawansowana konfiguracja reguł i parametrów.
3. **Monitorowanie:** Podgląd postępu generacji w czasie rzeczywistym.
4. **Widok Grafiku:** Interaktywna tabela z heatmapą pokrycia staffing% i walidacją konfliktów.

## 5. Lista TODO (Chronologiczna)

1. **[ZROBIONE]** `implement-golden-master-tests` – Stworzenie snapshotów wyników obecnego algorytmu.
2. **[ZROBIONE]** `setup-local-docker-env` – Przygotowanie `docker-compose.yml` z PostgreSQL (+ pgAdmin, healthcheck).
3. **[ZROBIONE]** `extract-ga-domain-module` – Usunięcie singletonów (`Config`, `ShiftPool`), parametryzacja przez `GenerationContext`, wstrzyknięty `Random`. *(zrealizowane razem z punktem 1 jako warunek wstępny kompilacji)*
4. **[ZROBIONE]** `design-relational-schema` – Flyway `V1__init.sql` (5 tabel: sections, employees, schedule_configs, generation_runs, schedules), encje JPA w `domain/entity/`, repozytoria w `infrastructure/repository/`, JSONB dla konfiguracji, `ddl-auto=validate`.
5. **[ZROBIONE]** `implement-spring-core-services` – DTO/request records, mappery (w tym `DomainMapper` Entity→GA), 5 serwisów (`SectionService`, `EmployeeService`, `ScheduleConfigService`, `GenerationService`, `ScheduleService`), `AsyncConfig` + `GenerationExecutor` z `@Async("generationTaskExecutor")`, `NotFoundException`.
6. **[ZROBIONE]** `implement-rest-api-v1` – `ApiResponse<T>` wrapper + `GlobalExceptionHandler` (`@RestControllerAdvice`), 5 kontrolerów REST (`/api/sections`, `/api/employees`, `/api/schedule-configs`, `/api/generation-runs`, `/api/schedules`), walidacja `@Valid` na request DTO, springdoc-openapi 2.8.9 + Swagger UI (`/swagger-ui.html`).
7. **[ZROBIONE]** `bootstrap-vue-app` – Inicjalizacja projektu Vue 3 (Vite, Router, Tailwind).
8. **[ZROBIONE]** `build-config-stepper-with-drafts` – Realizacja kreatora z mechanizmem zapisu stanu.
9. **[ZROBIONE]** `implement-schedule-visualizer` – Widok grafiku z analityką pokrycia.
10. `implement-export-and-publish` – Eksport do PDF/CSV i ostateczna publikacja grafiku.

## 6. Kluczowe Decyzje Techniczne
- **Język:** Kod i DB po angielsku, UI całkowicie po polsku.
- **Stan:** Brak Vuex/Pinia na start (KISS) – użycie `props/emits` oraz reaktywnych obiektów dla prostoty.
- **Zasada małych kroków:** AI generuje jeden moduł/warstwę na raz, po akceptacji planu działania.
