package ac.su.kdt.beusermanagementservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DevTrip User Management Service API")
                        .version("1.0")
                        .description("""
                                DevTrip 플랫폼의 사용자 관리 서비스 API입니다.
                                
                                주요 기능:
                                - 사용자 프로필 관리
                                - 사용자 대시보드
                                - 사용자 여권(완료한 미션 기록)
                                - Auth 서비스와의 사용자 데이터 동기화
                                """)
                        .contact(new Contact()
                                .name("DevTrip Team")
                                .email("support@devtrip.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8082")
                                .description("Local Development Server"),
                        new Server()
                                .url("http://user-service:8082")
                                .description("Docker/Kubernetes Environment")
                ));
    }
}