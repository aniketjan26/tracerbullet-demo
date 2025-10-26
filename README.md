# TracerBullet Demo - http4k Distributed Tracing

A demonstration project showing how **TracerBullet** distributed tracing works with **http4k** in Kotlin.

## What is TracerBullet?

TracerBullet is a pattern for **distributed tracing** - tracking requests as they flow through multiple services in a microservices architecture. The name comes from the military practice of using tracer ammunition that lights up to show the bullet's path.

In software:
- A **trace** represents a single request's journey through your entire system
- Each service creates a **span** (a segment of the trace)
- All spans share the same **trace ID** to correlate them
- **Parent span IDs** create a hierarchy showing the call chain

### Why is it called "TracerBullet"?

Just like a tracer bullet lights up to show its path through the air, TracerBullet "lights up" a request's path through your distributed system, making it visible and trackable.

## How This Demo Works

This project implements a simple microservices architecture with three services:

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│  Frontend   │─────▶│   Backend   │─────▶│  Database   │
│  Port 8080  │      │  Port 8081  │      │  Port 8082  │
└─────────────┘      └─────────────┘      └─────────────┘
     Span 1               Span 2               Span 3
     └──────────────── Same Trace ID ────────────────┘
```

### Key Components

1. **TraceContext.kt**: Holds trace information (trace ID, span ID, parent span ID)
2. **TracerBullet.kt**: http4k Filter that implements the tracing logic
3. **Application.kt**: Three microservices demonstrating the trace flow

### How TracerBullet Works

#### Server-Side Filter
When a request arrives:
1. Extract trace headers from the request (or create new trace if none exist)
2. Store trace context in thread-local storage
3. Process the request
4. Log the trace information
5. Add trace headers to the response

#### Client-Side Filter
When making downstream calls:
1. Get current trace context
2. Create a child span for the downstream service
3. Add trace headers to the outgoing request
4. Send the request

## Prerequisites

- **Java 17** or higher
- **Maven 3.6** or higher
- **curl** (for running the demo script)

## Building the Project

```bash
mvn clean package
```

This creates an executable JAR: `target/tracerbullet-demo-1.0.0.jar`

## Running the Demo

### Step 1: Start the Application

```bash
java -jar target/tracerbullet-demo-1.0.0.jar
```

You should see:
```
==================================================
  TracerBullet Demo - http4k Distributed Tracing
==================================================

Starting 3 microservices:
  - Frontend Service: http://localhost:8080
  - Backend Service:  http://localhost:8081
  - Database Service: http://localhost:8082

✓ Frontend Service started on port 8080
✓ Backend Service started on port 8081
✓ Database Service started on port 8082
```

### Step 2: Make Requests

#### Manual Testing

```bash
# Get user information
curl http://localhost:8080/users/123

# Create an order
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"product":"Widget","quantity":5}'
```

#### Automated Demo

```bash
./demo.sh
```

### Step 3: Observe the Traces

Watch the application console output. You'll see trace information like:

**Console Output:**

```
========================================
TRACE: a1b2c3d4-e5f6-7890-abcd-ef1234567890
SPAN: span-001 [root]
SERVICE: frontend-service
METHOD: GET /users/123
STATUS: 200
DURATION: 150ms
========================================

========================================
TRACE: a1b2c3d4-e5f6-7890-abcd-ef1234567890
SPAN: span-002 [parent=span-001]
SERVICE: backend-service
METHOD: GET /api/users/123
STATUS: 200
DURATION: 100ms
========================================

========================================
TRACE: a1b2c3d4-e5f6-7890-abcd-ef1234567890
SPAN: span-003 [parent=span-002]
SERVICE: database-service
METHOD: GET /db/users/123
STATUS: 200
DURATION: 50ms
========================================
```

**Notice:**
- All three spans share the **same trace ID**: `a1b2c3d4-e5f6-7890-abcd-ef1234567890`
- Each span has its own **unique span ID**
- Child spans reference their **parent span ID**
- You can see the complete request path through the system

### Step 4: View UML Diagrams

TracerBullet automatically generates **UML sequence diagrams** for each trace! These diagrams provide a visual representation of the request flow, including:
- Service interactions
- Request/response status codes (with color coding)
- Timing information
- Parent-child relationships

**Generated Files:**

After making requests, check the `diagrams/` directory:

```bash
diagrams/
├── trace-a1b2c3d4.puml    # PlantUML diagram file
├── trace-a1b2c3d4.txt     # Text summary with visual tree
└── index.html             # HTML viewer (after running viewer script)
```

**Viewing Options:**

**Option 1: Interactive Script**
```bash
./view-diagrams.sh
```

This script offers:
1. View text summaries in terminal
2. Generate HTML viewer for all diagrams
3. Open PlantUML diagrams in online viewer
4. Show latest diagram

**Option 2: HTML Viewer (Recommended for Demo)**
```bash
./generate-html-viewer.sh
```

This creates an interactive HTML page showing:
- All trace diagrams rendered as images
- Statistics (total traces, spans, errors)
- Full trace details
- Clickable diagrams for full-screen view

**Option 3: VSCode PlantUML Extension**

Install the PlantUML extension in VSCode and open any `.puml` file for local rendering.

**Option 4: Online PlantUML Viewer**

Visit https://www.plantuml.com/plantuml/uml/ and paste the content from any `.puml` file.

**Example Diagram Output:**

The generated diagrams show:
- **Green status codes (200s)**: Successful responses
- **Orange status codes (400s)**: Client errors
- **Red status codes (500s)**: Server errors
- **Timing information**: Duration of each span
- **Service activation**: Visual representation of when each service is active

**Text Summary Example:**

```
================================================================================
TRACE SUMMARY
================================================================================

Trace ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890
Total Spans: 3
Total Duration: 200ms

Request Flow:
--------------------------------------------------------------------------------
✓ [frontend-service] GET /users/123 → 200 (150ms)
   ✓ [backend-service] GET /api/users/123 → 200 (100ms)
      ✓ [database-service] GET /db/users/123 → 200 (50ms)
```

## Key Learning Points

### 1. Trace Propagation

TracerBullet uses HTTP headers to propagate trace context:

```kotlin
X-Trace-Id: a1b2c3d4-e5f6-7890-abcd-ef1234567890
X-Span-Id: span-001
X-Parent-Span-Id: span-000
X-Service-Name: backend-service
```

### 2. Filter Composition

http4k's Filter pattern makes it easy to add tracing:

```kotlin
// Server-side (receives requests)
val app = TracerBullet.serverFilter("my-service")
    .then(myRoutes)

// Client-side (makes requests)
val client = TracerBullet.clientFilter("downstream-service")
    .then(ApacheClient())
```

### 3. Thread-Local Context

Trace context is stored in thread-local storage, making it accessible anywhere in your request handling without passing it explicitly:

```kotlin
val currentTrace = TraceContextHolder.get()
```

### 4. Parent-Child Relationships

When Service A calls Service B:
- Service A's span becomes the **parent**
- Service B creates a **child span** with the same trace ID
- This builds a call hierarchy

## Project Structure

```
tracerbullet-demo/
├── pom.xml                          # Maven configuration
├── demo.sh                          # Demo script
├── view-diagrams.sh                 # Diagram viewer script
├── generate-html-viewer.sh          # HTML viewer generator
├── README.md                        # This file
├── diagrams/                        # Generated diagrams (created at runtime)
│   ├── trace-*.puml                 # PlantUML sequence diagrams
│   ├── trace-*.txt                  # Text summaries
│   └── index.html                   # HTML viewer page
└── src/main/kotlin/com/demo/tracerbullet/
    ├── Application.kt               # Main app with 3 services
    ├── TraceContext.kt              # Trace context data model
    ├── TracerBullet.kt              # Tracing filter implementation
    ├── TraceCollector.kt            # Collects span data for diagrams
    └── DiagramGenerator.kt          # Generates PlantUML diagrams
```

## Explaining to Others

When demoing this to someone, follow this flow:

1. **Start the app** and show the three services starting
2. **Make a simple request**: `curl http://localhost:8080/users/123`
3. **Show the console output** - point out how all three traces share the same trace ID
4. **Explain the flow**: Frontend received request → called Backend → called Database
5. **Show the UML diagrams** - Run `./view-diagrams.sh` or `./generate-html-viewer.sh`
   - Point out the visual flow of the request
   - Highlight the status codes with color coding
   - Show the timing information
   - Demonstrate how the tree structure shows parent-child relationships
6. **Make concurrent requests** to show different trace IDs for different requests
7. **Show the diagrams directory** - Multiple traces, each with its own diagram
8. **Show the code**:
   - Start with `TraceContext.kt` - simple data model
   - Then `TracerBullet.kt` - the filter implementation
   - Show `TraceCollector.kt` - how spans are collected
   - Show `DiagramGenerator.kt` - how PlantUML diagrams are generated
   - Finally `Application.kt` - how services use the filters

**Pro Demo Tip:** Open the HTML viewer (`diagrams/index.html`) in a browser before the demo. As you make requests, refresh the page to show new diagrams appearing in real-time!

## Real-World Usage

In production systems, TracerBullet patterns are used by:

- **Zipkin**: Distributed tracing system
- **Jaeger**: End-to-end distributed tracing
- **AWS X-Ray**: Traces requests across AWS services
- **Google Cloud Trace**: Distributed tracing for GCP
- **OpenTelemetry**: Vendor-neutral observability framework

This demo shows the core concepts that all these systems build upon.

## Extending the Demo

Ideas for extending this demo:

1. **Add more services**: Create a payment service, notification service, etc.
2. **Visualize traces**: Export traces to Zipkin or Jaeger
3. **Add error handling**: Show how traces help debug failures
4. **Add metrics**: Track trace duration, error rates, etc.
5. **Database integration**: Replace mock database with real DB calls
6. **Async operations**: Show how traces work with async/parallel calls

## License

This is a demo project for educational purposes.