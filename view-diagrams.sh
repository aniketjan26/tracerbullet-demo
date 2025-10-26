#!/bin/bash

echo "=========================================="
echo "  TracerBullet Diagram Viewer"
echo "=========================================="
echo ""

DIAGRAMS_DIR="diagrams"

if [ ! -d "$DIAGRAMS_DIR" ]; then
    echo "No diagrams directory found!"
    echo "Run the application and make some requests first."
    exit 1
fi

PUML_COUNT=$(find "$DIAGRAMS_DIR" -name "*.puml" | wc -l)
TXT_COUNT=$(find "$DIAGRAMS_DIR" -name "*.txt" | wc -l)

echo "Found $PUML_COUNT PlantUML diagrams and $TXT_COUNT text summaries"
echo ""

if [ $PUML_COUNT -eq 0 ]; then
    echo "No diagrams found yet!"
    echo "Run the application and make some requests."
    exit 0
fi

echo "Available viewing options:"
echo ""
echo "1. View text summaries in terminal"
echo "2. Generate HTML viewer for all diagrams"
echo "3. Open PlantUML diagrams in online viewer"
echo "4. Show latest diagram"
echo ""
read -p "Choose option (1-4): " choice

case $choice in
    1)
        echo ""
        echo "Text Summaries:"
        echo "=========================================="
        for file in "$DIAGRAMS_DIR"/*.txt; do
            echo ""
            cat "$file"
            echo ""
            read -p "Press Enter for next diagram (or Ctrl+C to exit)..."
        done
        ;;
    2)
        echo ""
        echo "Generating HTML viewer..."
        ./generate-html-viewer.sh
        ;;
    3)
        echo ""
        echo "PlantUML Online Viewer:"
        echo "=========================================="
        echo ""
        echo "Visit: https://www.plantuml.com/plantuml/uml/"
        echo ""
        echo "Then copy and paste the content from any .puml file:"
        echo ""
        ls -1 "$DIAGRAMS_DIR"/*.puml
        echo ""
        echo "Or use the VSCode PlantUML extension for local viewing."
        ;;
    4)
        LATEST_TXT=$(ls -t "$DIAGRAMS_DIR"/*.txt | head -1)
        LATEST_PUML=$(ls -t "$DIAGRAMS_DIR"/*.puml | head -1)

        echo ""
        echo "Latest Trace Summary:"
        echo "=========================================="
        cat "$LATEST_TXT"
        echo ""
        echo "PlantUML diagram: $LATEST_PUML"
        ;;
    *)
        echo "Invalid option"
        exit 1
        ;;
esac

echo ""
echo "Done!"
