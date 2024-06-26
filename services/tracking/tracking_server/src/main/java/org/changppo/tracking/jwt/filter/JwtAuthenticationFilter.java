package org.changppo.tracking.jwt.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.changppo.commons.FailedResponseBody;
import org.changppo.tracking.jwt.exception.JwtAuthenticationException;
import org.changppo.tracking.jwt.exception.JwtNotExistException;
import org.changppo.tracking.jwt.exception.JwtTokenExpiredException;
import org.changppo.tracking.service.TrackingService;
import org.changppo.utils.jwt.tracking.TrackingJwtClaims;
import org.changppo.utils.jwt.tracking.TrackingJwtHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final AuthenticationManager authenticationManager;
    private final TrackingJwtHandler trackingJwtHandler;
    private final TrackingService trackingService;
    private final RequestMatcher requestMatcher = new RequestHeaderRequestMatcher(HttpHeaders.AUTHORIZATION);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!requestMatcher.matches(request)) { // 헤더에서 Authorization 필드 확인
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String tokenValue = resolveToken(request).orElseThrow(JwtNotExistException::new);
            JwtAuthenticationToken token = new JwtAuthenticationToken(tokenValue); // 인증되지 않은 토큰
            Authentication authentication = this.authenticationManager.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtAuthenticationException e) {
            this.handleJwtAuthenticationException(response, e);
        }

    }

    private Optional<String> resolveToken(HttpServletRequest request){
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(bearerToken != null && bearerToken.startsWith("Bearer ")){
            return Optional.of(bearerToken.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }

    private void handleJwtAuthenticationException(HttpServletResponse response, JwtAuthenticationException e) throws IOException {
        if (e instanceof JwtTokenExpiredException) { // 재발급
            Claims claims = ((JwtTokenExpiredException) e).getClaims();
            TrackingJwtClaims trackingClaims = trackingJwtHandler.convert(claims);
            try {
                trackingService.validateApikey(trackingClaims.getApikeyId());
                String newToken = trackingJwtHandler.createToken(trackingClaims);
                response.setHeader("new-token", newToken);
            } catch (Exception ex) {
                log.warn("토큰 자동 재발급시 apikey가 invalid 해서 실패 : {}", e.getMessage());
            }
        }
        response.setStatus(e.getErrorCode().getStatus());
        response.setContentType("application/json;charset=UTF-8");
        ObjectMapper objectMapper = new ObjectMapper();
        String errorResponse = objectMapper.writeValueAsString(
                new FailedResponseBody<>(e.getErrorCode().getCode(), e.getErrorCode().getMessage()));
        response.getWriter().write(errorResponse);
        response.flushBuffer(); // 커밋
        response.getWriter().close();
    }

}
