package com.joaofeliciano.moneyapi.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.joaofeliciano.moneyapi.config.property.MoneyApiProperty;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Configuração de segurança do perfil {@code oauth-security}.
 *
 * <p>Substitui a antiga stack Spring Security OAuth2 ({@code @EnableAuthorizationServer}
 * / {@code @EnableResourceServer}, removida no Spring Security 6) por um Resource
 * Server que valida tokens JWT assinados com HMAC-SHA256. A emissão dos tokens
 * (grant {@code password} / {@code refresh_token}) fica em
 * {@link com.joaofeliciano.moneyapi.resource.TokenResource}.
 */
@Profile("oauth-security")
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	private final MoneyApiProperty moneyApiProperty;

	public SecurityConfig(MoneyApiProperty moneyApiProperty) {
		this.moneyApiProperty = moneyApiProperty;
	}

	@Bean
	public SecurityFilterChain oauthSecurityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/oauth/token", "/tokens/**").permitAll()
				.anyRequest().authenticated())
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.csrf(csrf -> csrf.disable())
			.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

		return http.build();
	}

	private SecretKey secretKey() {
		byte[] bytes = moneyApiProperty.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	public JwtEncoder jwtEncoder() {
		return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withSecretKey(secretKey())
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
	}

	/**
	 * Converte o JWT em authorities combinando os escopos (claim {@code scope},
	 * mapeados para {@code SCOPE_*}) com as permissões da aplicação (claim
	 * {@code authorities}, que já contém os valores {@code ROLE_*}).
	 */
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> {
			Collection<GrantedAuthority> authorities = new ArrayList<>(scopesConverter.convert(jwt));

			List<String> permissoes = jwt.getClaimAsStringList("authorities");
			if (permissoes != null) {
				permissoes.forEach(permissao -> authorities.add(new SimpleGrantedAuthority(permissao)));
			}

			return authorities;
		});
		return converter;
	}
}
