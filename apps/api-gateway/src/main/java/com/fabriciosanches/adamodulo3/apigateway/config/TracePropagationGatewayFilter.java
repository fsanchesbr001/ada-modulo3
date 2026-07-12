package com.fabriciosanches.adamodulo3.apigateway.config;

import com.fabriciosanches.adamodulo3.observability.TraceIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class TracePropagationGatewayFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Object traceId = request.getAttribute(TraceIdFilter.TRACE_ID_HEADER);
        if (traceId != null) {
            response.setHeader(TraceIdFilter.TRACE_ID_HEADER, traceId.toString());
        }
        filterChain.doFilter(request, response);
    }
}
