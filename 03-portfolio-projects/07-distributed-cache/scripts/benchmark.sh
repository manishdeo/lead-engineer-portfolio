#!/bin/bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
KEYS=1000
echo "🔥 Benchmarking cache at $BASE_URL with $KEYS keys"

echo ""
echo "--- WRITE benchmark ---"
START=$(date +%s%N)
for i in $(seq 1 $KEYS); do
  curl -s -X PUT "$BASE_URL/api/cache/bench-$i" \
    -H "Content-Type: application/json" \
    -d "{\"value\":\"value-$i\",\"ttlSeconds\":300}" > /dev/null &
  # Batch 50 concurrent requests
  if (( i % 50 == 0 )); then wait; fi
done
wait
END=$(date +%s%N)
WRITE_MS=$(( (END - START) / 1000000 ))
echo "Wrote $KEYS keys in ${WRITE_MS}ms ($(( KEYS * 1000 / WRITE_MS )) ops/sec)"

echo ""
echo "--- READ benchmark (should hit near-cache) ---"
START=$(date +%s%N)
for i in $(seq 1 $KEYS); do
  curl -s "$BASE_URL/api/cache/bench-$i" > /dev/null &
  if (( i % 50 == 0 )); then wait; fi
done
wait
END=$(date +%s%N)
READ_MS=$(( (END - START) / 1000000 ))
echo "Read $KEYS keys in ${READ_MS}ms ($(( KEYS * 1000 / READ_MS )) ops/sec)"

echo ""
echo "--- Cache stats ---"
curl -s "$BASE_URL/api/cache/stats" | python3 -m json.tool 2>/dev/null || curl -s "$BASE_URL/api/cache/stats"
echo ""
