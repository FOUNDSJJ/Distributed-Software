const iconToggel = document.getElementById("toggle");
const navLinks = document.getElementById("nav-links");
const productsContainer = document.getElementById("products-container");

if (iconToggel && navLinks) {
  iconToggel.addEventListener("click", () => {
    // 移动端点击图标时切换导航显示状态
    navLinks.classList.toggle("show");
  });
}

function formatPrice(price) {
  const numericPrice = Number(price);

  if (Number.isNaN(numericPrice)) {
    return "￥0.00";
  }

  return `￥${numericPrice.toFixed(2)}`;
}

function escapeHtml(value) {
  // 对动态文本做转义，避免直接拼接到 HTML 时出现注入问题
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function createProductCard(product) {
  const productName = String(product.name ?? "");
  const encodedProductName = encodeURIComponent(productName);
  const productId = Number(product.id ?? 0);

  // 商品卡片内容由接口返回的数据动态生成
  return `
    <div class="product">
      <div class="img-container">
        <img src="../assets/css/images/product.png" alt="${escapeHtml(productName)}" />
      </div>
      <div class="content">
        <h3 class="name">${escapeHtml(productName)}</h3>
        <p class="stock">余量：${product.stock}</p>
        <p class="description">${escapeHtml(product.description ?? "")}</p>
        <div class="rate">
          <i class="fa-solid fa-star"></i>
          <i class="fa-solid fa-star"></i>
          <i class="fa-solid fa-star"></i>
          <i class="fa-solid fa-star"></i>
        </div>
        <span class="price">${formatPrice(product.price)}</span>
        <div class="product-actions">
          <button
            type="button"
            class="product-order-btn"
            data-product-id="${productId}"
            data-product-name="${encodedProductName}"
          >
            立即下单
          </button>
          <button
            type="button"
            class="product-pay-btn"
            data-product-id="${productId}"
            data-product-name="${encodedProductName}"
          >
            支付订单
          </button>
        </div>
      </div>
    </div>
  `;
}

function showMessage(message) {
  if (!productsContainer) {
    return;
  }

  productsContainer.innerHTML = `<p class="products-message">${message}</p>`;
}

function renderProducts(products) {
  if (!productsContainer) {
    return;
  }

  if (!Array.isArray(products) || !products.length) {
    showMessage("仓库当中没有商品可出售");
    return;
  }

  productsContainer.innerHTML = products.map((product) => createProductCard(product)).join("");
}

function loadAllProducts() {
  // 页面初始化时拉取全部商品信息
  return fetch("/api/products/info", {
    method: "GET",
    credentials: "include",
  })
    .then((response) => response.json())
    .then((data) => {
      if (!productsContainer) {
        return;
      }

      if (!data.success) {
        showMessage(data.message || "仓库当中没有商品可出售");
        return;
      }

      const products = Object.values(data.datas || {}).slice(
        0,
        Number(data.number) || Object.values(data.datas || {}).length
      );

      renderProducts(products);
    })
    .catch((err) => {
      // 请求失败时同时输出日志和页面提示
      console.error("获取商品信息失败：", err);
      showMessage("获取商品信息失败，请稍后重试");
    });
}

window.shopPage = {
  formatPrice,
  createProductCard,
  showMessage,
  renderProducts,
  loadAllProducts,
};

loadAllProducts();
