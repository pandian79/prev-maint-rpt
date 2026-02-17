# KPI Compliance Analysis - User Guide

## Overview

The KPI Compliance Analysis feature processes preventive maintenance ZIP files from eG Enterprise and generates comprehensive HTML reports showing the compliance status of Key Performance Indicators (KPIs). The analysis is powered by **on-premise AI** using Ollama, ensuring your data never leaves your infrastructure.

## Features

✓ **Automated ZIP File Processing** - Automatically discovers and processes `eg_preventive_maintenance*.zip` files  
✓ **AI-Powered Analysis** - Uses local Ollama LLM to analyze KPI health and compliance  
✓ **No Cloud Data Transfer** - All analysis is performed on-premise; data is never sent to cloud services  
✓ **Professional HTML Reports** - Generates visually appealing, detailed compliance reports  
✓ **Contextual Interpretation** - Leverages eG help documentation for accurate KPI assessment  
✓ **Compliance Tracking** - Clearly identifies COMPLIANT, NON-COMPLIANT, and NEEDS REVIEW KPIs  

## Prerequisites

1. **Ollama Installation**
   - Install Ollama from https://ollama.ai
   - Pull a model (e.g., `ollama pull llama2` or `ollama pull gemma3:12b`)
   - Ensure Ollama service is running on `http://localhost:11434`

2. **Configuration**
   - Update `application.properties` with your Ollama settings
   - Ensure `prepare.kpi.compliance.report=true` is set

3. **Input Files**
   - Place `eg_preventive_maintenance_*.zip` files in the application directory
   - Ensure `fileCategoryMapping.properties` contains the KPIs you want to analyze

## Configuration

### application.properties

```properties
# Enable KPI Compliance Report Generation
prepare.kpi.compliance.report=true

# Ollama Configuration
ollama.enabled=true
ollama.api.url=http://localhost:11434/api/generate
ollama.model=gemma3:12b
ollama.temperature=0.7
ollama.max_tokens=2000
```

### fileCategoryMapping.properties

Add entries for each KPI you want to analyze:

```properties
eG-Agents_Not-installed-licensed-agents.json=quality
eG-Agents_Not-running-licensed-agents.json=quality
Disk-Activity_Disk-busy.json=performance
Memory-Usage_Memory-utilized.json=performance
```

Format: `<filename_suffix>=<category>`

## Usage

### Running the Analysis

The KPI compliance analysis runs as part of the main application:

```bash
java -jar prev-maint-rpt.jar <egManagerUrl> <username>
```

The application will:
1. Look for ZIP files matching `eg_preventive_maintenance_*.zip`
2. Process each ZIP file entry that matches `fileCategoryMapping.properties`
3. Extract historical data and metadata
4. Look up interpretation guides from `eghelp/*.json` files
5. Generate AI analysis using Ollama
6. Create an HTML report

### Output

Reports are saved as: `kpi_compliance_report_YYYY-MM-DD_HHMMSS.html`

## Report Structure

### 1. Header Section
- Report title and timestamp
- Source ZIP file name
- AI analysis caveat

### 2. Summary Dashboard
- Total KPI Checks
- Compliant Count (green)
- Non-Compliant Count (red)
- Needs Review Count (yellow)

### 3. KPI Details
Each KPI card includes:
- **Component Information**: Name, type, test name
- **Measure Details**: KPI name, timeline, unit
- **Description**: What the KPI measures
- **Interpretation Guide**: How to interpret the values
- **AI Analysis**: Detailed assessment with compliance status and reasoning
- **Compliance Badge**: Visual indicator (COMPLIANT/NON-COMPLIANT/NEEDS REVIEW)

### 4. AI Caveat Notice
⚠️ Prominent warning that:
- Report is AI-generated using on-premise Ollama
- Data was NOT shared with cloud LLM systems
- Users should verify accuracy before taking action

## How It Works

### Step 1: ZIP File Discovery
Scans the current directory for files matching `eg_preventive_maintenance*.zip`

### Step 2: Entry Filtering
For each ZIP entry:
- Checks if filename ends with any key in `fileCategoryMapping.properties`
- Skips entries that don't match

### Step 3: Data Extraction
Parses JSON structure:
```json
{
  "historicalData": {
    "metaData": {
      "componentType": "...",
      "measure": "...",
      "test": "...",
      "timeline": "...",
      "componentName": "..."
    },
    "historicalData": { /* time-series data */ },
    "diagnosisData": { /* contextual information */ }
  }
}
```

### Step 4: Help Lookup
Loads `eghelp/<test_name>.json` and finds matching measure:
```json
{
  "Measurement": "Not running licensed agents",
  "Description": "Indicates the number of agents...",
  "MeasurementUnit": "Number",
  "Interpretation": "An agent that is not running will not be able..."
}
```

### Step 5: AI Prompt Generation
Constructs a detailed prompt including:
- KPI metadata (component, test, measure, timeline)
- Description and interpretation guide
- Historical data (truncated if too large)
- Diagnosis data (if available)
- Instructions for compliance analysis

Example prompt structure:
```
You are an expert system administrator analyzing eG Innovations monitoring data.

KPI INFORMATION:
Component: eG-Manager
Test: eG Agents
Measure: Not running licensed agents
Timeline: Last 7 days

DESCRIPTION:
Indicates the number of agents (configured in the eG manager) that are not running currently.

INTERPRETATION GUIDE:
An agent that is not running will not be able to collect metrics...

HISTORICAL DATA:
{ "2026-02-10 00:00:00": 0, "2026-02-11 00:00:00": 1, ... }

ANALYSIS REQUIRED:
Determine if this KPI is COMPLIANT or NON-COMPLIANT...
```

### Step 6: Ollama Analysis
Sends prompt to Ollama API and receives response with:
- STATUS: COMPLIANT or NON-COMPLIANT
- REASON: Detailed analysis
- Specific concerns or recommendations

### Step 7: Compliance Determination
Parses AI response to extract compliance status:
- Looks for explicit "STATUS: COMPLIANT" or "STATUS: NON-COMPLIANT"
- Falls back to keyword matching if status not explicit
- Defaults to "NEEDS REVIEW" if uncertain

### Step 8: HTML Report Generation
Creates a professionally styled HTML report with:
- Bootstrap 5 for responsive design
- Color-coded compliance indicators
- Expandable KPI cards
- Summary dashboard
- AI analysis caveat

## Troubleshooting

### Ollama Connection Failed
**Symptom**: "AI analysis unavailable due to error: Connection refused"

**Solution**:
1. Verify Ollama is running: `ollama list`
2. Check the service: `curl http://localhost:11434/api/tags`
3. Verify `ollama.api.url` in `application.properties`
4. Ensure no firewall is blocking port 11434

### No ZIP Files Found
**Symptom**: "No preventive maintenance ZIP files found"

**Solution**:
1. Ensure ZIP files are in the application directory
2. Verify filenames start with `eg_preventive_maintenance`
3. Check file permissions

### No KPIs Processed
**Symptom**: "Analyzed 0 KPI(s)"

**Solution**:
1. Check `fileCategoryMapping.properties` exists and has entries
2. Verify ZIP file contains matching JSON files
3. Check logs for parsing errors
4. Ensure JSON structure matches expected format

### Help File Not Found
**Symptom**: Warning in logs "Help file not found: eghelp/XYZ.json"

**Solution**:
1. Verify `eghelp` folder exists in `src/main/resources`
2. Check that `<test_name>.json` file exists (e.g., `eG Agents.json`)
3. Ensure test name in metadata exactly matches filename

### AI Analysis Quality Issues
**Symptom**: Inconsistent or inaccurate analysis

**Solution**:
1. Try a different Ollama model (e.g., `llama2`, `mistral`, `gemma3:12b`)
2. Adjust `ollama.temperature` (lower = more consistent, higher = more creative)
3. Increase `ollama.max_tokens` for more detailed analysis
4. Ensure historical data isn't being truncated too aggressively

## Advanced Configuration

### Custom Ollama Models

```properties
# For better analysis with larger models
ollama.model=llama3:70b
ollama.max_tokens=4000

# For faster processing with smaller models
ollama.model=gemma:7b
ollama.max_tokens=1000
```

### Disabling AI Analysis

```properties
# Generate reports without AI analysis
ollama.enabled=false
```

Reports will still be generated but without AI-powered compliance determination.

## File Structure

```
prev-maint-rpt/
├── src/main/
│   ├── java/com/eginnovations/support/pmr/
│   │   ├── PrevMaintRptApplication.java
│   │   ├── PreventiveMaintenanceService.java
│   │   ├── KPIComplianceReportService.java
│   │   ├── OllamaService.java
│   │   └── model/
│   │       ├── HistoricalDataRoot.java
│   │       ├── MeasureHelp.java
│   │       └── KPIComplianceResult.java
│   └── resources/
│       ├── application.properties
│       ├── fileCategoryMapping.properties
│       └── eghelp/
│           ├── eG Agents.json
│           ├── Disk Activity.json
│           ├── Memory Usage.json
│           └── ...
├── eg_preventive_maintenance_2026-02-15_080507.zip
└── kpi_compliance_report_2026-02-17_142530.html
```

## Best Practices

1. **Regular Analysis**: Run KPI compliance analysis weekly to catch issues early
2. **Model Selection**: Use larger models (70B+) for production analysis, smaller for testing
3. **Review AI Output**: Always verify AI analysis before taking corrective action
4. **Baseline Establishment**: Run analysis on known-good systems to establish baselines
5. **Documentation**: Keep `eghelp/*.json` files updated with latest eG documentation
6. **Category Mapping**: Regularly update `fileCategoryMapping.properties` as new KPIs are added

## Security Considerations

✓ **Data Privacy**: All analysis is performed locally; no data sent to cloud services  
✓ **On-Premise AI**: Ollama runs entirely on your infrastructure  
✓ **No External Dependencies**: No internet connection required for analysis  
✓ **Audit Trail**: Comprehensive logging of all processing steps  

## Support

For issues or questions:
1. Check logs in `logs/` directory
2. Enable DEBUG logging: `logging.level.com.eginnovations.support.pmr=DEBUG`
3. Verify Ollama model is working: `ollama run <model_name> "test"`
4. Contact eG Innovations support with log files and sample ZIP

## Example Output

### Console Output
```
=== Starting KPI Compliance Analysis ===
Found 1 preventive maintenance ZIP file(s)

Processing: eg_preventive_maintenance_2026-02-15_080507.zip
Processing entry: eG-Agents_Not-running-licensed-agents.json
Analyzed 2 KPI(s)
✓ KPI Compliance Report generated: E:\eGCRM\crm-workspace\prev-maint-rpt\kpi_compliance_report_2026-02-17_142530.html

=== KPI Compliance Analysis Complete ===
```

### Report Summary
- **Total KPI Checks**: 2
- **Compliant**: 1 (50%)
- **Non-Compliant**: 1 (50%)
- **Needs Review**: 0 (0%)

## Version History

- **v1.0** (2026-02-17): Initial release
  - ZIP file processing
  - AI-powered analysis
  - HTML report generation
  - On-premise Ollama integration

## License

This software is part of the eG Enterprise suite. All rights reserved.
