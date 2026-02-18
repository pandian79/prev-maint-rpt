package com.eginnovations.support.pmr;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.eginnovations.support.pmr.model.AlarmReportData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Service
public class AlarmAnalysisReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlarmAnalysisReportService.class);
    
    // Priority order for sorting
    private static final Map<String, Integer> PRIORITY_ORDER = new HashMap<>();
    static {
        PRIORITY_ORDER.put("critical", 1);
        PRIORITY_ORDER.put("major", 2);
        PRIORITY_ORDER.put("minor", 3);
        PRIORITY_ORDER.put("warning", 4);
    }
    
    @Autowired
    private Environment env;
    
    @Autowired
    private OllamaService ollamaService;
    
    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private ObjectMapper objectMapper;
    
    // Markdown parser and renderer
    private Parser markdownParser = Parser.builder().build();
    private HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();
    
    /**
     * Generates HTML reports for all alarm_analysis_*.zip files in the current directory
     */
    public void generateHtmlReports() {
        logger.info("Starting HTML report generation for alarm analysis files...");
        
        // Step 0: Find all alarm_analysis_*.zip files
        File currentDir = new File(".");
        File[] zipFiles = currentDir.listFiles((dir, name) -> 
            name.startsWith("alarm_analysis_") && name.endsWith(".zip")
        );
        
        if (zipFiles == null || zipFiles.length == 0) {
            logger.warn("No alarm_analysis_*.zip files found in current directory");
            System.out.println("No alarm analysis zip files found to process.");
            return;
        }
        
        logger.info("Found {} alarm analysis zip files to process", zipFiles.length);
        System.out.println("Found " + zipFiles.length + " alarm analysis zip file(s) to process.");
        
        for (File zipFile : zipFiles) {
            try {
                generateHtmlReport(zipFile);
            } catch (Exception e) {
                logger.error("Failed to generate HTML report for {}", zipFile.getName(), e);
                System.err.println("Error processing " + zipFile.getName() + ": " + e.getMessage());
            }
        }
        
        logger.info("HTML report generation completed");
        System.out.println("HTML report generation completed!");
    }
    
    /**
     * Generates an HTML report for a single alarm analysis zip file
     */
    private void generateHtmlReport(File zipFile) throws IOException {
        logger.info("Processing zip file: {}", zipFile.getName());
        System.out.println("Processing: " + zipFile.getName());
        
        // Step 0: Create HTML file name based on zip file name
        String htmlFileName = zipFile.getName().replace(".zip", ".html");
        Path htmlPath = Paths.get(htmlFileName);
        
        // Prepare data for Thymeleaf template
        List<AlarmReportData> alarms = new ArrayList<>();
        
        // Step 1: Read alarm_analysis zip file
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            int alarmCount = 0;
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".json")) {
                    continue;
                }
                
                alarmCount++;
                logger.info("Processing alarm JSON file: {}", entry.getName());
                System.out.println("  Processing alarm " + alarmCount + "/" + zip.size() + ": " + entry.getName());
                
                try {
                    // Read the JSON content
                    String jsonContent = readZipEntry(zip, entry);
                    JsonNode alarmData = objectMapper.readTree(jsonContent);
                    
                    // Step 2: Extract representativeAlert
                    JsonNode representativeAlert = alarmData.get("representativeAlert");
                    if (representativeAlert == null) {
                        logger.warn("No representativeAlert found in {}", entry.getName());
                        continue;
                    }
                    
                    String test = representativeAlert.get("test").asText();
                    String measure = representativeAlert.get("measure").asText();
                    
                    // Create AlarmReportData object
                    AlarmReportData alarmReport = new AlarmReportData();
                    
                    // Populate basic fields
                    alarmReport.setComponentName(representativeAlert.get("componentName").asText());
                    alarmReport.setComponentType(representativeAlert.get("componentType").asText());
                    alarmReport.setTest(test);
                    alarmReport.setMeasure(measure);
                    alarmReport.setPriority(representativeAlert.get("priority").asText());
                    alarmReport.setLayer(representativeAlert.get("layer").asText());
                    alarmReport.setDescription(representativeAlert.get("description").asText());
                    alarmReport.setStartTime(representativeAlert.get("startTime").asText());
                    alarmReport.setDuration(representativeAlert.get("duration").asText());
                    
                    if (representativeAlert.has("info")) {
                        alarmReport.setInfo(representativeAlert.get("info").asText());
                    }
                    
                    if (representativeAlert.has("repeatCount")) {
                        alarmReport.setRepeatCount(representativeAlert.get("repeatCount").asInt());
                    } else {
                        alarmReport.setRepeatCount(1);
                    }
                    
                    // Step 3: Get interpretation from eghelp resources
                    populateInterpretation(alarmReport, test, measure);
                    
                    // Add metric graph if available
                    if (alarmData.has("measureGraphBase64") && !alarmData.get("measureGraphBase64").isNull()) {
                        alarmReport.setMeasureGraphBase64(alarmData.get("measureGraphBase64").asText());
                    }
                    
                    // Step 4: Get Ollama interpretation with logging
                    String entryName = entry.getName();
                    String ollamaInterpretation = getOllamaInterpretationWithLogging(
                        representativeAlert, alarmData, alarmReport.getInterpretation(), entryName
                    );
                    alarmReport.setAiAnalysis(ollamaInterpretation);
                    
                    // Add to list
                    alarms.add(alarmReport);
                    
                } catch (Exception e) {
                    logger.error("Error processing alarm file {}", entry.getName(), e);
                    // Create error alarm card
                    AlarmReportData errorAlarm = createErrorAlarm(entry.getName(), e.getMessage());
                    alarms.add(errorAlarm);
                }
            }
            
            if (alarmCount == 0) {
                logger.warn("No alarm JSON files found in {}", zipFile.getName());
            }
        }
        
        // Sort alarms by priority (Critical -> Major -> Minor)
        alarms.sort(new Comparator<AlarmReportData>() {
            @Override
            public int compare(AlarmReportData a1, AlarmReportData a2) {
                String p1 = a1.getPriority() != null ? a1.getPriority().toLowerCase() : "unknown";
                String p2 = a2.getPriority() != null ? a2.getPriority().toLowerCase() : "unknown";
                
                int order1 = PRIORITY_ORDER.getOrDefault(p1, 999);
                int order2 = PRIORITY_ORDER.getOrDefault(p2, 999);
                
                return Integer.compare(order1, order2);
            }
        });
        
        logger.info("Sorted {} alarms by priority (Critical -> Major -> Minor)", alarms.size());
        
        // Prepare Thymeleaf context
        Context context = new Context();
        context.setVariable("zipFileName", zipFile.getName());
        context.setVariable("reportDate", LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")
        ));
        context.setVariable("currentYear", LocalDateTime.now().getYear());
        context.setVariable("alarms", alarms);
        
        // Process template and write to file
        String htmlContent = templateEngine.process("alarm-report", context);
        
        try (FileWriter writer = new FileWriter(htmlPath.toFile(), StandardCharsets.UTF_8)) {
            writer.write(htmlContent);
        }
        
        logger.info("HTML report generated: {}", htmlFileName);
        System.out.println("  Report saved: " + htmlFileName);
    }
    
    /**
     * Reads a zip entry as a string
     */
    private String readZipEntry(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream is = zip.getInputStream(entry)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    
    /**
     * Populates interpretation data from eghelp resources based on test and measure
     */
    private void populateInterpretation(AlarmReportData alarmReport, String test, String measure) {
        try {
        	String helpFileName=test.replaceAll("/", "");
            String path = "eghelp/" + helpFileName + ".json";
            logger.info("Looking for help file at path: {}", path);
			ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                logger.warn("No help file found for test: {}", helpFileName);
                throw new MeasureHelpNotFoundException("No help file found for test: " + helpFileName);
                //alarmReport.setInterpretation("No interpretation data available for this test.");
//                return;
            }
            
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode measurements = objectMapper.readTree(content);
            
            for (JsonNode measurement : measurements) {
            	logger.debug("Checking measurement: {}", measurement.toString());
                JsonNode jsonNode = extractedJsonNode(measurement, "Measurement");
                if (jsonNode == null) {
					logger.warn("No 'Measurement' field found in help file for test '{}'", test);
					throw new MeasureHelpNotFoundException("No 'Measurement' field found in help file for test: " + test);
				}
				if (jsonNode.asText().equalsIgnoreCase(measure)) {
                    alarmReport.setInterpretation("found");
                    alarmReport.setInterpretationDescription(extractedJsonNode(measurement, "Description").asText());
                    JsonNode unitNode;
					try {
						unitNode = extractedJsonNode(measurement, "MeasurementUnit");
					} catch (MeasureHelpNotFoundException e) {
						try {
							unitNode = extractedJsonNode(measurement, "unit");
						} catch (MeasureHelpNotFoundException e1) {
							unitNode = extractedJsonNode(measurement, "measurementUnit");
						}
					}
                    
					alarmReport.setInterpretationUnit(unitNode.asText());
                    alarmReport.setInterpretationText(extractedJsonNode(measurement, "Interpretation").asText());
                    return;
                }
            }
            
            logger.warn("Measure '{}' not found in help file for test '{}'", measure, test);
            alarmReport.setInterpretation("No specific interpretation found for this measure.");
            
        } catch (Exception e) {
            logger.error("Error reading interpretation for test: {}, measure: {}", test, measure, e);
            alarmReport.setInterpretation("Error reading interpretation data: " + e.getMessage());
        }
    }

	private JsonNode extractedJsonNode(JsonNode measurement, String key) {
		JsonNode jsonNode = measurement.get(key);
		if (jsonNode == null) {
			jsonNode = measurement.get(key.toLowerCase());
		}
		if (jsonNode == null) {
			jsonNode = measurement.get(key.toUpperCase());
		}
		if (jsonNode == null && !key.equalsIgnoreCase("troubleshootingSteps")) {
			throw new MeasureHelpNotFoundException("No '" + key + "' field found in measurement: " + measurement.toString());
		}
		return jsonNode;
	}
    
    /**
     * Gets interpretation HTML string from resources (for Ollama prompt)
     */
    private String getInterpretationStringFromResources(String test, String measure) {
        try {
            ClassPathResource resource = new ClassPathResource("eghelp/" + test + ".json");
            if (!resource.exists()) {
                return "No interpretation data available for this test.";
            }
            
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode measurements = objectMapper.readTree(content);
            
            for (JsonNode measurement : measurements) {
            	JsonNode extractedJsonNode = this.extractedJsonNode(measurement, "Measurement");
                if (extractedJsonNode.asText().equals(measure)) {
                    StringBuilder interpretation = new StringBuilder();
                    JsonNode description = this.extractedJsonNode(measurement, "Description");
                    JsonNode interpretationJsonNode = this.extractedJsonNode(measurement, "Interpretation");
                    JsonNode troubleshootingStepsJsonNode = this.extractedJsonNode(measurement, "troubleshootingSteps");
					JsonNode measurementUnit;
					try {
						measurementUnit = this.extractedJsonNode(measurement, "MeasurementUnit");
					} catch (Exception e) {
						try {
							measurementUnit = this.extractedJsonNode(measurement, "measurementUnit");
						} catch (Exception e1) {
							measurementUnit = this.extractedJsonNode(measurement, "Unit");
						}
					}
					
					String prompt = "Not enough information available.";
					
					if (troubleshootingStepsJsonNode!=null) {
						ArrayNode stepsArray = (ArrayNode) troubleshootingStepsJsonNode;
						prompt = StreamSupport.stream(stepsArray.spliterator(), false)
						        .map(JsonNode::asText)
						        .collect(Collectors.joining(".\r\n\t* "));  // or any separator you like
					}
					
					interpretation.append("Description: ")
                        .append(description.asText())
                        .append(". \r\nUnit: ")
                        .append(measurementUnit.asText())
                        .append(". \r\nInterpretation: ")
                        .append(interpretationJsonNode.asText())
                        .append(". \r\nTroubleshooting Steps: \r\n\t* ")
                        .append(prompt);
                    return interpretation.toString();
                }
            }
            
            return "No specific interpretation found for this measure.";
            
        } catch (Exception e) {
            logger.error("Error reading interpretation for test: {}, measure: {}", test, measure, e);
            return "Error reading interpretation data: " + e.getMessage();
        }
    }
    
    /**
     * Creates an error alarm report entry
     */
    private AlarmReportData createErrorAlarm(String fileName, String errorMessage) {
        AlarmReportData errorAlarm = new AlarmReportData();
        errorAlarm.setComponentName("Error Processing: " + fileName);
        errorAlarm.setComponentType("Error");
        errorAlarm.setTest("N/A");
        errorAlarm.setMeasure("N/A");
        errorAlarm.setPriority("Critical");
        errorAlarm.setLayer("N/A");
        errorAlarm.setDescription("Failed to process alarm file");
        errorAlarm.setStartTime("N/A");
        errorAlarm.setDuration("N/A");
        errorAlarm.setRepeatCount(1);
        errorAlarm.setInterpretation(null);
        errorAlarm.setAiAnalysis("<div class='alert alert-danger'>" + escapeHtml(errorMessage) + "</div>");
        return errorAlarm;
    }
    
    /**
     * Gets interpretation from Ollama service with logging
     */
    private String getOllamaInterpretationWithLogging(JsonNode representativeAlert, 
                                          JsonNode alarmData, 
                                          String interpretation,
                                          String entryName) {
        try {
            // Create logs/ai directory if it doesn't exist
            Path logsDir = Paths.get("logs", "ai");
            Files.createDirectories(logsDir);
            
            // Generate log file names based on entry name
            String baseFileName = entryName.replace(".json", "").replaceAll("[^a-zA-Z0-9._-]", "_");
            Path promptLogPath = logsDir.resolve(baseFileName + ".prompt.log");
            Path responseLogPath = logsDir.resolve(baseFileName + ".response.log");
            
            // Form the prompt
            StringBuilder prompt = new StringBuilder();
            prompt.append("You are an expert system performance analyst. Analyze the following alarm data comprehensively:\n\n");
            prompt.append("You are a text-only assistant. Respond only in GitHub-flavored Markdown. Do not use any HTML tags like <p>, <strong>, <ul>, <li>, or <br>.\n");
            
            prompt.append("=== ALARM DETAILS ===\n");
            prompt.append("Component: ").append(representativeAlert.get("componentName").asText()).append("\n");
            prompt.append("Component Type: ").append(representativeAlert.get("componentType").asText()).append("\n");
            prompt.append("Test: ").append(representativeAlert.get("test").asText()).append("\n");
            prompt.append("Measure: ").append(representativeAlert.get("measure").asText()).append("\n");
            prompt.append("Priority: ").append(representativeAlert.get("priority").asText()).append("\n");
            prompt.append("Description: ").append(representativeAlert.get("description").asText()).append("\n");
            prompt.append("Start Time: ").append(representativeAlert.get("startTime").asText()).append("\n");
            prompt.append("Duration: ").append(representativeAlert.get("duration").asText()).append("\n");
            
            if (representativeAlert.has("repeatCount")) {
                prompt.append("Repeat Count: ").append(representativeAlert.get("repeatCount").asInt()).append(" times\n");
            }
            
            prompt.append("\n=== INTERPRETATION GUIDE ===\n");
            String interpretationForPrompt = getInterpretationStringFromResources(
                representativeAlert.get("test").asText(),
                representativeAlert.get("measure").asText()
            );
            prompt.append(interpretationForPrompt).append("\n");
            
            // Add historical data if available
            if (alarmData.has("historicalData") && !alarmData.get("historicalData").isNull()) {
                JsonNode historicalData = alarmData.get("historicalData");
                prompt.append("\n=== HISTORICAL DATA ===\n");
                prompt.append("The following historical trend data is available for this measure:\n");
                
                if (historicalData.isArray() && historicalData.size() > 0) {
                    prompt.append("Data Points: ").append(historicalData.size()).append("\n");
                    // Add sample of historical data
                    int sampleSize = Math.min(5, historicalData.size());
                    prompt.append("Sample of recent data points:\n");
                    for (int i = 0; i < sampleSize; i++) {
                        JsonNode dataPoint = historicalData.get(i);
                        prompt.append("  - ").append(dataPoint.toString()).append("\n");
                    }
                    if (historicalData.size() > 5) {
                        prompt.append("  ... and ").append(historicalData.size() - 5).append(" more data points\n");
                    }
                } else if (historicalData.isObject()) {
                    prompt.append(historicalData.toString()).append("\n");
                } else {
                    prompt.append("Historical data available but in unexpected format.\n");
                }
                prompt.append("\nIMPORTANT: Analyze the historical trend to identify patterns, anomalies, or changes over time.\n");
            }
            
            // Add diagnosis data if available
            if (alarmData.has("diagnosisData") && !alarmData.get("diagnosisData").isNull()) {
                JsonNode diagnosisData = alarmData.get("diagnosisData");
                prompt.append("\n=== DIAGNOSIS DATA ===\n");
                prompt.append("Detailed diagnostic information:\n");
                
                if (diagnosisData.isArray() && diagnosisData.size() > 0) {
                    for (int i = 0; i < diagnosisData.size(); i++) {
                        JsonNode diagnosis = diagnosisData.get(i);
                        prompt.append("Diagnosis ").append(i + 1).append(":\n");
                        prompt.append(diagnosis.toPrettyString()).append("\n");
                    }
                } else if (diagnosisData.isObject()) {
                    prompt.append(diagnosisData.toPrettyString()).append("\n");
                } else {
                    prompt.append(diagnosisData.toString()).append("\n");
                }
                prompt.append("\nIMPORTANT: Use the diagnosis data to identify specific issues and root causes.\n");
            }
            
            prompt.append("\n=== REQUIRED ANALYSIS ===\n");
            prompt.append("Based on ALL the data provided above (alarm details, interpretation guide, historical data, and diagnosis data), provide:\n\n");
            prompt.append("1. **Alert Interpretation**: What does this alarm indicate? Consider the historical trends.\n");
            prompt.append("2. **Root Cause Analysis**: What are the likely root causes? Use the diagnosis data if available.\n");
            prompt.append("3. **Impact Assessment**: What is the potential impact on the system?\n");
            prompt.append("4. **Remediation Steps**: What specific steps should be taken to fix the problem?\n");
            prompt.append("\nIMPORTANT: \n");
            prompt.append("- Respond in GitHub-style Markdown, no HTML tags.\n");
            prompt.append("- Provide response in English language alone.\n");
            prompt.append("- Reference the historical data in your analysis if provided\n");
            prompt.append("- Reference the diagnosis data in your analysis if provided\n");
            prompt.append("- Keep your analysis concise but comprehensive\n");
            
            // Write prompt to log file
            Files.writeString(promptLogPath, prompt.toString(), StandardCharsets.UTF_8);
            logger.info("Prompt written to: {}", promptLogPath);
            
            // Get response from Ollama
            String markdownResponse = ollamaService.generateResponse(prompt.toString());
            
            // Write response to log file
            Files.writeString(responseLogPath, markdownResponse, StandardCharsets.UTF_8);
            logger.info("Response written to: {}", responseLogPath);
            
            // Convert markdown to HTML
            String htmlResponse = convertMarkdownToHtml(markdownResponse);
            
            return htmlResponse;
            
        } catch (Exception e) {
            logger.error("Error getting Ollama interpretation", e);
            return "<div class='alert alert-warning'><strong>AI analysis temporarily unavailable:</strong> " + 
                   escapeHtml(e.getMessage()) + "</div>";
        }
    }
    
    /**
     * Converts Markdown text to HTML
     */
    private String convertMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "<p>No analysis available.</p>";
        }
        
        try {
            // Check if response already contains HTML tags
            if (markdown.contains("<div") || markdown.contains("<p>") || markdown.contains("<ul>")) {
                // Response already has HTML, clean up any mixed markdown
                // Parse as markdown anyway to get consistent output
                Node document = markdownParser.parse(markdown);
                return htmlRenderer.render(document);
            }
            
            // Parse markdown and render as HTML
            Node document = markdownParser.parse(markdown);
            return htmlRenderer.render(document);
            
        } catch (Exception e) {
            logger.error("Error converting markdown to HTML", e);
            // Fallback: return as-is wrapped in pre tag
            return "<pre>" + escapeHtml(markdown) + "</pre>";
        }
    }
    
    /**
     * Escapes HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
