package ac.su.kdt.beusermanagementservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    // @Bean: 이 메서드가 반환하는 객체(여기서는 RestTemplate)를 Spring 컨테이너가 관리하는 Bean으로 등록하라는 의미
    // 이렇게 등록해두면 다른 컴포넌트(@Service, @Component 등)에서 생성자 주입 등을 통해 이 객체를 편리하게 사용할 수 있음
    @Bean
    public RestTemplate restTemplate() {
        // RestTemplate: Spring 프레임워크에서 제공하는 동기(synchronous) 방식의 HTTP 통신 클라이언트
        // 이 객체를 사용하면 다른 서비스의 REST API를 간편하게 호출하고 응답을 받을 수 있음
        // 여기서는 new RestTemplate()을 통해 가장 기본적인 RestTemplate 객체를 생성하여 반환
        return new RestTemplate();
    }
}