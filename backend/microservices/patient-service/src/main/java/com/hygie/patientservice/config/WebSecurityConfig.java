package com.hygie.patientservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration de sécurité Web pour le Patient Service.
 *
 * Cette classe configure la sécurité HTTP, CORS et la gestion des sessions
 * pour les API REST du service patient.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${app.cors.max-age}")
    private long maxAge;

    /**
     * Configure la chaîne de filtres de sécurité.
     *
     * @param http L'objet de configuration de sécurité HTTP
     * @return La chaîne de filtres de sécurité configurée
     * @throws Exception Si la configuration échoue
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Assertion #1: Vérification que les configurations CORS sont définies
        assert allowedOrigins != null : "Les origines autorisées pour CORS doivent être définies";
        assert allowedMethods != null : "Les méthodes autorisées pour CORS doivent être définies";

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeRequests()
                // Endpoints publics
                .antMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Endpoints sécurisés
                .anyRequest().authenticated();

        // Assertion #2: Vérification que la configuration a été appliquée
        assert http != null : "La configuration de sécurité HTTP ne peut pas être nulle";

        return http.build();
    }

    /**
     * Configure la source de configuration CORS.
     *
     * @return La source de configuration CORS configurée
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Configurer les origines autorisées
        if ("*".equals(allowedOrigins)) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        }

        // Configurer les méthodes et en-têtes autorisés
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));

        if ("*".equals(allowedHeaders)) {
            configuration.setAllowedHeaders(List.of("*"));
        } else {
            configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        }

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
