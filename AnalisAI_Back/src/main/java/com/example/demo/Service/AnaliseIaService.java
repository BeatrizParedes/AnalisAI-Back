package com.example.demo.Service;

import com.example.demo.DTO.AnaliseTarefaRequest;
import com.example.demo.DTO.AnaliseTarefaResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;


@Service
public class AnaliseIaService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    public AnaliseIaService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    // ==============================
    // CHAT LIVRE (Novo Método)
    // ==============================
    public String chatLivre(String mensagemUsuario) {
        String promptSistema = "Você é a AnalisAI, uma assistente especialista em gestão ágil e Jira. " +
                "Seja breve, útil e amigável. Responda em texto simples (sem Markdown complexo). " +
                "Pergunta do usuário: " + mensagemUsuario;

        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", promptSistema)))
                )
        );

        try {
            String rawResponse = webClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(rawResponse);
            return root.path("candidates").path(0).path("content")
                    .path("parts").path(0).path("text").asText();

        } catch (Exception e) {
            return "Desculpe, tive um problema ao processar sua mensagem: " + e.getMessage();
        }
    }

    // ==============================
    // ANÁLISE DE TAREFAS
    // ==============================
    public AnaliseTarefaResponse analisarTarefa(AnaliseTarefaRequest request) {
        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "responseSchema", buildResponseSchema()
        );

        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", buildPrompt(request))))
                ),
                "generationConfig", generationConfig
        );

        try {
            String rawResponse = webClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, resp -> resp.bodyToMono(String.class)
                            .flatMap(error -> Mono.error(new RuntimeException("Erro 4xx Gemini: " + error))))
                    .onStatus(HttpStatusCode::is5xxServerError, resp -> Mono.error(new RuntimeException("Erro 5xx Gemini")))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            JsonNode root = objectMapper.readTree(rawResponse);
            String jsonText = root.path("candidates").path(0).path("content")
                    .path("parts").path(0).path("text").asText();

            if (jsonText == null || jsonText.isEmpty()) {
                throw new RuntimeException("IA retornou JSON vazio.");
            }

            return objectMapper.readValue(jsonText, AnaliseTarefaResponse.class);

        } catch (WebClientResponseException e) {
            throw new RuntimeException("Erro ao chamar Gemini: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            // Retorna um objeto de erro amigável para não quebrar o front
            return new AnaliseTarefaResponse(
                    request.key(),
                    List.of("ERRO: " + e.getMessage()),
                    "Falha na análise",
                    List.of(), List.of(), List.of("Tente novamente mais tarde.")
            );
        }
    }

    private String buildPrompt(AnaliseTarefaRequest request) {
        return String.format("""
            Você é um Analista de Projetos Sênior especializado em automação de RH.
            Gere uma análise de Riscos, Dependências e Sugestões para a tarefa abaixo.
            
            DADOS DA TAREFA:
            • Tipo: %s | Chave: %s | Status: %s | Responsável: %s
            • Resumo: %s
            • Atualizada em: %s
            
            INSTRUÇÕES:
            • Baseie a análise no resumo da tarefa.
            • Seja conciso, objetivo e técnico.
            • Retorne EXCLUSIVAMENTE JSON seguindo o schema enviado.
            """,
                request.issuetype(), request.key(), request.status(), request.assignee(),
                request.summary(), request.updated()
        );
    }

    private JsonNode buildResponseSchema() {
        try {
            String schemaJson = """
                {
                    "type": "OBJECT",
                    "properties": {
                        "key": { "type": "STRING" },
                        "riscosIdentificados": { "type": "ARRAY", "items": { "type": "STRING" } },
                        "resumoRiscos": { "type": "STRING" },
                        "dependenciasObrigatorias": { "type": "ARRAY", "items": { "type": "STRING" } },
                        "dependenciasSugeridas": { "type": "ARRAY", "items": { "type": "STRING" } },
                        "sugestoesOtimizacao": { "type": "ARRAY", "items": { "type": "STRING" } }
                    },
                    "required": ["key", "riscosIdentificados", "resumoRiscos",
                                 "dependenciasObrigatorias", "dependenciasSugeridas", "sugestoesOtimizacao"]
                }
            """;
            return objectMapper.readTree(schemaJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao construir schema JSON", e);
        }
    }
}