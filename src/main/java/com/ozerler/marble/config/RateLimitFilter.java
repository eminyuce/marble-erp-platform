package com.ozerler.marble.config;

import com.ozerler.marble.service.RateLimitService;
import com.ozerler.marble.service.RateLimitService.Tier;
import com.ozerler.marble.service.SettingService;
import com.ozerler.marble.util.ClientIps;
import com.ozerler.marble.util.HttpRequests;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * IP tabanlı istek sınırlandırma filtresi.
 * Aktiflik durumu admin panelindeki "security.rate_limiting.enabled" ayarına bağlıdır.
 * Katman (tier) belirleme: login > form > general.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final SettingService settingService;

    private static final String SETTING_KEY = "security.rate_limiting.enabled";

    private static final String JSON_RESPONSE = """
            {"error":"too_many_requests","message":"Çok fazla istek gönderdiniz. Lütfen bir süre bekleyip tekrar deneyin."}""";

    private static final String HTML_RESPONSE = """
            <!DOCTYPE html>
            <html lang="tr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>429 - Çok Fazla İstek</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                           display: flex; align-items: center; justify-content: center;
                           min-height: 100vh; margin: 0; background: #f8fafc; color: #334155; }
                    .card { text-align: center; max-width: 480px; padding: 3rem 2rem;
                            background: white; border-radius: 1rem; border: 1px solid #e2e8f0;
                            box-shadow: 0 4px 6px -1px rgba(0,0,0,.1); }
                    h1 { font-size: 4rem; color: #e11d48; margin: 0 0 .5rem; }
                    h2 { font-size: 1.25rem; margin: 0 0 1rem; color: #1e293b; }
                    p  { font-size: .875rem; line-height: 1.6; color: #64748b; }
                    a  { display: inline-block; margin-top: 1.5rem; padding: .6rem 1.5rem;
                         background: #2b6cb0; color: white; border-radius: .5rem;
                         text-decoration: none; font-size: .875rem; font-weight: 600; }
                    a:hover { background: #1e4e8c; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>429</h1>
                    <h2>Çok Fazla İstek</h2>
                    <p>Kısa süre içinde çok fazla istek gönderdiniz.<br>
                       Lütfen bir süre bekleyip tekrar deneyin.</p>
                    <a href="/">Ana Sayfaya Dön</a>
                </div>
            </body>
            </html>""";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Rate limiting devre dışıysa doğrudan geç
        if (!settingService.getBooleanSetting(SETTING_KEY, true)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = ClientIps.from(request);
        Tier tier = determineTier(request);

        if (!rateLimitService.isAllowed(clientIp, tier)) {
            log.warn("HTTP 429 — IP: {}, Endpoint: {} {}, Tier: {}",
                    clientIp, request.getMethod(), request.getRequestURI(), tier.name());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");

            if (HttpRequests.expectsJson(request)) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(JSON_RESPONSE);
            } else {
                response.setContentType(MediaType.TEXT_HTML_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(HTML_RESPONSE);
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Statik kaynakları ve health check'leri filtreden hariç tut
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/vendor/")
                || path.startsWith("/fonts/")
                || path.startsWith("/images/")
                || path.startsWith("/uploads/")
                || path.startsWith("/favicon.ico")
                || path.startsWith("/error")
                || path.startsWith("/actuator")
                || path.startsWith("/health");
    }

    /**
     * İstek yoluna ve HTTP metoduna göre katman (tier) belirler.
     * Login > Form > General sıralamasıyla en sıkı kural uygulanır.
     */
    private Tier determineTier(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Login endpoint'leri — en sıkı limit
        if (path.contains("/login") || path.contains("/adminlogin")) {
            if ("POST".equalsIgnoreCase(method)) {
                return Tier.LOGIN;
            }
        }

        // Form gönderim endpoint'leri — orta seviye limit
        if ("POST".equalsIgnoreCase(method)) {
            if (path.contains("/create")
                    || path.contains("/contact")
                    || path.contains("/save")
                    || path.contains("/register")
                    || path.contains("/change-password")) {
                return Tier.FORM;
            }
        }

        return Tier.GENERAL;
    }
}
