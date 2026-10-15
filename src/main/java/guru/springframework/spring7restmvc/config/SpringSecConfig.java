package guru.springframework.spring7restmvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Created by Pierrot on 16-10-2026
 */
@Configuration
public class SpringSecConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
        httpSecurity.csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**"));

        httpSecurity.httpBasic(Customizer.withDefaults());

        httpSecurity.authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated());
        return httpSecurity.build();
    }
}
