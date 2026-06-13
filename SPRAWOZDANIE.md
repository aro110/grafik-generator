# Sprawozdanie z projektu: Grafik Generator

## 1. Informacje ogolne

**Nazwa projektu:** Grafik Generator  
**Typ projektu:** aplikacja webowa full-stack do generowania miesiecznych grafikow pracy  
**Charakter projektu:** projekt indywidualny  
**Glowny cel:** automatyzacja tworzenia grafikow pracy dla sklepu lub firmy podzielonej na sekcje, z uwzglednieniem pracownikow, dni wolnych, urlopow, godzin otwarcia, wymaganej obsady oraz reguly ograniczajace liczbe dni pracy pod rzad.

Aplikacja pozwala uzytkownikowi skonfigurowac miesiac, sekcje, pracownikow, cele obsady i parametry algorytmu, a nastepnie wygenerowac grafiki pracy. Wyniki mozna przegladac w interfejsie webowym, porownywac z wymaganym poziomem obsady, eksportowac do pliku Excel, publikowac oraz opcjonalnie poprawiac przy pomocy integracji z modelem Gemini.

## 2. Problem, ktory rozwiazuje aplikacja

Ręczne tworzenie grafiku pracy jest czasochlonne, poniewaz trzeba jednoczesnie pilnowac wielu ograniczen:

- miesiac ma rozna liczbe dni,
- czesc dni moze byc swietami lub niedzielami niehandlowymi,
- pracownicy maja indywidualne dni wolne i urlopy,
- kazdy pracownik ma zaplanowana liczbe godzin i dni pracy,
- sklep ma inne godziny otwarcia w rozne dni tygodnia,
- w poszczegolne dni wymagana jest okreslona liczba pracownikow,
- nie powinno byc zbyt wielu dni pracy z rzedu,
- zmiany powinny byc rozlozone mozliwie rownomiernie,
- dla kilku sekcji trzeba otrzymac spojny obraz obsady calego sklepu.

Projekt rozwiazuje ten problem przez polaczenie formularza konfiguracyjnego, walidacji danych, algorytmu genetycznego oraz eksportu wyniku do formatu przydatnego operacyjnie.

## 3. Technologie

### Backend

Backend zostal wykonany w technologii **Java + Spring Boot**. Projekt Maven znajduje sie w katalogu glownym, a kod backendu w `backend/src/main/java`.

Najwazniejsze technologie backendowe:

- **Java 25** - jezyk implementacji backendu,
- **Spring Boot 4.0.5** - glowny framework aplikacji,
- **Spring Web MVC** - REST API,
- **Spring Data JPA / Hibernate** - komunikacja z baza danych,
- **PostgreSQL** - relacyjna baza danych,
- **Flyway** - migracje schematu bazy,
- **Lombok** - ograniczenie kodu boilerplate,
- **Apache POI** - generowanie plikow Excel `.xls`,
- **springdoc-openapi** - dokumentacja API przez Swagger UI,
- **Google GenAI SDK** - opcjonalna integracja z Gemini do proponowania zmian w grafikach.

### Frontend

Frontend znajduje sie w katalogu `frontend/grafik-generator` i zostal wykonany jako aplikacja SPA.

Najwazniejsze technologie frontendowe:

- **Vue 3** - framework interfejsu uzytkownika,
- **Vite** - narzedzie developerskie i bundler,
- **Vue Router** - routing po widokach aplikacji,
- **Tailwind CSS 4** - stylowanie,
- **lucide-vue-next** - ikony,
- **radix-vue** - komponenty pomocnicze,
- **xlsx** - biblioteka arkuszy po stronie frontendu, obecna w zaleznosciach.

### Infrastruktura

Projekt zawiera konfiguracje uruchomienia kontenerowego:

- `docker-compose.yml` - srodowisko lokalne: PostgreSQL, backend, frontend, pgAdmin,
- `docker-compose.prod.yml` - uproszczona konfiguracja produkcyjna,
- `Dockerfile` - obraz backendu,
- `frontend/grafik-generator/Dockerfile` - obraz frontendu,
- `frontend/grafik-generator/nginx.conf` - serwowanie aplikacji Vue przez Nginx i proxy `/api` do backendu.

## 4. Struktura projektu

Najwazniejsze katalogi:

```text
.
├── backend
│   └── src
│       ├── main
│       │   ├── java/pl/grafik/grafik_generator
│       │   │   ├── api
│       │   │   ├── application
│       │   │   ├── domain
│       │   │   └── infrastructure
│       │   └── resources
│       │       ├── application.properties
│       │       └── db/migration
│       └── test
├── frontend/grafik-generator
│   ├── src
│   │   ├── components
│   │   ├── composables
│   │   ├── router
│   │   ├── services
│   │   ├── utils
│   │   └── views
├── docker-compose.yml
├── docker-compose.prod.yml
├── Dockerfile
├── pom.xml
└── SPRAWOZDANIE.md
```

Backend jest podzielony warstwowo:

- `api` - kontrolery REST, wspolny format odpowiedzi, obsluga wyjatkow i OpenAPI,
- `application` - serwisy aplikacyjne, DTO, mappery, walidacja i logika przeplywow,
- `domain` - wlasciwa logika domenowa i algorytm generowania grafiku,
- `infrastructure` - repozytoria Spring Data JPA.

Frontend jest podzielony funkcjonalnie:

- `views` - glowne ekrany aplikacji,
- `components` - komponenty UI, layoutu, kreatora i grafiku,
- `services` - komunikacja z REST API,
- `utils` - budowanie i normalizacja draftu kreatora,
- `router` - definicja tras.

## 5. Glowny przeplyw dzialania aplikacji

Typowy scenariusz uzycia wyglada nastepujaco:

1. Uzytkownik otwiera dashboard.
2. Wybiera utworzenie nowego grafiku.
3. W kreatorze podaje:
   - miesiac grafiku,
   - godziny otwarcia dla dni tygodnia,
   - swieta i niedziele handlowe,
   - sekcje sklepu,
   - pracownikow w kazdej sekcji,
   - liczbe godzin i dni pracy kazdego pracownika,
   - dni wolne i urlopy,
   - wymagania obsadowe,
   - reguly zmian,
   - parametry algorytmu genetycznego.
4. Frontend waliduje formularz i wysyla dane do endpointu `POST /api/generation-runs/full-start`.
5. Backend zapisuje konfiguracje, sekcje i pracownikow w bazie danych.
6. Backend tworzy grupe generacji oraz osobne uruchomienia generacji dla kazdej sekcji.
7. Generowanie odbywa sie asynchronicznie w tle.
8. Frontend odpytuje backend o status generacji co 2 sekundy.
9. Po zakonczeniu generacji uzytkownik przechodzi do podsumowania sklepu.
10. Uzytkownik moze:
    - przegladac grafiki sekcji,
    - sprawdzac pokrycie obsady,
    - eksportowac pojedynczy grafik lub cala grupe do Excela,
    - opublikowac grafik,
    - wygenerowac ponownie,
    - poprosic AI o propozycje poprawek.

## 6. Backend

### 6.1. Aplikacja Spring Boot

Punktem startowym backendu jest klasa `GrafikGeneratorApplication`. Aplikacja nasluchuje domyslnie na porcie `8081`, co jest ustawione w `application.properties`:

```properties
server.port=${SERVER_PORT:8081}
```

Konfiguracja korzysta ze zmiennych srodowiskowych, dzieki czemu projekt mozna uruchamiac lokalnie i kontenerowo bez zmiany kodu.

Najwazniejsze ustawienia:

- polaczenie z PostgreSQL,
- walidacja schematu przez Hibernate (`ddl-auto=validate`),
- migracje Flyway,
- konfiguracja modelu Gemini,
- Swagger UI pod `/swagger-ui.html`.

### 6.2. Format odpowiedzi API

Wszystkie standardowe odpowiedzi REST sa opakowane w rekord `ApiResponse<T>`, ktory zawiera:

- `success` - informacja, czy operacja sie powiodla,
- `data` - dane odpowiedzi,
- `message` - komunikat,
- `timestamp` - czas odpowiedzi.

Dzieki temu frontend moze jednolicie obslugiwac sukcesy i bledy.

### 6.3. Obsluga bledow

Klasa `GlobalExceptionHandler` obsluguje najwazniejsze typy bledow:

- `NotFoundException` jako HTTP 404,
- bledy walidacji jako HTTP 400,
- `IllegalArgumentException` jako HTTP 400,
- `IllegalStateException` jako HTTP 409,
- pozostale bledy jako HTTP 500.

Jest to wazne, poniewaz aplikacja operuje na wielu danych wejsciowych i musi jasno informowac uzytkownika, dlaczego generowanie nie moglo zostac wykonane.

### 6.4. Kontrolery REST

Najwazniejsze kontrolery:

| Kontroler | Sciezka | Odpowiedzialnosc |
|---|---|---|
| `DashboardController` | `/api/dashboard` | dane ekranu glownego |
| `SectionController` | `/api/sections` | zarzadzanie sekcjami |
| `EmployeeController` | `/api/employees` | zarzadzanie pracownikami |
| `ScheduleConfigController` | `/api/schedule-configs` | konfiguracje generowania |
| `GenerationRunController` | `/api/generation-runs` | start, status i usuwanie generacji |
| `ScheduleController` | `/api/schedules` | podglad, eksport, publikacja i AI dla grafikow |

Przyklady endpointow:

- `GET /api/dashboard` - pobranie ostatnich generacji i opublikowanych grafikow,
- `POST /api/generation-runs/full-start` - utworzenie pelnej konfiguracji i start generowania,
- `GET /api/generation-runs/groups/{id}` - pobranie statusu grupy generacji,
- `GET /api/schedules/{id}/details` - szczegoly grafiku,
- `GET /api/schedules/summary/{groupId}` - podsumowanie sklepu dla grupy grafikow,
- `GET /api/schedules/{id}/export` - eksport pojedynczego grafiku,
- `GET /api/schedules/summary/{groupId}/export` - eksport calego sklepu,
- `POST /api/schedules/{id}/publish` - publikacja grafiku,
- `POST /api/schedules/{id}/ai-edits` - propozycja poprawki AI dla grafiku,
- `POST /api/schedules/summary/{groupId}/ai-edits` - propozycja poprawki AI dla calej grupy.

## 7. Model danych i baza PostgreSQL

Schemat bazy jest tworzony i rozwijany przez migracje Flyway w katalogu `backend/src/main/resources/db/migration`.

Najwazniejsze tabele:

### `sections`

Przechowuje sekcje, np. dzialy sklepu. Sekcja ma `id`, `name`, `created_at`, `updated_at`.

### `employees`

Przechowuje pracownikow przypisanych do sekcji. Najwazniejsze pola:

- `name`, `surname`,
- `section_id`,
- `total_hours`,
- `total_days`,
- `days_off` jako `jsonb`,
- `vacations` jako `jsonb`.

Dni wolne i urlopy sa zapisane jako listy indeksow dni miesiaca. W kodzie indeksy sa liczone od zera, czyli pierwszy dzien miesiaca ma indeks `0`.

### `schedule_configs`

Przechowuje konfiguracje generowania grafiku. Czesc danych jest zapisana jako `jsonb`, poniewaz sa to struktury zlozone:

- `store_hours` - godziny otwarcia,
- `staffing_targets` - cele obsady,
- `calendar` - miesiac, swieta, niedziele handlowe,
- `shift_rules` - reguly zmian,
- `vacation_config` - sposob liczenia urlopow,
- `ga_parameters` - parametry algorytmu genetycznego.

### `generation_run_groups`

Reprezentuje jedna zbiorcza generacje grafiku. Grupa moze zawierac kilka uruchomien, np. po jednym dla kazdej sekcji.

Status grupy moze miec wartosci:

- `PENDING`,
- `RUNNING`,
- `SUCCESS`,
- `PARTIAL`,
- `FAILED`.

### `generation_runs`

Reprezentuje pojedyncze uruchomienie algorytmu dla konkretnej sekcji. Zawiera:

- konfiguracje,
- sekcje,
- grupe,
- seed losowosci,
- status,
- postep,
- czas rozpoczecia i zakonczenia,
- ewentualny komunikat bledu.

### `schedules`

Przechowuje wynik generacji:

- `fitness` - wartosc funkcji celu,
- `genes` - macierz dlugosci zmian,
- `shift_starts` - macierz godzin rozpoczecia zmian,
- `protected_overrides` - reczne wyjatki chronionych dni,
- `published` i `published_at`.

Pole `genes` jest macierza `pracownik x dzien`. Wartosc `0` oznacza brak pracy, a wartosc dodatnia oznacza dlugosc zmiany w godzinach, np. `8`.

Pole `shift_starts` ma taki sam rozmiar jak `genes` i okresla godzine rozpoczecia zmiany.

### `schedule_ai_edits`

Tabela przechowuje propozycje zmian przygotowane przez AI:

- instrukcje uzytkownika,
- uzyty model,
- status propozycji,
- liste zmian,
- roznice miedzy grafikiem oryginalnym i proponowanym,
- ostrzezenia i bledy,
- proponowane macierze `genes` i `shift_starts`,
- powiazany zaakceptowany grafik lub zaakceptowana grupa.

## 8. Konfiguracja grafiku

Konfiguracja grafiku sklada sie z kilku obiektow domenowych.

### `CalendarConfig`

Zawiera:

- miesiac (`YearMonth`),
- swieta,
- niedziele handlowe.

Klasa potrafi:

- zwrocic liczbe dni w miesiacu,
- obliczyc date dla indeksu dnia,
- sprawdzic, czy dzien jest zamkniety,
- zwrocic liste zamknietych dni.

Dzien jest zamkniety, jezeli jest swietem albo jest niedziela, ktora nie zostala oznaczona jako handlowa.

### `StoreHours`

Przechowuje godziny otwarcia dla dni tygodnia. Dla kazdego dnia mozna okreslic godzine otwarcia i zamkniecia. Konstruktor pilnuje, aby zamkniecie bylo pozniej niz otwarcie.

### `StaffingTargets`

Okresla wymagana obsade. System wspiera dwa tryby:

- procent pracownikow sekcji, np. 60%,
- konkretna liczba osob, np. 4 osoby.

Dodatkowo mozna wskazac godziny szczytu (`peak hours`), ktore sa pozniej uzywane przy przypisywaniu godzin rozpoczecia zmian.

### `ShiftRules`

Zawiera reguly zmian:

- dostepne dlugosci zmian, np. `[6, 8]`,
- maksymalna liczba dni pracy pod rzad,
- czy przyznawac wolny weekend,
- maksymalna liczba osob zaczynajacych zmiane o tej samej godzinie.

### `VacationConfig`

Okresla, jak urlopy wplywaja na planowane godziny i dni pracy:

- ktore dni tygodnia sa dniami roboczymi dla urlopu,
- ile godzin odejmowac za dzien urlopu,
- czy pomijac swieta przy odejmowaniu urlopu.

### `GaParameters`

Parametry algorytmu genetycznego:

- `populationSize` - liczba osobnikow w populacji,
- `generations` - liczba pokolen,
- `eliteCount` - ilu najlepszych osobnikow przechodzi dalej bez zmian,
- `tournamentSize` - rozmiar turnieju selekcji,
- `mutationRate` - prawdopodobienstwo mutacji.

## 9. Algorytm generowania grafiku

Najwazniejsza czesc projektu znajduje sie w pakiecie `domain`.

Generowanie grafiku sklada sie z kilku etapow:

1. Utworzenie kontekstu generacji.
2. Zamiana encji bazy danych na modele domenowe.
3. Wygenerowanie pul mozliwych kombinacji zmian dla pracownikow.
4. Utworzenie populacji startowej.
5. Wielokrotna ewolucja populacji algorytmem genetycznym.
6. Wybor najlepszego grafiku.
7. Przypisanie godzin rozpoczecia zmian.
8. Zapis wyniku w bazie.

### 9.1. Pula zmian

Klasa `ShiftPool` generuje wszystkie mozliwe kombinacje dlugosci zmian dla danej liczby godzin i dni pracy.

Przyklad: jezeli pracownik ma przepracowac 160 godzin w 20 dni, a dostepne zmiany maja 6 i 8 godzin, system szuka kombinacji zmian, ktore spelniaja te warunki.

Generowanie odbywa sie metoda backtrackingu. Nastepnie kombinacje sa grupowane przez `ShiftClusterer` wedlug odchylenia standardowego:

- niska zmiennosc,
- srednia zmiennosc,
- wysoka zmiennosc.

Z kazdej grupy wybierana jest ograniczona liczba reprezentantow. Dzieki temu algorytm nie musi sprawdzac zbyt wielu bardzo podobnych kombinacji.

### 9.2. Model pracownika

Klasa domenowa `Employee` przechowuje:

- imie,
- nazwisko,
- sekcje,
- liczbe godzin,
- liczbe dni,
- pule zmian,
- dni wolne,
- urlopy.

W konstruktorze pracownika wykonywane sa wazne walidacje:

- czy da sie ulozyc wymagana liczbe godzin z dostepnych zmian,
- czy liczba godzin miesci sie miedzy minimum i maksimum wynikajacym z liczby dni,
- czy pracownik ma wystarczajaco duzo dostepnych dni pracy,
- czy urlopy powinny zmniejszyc plan godzin i dni.

Jezeli wlaczona jest opcja wolnego weekendu, system probuje dodac pracownikowi wolna sobote w sposob rozkladany pomiedzy pracownikow danej sekcji.

### 9.3. Reprezentacja grafiku

Klasa `Schedule` reprezentuje grafik jako macierz:

```text
pracownik x dzien miesiaca
```

Kazda komorka zawiera:

- `0` - pracownik nie pracuje,
- liczbe dodatnia - dlugosc zmiany w godzinach.

Przyklad uproszczonego wiersza:

```text
8 8 0 8 0 8 8 ...
```

Oznacza to, ze pracownik pracuje po 8 godzin w wybrane dni, a w dni z wartoscia `0` ma wolne.

### 9.4. Funkcja celu

Jakosc grafiku jest oceniana przez metode `calculateFitness()`. Im mniejsza wartosc `fitness`, tym lepszy grafik.

System nalicza kary za:

- brak wystarczajacej liczby godzin pracy w dniu wzgledem godzin otwarcia,
- zbyt mala lub zbyt duza liczbe pracownikow wzgledem celu obsady,
- prace w dni zamkniete,
- przekroczenie maksymalnej liczby dni pracy z rzedu,
- nierownomierny rozklad pracy w czterech czesciach miesiaca.

Najwieksza kara dotyczy braku pokrycia godzin otwarcia, poniewaz jest to krytyczne wymaganie operacyjne.

### 9.5. Algorytm genetyczny

Algorytm genetyczny jest zaimplementowany w klasie `Population`.

Proces:

1. Tworzona jest poczatkowa populacja losowych grafikow.
2. Kazdy grafik ma obliczana wartosc `fitness`.
3. Najlepsze grafiki sa zachowywane przez elitaryzm.
4. Rodzice sa wybierani przez selekcje turniejowa.
5. Z rodzicow powstaje potomek przez krzyzowanie wierszy macierzy.
6. Potomek moze zostac zmutowany.
7. Proces powtarza sie przez okreslona liczbe pokolen.
8. Jezeli znaleziono grafik z `fitness == 0`, algorytm moze zakonczyc sie szybciej.

Krzyzowanie polega na tym, ze dla kazdego pracownika potomek bierze caly wiersz grafiku od jednego z rodzicow. Mutacja zamienia miejscami dzien wolny i dzien pracujacy u pracownika, ale nie narusza dni niedostepnych ani dni zamknietych.

### 9.6. Przypisywanie godzin rozpoczecia zmian

Po wybraniu najlepszego grafiku aplikacja zna dlugosci zmian, ale musi jeszcze okreslic godziny rozpoczecia, np. `10:00`.

Odpowiada za to klasa `ShiftAssigner`.

Mechanizm:

- dla danego dnia pobierani sa pracownicy pracujacy,
- najpierw rozpatrywane sa dluzsze zmiany,
- system szuka najlepszego planu godzinowego,
- punktuje pokrycie godzin otwarcia,
- dodatkowo punktuje pokrycie godzin szczytu,
- karze zbyt duze nakladanie sie zmian,
- karze przekroczenie limitu osob zaczynajacych o tej samej godzinie,
- probuje rowniej rozkladac godziny szczytu miedzy pracownikow.

Zastosowano podejscie typu beam search, czyli przechowywana jest ograniczona liczba najlepszych czesciowych planow. Pozwala to uniknac pelnego przeszukiwania wszystkich kombinacji.

## 10. Generowanie asynchroniczne

Generowanie grafikow moze trwac dluzej, dlatego backend wykonuje je asynchronicznie.

Klasa `AsyncConfig` definiuje executor:

- `corePoolSize = 2`,
- `maxPoolSize = 4`,
- `queueCapacity = 50`,
- prefiks watkow `gen-`.

`GenerationServiceImpl` tworzy rekordy generacji w bazie, a po zatwierdzeniu transakcji uruchamia `GenerationExecutor`.

`GenerationExecutor`:

1. ustawia status runa na `RUNNING`,
2. pobiera konfiguracje i sekcje,
3. buduje domenowy `GenerationContext`,
4. uruchamia algorytm genetyczny,
5. przypisuje godziny rozpoczecia zmian,
6. zapisuje `ScheduleEntity`,
7. ustawia status `SUCCESS`,
8. aktualizuje status grupy.

W przypadku bledu run otrzymuje status `FAILED`, a komunikat bledu jest zapisywany w bazie. Status grupy moze wtedy zmienic sie na `FAILED` lub `PARTIAL`.

## 11. Frontend

### 11.1. Routing

Frontend definiuje cztery glowne trasy:

| Trasa | Widok | Znaczenie |
|---|---|---|
| `/` | `DashboardView` | ekran glowny |
| `/create` | `CreatorView` | kreator grafiku |
| `/schedule/:id` | `ScheduleView` | szczegoly pojedynczego grafiku |
| `/summary/:id` | `StoreSummaryView` | podsumowanie grupy grafikow |

### 11.2. Dashboard

Dashboard pokazuje:

- ostatnie generacje,
- statusy generacji,
- postep,
- liczbe zakonczonych generacji,
- liczbe opublikowanych grafikow,
- przycisk tworzenia nowego grafiku,
- mozliwosc usuniecia grupy generacji.

Dane sa pobierane przez `dashboardService.js` z endpointu `/api/dashboard`.

### 11.3. Kreator grafiku

Kreator w `CreatorView.vue` jest podzielony na siedem krokow:

1. **Kalendarz** - miesiac, godziny otwarcia, swieta i niedziele handlowe.
2. **Pracownicy** - sekcje, pracownicy, godziny, dni pracy, dni wolne i urlopy.
3. **Cele obsady** - procent lub konkretna liczba pracownikow na dzien tygodnia oraz godziny szczytu.
4. **Reguly** - dlugosci zmian i ograniczenia.
5. **Priorytet** - preset optymalizacji.
6. **Parametry GA** - rozmiar populacji, liczba generacji, elita, turniej, mutacja, seed.
7. **Podsumowanie** - walidacja i start generowania.

Draft kreatora jest zapisywany w `localStorage` pod kluczem `grafik_generator_draft_v2`. Dzieki temu odswiezenie strony nie usuwa wprowadzonych danych.

### 11.4. Komunikacja z backendem

Frontend korzysta z funkcji w katalogu `services`:

- `creatorService.js` - tworzenie pelnej konfiguracji i polling generacji,
- `dashboardService.js` - dashboard i usuwanie generacji,
- `scheduleService.js` - pobieranie szczegolow, eksport, publikacja i AI.

W trybie developerskim Vite przekierowuje `/api` na `http://localhost:8081`. W Dockerze analogiczna konfiguracja znajduje sie w Nginx.

### 11.5. Podglad grafiku

Widok pojedynczego grafiku (`ScheduleView`) pokazuje:

- nazwe sekcji,
- miesiac,
- status publikacji,
- srednie pokrycie obsady,
- fitness,
- widok tabelaryczny,
- widok kalendarzowy,
- eksport Excel,
- publikacje,
- panel propozycji AI.

### 11.6. Podsumowanie sklepu

Widok `StoreSummaryView` prezentuje cala grupe grafikow. Jest to wazne, gdy konfiguracja obejmuje wiele sekcji.

Widok pozwala:

- zobaczyc pokrycie sklepu dzien po dniu,
- przegladac grafiki poszczegolnych sekcji,
- eksportowac calosc do Excela,
- wygenerowac nowa wersje,
- zaproponowac poprawki AI dla calej grupy.

## 12. Eksport do Excela

Eksport jest realizowany po stronie backendu przez `ScheduleExcelExporter` z wykorzystaniem Apache POI.

Dostepne sa dwa typy eksportu:

- eksport pojedynczego grafiku,
- eksport calej grupy grafikow.

Eksport grupowy tworzy skoroszyt `.xls` z arkuszami m.in.:

- `roboczy`,
- `Grafik_Sala_Sprzedazy`,
- `Obsada`,
- `Szkolenia`,
- `Zmiany 8`.

W pliku znajduja sie informacje o godzinach rozpoczecia, zakonczenia, liczbie godzin, urlopach, sekcjach i podsumowaniu obsady.

## 13. Integracja z AI

Projekt zawiera integracje z Gemini przez zaleznosc `com.google.genai:google-genai`.

Konfiguracja:

```properties
gemini.api-key=${GEMINI_API_KEY:${GOOGLE_API_KEY:}}
gemini.model=${GEMINI_MODEL:gemini-3.5-flash}
gemini.max-output-tokens=${GEMINI_MAX_OUTPUT_TOKENS:65536}
```

AI nie nadpisuje grafiku automatycznie. Przeplyw jest bezpieczniejszy:

1. Uzytkownik wpisuje instrukcje.
2. Backend buduje prompt na podstawie aktualnego grafiku.
3. Gemini zwraca propozycje zmian.
4. Backend zapisuje propozycje jako `ScheduleAiEditEntity`.
5. Frontend pokazuje roznice.
6. Uzytkownik moze zaakceptowac albo odrzucic propozycje.
7. Po akceptacji powstaje nowy grafik albo nowa grupa grafikow.

W projekcie uwzgledniono rowniez opcje chronienia urlopow i dni wolnych. Uzytkownik moze zdecydowac, czy AI moze proponowac prace w takich dniach jako wyjatek.

## 14. Walidacja danych

Walidacja jest wykonywana na kilku poziomach:

- frontend sprawdza podstawowe braki w formularzu,
- DTO backendowe korzystaja z adnotacji walidacyjnych,
- `ScheduleConfigValidator` sprawdza spojnosci konfiguracji,
- klasy domenowe waliduja reguly logiczne.

Przyklady walidacji:

- niedziela handlowa musi faktycznie przypadac w niedziele,
- swieto musi nalezec do wybranego miesiaca,
- jezeli sa niedziele handlowe, trzeba ustawic godziny otwarcia dla niedzieli,
- procent obsady musi byc w zakresie 0-100,
- godzina zamkniecia musi byc po godzinie otwarcia,
- lista dlugosci zmian nie moze byc pusta,
- liczba dni pracy pracownika nie moze przekroczyc dostepnych dni.

## 15. Testy i weryfikacja

Projekt zawiera testy w `backend/src/test/java`.

### `GoldenMasterTest`

Test sprawdza stabilnosc algorytmu genetycznego dla stalego zestawu danych i stalego seeda `42`.

Test:

- buduje przykladowy kontekst generacji,
- tworzy sekcje z dwoma pracownikami,
- uruchamia populacje,
- serializuje najlepszy grafik,
- porownuje wynik z plikiem `backend/src/test/resources/golden-master/snapshot.txt`.

Taki test jest przydatny dla algorytmow heurystycznych, bo pozwala wykryc niezamierzone zmiany w zachowaniu generatora.

### `GrafikGeneratorApplicationTests`

Test sprawdza, czy kontekst Spring Boot potrafi sie zaladowac. W tescie wylaczono automatyczna konfiguracje bazy danych, JPA i Flyway, a repozytoria sa zastapione proxy testowymi.

### Proba uruchomienia testow

Podczas przygotowywania sprawozdania podjeto probe uruchomienia:

```bash
./mvnw test
npm run build
```

W aktualnym srodowisku obie komendy zostaly zatrzymane przez blad dostepu do Docker API:

```text
permission denied while trying to connect to the docker API at unix:///var/run/docker.sock
```

Nie jest to blad odnaleziony w kodzie projektu, tylko ograniczenie srodowiska wykonawczego, w ktorym wykonywana byla analiza. Na lokalnym komputerze z poprawnie skonfigurowanym Dockerem albo bez takiego ograniczenia nalezy uruchomic te komendy ponownie.

## 16. Uruchamianie projektu lokalnie

### Wymagania

- Java 25,
- Maven Wrapper jest dolaczony jako `./mvnw`,
- Node.js zgodny z `frontend/grafik-generator/package.json`, czyli `^20.19.0 || >=22.12.0`,
- Docker i Docker Compose,
- PostgreSQL, jezeli backend ma byc uruchamiany bez Dockera.

### Najprostsze uruchomienie przez Docker Compose

Z katalogu glownego projektu:

```bash
docker compose up --build
```

Po uruchomieniu:

- frontend: `http://localhost:3000`,
- backend: `http://localhost:8081`,
- Swagger UI: `http://localhost:8081/swagger-ui.html`,
- pgAdmin: `http://localhost:5050`.

Dane pgAdmin z `docker-compose.yml`:

- email: `admin@grafik.example.com`,
- haslo: `admin`.

### Uruchomienie backendu bez Dockera

Najpierw trzeba miec dzialajace PostgreSQL z baza:

- baza: `springappdb`,
- uzytkownik: `springuser`,
- haslo: `springpassword`.

Nastepnie:

```bash
./mvnw spring-boot:run
```

Backend wystartuje domyslnie na porcie `8081`.

### Uruchomienie frontendu bez Dockera

```bash
cd frontend/grafik-generator
npm install
npm run dev
```

Vite uruchomi aplikacje developerska. Proxy `/api` przekieruje zapytania do backendu na `http://localhost:8081`.

## 17. Wykorzystanie AI podczas pracy nad projektem

Podczas realizacji projektu wykorzystalem narzedzia sztucznej inteligencji jako wsparcie programistyczne i konsultacyjne. AI nie bylo traktowane jako samodzielny autor projektu, ale jako pomoc w przyspieszeniu pracy, analizie problemow, porzadkowaniu kodu oraz szukaniu rozwiazan dla konkretnych funkcjonalnosci.

Najwieksza rola AI pojawila sie przy tworzeniu i dopracowywaniu frontendu. Interfejs aplikacji sklada sie z wielu widokow, formularzy i komponentow: dashboardu, kreatora grafiku, podgladu pojedynczego grafiku, podsumowania sklepu, tabel, widoku kalendarzowego, komunikatow, dialogow potwierdzenia oraz elementow UI. AI pomagalo w przygotowaniu struktury komponentow Vue, rozdzieleniu widokow na mniejsze czesci, pisaniu logiki obslugi formularzy, komunikacji z backendem oraz w dopracowaniu zachowania interfejsu w sytuacjach takich jak ladowanie danych, blad, pusta lista czy trwajace generowanie. Dzieki temu frontend jest spojny, czytelny i lepiej oddziela warstwe prezentacji od logiki komunikacji z API.

AI pomagalo rowniez przy pisaniu wybranych fragmentow kodu backendowego. Dotyczylo to m.in. porzadkowania serwisow aplikacyjnych, przygotowywania DTO, mapowania danych, obslugi odpowiedzi API, dopracowania walidacji oraz rozwiazywania problemow pojawiajacych sie podczas laczenia kolejnych elementow systemu. W praktyce AI bylo uzywane podobnie jak bardzo szybki konsultant techniczny: proponowalo fragmenty implementacji, wskazywalo potencjalne bledy, pomagalo interpretowac komunikaty kompilatora i sugerowalo, gdzie szukac przyczyny problemu.

Istotnym obszarem wsparcia byla integracja z API Gemini. AI pomagalo zaplanowac sposob wprowadzenia funkcji "popraw z AI", czyli takiej, w ktorej model nie zmienia grafiku bezposrednio, lecz przygotowuje propozycje widoczna dla uzytkownika. Pomoc obejmowala zaprojektowanie przeplywu: przygotowanie instrukcji, wyslanie danych grafiku do modelu, odebranie propozycji zmian, zapisanie jej w bazie, pokazanie roznic w interfejsie oraz dopiero pozniejsze zaakceptowanie albo odrzucenie przez uzytkownika. Takie podejscie jest bezpieczniejsze, poniewaz decyzja koncowa pozostaje po stronie operatora aplikacji.

AI pomagalo takze przy rozwiazywaniu problemow technicznych, ktore pojawialy sie w trakcie pracy. Przy projekcie full-stack problemy czesto nie dotycza tylko jednego pliku, ale przeplywu miedzy kilkoma warstwami: formularzem we frontendzie, requestem HTTP, DTO w backendzie, encja JPA, baza danych i pozniejszym widokiem wyniku. W takich sytuacjach AI pomagalo analizowac caly lancuch danych, wskazywac niespojnosci nazw pol, problemy z formatem dat, indeksowaniem dni miesiaca, obsluga wartosci pustych oraz komunikacja miedzy frontendem i backendem.

Wykorzystanie AI bylo pomocne rowniez przy refaktoryzacji i organizacji kodu. Projekt stopniowo rozrastal sie o nowe funkcje, takie jak obsluga wielu sekcji, podsumowanie sklepu, eksport grupowy, urlopy, ponowne generowanie oraz AI edits. W takich momentach AI pomagalo porzadkowac strukture plikow, wydzielac odpowiedzialnosci do serwisow, komponentow i mapperow oraz utrzymywac czytelny podzial na warstwy. Dzieki temu projekt nie ogranicza sie do pojedynczego skryptu generujacego grafik, ale ma forme pelnej aplikacji z osobnymi warstwami domeny, aplikacji, API, infrastruktury i interfejsu uzytkownika.

AI wspieralo rowniez tworzenie tresci opisowych i dokumentacyjnych, w tym porzadkowanie sposobu opisu architektury, algorytmu i instrukcji uruchomienia. Bylo to szczegolnie przydatne przy przygotowywaniu sprawozdania, poniewaz projekt zawiera wiele elementow technicznych, ktore trzeba opisac w sposob zrozumialy: baze danych, migracje, algorytm genetyczny, funkcje celu, generowanie asynchroniczne, komunikacje REST, frontend i eksport Excel.

Podsumowujac, AI pelnilo role narzedzia wspomagajacego programowanie: pomagalo pisac czesc kodu, przygotowac frontend, rozwiazywac problemy, konsultowac decyzje techniczne, uporzadkowac strukture projektu oraz wdrozyc integracje z API Gemini. Ostateczne decyzje dotyczace funkcjonalnosci, kierunku projektu, wyboru rozwiazan i sposobu dzialania aplikacji pozostawaly po mojej stronie.

## 18. Podsumowanie

Grafik Generator jest kompletna aplikacja full-stack, ktora rozwiazuje praktyczny problem planowania pracy. Backend odpowiada za trwale przechowywanie konfiguracji, walidacje, asynchroniczne generowanie, eksport i integracje AI. Frontend zapewnia wygodny kreator oraz czytelne widoki wynikow. Najwazniejszym elementem projektu jest domenowy algorytm generowania, ktory laczy pule zmian, algorytm genetyczny i przypisywanie godzin rozpoczecia zmian.

Projekt nadaje sie do dalszego rozwoju, ale juz w obecnej formie prezentuje pelny przeplyw od danych wejsciowych, przez optymalizacje, po gotowy grafik mozliwy do eksportu i oceny.
