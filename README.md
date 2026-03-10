# 🛒 商品库存与秒杀系统

欢迎来到**商品库存与秒杀系统**！本主页设计简洁直观，方便用户浏览商品信息、库存状态和秒杀活动。


***


## 🔍 本地部署方法

### 🔹 快速开始

  ```bash
  git clone git@github.com:FOUNDSJJ/Distributed-Software.git
  ```

### 🔹 Docker部署

👤 **CMD（以管理员身份打开，需要准备Nodejs以及Npm环境）**

  参考[Docker-Deployment.md](./Docker-Deployment.md)文件。

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


## 🔍 项目主页预览
![主页预览](graph/index.png)

## 🔹 页面功能

- 👤 **用户操作入口**
  包含注册（注册📝）和登录（登录🔑）入口，方便用户参与活动。

- 🖱 **交互操作**
  用户可点击图表查看详细数据，操作直观便捷。


***


## 🔍 注册页面预览
![注册页面](graph/logup.png)

## 🔹 页面功能

- 📝 **用户注册表单**
  提供用户名、邮箱、密码等输入框，用户可以创建新账户。

- ✅ **验证与提示**
  支持表单验证（如邮箱格式、密码强度），注册错误或成功会有提示。

- 🔄 **切换到登录**
  提供登录入口，方便已有账户用户跳转到登录页面。

- 🖼 **视觉背景**
  背景包含图形或数据元素，让页面更具科技感和品牌风格。


***


## 🔍 登录页面预览
![登录页面](graph/login.png)

## 🔹 页面功能

- 🔑 **用户登录表单**
  提供用户名/邮箱和密码输入框，用户可直接登录账户。

- 🛡 **安全验证**
  支持密码隐藏输入、可能有验证码或安全提示，保障账户安全。

- 🔄 **切换到注册**
  提供注册入口，方便新用户创建账户。

- ⚡ **快速操作按钮**
  登录按钮突出，用户可快速提交信息。

- 🖼 **视觉背景**
  背景设计与注册页面一致，保持整体风格统一。


***


> 🚀 快速体验主页，直观了解库存与秒杀系统功能！