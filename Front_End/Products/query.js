const searchForm = document.getElementById("product-search-form");
const productNameInput = document.getElementById("product-name");
const searchFeedback = document.getElementById("search-feedback");

function setSearchFeedback(message, isError = false) {
  if (!searchFeedback) {
    return;
  }

  searchFeedback.textContent = message;
  searchFeedback.classList.toggle("error", isError);
}

if (searchForm && productNameInput) {
  searchForm.addEventListener("submit", function (e) {
    // 阻止表单刷新页面，改为异步查询
    e.preventDefault();

    const productName = productNameInput.value.trim();

    if (!productName) {
      setSearchFeedback("请输入要查询的商品名称", true);

      if (window.shopPage && typeof window.shopPage.loadAllProducts === "function") {
        // 未输入商品名时恢复默认商品列表
        window.shopPage.loadAllProducts();
      }

      return;
    }

    setSearchFeedback("正在查询商品...");

    // 按商品名查询单个商品详情
    fetch(`/api/products/by-name?name=${encodeURIComponent(productName)}`, {
      method: "GET",
      credentials: "include",
    })
      .then((response) => response.json())
      .then((data) => {
        if (!window.shopPage) {
          return;
        }

        if (!data.success) {
          setSearchFeedback(data.message || "该商品名不存在1", true);
          window.shopPage.showMessage(data.message || "该商品名不存在1");
          return;
        }

        window.shopPage.renderProducts([data.data]);
        setSearchFeedback(`查询成功！`);
      })
      .catch((err) => {
        // 查询失败时同步更新页面提示
        console.error("查询商品失败：", err);
        setSearchFeedback("查询商品失败，请稍后重试", true);

        if (window.shopPage) {
          window.shopPage.showMessage("查询商品失败，请稍后重试");
        }
      });
  });
}
