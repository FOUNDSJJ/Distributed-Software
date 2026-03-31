document.getElementById('LogInForm').addEventListener('submit', function(e) {
    e.preventDefault(); // 阻止表单默认提交

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('pwd').value;
    const remember = document.getElementById('remember').checked;

    // 先做基础校验，避免无效登录请求发送到后端
    if (!username || !password) {
        alert("用户名和密码不能为空！");
        return;
    }

    fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include', // 允许浏览器自动保存并携带会话 Cookie
        body: JSON.stringify({ username, password })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert('登录成功！');

            // 登录成功后按用户选择保存信息
            if (remember) {
                // 持久化保存用户信息
                localStorage.setItem('user', JSON.stringify(data.user));
            } else {
                // 当前会话内临时保存用户信息
                sessionStorage.setItem('user', JSON.stringify(data.user));
            }

            // 会话凭证由浏览器维护，这里只负责登录后的页面跳转
            window.location.replace("./Products/shop.html");

        } else {
            alert('登录失败：' + (data.message || '未知错误'));
        }
    })
    .catch(err => {
        // 统一处理网络异常和服务端异常
        console.error('网络或服务器错误：', err);
        alert('网络或服务器错误，请重试');
    });
});
