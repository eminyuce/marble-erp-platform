package com.ozerler.marble.logging;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Drains queued log events when the Spring context closes.
 * Logback {@code stop()} remains the primary flush path (Spring Boot invokes it
 * via the logging shutdown hook). This bean is a best-effort extra drain so
 * events are not stranded if bean destruction races the logging system.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AsyncLoggingShutdownCoordinator implements DisposableBean {

    @Override
    public void destroy() {
        AsyncLoggingAppender.flushAllActive();
    }
}
