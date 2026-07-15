package com.joaofeliciano.moneyapi.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("moneyapi-seguranca")
public class MoneyApiProperty {

	private String origemPermitida = "http://localhost:8080";
	private final Seguranca seguranca = new Seguranca();
	private final Jwt jwt = new Jwt();

	public Seguranca getSeguranca() {
		return seguranca;
	}

	public Jwt getJwt() {
		return jwt;
	}

	public String getOrigemPermitida() {
		return origemPermitida;
	}

	public void setOrigemPermitida(String origemPermitida) {
		this.origemPermitida = origemPermitida;
	}

	public static class Seguranca {

		private boolean enableHttps;

		public boolean isEnableHttps() {
			return enableHttps;
		}

		public void setEnableHttps(boolean enableHttps) {
			this.enableHttps = enableHttps;
		}
	}

	public static class Jwt {

		/**
		 * Chave secreta usada para assinar os tokens JWT (HMAC-SHA256).
		 * Deve ter no mínimo 32 caracteres (256 bits). Sobrescreva em produção.
		 */
		private String secret = "money-api-chave-secreta-troque-em-producao-2024";

		/** Validade do access token em segundos (padrão 30 minutos). */
		private int accessTokenValiditySeconds = 1800;

		/** Validade do refresh token em segundos (padrão 24 horas). */
		private int refreshTokenValiditySeconds = 3600 * 24;

		public String getSecret() {
			return secret;
		}

		public void setSecret(String secret) {
			this.secret = secret;
		}

		public int getAccessTokenValiditySeconds() {
			return accessTokenValiditySeconds;
		}

		public void setAccessTokenValiditySeconds(int accessTokenValiditySeconds) {
			this.accessTokenValiditySeconds = accessTokenValiditySeconds;
		}

		public int getRefreshTokenValiditySeconds() {
			return refreshTokenValiditySeconds;
		}

		public void setRefreshTokenValiditySeconds(int refreshTokenValiditySeconds) {
			this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
		}
	}
}
