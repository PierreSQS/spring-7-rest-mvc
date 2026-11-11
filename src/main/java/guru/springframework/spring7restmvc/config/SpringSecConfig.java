package guru.springframework.spring7restmvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 11-11-2026
 */
@Configuration
@EnableWebSecurity
public class SpringSecConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(httpOAuth2ResServerConfigurer ->
                        httpOAuth2ResServerConfigurer.jwt(Customizer.withDefaults()));

        return http.build();
    }

}
