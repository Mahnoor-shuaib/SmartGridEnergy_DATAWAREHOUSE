# ⚡ Smart Grid Energy Data Warehouse

## 🚨 Problem

Electricity distribution companies (DISCOs) in Pakistan have thousands of smart meters generating millions of readings daily. But the **operational database** is designed for fast inserts, not analytics.

**Result:**
- ❌ “Total consumption by city for last 5 years” → takes **minutes to hours**
- ❌ Compare this year vs last year → complex slow joins
- ❌ Cannot identify overloaded grid stations
- ❌ No peak vs off-peak analysis
- ❌ No revenue visibility by consumer category
- ❌ Analysts rewrite same complex queries again and again

**Data exists. Insights don't.**

---

## ✅ Solution

We built a **dedicated Data Warehouse** using **Star Schema** (1 Fact table + 5 Dimension tables) – completely separate from the operational database.

### How it works:
1. **Extract** – Daily pull from operational DB
2. **Transform** – Clean data, remove duplicates, resolve foreign keys, calculate cost
3. **Load** – Store in FACT table & update dimensions

## ✨ Features

- YoY consumption comparison by city
- Top 10 high-consumption meters
- Peak vs off-peak hour analysis
- Grid station load & outage monitoring
- Revenue by tariff category & city
- GUI for non-technical users


*"Same data. Now answers in seconds, not hours."*

## 🛠️ Tech

Oracle DB · Java 23 · Swing · ojdbc17 · NetBeans 25
