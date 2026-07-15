package com.joaofeliciano.moneyapi.resource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.joaofeliciano.moneyapi.config.property.MoneyApiProperty;
import com.joaofeliciano.moneyapi.security.client.OAuthClient;
import com.joaofeliciano.moneyapi.security.client.OAuthClientRegistry;
import com.joaofeliciano.moneyapi.security.token.TokenService;
import com.joaofeliciano.moneyapi.util.UsuarioSistema;

/**
 * Endpoint de emissão e revogação de tokens.
 *
 * <p>Reimplementa, com componentes do Spring Security 6, o fluxo que antes era
 * provido pela stack legada Spring Security OAuth2: os grants {@code password} e
 * {@code refresh_token} do endpoint {@code /oauth/token}, o armazenamento do
 * refresh token em cookie {@code httpOnly} e a revogação em {@code /tokens/revoke}.
 */
@Profile("oauth-security")
@RestController
public class TokenResource {

	private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;
	private final OAuthClientRegistry clientRegistry;
	private final TokenService tokenService;
	private final JwtDecoder jwtDecoder;
	private final MoneyApiProperty moneyApiProperty;

	public TokenResource(AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
			OAuthClientRegistry clientRegistry, TokenService tokenService, JwtDecoder jwtDecoder,
			MoneyApiProperty moneyApiProperty) {
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
		this.clientRegistry = clientRegistry;
		this.tokenService = tokenService;
		this.jwtDecoder = jwtDecoder;
		this.moneyApiProperty = moneyApiProperty;
	}

	@PostMapping(path = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<Object> token(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestParam("grant_type") String grantType,
			@RequestParam(name = "username", required = false) String username,
			@RequestParam(name = "password", required = false) String password,
			HttpServletRequest request, HttpServletResponse response) {

		Optional<OAuthClient> clientOptional = clientRegistry.authenticate(authorization);
		if (clientOptional.isEmpty()) {
			return erro(HttpStatus.UNAUTHORIZED, "invalid_client", "Cliente inválido.");
		}
		OAuthClient client = clientOptional.get();

		return switch (grantType) {
			case "password" -> passwordGrant(client, username, password, request, response);
			case "refresh_token" -> refreshTokenGrant(client, request, response);
			default -> erro(HttpStatus.BAD_REQUEST, "unsupported_grant_type",
					"Grant type não suportado: " + grantType);
		};
	}

	private ResponseEntity<Object> passwordGrant(OAuthClient client, String username, String password,
			HttpServletRequest request, HttpServletResponse response) {
		if (username == null || password == null) {
			return erro(HttpStatus.BAD_REQUEST, "invalid_request", "username e password são obrigatórios.");
		}

		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(username, password));
			UsuarioSistema usuario = (UsuarioSistema) authentication.getPrincipal();
			return emitirTokens(usuario, client.scopes(), request, response);
		} catch (AuthenticationException e) {
			return erro(HttpStatus.BAD_REQUEST, "invalid_grant", "Usuário e/ou senha incorretos.");
		}
	}

	private ResponseEntity<Object> refreshTokenGrant(OAuthClient client, HttpServletRequest request,
			HttpServletResponse response) {
		String refreshToken = lerRefreshTokenDoCookie(request);
		if (refreshToken == null) {
			return erro(HttpStatus.BAD_REQUEST, "invalid_request", "Refresh token não encontrado.");
		}

		try {
			Jwt jwt = jwtDecoder.decode(refreshToken);
			if (!tokenService.isRefreshToken(jwt)) {
				return erro(HttpStatus.BAD_REQUEST, "invalid_grant", "Token informado não é um refresh token.");
			}
			UsuarioSistema usuario = (UsuarioSistema) userDetailsService.loadUserByUsername(jwt.getSubject());
			return emitirTokens(usuario, client.scopes(), request, response);
		} catch (JwtException | UsernameNotFoundException e) {
			return erro(HttpStatus.BAD_REQUEST, "invalid_grant", "Refresh token inválido ou expirado.");
		}
	}

	private ResponseEntity<Object> emitirTokens(UsuarioSistema usuario, Set<String> escopos,
			HttpServletRequest request, HttpServletResponse response) {
		String accessToken = tokenService.gerarAccessToken(usuario, escopos);
		String refreshToken = tokenService.gerarRefreshToken(usuario.getUsername());

		adicionarRefreshTokenCookie(refreshToken, request, response);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("access_token", accessToken);
		body.put("token_type", "Bearer");
		body.put("expires_in", tokenService.getAccessTokenValiditySeconds());
		body.put("scope", String.join(" ", escopos));
		body.put("nome", usuario.getUsuario().getNome());

		return ResponseEntity.ok(body);
	}

	@DeleteMapping("/tokens/revoke")
	public ResponseEntity<Void> revoke(HttpServletRequest request, HttpServletResponse response) {
		Cookie cookie = criarRefreshTokenCookie(null, request);
		cookie.setMaxAge(0);
		response.addCookie(cookie);
		return ResponseEntity.noContent().build();
	}

	private String lerRefreshTokenDoCookie(HttpServletRequest request) {
		if (request.getCookies() == null) {
			return null;
		}
		for (Cookie cookie : request.getCookies()) {
			if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private void adicionarRefreshTokenCookie(String refreshToken, HttpServletRequest request,
			HttpServletResponse response) {
		Cookie cookie = criarRefreshTokenCookie(refreshToken, request);
		cookie.setMaxAge(2592000);
		response.addCookie(cookie);
	}

	private Cookie criarRefreshTokenCookie(String valor, HttpServletRequest request) {
		Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, valor);
		cookie.setHttpOnly(true);
		cookie.setSecure(moneyApiProperty.getSeguranca().isEnableHttps());
		cookie.setPath(request.getContextPath() + "/oauth/token");
		return cookie;
	}

	private ResponseEntity<Object> erro(HttpStatus status, String error, String descricao) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("error", error);
		body.put("error_description", descricao);
		return ResponseEntity.status(status).body(body);
	}
}
