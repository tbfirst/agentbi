# 智能BI系统 - 项目文档

## 📋 项目概述

智能BI系统是一个基于Spring Boot的智能数据分析平台，通过AI大模型自动分析上传的数据文件，生成专业的可视化图表和分析报告。系统采用前后端分离架构，支持用户注册登录、文件上传、AI智能分析、图表生成等功能。

### 核心特性

- 🔐 用户认证系统（注册、登录、退出）
- 📊 智能图表生成（支持多种图表类型）
- 🤖 AI驱动数据分析（基于阿里云百炼大模型）
- 📁 多格式文件支持（Excel、CSV等）
- 🔄 异步任务处理（RabbitMQ消息队列）
- 💾 多级缓存策略（Redis）
- 🎨 专业图表展示（ECharts）
- 📝 AI分析报告生成

---

## 🛠 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.5.7 | 核心框架 |
| Java | 21 | 开发语言 |
| MyBatis-Plus | 3.5.5 | ORM框架 |
| MySQL | 8.0+ | 数据库 |
| Redis | - | 缓存 |
| RabbitMQ | - | 消息队列 |
| Redisson | 3.48.0 | 分布式锁 |
| LangChain4j | 1.0.0-beta3 | AI应用框架 |
| 阿里云百炼SDK | 2.22.4 | AI大模型 |
| EasyExcel | 3.3.2 | Excel处理 |
| Apache Tika | 3.2.3 | 文件解析 |
| 阿里云OSS | 3.17.4 | 对象存储 |
| OpenCSV | 5.7.1 | CSV处理 |
| Hutool | 5.8.40 | 工具库 |

### 前端技术

| 技术 | 说明 |
|------|------|
| HTML5 | 页面结构 |
| CSS3 | 样式设计 |
| JavaScript | 交互逻辑 |
| ECharts 5.4.3 | 图表渲染 |
| Fetch API | HTTP请求 |

---

## 🏗 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                      前端层 (Browser)                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │ 登录页面  │  │ 注册页面  │  │ 图表页面  │              │
│  └──────────┘  └──────────┘  └──────────┘              │
└─────────────────────────────────────────────────────────────┘
                          ↓ HTTP/HTTPS
┌─────────────────────────────────────────────────────────────┐
│                   控制层 (Controller)                    │
│  ┌──────────────┐  ┌──────────────┐                 │
│  │ UserController │  │ ChartController│                 │
│  └──────────────┘  └──────────────┘                 │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                   服务层 (Service)                        │
│  ┌──────────────┐  ┌──────────────┐                 │
│  │ UserService   │  │ ChartService │                 │
│  └──────────────┘  └──────────────┘                 │
│  ┌──────────────┐  ┌──────────────┐                 │
│  │AiChartService │  │RagflowService│                 │
│  └──────────────┘  └──────────────┘                 │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│              消息队列层 (RabbitMQ)                       │
│  ┌──────────────┐  ┌──────────────┐                 │
│  │  工作队列    │  │  死信队列    │                 │
│  └──────────────┘  └──────────────┘                 │
│  ┌──────────────────────────────────┐                  │
│  │     ChartTaskConsumer          │                  │
│  └──────────────────────────────────┘                  │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                   数据访问层 (Mapper)                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐           │
│  │UserMapper │  │ChartMapper│  │FileTask  │           │
│  │          │  │          │  │Mapper    │           │
│  └──────────┘  └──────────┘  └──────────┘           │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                   数据存储层                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐           │
│  │  MySQL   │  │  Redis   │  │   OSS    │           │
│  │          │  │          │  │          │           │
│  └──────────┘  └──────────┘  └──────────┘           │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                   外部服务层                               │
│  ┌──────────────┐  ┌──────────────┐                 │
│  │ 阿里云百炼   │  │  RAGFlow     │                 │
│  │  (大模型)     │  │  (知识库)    │                 │
│  └──────────────┘  └──────────────┘                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 核心调用流程

### 1. 用户注册流程

```
用户访问注册页面
    ↓
填写用户名、密码
    ↓
提交注册请求 POST /api/user/register
    ↓
UserController.register()
    ↓
UserService.register()
    ↓
检查用户名是否已存在
    ↓
密码加密（BCrypt）
    ↓
保存到MySQL数据库
    ↓
返回注册成功
    ↓
跳转到登录页面
```

**接口说明：**
- **请求路径：** `POST /api/user/register`
- **请求参数：**
  ```json
  {
    "username": "用户名",
    "password": "密码"
  }
  ```
- **响应：**
  ```json
  {
    "code": 0,
    "message": "注册成功",
    "data": null
  }
  ```

---

### 2. 用户登录流程

```
用户访问登录页面
    ↓
填写用户名、密码
    ↓
提交登录请求 POST /api/user/login
    ↓
UserController.login()
    ↓
UserService.login()
    ↓
根据用户名查询用户信息
    ↓
验证密码（BCrypt）
    ↓
生成Session并存储到Redis
    ↓
返回登录成功
    ↓
跳转到图表生成页面
```

**接口说明：**
- **请求路径：** `POST /api/user/login`
- **请求参数：**
  ```json
  {
    "username": "用户名",
    "password": "密码"
  }
  ```
- **响应：**
  ```json
  {
    "code": 0,
    "message": "登录成功",
    "data": {
      "id": 1,
      "username": "用户名",
      "userRole": "user"
    }
  }
  ```

---

### 3. 文件上传与图表生成流程（核心流程）

```
用户上传文件并填写分析目标
    ↓
提交请求 POST /api/chart/aiChartMQApache2
    ↓
ChartController.genChartByAiMQApache2()
    ↓
验证用户登录状态
    ↓
ChartService.genChartByAiMQApache2()
    ↓
1. 文件安全扫描
    ↓
2. 上传文件到阿里云OSS
    ↓
3. 计算文件指纹（MD5）
    ↓
4. 检查文件是否已处理过（Redis缓存）
    ↓
5. 解析文件为CSV格式
    ↓
6. 上传CSV到OSS
    ↓
7. 创建文件解析任务记录到MySQL
    ↓
8. 发送任务消息到RabbitMQ队列
    ↓
返回 "FILE_PARSE_INITIATED:fileTaskId"
    ↓
前端开始轮询任务状态
```

**接口说明：**
- **请求路径：** `POST /api/chart/aiChartMQApache2`
- **请求参数：** `multipart/form-data`
  - `multipartFile`: 文件
  - `name`: 图表名称
  - `goal`: 分析目标
  - `chartType`: 图表类型
- **响应：**
  ```json
  {
    "code": 0,
    "message": "success",
    "data": "FILE_PARSE_INITIATED:abc123"
  }
  ```

---

### 4. 文件解析任务处理流程（异步）

```
RabbitMQ消费者接收消息
    ↓
ChartTaskConsumer.handleChartTask()
    ↓
1. 更新任务状态为RUNNING
    ↓
2. 检查Redis缓存是否已有结果
    ↓
   [缓存命中]
    ↓
直接返回缓存结果
    ↓
更新任务状态为SUCCEEDED
    ↓
ACK消息
    ↓
   [缓存未命中]
    ↓
3. 构建AI提示词
    ↓
4. 调用AiChartService.analyzeAndGenerateChart()
    ↓
   [RAGFlow知识库检索]
    ↓
   [调用阿里云百炼大模型]
    ↓
5. 获取AI分析结果
    ↓
6. 存储结果到Redis（多级缓存）
    ↓
   L1缓存：chart:result:{fingerprint} (1小时TTL)
    ↓
   L2缓存：chart:history:{memoryId}:{fingerprint} (7天TTL)
    ↓
7. 更新任务状态为SUCCEEDED
    ↓
8. ACK消息
```

**Redis缓存策略：**
```
L1缓存（指针）：
Key: chart:result:{fingerprint}
Value: {historyKey}|{timestamp}
TTL: 1小时

L2缓存（数据）：
Key: chart:history:{memoryId}:{fingerprint}
Value: {完整AI分析结果JSON}
TTL: 7天
```

---

### 5. 任务状态查询流程

```
前端轮询 GET /api/chart/fileTask/{fileTaskId}
    ↓
ChartController.getFileTaskStatus()
    ↓
验证用户登录状态
    ↓
ChartService.getFileTaskStatus()
    ↓
根据fileTaskId查询MySQL数据库
    ↓
返回任务状态信息
    ↓
   [SUCCEEDED]
    ↓
获取fingerprint
    ↓
调用图表结果查询接口
    ↓
   [RUNNING/PENDING]
    ↓
继续轮询
    ↓
   [FAILED]
    ↓
显示错误信息
```

**接口说明：**
- **请求路径：** `GET /api/chart/fileTask/{fileTaskId}`
- **响应：**
  ```json
  {
    "code": 0,
    "message": "success",
    "data": {
      "fileTaskId": "abc123",
      "status": "SUCCEEDED",
      "fingerprint": "xyz789",
      "csvUrl": "https://oss.example.com/file.csv",
      "errorMsg": null,
      "parseTimeMs": 1500,
      "fileSize": 102400,
      "csvSize": 51200
    }
  }
  ```

**任务状态说明：**
- `PENDING`: 待处理
- `RUNNING`: 执行中
- `SUCCEEDED`: 成功
- `FAILED`: 失败

---

### 6. 图表结果查询流程

```
前端调用 GET /api/chart/result/{fingerprint}
    ↓
ChartController.getChartResult()
    ↓
验证用户登录状态
    ↓
ChartService.getChartResultByFingerprint()
    ↓
1. 查询Redis L1缓存获取指针
    ↓
2. 根据指针查询L2缓存获取完整数据
    ↓
3. 解析AiAnalysisVO对象
    ↓
4. 转换为前端需要的格式
    ↓
   - chartConfig对象 → echartsCode JSON字符串
    ↓
   - analysis对象 → 格式化的分析文本
    ↓
5. 返回ChartResultResponse
    ↓
前端渲染ECharts图表
    ↓
显示AI分析结论
```

**接口说明：**
- **请求路径：** `GET /api/chart/result/{fingerprint}`
- **响应：**
  ```json
  {
    "code": 0,
    "message": "success",
    "data": {
      "chartConfig": {...},
      "echartsCode": "{\"title\":{\"text\":\"图表标题\"},\"series\":[...]}",
      "analysisResult": "**整体概况：\n数据整体呈现上升趋势...\n\n**关键发现：\n1. ...\n\n**优化建议：\n1. ...",
      "status": "COMPLETED"
    }
  }
  ```

---

### 7. 用户退出流程

```
用户点击退出登录
    ↓
提交请求 POST /api/user/logout
    ↓
UserController.logout()
    ↓
UserService.logout()
    ↓
清除Redis中的Session
    ↓
返回退出成功
    ↓
跳转到登录页面
```

**接口说明：**
- **请求路径：** `POST /api/user/logout`
- **响应：**
  ```json
  {
    "code": 0,
    "message": "退出成功",
    "data": null
  }
  ```

---

## 📊 数据库设计

### 用户表 (user)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| username | VARCHAR(50) | 用户名 | NOT NULL, UNIQUE |
| password | VARCHAR(100) | 密码（BCrypt加密） | NOT NULL |
| user_role | VARCHAR(20) | 用户角色 | DEFAULT 'user' |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE |
| is_delete | TINYINT | 是否删除 | DEFAULT 0 |

### 智能BI图表信息表 (chart)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| userId | BIGINT | 用户ID | - |
| name | VARCHAR(128) | 图表名称 | - |
| goal | TEXT | 分析目标 | - |
| chartData | TEXT | 图表数据 | - |
| chartType | VARCHAR(128) | 图表类型 | - |
| aiChart | TEXT | AI生成的图表数据 | - |
| aiResult | TEXT | AI生成的分析结论 | - |
| createTime | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| updateTime | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE |
| isDelete | TINYINT | 是否删除 | DEFAULT 0 |

### 文件解析任务表 (file_task_info)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| file_task_id | VARCHAR(64) | 文件任务ID | UNIQUE, NOT NULL |
| user_id | VARCHAR(64) | 用户ID | NOT NULL |
| fingerprint | VARCHAR(64) | 文件指纹 | NOT NULL |
| original_url | VARCHAR(512) | 原始文件OSS地址 | NOT NULL |
| csv_url | VARCHAR(512) | 解析后CSV地址 | - |
| status | VARCHAR(64) | 任务状态 | DEFAULT 'PENDING' |
| error_msg | TEXT | 错误信息 | - |
| file_size | BIGINT | 原始文件大小 | - |
| csv_size | BIGINT | CSV文件大小 | - |
| parse_time_ms | INT | 解析耗时（毫秒） | - |
| created_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| updated_time | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE |

**索引：**
- `idx_user_id`: user_id
- `idx_fingerprint`: fingerprint
- `idx_status_created`: status, created_time

---

## 🔌 API接口文档

### 用户相关接口

#### 1. 用户注册
- **接口：** `POST /api/user/register`
- **描述：** 用户注册
- **请求体：**
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```
- **响应：**
  ```json
  {
    "code": 0,
    "message": "注册成功",
    "data": null
  }
  ```

#### 2. 用户登录
- **接口：** `POST /api/user/login`
- **描述：** 用户登录
- **请求体：**
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```
- **响应：**
  ```json
  {
    "code": 0,
    "message": "登录成功",
    "data": {
      "id": 1,
      "username": "string",
      "userRole": "user"
    }
  }
  ```

#### 3. 用户退出
- **接口：** `POST /api/user/logout`
- **描述：** 用户退出登录
- **响应：**
  ```json
  {
    "code": 0,
    "message": "退出成功",
    "data": null
  }
  ```

### 图表相关接口

#### 1. 生成图表（异步）
- **接口：** `POST /api/chart/aiChartMQApache2`
- **描述：** 上传文件并生成图表
- **请求类型：** `multipart/form-data`
- **请求参数：**
  - `multipartFile`: 文件（必填）
  - `name`: 图表名称（必填）
  - `goal`: 分析目标（必填）
  - `chartType`: 图表类型（必填）
- **响应：**
  ```json
  {
    "code": 0,
    "message": "success",
    "data": "FILE_PARSE_INITIATED:abc123"
  }
  ```

#### 2. 查询文件任务状态
- **接口：** `GET /api/chart/fileTask/{fileTaskId}`
- **描述：** 查询文件解析任务状态
- **路径参数：**
  - `fileTaskId`: 文件任务ID
- **响应：**
  ```json
  {
    "code": 0,
    "message": "success",
    "data": {
      "fileTaskId": "string",
      "status": "SUCCEEDED",
      "fingerprint": "string",
      "csvUrl": "string",
      "errorMsg": "string",
      "parseTimeMs": 1500,
      "fileSize": 102400,
      "csvSize": 51200
    }
  }
  ```

#### 3. 查询图表结果
- **接口：** `GET /api/chart/result/{fingerprint}`
- **描述：** 根据指纹获取图表结果
- **路径参数：**
  - `fingerprint`: 文件指纹
- **响应：**
  ```json
  {
    "code": 0,
    "message": "success",
    "data": {
      "chartConfig": {...},
      "echartsCode": "string",
      "analysisResult": "string",
      "status": "COMPLETED"
    }
  }
  ```

---

## 🎨 前端页面说明

### 1. 登录页面 (login.html)

**功能：**
- 用户登录表单
- 表单验证
- 错误提示
- 跳转注册页面

**样式特点：**
- 腾讯云风格配色（#0052d9主色）
- 渐变背景
- 卡片式布局
- 响应式设计

### 2. 注册页面 (register.html)

**功能：**
- 用户注册表单
- 密码确认
- 表单验证
- 跳转登录页面

### 3. 图表生成页面 (chart.html)

**功能：**
- 文件上传（支持拖拽）
- 图表配置（名称、目标、类型）
- 任务状态轮询
- 图表展示（ECharts）
- AI分析结论展示
- 用户退出

**交互流程：**
1. 用户填写表单并上传文件
2. 提交后显示加载状态
3. 轮询任务状态（每2秒）
4. 任务完成后显示图表和分析结果

---

## 🔐 安全机制

### 1. 用户认证
- Session管理（Redis存储）
- 密码BCrypt加密
- 登录状态验证

### 2. 文件安全
- 文件类型检查
- 文件大小限制
- 文件内容扫描（Apache Tika）

### 3. 接口安全
- 用户登录验证
- 请求参数校验
- 异常处理

### 4. 数据安全
- 敏感信息加密
- SQL注入防护（MyBatis-Plus）
- XSS防护

---

## ⚙️ 配置说明

### application.yml

主要配置项：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/agentbi
    username: root
    password: password
  
  data:
    redis:
      # 单机模式配置（默认）
      host: localhost
      port: 6379
      database: 2
      password: "123456"
      timeout: 3000ms
      
      # 集群模式配置（启用时需注释掉单机配置）
      # cluster:
      #   enabled: true
      #   nodes: 127.0.0.1:7000,127.0.0.1:7001,127.0.0.1:7002,127.0.0.1:7003,127.0.0.1:7004,127.0.0.1:7005
      #   max-redirects: 3
      
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
  
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

# 布隆过滤器配置
bloom:
  filter:
    expected-insertions: 1000000    # 预期插入数据量
    false-probability: 0.001        # 误判率
    warm-up-days: 30                # 预热最近30天的数据

# 缓存防护配置
cache:
  cleanup:
    retention-days: 30              # 数据保留天数
    batch-size: 1000                # 批量处理大小
    cron: "0 0 2 * * ?"             # 清理任务执行时间
  archive:
    cron: "0 0 3 1 * ?"             # 归档任务执行时间
  rebuild-index:
    cron: "0 0 4 * * ?"             # 索引重建时间

aliyun:
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.com
    accessKeyId: your-access-key-id
    accessKeySecret: your-access-key-secret
    bucketName: your-bucket-name

dashscope:
  api-key: your-dashscope-api-key
```

---

## 🔧 Redis分片集群配置与验证

### 1. 集群架构说明

Redis分片集群采用无中心架构，数据自动分片到多个节点：

```
┌─────────────────────────────────────────────────────┐
│              Redis Cluster 架构                      │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐        │
│  │ Master1 │    │ Master2 │    │ Master3 │        │
│  │ slot    │    │ slot    │    │ slot    │        │
│  │ 0-5460  │    │ 5461-   │    │ 10923-  │        │
│  │         │    │ 10922   │    │ 16383   │        │
│  └────┬────┘    └────┬────┘    └────┬────┘        │
│       │              │              │              │
│  ┌────┴────┐    ┌────┴────┐    ┌────┴────┐        │
│  │ Slave1  │    │ Slave2  │    │ Slave3  │        │
│  └─────────┘    └─────────┘    └─────────┘        │
│                                                     │
└─────────────────────────────────────────────────────┘
```

**核心特性：**
- 数据自动分片到16384个槽位
- 主从复制保证高可用
- 自动故障转移
- 拓扑自动发现

### 2. 集群部署步骤

#### 2.1 创建集群目录

```bash
# 创建集群节点目录
mkdir -p /opt/redis-cluster/{7000,7001,7002,7003,7004,7005}
```

#### 2.2 配置节点

每个节点的 `redis.conf` 配置：

```conf
# redis-7000.conf 示例
port 7000
cluster-enabled yes
cluster-config-file nodes-7000.conf
cluster-node-timeout 5000
appendonly yes
daemonize yes
pidfile /var/run/redis_7000.pid
logfile /var/log/redis_7000.log
dir /opt/redis-cluster/7000
bind 0.0.0.0
protected-mode no
```

#### 2.3 启动节点

```bash
# 启动所有节点
redis-server /opt/redis-cluster/7000/redis.conf
redis-server /opt/redis-cluster/7001/redis.conf
redis-server /opt/redis-cluster/7002/redis.conf
redis-server /opt/redis-cluster/7003/redis.conf
redis-server /opt/redis-cluster/7004/redis.conf
redis-server /opt/redis-cluster/7005/redis.conf
```

#### 2.4 创建集群

```bash
# 使用 redis-cli 创建集群
redis-cli --cluster create \
  127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002 \
  127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005 \
  --cluster-replicas 1
```

### 3. 集群验证方法

#### 3.1 基础连接验证

```bash
# 连接集群任意节点
redis-cli -c -h 127.0.0.1 -p 7000

# 查看集群状态
127.0.0.1:7000> CLUSTER INFO
# 预期输出：cluster_state:ok

# 查看节点信息
127.0.0.1:7000> CLUSTER NODES
# 预期输出：显示所有主从节点信息
```

#### 3.2 数据分片验证

```bash
# 连接集群
redis-cli -c -p 7000

# 设置测试数据
127.0.0.1:7000> SET test:key1 "value1"
# 观察自动重定向到正确的槽位节点

# 获取数据
127.0.0.1:7000> GET test:key1
"value1"

# 查看key所在槽位
127.0.0.1:7000> CLUSTER KEYSLOT test:key1
(integer) 15239
```

#### 3.3 故障转移验证

```bash
# 1. 查看当前主节点
redis-cli -p 7000 CLUSTER NODES | grep master

# 2. 手动停止一个主节点（模拟故障）
redis-cli -p 7000 DEBUG SEGFAULT
# 或直接 kill 进程

# 3. 等待30秒后检查集群状态
redis-cli -p 7001 CLUSTER INFO
# 预期输出：cluster_state:ok（已自动故障转移）

# 4. 验证数据仍然可访问
redis-cli -c -p 7001 GET test:key1
```

#### 3.4 应用层验证

**方式一：日志验证**

启动应用后查看日志：

```log
# 预期日志输出
INFO  RedisClusterConfig - 初始化Redis分片集群连接工厂...
INFO  RedisClusterConfig - Redis分片集群连接工厂初始化完成，节点数: 6
INFO  RedisClusterConfig - RedisTemplate初始化完成，使用GenericJackson2JsonRedisSerializer序列化器
```

**方式二：接口测试**

```bash
# 1. 登录获取Session
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 2. 上传文件生成图表
curl -X POST http://localhost:8080/api/chart/aiChartMQApache2 \
  -F "multipartFile=@test.csv" \
  -F "name=测试图表" \
  -F "goal=分析数据趋势" \
  -F "chartType=折线图"

# 3. 查看Redis集群数据分布
redis-cli -c -p 7000 KEYS "*"
```

**方式三：监控脚本**

```bash
#!/bin/bash
# cluster_health_check.sh

echo "=== Redis Cluster Health Check ==="

for port in 7000 7001 7002 7003 7004 7005; do
    echo "\n--- Node $port ---"
    redis-cli -p $port PING
    redis-cli -p $port INFO replication | grep -E "role|connected_slaves"
done

echo "\n--- Cluster Status ---"
redis-cli -p 7000 CLUSTER INFO | grep -E "cluster_state|cluster_slots_assigned|cluster_slots_ok"

echo "\n--- Data Distribution ---"
redis-cli -p 7000 CLUSTER NODES | grep -E "master|slave" | awk '{print $2, $3, $4}'
```

### 4. 性能测试

#### 4.1 基准测试

```bash
# 使用 redis-benchmark
redis-benchmark -h 127.0.0.1 -p 7000 -c 100 -n 100000 -t set,get

# 预期结果
# SET: 100000 requests completed in 1.23 seconds
# GET: 100000 requests completed in 1.15 seconds
```

#### 4.2 集群吞吐量测试

```bash
# 多节点并发测试
redis-benchmark -h 127.0.0.1 -p 7000 -c 500 -n 500000 -t set,get &
redis-benchmark -h 127.0.0.1 -p 7001 -c 500 -n 500000 -t set,get &
redis-benchmark -h 127.0.0.1 -p 7002 -c 500 -n 500000 -t set,get &
wait
```

### 5. 常见问题排查

#### 5.1 连接超时

```bash
# 检查防火墙
sudo firewall-cmd --list-ports

# 开放端口
sudo firewall-cmd --add-port=7000-7005/tcp --permanent
sudo firewall-cmd --reload
```

#### 5.2 槽位未分配

```bash
# 检查槽位分配
redis-cli -p 7000 CLUSTER INFO | grep cluster_slots_assigned

# 如果槽位未完全分配，重新分配
redis-cli --cluster fix 127.0.0.1:7000
```

#### 5.3 节点通信失败

```bash
# 检查节点握手状态
redis-cli -p 7000 CLUSTER NODES

# 手动添加节点
redis-cli --cluster add-node 127.0.0.1:7006 127.0.0.1:7000
```

### 6. 配置切换

**单机模式 → 集群模式：**

```yaml
# application.yml
spring:
  data:
    redis:
      # 注释单机配置
      # host: localhost
      # port: 6379
      
      # 启用集群配置
      cluster:
        enabled: true
        nodes: 127.0.0.1:7000,127.0.0.1:7001,127.0.0.1:7002,127.0.0.1:7003,127.0.0.1:7004,127.0.0.1:7005
        max-redirects: 3
```

**验证切换成功：**

```bash
# 查看应用日志
tail -f logs/application.log | grep -i "cluster"

# 预期输出
INFO RedisClusterConfig - 初始化Redis分片集群连接工厂...
INFO RedisClusterConfig - Redis分片集群连接工厂初始化完成，节点数: 6
```

---

## 🚀 部署说明

### 环境要求

- JDK 21+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+
- Maven 3.6+

### 部署步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd demo
   ```

2. **配置数据库**
   ```bash
   mysql -u root -p < src/main/resources/sql/user.sql
   ```

3. **修改配置文件**
   ```yaml
   # 编辑 src/main/resources/application-dev.yml
   # 配置数据库、Redis、RabbitMQ、OSS等连接信息
   ```

4. **编译打包**
   ```bash
   mvn clean package
   ```

5. **运行应用**
   ```bash
   java -jar target/agentbi-0.0.1-SNAPSHOT.jar
   ```

6. **访问应用**
   ```
   http://localhost:8080/login.html
   ```

---

## 📝 开发规范

### 代码结构

```
com.tbfirst.agentbiinit
├── aichatmemorystore    # AI聊天记忆存储
├── aiservice             # AI服务
├── common               # 通用类
├── config               # 配置类
├── consumer             # 消息队列消费者
├── controller           # 控制器
├── exception           # 异常处理
├── manager             # 管理器
├── mapper             # 数据访问层
├── model              # 数据模型
│   ├── dto           # 数据传输对象
│   ├── entity        # 实体类
│   ├── enums        # 枚举类
│   └── vo           # 视图对象
├── prompt            # 提示词模板
├── ragflow           # RAGFlow知识库
├── service           # 服务层
│   └── impl        # 服务实现
└── utils            # 工具类
```

### 命名规范

- **类名：** 大驼峰命名法（PascalCase）
- **方法名：** 小驼峰命名法（camelCase）
- **常量名：** 全大写下划线分隔（UPPER_SNAKE_CASE）
- **包名：** 全小写点分隔（lower.case）

---
## 项目简单展示
<img width="1353" height="1469" alt="image" src="https://github.com/user-attachments/assets/4bb515bd-4dd9-4834-b330-1209b8b0d1ff" />

## 🔍 常见问题

### 1. 图表渲染失败
**原因：** ECharts配置格式错误
**解决：** 检查AI返回的chartConfig格式是否正确

### 2. 任务一直处于RUNNING状态
**原因：** RabbitMQ消费者异常
**解决：** 检查消费者日志，确认AI服务是否正常

### 3. 缓存未命中
**原因：** Redis连接问题或缓存过期
**解决：** 检查Redis连接，调整缓存TTL时间

### 4. 文件上传失败
**原因：** OSS配置错误或文件过大
**解决：** 检查OSS配置，调整文件大小限制

---

## 📞 联系方式

- 项目作者：tbfirst
- 技术支持：[邮箱地址]

---

## 📄 许可证

本项目仅供学习和研究使用。

---

## 🙏 致谢

感谢以下开源项目和技术社区的支持：
- Spring Boot
- MyBatis-Plus
- ECharts
- LangChain4j
- 阿里云百炼
- RabbitMQ
- Redis
