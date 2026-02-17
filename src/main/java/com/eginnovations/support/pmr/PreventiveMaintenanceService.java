package com.eginnovations.support.pmr;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.eginnovations.support.pmr.model.HistoricalDataRoot;
import com.eginnovations.support.pmr.model.KPIComplianceResult;
import com.eginnovations.support.pmr.model.MeasureHelp;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service to process preventive maintenance reports from ZIP files
 */
@Service
public class PreventiveMaintenanceService {
    
    private static final Logger logger = LoggerFactory.getLogger(PreventiveMaintenanceService.class);
    private static final String AI_LOGS_DIR = "logs/ai-pm";
    
    @Autowired
    private OllamaService ollamaService;
    @Autowired
    private Environment environment;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    private Properties fileCategoryMapping;
    
    public PreventiveMaintenanceService() {
        loadFileCategoryMapping();
        initializeAILogsDirectory();
    }
    
    /**
     * Initialize the AI logs directory
     */
    private void initializeAILogsDirectory() {
        try {
            Path logsPath = Paths.get(AI_LOGS_DIR);
            if (!Files.exists(logsPath)) {
                Files.createDirectories(logsPath);
                logger.info("Created AI logs directory: {}", AI_LOGS_DIR);
            }
        } catch (IOException e) {
            logger.error("Error creating AI logs directory: " + AI_LOGS_DIR, e);
        }
    }
    
    /**
     * Load the file category mapping properties
     */
    private void loadFileCategoryMapping() {
        fileCategoryMapping = new Properties();
        try {
            Resource resource = new ClassPathResource("fileCategoryMapping.properties");
            if (resource.exists()) {
                fileCategoryMapping.load(resource.getInputStream());
                logger.info("Loaded {} file category mappings", fileCategoryMapping.size());
            } else {
                logger.warn("fileCategoryMapping.properties not found in classpath");
            }
        } catch (IOException e) {
            logger.error("Error loading fileCategoryMapping.properties", e);
        }
    }
    
    /**
     * Find all preventive maintenance ZIP files in the current directory
     */
    public List<File> findPreventiveMaintenanceZips(String directory) {
        List<File> zipFiles = new ArrayList<>();
        File dir = new File(directory);
        
        if (!dir.exists() || !dir.isDirectory()) {
            logger.warn("Directory does not exist: {}", directory);
            return zipFiles;
        }
        
        File[] files = dir.listFiles((d, name) -> 
            name.startsWith("eg_preventive_maintenance") && name.endsWith(".zip")
        );
        
        if (files != null) {
            for (File file : files) {
                zipFiles.add(file);
                logger.info("Found preventive maintenance ZIP: {}", file.getName());
            }
        }
        
        return zipFiles;
    }
    
    /**
     * Process a single ZIP file and analyze all relevant entries
     */
    public List<KPIComplianceResult> processZipFile(File zipFile) throws IOException {
        List<KPIComplianceResult> results = new ArrayList<>();
        
        logger.info("Processing ZIP file: {}", zipFile.getName());
        
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            
            int processedCount = 0;
            int skippedCount = 0;
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                
                // Skip directories
                if (entry.isDirectory()) {
                    continue;
                }
                
                String entryName = entry.getName();
                
                // Check if this entry should be processed based on fileCategoryMapping
                if (!shouldProcessEntry(entryName)) {
                    skippedCount++;
                    logger.info("Skipping entry (not in category mapping): {}", entryName);
                    continue;
                }
                
                logger.info("Processing entry: {}", entryName);
                
                try {
                    KPIComplianceResult result = processZipEntry(zip, entry);
                    if (result != null) {
                        results.add(result);
                        processedCount++;
                    }
                } catch (Exception e) {
                    logger.error("Error processing entry: " + entryName, e);
                    // Continue processing other entries
                }
            }
            
            logger.info("Processed {} entries, skipped {} entries from {}", 
                       processedCount, skippedCount, zipFile.getName());
        }
        
        return results;
    }
    
    /**
     * Check if a ZIP entry should be processed based on fileCategoryMapping
     */
    private boolean shouldProcessEntry(String entryName) {
        // Extract just the filename from the path
        String fileName = entryName;
        if (entryName.contains("/")) {
            fileName = entryName.substring(entryName.lastIndexOf("/") + 1);
        }
        
        // Check if this filename is in our category mapping
        for (Object key : fileCategoryMapping.keySet()) {
            String mappingKey = (String) key;
            if (fileName.endsWith(mappingKey)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Process a single ZIP entry (JSON file)
     */
    private KPIComplianceResult processZipEntry(ZipFile zip, ZipEntry entry) throws IOException {
        String entryName = entry.getName();
        
        // Read the JSON content
        String jsonContent;
        try (InputStream is = zip.getInputStream(entry)) {
            jsonContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        
        // Parse the historical data
        HistoricalDataRoot root = parseHistoricalData(jsonContent);
        if (root == null || root.getHistoricalData() == null) {
            logger.warn("Could not parse historical data from: {}", entryName);
            return null;
        }
        
        HistoricalDataRoot.HistoricalDataContent historicalData = root.getHistoricalData();
        HistoricalDataRoot.MetaData metaData = historicalData.getMetaData();
        
        if (metaData == null) {
            logger.warn("No metadata found in: {}", entryName);
            return null;
        }
        
        // Get measure help information
        MeasureHelp measureHelp = getMeasureHelp(metaData.getTest(), metaData.getMeasure());
        
        // Build result object
        KPIComplianceResult result = new KPIComplianceResult();
        result.setEntryName(entryName);
        result.setComponentName(metaData.getComponentName());
        result.setComponentType(metaData.getComponentType());
        result.setTest(metaData.getTest());
        result.setMeasure(metaData.getMeasure());
        result.setTimeline(metaData.getTimeline());
        
        if (measureHelp != null) {
            result.setDescription(measureHelp.getDescription());
            result.setInterpretation(measureHelp.getInterpretation());
            result.setMeasurementUnit(measureHelp.getMeasurementUnit());
        }
        
        // Store raw data
        result.setRawData(objectMapper.writeValueAsString(historicalData.getData()));
        
        // Store diagnosis data
        Object diagnosisData = historicalData.getDiagnosisData();
        if (diagnosisData != null) {
            result.setDiagnosisData(objectMapper.writeValueAsString(diagnosisData));
        }
        
        // Generate AI analysis
        String aiAnalysis = generateAIAnalysis(result, historicalData);
        result.setAiAnalysis(aiAnalysis);
        
        // Determine compliance status from AI analysis
        determineComplianceStatus(result, aiAnalysis);
        
        return result;
    }
    
    /**
     * Parse historical data from JSON content
     */
    private HistoricalDataRoot parseHistoricalData(String jsonContent) {
        try {
            return objectMapper.readValue(jsonContent, HistoricalDataRoot.class);
        } catch (IOException e) {
            logger.error("Error parsing historical data JSON", e);
            return null;
        }
    }
    
    /**
     * Get measure help information from eghelp JSON files
     */
    private MeasureHelp getMeasureHelp(String testName, String measureName) {
        if (testName == null || measureName == null) {
            return null;
        }
        
        try {
        	String tmpFileName = testName.replaceAll("/", "");
            String helpFileName = "eghelp/" + tmpFileName + ".json";
            Resource resource = new ClassPathResource(helpFileName);
            
            if (!resource.exists()) {
                logger.warn("Help file not found: {}", helpFileName);
                return null;
            }
            
            String helpJson = new String(
                resource.getInputStream().readAllBytes(), 
                StandardCharsets.UTF_8
            );
            
            List<MeasureHelp> helpList = objectMapper.readValue(
                helpJson, 
                new TypeReference<List<MeasureHelp>>() {}
            );
            
            // Find the matching measure
            for (MeasureHelp help : helpList) {
                if (measureName.equals(help.getMeasurement())) {
                    logger.debug("Found help for measure: {} in test: {}", measureName, testName);
                    return help;
                }
            }
            
            logger.warn("Measure '{}' not found in help file: {}", measureName, helpFileName);
            throw new MeasureHelpNotFoundException("Measure help not found for test: " + testName + ", measure: " + measureName);
            
        } catch (IOException e) {
            logger.error("Error reading help file for test: " + testName, e);
        }
        
        return null;
    }
    
    /**
     * Generate AI analysis using Ollama
     */
    private String generateAIAnalysis(KPIComplianceResult result, 
                                     HistoricalDataRoot.HistoricalDataContent historicalData) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert system administrator and certified eG Innovations Engineer analyzing eG Innovations deployment data.\n\n");
        
        prompt.append("KPI INFORMATION:\n");
        prompt.append("Component: ").append(result.getComponentName()).append("\n");
        prompt.append("Component Type: ").append(result.getComponentType()).append("\n");
        prompt.append("Test: ").append(result.getTest()).append("\n");
        prompt.append("Measure: ").append(result.getMeasure()).append("\n");
        prompt.append("Timeline: ").append(result.getTimeline()).append("\n\n");
        
        if (result.getDescription() != null && !result.getDescription().isEmpty()) {
            prompt.append("DESCRIPTION:\n");
            prompt.append(result.getDescription()).append("\n\n");
        }
        
        if (result.getInterpretation() != null && !result.getInterpretation().isEmpty()) {
            prompt.append("INTERPRETATION GUIDE:\n");
            prompt.append(result.getInterpretation()).append("\n\n");
        }
        
        if (result.getMeasurementUnit() != null && !result.getMeasurementUnit().isEmpty()) {
            prompt.append("Measurement Unit: ").append(result.getMeasurementUnit()).append("\n\n");
        }
        
        prompt.append("HISTORICAL DATA:\n");
        try {
            String dataJson = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(historicalData.getData());
            // Limit data size to avoid overwhelming the LLM
            int dataSizeLimit = Integer.parseInt(this.environment.getProperty("llm.data.size.limit"));
			if (dataSizeLimit!=-1 && dataJson.length() > dataSizeLimit) {
                dataJson = dataJson.substring(0, dataSizeLimit) + "\n... (truncated)";
            }
            prompt.append(dataJson).append("\n\n");
        } catch (Exception e) {
            prompt.append("(Error formatting data)\n\n");
        }
        
        if (result.getDiagnosisData() != null) {
            prompt.append("DIAGNOSIS DATA:\n");
            String diagnosisStr = result.getDiagnosisData();
            int ddSizeLimit = Integer.parseInt(this.environment.getProperty("llm.dd.size.limit"));
            if (ddSizeLimit!=-1 && diagnosisStr.length() > ddSizeLimit) {
                diagnosisStr = diagnosisStr.substring(0, ddSizeLimit) + "\n... (truncated)";
            }
            prompt.append(diagnosisStr).append("\n\n");
        }
        
        prompt.append("ANALYSIS REQUIRED:\n");
        prompt.append("Based on the description, interpretation guide, and historical data provided above, ");
        prompt.append("analyze whether this KPI is COMPLIANT (healthy) or NON-COMPLIANT (needs attention).\n\n");
        prompt.append("Please provide:\n");
        prompt.append("1. A clear status: COMPLIANT or NON-COMPLIANT\n");
        prompt.append("2. A detailed explanation of why you reached this conclusion\n");
        prompt.append("3. Any specific concerns or recommendations if non-compliant\n");
        prompt.append("4. Key data points that support your analysis\n");
        prompt.append("5. Work within the data provided\n");
        prompt.append("6. Clearly decide COMPLIANT or NON-COMPLIANT or NEEDS REVIEW based on the data provided.\n");
        prompt.append("7. If historic values are given as - be informed that measurements are not applicable, mark the metrics as NEEDS REVIEW.\n");
        prompt.append("8. You are NOT allowed to infer that the KPI or sensor is unhealthy or faulty based solely on lack of variation. You MUST follow the interpretation guide above.\n");
        prompt.append("9. If your general knowledge or intuition conflicts with the interpretation guide, you MUST follow the interpretation guide.\n");
        prompt.append("10. 0 values for any event log errors, warnings are considered as COMPLIANT\n");
        prompt.append("11. non zero values for any event log errors, warnings are considered as NON-COMPLIANT\n");
        prompt.append("12. 0 values or near-zero values for any queue is considered as COMPLIANT\n");
        prompt.append("\n\n");
        
        prompt.append("Format your response as:\n");
        prompt.append("STATUS: [COMPLIANT or NON-COMPLIANT or NEEDS REVIEW]\n\n");
        prompt.append("REASON: [Your detailed analysis and justification for your verdict]\n");
        
        String promptText = prompt.toString();
        String response = null;
        
        try {
            // Save prompt to log file
            savePromptToLog(result.getEntryName(), promptText);
            
            // Get response from Ollama
            response = ollamaService.generateResponse(promptText);
            
            // Save response to log file
            saveResponseToLog(result.getEntryName(), response);
            
            return response;
        } catch (IOException e) {
            logger.error("Error generating AI analysis for: " + result.getEntryName(), e);
            return "AI analysis unavailable due to error: " + e.getMessage();
        }
    }
    
    /**
     * Save AI prompt to log file
     */
    private void savePromptToLog(String entryName, String prompt) {
        try {
            String logFileName = getLogFileName(entryName, ".prompt.log");
            Path logPath = Paths.get(AI_LOGS_DIR, logFileName);
            
            try (FileWriter writer = new FileWriter(logPath.toFile())) {
                writer.write("=".repeat(80) + "\n");
                writer.write("AI PROMPT LOG\n");
                writer.write("Entry: " + entryName + "\n");
                writer.write("Timestamp: " + java.time.LocalDateTime.now() + "\n");
                writer.write("=".repeat(80) + "\n\n");
                writer.write(prompt);
            }
            
            logger.debug("Saved prompt to: {}", logPath);
        } catch (IOException e) {
            logger.error("Error saving prompt to log for: " + entryName, e);
        }
    }
    
    /**
     * Save AI response to log file
     */
    private void saveResponseToLog(String entryName, String response) {
        try {
            String logFileName = getLogFileName(entryName, ".response.log");
            Path logPath = Paths.get(AI_LOGS_DIR, logFileName);
            
            try (FileWriter writer = new FileWriter(logPath.toFile())) {
                writer.write("=".repeat(80) + "\n");
                writer.write("AI RESPONSE LOG\n");
                writer.write("Entry: " + entryName + "\n");
                writer.write("Timestamp: " + java.time.LocalDateTime.now() + "\n");
                writer.write("=".repeat(80) + "\n\n");
                writer.write(response);
            }
            
            logger.debug("Saved response to: {}", logPath);
        } catch (IOException e) {
            logger.error("Error saving response to log for: " + entryName, e);
        }
    }
    
    /**
     * Generate log file name from ZIP entry name
     */
    private String getLogFileName(String entryName, String suffix) {
        // Extract filename from path and remove .json extension
        String fileName = entryName;
        if (entryName.contains("/")) {
            fileName = entryName.substring(entryName.lastIndexOf("/") + 1);
        }
        if (fileName.endsWith(".json")) {
            fileName = fileName.substring(0, fileName.length() - 5);
        }
        
        // Sanitize filename (replace invalid characters)
        fileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        return fileName + suffix;
    }
    
    /**
     * Determine compliance status from AI analysis
     */
    private void determineComplianceStatus(KPIComplianceResult result, String aiAnalysis) {
        if (aiAnalysis == null) {
            result.setComplianceStatus("UNKNOWN");
            result.setCompliant(false);
            return;
        }
        
        String upperAnalysis = aiAnalysis.toUpperCase();
        
        // Look for explicit status in the response
        if (upperAnalysis.contains("STATUS: NON-COMPLIANT") || 
            upperAnalysis.contains("STATUS:NON-COMPLIANT")) {
            result.setComplianceStatus("NON-COMPLIANT");
            result.setCompliant(false);
        } else if (upperAnalysis.contains("STATUS: COMPLIANT") || 
                   upperAnalysis.contains("STATUS:COMPLIANT")) {
            result.setComplianceStatus("COMPLIANT");
            result.setCompliant(true);
        } else if (upperAnalysis.contains("NEEDS ATTENTION") || 
                   upperAnalysis.contains("NON-COMPLIANT") ||
                   upperAnalysis.contains("NOT COMPLIANT")) {
            result.setComplianceStatus("NON-COMPLIANT");
            result.setCompliant(false);
        } else if (upperAnalysis.contains("COMPLIANT") || 
                   upperAnalysis.contains("HEALTHY") ||
                   upperAnalysis.contains("NORMAL")) {
            result.setComplianceStatus("COMPLIANT");
            result.setCompliant(true);
        } else {
            result.setComplianceStatus("NEEDS REVIEW");
            result.setCompliant(false);
        }
        
        logger.info("Compliance determined for {}: {}", 
                   result.getCheckName(), result.getComplianceStatus());
    }
}
