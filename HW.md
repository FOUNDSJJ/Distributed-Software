# 第一次作业

## 系统设计文档

### 技术栈选型说明
当前仓库已经可以明确看出后端、数据库、缓存和前端的基础技术选型。后端使用 `Spring Boot 3 + MyBatis`，数据库使用 `MySQL`，缓存与会话使用 `Redis`，前端为原生 `HTML + CSS + JavaScript`，反向代理与静态资源承载使用 `Nginx`。

相关文件或目录：
- `Back_End/Log/pom.xml`
- `Back_End/Log/src/main/resources/application.properties`
- `Front_End/`
- `nginx/conf.d/default.conf`
- `docker-compose.yml`

实现说明：
- `pom.xml` 中已经引入 `spring-boot-starter-web`、`mybatis-spring-boot-starter`、`mysql-connector-j`、`spring-boot-starter-security`、`spring-boot-starter-data-redis`。
- `application.properties` 中已经配置 MySQL、Redis、MyBatis 和服务端口。
- 前端页面与脚本位于 `Front_End/`，说明项目采用前后端分离的基础结构。

## 环境准备

### 初始化项目代码仓库（Git）
仓库已经完成 Git 初始化，项目根目录存在 `.git`、`.gitignore` 等版本管理文件。

相关文件或目录：
- `.git/`
- `.gitignore`
- `.gitattributes`

### 搭建基础开发环境（Spring Boot + MyBatis + MySQL）
仓库中已经搭建出可运行的 Java 后端工程，并完成了数据库脚本、Mapper、配置文件和打包产物的组织。

相关文件或目录：
- `Back_End/Log/`
- `Back_End/Log/pom.xml`
- `Back_End/Log/src/main/resources/application.properties`
- `Back_End/Log/src/main/resources/mapper/`
- `Database/Log_SQL/init_users_table.sql`
- `Database/Product_SQL/init_products_table.sql`

实现说明：
- `Back_End/Log/` 是 Maven 工程，具备标准的 `src/main/java`、`src/main/resources` 结构。
- `application.properties` 完成了数据库连接、Redis 连接、MyBatis 映射位置、服务端口等配置。
- `Database/` 下保存了用户表、商品表初始化 SQL，说明本地开发环境依赖的数据库结构已经准备。

### 搭建一个项目代码框架，实现简单的用户注册登陆功能
该功能已经完成，包含前端注册页/登录页、后端注册/登录接口、数据库持久化、密码加密和基于 Redis 的会话管理。

相关文件或目录：
- `Front_End/LogUp.html`
- `Front_End/LogUp.js`
- `Front_End/LogIn.html`
- `Front_End/LogIn.js`
- `Back_End/Log/src/main/java/com/example/auth/controller/AuthController.java`
- `Back_End/Log/src/main/java/com/example/auth/service/UserService.java`
- `Back_End/Log/src/main/java/com/example/auth/service/SessionService.java`
- `Back_End/Log/src/main/java/com/example/auth/util/PasswordUtil.java`
- `Back_End/Log/src/main/java/com/example/auth/mapper/UserMapper.java`
- `Back_End/Log/src/main/resources/mapper/UserMapper.xml`
- `Database/Log_SQL/init_users_table.sql`

实现说明：
- 前端通过 `LogUp.html + LogUp.js` 发起注册请求，通过 `LogIn.html + LogIn.js` 发起登录请求。
- `AuthController` 提供 `/api/auth/register`、`/api/auth/login`、`/api/auth/me`、`/api/auth/logout` 接口。
- `UserService` 负责用户名/手机号唯一性校验、密码加密、登录校验和最后登录时间更新。
- `PasswordUtil` 使用 BCrypt 处理密码哈希，避免明文存储密码。
- `SessionService` 将登录态写入 Redis，并通过 `SESSIONID` Cookie 维持会话。

# 第二次作业

## 容器环境

### 配置项目的 docker-compose 文件，将数据库、后端服务、Nginx 分别使用容器进行启动加载
仓库中已经提交 `docker-compose.yml`，其中包含 `mysql`、`redis`、`backend1`、`backend2`、`nginx` 五个服务；同时仓库中还提供了 `Docker-Deployment.md` 说明容器部署过程。

相关文件或目录：
- `docker-compose.yml`
- `Docker-Deployment.md`
- `nginx/conf.d/default.conf`

实现说明：
- `docker-compose.yml` 中配置了数据库容器、Redis 容器、两个后端实例和 Nginx 容器。
- Nginx 挂载 `Front_End/` 作为静态资源目录，挂载 `nginx/conf.d/` 作为代理配置目录。
- 结合仓库现状看，容器编排配置已经提交；但当前仓库文件列表中未看到独立 `Dockerfile` 文件，因此这部分更准确地说是“已完成容器编排配置与部署说明”。

## 负载均衡

### 后端服务启动多个实例分别开启不同 Rest 端口（如 8081 和 8082 端口）
该部分已完成配置。

相关文件或目录：
- `docker-compose.yml`

实现说明：
- `backend1` 对外映射 `8081:9090`。
- `backend2` 对外映射 `8082:9090`。
- 两个实例复用同一套后端工程，用于模拟多实例部署。

### 通过 Nginx（如 80 端口）进行代理和转发
该部分已完成配置。

相关文件或目录：
- `nginx/conf.d/default.conf`
- `docker-compose.yml`

实现说明：
- Nginx 监听 `80` 端口。
- `/api/` 路径通过 `proxy_pass` 转发到后端 upstream。
- 转发时保留了 `Host`、`X-Real-IP`、`X-Forwarded-For` 等头信息。

### 尝试为 Nginx 配置不同的负载均衡算法
该部分已完成配置。

相关文件或目录：
- `nginx/conf.d/default.conf`

实现说明：
- 已配置 `backend_round_robin`。
- 已配置 `backend_least_conn`。
- 已配置 `backend_ip_hash`。
- 当前 `location /api/` 默认使用的是 `backend_round_robin`，其余算法可通过修改 `proxy_pass` 指向进行切换测试。

### 使用 JMeter 进行压力测试
仓库中已经保留 JMeter 压测结果截图与说明材料。

相关文件或目录：
- `README.md`
- `graph/back_test.png`
- `graph/back_test1.png`

实现说明：
- `README.md` 中记录了后端接口压测思路。
- `graph/back_test.png`、`graph/back_test1.png` 为压测结果截图，可作为展示材料。

### 观察响应时间，并检查后端日志，验证各后端处理的请求数是否大致相等
仓库中已有对应说明材料。

相关文件或目录：
- `README.md`
- `Docker-Deployment.md`

实现说明：
- 文档中已经给出通过 `docker compose logs -f backend1 backend2` 查看后端日志的方式。
- 结合多实例和轮询策略，可以观察请求是否被较均匀地分发到两个后端实例。

## 动静分离

### 写一个简单的前端 Html 文件，可以包括 css、js 等
该部分已完成，且不仅有单个页面，还包含首页、注册页、登录页和商品页。

相关文件或目录：
- `Front_End/index.html`
- `Front_End/LogIn.html`
- `Front_End/LogUp.html`
- `Front_End/Products/shop.html`
- `Front_End/assets/css/`
- `Front_End/LogIn.js`
- `Front_End/LogUp.js`
- `Front_End/Products/shop.js`
- `Front_End/Products/query.js`

实现说明：
- `Front_End/` 下已经组织出完整静态页面资源。
- 页面样式、图片、脚本资源拆分明确，便于直接由 Nginx 提供静态访问。

### 在 Nginx 中配置动静分离
该部分已完成配置。

相关文件或目录：
- `nginx/conf.d/default.conf`
- `Front_End/`

实现说明：
- `/` 路径直接返回静态页面，根目录指向 `/usr/share/nginx/html`。
- `/api/` 路径代理到后端服务。
- `/static/` 路径配置了静态资源缓存时间 `expires 1h`。
- 这说明仓库已经按“静态资源由 Nginx 提供，动态请求转发后端”的方式进行了动静分离。

### 使用 JMeter 分别压测静态文件，以及后端服务，观察响应时间
仓库中已有对应截图和说明材料。

相关文件或目录：
- `README.md`
- `graph/front_test.png`
- `graph/back_test.png`
- `graph/back_test1.png`

实现说明：
- `graph/front_test.png` 对应静态页面压测结果。
- `graph/back_test.png`、`graph/back_test1.png` 对应后端接口压测结果。
- `README.md` 中也对静态资源和后端接口的压测方法进行了说明。

## 分布式缓存

### 引入 Redis 缓存，实现商品详情页缓存
该部分已完成。

相关文件或目录：
- `Back_End/Log/pom.xml`
- `Back_End/Log/src/main/resources/application.properties`
- `Back_End/Log/src/main/java/com/example/auth/controller/ProductController.java`
- `Back_End/Log/src/main/java/com/example/auth/service/ProductService.java`
- `Back_End/Log/src/main/java/com/example/auth/service/JsonUtil.java`
- `Back_End/Log/src/main/java/com/example/auth/mapper/ProductMapper.java`
- `Back_End/Log/src/main/resources/mapper/ProductMapper.xml`
- `Database/Product_SQL/init_products_table.sql`
- `Front_End/Products/shop.html`
- `Front_End/Products/query.js`

实现说明：
- `ProductController` 提供 `/api/products/{id}` 和 `/api/products/by-name` 等接口。
- `ProductService#getProductById` 优先从 Redis 读取商品详情，缓存未命中时回源 MySQL，再将结果写回 Redis。
- 前端商品页面和查询脚本可以访问商品查询接口，形成“商品详情页缓存”的业务链路。

### 处理缓存穿透、击穿、雪崩问题
该部分已完成。

相关文件或目录：
- `Back_End/Log/src/main/java/com/example/auth/service/ProductService.java`
- `README.md`

实现说明：
- 缓存穿透：对不存在的商品写入 `NULL` 占位值，并设置 2 分钟过期时间。
- 缓存击穿：使用 `setIfAbsent` 实现互斥锁 `lock:product:{id}`，防止热点 Key 同时回源。
- 缓存雪崩：商品缓存 TTL 使用 30 到 40 分钟的随机值，避免大量 Key 同时失效。

# 第三次作业

## 分布式缓存

### 引入 Redis，实现商品详情页缓存
该部分与第二次作业中的商品缓存实现一致，已经落在当前代码仓库中。

相关文件或目录：
- `Back_End/Log/src/main/java/com/example/auth/controller/ProductController.java`
- `Back_End/Log/src/main/java/com/example/auth/service/ProductService.java`
- `Back_End/Log/src/main/java/com/example/auth/service/JsonUtil.java`
- `Back_End/Log/src/main/resources/application.properties`
- `Back_End/Log/pom.xml`

### 处理缓存穿透、击穿、雪崩问题
该部分已经在当前实现中完成。

相关文件或目录：
- `Back_End/Log/src/main/java/com/example/auth/service/ProductService.java`
- `README.md`

实现说明：
- 穿透、击穿、雪崩三类问题的处理逻辑均集中在 `ProductService#getProductById` 中。
- `README.md` 也对对应策略进行了总结，便于老师和助教快速核对。

## 读写分离

## ElasticSearch，实现商品搜索功能（可选）
