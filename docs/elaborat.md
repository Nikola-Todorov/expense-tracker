# Елаборат — Проектна задача по КИИИС

> **Како да го користиш овој фајл**
>
> 1. Прочитај го и провери ги местата каде што пишува `<...>` — тие се само за тебе да ги пополниш (име, индекс, ментори, датум).
> 2. На места означени со `[СЛИКА N: ...]` — вметни ја соодветната скриншот-слика (листа на крајот од документот).
> 3. Експортирај во PDF (Word: *File → Export → Create PDF*; Google Docs: *File → Download → PDF Document*; или `pandoc elaborat.md -o elaborat.pdf` ако имаш pandoc).
> 4. Качи го PDF-от на CourseWare страницата на предметот.
>
> Овој draft е насочен на ~6–7 страни. Слободно скрати го таму каде што е премногу.

---

# Насловна страна

```
Универзитет „Св. Кирил и Методиј" — Скопје
Факултет за информатички науки и компјутерско инженерство (ФИНКИ)

Предмет:    Континуирана интеграција, испорака и сервиси (КИИИС)

Проектна задача:
   Expense Tracker — 3-слојна апликација со DevOps практики
   (контејнеризација, CI/CD и Kubernetes оркестрација)

Студент:    Никола Тодоров
Индекс:     <бр. на индекс>
e-mail:     nikolatodorov623@gmail.com

Ментори:    <имиња на професорите од листата на предметот>

Сесија:     <јунска / септемвриска / јануарска> 2026
Датум:      <датум на предавање>
```

---

# 1. Краток опис на проектот

Како проектна задача е избрана сопствено развиена 3-слојна веб-апликација
**Expense Tracker** — едноставен следач на трошоци со CRUD операции преку
REST API. Целта на задачата не е сложеноста на апликацијата, туку нејзината
контејнеризација, континуирана интеграција и оркестрација на јавно достапен
Kubernetes кластер, согласно барањата на предметот КИИИС.

Стек:
- **Frontend** — статички HTML/JS, се сервира преку **Nginx 1.27**.
- **Backend** — **Spring Boot 3** (Java 21), Maven multi-stage build, излага REST endpoint-и под `/api/expenses`.
- **База** — **PostgreSQL 16-alpine**, со постојано чување на податоци на именован volume (Compose) односно PVC од 2 GiB (Kubernetes).

Артефакти и линкови:

| Артефакт | Линк |
|---|---|
| Изворен код (јавен Git репо) | https://github.com/Nikola-Todorov/expense-tracker |
| DockerHub слика — backend | https://hub.docker.com/r/nixiblatec/kiiis-ledger-backend |
| DockerHub слика — frontend | https://hub.docker.com/r/nixiblatec/kiiis-ledger-ui |
| CI pipeline (GitHub Actions) | https://github.com/Nikola-Todorov/expense-tracker/actions |

---

# 2. Архитектура

Апликацијата се состои од три сервиси кои комуницираат низ заедничка мрежа
(Docker network во локално опкружување; Kubernetes Service-и во кластерот).
Кориснички трафик влегува низ **Ingress** на патот `/`; повиците кон
`/api/*` се рутирани директно кон бекендот.

[СЛИКА 1: дијаграм на архитектура — screenshot од Mermaid дијаграмот во README.md]

| Сервис | Технологија | Улога | Внатрешен порт |
|---|---|---|---|
| `ledger-ui` | Nginx + HTML/JS | Кориснички интерфејс | 80 |
| `ledger-api` | Spring Boot 3, Java 21 | REST API (`/api/expenses` GET/POST/PUT/DELETE) | 8080 |
| `ledger-db` | PostgreSQL 16-alpine | Постојано складирање | 5432 |

API endpoint-и:

| Метод | Пат | Опис |
|---|---|---|
| GET | `/api/expenses` | Листа на сите трошоци |
| GET | `/api/expenses/{id}` | Еден трошок по `id` |
| POST | `/api/expenses` | Креирање |
| PUT | `/api/expenses/{id}` | Ажурирање |
| DELETE | `/api/expenses/{id}` | Бришење |

---

# 3. Контејнеризација (10%)

Секој од двата самостојни сервиси (`ledger-api`, `ledger-ui`) има свој
`Dockerfile`. Базата користи официјален upstream image (`postgres:16-alpine`)
бидејќи не треба прилагодување — конфигурацијата се поднесува преку
променливи на околината и volume mount.

**`backend/Dockerfile`** — **multi-stage** билд. Првиот stage користи
`maven:3.9-eclipse-temurin-21` за да го компајлира и пакува JAR-от; вториот
stage стартува од `eclipse-temurin:21-jre-jammy`, копира само финалниот
артефакт и работи под non-root корисник (`spring`). Финалната слика е знатно
помала од build slika-та бидејќи не носи Maven и градба-тоолови.

**`frontend/Dockerfile`** — едноставен Nginx 1.27 со закопирани
`nginx.conf` и `index.html`.

[СЛИКА 2: содржина на двата Dockerfile-а (`cat backend/Dockerfile frontend/Dockerfile`)]

---

# 4. Docker Compose оркестрација (10%)

Сите три сервиси се поставени со еден `docker-compose.yml`. Карактеристики:

- Имено volume `kiiis-ledger-data` за постојано чување на Postgres податоци.
- Сопствена `bridge` мрежа `kiiis-app-net` (наместо `default`).
- `healthcheck` на базата со `pg_isready`; backend стартува со
  `depends_on.condition: service_healthy` — нема race на старт.
- Прилагодени имиња на контејнерите и имиџите (`kiiis-ledger-{db,backend,ui}`).
- Корисничкиот интерфејс е изложен на нестандарден host port **`8095`**
  (не `8090`) за да се избегне колизија со други локални сервиси.

Покренување:

```bash
docker compose up --build
# Open http://localhost:8095
```

Верификација на API преку nginx proxy-то на frontend контејнерот:

```bash
curl http://localhost:8095/api/expenses                       # -> []
curl -X POST http://localhost:8095/api/expenses \
     -H 'Content-Type: application/json' \
     -d '{"item":"Coffee","amount":3.50}'                     # -> 201, ID 1
curl http://localhost:8095/api/expenses                       # -> [{"id":1,...}]
```

[СЛИКА 3: `docker compose ps` со сите 3 контејнери `Up` / `healthy`]
[СЛИКА 4: прелистувач отворен на `http://localhost:8095` со неколку запишани трошоци]

**Persistence**: `docker compose down` ги уништува контејнерите, но не и
volume-от. По нов `docker compose up`, истите записи се присутни во GET-от —
успешно потврдено за време на изработката.

---

# 5. Јавен Git репозиториум (10%)

Целиот проект е јавно достапен на:

**https://github.com/Nikola-Todorov/expense-tracker**

Структура на репозиториумот:

```
expense-tracker/
├── backend/                    Spring Boot REST API + Dockerfile
├── frontend/                   Nginx + HTML/JS + Dockerfile
├── k8s/                        7 Kubernetes манифести
├── .github/workflows/ci.yml    GitHub Actions CI pipeline
├── docker-compose.yml          3-сервисна Compose orchestration
├── docs/elaborat.md            овој документ
├── README.md                   документација на проектот
└── .gitignore
```

[СЛИКА 5: GitHub repo home, видлив README + structure tree, ознака "Public"]

---

# 6. CI pipeline → DockerHub (20%)

Избрана платформа: **GitHub Actions**. Дефиниран во `.github/workflows/ci.yml`.

Тригери: `push` на `main` гранката + рачно (`workflow_dispatch`).
Покрај тоа, `concurrency` блок откажува претходно in-flight runs за иста
гранка — нема двојни push-еви на DockerHub од заостанати committed-и.

Pipeline-от е поделен на **два jobs** со `needs:` зависност (тест мора да помине пред push):

1. **`test`** — стартува `actions/setup-java@v4` со Temurin 21 и Maven cache,
   потоа `mvn -B -ntp test`. Овој job е заштитник: ако unit testovite не
   поминат, втората фаза воопшто не се извршува.
2. **`publish`** — поставува QEMU + Buildx за да поддржи **multi-arch**
   билдови, се најавува на DockerHub со tajno čuvani credentials (`DOCKERHUB_USERNAME`,
   `DOCKERHUB_TOKEN`), и со `docker/build-push-action@v6` ги гради и
   објавува двете слики за **`linux/amd64` и `linux/arm64`**. Тагови:
   - `:latest` (последна стабилна)
   - `:main` (последно од `main` гранката)
   - `:sha-<short>` (по специфичен commit, за reproducible deploys)

   GitHub Actions cache (`type=gha`) се користи на layer ниво за двете слики,
   па вторите runs се значително побрзи. На крај се испишува **Step Summary**
   во Markdown со точните слики и тагови.

DockerHub секрети се поставени на repo ниво (Settings → Secrets and variables → Actions).

[СЛИКА 6: GitHub Actions страница со успешен run, отворен Step Summary блок]
[СЛИКА 7: DockerHub страница за `nixiblatec/kiiis-ledger-backend` со трите тагови видливи]
[СЛИКА 8: DockerHub страница за `nixiblatec/kiiis-ledger-ui` со трите тагови видливи]

> Напомена: multi-arch билдот беше додаден откако првично-објавените
> `linux/amd64`-само слики не можеа да се повлечат на minikube кластерот
> кој работи на Apple Silicon (`linux/arm64`). Поправката беше едноставна
> (`platforms: linux/amd64,linux/arm64` + `setup-qemu-action@v3`) но
> демонстрира зошто portable image manifests се важни.

---

# 7. Kubernetes деплојмент (5 × 10% = 50%)

Сите 7 манифести се применуваат во посебен namespace **`kiiis-ledger`**
со една команда:

```bash
minikube start --driver=docker --cpus=2 --memory=4096
minikube addons enable ingress
kubectl apply -f k8s/
```

[СЛИКА 9: `kubectl get all,ingress,pvc -n kiiis-ledger` — една слика што го прикажува целиот стек жив (pods Running, services Ready, ingress со IP, PVC Bound)]

## 7.1 Namespace (`k8s/00-namespace.yaml`)

Сите ресурси се групирани во посебен namespace `kiiis-ledger` со
labels кои го означуваат предметот и проектот
(`kiiis.finki.mk/course=kiiis`, `kiiis.finki.mk/project=expense-tracker`).
Ова го изолира проектот од другите workloads на кластерот.

## 7.2 StatefulSet за базата + ConfigMap + Secret (10%)

Базата е реализирана како **StatefulSet** `ledger-db` со 1 replika,
поврзана со **headless Service** со исто име (потребно за stable pod DNS).
Користи `volumeClaimTemplates` за автоматско обезбедување на 2 GiB PVC
(`ledger-data-ledger-db-0`).

- **ConfigMap `ledger-db-config`** — носи `POSTGRES_DB`, `POSTGRES_USER`,
  `PGDATA` (поставен на под-директориум за да се избегне `lost+found`
  проблемот при `initdb`).
- **Secret `ledger-db-secret`** (тип `Opaque`) — носи `POSTGRES_PASSWORD`.
  Истиот password се користи и од бекендот преку посебен secret.

Дополнителни мерки:
- `readinessProbe` и `livenessProbe` со `pg_isready -U ledger_admin -d kiiis_ledger`.
- `resources.requests` / `limits` (`100m`–`500m` CPU, `192Mi`–`512Mi` RAM).

## 7.3 Deployment за бекендот + ConfigMap + Secret (10%)

`ledger-api` Deployment со **2 реплики** и `RollingUpdate` стратегија
со `maxUnavailable: 0` и `maxSurge: 1` — zero-downtime rollouts.

- **ConfigMap `ledger-api-config`** — пет non-sensitive параметри
  (`DB_HOST=ledger-db`, `DB_PORT`, `DB_NAME`, `DB_USER`, `APP_PORT`),
  внесени во контејнерот преку `envFrom`.
- **Secret `ledger-api-secret`** — `DB_PASSWORD`, внесен преку
  `valueFrom.secretKeyRef`.
- **Readiness / Liveness** на Spring Actuator endpoints
  (`/actuator/health/readiness`, `/actuator/health/liveness`).
- **Resource requests / limits** (`100m`–`750m` CPU, `384Mi`–`768Mi` RAM).
- `imagePullPolicy: IfNotPresent` — преферира локално кеширана слика
  (`minikube image load`) кога постои, инаку повлекува од регистарот.

## 7.4 Service (10%)

Дефинирани три Service-и:

- `ledger-db` — **headless** (`clusterIP: None`), потребен за StatefulSet pod DNS.
- `backend` — `ClusterIP` за бекендот. Името на овој Service е **намерно**
  задржано како `backend` (а не `ledger-api`) бидејќи во `nginx.conf` на
  frontend сликата upstream-от е hard-coded како `http://backend:8080`.
  Преименување би налагало повторно градење на frontend сликата.
- `ledger-ui` — `ClusterIP` за фронтендот, на кој Ingress-от рутира `/`.

## 7.5 Ingress (10%)

`ledger-ingress` (од класа `nginx`) за host **`ledger.kiiis.local`**:

- `/api/*` → Service `backend:8080`
- `/` → Service `ledger-ui:80`

Овој setup го заобиколува `/api` proxy block-от во frontend nginx-от:
во кластер, рутирањето е работа на Ingress-от, не на самата UI под.

## 7.6 Демонстрација

Бидејќи minikube со Docker driver на macOS не изложува директно IP до host-от,
користен е `kubectl port-forward` на `ingress-nginx-controller`:

```bash
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8085:80 &
curl -H "Host: ledger.kiiis.local" http://localhost:8085/api/expenses     # GET → []
curl -H "Host: ledger.kiiis.local" -X POST http://localhost:8085/api/expenses \
     -H "Content-Type: application/json" \
     -d '{"item":"k8s demo via ingress","amount":99.99}'                  # POST → 201
curl -H "Host: ledger.kiiis.local" http://localhost:8085/api/expenses     # GET → [{...}]
curl -H "Host: ledger.kiiis.local" http://localhost:8085/                 # → HTML
```

За пристап низ прелистувач, во посебен прозорец се покренува
`sudo minikube tunnel` и се додава `127.0.0.1 ledger.kiiis.local` во `/etc/hosts`.

[СЛИКА 10: терминал со успешен curl циклус (GET → POST → GET) низ Ingress]
[ОПЦИОНАЛНО — СЛИКА 11: прелистувач на `http://ledger.kiiis.local` со UI и податоци]

---

# 8. Свесни кастомизации (зошто не наликува на материјалите од вежби)

Согласно барањето на предметот, сите конфигурации беа намерно
прилагодени така да не се идентични со примерите од предавања и вежби:

| Слој | Стандардно | Во овој проект |
|---|---|---|
| DB име | `expensedb` | `kiiis_ledger` |
| DB корисник | `expenseuser` | `ledger_admin` |
| DB лозинка | `changeme` | `K11s_l3dger_dev_2026` |
| Compose volume | `expense-pgdata` | `kiiis-ledger-data` |
| Compose мрежа | `expense-net` | `kiiis-app-net` |
| Имиња на контејнери | `expense-*` | `kiiis-ledger-{db,backend,ui}` |
| Имиња на имиџи | `expense-*:local` | `kiiis-ledger-{backend,ui}:local` |
| Host порт за UI | `8090` | `8095` |
| K8s namespace | `expense-tracker` | `kiiis-ledger` |
| K8s StatefulSet | `database` | `ledger-db` |
| K8s Deployment-и | `backend`, `frontend` | `ledger-api`, `ledger-ui` |
| K8s ConfigMaps | `db-config`, `backend-config` | `ledger-db-config`, `ledger-api-config` |
| K8s Secrets | `db-secret`, `backend-secret` | `ledger-db-secret`, `ledger-api-secret` |
| Ingress host | `expense.local` | `ledger.kiiis.local` |
| CI image arch | само `linux/amd64` | `linux/amd64,linux/arm64` |

Дополнително:
- CI pipeline-от е поделен на два јобa (`test`, `publish`) со `needs:` зависност, GHA layer cache и Markdown step summary.
- Tag шемата ги вклучува `latest`, `main`, и `sha-<short>` — задоволително за reproducible deployments.
- Сите Deployments користат `RollingUpdate` со `maxUnavailable: 0` (zero-downtime).
- Сите објекти носат `app.kubernetes.io/{name,part-of,component}` labels (Kubernetes recommended).
- Сите контејнери имаат експлицитни `resources.requests` и `limits`.

---

# 9. Заклучок

Проектот ги покрива сите барања на предметот КИИИС:

- ✅ Јавен Git репозиториум
- ✅ Контејнеризација на сите сервиси
- ✅ Docker Compose оркестрација со named volume и health-aware зависности
- ✅ CI pipeline кој автоматски тестира, гради и публикува multi-arch имиџи на DockerHub
- ✅ Kubernetes манифести за Deployment, Service, Ingress и StatefulSet, со придружни ConfigMap-и и Secret-и
- ✅ Сите ресурси поставени во посебен namespace и end-to-end демонстрирани преку Ingress

Личен take-away: јасно гледам зошто DevOps практиките како health probes,
rolling updates и multi-arch имиџи не се „декоративни" — секоја од нив ме
спаси од концретен проблем за време на изработката (например, ImagePullBackOff
на arm64 minikube кој беше решен само со multi-arch билдови во CI).

---

# Прилози — листа на скриншоти

| # | Опис на скриншот |
|---|---|
| 1 | Дијаграм на архитектура (од README — Mermaid секцијата) |
| 2 | Содржина на `backend/Dockerfile` и `frontend/Dockerfile` (`cat`) |
| 3 | `docker compose ps` — сите 3 контејнери `Up` / `healthy` |
| 4 | Прелистувач отворен на `http://localhost:8095` со податоци |
| 5 | GitHub repo home (јавен, со README) |
| 6 | GitHub Actions страница со успешен run + Step Summary |
| 7 | DockerHub страница за `nixiblatec/kiiis-ledger-backend` со таговите |
| 8 | DockerHub страница за `nixiblatec/kiiis-ledger-ui` со таговите |
| 9 | `kubectl get all,ingress,pvc -n kiiis-ledger` — целосна состојба на кластерот |
| 10 | Терминал со успешен curl циклус (GET → POST → GET) низ Ingress |
| 11 | (опционално) Прелистувач на `http://ledger.kiiis.local` |
