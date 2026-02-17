package com.eginnovations.support.pmr.model;

/**
 * Model class representing an alarm for Thymeleaf template rendering
 */
public class AlarmReportData {
    
    // Basic alarm information
    private String componentName;
    private String componentType;
    private String test;
    private String measure;
    private String priority;
    private String priorityColor;
    private String layer;
    private String description;
    private String startTime;
    private String duration;
    private String info;
    private int repeatCount;
    
    // Visual data
    private String measureGraphBase64;
    
    // Interpretation data
    private String interpretation;
    private String interpretationDescription;
    private String interpretationUnit;
    private String interpretationText;
    
    // AI analysis
    private String aiAnalysis;
    
    // Constructors
    public AlarmReportData() {
    }
    
    // Getters and Setters
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
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
        this.priorityColor = getPriorityColorForBadge(priority);
    }
    
    public String getPriorityColor() {
        return priorityColor;
    }
    
    public String getLayer() {
        return layer;
    }
    
    public void setLayer(String layer) {
        this.layer = layer;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getStartTime() {
        return startTime;
    }
    
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    
    public String getDuration() {
        return duration;
    }
    
    public void setDuration(String duration) {
        this.duration = duration;
    }
    
    public String getInfo() {
        return info;
    }
    
    public void setInfo(String info) {
        this.info = info;
    }
    
    public int getRepeatCount() {
        return repeatCount;
    }
    
    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }
    
    public String getMeasureGraphBase64() {
        return measureGraphBase64;
    }
    
    public void setMeasureGraphBase64(String measureGraphBase64) {
        this.measureGraphBase64 = measureGraphBase64;
    }
    
    public String getInterpretation() {
        return interpretation;
    }
    
    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }
    
    public String getInterpretationDescription() {
        return interpretationDescription;
    }
    
    public void setInterpretationDescription(String interpretationDescription) {
        this.interpretationDescription = interpretationDescription;
    }
    
    public String getInterpretationUnit() {
        return interpretationUnit;
    }
    
    public void setInterpretationUnit(String interpretationUnit) {
        this.interpretationUnit = interpretationUnit;
    }
    
    public String getInterpretationText() {
        return interpretationText;
    }
    
    public void setInterpretationText(String interpretationText) {
        this.interpretationText = interpretationText;
    }
    
    public String getAiAnalysis() {
        return aiAnalysis;
    }
    
    public void setAiAnalysis(String aiAnalysis) {
        this.aiAnalysis = aiAnalysis;
    }
    
    /**
     * Gets Bootstrap color class for priority
     * Uses custom eG color for minor priority
     */
    private String getPriorityColorForBadge(String priority) {
        if (priority == null) return "secondary";
        
        switch (priority.toLowerCase()) {
            case "critical": return "danger";
            case "major": return "warning";
            case "minor": return "eg-minor"; // Custom eG minor color (#ccc100)
            case "warning": return "warning";
            default: return "secondary";
        }
    }
}
