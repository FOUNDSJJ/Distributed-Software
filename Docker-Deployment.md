# Docker 部署说明

## 1. 项目结构

当前项目建议从仓库根目录进行部署，例如：

```text
/home/Distributed-Software
```

当前后端与部署相关目录结构如下：

```text
Distributed-Software/
├─ Back_End/
│  ├─ Log/
│  │  ├─ Dockerfile
│  │  ├─ pom.xml
│  │  ├─ src/
│  │  └─ target/
│  └─ Order/
│     ├─ Dockerfile
│     ├─ pom.xml
│     ├─ src/
│     └─ target/
├─ Database/
│  ├─ MySQL_RW/
│  │  ├─ master/
│  │  │  ├─ init.sql
│  │  │  ├─ my.cnf
│  │  │  └─ replication-user.sql
│  │  └─ slave/
│  │     ├─ my.cnf
│  │     └─ start-replication.sql
│  └─ Product_SQL/
│     └─ data.csv
├─ Front_End/
├─ nginx/
│  └─ conf.d/
│     └─ default.conf
├─ docker-compose.yml
└─ Docker-Deployment.md
```

## 2. 当前部署服务说明

当前 Docker 部署包含以下服务：

- `mysql-master`：MySQL 主库
- `mysql-slave`：MySQL 从库
- `mysql-replica-init`：主从复制初始化服务
- `redis`：缓存、会话、秒杀库存与幂等状态存储
- `kafka`：秒杀订单异步消息队列
- `backend1`：`Back_End/Log` 第一个实例
- `backend2`：`Back_End/Log` 第二个实例
- `backend-order`：`Back_End/Order` 秒杀下单后端
- `nginx`：前端静态页面托管与 API 反向代理

当前端口映射如下：

- `3307 -> mysql-master:3306`
- `3308 -> mysql-slave:3306`
- `6379 -> redis:6379`
- `9092 -> kafka:9092`
- `8081 -> backend1:9090`
- `8082 -> backend2:9090`
- `8083 -> backend-order:9091`
- `80 -> nginx:80`

## 3. 数据库初始化

### 3.1 数据库名称

当前所有后端统一使用同一个数据库：

```sql
distributed_software
```

### 3.2 数据表

当前主库初始化脚本位于：

[`Database/MySQL_RW/master/init.sql`](/d:/Private/Grade_3/Distributed-Software-HW/Database/MySQL_RW/master/init.sql)

该脚本会创建以下数据表：

- `users`
- `products`
- `seckill_orders`

其中 `seckill_orders` 用于支持：

- 雪花算法生成的长整型订单号
- 同一用户同一商品只能秒杀一次
- 按订单号查询订单
- 按用户 ID 查询订单

### 3.3 旧数据卷注意事项

`init.sql` 只会在 MySQL 数据卷第一次创建时自动执行。

如果你的 MySQL 容器和数据卷在新增 `seckill_orders` 表之前就已经存在，那么即使你后来修改了 `init.sql`，该表也不会自动补建。这种情况下需要手动在主库中执行建表语句：

```bash
docker exec -it mysql-master mysql -uroot -pPassword
```

```sql
USE distributed_software;

CREATE TABLE IF NOT EXISTS seckill_orders (
    order_no BIGINT NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT NOT NULL,
    order_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (order_no),
    UNIQUE KEY uq_seckill_user_product (user_id, product_id),
    KEY idx_seckill_user_id (user_id),
    KEY idx_seckill_product_id (product_id),
    CONSTRAINT fk_seckill_order_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_seckill_order_product FOREIGN KEY (product_id) REFERENCES products(id)
);
```

如果主从复制工作正常，该表会从主库自动同步到从库。

### 3.4 检查数据表

检查主库：

```bash
docker exec -it mysql-master mysql -uroot -pPassword -e "USE distributed_software; SHOW TABLES;"
```

检查从库：

```bash
docker exec -it mysql-slave mysql -uroot -pPassword -e "USE distributed_software; SHOW TABLES;"
```

## 4. 后端构建

部署前需要先分别构建 `Log` 和 `Order` 两个后端。

### 4.1 构建 `Log`

```bash
cd /home/Distributed-Software/Back_End/Log
mvn clean package -DskipTests
```

### 4.2 构建 `Order`

```bash
cd /home/Distributed-Software/Back_End/Order
mvn clean package -DskipTests
```

### 4.3 当前 Dockerfile

`Back_End/Log/Dockerfile`

```dockerfile
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

`Back_End/Order/Dockerfile`

```dockerfile
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 9091

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

## 5. Docker Compose 部署

当前部署文件位于：

[`docker-compose.yml`](/d:/Private/Grade_3/Distributed-Software-HW/docker-compose.yml)

当前部署的关键点如下：

- `backend1` 和 `backend2` 都从 `./Back_End/Log` 构建
- `backend-order` 从 `./Back_End/Order` 构建
- Kafka 使用 `apache/kafka:3.7.1`
- `backend-order` 依赖 MySQL、Redis 和 Kafka
- Nginx 负责前端静态资源托管以及 API 反向代理

启动全部服务：

```bash
cd /home/Distributed-Software
docker compose up -d --build
```

只启动基础设施：

```bash
docker compose up -d mysql-master mysql-slave mysql-replica-init redis kafka
```

只重新构建并启动秒杀订单后端：

```bash
docker compose up -d --build backend-order
```

## 6. Nginx 反向代理说明

当前 Nginx 配置位于：

[`nginx/conf.d/default.conf`](/d:/Private/Grade_3/Distributed-Software-HW/nginx/conf.d/default.conf)

当前代理规则如下：

- `/api/` 转发到 `backend1` 和 `backend2`
- `/api/seckill/orders` 单独转发到 `backend-order`

关键配置如下：

```nginx
location /api/seckill/orders {
    proxy_pass http://backend-order:9091;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}

location /api/ {
    proxy_pass http://backend_round_robin;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

修改 Nginx 配置后，需要重启 Nginx：

```bash
docker compose restart nginx
```

## 7. 当前秒杀下单流程

### 7.1 前端请求格式

当前前端商品页点击“立即下单”按钮后，会发送如下请求体：

```json
{
  "product_name": "无线鼠标"
}
```

请求接口为：

```text
POST /api/seckill/orders
```

请求会携带浏览器中的 Cookie。

### 7.2 后端处理逻辑

当前秒杀下单后端的处理流程为：

- 从 Cookie 中读取 `SESSIONID`
- 通过 Redis 会话解析当前登录用户
- 根据 `product_name` 查询商品
- 通过 Redis 完成库存扣减和同用户去重
- 将下单请求写入 Kafka
- 由消费者异步创建订单并写入 MySQL

### 7.3 订单查询接口

按订单号查询：

```text
GET /api/seckill/orders/{orderId}
```

按用户 ID 查询：

```text
GET /api/seckill/orders?userId=1
```

## 8. 常用命令

### 8.1 查看容器状态

```bash
docker compose ps
```

### 8.2 查看后端日志

```bash
docker compose logs -f backend1 backend2 backend-order
```

### 8.3 查看 Kafka 日志

```bash
docker logs kafka --tail 200
```

### 8.4 查看 Nginx 日志

```bash
docker logs nginx --tail 100
```

### 8.5 重启单个服务

```bash
docker compose restart backend-order
docker compose restart nginx
```

### 8.6 停止全部服务

```bash
docker compose down
```

### 8.7 无缓存重建

```bash
docker compose build --no-cache
```

### 8.8 删除旧数据卷

仅当你希望重新初始化 MySQL 数据时使用：

```bash
docker compose down
docker volume rm distributed-software_mysql_master_data
docker volume rm distributed-software_mysql_slave_data
```

## 9. 常见问题排查

### 9.1 在 IDEA 中运行后端提示 `mysql-master` 无法解析

如果你不是在 Docker 中运行后端，而是在 IDEA 中直接运行，那么 `mysql-master` 这种容器内主机名对本机 Java 进程无效。

此时应改用宿主机端口：

- MySQL 主库：`127.0.0.1:3307`
- MySQL 从库：`127.0.0.1:3308`
- Redis：`127.0.0.1:6379`
- Kafka：`127.0.0.1:9092`

### 9.2 前端点击下单按钮但后端没有日志

建议检查以下内容：

- 页面是否从 `http://localhost/...` 打开
- Nginx 是否在修改配置后已重启
- `/api/seckill/orders` 是否已正确代理到 `backend-order`
- 浏览器开发者工具中请求的真实 URL 和返回内容

### 9.3 订单一直停留在 `QUEUED`

建议检查：

- Kafka 是否正常运行
- `seckill-order-topic` 是否存在
- `backend-order` 日志中消费者是否正常启动
- Kafka 与 `backend-order` 是否已经按最新代码重新构建部署

### 9.4 旧数据卷中没有 `seckill_orders`

说明当前 MySQL 数据卷是在新增该表之前创建的。

可以选择：

- 在 `mysql-master` 中手动执行建表语句
- 删除旧 volume 后重新初始化数据库

## 10. 推荐部署顺序

```bash
cd /home/Distributed-Software

# 1. 构建两个后端
cd Back_End/Log && mvn clean package -DskipTests
cd ../Order && mvn clean package -DskipTests
cd /home/Distributed-Software

# 2. 启动所有服务
docker compose up -d --build

# 3. 检查运行状态
docker compose ps
```

部署完成后的访问入口如下：

- 前端首页：`http://localhost`
- 登录/商品后端直连：`http://localhost:8081`、`http://localhost:8082`
- 秒杀订单后端直连：`http://localhost:8083`
- MySQL 主库：`127.0.0.1:3307`
- MySQL 从库：`127.0.0.1:3308`
- Redis：`127.0.0.1:6379`
- Kafka：`127.0.0.1:9092`
