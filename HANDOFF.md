# Devault: stan prac (2026-10-17)

> Notatka tymczasowa do przeniesienia na inny komputer / do nowego czatu. Do usunięcia po przeczytaniu.

Repo: Devault (Kotlin 2.3, Spring Boot 4.1, Java 21, mono-repo Gradle). Gałąź `ingestion-service`.
Zasady pracy: `AGENTS.md`. Projekt do nauki: kod produkcyjny piszę sam (asystent wyjaśnia mechanizm, pułapki, pisze kod tylko na prośbę), testy pisze i uruchamia asystent.

Testy: common-lib 8/8, auth-service-lib 29/29, workspace-service 60/61, ingestion-service 152/153.
Czerwone są tylko `*ApplicationTests.contextLoads` (brak bazy w testach; znane, odłożone razem z Testcontainers).

## Zrobione (od 2026-10-04)

### ingestion-service: endpointy
- `POST /workspaces/{workspaceId}/ingestion-sources`: podłączenie repo. 409 dla duplikatu (`SourceAlreadyConnectedException`), `externalId` małymi literami, walidacja `@Pattern` dla owner/name.
- `GET /workspaces/{workspaceId}/ingestion-sources`: lista źródeł we wszystkich statusach.
- `GET /workspaces/{workspaceId}/ingestion-sources/{sourceId}`: jedno źródło, 404 (`SourceNotFoundException`).
- `DELETE /workspaces/{workspaceId}/ingestion-sources/{sourceId}`: usuwa dokumenty jednym zbiorczym DELETE, potem źródło, w jednej transakcji. 409 przy PENDING/SYNCING (`SourceSyncInProgressException`).
- `GET /credentials`, `POST /credentials`.
- `DELETE /credentials/{id}`: `CredentialDeletionService` (osobna klasa, bo inaczej cykl zależności). 404 dla cudzego, 409 gdy używa go źródło (`CredentialInUseException`).
- `GET /credentials/{id}/rate-limit`: limit GitHuba na żywo (`RateLimitService`).

### Backfill i klient GitHuba (`GithubClient`)
- Nieznane repo: `repository` w DTO nullowalne, więc błąd GitHuba dochodzi jako `GithubApiException`.
- `raw_content` ma typ `text` (było varchar(255) i długie PR-y/commity po cichu przepadały).
- Duplikaty sprawdzane przed zapisem (`existsBySourceAndExternalRef`). Inne błędy zapisu kończą backfill statusem ERROR i są logowane.
- `@Retryable` na `GithubClient`: 5xx i błędy połączenia, 5 prób (1, 2, 4, 8, 16 s). 4xx i błędy GraphQL nie są ponawiane.
- Limit zapytań per credential (`ConcurrentHashMap` po `credentialRef`) i bramka przed każdym zapytaniem: przy ≤ 50 punktach czeka do `resetAt` + 2 s. Czas z wstrzykiwanego `Clock`.

### ING-9, pierwsza połowa (sprawdzanie ról)
- workspace-service: `GET /workspaces/{workspaceId}/members/me` zwraca własne członkostwo z rolą (404 brak workspace, 403 nieczłonek). `requireMember` wydzielone z `requireRole`.
- auth-service-lib: `JwtAuthenticationProvider` trzyma JWT w `credentials` (do przekazywania tokenu dalej).
- `WorkspaceRole` przeniesione do `common-lib` (`dev.devault.commonlib.type`).
- ingestion-service: `WorkspaceClient.getWorkspaceMemberRole(workspaceId)` woła `/members/me` z tokenem użytkownika z `SecurityContextHolder`.
  - 403 → `AccessDeniedException`, 404 → `NoSuchElementException`.
  - Inne błędy → `WorkspaceServiceException` (502, logowane).
  - Nieznana rola jest odrzucana.
- Konfiguracja: `workspace.service.uri` (`WorkspaceServiceProperties`), bean `workspaceRestClient`, zmienna `WORKSPACE_HOST` w compose i `.env.example`.

### Narzędzia i dokumentacja
- Generator Insomnii (`make insomnia`) sam wypełnia `sourceId` i `credentialRef` (z `credentialId`).
- Identyfikatory przemianowane na `Github` (`GithubClient`, `GithubApiException`).
- `DEPLOYMENT_MODES.md` (pomysł na tryb hybrydowy), sekcja w README, zaktualizowany `AGENTS.md`.

## Na czym skończyłem

ING-9, druga połowa. `WorkspaceClient` istnieje, ale nikt go jeszcze nie używa.
W `IngestionSourceService` są trzy `TODO(ING-9)` (linie 26, 49, 75).

## Następne kroki

1. Wstrzyknąć `WorkspaceClient` do `IngestionSourceService`.
2. `connectSource`: sprawdzić rolę (OWNER/ADMIN) przed zapisem i przed `runBackfill`. Musi to się stać w wątku żądania, bo w `@Async` nie ma tokenu.
3. Usuwanie źródła: OWNER/ADMIN.
4. Lista i pojedyncze źródło: każdy członek. Filtry po `connectedByUserId` zamienić na zapytania po samym workspace.
5. Do decyzji: dokładne reguły ról. Propozycja: podłączanie i usuwanie OWNER+ADMIN, odczyt wszyscy członkowie.
6. Testy (asystent), potem ręcznie: restart serwisów, `make insomnia`, import kolekcji.
7. Do rozważenia: timeouty dla `workspaceRestClient` (wymagają `RestClient.Builder`, czyli odstępstwa od konwencji z `AGENTS.md`) i ponawianie.

## Znane luki (na później)
- `lastSynced` nigdy nie jest ustawiane.
- Brak ponownej synchronizacji: źródło w ERROR można tylko usunąć i podłączyć od nowa. Po restarcie serwisu źródło może utknąć w SYNCING.
- Edycje PR/issue nie są aktualizowane (tylko wstawianie; `contentHash` jest gotowy do porównania). Ważne przy re-sync i webhookach.
- Brak limitu równoległych backfilli (`spring.task.execution.simple.concurrency-limit` albo własny `ThreadPoolTaskExecutor`).
- Wtórny limit GitHuba (`Retry-After`) nieobsłużony.
- Powód statusu ERROR jest tylko w logu, nie przy źródle (pasuje do ING-13).
- Aktualizacja tokenu w istniejącym credentialu (ING-13).
- `getTokenForUser` zwraca 403 dla cudzego credentiala, reszta API zwraca 404.

## Świadome decyzje (nie zmieniać bez pytania)
- Testcontainers i paginacja: później, razem, dla wszystkich serwisów.
- Usuwanie źródła synchroniczne; wariant z DELETING/202 dopiero razem z kolejką backfillu.
- Usuwanie credentiala bez `replaceWith`.
- Do workspace-service idzie token użytkownika, nie konto serwisowe.
- `WorkspaceRole` jest wspólne, ale klient mapuje rolę ze String (nieznana rola = błąd).

## Pułapki środowiska
- Nie uruchamiać `contextLoads` z `.env.local`: `ddl-auto=create` czyści bazę dev.
- IDE potrafi nadpisać plik zmieniony z zewnątrz: przed edycją File → Reload All from Disk.
- `git push` z terminala nie ma credential helpera: raz `gh auth setup-git`.

## Roadmapa po ING-9
ING-8 (zdarzenia Kafka), ING-6 (webhooki, HMAC), ING-13 (wygasły PAT), potem embedding-service, query-service (RAG), gateway, frontend, K8s. Później ewentualnie tryb hybrydowy.
