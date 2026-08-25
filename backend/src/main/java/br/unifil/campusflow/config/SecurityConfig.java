package br.unifil.campusflow.config;

import br.unifil.campusflow.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Somente o ADMIN opera o painel; REITOR e PROFESSOR sao solicitantes. */
    private static final String[] PERFIS_ADMINISTRATIVOS = { "ADMIN" };

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                // O proprio usuario sempre pode ler seu perfil e suas notificacoes
                .requestMatchers("/api/usuarios/me", "/api/notificacoes/**").authenticated()

                // Leitura de catalogo e do estado da grade: qualquer autenticado
                // (a visibilidade setorizada e aplicada no servico, nao aqui)
                .requestMatchers(HttpMethod.GET, "/api/salas", "/api/salas/**",
                                                 "/api/cursos", "/api/cursos/**",
                                                 "/api/periodo-grade", "/api/periodo-grade/**").authenticated()

                // Painel administrativo
                .requestMatchers("/api/usuarios", "/api/usuarios/**").hasAnyRole(PERFIS_ADMINISTRATIVOS)
                .requestMatchers("/api/relatorios/**").hasAnyRole(PERFIS_ADMINISTRATIVOS)
                .requestMatchers(HttpMethod.PUT, "/api/periodo-grade").hasAnyRole(PERFIS_ADMINISTRATIVOS)
                .requestMatchers(HttpMethod.GET, "/api/reservas/moderacao", "/api/reservas/moderacao/**",
                                                 "/api/propostas/moderacao", "/api/propostas/moderacao/**")
                    .hasAnyRole(PERFIS_ADMINISTRATIVOS)
                .requestMatchers("/api/propostas/*/gestor/**").hasAnyRole(PERFIS_ADMINISTRATIVOS)

                // Escrita de catalogo (CRUD de ambientes e cursos)
                .requestMatchers("/api/salas", "/api/salas/**",
                                 "/api/cursos", "/api/cursos/**").hasAnyRole(PERFIS_ADMINISTRATIVOS)

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
