# SalesRanking-backend

商家售卖排行榜后端服务。数据存储在 MySQL，查询走 Redis 缓存，通过定时任务每日同步。

## 技术栈

- Java 21 / Spring Boot 4.1
- MyBatis-Plus 3.5
- MySQL + Redis

---

## 整体实现思路

```
MySQL（merchant_rank_info 表）
        │
        │  启动预加载 / 定时任务同步
        ▼
Redis（单 Key JSON 缓存，TTL 7 天）
        │
        │  POST /api/rank/query（只读 Redis，不回源 DB）
        ▼
前端 / 调用方
```

**核心原则：**

- **写路径**：MySQL → Redis（全量覆盖写入，幂等）
- **读路径**：只查 Redis，不查 MySQL
- **榜单顺序**：通过 `sort` 字段升序保证，不使用 ZSET

---

## 定时任务

### 1. 每日同步（`MerchantRankSyncScheduler`）

| 项 | 说明 |
|---|---|
| Cron | `0 0 12 * * ?`（每天中午 12:00） |
| 同步内容 | **前一天**的排行榜数据 |
| 写入方式 | 按 `cityId + type + category` 分组，每组全量 `SET` 覆盖写入 Redis |

**重试机制：**

- 若当天 MySQL 无数据（写入组数 = 0），**5 分钟后自动重试**
- 最多重试 **24 次**（约 2 小时）
- 适用于数据延迟落库的场景

### 2. 启动预加载（`MerchantRankCacheInitializer`）

| 项 | 说明 |
|---|---|
| 触发时机 | 应用启动完成后 |
| 行为 | 自动同步**前一天**数据到 Redis |
| 配置 | `rank.sync.preload-on-startup: true`（默认开启） |

避免服务刚启动、定时任务尚未执行时，查询接口缓存未命中。

---

## Redis Key 设计

### Key 格式

```
rank:sale:v1:{日期}:{榜单类型}:{城市ID}:{类目}
```

### 示例

```
rank:sale:v1:2026-06-21:0:000000:0
```

| 字段 | 含义 | 取值 |
|------|------|------|
| 日期 | 榜单统计日期 | `yyyy-MM-dd` |
| 榜单类型 type | `0` 爆款榜 / `1` 飙升榜 |
| 城市ID cityId | 全国为 `000000` |
| 类目 category | `0` 全部 / `1` 美食 / `2` 休闲丽人 |

### 过期策略

- **物理过期（TTL）**：7 天
- 每个 Key 独立设置 TTL，到期自动删除

---

## Redis 数据结构

采用 **单 Key + JSON 字符串** 存储，类型为 Redis `String`。

### Value 结构（JSON）

```json
{
  "items": [
    {
      "merchantId": 200001,
      "sort": 1,
      "saleNumMonth": 14948,
      "saleNumDay": 505
    },
    {
      "merchantId": 200002,
      "sort": 2,
      "saleNumMonth": 14749,
      "saleNumDay": 491
    }
  ]
}
```

| 字段 | 说明 |
|------|------|
| `items` | 榜单条目列表 |
| `merchantId` | 商家 ID |
| `sort` | 排名（越小越靠前） |
| `saleNumMonth` | 月售数量 |
| `saleNumDay` | 日售数量 |

> `cityId`、`type`、`category` 已编码在 Key 中，Value 不再重复存储。

### 写入特点

- 使用 `SET` **全量覆盖**，重复同步不会产生数据叠加
- 写入前按 `merchantId` **去重**，同一商家只保留 `sort` 最小（排名最高）的一条
- 读写均按 `sort` **升序**排列

---

## 查询逻辑

### 接口

```
POST /api/rank/query
Content-Type: application/json
```

**请求体：**

```json
{
  "cityId": "000000",
  "type": 0,
  "category": 0
}
```

**响应体：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "merchantId": 200001,
      "sort": 1,
      "saleNumMonth": 14948,
      "saleNumDay": 505,
      "date": "2026-06-21"
    }
  ]
}
```

### 查询流程

```
1. 校验参数（cityId / type / category 不能为空）
2. 以「昨天」为起始日期
3. 构造 Redis Key，读取缓存
4. 命中且 items 非空 → 按 sort 升序返回
5. 未命中 → 日期往前回退 1 天，重复步骤 3~4
6. 最多回退 7 天（与 Redis TTL 一致）
7. 全部未命中 → 返回空列表 []
```

### 回退机制示意

假设今天是 `2026-06-22`：

| 尝试次序 | 查询日期 | 说明 |
|---------|---------|------|
| 第 1 次 | 2026-06-21 | 最新榜单（昨天） |
| 第 2 次 | 2026-06-20 | 回退 1 天 |
| 第 3 次 | 2026-06-19 | 回退 2 天 |
| … | … | … |
| 第 7 次 | 2026-06-15 | 最后一次尝试 |

- 返回结果中的 `date` 字段为**实际命中的榜单日期**，不一定是昨天
- 若使用了历史榜单，服务端会打印回退日志

### 重要约束

- 查询**只读 Redis**，缓存未命中时**不会回源 MySQL**
- 榜单数据的时效性依赖定时任务和启动预加载保证

---

## 本地启动

```bash
# 1. 复制本地配置
copy src\main\resources\application-local.yaml.example src\main\resources\application-local.yaml

# 2. 编辑 application-local.yaml，填入 MySQL / Redis 连接信息

# 3. 启动
.\mvnw.cmd spring-boot:run
```

## 运行测试

```bash
# 接口单元测试（不依赖 MySQL / Redis，适合 CI）
.\mvnw.cmd test

# 含集成测试（需本地 MySQL + Redis）
.\mvnw.cmd test -Dgroups=integration
```
