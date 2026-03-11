# 🛒 商品库存与秒杀系统

> 一个面向商品管理与秒杀场景的 Web 系统，支持用户注册、登录、主页浏览与交互式数据查看，适合作为课程设计、软件工程项目展示与前后端协同开发实践。

---

## ✨ 项目亮点

- 🧭 **界面直观**：围绕商品库存与秒杀业务场景进行页面设计，整体结构清晰、上手容易。
- 🔐 **用户体系完整**：提供注册与登录入口，覆盖基础账户操作流程。
- 📊 **交互体验良好**：支持点击图表查看详细数据，便于展示库存与活动信息。
- 🧱 **适合项目展示**：包含主页、注册页、登录页等核心界面，便于课程汇报与仓库展示。

---

## 🚀 快速开始

```bash
git clone git@github.com:FOUNDSJJ/Distributed-Software.git
```

---

## 🐳 部署说明

### 📦 Docker 部署

🛠 **推荐先阅读部署文档**  
详细部署步骤请参考[Docker-Deployment.md](./Docker-Deployment.md)文件。

<!-- - 👤 **CMD（以管理员身份打开，需要准备Nodejs以及Npm环境）**

  （1）在前端根目录下新建```server.js```，加入以下内容并根据自己电脑配置修改其中的参数。
  ```js
  const http = require('http');
  const serveStatic = require('serve-static');
  const { createProxyMiddleware } = require('http-proxy-middleware');
  const os = require('os');

  // ----------------------
  // 静态文件服务
  // ----------------------
  const staticHandler = serveStatic('./', {
    index: ['index.html'], //index.html为导航页相对路径
    setHeaders: (res, path) => {
      res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
      res.setHeader('Pragma', 'no-cache');
      res.setHeader('Expires', '0');
    }
  });

  // ----------------------
  // 代理映射配置
  // ----------------------
  const proxyMap = [
    {
      prefix: '/auth',                       // 前端请求以 /auth 开头
      target: '后端ip + auth对应的端口',  // 后端 IP + 端口
    },
    // 可以继续添加其他后端映射，例如：
    // {
    //   prefix: '/other',
    //   target: '后端ip + other对应的端口',
    // }
  ];

  // ----------------------
  // 创建 HTTP 服务器
  // ----------------------
  const server = http.createServer((req, res) => {
    // 检查请求是否匹配代理前缀
    const matched = proxyMap.find(item => req.url.startsWith(item.prefix));

    if (matched) {
      // 动态创建代理中间件
      const proxy = createProxyMiddleware({
        context: matched.prefix,  // 匹配路径
        target: matched.target,   // 后端地址
        changeOrigin: true,       // 修改请求头 origin 为 target
        logLevel: 'debug',        // 开启调试日志
      });
      return proxy(req, res, () => {});
    }

    // 如果没有匹配代理，作为静态文件处理
    staticHandler(req, res, () => {
      res.statusCode = 404;
      res.end('Not found');
    });
  });

  // ----------------------
  // 启动服务器
  // ----------------------
  const PORT = 3000; //3000为前端端口，可以改成其他可以使用的端口
  server.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running at:`);
    console.log(`- Local:   http://localhost:${PORT}`);
    console.log(`- Network: http://${getLocalIP()}:${PORT}`);
  });

  // ----------------------
  // 获取局域网 IP
  // ----------------------
  function getLocalIP() {
    const nets = os.networkInterfaces();
    for (const name of Object.keys(nets)) {
      for (const net of nets[name]) {
        if (net.family === 'IPv4' && !net.internal) {
          return net.address;
        }
      }
    }
    return '0.0.0.0';
  }
  ```

  （2）cmd当中依次输入以下指令：
  ```cmd
  //切换到前端文件夹
  cd Front_End

  //初始化nodejs的package.json
  npm init -y

  //安装http-server、serve-static以及http-proxy-middleware功能
  npm install http-server serve-static http-proxy-middleware

  //将前端运行起来
  node server.js
  ```

- 🖱 **访问前端**
  浏览器输入```http://localhost:3000```即可

### 🔹 数据库创建（Mysql+Java）

- 👤 **CMD（创建数据库）**
  ```cmd
  mysql -u root -p

  CREATE DATABASE Distributed-Software; 

  USE Distributed-Software;

  mysql> Back_End/Database/create_users_table.sql
  ```

- 👤 **Java（创建数据库）**
  先通过CMD方式在Mysql当中创建数据库```Distributed-Software```，再通过```IDEA```运行```RunCreateUsersTable.java```直接创建表格，注意需要将程序当中的```password```更换为```mysql```当中```root```用户的密码。

*** -->

## 🖼️ 界面预览

### 🏠 项目主页
![主页预览](graph/index.png)

**页面特性：**
- 🪪 **账户入口清晰**：首页集成注册与登录入口，便于用户快速进入系统。
- 📈 **数据展示直观**：通过图表呈现业务信息，增强内容可读性。
- 🖱️ **支持交互查看**：用户可点击图表查看更详细的数据内容。

---

### 📝 注册页面
![注册页面](graph/logup.png)

**页面特性：**
- 👤 **注册流程完整**：提供用户名、邮箱、密码等基础输入项，便于创建新账户。
- ✅ **表单反馈明确**：支持常见校验与提示信息，提升注册体验。
- 🔁 **页面跳转自然**：可从注册页快速切换至登录页。
- 🎨 **视觉风格统一**：背景与页面元素具备一定科技感，增强整体表现力。

---

### 🔐 登录页面
![登录页面](graph/login.png)

**页面特性：**
- 🔑 **登录入口直接**：支持用户输入账户信息后快速完成登录。
- 🛡️ **安全感更强**：密码隐藏输入等设计有助于提升使用体验与账户安全感。
- 🔄 **支持注册跳转**：新用户可从登录页便捷跳转至注册页。
- ⚡ **操作路径简洁**：按钮突出，交互流程清晰。
- 🖼️ **设计语言一致**：与注册页面保持统一风格，整体观感协调。

---

## 🧪 测试说明

为验证系统在实际运行中的可用性与稳定性，可使用 **Apache JMeter** 对前端静态页面与后端接口功能进行基础测试、并发测试与压力测试。

### 🔍 前端静态页面测试

![前端静态页面测试](./graph/front_test.png)

前端页面主要包括主页、注册页、登录页等静态资源页面，可通过 JMeter 对页面访问性能进行测试。

**测试目标：**
- 🌐 验证静态页面是否能够被正常访问
- ⚡ 观察页面在并发访问下的响应时间表现
- 📦 检查 HTML、CSS、JS、图片等资源的加载情况
- 📈 为前端页面展示性能提供数据支持

**测试方法：**
- 在 JMeter 中创建 **Thread Group**
- 添加 **HTTP Request**，分别请求前端页面地址，例如：
  - `http://localhost/`
  - `http://localhost/login.html`
  - `http://localhost/logup.html`
- 可根据需要设置并发用户数、循环次数与启动时间
- 添加监听器查看测试结果，例如：
  - **View Results Tree**
  - **Summary Report**
  - **Aggregate Report**

**可关注指标：**
- 平均响应时间
- 吞吐量
- 错误率
- 最大/最小响应时间

### 🛠️ 后端功能测试

![后端功能测试](./graph/back_test.png)

后端功能测试主要面向注册、登录、用户验证及相关业务接口，重点验证接口正确性与并发处理能力。

**测试目标：**
- 🔐 验证注册、登录等核心接口是否可正常工作
- 📨 检查请求参数与返回结果是否符合预期
- 🚦 测试后端在多用户并发访问下的稳定性
- 🗄️ 验证系统与数据库交互是否正常

**测试方法：**
- 在 JMeter 中创建 **Thread Group**
- 使用 **HTTP Request** 对后端接口发送 `GET` 或 `POST` 请求
- 若接口为 JSON 提交，可在线程组中添加：
  - **HTTP Header Manager**
  - `Content-Type: application/json`
- 在请求体中填写接口参数，例如用户名、邮箱、密码等
- 可结合 **CSV Data Set Config** 批量导入测试账号，模拟多用户请求

**示例测试场景：**
- 👤 用户注册接口测试
- 🔑 用户登录接口测试
- ✅ 非法输入校验测试
- 🔁 多用户并发登录测试
- 📊 秒杀相关接口的高并发访问测试

### 📋 测试结果分析

![后端功能测试](./graph/back_test1.png)

完成测试后，可从以下几个维度对系统表现进行分析：

- **功能正确性**：接口是否返回正确结果，页面是否能正常访问
- **性能表现**：响应时间是否稳定，吞吐量是否满足预期
- **稳定性**：高并发场景下是否出现报错、超时或服务异常
- **可优化点**：是否存在静态资源加载慢、接口响应慢、数据库压力过大等问题

### 💡 测试建议

- 建议先进行小规模功能验证，再逐步增加并发量
- 前端静态页面测试适合用于验证资源访问性能
- 后端接口测试更适合评估业务逻辑与服务承载能力
- 若系统包含秒杀业务，建议重点设计高并发压测场景
- 可将 JMeter 测试结果截图或导出报表用于课程答辩与项目展示

---

## 📌 适用场景

- 🎓 分布式软件原理与技术作业展示
- 💻 前后端分离项目练习
- 📦 商品库存管理系统原型演示
- ⏰ 秒杀业务场景页面展示

---

> 🚀 通过项目主页、注册页与登录页的完整展示，你可以快速了解本系统在库存管理与秒杀场景下的基础功能与交互设计。