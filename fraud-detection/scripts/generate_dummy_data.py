#!/usr/bin/env python3
"""
Dummy Transaction Data Generator for Fraud Detection POC.

Generates realistic-looking bank transactions with labels
(fraud vs normal) for testing the scoring pipeline.

Usage:
    python3 scripts/generate_dummy_data.py [--count 1000] [--fraud-ratio 0.05]
"""

import json
import random
import time
import argparse
import os

random.seed(42)

# ============================================================
# Configuration
# ============================================================
ACCOUNTS = [
    {"id": "acc_normal_001", "type": "normal", "geo": "37.7749,-122.4194", "device": "device-mbp-001"},
    {"id": "acc_normal_002", "type": "normal", "geo": "40.7128,-74.0060", "device": "device-iphone-002"},
    {"id": "acc_normal_003", "type": "normal", "geo": "51.5074,-0.1278", "device": "device-pixel-003"},
    {"id": "acc_suspicious_001", "type": "suspicious", "geo": "35.6762,139.6503", "device": "emulator-android-xyz"},
    {"id": "acc_suspicious_002", "type": "suspicious", "geo": "1.3521,103.8198", "device": "unknown-device-999"},
]

MERCHANTS = ["AMZN", "GOOG", "APPL", "WMT", "TGT", "UBER", "DOOR", "NFLX", "SPOT", "AIRBNB"]
CHANNELS = ["ONLINE", "ATM", "POS"]
CURRENCIES = ["USD", "EUR", "GBP", "SGD"]

def generate_normal_txn(account, txn_id, base_time):
    """Generate a normal-looking transaction."""
    return {
        "transactionId": f"txn-{txn_id:06d}",
        "accountId": account["id"],
        "amount": round(random.lognormvariate(4.5, 0.8), 2),  # ~$50-$500
        "currency": random.choice(CURRENCIES),
        "merchantId": random.choice(MERCHANTS),
        "channel": random.choice(Channel_WEIGHTS := ["ONLINE"] * 6 + ["POS"] * 3 + ["ATM"] * 1),
        "deviceId": account["device"],
        "ipAddress": f"8.8.{random.randint(0,255)}.{random.randint(1,254)}",
        "geoLocation": account["geo"],
        "timestamp": base_time + random.randint(0, 3600000),  # within 1 hour
        "metadata": {"source": "normal"}
    }

def generate_fraud_txn(account, txn_id, base_time):
    """Generate a suspicious/fraudulent transaction."""
    # Sometimes use a different geo (impossible travel)
    geo = account["geo"]
    if random.random() < 0.4:
        geo = f"{random.uniform(-90, 90):.4f},{random.uniform(-180, 180):.4f}"

    return {
        "transactionId": f"txn-{txn_id:06d}",
        "accountId": account["id"],
        "amount": round(random.lognormvariate(7.5, 1.2), 2),  # ~$5000-$50000
        "currency": random.choice(CURRENCIES),
        "merchantId": random.choice(MERCHANTS[:3]),  # Fewer merchants
        "channel": random.choices(["ONLINE", "ATM", "POS"], weights=[7, 2, 1])[0],
        "deviceId": random.choice(["emulator-fake-001", "unknown-device-999",
                                    "proxy-device-vpn", account["device"]]),
        "ipAddress": f"{random.randint(1,254)}.{random.randint(1,254)}.{random.randint(1,254)}.{random.randint(1,254)}",
        "geoLocation": geo,
        "timestamp": base_time + random.randint(0, 3600000),
        "metadata": {"source": "suspicious"}
    }

def generate(count=100, fraud_ratio=0.05, output="transactions.json"):
    """Generate dummy transaction data."""
    base_time = int(time.time() * 1000) - 86400000  # 1 day ago
    transactions = []

    for i in range(count):
        is_fraud = random.random() < fraud_ratio
        account = random.choice(ACCOUNTS)

        if is_fraud:
            txn = generate_fraud_txn(account, i, base_time)
        else:
            txn = generate_normal_txn(account, i, base_time)

        transactions.append(txn)

    # Write to file
    with open(output, "w") as f:
        json.dump(transactions, f, indent=2)

    # Summary
    fraud_count = sum(1 for t in transactions if t["metadata"]["source"] == "suspicious")
    normal_count = count - fraud_count
    total_amount = sum(t["amount"] for t in transactions)

    print(f"✅ Generated {count} transactions → {output}")
    print(f"   Normal: {normal_count} | Fraud: {fraud_count} ({fraud_count/count*100:.1f}%)")
    print(f"   Total amount: ${total_amount:,.2f}")
    print(f"   Avg amount: ${total_amount/count:,.2f}")
    print()
    print("Sample normal txn:")
    for t in transactions:
        if t["metadata"]["source"] == "normal":
            print(f"  {t['transactionId']} | {t['accountId']} | ${t['amount']:>8.2f} | {t['deviceId'][:20]:20s}")
            break
    print("Sample fraud txn:")
    for t in transactions:
        if t["metadata"]["source"] == "suspicious":
            print(f"  {t['transactionId']} | {t['accountId']} | ${t['amount']:>8.2f} | {t['deviceId'][:20]:20s}")
            break

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate dummy transaction data")
    parser.add_argument("--count", type=int, default=100, help="Number of transactions")
    parser.add_argument("--fraud-ratio", type=float, default=0.08, help="Ratio of fraud transactions")
    parser.add_argument("--output", type=str, default="data/dummy_transactions.json",
                        help="Output file path")
    args = parser.parse_args()

    os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
    generate(args.count, args.fraud_ratio, args.output)
