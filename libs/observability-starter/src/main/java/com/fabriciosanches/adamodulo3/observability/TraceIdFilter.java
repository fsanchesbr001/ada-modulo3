package com.fabriciosanches.adamodulo3.observability;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "trace_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        request.setAttribute(TRACE_ID_HEADER, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        filterChain.doFilter(request, response);
    }
}
