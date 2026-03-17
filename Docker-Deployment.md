# Docker项目一键部署指南

## 1. 目录结构示例

假设项目目录 `/home/Distributed-Software` 下：

```text
Distributed-Software/
│
├─ Log/                   # 后端 Spring Boot jar 包
│   ├─ target/Log-1.0-SNAPSHOT.jar
│   └─ Dockerfile
│
├─ Front_End/                  # 前端打包后的静态文件
│   └─ **/
│
├─ Database/                  # 数据库初始化 SQL
│   └─ Log_SQL/init_users_table.sql
│
├─ nginx
│   └─ conf.d/default.conf
│
└─ docker-compose.yml         # Docker Compose 配置
```

## 2. 数据库初始化

### 2.1 SQL文件内容示例（init_users_table.sql）

```sql
CREATE DATABASE IF NOT EXISTS distributed_software
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE distributed_software;

-- 创建用户表
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户唯一ID',
    `username` VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    `phone_number` VARCHAR(20) UNIQUE NOT NULL COMMENT '手机号（唯一）',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '信息更新时间',
    `last_login` TIMESTAMP NULL DEFAULT NULL COMMENT '最近登录时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态: 0=禁用,1=启用',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 可选：插入测试用户
INSERT INTO users (username, phone_number, password_hash, status)
VALUES ('admin', '13800000000', 'admin123', 1);
```


### 2.1 SQL文件内容示例（init_products_table.sql）

```sql
CREATE DATABASE IF NOT EXISTS distributed_software
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE distributed_software;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    name VARCHAR(255) NOT NULL COMMENT '商品名称',
    price DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    description TEXT COMMENT '商品描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';
```

### 2.3 导入 SQL 到 Docker MySQL 容器

```bash
# 假设 MySQL 容器名为 mysql，root 密码是 Password
docker exec -i mysql mysql -uroot -pPassword < /home/Distributed-Software/Database/Log_SQL/init_users_table.sql
docker exec -i mysql mysql -uroot -pPassword < /home/Distributed-Software/Database/Product_SQL/init_products_table.sql
```

### 2.4 验证表是否创建成功

```bash
docker exec -it mysql mysql -uroot -pPassword -e "USE distributed_software; SHOW TABLES;"
```

### 2.5 通过csv导入表格商品数据

```bash
# 将数据文件导入mysql容器
docker cp /path/to/your/csv mysql:/var/lib/mysql-files/data.csv

# 进入mysql容器
docker exec -it mysql bash

# 密码登录
mysql -u root -pPassword

USE distributed_software

# 将数据导入，并将数据每一项和表格属性一一对应
LOAD DATA INFILE '/var/lib/mysql-files/data.csv' INTO TABLE products FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\n' (name, price, stock, description);
```

## 3. Docker 配置
### 3.1 Dockerfile

```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY target/Log-1.0-SNAPSHOT.jar app.jar

EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
```
### 3.2 docker-compose.yml示例

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: Password
      MYSQL_DATABASE: distributed_software
      TZ: Asia/Shanghai
    command:
      --default-authentication-plugin=mysql_native_password
    ports:
      - "3307:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./mysql-init:/docker-entrypoint-initdb.d

  redis:
    image: redis:7
    container_name: redis
    restart: always
    ports:
      - "6379:6379"

  backend1:
    build:
      context: ./Log
    container_name: backend1
    restart: always
    environment:
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_DB: distributed_software
      MYSQL_USERNAME: root
      MYSQL_PASSWORD: Password
      REDIS_HOST: redis
      REDIS_PORT: 6379
    depends_on:
      - mysql
      - redis
    ports:
      - "8081:9090"

  backend2:
    build:
      context: ./Log
    container_name: backend2
    restart: always
    environment:
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_DB: distributed_software
      MYSQL_USERNAME: root
      MYSQL_PASSWORD: Password
      REDIS_HOST: redis
      REDIS_PORT: 6379
    depends_on:
      - mysql
      - redis
    ports:
      - "8082:9090"

  nginx:
    image: nginx:latest
    container_name: nginx
    restart: always
    depends_on:
      - backend1
      - backend2
    ports:
      - "80:80"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d
      - ./Front_End:/usr/share/nginx/html

volumes:
  mysql_data:
```

### 3.3 default.conf示例

```yaml
upstream backend_round_robin {
    server backend1:9090;
    server backend2:9090;
}

upstream backend_least_conn {
    least_conn;
    server backend1:9090;
    server backend2:9090;
}

upstream backend_ip_hash {
    ip_hash;
    server backend1:9090;
    server backend2:9090;
}

server {
    listen 80;
    server_name localhost;

    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location /static/ {
        root /usr/share/nginx/html;
        expires 1h;
    }

    location /api/ {
        proxy_pass http://backend_round_robin;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## 4. 常用部署命令

```bash
# 1. 构建所有服务镜像（第一次或更新 jar 后使用）
docker compose build --no-cache

# 2. 启动所有服务
docker compose up -d

# 3. 查看容器状态
docker compose ps

# 4. 查看后端实时日志
docker compose logs -f backend1 backend2

# 5. 导入数据库 SQL 文件
docker exec -i mysql mysql -uroot -pPassword < /home/Distributed-Software/Database/Log_SQL/init_users_table.sql

# 6. 进入 mysql 容器
docker exec -it mysql mysql -uroot -pPassword

# 7. 停止所有服务
docker compose down

# 8. 重启服务（WSL 关闭后再次部署）
docker compose up -d

# 9. 清理旧数据（可选）
docker volume rm distributed-software_mysql_data

# 10. 导出jar包
mvn clean package

# 11. 更换jar包（需要重新构建docker容器）
docker compose down
docker compose build
```

## 5. 部署顺序说明

- 准备 SQL 文件：包含数据库、表结构、初始数据。

- 启动 MySQL 和 Redis：docker compose up -d mysql redis 或直接 docker compose up -d。

- 导入 SQL 文件：确保数据库和表结构完整。

- 构建后端镜像：docker compose build --no-cache。

- 启动后端和前端：docker compose up -d。

- 验证服务：

    - 浏览器访问前端：```http://localhost```

    - 后端端口：```http://localhost:8081```、```http://localhost:8082```

    - 数据库表：```docker exec -it mysql mysql -uroot -pPassword -e "USE distributed_software; SHOW TABLES;"```

- 后续操作：

    - 修改 jar 或前端静态文件后，重新 build 并 up。

    - WSL 重启后，直接 ```docker compose up -d``` 启动即可。

## 6. 下次重新部署最简命令

```bash 
cd /home/Distributed-Software

docker compose up -d
```