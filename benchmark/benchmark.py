#!/usr/bin/env python3
"""
PayRoute Performance & Concurrency Benchmark
Generates concurrent simulated payment requests to measure:
- Requests Per Second (RPS)
- Average Response Time
- p50, p95, p99 Latency
- Failure & Failover Rate
"""

import sys
import time
import uuid
import json
import random
import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
import urllib.request
import urllib.error

def send_payment(base_url, merchant_code, amount, currency, timeout):
    url = f"{base_url}/api/payments"
    idempotency_key = f"bench_{uuid.uuid4()}"
    payload = json.dumps({
        "amount": amount,
        "currency": currency,
        "customerEmail": f"user_{random.randint(1, 1000)}@benchmark.io",
        "customerId": f"bench_cust_{random.randint(1, 500)}"
    }).encode("utf-8")

    headers = {
        "Content-Type": "application/json",
        "Idempotency-Key": idempotency_key,
        "X-Merchant-Code": merchant_code
    }

    req = urllib.request.Request(url, data=payload, headers=headers, method="POST")
    start = time.perf_counter()
    success = False
    status_code = 0
    attempts_count = 1
    provider_used = None

    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            status_code = response.status
            elapsed_ms = (time.perf_counter() - start) * 1000.0
            data = json.loads(response.read().decode("utf-8"))
            success = (data.get("status") == "SUCCESS")
            attempts_count = data.get("totalAttempts", 1)
            provider_used = data.get("selectedProviderCode")
    except urllib.error.HTTPError as e:
        elapsed_ms = (time.perf_counter() - start) * 1000.0
        status_code = e.code
    except Exception as e:
        elapsed_ms = (time.perf_counter() - start) * 1000.0
        status_code = 500

    return {
        "success": success,
        "status_code": status_code,
        "latency_ms": elapsed_ms,
        "attempts_count": attempts_count,
        "provider_used": provider_used
    }

def calculate_percentile(sorted_list, percentile):
    if not sorted_list:
        return 0.0
    k = (len(sorted_list) - 1) * (percentile / 100.0)
    f = int(k)
    c = min(f + 1, len(sorted_list) - 1)
    d = k - f
    return sorted_list[f] + (sorted_list[c] - sorted_list[f]) * d

def run_benchmark(base_url, total_requests, concurrency, merchant_code):
    print("=" * 65)
    print("      PayRoute - High Concurrency Payment Benchmark")
    print("=" * 65)
    print(f"Target URL:         {base_url}/api/payments")
    print(f"Total Requests:     {total_requests}")
    print(f"Concurrency Level:  {concurrency} threads")
    print(f"Merchant Code:      {merchant_code}")
    print("-" * 65)
    print("Running workload, please wait...")

    results = []
    wall_start = time.perf_counter()

    with ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [
            executor.submit(
                send_payment,
                base_url,
                merchant_code,
                random.choice([100.0, 500.0, 1500.0, 5000.0, 10000.0]),
                "INR",
                timeout=10.0
            )
            for _ in range(total_requests)
        ]

        for future in as_completed(futures):
            results.append(future.result())

    wall_elapsed = time.perf_counter() - wall_start
    latencies = sorted([r["latency_ms"] for r in results])
    success_count = sum(1 for r in results if r["success"])
    fail_count = total_requests - success_count
    failover_count = sum(1 for r in results if r["attempts_count"] > 1)

    rps = total_requests / wall_elapsed if wall_elapsed > 0 else 0
    avg_latency = sum(latencies) / len(latencies) if latencies else 0
    p50 = calculate_percentile(latencies, 50)
    p95 = calculate_percentile(latencies, 95)
    p99 = calculate_percentile(latencies, 99)
    min_lat = latencies[0] if latencies else 0
    max_lat = latencies[-1] if latencies else 0

    print("\n" + "=" * 65)
    print("                  BENCHMARK RESULTS")
    print("=" * 65)
    print(f"Total Time Elapsed:       {wall_elapsed:.2f} seconds")
    print(f"Requests per Second:      {rps:.2f} req/sec")
    print(f"Successful Requests:      {success_count} ({(success_count/total_requests)*100:.1f}%)")
    print(f"Failed Requests:          {fail_count} ({(fail_count/total_requests)*100:.1f}%)")
    print(f"Failover Transactions:    {failover_count} ({(failover_count/total_requests)*100:.1f}%)")
    print("-" * 65)
    print(f"Average Latency:          {avg_latency:.2f} ms")
    print(f"p50 (Median) Latency:     {p50:.2f} ms")
    print(f"p95 Latency:              {p95:.2f} ms")
    print(f"p99 Latency:              {p99:.2f} ms")
    print(f"Min Latency:              {min_lat:.2f} ms")
    print(f"Max Latency:              {max_lat:.2f} ms")
    print("=" * 65)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="PayRoute Benchmark Client")
    parser.add_argument("--url", default="http://localhost:8080", help="Base backend URL")
    parser.add_argument("-n", "--requests", type=int, default=100, help="Total requests to send")
    parser.add_argument("-c", "--concurrency", type=int, default=10, help="Concurrent workers")
    parser.add_argument("-m", "--merchant", default="MERCH-ACME-001", help="Merchant code")

    args = parser.parse_args()
    run_benchmark(args.url, args.requests, args.concurrency, args.merchant)
