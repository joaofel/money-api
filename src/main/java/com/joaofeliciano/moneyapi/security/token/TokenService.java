package com.joaofeliciano.moneyapi.security.token;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.joaofeliciano.moneyapi.config.property.MoneyApiProperty;
import com.joaofeliciano.moneyapi.util.UsuarioSistema;

/**
 * Emite os tokens JWT da aplicação. Substitui o antigo par
 * {@code JwtAccessTokenConverter} + {@code CustomTokenEnhancer}.
 */
@Profile("oauth-security")
@Service
public class TokenService {

	private static final String ISSUER = "money-api";
	private static final String REFRESH_TOKEN_TYPE = "refresh";

	private final JwtEncoder jwtEncoder;
	private final MoneyApiProperty moneyApiProperty;

	public TokenService(JwtEncoder jwtEncoder, MoneyApiProperty moneyApiProperty) {
		this.jwtEncoder = jwtEncoder;
		this.moneyApiProperty = moneyApiProperty;
	}

	/**
	 * Gera o access token contendo as permissões do usuário (claim
	 * {@code authorities}), os escopos concedidos (claim {@code scope}) e o
	 * nome do usuário (claim {@code nome}).
	 */
	public String gerarAccessToken(UsuarioSistema usuario, Set<String> escopos) {
		Instant agora = Instant.now();

		List<String> authorities = usuario.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.issuedAt(agora)
				.expiresAt(agora.plusSeconds(moneyApiProperty.getJwt().getAccessTokenValiditySeconds()))
				.subject(usuario.getUsername())
				.claim("nome", usuario.getUsuario().getNome())
				.claim("authorities", authorities)
				.claim("scope", String.join(" ", escopos))
				.build();

		return encode(claims);
	}

	public String gerarRefreshToken(String username) {
		Instant agora = Instant.now();

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.issuedAt(agora)
				.expiresAt(agora.plusSeconds(moneyApiProperty.getJwt().getRefreshTokenValiditySeconds()))
				.subject(username)
				.claim("type", REFRESH_TOKEN_TYPE)
				.build();

		return encode(claims);
	}

	public boolean isRefreshToken(org.springframework.security.oauth2.jwt.Jwt jwt) {
		return REFRESH_TOKEN_TYPE.equals(jwt.getClaimAsString("type"));
	}

	public int getAccessTokenValiditySeconds() {
		return moneyApiProperty.getJwt().getAccessTokenValiditySeconds();
	}

	private String encode(JwtClaimsSet claims) {
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
