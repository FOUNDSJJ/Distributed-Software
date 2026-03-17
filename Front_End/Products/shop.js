const iconToggel = document.getElementById("toggle");
const navLinks = document.getElementById("nav-links");
const productsContainer = document.getElementById("products-container");

if (iconToggel && navLinks) {
  iconToggel.addEventListener("click", () => {
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

function createProductCard(product) {
  return `
    <div class="product">
      <div class="img-container">
        <img src="../assets/css/images/product.png" alt="${product.name}" />
      </div>
      <div class="content">
        <h3 class="name">${product.name}</h3>
        <p class="stock">余量：${product.stock}</p>
        <p class="description">${product.description}</p>
        <div class="rate">
          <i class="fa-solid fa-star"></i>
          <i class="fa-solid fa-star"></i>
          <i class="fa-solid fa-star"></i>
          <i class="fa-solid fa-star"></i>
        </div>
        <span class="price">${formatPrice(product.price)}</span>
        <div class="product-cart">
          <i class="fa-solid fa-cart-arrow-down"></i>
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
