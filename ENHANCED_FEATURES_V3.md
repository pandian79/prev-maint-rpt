# Enhanced HTML Report Service - Version 3.0

## 🎉 Major Enhancements

The HTML Report Service has been significantly enhanced with professional features for better analysis and user experience.

---

## ✨ What's New

### 1. **Official eG Innovations Colors**
Updated priority colors to match eG branding:
- **Critical**: `#cd0f0f` (Red)
- **Major**: `#fa9d1c` (Orange)
- **Minor**: `#ccc100` (Yellow)

### 2. **Smart Priority Sorting**
Alarms are now automatically sorted by priority:
1. **Critical** alarms first (most urgent)
2. **Major** alarms second
3. **Minor** alarms third
4. **Other** alarms last

This helps users focus on the most important issues immediately.

### 3. **Enhanced AI Analysis**
#### Markdown to HTML Conversion
- AI responses are now consistently formatted
- Markdown output from LLM is converted to clean HTML
- No more mixed markup and HTML content
- Professional, consistent presentation

#### Comprehensive Data Analysis
Ollama now analyzes:
- ✅ **Alarm Details** (component, test, measure, priority, etc.)
- ✅ **Interpretation Guide** (from eG help files)
- ✅ **Historical Data** (trends, patterns, anomalies)
- ✅ **Diagnosis Data** (detailed diagnostic information)

### 4. **Complete Logging System**
Every AI interaction is logged for audit and debugging:
- **Prompt Log**: `logs/ai/<filename>.prompt.log`
- **Response Log**: `logs/ai/<filename>.response.log`

Logs include:
- Complete prompt sent to Ollama
- All data included (historical, diagnosis)
- Full AI response
- Timestamp and context

---

## 🎯 Implementation Details

### Priority Sorting

```java
// Priority order mapping
PRIORITY_ORDER.put("critical", 1);
PRIORITY_ORDER.put("major", 2);
PRIORITY_ORDER.put("minor", 3);
PRIORITY_ORDER.put("warning", 4);

// Alarms are sorted after collection
alarms.sort((a1, a2) -> {
    int order1 = PRIORITY_ORDER.getOrDefault(a1.getPriority().toLowerCase(), 999);
    int order2 = PRIORITY_ORDER.getOrDefault(a2.getPriority().toLowerCase(), 999);
    return Integer.compare(order1, order2);
});
```

### Historical Data Integration

When available, historical data is included in the prompt:
```
=== HISTORICAL DATA ===
The following historical trend data is available for this measure:
Data Points: 50
Sample of recent data points:
  - {"timestamp": "2026-02-15 08:00", "value": 85.3}
  - {"timestamp": "2026-02-15 08:05", "value": 87.1}
  ...

IMPORTANT: Analyze the historical trend to identify patterns, anomalies, or changes over time.
```

### Diagnosis Data Integration

When available, diagnosis data is included:
```
=== DIAGNOSIS DATA ===
Detailed diagnostic information:
Diagnosis 1:
{
  "component": "Tomcat",
  "issue": "High memory usage",
  "details": {...}
}

IMPORTANT: Use the diagnosis data to identify specific issues and root causes.
```

### Markdown Conversion

Using CommonMark library for professional conversion:
```java
Parser markdownParser = Parser.builder().build();
HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

Node document = markdownParser.parse(markdownResponse);
String html = htmlRenderer.render(document);
```

Supports:
- Headers (##, ###)
- Bold (**text**)
- Lists (-, *)
- Code blocks (`code`)
- Links
- And more...

### Logging System

Automatic logging to `logs/ai/` directory:
```
logs/
└── ai/
    ├── alarm_001.prompt.log
    ├── alarm_001.response.log
    ├── alarm_002.prompt.log
    ├── alarm_002.response.log
    └── ...
```

Each log file contains:
- Full prompt with all data sections
- Complete AI response
- File-based for easy audit

---

## 📦 Dependencies Added

### CommonMark (Markdown Parser)
```xml
<dependency>
    <groupId>org.commonmark</groupId>
    <artifactId>commonmark</artifactId>
    <version>0.21.0</version>
</dependency>
```

Provides:
- Fast Markdown parsing
- HTML rendering
- Extension support
- Well-tested library

---

## 🎨 Color Reference

### eG Innovations Priority Colors

| Priority | Hex Color | RGB | Usage |
|----------|-----------|-----|-------|
| Critical | #cd0f0f | (205, 15, 15) | Left border of alarm card |
| Major | #fa9d1c | (250, 157, 28) | Left border of alarm card |
| Minor | #ccc100 | (204, 193, 0) | Left border of alarm card |

### Visual Examples

**Critical Alarm Card:**
```
┌─┐ ◄── Red border (#cd0f0f)
│ │ Alarm #1: Database - High CPU Usage [Critical]
│ │ [Alarm details...]
└─┘
```

**Major Alarm Card:**
```
┌─┐ ◄── Orange border (#fa9d1c)
│ │ Alarm #2: Web Server - Memory Warning [Major]
│ │ [Alarm details...]
└─┘
```

**Minor Alarm Card:**
```
┌─┐ ◄── Yellow border (#ccc100)
│ │ Alarm #3: Network - Latency Increase [Minor]
│ │ [Alarm details...]
└─┘
```

---

## 🚀 Enhanced AI Prompts

### Before (Limited Context)
```
=== ALARM DETAILS ===
Component: Tomcat
Test: Application Event Log
Measure: Application errors
...

=== HISTORICAL DATA AVAILABLE ===
Historical trend data is available for analysis.
```

### After (Complete Context)
```
=== ALARM DETAILS ===
Component: Tomcat
Test: Application Event Log
Measure: Application errors
...

=== HISTORICAL DATA ===
Data Points: 50
Sample of recent data points:
  - {"timestamp": "2026-02-15 08:00", "value": 5}
  - {"timestamp": "2026-02-15 08:05", "value": 12}
  - {"timestamp": "2026-02-15 08:10", "value": 23}
  ...and 47 more data points

IMPORTANT: Analyze the historical trend...

=== DIAGNOSIS DATA ===
{
  "errorType": "ApplicationException",
  "stackTrace": "...",
  "frequency": "4 times in 5 minutes"
}

IMPORTANT: Use the diagnosis data to identify...
```

### AI Response Format

**Markdown (from LLM):**
```markdown
## Alert Interpretation
The alarm indicates a **significant increase** in application errors...

## Root Cause Analysis
Based on the diagnosis data:
- Memory leak in the application
- Database connection timeout
- Insufficient heap space

## Remediation Steps
1. Restart the Tomcat instance
2. Increase heap size to 4GB
3. Review recent deployments
```

**HTML (rendered):**
```html
<h2>Alert Interpretation</h2>
<p>The alarm indicates a <strong>significant increase</strong> in application errors...</p>

<h2>Root Cause Analysis</h2>
<p>Based on the diagnosis data:</p>
<ul>
<li>Memory leak in the application</li>
<li>Database connection timeout</li>
<li>Insufficient heap space</li>
</ul>

<h2>Remediation Steps</h2>
<ol>
<li>Restart the Tomcat instance</li>
<li>Increase heap size to 4GB</li>
<li>Review recent deployments</li>
</ol>
```

---

## 🔍 Audit Trail

### Log File Structure

**Prompt Log (`alarm_001.prompt.log`):**
```
You are an expert system performance analyst. Analyze the following alarm data comprehensively:

=== ALARM DETAILS ===
Component: Tomcat:8080
...

=== HISTORICAL DATA ===
[Full historical data]

=== DIAGNOSIS DATA ===
[Full diagnosis data]

=== REQUIRED ANALYSIS ===
...
```

**Response Log (`alarm_001.response.log`):**
```
## Alert Interpretation

The increasing trend in application errors indicates...

## Root Cause Analysis

Analysis of the diagnosis data reveals...
[Complete AI response]
```

### Benefits of Logging

1. **Audit Trail**: Complete record of AI interactions
2. **Debugging**: Identify prompt issues or AI problems
3. **Quality Control**: Review AI analysis quality
4. **Training**: Improve prompts based on responses
5. **Compliance**: Evidence of AI usage and decisions

---

## 📊 Report Flow

### New Processing Flow

```
┌─────────────────────────────────────────────────┐
│ 1. Read alarm_analysis_*.zip                    │
└───────────────┬─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────┐
│ 2. Extract all alarms to list                   │
└───────────────┬─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────┐
│ 3. Sort by priority (Critical → Major → Minor)  │
│    ✓ Critical alarms first                      │
│    ✓ Major alarms second                        │
│    ✓ Minor alarms last                          │
└───────────────┬─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────┐
│ 4. For each alarm:                              │
│    a. Get interpretation from eG help           │
│    b. Build comprehensive prompt with:          │
│       • Alarm details                           │
│       • Historical data (if available)          │
│       • Diagnosis data (if available)           │
│    c. Log prompt to logs/ai/<name>.prompt.log   │
│    d. Get AI analysis from Ollama               │
│    e. Log response to logs/ai/<name>.response   │
│    f. Convert Markdown → HTML                   │
└───────────────┬─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────┐
│ 5. Render HTML using Thymeleaf                  │
│    • Critical alarms at top (red border)        │
│    • Major alarms in middle (orange border)     │
│    • Minor alarms at bottom (yellow border)     │
└───────────────┬─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────┐
│ 6. Generate alarm_analysis_*.html               │
└─────────────────────────────────────────────────┘
```

---

## 🎓 Usage Examples

### Example 1: Critical Alarms First

**Input (ZIP contains):**
1. Minor alarm - Network latency
2. Critical alarm - Database down
3. Major alarm - High memory

**Output (HTML displays):**
1. ❗ Critical alarm - Database down (red border)
2. ⚠️ Major alarm - High memory (orange border)
3. ℹ️ Minor alarm - Network latency (yellow border)

### Example 2: Rich AI Analysis

**With Historical Data:**
```
## Alert Interpretation
Analysis of the last 50 data points shows a 300% increase 
in error rate starting at 08:15 AM, correlating with the 
application deployment.

## Root Cause Analysis
The diagnosis data indicates:
- OutOfMemoryError in thread pool
- Connection pool exhaustion
- 15 concurrent deployment activities detected
...
```

### Example 3: Log Files

After processing `alarm_database_error_001.json`:

**Generated Files:**
- `logs/ai/alarm_database_error_001.prompt.log`
- `logs/ai/alarm_database_error_001.response.log`

Can be reviewed for:
- Prompt optimization
- AI response quality
- Troubleshooting
- Compliance audits

---

## ✅ Quality Improvements

| Aspect | Before | After |
|--------|--------|-------|
| **Color Accuracy** | Generic colors | Official eG colors |
| **Alarm Order** | Random (ZIP order) | Priority-sorted |
| **AI Context** | Limited | Full (historical + diagnosis) |
| **AI Response Format** | Mixed HTML/Markdown | Clean HTML |
| **Audit Trail** | None | Complete logging |
| **Data Usage** | Partial | Comprehensive |
| **User Experience** | Good | Excellent |

---

## 🔧 Configuration

No additional configuration needed. Features work automatically:

- ✅ Colors applied automatically
- ✅ Sorting happens transparently
- ✅ Historical/diagnosis data detected and used
- ✅ Markdown converted automatically
- ✅ Logs created in `logs/ai/` directory

---

## 📝 Log Management

### Log Directory Structure
```
project-root/
├── logs/
│   └── ai/
│       ├── alarm_001.prompt.log
│       ├── alarm_001.response.log
│       ├── alarm_002.prompt.log
│       ├── alarm_002.response.log
│       └── ...
├── alarm_analysis_*.zip
└── alarm_analysis_*.html
```

### Log Retention

Consider adding log rotation:
- Keep logs for compliance period
- Archive old logs
- Implement cleanup policy

### Searching Logs

```bash
# Find all prompts mentioning "database"
grep -r "database" logs/ai/*.prompt.log

# Find all AI responses mentioning "restart"
grep -r "restart" logs/ai/*.response.log

# Count total log files
ls logs/ai/ | wc -l
```

---

## 🎊 Summary

The HTML Report Service now provides:

✅ **Professional Presentation** - Official eG colors  
✅ **Smart Organization** - Priority-based sorting  
✅ **Deep Analysis** - Historical + diagnosis data  
✅ **Consistent Format** - Markdown → HTML conversion  
✅ **Complete Audit Trail** - Comprehensive logging  
✅ **Better User Experience** - Critical alerts first  
✅ **AI Transparency** - Visible prompt/response logs  
✅ **Production Ready** - Robust error handling  

---

**Version**: 3.0  
**Date**: February 16, 2026  
**Status**: Production Ready ✅

Ready to generate premium, audit-ready alarm analysis reports! 🚀
