# HTML Alarm Report Generation

## Overview

The HTML Report Service generates professional, AI-powered analysis reports from alarm analysis ZIP files. The reports use Bootstrap styling similar to the eG Innovations website and include on-premise AI analysis using Ollama.

## Features

- **Professional Design**: Bootstrap-based responsive design with eG Innovations color scheme
- **AI-Powered Analysis**: On-premise AI analysis using Ollama (no cloud data transmission)
- **Measure Interpretation**: Automatic lookup of measure descriptions from eG help files
- **Visual Metrics**: Embedded metric graphs from alarm data
- **Comprehensive Details**: Full alarm context including component, test, measure, priority, etc.
- **Privacy-Focused**: All AI processing done locally, no data sent to cloud LLMs

## Configuration

### application.properties

```properties
# Ollama Configuration
ollama.enabled=true
ollama.api.url=http://localhost:11434/api/generate
ollama.model=llama2
ollama.temperature=0.7
ollama.max_tokens=2000
```

### Configuration Options

- `ollama.enabled`: Enable/disable Ollama AI analysis (default: true)
- `ollama.api.url`: Ollama API endpoint (default: http://localhost:11434/api/generate)
- `ollama.model`: AI model to use (default: llama2, alternatives: llama3, mistral, codellama)
- `ollama.temperature`: Creativity level 0.0-1.0 (default: 0.7)
- `ollama.max_tokens`: Maximum response length (default: 2000)

## Prerequisites

### 1. Install Ollama

**Windows:**
```cmd
Download and install from: https://ollama.ai/download
```

**Start Ollama:**
```cmd
ollama serve
```

### 2. Download AI Model

```cmd
ollama pull llama2
```

For better analysis, consider using larger models:
```cmd
ollama pull llama3
ollama pull mistral
```

## Usage

The HTML report generation runs automatically after alarm processing:

```bash
java -jar prev-maint-rpt.jar <egManagerUrl> <username>
```

### Process Flow

1. **Extract Alarms**: Generates alarm_analysis_*.zip files
2. **Preventive Maintenance**: Generates eg_preventive_maintenance_*.zip files
3. **HTML Reports**: Automatically generates HTML reports for each alarm_analysis_*.zip

### Output Files

For each `alarm_analysis_all-alarms_2026-02-15_080122.zip`, generates:
- `alarm_analysis_all-alarms_2026-02-15_080122.html`

## Report Structure

### Header Section
- Report title: "eG Alarm Analysis Report"
- Source ZIP file name
- Report generation date and time

### Alarm Cards (One per alarm)
Each alarm includes:

1. **Card Header**
   - Alarm number
   - Component name and measure
   - Repeat count badge (if alarm repeated)

2. **Alarm Details Table**
   - Component name and type
   - Test and measure names
   - Priority level (with color coding)
   - Layer
   - Description
   - Start time and duration
   - Additional info

3. **Metric Trend Graph**
   - Embedded image from alarm data
   - Base64-encoded PNG graph

4. **Measure Interpretation**
   - Description from eG help files
   - Measurement unit
   - Standard interpretation guide

5. **AI-Powered Analysis**
   - Alert interpretation
   - Root cause analysis
   - Impact assessment
   - Remediation steps

### Footer Section
- Privacy notice (on-premise AI processing)
- Copyright information
- eG Innovations website link

## Priority Color Coding

- **Critical**: Red border and danger badge
- **Major**: Orange/Warning border and badge
- **Minor**: Blue/Info border and badge
- **Warning**: Yellow/Warning border and badge

## AI Analysis

The Ollama service provides:

1. **Alert Interpretation**: What the alarm indicates
2. **Root Cause Analysis**: Likely causes of the issue
3. **Impact Assessment**: Potential impact on the system
4. **Remediation Steps**: Steps to fix the problem

### Fallback Behavior

If Ollama is unavailable:
- Reports still generate successfully
- AI analysis section shows friendly message
- All other report sections remain intact

## Help Files

Measure interpretations are loaded from:
```
src/main/resources/eghelp/<TestName>.json
```

Example: `Application Event Log.json`

Format:
```json
[
  {
    "Measurement": "Application errors",
    "Description": "Number of application error events generated",
    "MeasurementUnit": "Number",
    "Interpretation": "Low value indicates healthy state..."
  }
]
```

## Troubleshooting

### Ollama Not Running

**Symptom**: "Connection error" in AI analysis section

**Solution**:
```cmd
ollama serve
```

### Model Not Found

**Symptom**: "Model not found" error

**Solution**:
```cmd
ollama pull llama2
```

### Slow Response

**Symptom**: Long wait times for report generation

**Solutions**:
1. Use a smaller model (llama2 instead of llama3)
2. Reduce `ollama.max_tokens`
3. Increase `ollama.temperature` for faster but less detailed responses
4. Disable Ollama: `ollama.enabled=false`

### Missing Help Files

**Symptom**: "No interpretation data available" in report

**Solution**: Add JSON file to `src/main/resources/eghelp/` with test name

### Memory Issues

**Symptom**: OutOfMemoryError during report generation

**Solutions**:
1. Increase JVM heap: `java -Xmx4g -jar prev-maint-rpt.jar`
2. Process smaller alarm files
3. Disable Ollama for large datasets

## Best Practices

1. **Model Selection**
   - Development/Testing: llama2 (faster, less accurate)
   - Production: llama3 or mistral (slower, more accurate)

2. **Temperature Settings**
   - Technical Analysis: 0.3-0.5 (more deterministic)
   - General Insights: 0.7-0.9 (more creative)

3. **Performance**
   - Run Ollama on same machine for best performance
   - Use SSD storage for model files
   - GPU acceleration recommended for large models

4. **Privacy**
   - All data stays local (on-premise)
   - No internet connection required for AI analysis
   - Suitable for sensitive production environments

## Advanced Configuration

### Custom Prompts

To customize AI analysis prompts, modify:
`HtmlReportService.getOllamaInterpretation()`

### Custom Styling

To customize report appearance, modify:
`HtmlReportService.generateHtmlHeader()` CSS section

### Multiple Models

To use different models for different analysis types:

```properties
ollama.model.alarms=llama2
ollama.model.performance=mistral
```

## API Reference

### HtmlReportService

- `generateHtmlReports()`: Main entry point
- `generateHtmlReport(File zipFile)`: Process single ZIP file
- `getInterpretationFromResources(String test, String measure)`: Get help text
- `getOllamaInterpretation(...)`: Get AI analysis

### OllamaService

- `generateResponse(String prompt)`: Send prompt to Ollama
- `testConnection()`: Test Ollama connectivity
- `formatResponse(String response)`: Format AI response as HTML

## Examples

### Basic Usage

```bash
# Start Ollama
ollama serve

# Run application
java -jar prev-maint-rpt.jar https://egmanager.example.com admin
```

### Custom Model

```bash
# Download model
ollama pull mistral

# Update application.properties
# ollama.model=mistral

# Run application
java -jar prev-maint-rpt.jar https://egmanager.example.com admin
```

### Disable AI Analysis

```properties
# application.properties
ollama.enabled=false
```

Reports will still generate without AI analysis section.

## License

Copyright © 2026 eG Innovations. All rights reserved.
