#!/bin/sh
set -eu

NACOS_ADDR="${NACOS_ADDR:-nacos:8848}"
NACOS_BASE_URL="http://${NACOS_ADDR}/nacos/v1"
HEALTH_URL="${NACOS_BASE_URL}/console/health/readiness"

echo "Waiting for Nacos at ${NACOS_ADDR}..."
until curl -fsS "${HEALTH_URL}" >/dev/null 2>&1; do
  sleep 5
done

for file in /configs/*.yaml; do
  data_id="${file##*/}"
  echo "Importing ${data_id}"
  http_code="$(curl -sS -o /tmp/nacos-import-response.txt -w "%{http_code}" -X POST "${NACOS_BASE_URL}/cs/configs" \
    --data-urlencode "dataId=${data_id}" \
    --data-urlencode "group=DEFAULT_GROUP" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content@${file}")"

  if [ "${http_code}" != "200" ]; then
    echo "Import failed for ${data_id}, HTTP ${http_code}:"
    cat /tmp/nacos-import-response.txt
    exit 1
  fi
done

echo "Nacos config import completed."
