#!/usr/bin/env python3
"""Send a signed Midtrans webhook to the local demo stack.

Useful when the demo uses placeholder Midtrans credentials: initiate the payment or
disbursement through the API (the gateway call may fail), read the created
payment/disbursement UUID from PostgreSQL, then simulate the gateway callback here.

Example:
  python3 scripts/simulate-midtrans-webhook.py \
    --order-id 1ec43a50-649f-4806-b1aa-a1870f2a2089 \
    --amount 5000000.00 \
    --server-key SB-Mid-server-xxxxxxxxxxxxxxxx
"""
from __future__ import annotations

import argparse
import hashlib
import json
import ssl
import urllib.request
from datetime import datetime, timezone


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--order-id", required=True)
    parser.add_argument("--amount", required=True, help="Gross amount, e.g. 908333.33")
    parser.add_argument("--status-code", default="200")
    parser.add_argument("--transaction-status", default="settlement")
    parser.add_argument("--server-key", default="SB-Mid-server-xxxxxxxxxxxxxxxx")
    parser.add_argument("--url", default="https://localhost/api/v1/payments/webhook/midtrans")
    parser.add_argument("--transaction-id", default=None)
    args = parser.parse_args()

    signature = hashlib.sha512(
        (args.order_id + args.status_code + args.amount + args.server_key).encode("utf-8")
    ).hexdigest()
    payload = {
        "order_id": args.order_id,
        "status_code": args.status_code,
        "gross_amount": args.amount,
        "signature_key": signature,
        "transaction_status": args.transaction_status,
        "transaction_id": args.transaction_id
        or f"SIM-{args.order_id[:8]}-{int(datetime.now(timezone.utc).timestamp())}",
    }

    context = ssl.create_default_context()
    context.check_hostname = False
    context.verify_mode = ssl.CERT_NONE
    request = urllib.request.Request(
        args.url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, context=context) as response:
        print(response.status, response.read().decode("utf-8"))


if __name__ == "__main__":
    main()
