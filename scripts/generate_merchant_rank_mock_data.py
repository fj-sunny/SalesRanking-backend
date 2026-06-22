#!/usr/bin/env python3
"""Generate mock data for merchant_rank_info table."""

from datetime import datetime
import random

CITIES = [
    ("000000", "全国"),
    ("100000", "北京"),
    ("200000", "上海"),
    ("518000", "深圳"),
    ("610000", "成都"),
    ("430000", "武汉"),
]

CATEGORIES = [
    (0, "全部类型"),
    (1, "美食"),
    (2, "休闲丽人"),
]

RANK_TYPES = [
    (0, "爆款榜"),
    (1, "飙升榜"),
]

DATE_STR = "2026-06-21"
CREATE_TIME = int(datetime(2026, 6, 21, 12, 0, 0).timestamp())
OUTPUT_PATH = "merchant_rank_info_2026-06-21.sql"
BATCH_SIZE = 500
MERCHANT_ID_START = 200001

INSERT_HEADER = (
    "INSERT INTO `merchant_rank_info` "
    "(`city_id`, `type`, `category`, `merchant_id`, `sort`, "
    "`sale_num_month`, `sale_num_day`, `date`, `is_delete`, `create_time`, `update_time`) VALUES"
)


def build_sale_nums(rank_type: int, sort: int) -> tuple[int, int]:
    if rank_type == 0:
        sale_num_month = max(50, 15000 - sort * 120 + random.randint(-200, 200))
        sale_num_day = max(10, sale_num_month // 30 + random.randint(-10, 10))
    else:
        sale_num_day = max(50, 800 - sort * 6 + random.randint(-20, 20))
        sale_num_month = max(100, sale_num_day * 20 + random.randint(-100, 100))
    return sale_num_month, sale_num_day


def main() -> None:
    random.seed(20260621)
    values: list[str] = []
    merchant_id = MERCHANT_ID_START

    for city_id, _ in CITIES:
        for category, _ in CATEGORIES:
            for rank_type, _ in RANK_TYPES:
                for sort in range(1, 101):
                    sale_num_month, sale_num_day = build_sale_nums(rank_type, sort)
                    values.append(
                        f"('{city_id}', {rank_type}, {category}, {merchant_id}, {sort}, "
                        f"{sale_num_month}, {sale_num_day}, '{DATE_STR}', 0, {CREATE_TIME}, {CREATE_TIME})"
                    )
                    merchant_id += 1

    lines = [
        "-- merchant_rank_info 测试数据",
        f"-- 6个城市 x 3个类目 x 2个榜单 x 100条 = {len(values)}条",
        f"-- 日期: {DATE_STR}",
        "",
    ]

    for i in range(0, len(values), BATCH_SIZE):
        lines.append(INSERT_HEADER)
        batch = values[i : i + BATCH_SIZE]
        lines.append(",\n".join(batch) + ";")
        lines.append("")

    with open(OUTPUT_PATH, "w", encoding="utf-8") as file:
        file.write("\n".join(lines))

    print(f"Generated {len(values)} rows -> {OUTPUT_PATH}")


if __name__ == "__main__":
    main()
