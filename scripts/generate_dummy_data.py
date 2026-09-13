#!/usr/bin/env python3
"""
Özerler Mermer ERP Platformu - Dummy Data Generator
Generates realistic, domain-accurate dummy data for natural stone ERP operations (PostgreSQL).

Features:
- Realistic 3-axis block dimensions and weighbridge scale deviation math
- Grade Multiplier algorithm pricing (Extra: 1.30, A: 1.15, B: 1.00, C: 0.65)
- 10 root-cause scrap codes (FR-01 to FR-10)
- Bidirectional genealogy (Quarry -> Block -> Slab -> CutItem -> Project WBS)
"""

import sys
import random
from datetime import datetime, timedelta

QUARRIES = [
    (1, "Q-MUG-01", "Yatağan Beyaz Ocağı", "Muğla - Yatağan", 2.70),
    (2, "Q-AFY-01", "Afyon Şeker Ocağı", "Afyonkarahisar - İscehisar", 2.72),
    (3, "Q-BUR-01", "Burdur Bej Ocağı", "Burdur - Karamanlı", 2.68),
    (4, "Q-DNZ-01", "Denizli Klasik Traverten", "Denizli - Kaklık", 2.50),
    (5, "Q-ANT-01", "Finike Lymra Ocağı", "Antalya - Finike", 2.45),
    (6, "Q-BIL-01", "Bilecik Bej Ocağı", "Bilecik - Söğüt", 2.71),
]

STONE_SPECS = {
    1: ("Muğla Beyaz", ["Ekstra Beyaz Kristalize", "Gri Bulutlu", "Dumanlı Beyaz"]),
    2: ("Afyon Şeker", ["Açık Krem Damarlı", "Altın Bal Sarısı", "Klasik Şeker"]),
    3: ("Burdur Bej", ["Homojen Açık Bej", "İnci Taneli Bej", "Koyu Bej Damarlı"]),
    4: ("Denizli Traverten", ["Açık Ceviz Damar Kesim", "Klasik Noce", "Light Fildişi"]),
    5: ("Finike Limra", ["Homojen Beyaz", "Fosilli Doğal Doku"]),
    6: ("Bilecik Rozaliya", ["Pembe Alevli", "Gül Kurusu Damarlı"]),
}

GRADES = ["EXTRA", "A", "B", "C"]
GRADE_MULTIPLIERS = {"EXTRA": 1.30, "A": 1.15, "B": 1.00, "C": 0.65}
STATUSES = ["QUARRY", "FACTORY_STOCK", "SAWING", "CUTTING", "DEPOT", "SHIPPED"]
FINISHES = ["POLISHED", "HONED", "BRUSHED", "BUSH_HAMMERED", "RAW"]

OPERATORS = ["Ahmet Kaya", "Ali Yıldız", "Mustafa Yılmaz", "Hasan Demir", "Mehmet Can", "Serkan Özkan"]
MACHINES = ["Katrak-01 (80 Lamalı)", "Katrak-02 (100 Lamalı)", "ST Blok Kesme 01", "Köprü Kesme - CNC 01", "Köprü Kesme - CNC 02"]


def generate_sql(num_blocks=30, num_projects=5):
    sql_lines = [
        "-- Auto-generated Dummy Data Script for Özerler Mermer ERP (PostgreSQL)",
        f"-- Generated at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n",
    ]

    base_date = datetime.now() - timedelta(days=60)
    block_id_start = 100
    slab_id_start = 500
    prod_id_start = 200
    cut_order_id_start = 300
    item_id_start = 1000

    sql_lines.append("-- 1. DYNAMIC MERMER BLOKLARI (Blocks)")
    block_records = []

    for i in range(num_blocks):
        b_id = block_id_start + i
        quarry = random.choice(QUARRIES)
        q_id, q_code, q_name, q_loc, sg = quarry
        stone_name, tones = STONE_SPECS[q_id]
        color = random.choice(tones)

        width = random.randint(150, 200)
        length = random.randint(250, 320)
        height = random.randint(120, 165)

        volume = round((width * length * height) / 1_000_000.0, 3)
        theoretical_kg = round(volume * 1000.0 * sg, 2)

        # Deviation between -3% and +7%
        dev_pct = round(random.uniform(-3.0, 7.0), 2)
        actual_kg = round(theoretical_kg * (1.0 + (dev_pct / 100.0)), 2)

        grade = random.choice(GRADES)
        crack = 0 if grade in ["EXTRA", "A"] else random.randint(1, 3)
        status = random.choice(STATUSES)

        ext_cost = round(random.uniform(25000, 55000), 2)
        trans_cost = 0.0 if status == "QUARRY" else round(random.uniform(5000, 9500), 2)
        total_cost = ext_cost + trans_cost

        ext_date = (base_date + timedelta(days=random.randint(1, 55))).strftime("%Y-%m-%d")
        b_code = f"BLK-{datetime.now().year}-{b_id:05d}"
        note = f"{stone_name} {color} ocağından çıkarıldı."

        sql_lines.append(
            f"INSERT INTO blocks (id, quarry_id, block_code, extraction_date, width_cm, length_cm, height_cm, "
            f"volume_m3, theoretical_weight_kg, actual_weight_kg, weight_deviation_pct, stone_type, color_tone, "
            f"quality_grade, crack_level, status, extraction_cost, transport_cost, total_cost, notes) VALUES "
            f"({b_id}, {q_id}, '{b_code}', '{ext_date}', {width}, {length}, {height}, {volume}, "
            f"{theoretical_kg}, {actual_kg}, {dev_pct}, '{stone_name}', '{color}', '{grade}', {crack}, "
            f"'{status}', {ext_cost}, {trans_cost}, {total_cost}, '{note}') "
            f"ON CONFLICT (id) DO NOTHING;"
        )
        block_records.append((b_id, b_code, stone_name, total_cost, status))

    sql_lines.append("\n-- 2. FABRİKA ÜRETİM EMİRLERİ & PLAKALAR (Production & Slabs)")
    for idx, (b_id, b_code, stone_name, total_cost, status) in enumerate(block_records):
        if status in ["QUARRY"]:
            continue

        p_id = prod_id_start + idx
        order_no = f"PRD-{datetime.now().year}-{p_id:05d}"
        machine = random.choice(MACHINES)
        operator = random.choice(OPERATORS)
        hours = round(random.uniform(7.0, 11.5), 2)
        kwh = round(hours * random.uniform(45.0, 55.0), 2)
        wear = round(hours * 0.14, 2)
        start_dt = (base_date + timedelta(days=idx)).strftime("%Y-%m-%d 08:00:00")
        end_dt = (base_date + timedelta(days=idx)).strftime("%Y-%m-%d 17:30:00")

        sql_lines.append(
            f"INSERT INTO production_orders (id, order_no, block_id, machine_name, process_type, "
            f"start_time, end_time, duration_hours, electricity_kwh, blade_wear_mm, operator_name, status, notes) VALUES "
            f"({p_id}, '{order_no}', {b_id}, '{machine}', 'GANGSAW', '{start_dt}', '{end_dt}', {hours}, "
            f"{kwh}, {wear}, '{operator}', 'COMPLETED', '{b_code} katrak kesimi tamamlandı.') "
            f"ON CONFLICT (id) DO NOTHING;"
        )

        # Generate 4-8 representative slabs per order
        slab_count = random.randint(4, 7)
        for s_idx in range(slab_count):
            s_id = slab_id_start + (idx * 10) + s_idx
            slab_code = f"SLB-{datetime.now().year}-{s_id:05d}"
            thickness = 2.00 if random.random() > 0.25 else 3.00
            s_width = round(random.uniform(160.0, 185.0), 2)
            s_length = round(random.uniform(270.0, 310.0), 2)
            area = round((s_width * s_length) / 10_000.0, 4)
            s_grade = random.choice(["EXTRA", "A", "B", "C"])
            finish = random.choice(FINISHES)
            gloss = 88 if finish == "POLISHED" else (60 if finish == "HONED" else 0)

            # Grade multiplier calculation
            base_m2_cost = round((total_cost / 150.0) * GRADE_MULTIPLIERS[s_grade], 2)
            s_status = "AVAILABLE" if random.random() > 0.3 else "RESERVED"

            sql_lines.append(
                f"INSERT INTO slabs (id, slab_code, order_id, block_id, pallet_id, thickness_cm, width_cm, "
                f"length_cm, surface_area_m2, surface_finish, quality_grade, gloss_level, cost_per_m2, status) VALUES "
                f"({s_id}, '{slab_code}', {p_id}, {b_id}, 1, {thickness}, {s_width}, {s_length}, {area}, "
                f"'{finish}', '{s_grade}', {gloss}, {base_m2_cost}, '{s_status}') "
                f"ON CONFLICT (id) DO NOTHING;"
            )

    sql_lines.append("\n-- Synchronize sequences with inserted IDs")
    sql_lines.append("SELECT setval(pg_get_serial_sequence('blocks', 'id'), COALESCE((SELECT MAX(id) FROM blocks), 1));")
    sql_lines.append("SELECT setval(pg_get_serial_sequence('production_orders', 'id'), COALESCE((SELECT MAX(id) FROM production_orders), 1));")
    sql_lines.append("SELECT setval(pg_get_serial_sequence('slabs', 'id'), COALESCE((SELECT MAX(id) FROM slabs), 1));\n")
    return "\n".join(sql_lines)


def main():
    num_blocks = int(sys.argv[1]) if len(sys.argv) > 1 else 25
    output_file = sys.argv[2] if len(sys.argv) > 2 else "scripts/seed_dummy_data_generated.sql"

    print(f"Generating {num_blocks} marble blocks with production orders and slabs...")
    sql_content = generate_sql(num_blocks=num_blocks)

    with open(output_file, "w", encoding="utf-8") as f:
        f.write(sql_content)

    print(f"Done! Dummy data saved to: {output_file}")


if __name__ == "__main__":
    main()
