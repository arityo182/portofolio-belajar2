<!-- FILE: docs/deployment/ci-cd-github-actions.md -->
# CI/CD dengan GitHub Actions
## Platform Koperasi Digital / P2P Lending untuk UMKM Indonesia

| Atribut | Nilai |
|---|---|
| Dokumen | CI/CD Pipeline (GitHub Actions) |
| Versi | 1.0 |
| Tanggal | 2026-10-24 |
| Target | AWS ECS Fargate + ECR |

---

## 1. Pipeline Overview

| Branch | Trigger | Workflow | Tujuan |
|---|---|---|---|
| PR → `develop` | pull_request | `test.yml` | Validasi test + coverage sebelum merge |
| push `develop` | push | `build-push.yml` → `deploy-staging.yml` | Build image + deploy staging |
| push `main` | push | `build-push.yml` → `deploy-production.yml` | Build image + deploy produksi (dengan approval) |

```
PR → develop     : test.yml (gate merge)
push develop     : build-push.yml → deploy-staging.yml (otomatis)
push main        : build-push.yml → deploy-production.yml (manual approval)
```

Konsisten dengan GitFlow: `feature/*` → PR → `develop` (staging) → `release/*` → `main` (production).

---

## 2. `test.yml` — Test & Coverage (PR ke develop)

```yaml
name: Test
on:
  pull_request:
    branches: [develop]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: gradle

      - name: Run Gradle tests + coverage
        run: ./gradlew test jacocoTestReport jacocoTestCoverageVerification

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v4
        with:
          token: ${{ secrets.CODECOV_TOKEN }}
          files: '**/build/reports/jacoco/test/jacocoTestReport.xml'
          flags: backend

  frontend-test:
    runs-on: ubuntu-latest
    defaults: { run: { working-directory: ./frontend } }
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - name: Run Jest + coverage
        run: npm run test:coverage
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v4
        with:
          token: ${{ secrets.CODECOV_TOKEN }}
          files: ./frontend/coverage/lcov.info
          flags: frontend

  comment-pr:
    needs: [backend-test, frontend-test]
    runs-on: ubuntu-latest
    if: always()
    permissions: { pull-requests: write }
    steps:
      - name: Comment test result on PR
        uses: actions/github-script@v7
        with:
          script: |
            const be = '${{ needs.backend-test.result }}';
            const fe = '${{ needs.frontend-test.result }}';
            const icon = (r) => r === 'success' ? '✅' : '❌';
            github.rest.issues.createComment({
              owner: context.repo.owner,
              repo: context.repo.repo,
              issue_number: context.issue.number,
              body: `### Hasil Test\n- Backend: ${icon(be)} ${be}\n- Frontend: ${icon(fe)} ${fe}`
            });
```

---

## 3. `build-push.yml` — Build & Push Image (push develop/main)

```yaml
name: Build & Push
on:
  push:
    branches: [develop, main]

env:
  AWS_REGION: ap-southeast-1
  ECR_REGISTRY: ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.ap-southeast-1.amazonaws.com

jobs:
  build-push:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service:
          - auth-service
          - loan-service
          - scoring-service
          - payment-service
          - frontend
    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_DEPLOY_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        uses: aws-actions/amazon-ecr-login@v2

      - name: Set image tag
        id: tag
        run: echo "tag=${GITHUB_REF_NAME}-${GITHUB_SHA::7}" >> "$GITHUB_OUTPUT"

      - name: Build Docker image
        run: |
          docker build -t $ECR_REGISTRY/koperasi/${{ matrix.service }}:${{ steps.tag.outputs.tag }} \
            ./services/${{ matrix.service }} || \
          docker build -t $ECR_REGISTRY/koperasi/${{ matrix.service }}:${{ steps.tag.outputs.tag }} \
            ./${{ matrix.service }}

      - name: Scan image with Trivy
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ env.ECR_REGISTRY }}/koperasi/${{ matrix.service }}:${{ steps.tag.outputs.tag }}
          format: table
          exit-code: '1'
          severity: 'CRITICAL,HIGH'
          ignore-unfixed: true

      - name: Push image to ECR
        run: docker push $ECR_REGISTRY/koperasi/${{ matrix.service }}:${{ steps.tag.outputs.tag }}
```

---

## 4. `deploy-staging.yml` — Deploy Staging (push develop)

```yaml
name: Deploy Staging
on:
  workflow_run:
    workflows: ["Build & Push"]
    types: [completed]
    branches: [develop]

env:
  AWS_REGION: ap-southeast-1
  ECR_REGISTRY: ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.ap-southeast-1.amazonaws.com
  CLUSTER: koperasi-staging

jobs:
  deploy:
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [auth-service, loan-service, scoring-service, payment-service, frontend]
    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_DEPLOY_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Set image tag
        id: tag
        run: echo "tag=develop-${GITHUB_SHA::7}" >> "$GITHUB_OUTPUT"

      - name: Render new task definition
        id: taskdef
        uses: aws-actions/amazon-ecs-render-task-definition@v1
        with:
          task-definition: ./infra/ecs/${{ matrix.service }}-staging.json
          container-name: ${{ matrix.service }}
          image: ${{ env.ECR_REGISTRY }}/koperasi/${{ matrix.service }}:${{ steps.tag.outputs.tag }}

      - name: Deploy to ECS staging
        uses: aws-actions/amazon-ecs-deploy-task-definition@v2
        with:
          task-definition: ${{ steps.taskdef.outputs.task-definition }}
          service: koperasi-${{ matrix.service }}
          cluster: ${{ env.CLUSTER }}
          wait-for-service-stability: true

  smoke-test:
    needs: deploy
    runs-on: ubuntu-latest
    steps:
      - name: Smoke test health endpoints
        run: |
          set -e
          curl -fsS https://staging-api.koperasi.example.id/actuator/health
          curl -fsS https://staging-api.koperasi.example.id/api/v1/loans/products

  notify:
    needs: [deploy, smoke-test]
    runs-on: ubuntu-latest
    if: always()
    steps:
      - name: Slack notification
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {"text":"Staging deploy: ${{ needs.smoke-test.result }} (commit ${{ github.sha }})"}
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

---

## 5. `deploy-production.yml` — Deploy Production (push main)

```yaml
name: Deploy Production
on:
  workflow_run:
    workflows: ["Build & Push"]
    types: [completed]
    branches: [main]

env:
  AWS_REGION: ap-southeast-1
  ECR_REGISTRY: ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.ap-southeast-1.amazonaws.com
  CLUSTER: koperasi-prod

jobs:
  deploy:
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    runs-on: ubuntu-latest
    environment:
      name: production          # Environment protection rule -> manual approval gate
      url: https://app.koperasi.example.id
    strategy:
      max-parallel: 2
      matrix:
        service: [auth-service, loan-service, scoring-service, payment-service, frontend]
    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_DEPLOY_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Set image tag
        id: tag
        run: echo "tag=main-${GITHUB_SHA::7}" >> "$GITHUB_OUTPUT"

      - name: Render new task definition
        id: taskdef
        uses: aws-actions/amazon-ecs-render-task-definition@v1
        with:
          task-definition: ./infra/ecs/${{ matrix.service }}-prod.json
          container-name: ${{ matrix.service }}
          image: ${{ env.ECR_REGISTRY }}/koperasi/${{ matrix.service }}:${{ steps.tag.outputs.tag }}

      - name: Deploy to ECS production
        uses: aws-actions/amazon-ecs-deploy-task-definition@v2
        with:
          task-definition: ${{ steps.taskdef.outputs.task-definition }}
          service: koperasi-${{ matrix.service }}
          cluster: ${{ env.CLUSTER }}
          wait-for-service-stability: true
          codedeploy-appspec: ./infra/ecs/appspec.yaml    # opsional blue/green

  verify:
    needs: deploy
    runs-on: ubuntu-latest
    steps:
      - name: Verify deployment
        run: |
          set -e
          curl -fsS https://api.koperasi.example.id/actuator/health
          curl -fsS https://app.koperasi.example.id

  notify:
    needs: [deploy, verify]
    runs-on: ubuntu-latest
    if: always()
    steps:
      - name: Slack notification
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {"text":"PRODUCTION deploy: ${{ needs.verify.result }} (commit ${{ github.sha }}).\nRollback: lihat docs/deployment/ci-cd-github-actions.md §7"}
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

---

## 6. GitHub Secrets yang Dibutuhkan

| Secret Name | Description |
|---|---|
| `AWS_ACCOUNT_ID` | ID akun AWS (untuk URL ECR) |
| `AWS_DEPLOY_ROLE_ARN` | ARN IAM role (OIDC) untuk deploy ke ECR/ECS |
| `CODECOV_TOKEN` | Token upload coverage ke Codecov |
| `SLACK_WEBHOOK_URL` | Webhook Slack untuk notifikasi deploy |
| `MIDTRANS_SERVER_KEY` | (bila dibutuhkan smoke test terintegrasi) |
| `XENDIT_API_KEY` | (bila dibutuhkan smoke test terintegrasi) |

> Autentikasi AWS memakai **OIDC** (`aws-actions/configure-aws-credentials` + `role-to-assume`) — hindari menyimpan access key statis. Approval produksi dikelola via **Environment protection rule** pada environment `production` (reviewer wajib + wait timer opsional).

---

## 7. Rollback Procedure

### 7.1 Manual (cepat — kembali ke task definition sebelumnya)
```bash
# Lihat revisi task definition
aws ecs list-task-definitions --family-prefix koperasi-loan --sort DESC

# Update service ke revisi stabil sebelumnya
aws ecs update-service \
  --cluster koperasi-prod \
  --service koperasi-loan-service \
  --task-definition koperasi-loan:<REVISION_SEBELUMNYA> \
  --force-new-deployment

# Pantau sampai stabil
aws ecs wait services-stable --cluster koperasi-prod --services koperasi-loan-service
```

### 7.2 Manual (via image tag)
```bash
# Re-deploy dengan tag commit yang diketahui stabil
# render task def dengan image koperasi/loan:main-<sha-stabil> lalu update-service
```

### 7.3 Automated
- **Circuit breaker ECS:** aktifkan `deploymentConfiguration.deploymentCircuitBreaker = { enable: true, rollback: true }` pada service → ECS otomatis rollback bila deployment gagal mencapai stabil.
- **Blue/Green (CodeDeploy):** bila dikonfigurasi (`appspec.yaml`), traffic dialihkan bertahap; alarm CloudWatch gagal → auto-rollback ke versi biru.
- **Trigger:** gagal `wait-for-service-stability`, atau alarm CloudWatch (5xx rate, health check gagal) → rollback otomatis + notifikasi Slack.

```json
"deploymentConfiguration": {
  "deploymentCircuitBreaker": { "enable": true, "rollback": true },
  "maximumPercent": 200,
  "minimumHealthyPercent": 100
}
```
