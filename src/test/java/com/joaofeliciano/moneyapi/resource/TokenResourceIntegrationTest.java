package com.joaofeliciano.moneyapi.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Testa de ponta a ponta o novo fluxo de autenticação JWT (perfil
 * {@code oauth-security}): emissão de token via grant {@code password} e acesso
 * a um recurso protegido por permissão + escopo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TokenResourceIntegrationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void seedUsuario() {
		jdbcTemplate.update("DELETE FROM usuario_permissao");
		jdbcTemplate.update("DELETE FROM usuario");
		jdbcTemplate.update("DELETE FROM permissao");

		String senha = new BCryptPasswordEncoder().encode("admin");
		jdbcTemplate.update("INSERT INTO usuario (codigo, nome, email, senha) VALUES (?, ?, ?, ?)",
				1L, "Administrador", "admin@algamoney.com", senha);
		jdbcTemplate.update("INSERT INTO permissao (codigo, descricao) VALUES (?, ?)",
				2L, "ROLE_PESQUISAR_CATEGORIA");
		jdbcTemplate.update("INSERT INTO usuario_permissao (codigo_usuario, codigo_permissao) VALUES (?, ?)",
				1L, 2L);
	}

	@Test
	void deveEmitirAccessTokenComGrantPassword() {
		ResponseEntity<Map> resposta = solicitarToken("admin@algamoney.com", "admin");

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resposta.getBody()).containsKeys("access_token", "token_type", "expires_in", "scope", "nome");
		assertThat(resposta.getBody().get("token_type")).isEqualTo("Bearer");
		assertThat(resposta.getBody().get("nome")).isEqualTo("Administrador");
		assertThat(resposta.getBody().get("scope").toString()).contains("read");
	}

	@Test
	void deveRejeitarCredenciaisInvalidas() {
		ResponseEntity<Map> resposta = solicitarToken("admin@algamoney.com", "senha-errada");
		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void deveRejeitarClienteInvalido() {
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth("angular", "secret-errado");
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "password");
		form.add("username", "admin@algamoney.com");
		form.add("password", "admin");

		ResponseEntity<Map> resposta = restTemplate.postForEntity(
				"/oauth/token", new HttpEntity<>(form, headers), Map.class);
		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void deveAcessarRecursoProtegidoComTokenValido() {
		String accessToken = (String) solicitarToken("admin@algamoney.com", "admin").getBody().get("access_token");

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);

		ResponseEntity<String> resposta = restTemplate.exchange(
				"/categorias", HttpMethod.GET, new HttpEntity<>(headers), String.class);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void deveNegarRecursoProtegidoSemToken() {
		ResponseEntity<String> resposta = restTemplate.exchange(
				"/categorias", HttpMethod.GET, HttpEntity.EMPTY, String.class);
		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	private ResponseEntity<Map> solicitarToken(String username, String password) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth("angular", "@ngul@r0");
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "password");
		form.add("username", username);
		form.add("password", password);

		return restTemplate.postForEntity("/oauth/token", new HttpEntity<>(form, headers), Map.class);
	}
}
