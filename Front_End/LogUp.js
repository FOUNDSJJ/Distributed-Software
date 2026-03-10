document.getElementById('registerForm').addEventListener('submit', function(e) {
    e.preventDefault(); // 阻止表单默认提交

    // 获取表单值
    let username = document.getElementById('username').value.trim();
    let phone_number = document.getElementById('phonenumber').value.trim();
    let password = document.getElementById('pwd').value;
    let confirmPassword = document.getElementById('c_pwd').value;
    let agree = document.getElementById('agree').checked; // 修正 checkbox 获取方式

    // ----------------- 表单校验 -----------------
    if (!agree) {
        alert("请先阅读并同意《用户注册协议》！");
        return;
    }

    if (!username || username.length < 3 || username.length > 20) {
        alert("用户名长度必须在 3 到 20 个字符之间！");
        return;
    }

    const phoneRegex = /^1[3-9]\d{9}$/;  // 中国手机号正则
    if (!phoneRegex.test(phone_number)) {
        alert("手机号格式不正确！");
        return;
    }

    if (password.length < 8) {
        alert("密码长度至少 8 位！");
        return;
    }
    if (!/[A-Z]/.test(password) || !/[a-z]/.test(password) || !/\d/.test(password)) {
        alert("密码必须包含大小写字母和数字！");
        return;
    }

    if (password !== confirmPassword) {
        alert("两次密码输入不一样！");
        return;
    }

    // ----------------- 提交注册请求 -----------------
    const registerBtn = document.getElementById('registerBtn'); // 修正 id
    registerBtn.disabled = true;
    registerBtn.value = '注册中...'; // input 用 value 修改文字

    fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password, phone_number })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(err => {
                throw new Error(err.message || '网络响应错误');
            });
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            alert('注册成功！');
            window.location.href = "LogIn.html";
        } else {
            alert('注册失败：' + (data.message || '未知错误'));
        }
    })
    .catch(error => {
        console.error('发生错误：', error);
        alert('注册失败：' + error.message);
    })
    .finally(() => {
        registerBtn.disabled = false;
        registerBtn.value = '同意以上协议并注册';
    });
});