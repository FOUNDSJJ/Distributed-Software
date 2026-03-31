const orderFeedback = document.getElementById("order-feedback");
const productsContainerForOrder = document.getElementById("products-container");
const LAST_ORDER_STORAGE_KEY = "seckill:lastOrders";

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

function readLastOrders() {
  try {
    return JSON.parse(window.localStorage.getItem(LAST_ORDER_STORAGE_KEY) || "{}");
  } catch (error) {
    // 本地缓存异常时回退为空对象，避免影响下单流程
    console.warn("解析最后一个订单缓存失败:", error);
    return {};
  }
}

function saveLastOrder(productName, orderId) {
  if (!productName || !orderId) {
    return;
  }

  const cache = readLastOrders();
  cache[productName] = {
    orderId: String(orderId),
    savedAt: Date.now(),
  };

  // 缓存最近一次下单结果，便于后续支付快速定位订单
  window.localStorage.setItem(LAST_ORDER_STORAGE_KEY, JSON.stringify(cache));
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

  // 通过商品名调用后端秒杀下单接口
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

      saveLastOrder(productName, data.order_id);

      const orderId = data.order_id ? `，订单号：${String(data.order_id)}` : "";
      const status = data.status ? `，状态：${data.status}` : "";
      showOrderResult(`${data.message || "下单请求已提交"}${orderId}${status}`);
    })
    .catch((error) => {
      // 网络或服务异常时给出统一提示
      console.error("创建秒杀订单失败:", error);
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
    // 事件委托处理动态生成的下单按钮
    const orderButton = event.target.closest(".product-order-btn");
    if (!orderButton) {
      return;
    }

    const encodedProductName = orderButton.dataset.productName || "";
    const productName = decodeURIComponent(encodedProductName);
    createOrderByName(productName, orderButton);
  });
}
