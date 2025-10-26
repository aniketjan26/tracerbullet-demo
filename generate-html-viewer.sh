#!/bin/bash

echo "Generating HTML viewer for diagrams..."

DIAGRAMS_DIR="diagrams"
HTML_FILE="$DIAGRAMS_DIR/index.html"

cat > "$HTML_FILE" << 'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TracerBullet Diagrams</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }

        .container {
            max-width: 1400px;
            margin: 0 auto;
        }

        header {
            background: white;
            border-radius: 10px;
            padding: 30px;
            margin-bottom: 30px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
        }

        h1 {
            color: #667eea;
            margin-bottom: 10px;
        }

        .subtitle {
            color: #666;
            font-size: 1.1em;
        }

        .diagram-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(500px, 1fr));
            gap: 20px;
        }

        .diagram-card {
            background: white;
            border-radius: 10px;
            padding: 20px;
            box-shadow: 0 5px 15px rgba(0,0,0,0.2);
            transition: transform 0.3s ease;
        }

        .diagram-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 10px 25px rgba(0,0,0,0.3);
        }

        .diagram-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
            padding-bottom: 15px;
            border-bottom: 2px solid #f0f0f0;
        }

        .diagram-title {
            font-weight: bold;
            color: #333;
            font-size: 1.1em;
        }

        .diagram-image {
            width: 100%;
            border: 1px solid #e0e0e0;
            border-radius: 5px;
            margin-bottom: 15px;
            cursor: pointer;
        }

        .diagram-content {
            background: #f8f9fa;
            border-radius: 5px;
            padding: 15px;
            font-family: 'Courier New', monospace;
            font-size: 0.9em;
            white-space: pre-wrap;
            max-height: 300px;
            overflow-y: auto;
        }

        .view-buttons {
            display: flex;
            gap: 10px;
            margin-top: 15px;
        }

        .btn {
            padding: 8px 16px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-size: 0.9em;
            transition: all 0.3s ease;
            text-decoration: none;
            display: inline-block;
        }

        .btn-primary {
            background: #667eea;
            color: white;
        }

        .btn-primary:hover {
            background: #5568d3;
        }

        .btn-secondary {
            background: #f0f0f0;
            color: #333;
        }

        .btn-secondary:hover {
            background: #e0e0e0;
        }

        .stats {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin-top: 20px;
        }

        .stat-card {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 8px;
            text-align: center;
        }

        .stat-value {
            font-size: 2em;
            font-weight: bold;
            color: #667eea;
        }

        .stat-label {
            color: #666;
            margin-top: 5px;
        }

        .modal {
            display: none;
            position: fixed;
            z-index: 1000;
            left: 0;
            top: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0,0,0,0.9);
        }

        .modal-content {
            margin: auto;
            display: block;
            max-width: 90%;
            max-height: 90%;
            margin-top: 50px;
        }

        .close {
            position: absolute;
            top: 30px;
            right: 50px;
            color: #f1f1f1;
            font-size: 40px;
            font-weight: bold;
            cursor: pointer;
        }

        .close:hover {
            color: #bbb;
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>TracerBullet Distributed Tracing Diagrams</h1>
            <p class="subtitle">Visual representation of request flows through microservices</p>

            <div class="stats" id="stats">
                <!-- Stats will be injected here -->
            </div>
        </header>

        <div class="diagram-grid" id="diagramGrid">
            <!-- Diagrams will be injected here -->
        </div>
    </div>

    <div id="imageModal" class="modal">
        <span class="close">&times;</span>
        <img class="modal-content" id="modalImage">
    </div>

    <script>
        // Load diagrams
        async function loadDiagrams() {
            const txtFiles = [
EOF

# Add all text files
for file in "$DIAGRAMS_DIR"/*.txt; do
    if [ -f "$file" ]; then
        basename=$(basename "$file")
        echo "                '${basename}'," >> "$HTML_FILE"
    fi
done

cat >> "$HTML_FILE" << 'EOF'
            ];

            const grid = document.getElementById('diagramGrid');
            const stats = document.getElementById('stats');

            let totalTraces = 0;
            let totalSpans = 0;
            let totalErrors = 0;

            for (const txtFile of txtFiles) {
                const response = await fetch(txtFile);
                const content = await response.text();

                // Parse summary info
                const traceIdMatch = content.match(/Trace ID: ([^\n]+)/);
                const spansMatch = content.match(/Total Spans: (\d+)/);
                const durationMatch = content.match(/Total Duration: (\d+)ms/);
                const errorMatch = content.match(/⚠️ ERROR RESPONSE/g);

                const traceId = traceIdMatch ? traceIdMatch[1] : 'Unknown';
                const spans = spansMatch ? parseInt(spansMatch[1]) : 0;
                const duration = durationMatch ? durationMatch[1] : 'N/A';
                const hasErrors = errorMatch ? errorMatch.length : 0;

                totalTraces++;
                totalSpans += spans;
                totalErrors += hasErrors;

                const pumlFile = txtFile.replace('.txt', '.puml');
                const pumlResponse = await fetch(pumlFile);
                const pumlContent = await pumlResponse.text();

                // Generate PlantUML server URL
                const encodedPuml = encodePlantUML(pumlContent);
                const imageUrl = `https://www.plantuml.com/plantuml/svg/${encodedPuml}`;

                const card = document.createElement('div');
                card.className = 'diagram-card';
                card.innerHTML = `
                    <div class="diagram-header">
                        <div class="diagram-title">Trace: ${traceId.substring(0, 16)}...</div>
                        <div>${spans} spans • ${duration}ms ${hasErrors > 0 ? '⚠️' : '✓'}</div>
                    </div>
                    <img src="${imageUrl}" class="diagram-image" alt="Trace Diagram" onclick="showModal('${imageUrl}')">
                    <div class="diagram-content">${content}</div>
                    <div class="view-buttons">
                        <a href="${imageUrl}" target="_blank" class="btn btn-primary">Open Full Diagram</a>
                        <a href="${pumlFile}" target="_blank" class="btn btn-secondary">View PlantUML</a>
                        <a href="${txtFile}" target="_blank" class="btn btn-secondary">View Text</a>
                    </div>
                `;

                grid.appendChild(card);
            }

            // Update stats
            stats.innerHTML = `
                <div class="stat-card">
                    <div class="stat-value">${totalTraces}</div>
                    <div class="stat-label">Total Traces</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${totalSpans}</div>
                    <div class="stat-label">Total Spans</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${totalErrors}</div>
                    <div class="stat-label">Error Spans</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${(totalSpans / totalTraces).toFixed(1)}</div>
                    <div class="stat-label">Avg Spans/Trace</div>
                </div>
            `;
        }

        // Simple PlantUML encoding (for demonstration)
        function encodePlantUML(text) {
            // This is a simplified version - in production you'd use proper encoding
            return btoa(text).replace(/\+/g, '-').replace(/\//g, '_');
        }

        // Modal functionality
        function showModal(imageUrl) {
            const modal = document.getElementById('imageModal');
            const modalImg = document.getElementById('modalImage');
            modal.style.display = 'block';
            modalImg.src = imageUrl;
        }

        document.querySelector('.close').onclick = function() {
            document.getElementById('imageModal').style.display = 'none';
        }

        // Load diagrams on page load
        loadDiagrams();
    </script>
</body>
</html>
EOF

echo "✓ HTML viewer generated: $HTML_FILE"
echo ""
echo "Opening in browser..."

# Try to open in browser
if command -v xdg-open > /dev/null; then
    xdg-open "$HTML_FILE"
elif command -v open > /dev/null; then
    open "$HTML_FILE"
else
    echo "Please open $HTML_FILE in your browser"
fi
