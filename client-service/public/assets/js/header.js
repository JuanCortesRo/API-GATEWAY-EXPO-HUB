(function () {
  "use strict";

  const headerPlaceholders = document.querySelectorAll("[data-site-header]");

  headerPlaceholders.forEach((placeholder) => {
    const isAuthenticated = placeholder.dataset.authenticated === "true";
    const navigation = isAuthenticated
      ? `<nav id="navmenu" class="navmenu">
          <ul>
            <li><a href="main.html#hero">Inicio</a></li>
            <li><a href="main.html#services">Servicios</a></li>
            <li><a href="main.html#appointment">Citas</a></li>
            <li><a href="login.html" class="logout-link"><i class="bi bi-box-arrow-right" aria-hidden="true"></i><span>Salir</span></a></li>
          </ul>
          <i class="mobile-nav-toggle d-xl-none bi bi-list"></i>
        </nav>`
      : "";

    placeholder.outerHTML = `
      <header id="header" class="header sticky-top">
        <div class="branding d-flex align-items-center">
          <div class="container position-relative d-flex align-items-center justify-content-between">
            <a href="main.html" class="logo d-flex align-items-center me-auto">
              <h1 class="sitename">CitAPIs</h1>
            </a>
            ${navigation}
          </div>
        </div>
      </header>`;
  });

  const mobileNavToggle = document.querySelector(".mobile-nav-toggle");

  if (mobileNavToggle) {
    mobileNavToggle.addEventListener("click", () => {
      document.body.classList.toggle("mobile-nav-active");
      mobileNavToggle.classList.toggle("bi-list");
      mobileNavToggle.classList.toggle("bi-x");
    });
  }
})();