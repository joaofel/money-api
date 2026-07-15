package com.joaofeliciano.moneyapi.security.client;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Registro em memória dos clientes OAuth aceitos pelo endpoint de token.
 * Reproduz os clientes {@code angular} e {@code mobile} que antes eram
 * declarados no {@code AuthorizationServerConfig}.
 */
@Profile("oauth-security")
@Component
public class OAuthClientRegistry {

	private final Map<String, OAuthClient> clients = new LinkedHashMap<>();

	public OAuthClientRegistry() {
		register(new OAuthClient("angular", "@ngul@r0", Set.of("read", "write")));
		register(new OAuthClient("mobile", "m0bile0", Set.of("read")));
	}

	private void register(OAuthClient client) {
		clients.put(client.clientId(), client);
	}

	/**
	 * Valida as credenciais recebidas no header {@code Authorization: Basic ...}
	 * e devolve o cliente correspondente.
	 */
	public Optional<OAuthClient> authenticate(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.toLowerCase().startsWith("basic ")) {
			return Optional.empty();
		}

		String base64 = authorizationHeader.substring("basic ".length()).trim();
		String decoded;
		try {
			decoded = new String(Base64.getDecoder().decode(base64));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}

		int separator = decoded.indexOf(':');
		if (separator < 0) {
			return Optional.empty();
		}

		String clientId = decoded.substring(0, separator);
		String secret = decoded.substring(separator + 1);

		OAuthClient client = clients.get(clientId);
		if (client == null || !client.matches(clientId, secret)) {
			return Optional.empty();
		}
		return Optional.of(client);
	}
}
