# Docker Deployment Guide

## 1. Project Structure

Deploy the project from the repository root, for example:

```text
/home/Distributed-Software
```

Current backend and deployment layout:

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

## 2. Service Overview

The current deployment includes:

- `mysql-master`: MySQL primary database
- `mysql-slave`: MySQL replica database
- `mysql-replica-init`: initializes master-slave replication
- `redis`: cache, session, and seckill stock state
- `kafka`: asynchronous order queue
- `backend1`: `Back_End/Log` instance 1
- `backend2`: `Back_End/Log` instance 2
- `backend-order`: `Back_End/Order` seckill order service
- `nginx`: static frontend hosting and API reverse proxy

Port mapping:

- `3307 -> mysql-master:3306`
- `3308 -> mysql-slave:3306`
- `6379 -> redis:6379`
- `9092 -> kafka:9092`
- `8081 -> backend1:9090`
- `8082 -> backend2:9090`
- `8083 -> backend-order:9091`
- `80 -> nginx:80`

## 3. Database Initialization

### 3.1 Database Name

All services use the same database:

```sql
distributed_software
```

### 3.2 Tables

The master initialization script is:

[`Database/MySQL_RW/master/init.sql`](/d:/Private/Grade_3/Distributed-Software-HW/Database/MySQL_RW/master/init.sql)

It creates the following tables:

- `users`
- `products`
- `seckill_orders`

The `seckill_orders` table supports:

- snowflake-style long order IDs
- one user can seckill one product only once
- order lookup by `order_no`
- order lookup by `user_id`

### 3.3 Important Note About Existing Volumes

`init.sql` is only executed automatically when the MySQL data volume is created for the first time.

If you added `seckill_orders` after MySQL had already been started before, you must create it manually in the master database:

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

Replication will sync the table from master to slave if replication is working normally.

### 3.4 Check Tables

Check tables in the master database:

```bash
docker exec -it mysql-master mysql -uroot -pPassword -e "USE distributed_software; SHOW TABLES;"
```

Check tables in the slave database:

```bash
docker exec -it mysql-slave mysql -uroot -pPassword -e "USE distributed_software; SHOW TABLES;"
```

## 4. Backend Build

Build the two backend jars before building Docker images.

### 4.1 Build `Log`

```bash
cd /home/Distributed-Software/Back_End/Log
mvn clean package -DskipTests
```

### 4.2 Build `Order`

```bash
cd /home/Distributed-Software/Back_End/Order
mvn clean package -DskipTests
```

### 4.3 Dockerfiles

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

## 5. Docker Compose

Current deployment file:

[`docker-compose.yml`](/d:/Private/Grade_3/Distributed-Software-HW/docker-compose.yml)

Key points:

- `backend1` and `backend2` are built from `./Back_End/Log`
- `backend-order` is built from `./Back_End/Order`
- Kafka uses `apache/kafka:3.7.1`
- `backend-order` depends on MySQL, Redis, and Kafka
- Nginx serves the frontend and proxies API requests

Start all services:

```bash
cd /home/Distributed-Software
docker compose up -d --build
```

Start infrastructure only:

```bash
docker compose up -d mysql-master mysql-slave mysql-replica-init redis kafka
```

Start only the order backend after rebuilding:

```bash
docker compose up -d --build backend-order
```

## 6. Nginx Reverse Proxy

Current Nginx config:

[`nginx/conf.d/default.conf`](/d:/Private/Grade_3/Distributed-Software-HW/nginx/conf.d/default.conf)

Important routes:

- `/api/` -> load balance to `backend1` and `backend2`
- `/api/seckill/orders` -> route to `backend-order`

Relevant config:

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

After modifying Nginx config:

```bash
docker compose restart nginx
```

## 7. Seckill Order Flow

### 7.1 Current Frontend Request

The frontend product page sends:

```json
{
  "product_name": "无线鼠标"
}
```

The request is sent to:

```text
POST /api/seckill/orders
```

with browser cookies included.

### 7.2 Current Backend Behavior

The order backend:

- reads `SESSIONID` from cookie
- resolves the current logged-in user from Redis
- finds the product by `product_name`
- uses Redis to do stock deduction and duplicate-order guard
- pushes order creation to Kafka
- creates the order asynchronously in MySQL

### 7.3 Order Query

Query by order ID:

```text
GET /api/seckill/orders/{orderId}
```

Query by user ID:

```text
GET /api/seckill/orders?userId=1
```

## 8. Common Commands

### 8.1 Check Running Containers

```bash
docker compose ps
```

### 8.2 Check Backend Logs

```bash
docker compose logs -f backend1 backend2 backend-order
```

### 8.3 Check Kafka Logs

```bash
docker logs kafka --tail 200
```

### 8.4 Check Nginx Logs

```bash
docker logs nginx --tail 100
```

### 8.5 Restart a Single Service

```bash
docker compose restart backend-order
docker compose restart nginx
```

### 8.6 Stop All Services

```bash
docker compose down
```

### 8.7 Rebuild Without Cache

```bash
docker compose build --no-cache
```

### 8.8 Remove Old Volumes

Use this only when you want to reinitialize MySQL data completely:

```bash
docker compose down
docker volume rm distributed-software_mysql_master_data
docker volume rm distributed-software_mysql_slave_data
```

## 9. Troubleshooting

### 9.1 `mysql-master` Cannot Be Resolved in IDEA

If you run backend services directly in IDEA, `mysql-master` is not a valid host outside Docker Compose.

Use local ports instead:

- MySQL master: `127.0.0.1:3307`
- MySQL slave: `127.0.0.1:3308`
- Redis: `127.0.0.1:6379`
- Kafka: `127.0.0.1:9092`

### 9.2 Frontend Clicks But Order Backend Has No Log

Check:

- page is opened from `http://localhost/...`
- Nginx has been restarted after config changes
- `/api/seckill/orders` is correctly proxied to `backend-order`
- browser developer tools show the real request URL and response

### 9.3 Order Stays in `QUEUED`

Check:

- Kafka is running
- `seckill-order-topic` exists
- `backend-order` logs show consumer startup
- Kafka and order service were rebuilt after code changes

### 9.4 Existing MySQL Volume Does Not Contain `seckill_orders`

That means MySQL was initialized before the table was added to `init.sql`.

Create the table manually in `mysql-master`, or delete the old volumes and rebuild the database.

## 10. Recommended Deployment Sequence

```bash
cd /home/Distributed-Software

# 1. build backend jars if needed
cd Back_End/Log && mvn clean package -DskipTests
cd ../Order && mvn clean package -DskipTests
cd /home/Distributed-Software

# 2. start all services
docker compose up -d --build

# 3. check status
docker compose ps
```

Access points after deployment:

- frontend: `http://localhost`
- log backend direct access: `http://localhost:8081` and `http://localhost:8082`
- order backend direct access: `http://localhost:8083`
- MySQL master: `127.0.0.1:3307`
- MySQL slave: `127.0.0.1:3308`
- Redis: `127.0.0.1:6379`
- Kafka: `127.0.0.1:9092`
