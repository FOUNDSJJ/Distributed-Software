document.getElementById('LogInForm').addEventListener('submit', function(e) {
  e.preventDefault(); // 阻止表单默认提交

  let username = document.getElementById('username').value;
  let password = document.getElementById('pwd').value;

  fetch('http://localhost:8080/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ username, password })
  })
  .then(response => {
    if (!response.ok) {
      throw new Error('网络响应错误');
    }
    return response.json();  // 解析返回的 JSON 数据
  })
  .then(data => {
    if(data.success){
        alert('登录成功！');
        window.location.replace("MainInterface/index.html");
    }
  })
  .catch(error => {
    console.error('发生错误：', error);
    alert('登录失败，请重试');
  });
});