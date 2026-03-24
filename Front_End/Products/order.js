const orderFeedback = document.getElementById("order-feedback");
const productsContainerForOrder = document.getElementById("products-container");

function setOrderFeedback(message, isError = false) {
  if (!orderFeedback) {
    return;
  }

  orderFeedback.textContent = message;
  orderFeedback.classList.toggle("error", isError);
}

function showOrderResult(message, isError = false) {
  setOrderFeedback(message, isError);
  window.alert(message);
}

function createOrderByName(productName, orderButton) {
  if (!productName) {
    showOrderResult("商品名称不能为空。", true);
    return;
  }

  if (orderButton) {
    orderButton.disabled = true;
    orderButton.textContent = "提交中...";
  }

  setOrderFeedback(`正在提交 ${productName} 的订单请求...`);

  fetch("/api/seckill/orders", {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      product_name: productName,
    }),
  })
    .then((response) => response.json())
    .then((data) => {
      if (!data.success) {
        showOrderResult(data.message || "下单失败，请稍后再试。", true);
        return;
      }

      const orderId = data.order_id ? `，订单号：${data.order_id}` : "";
      const status = data.status ? `，状态：${data.status}` : "";
      showOrderResult(`${data.message || "下单请求已提交"}${orderId}${status}`);
    })
    .catch((error) => {
      console.error("Failed to create seckill order:", error);
      showOrderResult("下单请求发送失败，请检查网络或登录状态。", true);
    })
    .finally(() => {
      if (orderButton) {
        orderButton.disabled = false;
        orderButton.textContent = "立即下单";
      }
    });
}

if (productsContainerForOrder) {
  productsContainerForOrder.addEventListener("click", (event) => {
    const orderButton = event.target.closest(".product-order-btn");
    if (!orderButton) {
      return;
    }

    const encodedProductName = orderButton.dataset.productName || "";
    const productName = decodeURIComponent(encodedProductName);
    createOrderByName(productName, orderButton);
  });
}
