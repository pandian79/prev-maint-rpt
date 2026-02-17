# Quick Start Guide - HTML Alarm Reports

## Prerequisites Setup (One-time)

### Step 1: Install Ollama (AI Engine)

1. Download Ollama from: https://ollama.ai/download
2. Install and start Ollama:
   ```cmd
   ollama serve
   ```
   Keep this terminal window open.

3. Download an AI model (in a new terminal):
   ```cmd
   ollama pull llama2
   ```
   This downloads ~4GB, takes 5-10 minutes depending on internet speed.

### Step 2: Verify Ollama is Running

```cmd
curl http://localhost:11434/api/tags
```

Should return a list of installed models.

## Running the Application

### Standard Run (with AI analysis)

```cmd
java -jar prev-maint-rpt.jar https://your-egmanager.com admin
```

The application will:
1. ✓ Connect to eG Manager
2. ✓ Extract alarm data → Creates `alarm_analysis_*.zip`
3. ✓ Generate preventive maintenance data → Creates `eg_preventive_maintenance_*.zip`
4. ✓ Generate HTML reports → Creates `alarm_analysis_*.html`

### Quick Run (without AI analysis)

If Ollama is not available or you want faster processing:

1. Edit `application.properties`:
   ```properties
   ollama.enabled=false
   ```

2. Run normally:
   ```cmd
   java -jar prev-maint-rpt.jar https://your-egmanager.com admin
   ```

Reports generate without AI analysis section (still includes all other data).

## What You Get

### Input (automatically created)
- `alarm_analysis_all-alarms_2026-02-15_080122.zip`

### Output (automatically generated)
- `alarm_analysis_all-alarms_2026-02-15_080122.html`

## Opening the Report

Simply double-click the `.html` file - it opens in your default browser.

No web server needed - it's a standalone HTML file.

## Report Contents

Each alarm in the report shows:

✓ Component and measure details  
✓ Priority and timing information  
✓ Visual metric graph  
✓ Measure interpretation from eG docs  
✓ AI-powered analysis with:
  - Alert interpretation
  - Root cause analysis
  - Impact assessment
  - Remediation steps

## Common Issues

### "AI analysis unavailable"

**Cause**: Ollama not running

**Fix**:
```cmd
ollama serve
```

### "Connection error"

**Cause**: Ollama on different port

**Fix**: Update `application.properties`:
```properties
ollama.api.url=http://localhost:YOUR_PORT/api/generate
```

### "Model not found"

**Cause**: AI model not downloaded

**Fix**:
```cmd
ollama pull llama2
```

### Slow performance

**Fix**: Use smaller model or disable AI
```properties
ollama.enabled=false
```

## AI Model Recommendations

| Model | Size | Speed | Accuracy | Use Case |
|-------|------|-------|----------|----------|
| llama2 | 4GB | Fast | Good | Development, Quick Analysis |
| llama3 | 8GB | Medium | Better | Production, Detailed Analysis |
| mistral | 7GB | Medium | Best | Critical Issues, Root Cause |

Download additional models:
```cmd
ollama pull llama3
ollama pull mistral
```

Change model in `application.properties`:
```properties
ollama.model=llama3
```

## Sharing Reports

The HTML file is **self-contained** and can be:
- ✓ Emailed
- ✓ Copied to USB drive
- ✓ Uploaded to SharePoint/Google Drive
- ✓ Archived for compliance

All images are embedded (base64), no external dependencies.

## Privacy Notice

All AI processing happens **on your local machine**:
- ✗ No data sent to cloud
- ✗ No internet required for AI
- ✗ No external API calls
- ✓ Complete data privacy
- ✓ Suitable for sensitive environments

## Example Workflow

```cmd
# Terminal 1 - Start Ollama (keep running)
ollama serve

# Terminal 2 - Run application
cd E:\eGCRM\crm-workspace\prev-maint-rpt
java -jar target\prev-maint-rpt-1.jar https://egmanager.local admin

# Enter password when prompted
# Wait for processing (5-15 minutes depending on alarm count)
# Open generated HTML file in browser
```

## Tips

1. **First Run**: May take longer as AI model loads into memory
2. **Subsequent Runs**: Faster as model stays in memory
3. **Large Datasets**: Consider disabling AI or using smaller model
4. **Multiple Reports**: All alarm_analysis_*.zip files are processed automatically

## Need Help?

Check the detailed documentation:
- `HTML_REPORT_README.md` - Comprehensive guide
- `application.properties` - Configuration options
- Logs in console output

## Advanced: Custom Help Files

Add custom measure interpretations:

1. Create file: `src/main/resources/eghelp/<TestName>.json`
2. Format:
   ```json
   [
     {
       "Measurement": "Measure Name",
       "Description": "What it measures",
       "MeasurementUnit": "Unit",
       "Interpretation": "How to interpret values"
     }
   ]
   ```
3. Rebuild and run

The report automatically picks up the interpretation!
