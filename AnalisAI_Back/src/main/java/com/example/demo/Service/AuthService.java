/*
 * package com.example.demo.Service;
 *
 * import com.example.demo.DTO.TokenResponse;
 * import org.slf4j.Logger;
 * import org.slf4j.LoggerFactory;
 * import org.springframework.beans.factory.annotation.Value;
 * import org.springframework.core.ParameterizedTypeReference;
 * import org.springframework.http.HttpHeaders;
 * import org.springframework.http.MediaType;
 * import org.springframework.stereotype.Service;
 * import org.springframework.web.reactive.function.client.WebClient;
 * import org.springframework.web.util.UriComponentsBuilder;
 * import reactor.core.publisher.Mono;
 *
 * import java.time.Instant;
 * import java.util.*;
 */

/**
 * Serviço responsável por gerenciar todo o fluxo de autenticação e autorização
 * OAuth 2.0 (3LO) com a API do Atlassian.
 *
 * Isso inclui:
 * 1. Geração da URL de autorização para o usuário final.
 * 2. Troca do "authorization code" por um "access token" e "refresh token".
 * 3. Atualização (refresh) de um "access token" expirado.
 * 4. Busca dos recursos acessíveis (sites Jira/Confluence) para um token.
 */
/*
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // Constantes para os endpoints da API de autenticação do Atlassian
    private static final String AUTH_BASE = "https://auth.atlassian.com";
    private static final String AUTH_TOKEN = AUTH_BASE + "/oauth/token";
    private static final String ACCESSIBLE_RESOURCES = "https://api.atlassian.com/oauth/token/accessible-resources";

    private final WebClient webClient;

    // Propriedades injetadas a partir do application.properties
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;
*/
/**
 * Construtor para injeção de dependências.
 * Inicializa o WebClient e armazena as configurações de OAuth do Atlassian
 * injetadas a partir das propriedades da aplicação.
 *
 * @param clientId     O Client ID da sua aplicação OAuth no Atlassian Developer Console.
 * @param clientSecret O Client Secret da sua aplicação OAuth.
 * @param redirectUri  A URL de callback para onde o Atlassian deve redirecionar o usuário após a autorização.
 * @param scope        Os escopos de permissão que sua aplicação está solicitando.
 */
/*
    public AuthService(
            @Value("${atlassian.client-id}") String clientId,
            @Value("${atlassian.client-secret}") String clientSecret,
            @Value("${atlassian.redirect-uri:http://localhost:8081/auth/callback}") String redirectUri,
            @Value("${atlassian.scope:read:jira-user read:jira-work write:jira-work manage:jira-project offline_access}") String scope
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.scope = scope;

        // Inicializa o WebClient, que será usado para todas as chamadas HTTP
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
*/
/**
 * 🔹 Gera a URL de autorização (login Atlassian)
 * Monta a URL para a qual o usuário deve ser redirecionado para iniciar o
 * processo de login e concessão de permissão (consentimento) à aplicação.
 *
 * @param clientId     O Client ID da aplicação.
 * @param redirectUri  A URL de callback para onde o usuário será enviado após o login.
 * @param scope        Os escopos de permissão solicitados.
 * @return Uma String contendo a URL de autorização completa.
 */
/*
    public String buildAuthorizationUrl(String clientId, String redirectUri, String scope) {
        // O 'state' é um valor aleatório usado para prevenir ataques CSRF.
        // Ele é enviado na requisição e deve ser validado no callback.
        String state = UUID.randomUUID().toString();

        log.info("Gerando URL de autorização Atlassian com redirect_uri={}", redirectUri);

        return UriComponentsBuilder.newInstance()
                .scheme("https")
                .host("auth.atlassian.com")
                .path("/authorize")
                .queryParam("audience", "api.atlassian.com") // Define o público-alvo da API
                .queryParam("client_id", clientId)
                .queryParam("scope", scope)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state) // Parâmetro anti-CSRF
                .queryParam("response_type", "code") // Indica que queremos um "authorization code"
                .queryParam("prompt", "consent") // Força o usuário a ver a tela de consentimento
                .build()
                .toUriString();
    }
*/
/**
 * 🔹 Troca o authorization code recebido por um access token + refresh token
 * Este método é chamado após o usuário ser redirecionado de volta para a aplicação
 * (no 'redirectUri') com um 'code' na URL.
 *
 * @param code O 'authorization_code' recebido como parâmetro na URL de callback.
 * @return Um objeto {@link TokenResponse} contendo o access token, refresh token e data de expiração.
 * @throws RuntimeException Se a chamada à API do Atlassian falhar.
 */
/*
    public TokenResponse exchangeCodeForToken(String code) {
        log.info("Trocando authorization code por token...");

        // Monta o corpo da requisição (payload)
        Map<String, Object> body = new HashMap<>();
        body.put("grant_type", "authorization_code"); // Indica o tipo de concessão
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("code", code); // O código recebido
        body.put("redirect_uri", redirectUri); // A mesma URI de redirecionamento usada na etapa 1

        // Executa a chamada POST para o endpoint de token
        Map<String, Object> resp = webClient.post()
                .uri(AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                // Tratamento de erro: se a resposta for um erro (4xx ou 5xx)
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Erro Atlassian ao trocar code: {}", errorBody);
                            return Mono.error(new RuntimeException("Erro Atlassian: " + errorBody));
                        })
                )
                // Converte a resposta para um Map
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                // Bloqueia a execução até a resposta ser recebida (em um cenário real, o Mono seria retornado)
                .block();

        // Converte o Map de resposta para o DTO TokenResponse
        return toTokenResponse(resp);
    }
*/
/**
 * 🔹 Atualiza o access token usando o refresh token
 * Quando o 'access_token' expira, este método usa o 'refresh_token'
 * (que tem vida longa) para obter um *novo* 'access_token' sem que o usuário
 * precise fazer login novamente.
 *
 * @param refreshToken O 'refresh_token' obtido durante a troca de código.
 * @return Um novo objeto {@link TokenResponse} com o novo access token.
 * @throws RuntimeException Se a chamada à API do Atlassian falhar.
 */
/*
    public TokenResponse refreshAccessToken(String refreshToken) {
        log.info("Atualizando access token via refresh token...");

        Map<String, Object> body = new HashMap<>();
        body.put("grant_type", "refresh_token"); // Indica o tipo de concessão
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("refresh_token", refreshToken); // O refresh token

        Map<String, Object> resp = webClient.post()
                .uri(AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                // Tratamento de erro
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Erro Atlassian ao atualizar token: {}", errorBody);
                            return Mono.error(new RuntimeException("Erro Atlassian: " + errorBody));
                        })
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        return toTokenResponse(resp);
    }
*/
/**
 * 🔹 Retorna os recursos Jira acessíveis para o token (útil pra obter o cloudId)
 * Após obter um 'access_token', este método busca a lista de "sites"
 * (instâncias do Jira, Confluence, etc.) aos quais o usuário deu permissão.
 * O 'cloudId' de um desses recursos é necessário para fazer chamadas
 * à API específica do Jira.
 *
 * @param accessToken O 'access_token' válido.
 * @return Uma Lista de Maps, onde cada Map representa um recurso acessível (contendo 'id', 'url', 'name', 'scopes').
 * @throws RuntimeException Se a chamada à API do Atlassian falhar.
 */
/*
    public List<Map<String, Object>> getAccessibleResources(String accessToken) {
        log.info("Buscando accessible-resources com token atual...");

        return webClient.get()
                .uri(ACCESSIBLE_RESOURCES)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken) // Adiciona o token no header
                .retrieve()
                // Tratamento de erro
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Erro Atlassian ao buscar accessible-resources: {}", errorBody);
                            return Mono.error(new RuntimeException("Erro Atlassian: " + errorBody));
                        })
                )
                // A resposta aqui é uma Lista de objetos
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .block();
    }
*/
/**
 * 🔹 Converte o body da resposta em um TokenResponse DTO
 * Método utilitário privado para transformar a resposta genérica (Map) da API
 * de token em um objeto DTO (Data Transfer Object) tipado.
 *
 * @param resp O Map<String, Object> deserializado da resposta JSON da API.
 * @return Um objeto {@link TokenResponse} preenchido.
 * @throws RuntimeException Se a resposta da API for nula.
 */
/*
    private TokenResponse toTokenResponse(Map<String, Object> resp) {
        if (resp == null) {
            throw new RuntimeException("Resposta nula ao obter token");
        }

        // Extrai os campos do Map, convertendo-os para os tipos esperados
        String accessToken = asString(resp.get("access_token"));
        String refreshToken = asString(resp.get("refresh_token"));
        String tokenType = asString(resp.get("token_type"));
        Integer expiresIn = Optional.ofNullable(resp.get("expires_in"))
                .map(v -> Integer.parseInt(String.valueOf(v))) // Converte para String e depois para Integer
                .orElse(null);
        String scopeResp = asString(resp.get("scope"));

        // Calcula o momento exato em que o token irá expirar
        Instant expiresAt = (expiresIn != null) ? Instant.now().plusSeconds(expiresIn) : null;

        // Assumindo que você tenha um DTO TokenResponse (não fornecido no snippet)
        // que aceite esses parâmetros.
        // return new TokenResponse(accessToken, refreshToken, tokenType, expiresIn, expiresAt, scopeResp);

        // Retorno fictício para compilar, já que TokenResponse não foi fornecido:
        // (No seu código real, a linha acima deve funcionar)
        log.info("Token processado com sucesso. AccessToken: ...{}", (accessToken != null ? accessToken.substring(Math.max(0, accessToken.length() - 6)) : "null"));
        // Simula o retorno, substitua pela sua classe DTO
        return new TokenResponse(accessToken, refreshToken, tokenType, expiresIn, expiresAt, scopeResp);
    }
*/
/**
 * Utilitário simples para converter um Objeto em String de forma segura
 * (evitando NullPointerException).
 *
 * @param o O objeto a ser convertido.
 * @return A representação em String do objeto, ou null se o objeto for nulo.
 */
/*
    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }
*/
/*
 * Definição fictícia do DTO TokenResponse, apenas para o código compilar.
 * Use a sua definição real.
 */
    /*
    private static class TokenResponse {
        public TokenResponse(String accessToken, String refreshToken, String tokenType, Integer expiresIn, Instant expiresAt, String scopeResp) {
            // Construtor
        }
    }
    */
//}