# Thymeleaf Refactoring - HTML Report Service

## 🎉 Major Improvement: Separation of Concerns

The HTML Report Service has been refactored to use **Thymeleaf** templating engine, dramatically improving maintainability and flexibility.

---

## 📋 What Changed

### Before (Java-based HTML Generation)
- ❌ HTML code embedded in Java strings
- ❌ 200+ lines of StringBuilder operations
- ❌ Difficult to maintain and modify styling
- ❌ Hard to preview changes
- ❌ Mix of business logic and presentation

### After (Thymeleaf Template-based)
- ✅ Clean separation of concerns
- ✅ HTML in proper template files
- ✅ Easy to maintain and update
- ✅ Preview in browser with dummy data
- ✅ Business logic stays in Java, presentation in HTML

---

## 🗂️ New File Structure

### 1. **Thymeleaf Template**
**Location**: `src/main/resources/templates/alarm-report.html`

Professional HTML5 template with:
- Bootstrap 5.3 styling
- eG Innovations branding
- Thymeleaf expressions for dynamic content
- Proper semantic HTML

### 2. **Model Class**
**Location**: `src/main/java/com/eginnovations/support/pmr/model/AlarmReportData.java`

Clean Java model representing alarm data:
```java
public class AlarmReportData {
    private String componentName;
    private String componentType;
    private String test;
    private String measure;
    private String priority;
    // ... and more
}
```

### 3. **Refactored Service**
**Location**: `src/main/java/com/eginnovations/support/pmr/HtmlReportService.java`

Simplified service that:
- Collects alarm data
- Populates model objects
- Renders template via Thymeleaf

---

## 🎨 Template Features

### Thymeleaf Expressions

**Variables:**
```html
<span th:text="${alarm.componentName}">Component Name</span>
```

**Conditionals:**
```html
<div th:if="${alarm.repeatCount > 1}">
    <span th:text="'Repeated ' + ${alarm.repeatCount} + ' times'"></span>
</div>
```

**Iteration:**
```html
<div th:each="alarm, iterStat : ${alarms}">
    <!-- Alarm card content -->
</div>
```

**Dynamic Classes:**
```html
<div th:class="'card alarm-card priority-' + ${#strings.toLowerCase(alarm.priority)}">
```

**Unescaped HTML (for AI analysis):**
```html
<div th:utext="${alarm.aiAnalysis}"></div>
```

---

## 🔧 How It Works

### Step 1: Prepare Data
```java
List<AlarmReportData> alarms = new ArrayList<>();
// ... populate alarms from ZIP file
```

### Step 2: Create Thymeleaf Context
```java
Context context = new Context();
context.setVariable("zipFileName", zipFile.getName());
context.setVariable("reportDate", reportDate);
context.setVariable("currentYear", 2026);
context.setVariable("alarms", alarms);
```

### Step 3: Process Template
```java
String htmlContent = templateEngine.process("alarm-report", context);
```

### Step 4: Write to File
```java
try (FileWriter writer = new FileWriter(htmlPath.toFile(), StandardCharsets.UTF_8)) {
    writer.write(htmlContent);
}
```

---

## ✨ Benefits

### 1. **Maintainability**
- Change styling in HTML file (no Java recompilation)
- Easy to understand HTML structure
- Designer-friendly format

### 2. **Flexibility**
- Add new fields: Just update model and template
- Change layout: Edit HTML, not Java strings
- A/B testing: Create multiple templates

### 3. **Testability**
- Unit test Java logic separately
- Preview templates with mock data
- Validate HTML with standard tools

### 4. **Performance**
- Thymeleaf caches templates
- No string concatenation overhead
- Efficient rendering

### 5. **Developer Experience**
- IDE autocomplete in templates
- Syntax highlighting
- HTML validation
- Easier debugging

---

## 📝 Template Customization

### Change Colors
Edit `alarm-report.html` CSS:
```html
<style>
    .header-section { 
        background: linear-gradient(135deg, #YOUR_COLOR_1 0%, #YOUR_COLOR_2 100%); 
    }
</style>
```

### Add New Field
1. Add property to `AlarmReportData.java`:
   ```java
   private String newField;
   public String getNewField() { return newField; }
   public void setNewField(String newField) { this.newField = newField; }
   ```

2. Populate in `HtmlReportService.java`:
   ```java
   alarmReport.setNewField(representativeAlert.get("newField").asText());
   ```

3. Display in `alarm-report.html`:
   ```html
   <tr>
       <td>New Field:</td>
       <td th:text="${alarm.newField}">Default Value</td>
   </tr>
   ```

### Change Layout
Edit `alarm-report.html` structure:
```html
<!-- Change from cards to tables, lists, or any layout -->
<div class="alarm-list">
    <div th:each="alarm : ${alarms}" class="alarm-item">
        <!-- Your custom layout -->
    </div>
</div>
```

---

## 🚀 Usage (No Changes)

The API remains the same:
```java
@Autowired
private HtmlReportService htmlReportService;

// Generate reports
htmlReportService.generateHtmlReports();
```

---

## 📦 Dependencies

### Added to pom.xml:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

Thymeleaf is automatically configured by Spring Boot.

---

## 🎓 Thymeleaf Quick Reference

### Common Expressions

| Expression | Description | Example |
|------------|-------------|---------|
| `${var}` | Variable | `${alarm.componentName}` |
| `*{field}` | Selection | `*{componentName}` (within object) |
| `#{msg}` | Message | `#{welcome.message}` |
| `@{url}` | URL | `@{/css/style.css}` |
| `~{template}` | Fragment | `~{fragments/header}` |

### Conditionals
```html
<div th:if="${condition}">Shown if true</div>
<div th:unless="${condition}">Shown if false</div>
<div th:switch="${value}">
    <span th:case="'value1'">Case 1</span>
    <span th:case="'value2'">Case 2</span>
</div>
```

### Loops
```html
<div th:each="item : ${items}">
    <span th:text="${item}"></span>
</div>

<div th:each="item, iterStat : ${items}">
    Index: <span th:text="${iterStat.index}"></span>
    Count: <span th:text="${iterStat.count}"></span>
</div>
```

### Utility Objects
```html
<!-- Strings -->
${#strings.toUpperCase(text)}
${#strings.toLowerCase(text)}
${#strings.isEmpty(text)}

<!-- Lists -->
${#lists.size(list)}
${#lists.isEmpty(list)}

<!-- Dates -->
${#dates.format(date, 'yyyy-MM-dd')}

<!-- Numbers -->
${#numbers.formatDecimal(num, 1, 2)}
```

---

## 🔍 Debugging

### View Generated HTML
The generated HTML is written to:
```
alarm_analysis_*.html
```

Open in browser to inspect.

### Template Errors
Check console for Thymeleaf parsing errors:
```
org.thymeleaf.exceptions.TemplateProcessingException
```

### Model Data
Add logging in service:
```java
logger.debug("Alarm data: {}", alarmReport);
```

---

## 📚 Further Reading

- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [Spring Boot + Thymeleaf](https://spring.io/guides/gs/serving-web-content/)
- [Thymeleaf Tutorial](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html)

---

## ✅ Migration Checklist

- [x] Add Thymeleaf dependency to pom.xml
- [x] Create template directory structure
- [x] Create alarm-report.html template
- [x] Create AlarmReportData model class
- [x] Refactor HtmlReportService to use Thymeleaf
- [x] Remove old HTML generation methods
- [x] Test with existing ZIP files
- [x] Verify output matches previous format
- [x] Update documentation

---

## 🎊 Result

The HTML Report Service is now:
- ✅ More maintainable
- ✅ More flexible
- ✅ More testable
- ✅ More developer-friendly
- ✅ More designer-friendly
- ✅ Industry standard approach
- ✅ Production ready

**No changes to external API or functionality - just better code architecture!**

---

*Last Updated: February 15, 2026*  
*Version: 2.0 (Thymeleaf Edition)*
