#!/bin/bash

echo "========================================"
echo "  TracerBullet Demo Script"
echo "========================================"
echo ""
echo "This script will make requests to demonstrate"
echo "distributed tracing with TracerBullet."
echo ""
echo "Make sure the application is running first!"
echo "Run: mvn clean package && java -jar target/tracerbullet-demo-1.0.0.jar"
echo ""
echo "Press Enter to continue..."
read

echo ""
echo "=========================================="
echo "Demo 1: Simple User Lookup"
echo "=========================================="
echo "This will create a trace that flows through:"
echo "Frontend -> Backend -> Database"
echo ""
sleep 2

curl -v http://localhost:8080/users/123

echo ""
echo ""
echo "=========================================="
echo "Demo 2: Order Creation"
echo "=========================================="
echo "Another trace flowing through all services:"
echo ""
sleep 2

curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"product":"Widget","quantity":5}'

echo ""
echo ""
echo "=========================================="
echo "Demo 3: Multiple Concurrent Requests"
echo "=========================================="
echo "Watch how each request gets its own trace ID:"
echo ""
sleep 2

curl http://localhost:8080/users/456 &
curl http://localhost:8080/users/789 &
curl http://localhost:8080/users/101 &
wait

echo ""
echo ""
echo "=========================================="
echo "Demo Complete!"
echo "=========================================="
echo ""
echo "Check the application console to see the"
echo "complete trace information for each request!"
echo ""
