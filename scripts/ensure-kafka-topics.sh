#!/usr/bin/env bash
# Ensure the demo Kafka topics exist with 3 partitions.
# Replication stays at 1 because the local compose stack runs a single broker.
set -euo pipefail

CONTAINER="${KAFKA_CONTAINER:-koperasi-kafka}"
BOOTSTRAP="${KAFKA_BOOTSTRAP:-localhost:9092}"
PARTITIONS="${KAFKA_PARTITIONS:-3}"

TOPICS=(
  loan.application.created
  loan.application.under_review
  loan.application.approved
  loan.application.rejected
  loan.application.disbursed
  loan.application.active
  loan.application.completed
  loan.installment.overdue
  loan.defaulted
  loan.scoring.requested
  loan.scoring.completed
  payment.disbursement.initiated
  payment.disbursement.completed
  payment.disbursement.failed
  payment.installment.received
  payment.installment.failed
  payment.retry
  payment.retry.DLT
  notification.send.requested
  audit.event.created
)

existing="$(docker exec "$CONTAINER" kafka-topics --bootstrap-server "$BOOTSTRAP" --list)"

for topic in "${TOPICS[@]}"; do
  if grep -qx "$topic" <<<"$existing"; then
    current="$(docker exec "$CONTAINER" kafka-topics --bootstrap-server "$BOOTSTRAP" \
      --describe --topic "$topic" | awk -F'PartitionCount: ' 'NR==1 {split($2, a, /[^0-9]/); print a[1]}')"
    if [[ -n "$current" && "$current" -lt "$PARTITIONS" ]]; then
      docker exec "$CONTAINER" kafka-topics --bootstrap-server "$BOOTSTRAP" \
        --alter --topic "$topic" --partitions "$PARTITIONS"
      echo "altered $topic: $current -> $PARTITIONS partitions"
    fi
  else
    docker exec "$CONTAINER" kafka-topics --bootstrap-server "$BOOTSTRAP" \
      --create --if-not-exists --topic "$topic" \
      --partitions "$PARTITIONS" --replication-factor 1
    echo "created $topic"
  fi
done
