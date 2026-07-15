[![Codacy Badge](https://api.codacy.com/project/badge/Grade/cace4ee9e7f7420d8782812062669e56)](https://www.codacy.com/app/joaofel/money-api?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=joaofel/money-api&amp;utm_campaign=Badge_Grade)

Aplicação para aprendizado Webservice Rest.

Ferramentas utilizadas:

    Spring Boot 3.5;
    Spring Security 6 (Resource Server / JWT);
    Spring Rest;
    Spring JPA (Hibernate 6);
    JAVA 21;
    Flyway;

Funcionamento:

    A aplicação deve controlar um sistema de gerenciamento de despesas e receitas.
    API sera consumida por um client em Angular.

Autenticação:

    O login é feito em POST /oauth/token (grant_type=password) usando Basic Auth
    do client (angular / mobile). O endpoint devolve um access token JWT (HS256) e
    grava o refresh token em cookie httpOnly. A renovação usa grant_type=refresh_token
    e a revogação do cookie é feita em DELETE /tokens/revoke.
