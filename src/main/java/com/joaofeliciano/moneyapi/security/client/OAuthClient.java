package com.joaofeliciano.moneyapi.security.client;

import java.util.Set;

/**
 * Cliente OAuth cadastrado em memória (equivalente ao antigo
 * {@code ClientDetailsServiceConfigurer.inMemory()}).
 */
public record OAuthClient(String clientId, String secret, Set<String> scopes) {

	public boolean matches(String clientId, String secret) {
		return this.clientId.equals(clientId) && this.secret.equals(secret);
	}
}
