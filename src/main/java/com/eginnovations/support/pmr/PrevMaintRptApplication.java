package com.eginnovations.support.pmr;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import com.eg.api.client.EgRequestHeader;
import com.eg.api.client.dao.AlarmsRepository;
import com.eg.api.client.dao.TestRepository;
import com.eg.api.client.entity.ManagedComponent;
import com.eginnovations.support.pmr.model.KPIComplianceResult;

@SpringBootApplication
public class PrevMaintRptApplication implements CommandLineRunner {
	Logger logger = LoggerFactory.getLogger(PrevMaintRptApplication.class);
	@Autowired
	private AlarmProcessingService alarmProcessingService;
	@Autowired
	private AlarmAnalysisReportService htmlReportService;
	@Autowired
	InventoryService inventoryService;
	@Autowired
	private PreventiveMaintenanceService preventiveMaintenanceService;
	@Autowired
	private KPIComplianceReportService kpiComplianceReportService;
	@Autowired
	Environment env;
	
	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(PrevMaintRptApplication.class, args);
		// Exit with an appropriate code so the process ends when work is done
		System.exit(SpringApplication.exit(ctx, () -> 0));
	}

	@Override
	public void run(String... args) throws Exception {
		logger.info("PrevMaintRptApplication started with command-line arguments: {}", String.join(" ", args));
		String doPm = this.env.getProperty("prepare.json.preventive.maintenance");
		logger.info("Configuration - prepare.json.preventive.maintenance: {}", doPm);
		String doAlarmAnalysis = this.env.getProperty("prepare.json.alarm.analysis");
		logger.info("Configuration - prepare.json.alarm.analysis: {}", doAlarmAnalysis);
		if ("true".equalsIgnoreCase(doPm) || "true".equalsIgnoreCase(doAlarmAnalysis)) {
			String egMgr = null;
			String user = null;
			
			if (args.length >= 2) {
				egMgr = args[0];
				user = args[1];
			} else {
				System.err.println("Usage: java -jar app.jar <egManagerUrl> <username>");
				return;
			}
	
			String password = null;
			String apiKey = null;
			if (System.console() != null) {
				char[] passwordArray = System.console().readPassword("Enter password for %s: ", user);
				if (passwordArray != null) {
					password = new String(passwordArray);
				}
				char[] apiKeyArray = System.console().readPassword("Enter API key for %s (if applicable, else press Enter): ", user);
				if (apiKeyArray != null && apiKeyArray.length > 0) {
					apiKey = new String(apiKeyArray);
					logger.info("API key provided, length: {}", apiKey.length());
					// If API key is provided, we can choose to use it instead of password or in combination based on requirements
					// For now, just logging its presence. Implementation can be added as needed.
					logger.info("API key will be used for authentication if provided.");
				}
			} else {
				System.out.println("Console is not available. Please enter password for " + user + ": ");
				Scanner scanner = new Scanner(System.in);
				if (scanner.hasNextLine()) {
					password = scanner.nextLine();
				}
				scanner.close();
			}
			
			if (password == null || password.isEmpty()) {
				System.err.println("Password cannot be empty.");
				return;
			}
			
			password = Base64.getEncoder().encodeToString(password.getBytes());
			
			EgRequestHeader egRequestHeader = new EgRequestHeader(egMgr, user, password, apiKey);
			AlarmsRepository alarmsRepository = new AlarmsRepository();
			
			logger.info("Step 1: Validating API credentials by fetching Alarm Count from EG Manager");
			logger.info("Fetching alarm count from EG Manager: {}", egMgr);
			System.out.println("Validating credentials and connectivity to eG Manager...");
			Map<String, Integer> alarmCount;
			try {
				alarmCount = alarmsRepository.getAlarmCount(egRequestHeader);
			} catch (Exception e) {
				if (e.toString().contains("EG REST API access is restricted in this eG Manager")) {
					System.err.println("EG REST API access is restricted in this eG Manager. Please enable it from the eG Manager settings and try again.");
					return;
				} else {
					throw e;
				}
			} 
			logger.info("Alarm Count: {}", alarmCount);
			
			logger.info("Step 2A: Collection of eG Databases");
			Scanner scanner = new Scanner(System.in);
			List<ManagedComponent> selectedServers = new ArrayList<ManagedComponent>();
			while (true) {
				System.out.println("Have you managed eG backend database in eG manager as a separate database component [Y/n]");
				String response = scanner.nextLine().trim();
				logger.info("User response for analyzing backend database: {}", response);
				// If user just presses enter or enters 'y', process it
				if (response.isEmpty() || response.equalsIgnoreCase("y")) {
					ManagedComponent component = new ManagedComponent();
					System.out.println("Select Database Type:");
					System.out.println("1. Microsoft SQL");
					System.out.println("2. Oracle");
					System.out.print("Enter choice (1/2): ");
					String dbChoice = scanner.nextLine().trim();
					logger.info("User selected database type choice: {}", dbChoice);
					// Currently just storing choice, assuming logic comes later or just collecting inputs.
					
					System.out.print("Enter database component name: ");
					String componentName = scanner.nextLine().trim();
					
					if (!componentName.matches(".*:\\d+$")) {
						System.out.print("Enter database component port: ");
						String port = scanner.nextLine().trim();
						component.setComponentName(componentName);
						component.setPort(port);
						componentName = componentName + ":" + port;
						
					}else {
						component.setComponentName(componentName.split(":")[0]);
						component.setPort(componentName.split(":")[1]);
					}
					System.out.println("Configured Database Component: " + componentName);
					
					component.setComponentType(dbChoice.equals("1")?"Microsoft SQL":"Oracle Database");
					selectedServers.add(component);
					System.out.println("Do you want to add more eG Database Components [Y/n]");
					String haveMoreComponents = scanner.nextLine().trim();
					if (haveMoreComponents.isEmpty() || haveMoreComponents.equalsIgnoreCase("n"))
						break;
				} else if (response.isEmpty() || response.equalsIgnoreCase("n")) {
					logger.info("User chose not to analyze backend database components.");
					break;
				} else {
					System.out.println("Invalid input. Please enter 'Y' for yes or 'N' for no.");
				}
			}
			scanner.close();
			
			TestRepository testDataDao = new TestRepository();
			logger.info("Fetching test mapping");
			Map<String, String> testMapping = testDataDao.getTestMapping(egRequestHeader);
			logger.info("Test mapping size: {}", testMapping.size());
			logger.info("Fetching measure mapping");
			Map<String, String> measureMapping = testDataDao.getMeasureMapping(egRequestHeader);
			logger.info("Measure mapping size: {}", measureMapping.size());
			
			if (doAlarmAnalysis.equalsIgnoreCase("true")) {
				System.out.println("Processing alarms and preparing preventive maintenance report...");
				this.alarmProcessingService.extractAlarms(egRequestHeader);
				System.out.println("Alarms extracted successfully. Preparing preventive maintenance report...");
			}
			if (doPm.equalsIgnoreCase("true")) {
				this.alarmProcessingService.preparePreventiveMaintenance(egRequestHeader, selectedServers, testMapping,
					measureMapping);
				System.out.println("Preventive maintenance report prepared successfully. Please check the output directory for the generated report.");
			}
		}
		if (env.getProperty("prepare.alarm.analysis.report").equalsIgnoreCase("true")) {
			// Generate HTML reports from alarm analysis zip files
			System.out.println("Generating HTML reports from alarm analysis data...");
			this.htmlReportService.generateHtmlReports();
			System.out.println("HTML reports generated successfully.");
		} else {
			logger.info("ZIP file analysis is disabled. Send the zip file to eG Innovations support team for analysis.");
		}
		
		// Process KPI Compliance Analysis from preventive maintenance ZIP files
		if ("true".equalsIgnoreCase(env.getProperty("prepare.kpi.compliance.report", "false"))) {
			System.out.println("\n=== Starting KPI Compliance Analysis ===");
			logger.info("KPI Compliance Analysis enabled");
			
			String currentDir = System.getProperty("user.dir");
			logger.info("Looking for preventive maintenance ZIP files in: {}", currentDir);
			
			java.util.List<java.io.File> zipFiles = preventiveMaintenanceService.findPreventiveMaintenanceZips(currentDir);
			
			if (zipFiles.isEmpty()) {
				System.out.println("No preventive maintenance ZIP files found starting with 'eg_preventive_maintenance'");
				logger.warn("No preventive maintenance ZIP files found in directory: {}", currentDir);
			} else {
				System.out.println("Found " + zipFiles.size() + " preventive maintenance ZIP file(s)");
				
				for (java.io.File zipFile : zipFiles) {
					System.out.println("\nProcessing: " + zipFile.getName());
					logger.info("Processing ZIP file: {}", zipFile.getAbsolutePath());
					
					try {
						java.util.List<KPIComplianceResult> results = 
							preventiveMaintenanceService.processZipFile(zipFile);
						
						System.out.println("Analyzed " + results.size() + " KPI(s)");
						
						if (!results.isEmpty()) {
							String htmlReport = kpiComplianceReportService.generateReport(results, zipFile.getName());
							java.io.File reportFile = kpiComplianceReportService.saveReport(htmlReport, zipFile.getName());
							
							System.out.println("✓ KPI Compliance Report generated: " + reportFile.getAbsolutePath());
							logger.info("Report saved to: {}", reportFile.getAbsolutePath());
						}
						
					} catch (Exception e) {
						System.err.println("Error processing " + zipFile.getName() + ": " + e.getMessage());
						logger.error("Error processing ZIP file: " + zipFile.getName(), e);
					}
				}
				
				System.out.println("\n=== KPI Compliance Analysis Complete ===");
			}
		} else {
			logger.info("KPI Compliance Analysis is disabled. Enable with prepare.kpi.compliance.report=true");
		}
	}
	
	
	
}