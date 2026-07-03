CREATE TABLE plans (
    id                  UUID PRIMARY KEY,
    tier                VARCHAR(255),
    rate_limit          INT NOT NULL,
    max_retries         INT NOT NULL,
    payload_size_limit  INT NOT NULL
);

CREATE TABLE tenants (
    id                    UUID PRIMARY KEY,
    name                  VARCHAR(255),
    plan_id               UUID NOT NULL REFERENCES plans (id),
    status                VARCHAR(255),
    max_retries_override  INT NOT NULL,
    created_at            TIMESTAMPTZ
);

CREATE TABLE users (
    user_id        UUID PRIMARY KEY,
    tenant_id      UUID NOT NULL REFERENCES tenants (id),
    email          VARCHAR(255) UNIQUE,
    password_hash  VARCHAR(255),
    role           VARCHAR(255),
    created_at     TIMESTAMPTZ
);

CREATE TABLE api_keys (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL REFERENCES tenants (id),
    name        VARCHAR(255),
    key_hash    VARCHAR(255) UNIQUE,
    status      VARCHAR(255),
    created_at  TIMESTAMPTZ,
    expires_at  TIMESTAMPTZ
);

CREATE TABLE tenant_metrics (
    tenant_id           UUID PRIMARY KEY REFERENCES tenants (id),
    total_tasks         BIGINT NOT NULL,
    queued_tasks        BIGINT NOT NULL,
    running_tasks       BIGINT NOT NULL,
    scheduled_tasks     BIGINT NOT NULL,
    failed_tasks        BIGINT NOT NULL,
    retrying_tasks      BIGINT NOT NULL,
    dead_tasks          BIGINT NOT NULL,
    success_tasks       BIGINT NOT NULL,
    cancelled_tasks     BIGINT NOT NULL,
    avg_execution_ms    DOUBLE PRECISION NOT NULL,
    avg_queue_wait_ms   DOUBLE PRECISION NOT NULL,
    success_rate        DOUBLE PRECISION NOT NULL,
    failure_rate         DOUBLE PRECISION NOT NULL,
    rate_limit_hits      BIGINT NOT NULL,
    current_rate_limit   INT NOT NULL,
    tasks_moved_to_dlq   BIGINT NOT NULL,
    dlq_rate             DOUBLE PRECISION NOT NULL,
    computed_at           TIMESTAMPTZ
);

CREATE TABLE workers (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255),
    queue_type      VARCHAR(255),
    status          VARCHAR(255),
    last_heartbeat  TIMESTAMPTZ,
    started_at      TIMESTAMPTZ,
    hostname        VARCHAR(255),
    pid             INT NOT NULL,
    capacity        INT NOT NULL,
    version         VARCHAR(255)
);

CREATE TABLE tasks (
    id               UUID PRIMARY KEY,
    tenant_id        UUID NOT NULL REFERENCES tenants (id),
    priority         VARCHAR(255),
    task_type        VARCHAR(255),
    task_status      VARCHAR(255),
    payload          JSONB,
    payload_ref      VARCHAR(255),
    scheduled_at     TIMESTAMPTZ,
    retry_count      INT NOT NULL,
    created_at       TIMESTAMPTZ,
    moved_to_dlq_at  TIMESTAMPTZ
);

CREATE TABLE task_executions (
    id               UUID PRIMARY KEY,
    task_id          UUID NOT NULL REFERENCES tasks (id),
    worker_id        UUID REFERENCES workers (id),
    attempt_number   INT NOT NULL,
    started_at       TIMESTAMPTZ,
    finished_at      TIMESTAMPTZ,
    status           VARCHAR(255),
    error_type       VARCHAR(255),
    error_message    TEXT
);

CREATE INDEX idx_tasks_tenant_id ON tasks (tenant_id);
CREATE INDEX idx_task_executions_task_id ON task_executions (task_id);
CREATE INDEX idx_task_executions_worker_id ON task_executions (worker_id);
CREATE INDEX idx_users_tenant_id ON users (tenant_id);
CREATE INDEX idx_api_keys_tenant_id ON api_keys (tenant_id);
