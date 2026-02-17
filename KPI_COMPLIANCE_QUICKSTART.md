# KPI Compliance Analysis - Quick Start Guide

## Quick Setup (5 minutes)

### 1. Install Ollama
```bash
# Windows: Download from https://ollama.ai/download
# After installation, pull a model:
ollama pull gemma3:12b
```

### 2. Configure Application
Edit `src/main/resources/application.properties`:
```properties
prepare.kpi.compliance.report=true
ollama.enabled=true
ollama.model=gemma3:12b
```

### 3. Configure KPIs to Analyze
Edit `src/main/resources/fileCategoryMapping.properties`:
```properties
eG-Agents_Not-installed-licensed-agents.json=quality
eG-Agents_Not-running-licensed-agents.json=quality
```

### 4. Run Analysis
```bash
# Make sure eg_preventive_maintenance_*.zip is in the current directory
java -jar prev-maint-rpt.jar http://your-eg-manager:8080 admin
```

### 5. View Report
Open the generated `kpi_compliance_report_YYYY-MM-DD_HHMMSS.html` in your browser

## Key Files

| File | Purpose |
|------|---------|
| `PreventiveMaintenanceService.java` | ZIP processing and AI analysis orchestration |
| `KPIComplianceReportService.java` | HTML report generation |
| `OllamaService.java` | Ollama API integration |
| `fileCategoryMapping.properties` | Defines which KPIs to analyze |
| `eghelp/*.json` | Interpretation guides for KPIs |

## Architecture Flow

```
ZIP File → Filter by Category → Parse JSON → Lookup Help → Build Prompt
                                                                ↓
HTML Report ← Determine Status ← Parse Response ← Send to Ollama
```

## Sample KPI Entry Structure

**Input (ZIP Entry JSON)**:
```json
{
  "historicalData": {
    "metaData": {
      "componentType": "eG Manager",
      "measure": "Not running licensed agents",
      "test": "eG Agents",
      "timeline": "Last 7 days",
      "componentName": "eG-Manager"
    },
    "historicalData": {
      "2026-02-10 00:00:00": 0,
      "2026-02-11 00:00:00": 1
    },
    "diagnosisData": "Agent XYZ stopped on 2026-02-11"
  }
}
```

**Output (KPIComplianceResult)**:
```java
{
  "checkName": "eG-Manager - eG Agents - Not running licensed agents",
  "complianceStatus": "NON-COMPLIANT",
  "aiAnalysis": "STATUS: NON-COMPLIANT\nREASON: Historical data shows...",
  "description": "Indicates the number of agents...",
  "interpretation": "An agent that is not running will not be able..."
}
```

## Customization Points

### 1. Add New KPIs
Add to `fileCategoryMapping.properties`:
```properties
Your-Test_Your-Measure.json=category
```

### 2. Add Help Documentation
Create `src/main/resources/eghelp/Your Test.json`:
```json
[
  {
    "Measurement": "Your Measure",
    "Description": "What it measures",
    "MeasurementUnit": "Unit",
    "Interpretation": "How to interpret"
  }
]
```

### 3. Customize AI Prompt
Edit `PreventiveMaintenanceService.generateAIAnalysis()`:
```java
prompt.append("YOUR CUSTOM INSTRUCTIONS\n");
```

### 4. Change Report Styling
Edit `KPIComplianceReportService.getCSS()`:
```java
return "body { background-color: #yourcolor; }";
```

## Testing

### Test Ollama Connection
```bash
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{"model":"gemma3:12b","prompt":"Hello","stream":false}'
```

### Test Without AI
```properties
ollama.enabled=false
```
Reports will generate with "AI Analysis Unavailable" message.

### Debug Mode
```properties
logging.level.com.eginnovations.support.pmr=DEBUG
```

## Common Tasks

### Add More KPIs
1. Get JSON filename suffix from ZIP (e.g., `Disk-Activity_Disk-busy.json`)
2. Add to `fileCategoryMapping.properties`
3. Ensure matching `eghelp/<test>.json` exists
4. Rebuild and run

### Change AI Model
```properties
# Smaller/faster
ollama.model=gemma:7b

# Larger/better
ollama.model=llama3:70b
```

### Adjust Data Truncation
Edit `PreventiveMaintenanceService.generateAIAnalysis()`:
```java
if (dataJson.length() > 5000) { // Change from 3000
    dataJson = dataJson.substring(0, 5000) + "\n... (truncated)";
}
```

### Custom Compliance Logic
Edit `PreventiveMaintenanceService.determineComplianceStatus()`:
```java
if (upperAnalysis.contains("YOUR_KEYWORD")) {
    result.setComplianceStatus("YOUR_STATUS");
}
```

## Performance Tips

1. **Parallel Processing**: Process multiple ZIP files in parallel (future enhancement)
2. **Model Selection**: Balance speed vs accuracy
   - Fast: `gemma:7b` (~2-3 sec/KPI)
   - Balanced: `gemma3:12b` (~5-8 sec/KPI)
   - Accurate: `llama3:70b` (~20-30 sec/KPI)
3. **Data Truncation**: Limit historical data to reduce token usage
4. **Caching**: Cache eghelp lookups (already implemented)

## Error Handling

All errors are logged and processing continues:
- **ZIP read error**: Skip to next ZIP
- **JSON parse error**: Skip to next entry
- **Help file missing**: Continue without interpretation
- **Ollama error**: Report "AI analysis unavailable"

## Integration Points

### With Existing Reports
```java
// After alarm analysis
if (prepare.kpi.compliance.report) {
    // Run KPI analysis
}
```

### With External Systems
Export to JSON for integration:
```java
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(results);
```

## Developer Notes

- Uses Jackson for JSON parsing with case-insensitive properties
- Spring Boot for dependency injection
- Apache HttpClient 5 for Ollama API calls
- Bootstrap 5 for responsive HTML reports
- SLF4J for logging

## Next Steps

1. Review generated report HTML structure
2. Customize CSS to match your branding
3. Add more KPIs to `fileCategoryMapping.properties`
4. Fine-tune Ollama model and parameters
5. Set up scheduled analysis runs
6. Integrate with monitoring dashboards

## Support Resources

- Full Documentation: `KPI_COMPLIANCE_README.md`
- Sample Files: `src/main/resources/`
- Logs: `logs/` directory
- Ollama Docs: https://ollama.ai/docs

---

**Remember**: This is on-premise AI - your data never leaves your infrastructure! 🔒
