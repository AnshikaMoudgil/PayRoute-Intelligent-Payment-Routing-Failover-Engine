@echo off
echo Starting PayRoute Performance Benchmark...
python benchmark\benchmark.py -n 100 -c 10 --url http://localhost:8080
pause
