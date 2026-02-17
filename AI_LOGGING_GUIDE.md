# AI Prompt and Response Logging

## Overview

The KPI Compliance Analysis system now automatically saves all AI prompts and responses to log files in the `logs/ai-pm` directory. This feature provides full transparency and auditability of the AI analysis process.

## Log File Structure

### Directory
```
logs/
└── ai-pm/
    ├── eG-Manager_COMMONROSE_Disk-Activity_Disk-busy.prompt.log
    ├── eG-Manager_COMMONROSE_Disk-Activity_Disk-busy.response.log
    ├── eG-Manager_COMMONROSE_Memory-Usage_Memory-utilized.prompt.log
    ├── eG-Manager_COMMONROSE_Memory-Usage_Memory-utilized.response.log
    └── ...
```

### File Naming Convention
- **Prompt logs**: `<entry-name>.prompt.log`
- **Response logs**: `<entry-name>.response.log`

Where `<entry-name>` is derived from the ZIP entry filename:
- Path separators replaced with underscores
- `.json` extension removed
- Special characters sanitized

### Example
For ZIP entry: `eG-Manager_COMMONROSE_Disk-Activity_Disk-busy.json`
- Prompt log: `eG-Manager_COMMONROSE_Disk-Activity_Disk-busy.prompt.log`
- Response log: `eG-Manager_COMMONROSE_Disk-Activity_Disk-busy.response.log`

## Log File Format

### Prompt Log (.prompt.log)
```
================================================================================
AI PROMPT LOG
Entry: eG-Manager_COMMONROSE_Disk-Activity_Disk-busy.json
Timestamp: 2026-02-17T14:25:30.123456
================================================================================

You are an expert system administrator analyzing eG Innovations monitoring data.

KPI INFORMATION:
Component: eG-Manager
Component Type: eG Manager
Test: Disk Activity
Measure: Disk busy
Timeline: Last 7 days

DESCRIPTION:
Indicates the percentage of time the disk was busy...

INTERPRETATION GUIDE:
A high disk busy percentage indicates...

Measurement Unit: Percentage

HISTORICAL DATA:
{
  "2026-02-10 00:00:00": 45.2,
  "2026-02-11 00:00:00": 52.8,
  ...
}

ANALYSIS REQUIRED:
Based on the description, interpretation guide, and historical data provided above, 
analyze whether this KPI is COMPLIANT (healthy) or NON-COMPLIANT (needs attention).
...
```

### Response Log (.response.log)
```
================================================================================
AI RESPONSE LOG
Entry: eG-Manager_COMMONROSE_Disk-Activity_Disk-busy.json
Timestamp: 2026-02-17T14:25:35.789012
================================================================================

STATUS: NON-COMPLIANT

REASON: The historical data shows concerning disk busy percentages over the 
analysis period. Specifically:

1. Average disk busy time: 52.3% (above 50% threshold)
2. Peak values reaching 78% on 2026-02-13
3. Sustained high usage over 3 consecutive days

Key concerns:
- Disk bottleneck could impact system performance
- Risk of I/O delays affecting user operations
- Potential storage capacity planning needed

Recommendations:
- Investigate processes causing high disk I/O
- Consider disk upgrade or redistribution of workload
- Monitor closely for further degradation
```

## Benefits

### 1. Full Transparency
- Every prompt sent to Ollama is recorded
- Every response received is preserved
- Complete audit trail for compliance

### 2. Troubleshooting
- Review prompts to understand AI input
- Analyze responses to debug incorrect assessments
- Identify patterns in AI behavior

### 3. Quality Assurance
- Verify prompts contain correct information
- Ensure responses are properly formatted
- Validate compliance determination logic

### 4. Continuous Improvement
- Study successful analyses
- Identify prompt improvements
- Refine AI instructions based on results

### 5. Documentation
- Reference logs when explaining results to stakeholders
- Provide evidence for compliance decisions
- Support incident post-mortems

## Usage

### Automatic Logging
Logging happens automatically during KPI analysis. No configuration required!

When you run:
```bash
java -jar prev-maint-rpt.jar
```

The system will:
1. Create `logs/ai-pm/` directory if it doesn't exist
2. Save prompt before sending to Ollama
3. Save response after receiving from Ollama
4. Continue processing (logging errors don't stop analysis)

### Accessing Logs

#### View All Logs
```bash
cd logs/ai-pm
dir
```

#### View a Specific Prompt
```bash
type eG-Manager_COMMONROSE_Disk-Activity_Disk-busy.prompt.log
```

#### View a Specific Response
```bash
type eG-Manager_COMMONROSE_Disk-Activity_Disk-busy.response.log
```

#### Search Logs
```bash
# Find all non-compliant responses
findstr /M "NON-COMPLIANT" *.response.log

# Find prompts mentioning specific component
findstr /M "eG-Manager" *.prompt.log
```

### Log Analysis

#### Count Total Analyses
```bash
# Windows
dir *.prompt.log | find /c ".log"

# Linux/Mac
ls -1 *.prompt.log | wc -l
```

#### Find Patterns
```bash
# Find all non-compliant assessments
findstr "STATUS: NON-COMPLIANT" *.response.log

# Find specific KPI analyses
findstr "Measure: Not running licensed agents" *.prompt.log
```

## Log Maintenance

### Disk Space Considerations
- Each prompt log: ~1-3 KB
- Each response log: ~0.5-2 KB
- For 100 KPIs: ~200-500 KB total

### Cleanup Strategy

#### Archive Old Logs
```bash
# Create archive directory
mkdir logs\ai-pm\archive

# Move logs older than 30 days
forfiles /P logs\ai-pm /M *.log /D -30 /C "cmd /c move @file logs\ai-pm\archive"
```

#### Compress Archives
```bash
# Create ZIP of archived logs
powershell Compress-Archive -Path logs\ai-pm\archive\*.log -DestinationPath logs\ai-pm\archive\ai-logs-2026-01.zip
```

#### Delete Very Old Logs
```bash
# Delete logs older than 90 days from archive
forfiles /P logs\ai-pm\archive /M *.log /D -90 /C "cmd /c del @file"
```

### Recommended Retention

| Period | Action |
|--------|--------|
| 0-7 days | Keep in active directory |
| 7-30 days | Keep for recent reference |
| 30-90 days | Archive to subdirectory |
| 90+ days | Compress and/or delete |

## Troubleshooting

### Logs Not Being Created

**Check 1**: Directory permissions
```bash
# Verify logs/ai-pm exists and is writable
mkdir logs\ai-pm
```

**Check 2**: Application logs
```bash
# Look for errors in application logs
type logs\application.log | findstr "ai-pm"
```

**Check 3**: Disk space
```bash
# Verify sufficient disk space
dir
```

### Incomplete Log Files

**Possible causes:**
1. Application crashed during analysis
2. Disk full during write
3. Permissions issue mid-write

**Solution**: Re-run analysis

### Log Files Too Large

**Adjust data truncation** in `PreventiveMaintenanceService.java`:
```java
// Reduce from 3000 to 1500 characters
if (dataJson.length() > 1500) {
    dataJson = dataJson.substring(0, 1500) + "\n... (truncated)";
}
```

## Security Considerations

### Data Sensitivity
- Logs contain KPI data from your environment
- May include component names, measurements, values
- Treat logs with same security as eG Manager data

### Access Control
- Restrict access to `logs/ai-pm/` directory
- Set appropriate file permissions
- Consider encryption for long-term archives

### Compliance
- Logs support audit requirements
- Demonstrate AI decision transparency
- Provide evidence for compliance reviews

## Advanced Usage

### Log Analysis Scripts

#### PowerShell: Summary Report
```powershell
# Count compliant vs non-compliant
$compliant = (Select-String -Path "logs\ai-pm\*.response.log" -Pattern "STATUS: COMPLIANT").Count
$nonCompliant = (Select-String -Path "logs\ai-pm\*.response.log" -Pattern "STATUS: NON-COMPLIANT").Count

Write-Host "Compliant: $compliant"
Write-Host "Non-Compliant: $nonCompliant"
```

#### Python: Parse Logs
```python
import os
import re

def parse_response_log(file_path):
    with open(file_path, 'r') as f:
        content = f.read()
        status_match = re.search(r'STATUS: (\w+)', content)
        return status_match.group(1) if status_match else 'UNKNOWN'

# Analyze all response logs
results = {}
for file in os.listdir('logs/ai-pm'):
    if file.endswith('.response.log'):
        status = parse_response_log(os.path.join('logs/ai-pm', file))
        results[file] = status

print(results)
```

### Integration with Monitoring

#### Alert on Excessive Non-Compliance
```bash
# Count non-compliant responses
$count = (Select-String -Path "logs\ai-pm\*.response.log" -Pattern "STATUS: NON-COMPLIANT").Count

# Alert if more than 50% are non-compliant
$total = (Get-ChildItem logs\ai-pm\*.response.log).Count
if (($count / $total) -gt 0.5) {
    Write-Warning "High non-compliance rate: $count / $total"
    # Send alert to monitoring system
}
```

## Implementation Details

### Code Location
File: `PreventiveMaintenanceService.java`

**Key Methods:**
- `initializeAILogsDirectory()` - Creates log directory
- `savePromptToLog()` - Saves prompt to file
- `saveResponseToLog()` - Saves response to file
- `getLogFileName()` - Generates sanitized filename

**Logging Flow:**
```
generateAIAnalysis()
    ↓
savePromptToLog() → logs/ai-pm/<entry>.prompt.log
    ↓
ollamaService.generateResponse()
    ↓
saveResponseToLog() → logs/ai-pm/<entry>.response.log
    ↓
return response
```

### Error Handling
- Logging errors are caught and logged to application log
- Analysis continues even if logging fails
- Non-blocking I/O for minimal performance impact

## Best Practices

1. **Regular Review**: Periodically review logs for quality assurance
2. **Archive Regularly**: Keep active directory small for performance
3. **Monitor Disk Space**: Set up alerts for disk usage
4. **Secure Access**: Restrict log directory permissions
5. **Use for Training**: Study logs to improve prompt engineering

## Summary

The AI logging feature provides complete transparency into the KPI compliance analysis process. Every interaction with Ollama is recorded, enabling:

- ✅ Full audit trail
- ✅ Troubleshooting support
- ✅ Quality assurance
- ✅ Continuous improvement
- ✅ Compliance documentation

Logs are automatically created in `logs/ai-pm/` with no additional configuration required!

---

**Location**: `logs/ai-pm/`  
**Format**: Plain text  
**Naming**: `<entry-name>.[prompt|response].log`  
**Retention**: Configurable based on your needs
