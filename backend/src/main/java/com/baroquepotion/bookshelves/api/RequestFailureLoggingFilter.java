package com.baroquepotion.bookshelves.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs only failed HTTP responses so runtime history stays signal-heavy.
 */
@Component
public class RequestFailureLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestFailureLoggingFilter.class);

    /**
     * Passes the request through and records only non-successful responses.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param filterChain remaining servlet filter chain
     * @throws ServletException if the downstream chain fails at the servlet layer
     * @throws IOException if request processing fails at the I/O layer
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        if (response.getStatus() >= 400) {
            log.warn("HTTP {} {} returned {}", request.getMethod(), request.getRequestURI(), response.getStatus());
        }
    }
}
