package com.eginnovations.support.pmr.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for measure help information from eghelp JSON files
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeasureHelp {
    
    @JsonProperty("Measurement")
    private String measurement;
    
    @JsonProperty("Description")
    private String description;
    
    @JsonProperty("MeasurementUnit")
    private String measurementUnit;
    
    @JsonProperty("Interpretation")
    private String interpretation;
    
    public String getMeasurement() {
        return measurement;
    }
    
    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getMeasurementUnit() {
        return measurementUnit;
    }
    
    public void setMeasurementUnit(String measurementUnit) {
        this.measurementUnit = measurementUnit;
    }
    
    public String getInterpretation() {
        return interpretation;
    }
    
    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }
}
