#!/usr/bin/env python3
"""
Generate 30 days of dummy transaction data for Nightly Batch testing.

Usage:
    python3 scripts/generate_30d_data.py --accounts 5 --days 30
"""
import json, random, time, argparse, os
random.seed(42)

ACCOUNTS = [
    {"id": "acc_normal_001", "type": "normal", "geo": "37.7749,-122.4194", "device": "device-mbp-001"},
    {"id": "acc_normal_002", "type": "normal", "geo": "40.7128,-74.0060", "device": "device-iphone-002"},
    {"id": "acc_normal_003", "type": "normal", "geo": "51.5074,-0.1278", "device": "device-pixel-003"},
    {"id": "acc_suspicious_001", "type": "suspicious", "geo": "35.6762,139.6503", "device": "emulator-android-xyz"},
    {"id": "acc_suspicious_002", "type": "suspicious", "geo": "1.3521,103.8198", "device": "unknown-device-999"},
]
MERCHANTS = ["AMZN","GOOG","APPL","WMT","TGT","UBER","DOOR","NFLX","SPOT","AIRBNB"]

def gen_txn(aid, i, ts, is_fraud):
    if is_fraud:
        geo = f"{random.uniform(-90,90):.4f},{random.uniform(-180,180):.4f}" if random.random()<0.4 else ACCOUNTS[aid][2]
        return {
            "transactionId": f"txn-30d-{i:06d}",
            "accountId": ACCOUNTS[aid][0],
            "amount": round(random.lognormvariate(7.5,1.2),2),
            "currency": random.choice(["USD","EUR","GBP","SGD"]),
            "merchantId": random.choice(MERCHANTS[:3]),
            "channel": random.choices(["ONLINE","ATM","POS"],[7,2,1])[0],
            "deviceId": random.choice(["emulator-fake-001","unknown-device-999","proxy-device-vpn",ACCOUNTS[aid][3]]),
            "ipAddress": f"{random.randint(1,254)}.{random.randint(1,254)}.{random.randint(1,254)}.{random.randint(1,254)}",
            "geoLocation": geo,
            "timestamp": ts,
            "metadata": {"source": "suspicious"}
        }
    else:
        return {
            "transactionId": f"txn-30d-{i:06d}",
            "accountId": ACCOUNTS[aid][0],
            "amount": round(random.lognormvariate(4.5,0.8),2),
            "currency": random.choice(["USD","EUR","GBP","SGD"]),
            "merchantId": random.choice(MERCHANTS),
            "channel": random.choices(["ONLINE"]*6+["POS"]*3+["ATM"]*1),
            "deviceId": ACCOUNTS[aid][3],
            "ipAddress": f"8.8.{random.randint(0,255)}.{random.randint(1,254)}",
            "geoLocation": ACCOUNTS[aid][2],
            "timestamp": ts,
            "metadata": {"source": "normal"}
        }

def generate(accounts=5, days=30, output_dir="data/30d"):
    os.makedirs(output_dir, exist_ok=True)
    total = 0
    now = int(time.time()*1000)
    per_account = {a[0]: {"normal":0,"fraud":0,"amount":0} for a in ACCOUNTS[:accounts]}

    for day in range(days):
        day_start = now - (days - day) * 86400000
        day_txns = []

        for aid in range(accounts):
            # 5-20 txns per account per day
            for _ in range(random.randint(5, 20)):
                is_fraud = random.random() < (0.12 if aid >= 3 else 0.03)
                ts = day_start + random.randint(0, 86399999)
                txn = gen_txn(aid, total, ts, is_fraud)
                day_txns.append(txn)
                per_account[ACCOUNTS[aid][0]]["normal" if not is_fraud else "fraud"] += 1
                per_account[ACCOUNTS[aid][0]]["amount"] += txn["amount"]
                total += 1

        # One file per day (like real batch would process daily)
        with open(f"{output_dir}/day_{day+1:02d}.json", "w") as f:
            json.dump(day_txns, f, indent=2)

    print(f"✅ Generated {days} days of data → {output_dir}/")
    print(f"   Total transactions: {total:,}")
    print(f"   Per account:")
    for a, stats in per_account.items():
        print(f"     {a:20s}: normal={stats['normal']:3d} fraud={stats['fraud']:3d} total=${stats['amount']:>8,.0f}")
    print(f"\n   Files: day_01.json ~ day_{days:02d}.json")

if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--accounts", type=int, default=5)
    p.add_argument("--days", type=int, default=30)
    p.add_argument("--output", type=str, default="data/30d")
    args = p.parse_args()
    generate(args.accounts, args.days, args.output)
