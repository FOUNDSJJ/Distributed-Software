(function () {
  const payFeedback = document.getElementById("order-feedback");
  const productsContainerForPay = document.getElementById("products-container");
  const PAY_LAST_ORDER_STORAGE_KEY = "seckill:lastOrders";
  const PAYABLE_STATUSES = new Set(["CREATED", "PAY_FAILED"]);
  const WAITING_STATUSES = new Set(["QUEUED", "PENDING_STOCK"]);

  function setPayFeedback(message, isError = false) {
    if (!payFeedback) {
      return;
    }

    payFeedback.textContent = message;
    payFeedback.classList.toggle("error", isError);
  }

  function showPayResult(message, isError = false) {
    setPayFeedback(message, isError);
    window.alert(message);
  }

  function fetchCurrentUser() {
    // 支付前先确认当前登录用户
    return fetch("/api/auth/me", {
      method: "GET",
      credentials: "include",
    }).then((response) => response.json());
  }

  function fetchUserOrders(userId) {
    return fetch(`/api/seckill/orders?userId=${encodeURIComponent(userId)}`, {
      method: "GET",
      credentials: "include",
    }).then((response) => response.json());
  }

  function fetchOrderDetail(orderId) {
    return fetch(`/api/seckill/orders/${encodeURIComponent(orderId)}`, {
      method: "GET",
      credentials: "include",
    }).then((response) => response.json());
  }

  function findPayableOrder(orders, productId) {
    if (!Array.isArray(orders)) {
      return null;
    }

    return (
      orders.find((order) => {
        const matchesProduct = String(order.productId) === String(productId);
        return matchesProduct && PAYABLE_STATUSES.has(order.status);
      }) || null
    );
  }

  function payOrder(orderId) {
    // 支付请求由后端负责推进订单状态流转
    return fetch(`/api/seckill/orders/${encodeURIComponent(orderId)}/pay`, {
      method: "POST",
      credentials: "include",
    }).then((response) => response.json());
  }

  function readLastOrders() {
    try {
      return JSON.parse(window.localStorage.getItem(PAY_LAST_ORDER_STORAGE_KEY) || "{}");
    } catch (error) {
      // 本地缓存损坏时不阻塞支付流程
      console.warn("Failed to parse last order cache:", error);
      return {};
    }
  }

  function getSavedOrderId(productName) {
    const cache = readLastOrders();
    const record = cache[productName];
    return record && record.orderId ? String(record.orderId) : null;
  }

  function normalizeOrderStatusResponse(payload) {
    if (!payload || !payload.success || !payload.data) {
      return null;
    }

    return {
      orderId: payload.data.orderNo || payload.data.orderId ? String(payload.data.orderNo || payload.data.orderId) : null,
      status: payload.data.status || null,
      productId: payload.data.productId != null ? String(payload.data.productId) : null,
    };
  }

  function wait(ms) {
    return new Promise((resolve) => {
      window.setTimeout(resolve, ms);
    });
  }

  async function waitUntilPayable(orderId, maxAttempts = 6, intervalMs = 1000) {
    // 轮询订单状态，等待库存扣减完成后再尝试支付
    for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
      const detail = await fetchOrderDetail(orderId);
      const normalized = normalizeOrderStatusResponse(detail);
      if (!normalized || !normalized.status) {
        return null;
      }

      if (PAYABLE_STATUSES.has(normalized.status)) {
        return normalized;
      }

      if (!WAITING_STATUSES.has(normalized.status)) {
        return normalized;
      }

      if (attempt < maxAttempts - 1) {
        await wait(intervalMs);
      }
    }

    return null;
  }

  async function payLatestOrderByProduct(productId, productName, payButton) {
    if (!productId) {
      showPayResult("商品 ID 不能为空。", true);
      return;
    }

    if (payButton) {
      payButton.disabled = true;
      payButton.textContent = "支付中...";
    }

    setPayFeedback(`正在查找 ${productName || "当前商品"} 的可支付订单...`);

    try {
      const userData = await fetchCurrentUser();
      if (!userData.success || !userData.user || !userData.user.id) {
        throw new Error(userData.message || "请先登录后再支付");
      }

      const savedOrderId = getSavedOrderId(productName);
      if (savedOrderId) {
        // 优先使用最近一次下单记录，减少一次订单列表查询
        setPayFeedback(`已找到最近订单 ${savedOrderId}，正在确认是否可支付...`);
        const orderDetail = await waitUntilPayable(savedOrderId);
        if (orderDetail && PAYABLE_STATUSES.has(orderDetail.status)) {
          const payData = await payOrder(orderDetail.orderId);
          if (!payData.success) {
            throw new Error(payData.message || "支付请求失败");
          }

          const orderIdText = payData.order_id ? `，订单号：${payData.order_id}` : "";
          const statusText = payData.status ? `，状态：${payData.status}` : "";
          showPayResult(`${payData.message || "支付请求已提交"}${orderIdText}${statusText}`);
          return;
        }

        if (orderDetail && WAITING_STATUSES.has(orderDetail.status)) {
          throw new Error("订单仍在处理中，请稍后再点击支付");
        }

        if (orderDetail && orderDetail.status === "PAID") {
          throw new Error("该订单已支付，无需重复支付");
        }

        if (orderDetail && orderDetail.status === "PAYING") {
          throw new Error("该订单正在支付处理中，请稍后查看结果");
        }
      }

      const orderData = await fetchUserOrders(userData.user.id);
      if (!orderData.success) {
        throw new Error(orderData.message || "查询订单失败");
      }

      const payableOrder = findPayableOrder(orderData.data, productId);
      if (!payableOrder || !payableOrder.orderNo) {
        throw new Error("当前商品没有可支付的订单");
      }

      setPayFeedback(`已找到订单 ${payableOrder.orderNo}，正在发起支付...`);
      const payData = await payOrder(payableOrder.orderNo);
      if (!payData.success) {
        throw new Error(payData.message || "支付请求失败");
      }

      const orderIdText = payData.order_id ? `，订单号：${payData.order_id}` : "";
      const statusText = payData.status ? `，状态：${payData.status}` : "";
      showPayResult(`${payData.message || "支付请求已提交"}${orderIdText}${statusText}`);
    } catch (error) {
      // 统一输出支付异常，便于联调排查
      console.error("Failed to pay seckill order:", error);
      showPayResult(error.message || "支付请求发送失败，请稍后再试", true);
    } finally {
      if (payButton) {
        payButton.disabled = false;
        payButton.textContent = "支付订单";
      }
    }
  }

  if (productsContainerForPay) {
    productsContainerForPay.addEventListener("click", (event) => {
      // 事件委托处理动态渲染出的支付按钮
      const payButton = event.target.closest(".product-pay-btn");
      if (!payButton) {
        return;
      }

      const productId = Number(payButton.dataset.productId || 0);
      const encodedProductName = payButton.dataset.productName || "";
      const productName = decodeURIComponent(encodedProductName);
      payLatestOrderByProduct(productId, productName, payButton);
    });
  }
})();
