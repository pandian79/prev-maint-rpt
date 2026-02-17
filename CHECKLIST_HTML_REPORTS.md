# HTML Report Generation - Pre-Flight Checklist

## ✅ Before Running the Application

### 1. Install Ollama (Required for AI Analysis)

- [ ] Download from https://ollama.ai/download
- [ ] Install Ollama
- [ ] Start Ollama service: `ollama serve`
- [ ] Keep Ollama terminal window open

### 2. Download AI Model

- [ ] Open new terminal
- [ ] Run: `ollama pull llama2` (downloads ~4GB)
- [ ] Wait for download to complete (~5-10 minutes)

### 3. Verify Ollama is Running

- [ ] Test command: `curl http://localhost:11434/api/tags`
- [ ] Should see list of installed models including "llama2"

### 4. Check Application Configuration

- [ ] Open `application.properties`
- [ ] Verify: `ollama.enabled=true`
- [ ] Verify: `ollama.api.url=http://localhost:11434/api/generate`
- [ ] Verify: `ollama.model=llama2`

### 5. Prepare Help Files (Optional)

- [ ] Check `src/main/resources/eghelp/` for test JSON files
- [ ] Add custom test interpretations if needed
- [ ] Format: `<TestName>.json` with measure details

## 🚀 Running the Application

### 6. Start Application

- [ ] Open terminal/command prompt
- [ ] Navigate to project directory
- [ ] Run: `java -jar target/prev-maint-rpt-1.jar https://YOUR_EG_MANAGER admin`
- [ ] Enter password when prompted

### 7. Monitor Progress

- [ ] Watch console output for progress messages
- [ ] Look for: "Validating credentials..."
- [ ] Look for: "Fetching alarm history..."
- [ ] Look for: "Processing alarms..."
- [ ] Look for: "Generating HTML reports..."

### 8. Expected Output Files

After completion, verify these files exist:

- [ ] `alarm_analysis_all-alarms_YYYY-MM-DD_HHMMSS.zip`
- [ ] `alarm_analysis_all-alarms_YYYY-MM-DD_HHMMSS.html` ← **New!**
- [ ] `eg_preventive_maintenance_YYYY-MM-DD_HHMMSS.zip`

## 📊 Verify HTML Report

### 9. Open Report

- [ ] Double-click the `.html` file
- [ ] Opens in default web browser
- [ ] No web server required

### 10. Check Report Contents

- [ ] Header shows: "eG Alarm Analysis Report"
- [ ] Report date displayed
- [ ] Source ZIP filename shown
- [ ] Alarm cards present (if alarms exist)

### 11. Verify Each Alarm Card

For each alarm, check:

- [ ] Component name and measure visible
- [ ] Repeat count badge (if repeated)
- [ ] Details table with all alarm info
- [ ] Metric graph image (if available)
- [ ] Measure interpretation section
- [ ] AI-powered analysis section

### 12. Check Footer

- [ ] Yellow caveat box present
- [ ] "On-premise AI" notice visible
- [ ] Blue footer with copyright
- [ ] eG Innovations website link

## 🔍 Troubleshooting Checks

### If "AI analysis unavailable" appears:

- [ ] Verify Ollama is running: `ollama serve`
- [ ] Check Ollama URL in `application.properties`
- [ ] Test connection: `curl http://localhost:11434/api/tags`
- [ ] Check logs for Ollama connection errors

### If "No interpretation data available":

- [ ] Check if help file exists: `src/main/resources/eghelp/<TestName>.json`
- [ ] Verify JSON format is correct
- [ ] Check test name matches exactly (case-sensitive)

### If HTML file not generated:

- [ ] Check if alarm ZIP file exists
- [ ] Check console for error messages
- [ ] Verify write permissions in current directory
- [ ] Check logs for exceptions

### If report looks broken:

- [ ] Check if Bootstrap CSS loaded (requires internet for CDN)
- [ ] Try different browser
- [ ] Check browser console for errors (F12)

## ⚡ Quick Checks

### Ollama Status
```cmd
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Check if model is loaded
ollama list
```

### Expected: Should see llama2 in list

### File Permissions
```cmd
# Verify write permissions
dir alarm_analysis*.html

# Should show file created today
```

### Port Conflicts
```cmd
# Check if port 11434 is in use
netstat -an | findstr "11434"

# Should show LISTENING
```

## 📝 Common Configurations

### Configuration 1: Disable AI (Fastest)
```properties
ollama.enabled=false
```
- Reports generate without AI section
- Faster processing
- No Ollama needed

### Configuration 2: Better AI (Slower)
```properties
ollama.model=llama3
ollama.temperature=0.5
ollama.max_tokens=3000
```
- Download: `ollama pull llama3`
- More detailed analysis
- Longer processing time

### Configuration 3: Custom Ollama URL
```properties
ollama.api.url=http://192.168.1.100:11434/api/generate
```
- Use Ollama on different machine
- Faster if that machine has GPU

## 🎯 Success Indicators

✅ **Everything is working if:**

1. Console shows "HTML reports generated successfully"
2. HTML file exists with same name as ZIP file
3. HTML opens in browser without errors
4. All alarm cards display correctly
5. AI analysis sections show detailed text (not errors)
6. Images display (if present in alarm data)
7. Footer and caveat are visible
8. No red error messages in console

## 🆘 Quick Fixes

### Problem: Slow Report Generation
**Solutions:**
- Use smaller model: `ollama.model=llama2`
- Reduce tokens: `ollama.max_tokens=1000`
- Disable AI: `ollama.enabled=false`

### Problem: Out of Memory
**Solutions:**
- Increase heap: `java -Xmx4g -jar ...`
- Process fewer alarms
- Use smaller AI model

### Problem: Connection Timeout
**Solutions:**
- Increase timeout in OllamaService
- Check network connection
- Restart Ollama: Stop and `ollama serve`

## 📞 Getting Help

If issues persist after checklist:

1. Check `IMPLEMENTATION_SUMMARY.md` for technical details
2. Read `HTML_REPORT_README.md` for comprehensive guide
3. Review `QUICKSTART_HTML_REPORTS.md` for setup steps
4. Check application logs for detailed error messages
5. Verify all prerequisites are met

## ✨ Ready to Go!

If all checks pass, you're ready to generate professional AI-powered alarm reports!

**Command:**
```cmd
ollama serve
# (in new terminal)
java -jar target/prev-maint-rpt-1.jar https://your-egmanager.com admin
```

**Expected time:**
- Small dataset (< 100 alarms): 5-10 minutes
- Medium dataset (100-500 alarms): 10-30 minutes
- Large dataset (> 500 alarms): 30+ minutes

**Pro tip:** Keep Ollama running between multiple report generations for faster processing!
