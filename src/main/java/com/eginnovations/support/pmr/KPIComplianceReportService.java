package com.eginnovations.support.pmr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.eginnovations.support.pmr.model.KPIComplianceResult;
import com.eginnovations.support.pmr.model.KPIReportModel;
import com.eginnovations.support.pmr.model.KPIReportModel.KPIResultViewModel;

/**
 * Service to generate HTML reports for KPI compliance analysis
 */
@Service
public class KPIComplianceReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(KPIComplianceReportService.class);
    
    @Autowired
    private TemplateEngine templateEngine;
    
    // CommonMark parser and renderer for markdown to HTML conversion
    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();
    
    /**
     * Generate HTML report for KPI compliance results using Thymeleaf template
     */
    public String generateReport(List<KPIComplianceResult> results, String zipFileName) throws IOException {
        logger.info("Generating KPI compliance report using Thymeleaf template");
        
        // Build the model for the template
        KPIReportModel model = buildReportModel(results, zipFileName);
        
        // Create Thymeleaf context
        Context context = new Context();
        context.setVariable("zipFileName", model.getZipFileName());
        context.setVariable("timestamp", model.getTimestamp());
        context.setVariable("totalChecks", model.getTotalChecks());
        context.setVariable("compliantCount", model.getCompliantCount());
        context.setVariable("nonCompliantCount", model.getNonCompliantCount());
        context.setVariable("needsReviewCount", model.getNeedsReviewCount());
        context.setVariable("results", model.getResults());
        
        // Process the template
        String html = templateEngine.process("kpi-compliance-report", context);
        
        logger.info("Report generated successfully with {} KPIs", results.size());
        return html;
    }
    
    /**
     * Build report model from KPI compliance results
     */
    private KPIReportModel buildReportModel(List<KPIComplianceResult> results, String zipFileName) {
        KPIReportModel model = new KPIReportModel();
        
        // Set basic information
        model.setZipFileName(zipFileName);
        model.setTimestamp(getCurrentTimestamp());
        
        // Calculate statistics
        model.setTotalChecks(results.size());
        model.setCompliantCount(results.stream()
            .filter(r -> "COMPLIANT".equals(r.getComplianceStatus()))
            .count());
        model.setNonCompliantCount(results.stream()
            .filter(r -> "NON-COMPLIANT".equals(r.getComplianceStatus()))
            .count());
        model.setNeedsReviewCount(results.stream()
            .filter(r -> "NEEDS REVIEW".equals(r.getComplianceStatus()))
            .count());
        
        // Convert results to view models
        List<KPIResultViewModel> viewModels = new ArrayList<>();
        for (KPIComplianceResult result : results) {
            viewModels.add(convertToViewModel(result));
        }
        model.setResults(viewModels);
        
        return model;
    }
    
    /**
     * Convert KPIComplianceResult to view model
     */
    private KPIResultViewModel convertToViewModel(KPIComplianceResult result) {
        KPIResultViewModel viewModel = new KPIResultViewModel();
        
        // Generate KPI ID for anchor links
        String kpiId = generateKPIId(result);
        viewModel.setKpiId(kpiId);
        
        // Set status classes
        String statusClass = "status-needs-review";
        String badgeClass = "badge-needs-review";
        String aiAnalysisClass = "ai-analysis-needs-review";
        if ("COMPLIANT".equals(result.getComplianceStatus())) {
            statusClass = "status-compliant";
            badgeClass = "badge-compliant";
            aiAnalysisClass = "ai-analysis-compliant";
        } else if ("NON-COMPLIANT".equals(result.getComplianceStatus())) {
            statusClass = "status-non-compliant";
            badgeClass = "badge-non-compliant";
            aiAnalysisClass = "ai-analysis-non-compliant";
        }
        viewModel.setStatusClass(statusClass);
        viewModel.setBadgeClass(badgeClass);
        viewModel.setAiAnalysisClass(aiAnalysisClass);
        
        // Set basic fields
        viewModel.setCheckName(result.getCheckName());
        viewModel.setComplianceStatus(result.getComplianceStatus());
        viewModel.setComponentName(result.getComponentName());
        viewModel.setComponentType(result.getComponentType());
        viewModel.setTest(result.getTest());
        viewModel.setMeasure(result.getMeasure());
        viewModel.setTimeline(result.getTimeline());
        viewModel.setMeasurementUnit(result.getMeasurementUnit());
        viewModel.setDescription(result.getDescription());
        viewModel.setInterpretation(result.getInterpretation());
        
        // Convert AI analysis markdown to HTML
        viewModel.setAiAnalysis(result.getAiAnalysis());
        if (result.getAiAnalysis() != null && !result.getAiAnalysis().isEmpty()) {
            String aiAnalysisHtml = formatAIAnalysis(result.getAiAnalysis());
            viewModel.setAiAnalysisHtml(aiAnalysisHtml);
        }
        
        return viewModel;
    }
    
    /**
     * Generate unique ID for a KPI card
     */
    private String generateKPIId(KPIComplianceResult result) {
        // Create a unique ID from component, test, and measure
        String id = result.getComponentName() + "_" + result.getTest() + "_" + result.getMeasure();
        // Sanitize for use as HTML ID (remove spaces and special characters)
        id = id.replaceAll("[^a-zA-Z0-9_-]", "_");
        return id;
    }
    
    /**
     * Format AI analysis text with proper HTML formatting using CommonMark
     */
    private String formatAIAnalysis(String analysis) {
        if (analysis == null || analysis.isEmpty()) {
            return "";
        }
        
        // Parse markdown and convert to HTML
        org.commonmark.node.Node document = markdownParser.parse(analysis);
        String html = htmlRenderer.render(document);
        
        return html;
    }
    
    /**
     * Get current timestamp
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }
    
    /**
     * Save report to file
     */
    public File saveReport(String htmlContent, String zipFileName) throws IOException {
        // Create output filename
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        String timestamp = sdf.format(new Date());
        String outputFileName = "kpi_compliance_report_" + timestamp + ".html";
        
        Path outputPath = Paths.get(outputFileName);
        Files.writeString(outputPath, htmlContent);
        
        logger.info("Report saved to: {}", outputPath.toAbsolutePath());
        
        return outputPath.toFile();
    }
}
