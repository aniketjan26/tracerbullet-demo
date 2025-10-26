# TracerBullet Demo - Setup Guide

**Quick setup guide for demo participants**

---

## ⏱️ Time Required: 10 minutes

This guide will help you set up everything needed for the TracerBullet demonstration.

---

## 📋 Prerequisites Checklist

### Required (Must Have)

- [ ] **Java 17+** - [Download](https://adoptium.net/)
- [ ] **Maven 3.6+** - [Download](https://maven.apache.org/download.cgi)
- [ ] **Git** - [Download](https://git-scm.com/downloads)
- [ ] **Web Browser** - Chrome, Firefox, Safari, or Edge

### Optional (Nice to Have)

- [ ] **curl** - For making HTTP requests (pre-installed on Mac/Linux)
- [ ] **IDE** - IntelliJ IDEA or VSCode for code exploration

---

## 🚀 Quick Setup (5 Steps)

### Step 1: Verify Prerequisites

Open terminal/command prompt and run:

```bash
java -version
# Should show: openjdk version "17.0.x" or higher

mvn -version
# Should show: Apache Maven 3.6.x or higher

git --version
# Should show: git version x.x.x
```

**✅ All commands work?** Proceed to Step 2.

**❌ Any command fails?** Install missing software first:
- **Java:** https://adoptium.net/
- **Maven:** https://maven.apache.org/install.html
- **Git:** https://git-scm.com/downloads

### Step 2: Clone Repository

```bash
git clone https://github.com/aniketjan26/tracerbullet-demo.git
cd tracerbullet-demo
```

### Step 3: Build and Test

```bash
mvn clean install
```

**This will:**
- ⏳ Download dependencies (first time: ~2 minutes)
- ⏳ Compile code
- ⏳ Run 7 acceptance tests
- ⏳ Generate UML diagrams
- ⏳ Build executable JAR

**Expected output:**
```
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
📊 Generating test report...
✓ Test report generated: diagrams/test-report.html
[INFO] BUILD SUCCESS
```

**⏱️ Time:** 3-5 minutes (first time)

### Step 4: Verify Diagrams

Check that diagrams were generated:

```bash
ls diagrams/
```

**You should see:**
- `test-report.html` ← Main test report
- `test-summary.txt` ← Text summary
- `trace-*.puml` files ← PlantUML diagrams
- `trace-*.txt` files ← Trace summaries

### Step 5: Open Test Report

**macOS:**
```bash
open diagrams/test-report.html
```

**Linux:**
```bash
xdg-open diagrams/test-report.html
```

**Windows:**
```bash
start diagrams/test-report.html
```

**Or:** Open `diagrams/test-report.html` in your browser manually.

**✅ You should see:** A beautiful HTML page with test scenarios and diagrams.

---

## 🎯 You're Ready!

If you can see the test report with diagrams, you're all set for the demo! 🎉

---

## 🔧 Troubleshooting

### Problem: "mvn: command not found"

**Solution:**
1. Install Maven: https://maven.apache.org/install.html
2. Add Maven to your PATH
3. Restart terminal
4. Try again: `mvn -version`

### Problem: "java: command not found"

**Solution:**
1. Install Java 17+: https://adoptium.net/
2. Set JAVA_HOME environment variable
3. Restart terminal
4. Try again: `java -version`

### Problem: "Port already in use" during tests

**Solution:**
1. Kill processes using ports 9080, 9081, 9082
2. Or skip tests: `mvn clean package -DskipTests`

### Problem: Build fails

**Solution:**
1. Check internet connection (Maven needs to download dependencies)
2. Clear Maven cache: `rm -rf ~/.m2/repository`
3. Try again: `mvn clean install`

### Problem: Diagrams not showing in browser

**Solution:**
1. Make sure tests ran successfully (check console output)
2. Verify `diagrams/` folder exists and has .puml files
3. Try a different browser

---

## 📚 Optional: Read Before Demo

For deeper understanding, read the full article:

**[ARTICLE.md](ARTICLE.md)** - Comprehensive guide covering:
- Why distributed tracing matters
- How TracerBullet works
- Real-world use cases
- Detailed technical concepts

**⏱️ Reading time:** 15-20 minutes

---

## 🎪 What to Expect in Demo

### Part 1: Test Report (20 min)
- Review generated test report
- Understand sequence diagrams
- See traces in action

### Part 2: Live Demo (20 min)
- Start microservices
- Make real requests
- Watch traces generate

### Part 3: Code Walkthrough (10 min)
- Explore TracerBullet implementation
- Understand the code

### Part 4: Q&A (10 min)
- Ask questions
- Discuss use cases

---

## ✅ Pre-Demo Checklist

Before the demo, make sure you have:

- [x] Java 17+ installed
- [x] Maven 3.6+ installed
- [x] Repository cloned
- [x] Build successful (`mvn clean install`)
- [x] 7 tests passed
- [x] Diagrams generated
- [x] Test report opens in browser
- [ ] Read ARTICLE.md (optional but recommended)
- [ ] Questions prepared for Q&A

---

## 📞 Need Help?

If you encounter issues:

1. **Check troubleshooting section above**
2. **Review error messages carefully**
3. **Bring questions to the demo** - we'll address them in Q&A
4. **GitHub Issues:** [Report issues here](https://github.com/aniketjan26/tracerbullet-demo/issues)

---

## 🚀 Quick Commands Reference

```bash
# Clone repository
git clone https://github.com/aniketjan26/tracerbullet-demo.git
cd tracerbullet-demo

# Build and test (generates diagrams)
mvn clean install

# Build without tests
mvn clean package -DskipTests

# Run tests only
mvn test

# Run application
java -jar target/tracerbullet-demo-1.0.0.jar

# Test application (in another terminal)
curl http://localhost:8080/users/123

# View test report
open diagrams/test-report.html  # macOS
xdg-open diagrams/test-report.html  # Linux
start diagrams/test-report.html  # Windows
```

---

## 📁 Project Structure

After successful build:

```
tracerbullet-demo/
├── ARTICLE.md              ← Full article (read before demo)
├── SETUP-GUIDE.md          ← This file
├── README.md               ← Project documentation
├── pom.xml                 ← Maven configuration
├── src/
│   ├── main/kotlin/        ← Application code
│   └── test/kotlin/        ← Test code (7 scenarios)
├── target/
│   └── tracerbullet-demo-1.0.0.jar  ← Executable
└── diagrams/               ← Generated by tests
    ├── test-report.html    ← View this!
    ├── test-summary.txt
    └── trace-*.puml        ← Diagrams
```

---

## 🎓 Key Concepts (Quick Primer)

Before the demo, understand these basics:

### Trace
A complete journey of a single request through your system.
- **Trace ID:** Unique identifier (e.g., `a1b2c3d4-5678-90ab-cdef-1234567890ab`)
- Shared across all services

### Span
A single operation within a trace.
- **Span ID:** Unique identifier for this operation
- **Parent Span ID:** Links to calling service
- Contains: service name, duration, status code

### Example Flow

```
User Request → Trace ID: abc123
├─ Frontend Service → Span 1 (parent: none)
│  └─ Backend Service → Span 2 (parent: Span 1)
│     └─ Database Service → Span 3 (parent: Span 2)
```

All three spans share **Trace ID: abc123** ← This is the magic! ✨

---

## 🎯 Success Criteria

You're ready when:

✅ All prerequisite software installed
✅ Repository cloned
✅ Build completed successfully
✅ 7/7 tests passed
✅ Diagrams generated in `diagrams/` folder
✅ Test report opens and shows scenarios with diagrams
✅ You understand trace vs span concept

---

**See you at the demo! 🚀**

*Questions? Bring them to the Q&A session!*
