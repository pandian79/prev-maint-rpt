package com.eginnovations.support.pmr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Service to interact with Ollama API for AI-powered analysis
 */
@Service
public class OllamaService {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);
    
    @Autowired
    private Environment env;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    // Default Ollama API endpoint
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String DEFAULT_MODEL = "llama2";
    private static final int DEFAULT_TIMEOUT = 60000; // 60 seconds
    
    /**
     * Generates a response from Ollama based on the given prompt
     * 
     * @param prompt The prompt to send to Ollama
     * @return The generated response
     * @throws IOException If there's an error communicating with Ollama
     */
    public String generateResponse(String prompt) throws IOException {
        String ollamaUrl = env.getProperty("ollama.api.url", DEFAULT_OLLAMA_URL);
        String model = env.getProperty("ollama.model", DEFAULT_MODEL);
        boolean enabled = Boolean.parseBoolean(env.getProperty("ollama.enabled", "true"));
        
        if (!enabled) {
            logger.warn("Ollama service is disabled in configuration");
            return getFallbackResponse();
        }
        
        logger.info("Sending prompt to Ollama (model: {}, url: {})", model, ollamaUrl);
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(ollamaUrl);
            
            // Build request JSON
            ObjectNode requestJson = objectMapper.createObjectNode();
            requestJson.put("model", model);
            requestJson.put("prompt", prompt);
            requestJson.put("stream", false);
            
            // Add optional parameters from configuration
            if (env.containsProperty("ollama.temperature")) {
                requestJson.put("temperature", Double.parseDouble(env.getProperty("ollama.temperature")));
            }
            if (env.containsProperty("ollama.max_tokens")) {
                requestJson.put("max_tokens", Integer.parseInt(env.getProperty("ollama.max_tokens")));
            }
            
            String requestBody = objectMapper.writeValueAsString(requestJson);
            request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
            request.setHeader("Content-Type", "application/json");
            
            logger.debug("Ollama request: {}", requestBody);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getCode();
                String responseBody = new String(
                    response.getEntity().getContent().readAllBytes(), 
                    StandardCharsets.UTF_8
                );
                
                logger.debug("Ollama response status: {}, body length: {}", statusCode, responseBody.length());
                
                if (statusCode == 200) {
                    JsonNode responseJson = objectMapper.readTree(responseBody);
                    String generatedText = responseJson.get("response").asText();
                    logger.info("Successfully received response from Ollama (length: {} chars)", generatedText.length());
                    return generatedText;
                } else {
                    logger.error("Ollama API error: status={}, body={}", statusCode, responseBody);
                    return getErrorResponse("Ollama API returned status " + statusCode);
                }
            }
            
        } catch (IOException e) {
            logger.error("IO error communicating with Ollama "+ e);
            return getErrorResponse("Connection error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error getting Ollama response", e);
            return getErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Returns a fallback response when Ollama is unavailable
     */
    private String getFallbackResponse() {
        return "<div class='alert alert-info'>" +
               "<strong>AI Analysis Unavailable</strong><br>" +
               "The AI analysis service is currently unavailable. " +
               "Please review the alarm details and interpretation guide above for manual analysis." +
               "</div>";
    }
    
    /**
     * Returns an error response
     */
    private String getErrorResponse(String error) {
        return "<div class='alert alert-warning'>" +
               "<strong>AI Analysis Error</strong><br>" +
               error + "<br>" +
               "Please review the alarm details and interpretation guide above for manual analysis." +
               "</div>";
    }
    
    /**
     * Tests connectivity to Ollama service
     * 
     * @return true if Ollama is reachable, false otherwise
     */
    public boolean testConnection() {
        try {
            String testPrompt = "Hello";
            generateResponse(testPrompt);
            return true;
        } catch (Exception e) {
            logger.warn("Ollama connection test failed", e);
            return false;
        }
    }
}
