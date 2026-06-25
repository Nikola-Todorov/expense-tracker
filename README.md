# Expense Tracker — KIIIS / FINKI DevOps Project

A small 3-tier application built to demonstrate containerization, orchestration,
CI/CD and Kubernetes deployment. All names, ports, credentials and resource
identifiers in this project are intentionally project-specific (see
[Customizations](#customizations) below) and differ from course materials.

## Architecture (3 services)

| Service   | Tech                       | Role                                |
|-----------|----------------------------|-------------------------------------|
| frontend  | Nginx + static HTML/JS     | UI, proxies `/api` to backend       |
| backend   | Spring Boot 3 (Java 21)    | REST API (CRUD for expense entries) |
| database  | PostgreSQL 16              | Persistent storage on named volume  |

## API endpoints

| Method | Path                  | Description       |
|--------|-----------------------|-------------------|
| GET    | `/api/expenses`       | List all          |
| GET    | `/api/expenses/{id}`  | Get one           |
| POST   | `/api/expenses`       | Create            |
| PUT    | `/api/expenses/{id}`  | Update            |
| DELETE | `/api/expenses/{id}`  | Delete            |

---

## 1. Run locally with Docker Compose

```bash
docker compose up --build
```

Open <http://localhost:8095>

Smoke-test the API directly through the nginx proxy:

```bash
curl http://localhost:8095/api/expenses
curl -X POST http://localhost:8095/api/expenses \
  -H 'Content-Type: application/json' \
  -d '{"item":"Coffee","amount":3.50}'
```

Data persists on the named volume `kiiis-ledger-data`. A `docker compose down`
followed by `docker compose up` keeps existing rows; only `docker compose down -v`
will wipe them.

---

## 2. CI pipeline (GitHub Actions → DockerHub)

The workflow at `.github/workflows/ci.yml` runs on every push to `main`:

1. Runs backend tests (`mvn test`)
2. Builds backend + frontend images
3. Pushes them to DockerHub (`:latest` and `:<git-sha>` tags)

**Required GitHub repo secrets:**
- `DOCKERHUB_USERNAME` — your DockerHub username
- `DOCKERHUB_TOKEN` — a DockerHub access token (Account Settings → Security)

---

## 3. Deploy to Kubernetes

Edit the two Deployment files and replace `YOUR_DOCKERHUB_USERNAME` in
`k8s/03-backend-deployment.yaml` and `k8s/05-frontend.yaml`.

```bash
kubectl apply -f k8s/

# Watch it come up
kubectl get all -n kiiis-ledger

# Add the ingress host to /etc/hosts (use `minikube ip` for the actual address)
echo "$(minikube ip) ledger.kiiis.local" | sudo tee -a /etc/hosts
```

Then open <http://ledger.kiiis.local>.

### Manifests included
- `00-namespace.yaml` — dedicated namespace
- `01-db-config.yaml` — DB ConfigMap + Secret
- `02-database-statefulset.yaml` — Postgres StatefulSet + headless Service + PVC
- `03-backend-deployment.yaml` — backend Deployment
- `04-backend-service.yaml` — backend Service
- `05-frontend.yaml` — frontend Deployment + Service
- `06-ingress.yaml` — Ingress

---

## Customizations

Deliberate deviations from the generic course example, applied across compose,
manifests and code so nothing reads as boilerplate:

| Area              | Stock value          | This project              |
|-------------------|----------------------|---------------------------|
| DB name           | `expensedb`          | `kiiis_ledger`            |
| DB user           | `expenseuser`        | `ledger_admin`            |
| DB password       | `changeme`           | `K11s_l3dger_dev_2026`    |
| Compose volume    | `expense-pgdata`     | `kiiis-ledger-data`       |
| Compose network   | `expense-net`        | `kiiis-app-net`           |
| Container names   | `expense-*`          | `kiiis-ledger-{db,backend,ui}` |
| Image tags        | `expense-*:local`    | `kiiis-ledger-{backend,ui}:local` |
| UI host port      | `8090`               | `8095`                    |
| K8s namespace     | `expense-tracker`    | `kiiis-ledger`            |
| Ingress host      | `expense.local`      | `ledger.kiiis.local`      |
