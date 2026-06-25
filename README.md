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

## 3. Deploy to Kubernetes (minikube)

```bash
# 1. start a cluster + ingress addon
minikube start --driver=docker --cpus=2 --memory=4096
minikube addons enable ingress

# 2. apply everything into the dedicated namespace
kubectl apply -f k8s/

# 3. watch it come up
kubectl get all,ingress,pvc -n kiiis-ledger
kubectl -n kiiis-ledger rollout status deploy/ledger-api
kubectl -n kiiis-ledger rollout status deploy/ledger-ui

# 4a. quick demo via port-forward (no sudo)
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8085:80 &
curl -H "Host: ledger.kiiis.local" http://localhost:8085/api/expenses

# 4b. browser demo (needs sudo for /etc/hosts + tunnel)
sudo minikube tunnel &
echo "127.0.0.1 ledger.kiiis.local" | sudo tee -a /etc/hosts
open http://ledger.kiiis.local
```

### Manifests included
- `00-namespace.yaml` — `kiiis-ledger` namespace + project labels
- `01-db-config.yaml` — `ledger-db-config` ConfigMap + `ledger-db-secret`
- `02-database-statefulset.yaml` — `ledger-db` StatefulSet + headless Service + 2Gi PVC
- `03-backend-deployment.yaml` — `ledger-api` Deployment + `ledger-api-config`/`-secret`
- `04-backend-service.yaml` — Service named `backend` (kept to satisfy the frontend nginx upstream)
- `05-frontend.yaml` — `ledger-ui` Deployment + Service
- `06-ingress.yaml` — `ledger-ingress` routing `/api` → backend, `/` → ledger-ui

> **Apple Silicon note**: minikube on Docker driver runs arm64. The CI pipeline
> builds multi-arch images (`linux/amd64,linux/arm64`) so pods pull a matching
> manifest. If iterating locally without re-pushing, `minikube image load
> docker.io/<user>/kiiis-ledger-backend:latest` side-loads the arm64 build the
> compose stack already produced; the manifests set `imagePullPolicy: IfNotPresent`
> so a cached image is preferred.

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
| K8s StatefulSet   | `database`           | `ledger-db`               |
| K8s Deployments   | `backend`, `frontend`| `ledger-api`, `ledger-ui` |
| K8s ConfigMaps    | `db-config`, `backend-config` | `ledger-db-config`, `ledger-api-config` |
| K8s Secrets       | `db-secret`, `backend-secret` | `ledger-db-secret`, `ledger-api-secret` |
| Ingress host      | `expense.local`      | `ledger.kiiis.local`      |
| CI image arch     | `linux/amd64` only   | `linux/amd64,linux/arm64` |
