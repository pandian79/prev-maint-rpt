package com.eginnovations.support.pmr;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.eg.api.client.EgRequestHeader;
import com.eg.api.client.dao.AlarmsRepository;
import com.eg.api.client.entity.ManagedComponent;
import com.eg.api.client.dao.GenericApiRepository;
import com.eg.api.client.dao.MetricsRepository;
import com.eg.api.client.dao.TestRepository;
import com.eg.api.client.entity.AlarmHistory;
import com.eg.api.client.entity.AlarmHistoryRecord;
import com.eg.api.client.entity.AlarmHistoryRequestBody;
import com.eg.api.client.entity.DiagnosisDataRequestBody;
import com.eg.api.client.entity.EnabledDisabledTests;
import com.eg.api.client.entity.HistoricalDataRequestBody;
import com.eg.api.client.entity.TestData;
import com.eg.api.client.exception.ComponentNotAssociatedException;
import com.eg.api.client.exception.InvalidRequestHeaderException;
import com.eg.api.client.exception.TestNotAssociatedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service class responsible for processing alarms, fetching historical data, diagnosis data, and preparing preventive maintenance reports.
 * @author Murugapandian
 * @since 2026-02
 */

@Service
public class AlarmProcessingService {
	Logger logger = LoggerFactory.getLogger(AlarmProcessingService.class);
	@Autowired
	Environment env;
	@Autowired
	InventoryService inventoryService;
	@Autowired
	Environment environment;
	
	private MetricsRepository metricsRepository = new MetricsRepository();
	private GenericApiRepository genericApiRepository = new GenericApiRepository();
	private AlarmsRepository alarmsRepository = new AlarmsRepository();
	private TestRepository testRepository = new TestRepository();

	void extractAlarms(EgRequestHeader egRequestHeader)
			throws JsonMappingException, JsonProcessingException, InvalidRequestHeaderException, Exception {
		AlarmHistoryRequestBody body = new AlarmHistoryRequestBody();
		body.setTimeline(this.environment.getProperty("analysis.timeline", "24 hours"));
		logger.info("Step 4: Fetch Alarm History");
		System.out.println("Fetching alarm history for all components...");
		AlarmHistory alarmsHistory = alarmsRepository.getAlarmsHistory(egRequestHeader, body);
		System.out.println("Total alarms fetched: " + (alarmsHistory.getProblemDetails() != null ? alarmsHistory.getProblemDetails().size() : 0));
		logger.info("Others: Alarm History: {}", alarmsHistory.getSummary());
		
		analyzeGroupedAlarms("all-alarms", alarmsHistory, egRequestHeader);
		
	}

	/**
	 * Analyzes grouped alarms by fetching historical data, diagnosis data, and measure graphs.
	 * Saves the collected data into JSON files named by groupId.
	 * 
	 * @param groupedAlarms Map of grouped alarms
	 * @param genericApiRepository Repository to fetch generic API data
	 * @param egRequestHeader Request header for EG Manager API calls
	 */
	
	private void analyzeGroupedAlarms(String fileName, AlarmHistory alarmHistory, EgRequestHeader egRequestHeader) {
		logger.info("Analyzing grouped alarms from file: {}", fileName);
		Map<String, List<AlarmHistoryRecord>> groupedAlarms = groupAlarms(alarmHistory);
		System.out.println("Total alarm groups identified: " + groupedAlarms.size());
		logger.info("Grouped Alarms Count: {}", groupedAlarms.size());
		ObjectMapper objectMapper = new ObjectMapper();
		String formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
		String zipFileName = "alarm_analysis_"+ fileName + "_" + formattedTime + ".zip";
		zipFileName = normalize(zipFileName);
		
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName))) {
			logger.info("Writing output to zip file: {}", zipFileName);
			
			int c=0;
			int total = groupedAlarms.size();

			for (Map.Entry<String, List<AlarmHistoryRecord>> entry : groupedAlarms.entrySet()) {
				c++;
				System.out.println("Processing alarm group "+c+"/"+total+": "+entry.getKey()+" with "+entry.getValue().size()+" alarms");
				String groupId = UUID.randomUUID().toString();
				List<AlarmHistoryRecord> records = entry.getValue();
				if (records.isEmpty()) continue;
				
				AlarmHistoryRecord firstRecord = records.get(0);
				logger.info("Processing alarm group: {} | ID: {} | Total Alarms {}", entry.getKey(), groupId, records.size());
				firstRecord.setRepeatCount(records.size()); // Set repeat count in the representative alarm record
				
				try {
					// Prepare common data structure to serialize
					Map<String, Object> serializedData = new HashMap<>();
					serializedData.put("groupId", groupId);
					serializedData.put("key", entry.getKey());
					serializedData.put("representativeAlert", firstRecord);
					
					// 1. Get Historical Data
					HistoricalDataRequestBody histReq = new HistoricalDataRequestBody();
					String startTime = firstRecord.getStartTime(); //Aug 30, 2021 22:00:00
					long hoursPassed = 1;
					if (startTime != null && !startTime.isEmpty()) {
						try {
							DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", java.util.Locale.ENGLISH);
							LocalDateTime startDateTime = LocalDateTime.parse(startTime, formatter);
							hoursPassed = java.time.temporal.ChronoUnit.HOURS.between(startDateTime, LocalDateTime.now());
							if (hoursPassed < 1) hoursPassed = 1;
							if (hoursPassed > 24) hoursPassed = 24; // Limit to 24 hours for relevance
						} catch (Exception e) {
							logger.warn("Error parsing start time: {}", startTime);
						}
					}
					histReq.setTimeline(hoursPassed + " hours");
					
					histReq.setComponentName(firstRecord.getComponentName());
					String componentType = firstRecord.getComponentType();
					if (componentType.indexOf(',') != -1) {
						componentType = componentType.substring(0, componentType.indexOf(',')).trim();
					}
					histReq.setComponentType(componentType);
					histReq.setTest(firstRecord.getTest());
					histReq.setMeasure(firstRecord.getMeasure());
					histReq.setShowDisplayName(false);
					serializedData.put("metaData", histReq);
					
					// Try to set start/end date from the alarm record if available and timeline is not "Any"?
					// The prompt says "get historical data...". Let's rely on entity default timeline or the one we set.
					
					try {
						Map<String, List<TestData>> historicalDataMap = genericApiRepository.getHistoricalData(histReq, egRequestHeader);
						serializedData.put("historicalData", historicalDataMap);
					} catch(ComponentNotAssociatedException e) {
						logger.warn("Error while getting historical data for "+histReq+": "+e.getMessage());
					} catch (Exception e) {
						logger.error("Failed to fetch historical data for group {}", groupId, e);
						serializedData.put("historicalDataError", e.getMessage());
					}
	
					// 2. Get Detailed Diagnosis Data
					DiagnosisDataRequestBody diagReq = new DiagnosisDataRequestBody();
					diagReq.setTimeline(hoursPassed + " hours");
					diagReq.setComponentName(firstRecord.getComponentName());
					diagReq.setComponentType(componentType);
					diagReq.setTest(firstRecord.getTest());
					diagReq.setMeasure(firstRecord.getMeasure());
					diagReq.setInfo(firstRecord.getInfo()); // Info is descriptor
					diagReq.setShowDisplayName(false);
					
					try {
						List<Map<String, String>> diagnosisData = genericApiRepository.getDiagnosisData(diagReq, egRequestHeader);
						serializedData.put("diagnosisData", diagnosisData);
					} catch (Exception e) {
						// It's possible diagnosis is not available for all measures
						logger.warn("Failed to fetch diagnosis data for group {}: {}", groupId, e.getMessage());
						serializedData.put("diagnosisDataError", e.getMessage());
					}
	
					// 3. Get Image of the metrics
					Map<String, String> imageReqMap = new HashMap<>();
					imageReqMap.put("timeline", hoursPassed + " hours");
					imageReqMap.put("componentName", firstRecord.getComponentName());
					imageReqMap.put("componentType", componentType);
					imageReqMap.put("test", firstRecord.getTest());
					imageReqMap.put("measure", firstRecord.getMeasure());
					// callImageApi is private or wrapped? GenericApiRepository has getMeasureGraph(Map<String, String>, EgRequestHeader)
					
					try {
						byte[] imageBytes = genericApiRepository.getMeasureGraph(imageReqMap, egRequestHeader);
						if (imageBytes != null) {
							String base64Image = Base64.getEncoder().encodeToString(imageBytes);
							serializedData.put("measureGraphBase64", base64Image);
						}
					} catch (Exception e) {
						logger.error("Failed to fetch measure graph for group {} {}", imageReqMap, groupId);
						serializedData.put("measureGraphError", e.getMessage());
					}
	
					// Serialize to file
					String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(serializedData);
					String zipEntryName = entry.getKey()+"."+groupId + ".json";
					logger.info("Creating zip entry for group {} with name {}", groupId, zipEntryName);
					zipEntryName = normalize(zipEntryName);
					ZipEntry zipEntry = new ZipEntry(zipEntryName);
					zos.putNextEntry(zipEntry);
					zos.write(jsonOutput.getBytes(StandardCharsets.UTF_8));
					zos.closeEntry();
					logger.info("Saved data for group {} to zip entry {}.json", groupId, groupId);
				} catch (Exception e) {
					logger.error("Error processing alarm group {}", groupId, e);
				}
			}
		} catch (IOException e) {
			logger.error("Error writing zip file", e);
		}
	}

	private String normalize(String text) {
		text = text.replaceAll(" ", "_");
		text = text.replaceAll(":", "_");
		text = text.replaceAll("/", "_");
		text = text.replace("\\", "_");
		text = text.replaceAll("#", "_");
		return text;
	}
	
	/**
	 * Groups alarms based on componentName, test, measure, info
	 * @param alarmHistory The AlarmHistory object containing alarm records
	 * @return A map where the key is a composite string of componentName|test|measure|info and the value is a list of AlarmHistoryRecord
	 */
	
	private Map<String, List<AlarmHistoryRecord>> groupAlarms(AlarmHistory alarmHistory) {
		if (alarmHistory == null || alarmHistory.getProblemDetails() == null) {
			return Collections.emptyMap();
		}
		
		Map<String, List<AlarmHistoryRecord>> grouped = new HashMap<>();
		for (AlarmHistoryRecord record : alarmHistory.getProblemDetails()) {
			// Key based on componentName, test, measure, info
			String key = String.format("%s-%s-%s-%s", 
					record.getComponentName(), 
					record.getTest(), 
					record.getMeasure(), 
					record.getInfo());
			
			grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
		}
		return grouped;
	}
	
	/**
	 * Prepares preventive maintenance data by fetching enabled tests for each component, then fetching historical data and diagnosis data for each test and measure.
	 * Saves the collected data into a zip file containing JSON files named by component, test, and measure.
	 * 
	 * @param egRequestHeader Request header for EG Manager API calls
	 * @param selectedServers List to be populated with components that are of type "eG Manager" or "eG Agent"
	 * @param testDataDao Repository to fetch test data
	 * @param testMapping Mapping of internal test names to display names
	 * @param measureMapping Mapping of internal test:measure names to display names
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 * @throws InvalidRequestHeaderException
	 */
	public void preparePreventiveMaintenance(EgRequestHeader egRequestHeader,
			List<ManagedComponent> selectedServers,
			Map<String, String> testMapping, 
			Map<String, String> measureMapping)
			throws JsonMappingException, JsonProcessingException, InvalidRequestHeaderException {
		List<ManagedComponent> components = inventoryService.getComponents(egRequestHeader);
		logger.info("Total components fetched from inventory: {}", components.size());
		String formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
		String zipFileName = "eg_preventive_maintenance_" + formattedTime + ".zip";
		
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName))) {
            ObjectMapper objectMapper = new ObjectMapper();
            
			for (ManagedComponent component : components) {
				if (
						component.getComponentType().contains("eG Manager") 
						|| component.getComponentType().contains("eG Agent")
						) {
					selectedServers.add(component);
				}else {
					logger.info("Skipping component: {} of type {}", component.getComponentName(), component.getComponentType());
				}
			}
			
			logger.info("Analysing capacity of {} components: ", selectedServers.size());
			for (ManagedComponent component : selectedServers) {
				logger.info("Processing component:  {}/{} - {}",
						selectedServers.indexOf(component)+1, selectedServers.size(), component.getComponentName());
				System.out.println("Processing component:  "+(selectedServers.indexOf(component)+1)+"/"+selectedServers.size()+" - "+component.getComponentName());
				
				Map<String, String> bodyMap = new HashMap<>();
				bodyMap.put("componentName", component.getComponentName());
				bodyMap.put("componenttype", component.getComponentType());
				
				try {
					
					logger.info("Fetching enabled/disabled tests for component {} of type {}", 
							component.getComponentName(), component.getComponentType());
					final AtomicReference<EnabledDisabledTests> enabledDisabledTestsRef = new AtomicReference<>();
					try {
						enabledDisabledTestsRef.set(testRepository.showTests(egRequestHeader, bodyMap));
					} catch (RuntimeException e) {
						// Non-admin users will fail with "User does not have privilege to do this administration activity"
						// Fall back to loading from classpath
						logger.warn("API call failed (likely {} is a non-admin user), loading from classpath instead: {}", egRequestHeader.getUser(), e.getMessage());
						
						String fileName = "showTests." + component.getComponentType() + ".json";
						try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
							if (inputStream != null) {
								enabledDisabledTestsRef.set(objectMapper.readValue(inputStream, EnabledDisabledTests.class));
								logger.info("Loaded enabled/disabled tests from classpath file: {}", fileName);
							} else {
								logger.warn("File not found in classpath: {}, skipping component", fileName);
								continue;
							}
						} catch (IOException ioException) {
							logger.error("Failed to load or parse file {} from classpath", fileName, ioException);
							continue;
						}
					}
					
					EnabledDisabledTests enabledDisabledTests = enabledDisabledTestsRef.get();
					if (enabledDisabledTests == null) {
						logger.warn("No enabled/disabled tests available for component {}, skipping", component.getComponentName());
						continue;
					}
					
					logger.info("Enabled tests for component {}: {}", 
							component.getComponentName(), enabledDisabledTests.getEnabledTests().size());
					
					enabledDisabledTests.getEnabledTests().forEach(test -> {
						System.out.println("Processing test: "+
								enabledDisabledTests.getEnabledTests().indexOf(test)+1+"/"+
								enabledDisabledTests.getEnabledTests().size()+
								"for "+component.getComponentType()+" "+component.getComponentName());
						logger.info("Enabled test for component {}: {}", component.getComponentName(), test);
						testMapping.keySet().forEach(internalTest ->{
							if (testMapping.get(internalTest).equals(test)) {
								logger.info("Getting metrics for test {} (internal name: {}) for component {}", 
										test, internalTest, component.getComponentName());
								measureMapping.keySet().forEach(testMeasure -> { //AppEvtLogTest:Information_count
									if (testMeasure.startsWith(internalTest+":")) {
										logger.info("Getting metrics for measure {} (display name: {}) for test {} for component {}", 
												testMeasure, measureMapping.get(testMeasure), test, component.getComponentName());
										String measureName = measureMapping.get(testMeasure);
										Map<String, String> historyBodyMap = new HashMap<>();
										historyBodyMap.put("timeline", this.environment.getProperty("analysis.timeline", "24 hours"));
										historyBodyMap.put("componentName", component.getComponentName()+":"+component.getPort());
										historyBodyMap.put("componentType", component.getComponentType());
										historyBodyMap.put("test", test);
										historyBodyMap.put("measure", measureName);
										historyBodyMap.put("showDisplayName", "true");
										
										// 2. Get Detailed Diagnosis Data
										DiagnosisDataRequestBody diagReq = new DiagnosisDataRequestBody();
										diagReq.setTimeline("1 days");
										diagReq.setComponentName(component.getComponentName()+":"+component.getPort());
										diagReq.setComponentType(component.getComponentType());
										diagReq.setTest(test);
										diagReq.setMeasure(measureName);
										
										diagReq.setShowDisplayName(true);
										
										Map<String, List<TestData>> historicalData=null;
										String name = component.getComponentType()
												+ "_"
												+ component.getComponentName()
												+ "_"
												+ historyBodyMap.get("test")+"_"
												+ measureName
												+ ".json";
										name = name.replaceAll(" ", "-");
										name = name.replaceAll(":", "-");
										name = name.replaceAll("/", "-");
										ZipEntry zipEntry = new ZipEntry(name);
										String jsonOutput = null;
										Map<String, Object> outputMap = new HashMap<>();
										try {
											//Step 1: collect historical data for the measure
											try {
												historicalData = metricsRepository.getHistoricalData(egRequestHeader, historyBodyMap);
											} catch (TestNotAssociatedException e) {
												logger.warn("Test {} or measure {} may not be associated with component {}, skipping historical data and diagnosis data fetch for this measure. Error: {}", 
														test, measureName, component.getComponentName(), e.getMessage());
												historicalData = new HashMap<>();
											}
											int valueCount = historicalData.values().stream().mapToInt(List::size).sum();
											logger.info("Fetched historical data for component {} test {} measure {}, data points: {}", 
													component.getComponentName(), test, measureName, valueCount);
											
											Map<String, Object> historicalDataMap = new HashMap<>();
											outputMap.put("historicalData", historicalDataMap);
											historicalDataMap.put("metaData",historyBodyMap);
											historicalDataMap.put("historicalData", historicalData);
											
											//Step 2: collect historical dd for the measure
											diagReq.setInfo(null);
											if (historicalData.size()>1) {
												Set<String> keySet = historicalData.keySet();
												logger.info("historical data has multiple descriptors: {}", keySet);
												for (String info: keySet) {
													diagReq.setInfo(info);
													logger.info("Fetching diagnosis data for component {} test {} measure {} info {}", 
															component.getComponentName(), test, measureName, info);
													List<Map<String, String>> diagnosisData = genericApiRepository.getDiagnosisData(diagReq, egRequestHeader);
													Map<String, Object> historicalDetailedDiagnosisDataMap = new HashMap<>();
													historicalDetailedDiagnosisDataMap.put("diagnosisDataMetaData-"+info, diagReq);
													historicalDetailedDiagnosisDataMap.put("diagnosisData-"+info, diagnosisData);
													outputMap.put("historicalDetailedDiagnosisData-"+info, historicalDetailedDiagnosisDataMap);
												}
											}else {
												logger.info("historical data has single descrtiptor, fetching diagnosis data with info as null for component {} test {} measure {}", 
														component.getComponentName(), test, measureName);
												List<Map<String, String>> diagnosisData = genericApiRepository.getDiagnosisData(diagReq, egRequestHeader);
												historicalDataMap.put("diagnosisDataMetaData", diagReq);
												Set<String> keySet = historicalData.keySet();
												if (historicalDataMap.get("message")!=null) {
													logger.warn("Retrying DD for component {} test {} measure {}: {}", 
															component.getComponentName(), test, measureName, historicalDataMap.get("message"));
													diagReq.setComponentName(component.getComponentName());
													diagnosisData = genericApiRepository.getDiagnosisData(diagReq, egRequestHeader);
												}
												if (historicalDataMap.get("message")!=null) {
													logger.warn("Retrying 2 DD for component {} test {} measure {}: {}", 
															component.getComponentName(), test, measureName, historicalDataMap.get("message"));
													diagReq.setComponentName(component.getComponentName());
													diagReq.setInfo(keySet.iterator().next());
													diagnosisData = genericApiRepository.getDiagnosisData(diagReq, egRequestHeader);
												}
												keySet = historicalData.keySet();
												if (historicalDataMap.get("message")!=null) {
													logger.warn("Retrying 3 DD for component {} test {} measure {}: {}", 
															component.getComponentName(), test, measureName, historicalDataMap.get("message"));
													diagReq.setComponentName(component.getComponentName()+":"+component.getPort());
													diagReq.setInfo(keySet.iterator().next());
													diagnosisData = genericApiRepository.getDiagnosisData(diagReq, egRequestHeader);
												}
												historicalDataMap.put("diagnosisData", diagnosisData);
											}
										}catch (ComponentNotAssociatedException e) {
											outputMap.put("error", e.getMessage());
											outputMap.put("metaData", historyBodyMap);
											outputMap.put("diagnosisDataMetaData", diagReq);
										}catch (Exception e) {
											logger.error("Failed to fetch historical data for component {} test {} measure {}", 
													component.getComponentName(), test, measureName, e);
											outputMap.put("error", e.getMessage());
											outputMap.put("metaData", historyBodyMap);
											outputMap.put("diagnosisDataMetaData", diagReq);
										}finally {
											try {
												jsonOutput = objectMapper.
														writerWithDefaultPrettyPrinter().writeValueAsString(outputMap);
												zos.putNextEntry(zipEntry);
												zos.write(jsonOutput.getBytes(StandardCharsets.UTF_8));
												zos.closeEntry();
											} catch (IOException e1) {
												logger.error("Failed to create zip entry for component {} test {} measure {}", 
														component.getComponentName(), test, measureName, e1);
											}
										}
									}
								});
							}
						}
						);
					});
				} catch (Exception e) {
					logger.error("Failed to fetch historical data for component {}", component.getComponentName(), e);
				}
			}
			logger.info("Saved CPU utilization data to {}", zipFileName);
		} catch (IOException e) {
			logger.error("Error writing CPU utilization zip file", e);
		}
	}

}