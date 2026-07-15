package com.joaofeliciano.moneyapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança do perfil {@code basic-security} (HTTP Basic,
 * sem sessão). Reescrita a partir do antigo {@code WebSecurityConfigurerAdapter}
 * (removido no Spring Security 6) para o modelo baseado em {@link SecurityFilterChain}.
 */
@Profile("basic-security")
@Configuration
@EnableWebSecurity
public class BasicSecurityConfig {

	@Bean
	public SecurityFilterChain basicSecurityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(authorize -> authorize
				.anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults())
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.csrf(csrf -> csrf.disable());

		return http.build();
	}
}
