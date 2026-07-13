const Auth = (() => {
  const form = document.getElementById('authForm');
  const tabBtns = document.querySelectorAll('.auth-tabs button');
  const title = document.getElementById('formTitle');
  const submitBtn = document.getElementById('submitBtn');
  const toggleText = document.getElementById('toggleText');
  const errorDiv = document.getElementById('authError');

  let mode = 'login';

  if (API.getTokens() && API.getUser()) {
    window.location.href = '/app.html';
    return;
  }

  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      mode = btn.dataset.mode;
      tabBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      title.textContent = mode === 'login' ? 'Iniciar sesion' : 'Crear cuenta';
      submitBtn.textContent = mode === 'login' ? 'Entrar' : 'Registrarse';
      toggleText.innerHTML = mode === 'login'
        ? 'No tienes cuenta? <a href="#" id="toggleMode">Registrate</a>'
        : 'Ya tienes cuenta? <a href="#" id="toggleMode">Inicia sesion</a>';
      errorDiv.style.display = 'none';
      attachToggle();
    });
  });

  function attachToggle() {
    const link = document.getElementById('toggleMode');
    if (link) {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        tabBtns.forEach(b => b.classList.remove('active'));
        const target = mode === 'login' ? tabBtns[1] : tabBtns[0];
        target.click();
      });
    }
  }
  attachToggle();

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorDiv.style.display = 'none';

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    if (!username || !password) {
      showError('Completa todos los campos');
      return;
    }

    try {
      submitBtn.disabled = true;
      submitBtn.textContent = 'Procesando...';

      if (mode === 'registro') {
        const res = await API.post('/auth/registro', { username, password });
        if (typeof res === 'string' && res.includes('ya existe')) {
          showError(res);
          return;
        }
      }

      const data = await API.post('/auth/login', { username, password });
      API.saveTokens(data.accessToken, data.refreshToken);
      API.setUser(username);
      window.location.href = '/app.html';
    } catch (err) {
      showError(err.message || 'Error al autenticar');
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = mode === 'login' ? 'Entrar' : 'Registrarse';
    }
  });

  function showError(msg) {
    errorDiv.textContent = msg;
    errorDiv.style.display = 'block';
  }
})();
