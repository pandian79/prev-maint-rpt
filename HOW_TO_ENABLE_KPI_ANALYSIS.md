# How to Enable and Use KPI Compliance Analysis

## Step-by-Step Activation Guide

### Step 1: Install Ollama (One-Time Setup)

#### On Windows:
1. Download Ollama from: https://ollama.ai/download
2. Run the installer (OllamaSetup.exe)
3. Installation will complete automatically
4. Ollama will start as a Windows service

#### Verify Installation:
```cmd
ollama --version
```
You should see: `ollama version 0.x.x` (or similar)

### Step 2: Download an AI Model (One-Time Setup)

Open Command Prompt or PowerShell:

```cmd
# Recommended model (balanced speed and accuracy)
ollama pull gemma3:12b

# Alternative: Faster but less accurate
ollama pull gemma:7b

# Alternative: More accurate but slower
ollama pull llama3:70b
```

**Wait for download to complete** (models are 7GB-40GB)

Verify the model is available:
```cmd
ollama list
```

### Step 3: Test Ollama

```cmd
ollama run gemma3:12b "What is 2+2?"
```

You should get a response like "4" or "The answer is 4."

Press `Ctrl+D` or type `/bye` to exit the chat.

### Step 4: Configure the Application

Edit `src\main\resources\application.properties`:

```properties
# Change this from false to true
prepare.kpi.compliance.report=true

# Verify Ollama settings (should already be correct)
ollama.enabled=true
ollama.api.url=http://localhost:11434/api/generate
ollama.model=gemma3:12b
ollama.temperature=0.7
ollama.max_tokens=2000
```

### Step 5: Configure KPIs to Analyze

Edit `src\main\resources\fileCategoryMapping.properties`:

```properties
# Add the KPIs you want to analyze
# Format: filename_suffix=category

# Example: Monitor eG Agents
eG-Agents_Not-installed-licensed-agents.json=quality
eG-Agents_Not-running-licensed-agents.json=quality

# Example: Monitor Disk Performance
Disk-Activity_Disk-busy.json=performance

# Example: Monitor Memory
Memory-Usage_Memory-utilized.json=performance
Memory-Exhaustion_Non-paged-pool-usage.json=performance
```

**How to find the correct filename suffix:**
1. Extract your `eg_preventive_maintenance_*.zip` file
2. Look at the JSON filenames
3. Use the entire filename as the key in properties file

### Step 6: Prepare Your Data

1. **Obtain the ZIP file** from eG Enterprise:
   - Navigate to eG Manager
   - Go to Preventive Maintenance Report
   - Download the ZIP file (e.g., `eg_preventive_maintenance_2026-02-15_080507.zip`)

2. **Place the ZIP file** in your application directory:
   ```
   E:\eGCRM\crm-workspace\prev-maint-rpt\
   └── eg_preventive_maintenance_2026-02-15_080507.zip
   ```

### Step 7: Build the Application (If Not Already Built)

```cmd
cd E:\eGCRM\crm-workspace\prev-maint-rpt
mvn clean package -DskipTests
```

The JAR file will be in: `target\prev-maint-rpt-1.jar`

### Step 8: Run the Application

#### Option A: Standalone KPI Analysis (Recommended for First Time)

Set only KPI analysis to true:
```properties
prepare.json.preventive.maintenance=false
prepare.json.alarm.analysis=false
prepare.alarm.analysis.report=false
prepare.kpi.compliance.report=true
```

Run without arguments:
```cmd
cd E:\eGCRM\crm-workspace\prev-maint-rpt
java -jar target\prev-maint-rpt-1.jar
```

#### Option B: Full Report (Including eG Manager Connection)

Keep all enabled and run with eG Manager credentials:
```cmd
cd E:\eGCRM\crm-workspace\prev-maint-rpt
java -jar target\prev-maint-rpt-1.jar http://your-eg-manager:8080 admin
```
Enter password when prompted.

### Step 9: Monitor Progress

You'll see output like:

```
=== Starting KPI Compliance Analysis ===
Found 1 preventive maintenance ZIP file(s)

Processing: eg_preventive_maintenance_2026-02-15_080507.zip
Processing entry: eG-Agents_Not-running-licensed-agents.json
Sending prompt to Ollama...
Processing entry: eG-Agents_Not-installed-licensed-agents.json
Sending prompt to Ollama...
Analyzed 2 KPI(s)
✓ KPI Compliance Report generated: kpi_compliance_report_2026-02-17_142530.html

=== KPI Compliance Analysis Complete ===
```

**Note**: Each KPI takes 5-30 seconds to analyze depending on your model choice.

### Step 10: View the Report

1. **Find the report**:
   ```
   E:\eGCRM\crm-workspace\prev-maint-rpt\
   └── kpi_compliance_report_2026-02-17_142530.html
   ```

2. **Open in browser**:
   - Right-click the HTML file
   - Open with your preferred browser (Chrome, Firefox, Edge)

3. **Review the results**:
   - Check the summary dashboard
   - Review each KPI card
   - Read the AI analysis
   - Note any NON-COMPLIANT items

## Quick Troubleshooting

### Problem: "Ollama connection failed"

**Check 1**: Is Ollama running?
```cmd
curl http://localhost:11434/api/tags
```
Should return JSON with model list.

**Fix**: Start Ollama service
```cmd
# On Windows, Ollama should auto-start
# If not, run:
ollama serve
```

**Check 2**: Is the URL correct?
```properties
# In application.properties
ollama.api.url=http://localhost:11434/api/generate
```

### Problem: "No preventive maintenance ZIP files found"

**Check**: Are ZIP files in the correct directory?
```cmd
dir eg_preventive_maintenance*.zip
```

**Fix**: Copy ZIP files to the directory where you run the application:
```cmd
copy "C:\Downloads\eg_preventive_maintenance_*.zip" E:\eGCRM\crm-workspace\prev-maint-rpt\
```

### Problem: "Analyzed 0 KPI(s)"

**Check**: Do ZIP entries match fileCategoryMapping.properties?

**Debug Steps**:
1. Extract the ZIP file manually
2. Look at the JSON filenames
3. Add matching entries to `fileCategoryMapping.properties`

Example ZIP contents:
```
eg_preventive_maintenance_2026-02-15_080507/
├── eG-Agents_Not-running-licensed-agents.json  ← Matches property file
├── eG-Agents_Not-installed-licensed-agents.json  ← Matches property file
├── Disk-Activity_Disk-busy.json  ← NOT in property file (will be skipped)
└── Memory-Usage_Memory-utilized.json  ← NOT in property file (will be skipped)
```

### Problem: "Help file not found: eghelp/XYZ.json"

**Check**: Does the help file exist?
```cmd
dir src\main\resources\eghelp\*.json
```

**Fix Option 1**: Create the missing help file
1. Copy an existing help file as a template
2. Rename it to match the test name
3. Update the measurements

**Fix Option 2**: Analysis will continue without interpretation guide
- The KPI will still be analyzed, but without the detailed interpretation
- AI will work with just the historical data

### Problem: Poor AI Analysis Quality

**Solution 1**: Use a larger model
```cmd
ollama pull llama3:70b
```
Then update application.properties:
```properties
ollama.model=llama3:70b
```

**Solution 2**: Adjust temperature (lower = more consistent)
```properties
ollama.temperature=0.3
```

**Solution 3**: Increase detail
```properties
ollama.max_tokens=4000
```

## Configuration Matrix

| Scenario | prepare.json.preventive.maintenance | prepare.json.alarm.analysis | prepare.alarm.analysis.report | prepare.kpi.compliance.report | Requires eG Manager? |
|----------|-------------------------------------|----------------------------|------------------------------|------------------------------|----------------------|
| Only KPI Analysis | false | false | false | **true** | ❌ No |
| Only Alarm Reports | false | false | **true** | false | ❌ No |
| Everything | true | true | true | **true** | ✅ Yes |
| Generate new data + analyze | true | true | true | **true** | ✅ Yes |

## Performance Expectations

| Model | Speed per KPI | Accuracy | Memory Usage |
|-------|---------------|----------|--------------|
| gemma:7b | 2-3 seconds | Good | 4GB RAM |
| gemma3:12b | 5-8 seconds | Better | 8GB RAM |
| llama3:70b | 20-30 seconds | Best | 40GB RAM |

**For production**: `gemma3:12b` offers the best balance.

## Verification Checklist

Before running in production, verify:

- [ ] Ollama is installed and running
- [ ] Model is downloaded (`ollama list`)
- [ ] `application.properties` has `prepare.kpi.compliance.report=true`
- [ ] `fileCategoryMapping.properties` is configured
- [ ] ZIP file(s) are in the application directory
- [ ] eghelp JSON files exist for your tests
- [ ] Test run completed successfully
- [ ] HTML report was generated
- [ ] Report opens correctly in browser
- [ ] AI analysis makes sense

## Next Steps

After successful setup:

1. **Automate**: Schedule regular analysis runs
2. **Integrate**: Export results to your monitoring dashboard
3. **Customize**: Adjust AI prompts for your environment
4. **Expand**: Add more KPIs to `fileCategoryMapping.properties`
5. **Share**: Distribute reports to your team

## Need Help?

1. **Check logs**: `logs/` directory
2. **Enable debug logging**:
   ```properties
   logging.level.com.eginnovations.support.pmr=DEBUG
   ```
3. **Read full documentation**: `KPI_COMPLIANCE_README.md`
4. **Quick reference**: `KPI_COMPLIANCE_QUICKSTART.md`
5. **Implementation details**: `KPI_COMPLIANCE_IMPLEMENTATION_SUMMARY.md`

---

**Remember**: This is 100% on-premise AI. Your data never leaves your infrastructure! 🔒
