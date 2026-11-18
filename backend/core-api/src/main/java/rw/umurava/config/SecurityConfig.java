package rw.umurava.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;
@Configuration
public class SecurityConfig {
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  return http.csrf(AbstractHttpConfigurer::disable).cors(cors -> {})
   .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
   .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.GET,"/actuator/health","/api/v1/system/info").permitAll().requestMatchers(HttpMethod.OPTIONS,"/**").permitAll().anyRequest().denyAll()).build();
 }
 @Bean CorsConfigurationSource corsConfigurationSource(@Value("${umurava.cors.allowed-origins:http://localhost:3000}") String origins) {
  CorsConfiguration c=new CorsConfiguration(); c.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList());
  c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS")); c.setAllowedHeaders(List.of("Authorization","Content-Type","X-Request-Id")); c.setAllowCredentials(false);
  UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource(); s.registerCorsConfiguration("/api/**",c); return s;
 }
}
