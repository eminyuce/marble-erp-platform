CREATE TABLE application_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    level VARCHAR(16) NOT NULL,
    logger_name VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    exception_class VARCHAR(255),
    exception_message TEXT,
    stack_trace TEXT,
    username VARCHAR(100),
    client_ip VARCHAR(100),
    http_method VARCHAR(16),
    request_uri VARCHAR(500),
    correlation_id VARCHAR(64),
    thread_name VARCHAR(100)
);

CREATE INDEX idx_application_logs_timestamp ON application_logs(timestamp DESC);
CREATE INDEX idx_application_logs_level ON application_logs(level);
CREATE INDEX idx_application_logs_username ON application_logs(username);
CREATE INDEX idx_application_logs_logger ON application_logs(logger_name);
