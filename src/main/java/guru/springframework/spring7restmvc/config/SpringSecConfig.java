package guru.springframework.spring7restmvc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Created by Pierrot on 16-10-2026
 */
@Configuration
public class SpringSecConfig {
    public SecurityFilterChain securityWebFilterChain(HttpSecurity httpSecurity) {
        httpSecurity.csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**"));

        httpSecurity.authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated());
        return httpSecurity.build();
    }
}
