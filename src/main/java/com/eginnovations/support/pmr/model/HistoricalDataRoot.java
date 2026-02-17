package com.eginnovations.support.pmr.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for the root historical data structure
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoricalDataRoot {
    
    @JsonProperty("historicalData")
    private HistoricalDataContent historicalData;
    
    public HistoricalDataContent getHistoricalData() {
        return historicalData;
    }
    
    public void setHistoricalData(HistoricalDataContent historicalData) {
        this.historicalData = historicalData;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HistoricalDataContent {
        
        @JsonProperty("metaData")
        private MetaData metaData;
        
        @JsonProperty("diagnosisData")
        private Object diagnosisData;
        
        @JsonProperty("historicalData")
        private Map<String, Object> data;
        
        public MetaData getMetaData() {
            return metaData;
        }
        
        public void setMetaData(MetaData metaData) {
            this.metaData = metaData;
        }
        
        public Object getDiagnosisData() {
            return diagnosisData;
        }
        
        public void setDiagnosisData(Object diagnosisData) {
            this.diagnosisData = diagnosisData;
        }
        
        public Map<String, Object> getData() {
            return data;
        }
        
        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetaData {
        
        @JsonProperty("componentType")
        private String componentType;
        
        @JsonProperty("measure")
        private String measure;
        
        @JsonProperty("test")
        private String test;
        
        @JsonProperty("timeline")
        private String timeline;
        
        @JsonProperty("componentName")
        private String componentName;
        
        public String getComponentType() {
            return componentType;
        }
        
        public void setComponentType(String componentType) {
            this.componentType = componentType;
        }
        
        public String getMeasure() {
            return measure;
        }
        
        public void setMeasure(String measure) {
            this.measure = measure;
        }
        
        public String getTest() {
            return test;
        }
        
        public void setTest(String test) {
            this.test = test;
        }
        
        public String getTimeline() {
            return timeline;
        }
        
        public void setTimeline(String timeline) {
            this.timeline = timeline;
        }
        
        public String getComponentName() {
            return componentName;
        }
        
        public void setComponentName(String componentName) {
            this.componentName = componentName;
        }
    }
}
