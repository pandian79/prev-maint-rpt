package com.eginnovations.support.pmr;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.eginnovations.support.pmr.model.KPIComplianceResult;

/**
 * Test class for KPI Compliance Analysis
 * 
 * This is an example test to demonstrate the functionality.
 * Requires:
 * 1. Ollama running locally
 * 2. eg_preventive_maintenance_*.zip file in project root
 * 3. fileCategoryMapping.properties configured
 */
@SpringBootTest
public class KPIComplianceTest {
    
    @Autowired
    private PreventiveMaintenanceService preventiveMaintenanceService;
    
    @Autowired
    private KPIComplianceReportService reportService;
    
    /**
     * Test finding ZIP files
     * This test will pass even if no ZIP files are found
     */
    @Test
    public void testFindZipFiles() {
        String currentDir = System.getProperty("user.dir");
        List<File> zipFiles = preventiveMaintenanceService.findPreventiveMaintenanceZips(currentDir);
        
        System.out.println("Found " + zipFiles.size() + " preventive maintenance ZIP file(s)");
        for (File file : zipFiles) {
            System.out.println("  - " + file.getName());
        }
    }
    
    /**
     * Test processing a single ZIP file
     * Uncomment and run only if you have a ZIP file and Ollama running
     */
    // @Test
    public void testProcessZipFile() throws Exception {
        String currentDir = System.getProperty("user.dir");
        List<File> zipFiles = preventiveMaintenanceService.findPreventiveMaintenanceZips(currentDir);
        
        if (zipFiles.isEmpty()) {
            System.out.println("No ZIP files found. Skipping test.");
            return;
        }
        
        File zipFile = zipFiles.get(0);
        System.out.println("Processing: " + zipFile.getName());
        
        List<KPIComplianceResult> results = preventiveMaintenanceService.processZipFile(zipFile);
        
        System.out.println("Processed " + results.size() + " KPI(s)");
        
        for (KPIComplianceResult result : results) {
            System.out.println("\n=== " + result.getCheckName() + " ===");
            System.out.println("Status: " + result.getComplianceStatus());
            System.out.println("Component: " + result.getComponentName());
            System.out.println("Test: " + result.getTest());
            System.out.println("Measure: " + result.getMeasure());
            if (result.getAiAnalysis() != null) {
                String analysis = result.getAiAnalysis();
                if (analysis.length() > 200) {
                    analysis = analysis.substring(0, 200) + "...";
                }
                System.out.println("AI Analysis Preview: " + analysis);
            }
        }
    }
    
    /**
     * Test full workflow: process ZIP and generate report
     * Uncomment and run only if you have a ZIP file and Ollama running
     */
    // @Test
    public void testFullWorkflow() throws Exception {
        String currentDir = System.getProperty("user.dir");
        List<File> zipFiles = preventiveMaintenanceService.findPreventiveMaintenanceZips(currentDir);
        
        if (zipFiles.isEmpty()) {
            System.out.println("No ZIP files found. Skipping test.");
            return;
        }
        
        File zipFile = zipFiles.get(0);
        System.out.println("Processing: " + zipFile.getName());
        
        // Process ZIP file
        List<KPIComplianceResult> results = preventiveMaintenanceService.processZipFile(zipFile);
        System.out.println("Analyzed " + results.size() + " KPI(s)");
        
        if (!results.isEmpty()) {
            // Generate report
            String htmlReport = reportService.generateReport(results, zipFile.getName());
            File reportFile = reportService.saveReport(htmlReport, zipFile.getName());
            
            System.out.println("Report generated: " + reportFile.getAbsolutePath());
            System.out.println("Report size: " + htmlReport.length() + " characters");
            
            // Print summary
            long compliant = results.stream()
                .filter(r -> "COMPLIANT".equals(r.getComplianceStatus()))
                .count();
            long nonCompliant = results.stream()
                .filter(r -> "NON-COMPLIANT".equals(r.getComplianceStatus()))
                .count();
            
            System.out.println("\nSummary:");
            System.out.println("  Total: " + results.size());
            System.out.println("  Compliant: " + compliant);
            System.out.println("  Non-Compliant: " + nonCompliant);
        }
    }
}
