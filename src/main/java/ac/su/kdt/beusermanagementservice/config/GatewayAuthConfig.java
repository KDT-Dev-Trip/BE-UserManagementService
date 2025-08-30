package ac.su.kdt.beusermanagementservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Gateway에서 전달되는 사용자 인증 헤더 처리
 * /gateway/** 경로로 들어오는 요청의 X-User-Id, X-User-Email, X-User-Role 헤더를 추출
 */
@Slf4j
@Configuration
public class GatewayAuthConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new GatewayAuthInterceptor())
                .addPathPatterns("/api/**") // 모든 API 경로에 적용
                .excludePathPatterns("/api/health"); // health check 제외
    }
    
    public static class GatewayAuthInterceptor implements HandlerInterceptor {
        
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            try {
                String userId = request.getHeader("X-User-Id");
                String userEmail = request.getHeader("X-User-Email");
                String userRole = request.getHeader("X-User-Role");
                String gatewayRoute = request.getHeader("X-Gateway-Route");
                
                log.info("GatewayAuthInterceptor - Processing request: {} {}, Gateway-Route: {}", 
                         request.getMethod(), request.getRequestURI(), gatewayRoute);
                
                if (gatewayRoute != null) {
                    log.info("Gateway request detected - Route: {}, User: {}, Email: {}, Role: {}", 
                            gatewayRoute, userId, userEmail, userRole);
                    
                    // 헤더 정보를 request attributes에 저장하여 컨트롤러에서 사용할 수 있게 함
                    if (userId != null && !userId.trim().isEmpty()) {
                        request.setAttribute("gateway.user.id", userId);
                    }
                    if (userEmail != null && !userEmail.trim().isEmpty()) {
                        request.setAttribute("gateway.user.email", userEmail);
                    }
                    if (userRole != null && !userRole.trim().isEmpty()) {
                        request.setAttribute("gateway.user.role", userRole);
                    }
                    request.setAttribute("gateway.authenticated", true);
                } else {
                    log.debug("Direct access (non-gateway) to {} {}", request.getMethod(), request.getRequestURI());
                }
                
                return true;
                
            } catch (Exception e) {
                log.error("Error in GatewayAuthInterceptor for {} {}: {}", 
                         request.getMethod(), request.getRequestURI(), e.getMessage(), e);
                // 인터셉터 오류가 있어도 요청을 계속 진행
                return true;
            }
        }
    }
}