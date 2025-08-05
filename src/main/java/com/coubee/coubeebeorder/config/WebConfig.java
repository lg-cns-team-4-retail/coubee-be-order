@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 와일드카드(*) 대신 정확한 출처를 명시합니다.
                .allowedOrigins(
                        "http://localhost:5501", 
                        "https://coubee-api.murkui.com"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // .allowedOrigins()와 함께 사용하면 안전합니다.
                .maxAge(3600);
    }
}