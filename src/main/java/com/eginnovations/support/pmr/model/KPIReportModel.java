package com.eginnovations.support.pmr.model;

import java.util.List;

/**
 * Model for KPI Compliance Report template
 */
public class KPIReportModel {
    
    private String zipFileName;
    private String timestamp;
    private int totalChecks;
    private long compliantCount;
    private long nonCompliantCount;
    private long needsReviewCount;
    private List<KPIResultViewModel> results;
    
    public String getZipFileName() {
        return zipFileName;
    }
    
    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public int getTotalChecks() {
        return totalChecks;
    }
    
    public void setTotalChecks(int totalChecks) {
        this.totalChecks = totalChecks;
    }
    
    public long getCompliantCount() {
        return compliantCount;
    }
    
    public void setCompliantCount(long compliantCount) {
        this.compliantCount = compliantCount;
    }
    
    public long getNonCompliantCount() {
        return nonCompliantCount;
    }
    
    public void setNonCompliantCount(long nonCompliantCount) {
        this.nonCompliantCount = nonCompliantCount;
    }
    
    public long getNeedsReviewCount() {
        return needsReviewCount;
    }
    
    public void setNeedsReviewCount(long needsReviewCount) {
        this.needsReviewCount = needsReviewCount;
    }
    
    public List<KPIResultViewModel> getResults() {
        return results;
    }
    
    public void setResults(List<KPIResultViewModel> results) {
        this.results = results;
    }
    
    /**
     * View model for individual KPI results
     */
    public static class KPIResultViewModel {
        private String kpiId;
        private String statusClass;
        private String badgeClass;
        private String aiAnalysisClass;
        private String checkName;
        // ...existing code...
        private String complianceStatus;
        private String componentName;
        private String componentType;
        private String test;
        private String measure;
        private String timeline;
        private String measurementUnit;
        private String description;
        private String interpretation;
        private String aiAnalysis;
        private String aiAnalysisHtml;
        
        public String getKpiId() {
            return kpiId;
        }
        
        public void setKpiId(String kpiId) {
            this.kpiId = kpiId;
        }
        
        public String getStatusClass() {
            return statusClass;
        }
        
        public void setStatusClass(String statusClass) {
            this.statusClass = statusClass;
        }
        
        public String getBadgeClass() {
            return badgeClass;
        }
        
        public void setBadgeClass(String badgeClass) {
            this.badgeClass = badgeClass;
        }
        
        public String getAiAnalysisClass() {
            return aiAnalysisClass;
        }
        
        public void setAiAnalysisClass(String aiAnalysisClass) {
            this.aiAnalysisClass = aiAnalysisClass;
        }
        
        public String getCheckName() {
            return checkName;
        }
        
        public void setCheckName(String checkName) {
            this.checkName = checkName;
        }
        
        public String getComplianceStatus() {
            return complianceStatus;
        }
        
        public void setComplianceStatus(String complianceStatus) {
            this.complianceStatus = complianceStatus;
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
        
        public String getMeasurementUnit() {
            return measurementUnit;
        }
        
        public void setMeasurementUnit(String measurementUnit) {
            this.measurementUnit = measurementUnit;
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
        
        public String getAiAnalysis() {
            return aiAnalysis;
        }
        
        public void setAiAnalysis(String aiAnalysis) {
            this.aiAnalysis = aiAnalysis;
        }
        
        public String getAiAnalysisHtml() {
            return aiAnalysisHtml;
        }
        
        public void setAiAnalysisHtml(String aiAnalysisHtml) {
            this.aiAnalysisHtml = aiAnalysisHtml;
        }
    }
}
