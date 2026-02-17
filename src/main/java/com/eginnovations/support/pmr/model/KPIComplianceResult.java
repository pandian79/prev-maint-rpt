package com.eginnovations.support.pmr.model;

/**
 * Model representing a KPI compliance check result
 */
public class KPIComplianceResult {
    
    private String entryName;
    private String componentName;
    private String componentType;
    private String test;
    private String measure;
    private String timeline;
    private String description;
    private String interpretation;
    private String measurementUnit;
    private boolean compliant;
    private String complianceStatus; // "COMPLIANT", "NON-COMPLIANT", "NEEDS ATTENTION"
    private String aiAnalysis;
    private String rawData;
    private String diagnosisData;
    
    public String getEntryName() {
        return entryName;
    }
    
    public void setEntryName(String entryName) {
        this.entryName = entryName;
    }
    
    public String getComponentName() {
        return componentName;
    }
    
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }
    
    public String getComponentType() {
        return componentType;
    }
    
    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }
    
    public String getTest() {
        return test;
    }
    
    public void setTest(String test) {
        this.test = test;
    }
    
    public String getMeasure() {
        return measure;
    }
    
    public void setMeasure(String measure) {
        this.measure = measure;
    }
    
    public String getTimeline() {
        return timeline;
    }
    
    public void setTimeline(String timeline) {
        this.timeline = timeline;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getInterpretation() {
        return interpretation;
    }
    
    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }
    
    public String getMeasurementUnit() {
        return measurementUnit;
    }
    
    public void setMeasurementUnit(String measurementUnit) {
        this.measurementUnit = measurementUnit;
    }
    
    public boolean isCompliant() {
        return compliant;
    }
    
    public void setCompliant(boolean compliant) {
        this.compliant = compliant;
    }
    
    public String getComplianceStatus() {
        return complianceStatus;
    }
    
    public void setComplianceStatus(String complianceStatus) {
        this.complianceStatus = complianceStatus;
    }
    
    public String getAiAnalysis() {
        return aiAnalysis;
    }
    
    public void setAiAnalysis(String aiAnalysis) {
        this.aiAnalysis = aiAnalysis;
    }
    
    public String getRawData() {
        return rawData;
    }
    
    public void setRawData(String rawData) {
        this.rawData = rawData;
    }
    
    public String getDiagnosisData() {
        return diagnosisData;
    }
    
    public void setDiagnosisData(String diagnosisData) {
        this.diagnosisData = diagnosisData;
    }
    
    /**
     * Get a display-friendly name for the check
     */
    public String getCheckName() {
        return componentName + " - " + test + " - " + measure;
    }
}
