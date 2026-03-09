document.getElementById('LogInForm').addEventListener('submit', function(e) {
    e.preventDefault(); // 阻止表单默认提交

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('pwd').value;
    const remember = document.getElementById('remember').checked;

    // 简单前端校验
    if (!username || !password) {
        alert("用户名和密码不能为空！");
        return;
    }

    fetch('auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include', // ✅ 关键：允许跨域 Cookie 保存
        body: JSON.stringify({ username, password })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert('登录成功！');

            // ----------------- 存储用户信息 -----------------
            if (remember) {
                // 记住密码/用户信息 → localStorage
                localStorage.setItem('user', JSON.stringify(data.user));
            } else {
                // 临时登录 → sessionStorage
                sessionStorage.setItem('user', JSON.stringify(data.user));
            }

            // ----------------- Cookie 已经由浏览器保存 -----------------
            // 后端返回的 SESSIONID Cookie 会自动被浏览器保存（HttpOnly）
            // JS 无法读取，但浏览器会在后续 fetch 请求自动携带

            // 登录成功跳转
            window.location.replace("MainInterface/index.html");

        } else {
            alert('登录失败：' + (data.message || '未知错误'));
        }
    })
    .catch(err => {
        console.error('网络或服务器错误：', err);
        alert('网络或服务器错误，请重试');
    });
});