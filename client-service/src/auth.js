import { signIn, signUp, confirmSignUp, getCurrentUser } from 'aws-amplify/auth';

getCurrentUser()
  .then(() => { window.location.href = 'main.html'; })
  .catch(() => {});

let pendingRegistration = null;

const tabs = document.querySelectorAll('[data-auth-tab]');
const panels = document.querySelectorAll('[data-auth-panel]');

tabs.forEach((tab) => {
  tab.addEventListener('click', () => {
    const target = tab.dataset.authTab;

    tabs.forEach((item) => {
      const isActive = item === tab;
      item.classList.toggle('is-active', isActive);
      item.setAttribute('aria-selected', String(isActive));
    });

    panels.forEach((panel) => {
      panel.classList.toggle('is-hidden', panel.dataset.authPanel !== target);
    });
  });
});

function showError(form, message) {
  let box = form.querySelector('.auth-error');
  if (!box) {
    box = document.createElement('div');
    box.className = 'alert alert-danger auth-error';
    box.hidden = true;
    form.prepend(box);
  }
  box.textContent = message;
  box.hidden = false;
}

function clearError(form) {
  const box = form.querySelector('.auth-error');
  if (box) box.hidden = true;
}

function errorMessage(error) {
  const code = error?.name ?? error?.code ?? '';
  const map = {
    UserNotFoundException: 'No existe una cuenta con ese correo.',
    NotAuthorizedException: 'Correo o contraseña incorrectos.',
    UserNotConfirmedException: 'Tu cuenta no está confirmada. Revisa tu correo.',
    PasswordResetRequiredException: 'Debes restablecer tu contraseña.',
    UsernameExistsException: 'Ya existe una cuenta con ese correo.',
    InvalidPasswordException: 'La contraseña no cumple los requisitos de seguridad.',
    CodeMismatchException: 'El código de verificación no es correcto.',
    ExpiredCodeException: 'El código de verificación expiró. Solicita uno nuevo.',
    LimitExceededException: 'Demasiados intentos. Espera un momento.',
    TooManyRequestsException: 'Demasiadas solicitudes. Espera un momento.'
  };
  return map[code] ?? error?.message ?? 'Ocurrió un error. Intenta de nuevo.';
}

const loginPanel = document.querySelector('[data-auth-panel="login-panel"]');
const loginForm = loginPanel?.querySelector('form');

if (loginForm) {
  loginForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearError(loginForm);

    const email = loginForm.querySelector('[name="email"]').value;
    const password = loginForm.querySelector('[name="password"]').value;
    const submit = loginForm.querySelector('[type="submit"]');
    const originalLabel = submit.textContent;

    submit.disabled = true;
    submit.textContent = 'Ingresando...';

    try {
      await signIn({ username: email, password });
      window.location.href = 'main.html';
    } catch (error) {
      console.error('signIn:', error);
      showError(loginForm, errorMessage(error));
      submit.disabled = false;
      submit.textContent = originalLabel;
    }
  });
}

const registerPanel = document.querySelector('[data-auth-panel="register-panel"]');
const registerForm = registerPanel?.querySelector('form');

if (registerForm) {
  registerForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearError(registerForm);

    const name = registerForm.querySelector('[name="name"]').value.trim();
    const cedula = registerForm.querySelector('[name="cedula"]').value.trim();
    const email = registerForm.querySelector('[name="email"]').value.trim();
    const password = registerForm.querySelector('[name="password"]').value;
    const confirmation = registerForm.querySelector('[name="password_confirmation"]').value;

    if (password !== confirmation) {
      showError(registerForm, 'Las contraseñas no coinciden.');
      return;
    }

    const submit = registerForm.querySelector('[type="submit"]');
    const originalLabel = submit.textContent;
    submit.disabled = true;
    submit.textContent = 'Creando cuenta...';

    try {
      const result = await signUp({
        username: email,
        password,
        options: {
          userAttributes: { email, name, 'custom:cedula': cedula }
        }
      });

      pendingRegistration = { name, cedula, email, password };

      if (result.isSignUpComplete) {
        await signIn({ username: email, password });
        window.location.href = 'main.html';
        return;
      }

      showConfirmPanel(email);
    } catch (error) {
      console.error('signUp:', error);
      showError(registerForm, errorMessage(error));
      submit.disabled = false;
      submit.textContent = originalLabel;
    }
  });
}

function showConfirmPanel(email) {
  const confirmPanel = document.querySelector('[data-auth-panel="confirm-panel"]');
  const note = confirmPanel.querySelector('[data-confirm-note]');
  note.textContent = `Enviamos un código de verificación a ${email}.`;

  panels.forEach((panel) => panel.classList.toggle('is-hidden', panel !== confirmPanel));

  const tabList = document.querySelector('.auth-tabs');
  if (tabList) tabList.hidden = true;
}

const confirmPanel = document.querySelector('[data-auth-panel="confirm-panel"]');
const confirmForm = confirmPanel?.querySelector('form');

if (confirmForm) {
  confirmForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearError(confirmForm);

    if (!pendingRegistration) {
      window.location.reload();
      return;
    }

    const code = confirmForm.querySelector('[name="code"]').value.trim();
    const submit = confirmForm.querySelector('[type="submit"]');
    const originalLabel = submit.textContent;
    submit.disabled = true;
    submit.textContent = 'Confirmando...';

    try {
      await confirmSignUp({
        username: pendingRegistration.email,
        confirmationCode: code
      });
      await signIn({
        username: pendingRegistration.email,
        password: pendingRegistration.password
      });
    } catch (error) {
      console.error('confirm:', error);
      showError(confirmForm, errorMessage(error));
      submit.disabled = false;
      submit.textContent = originalLabel;
      return;
    }

    window.location.href = 'main.html';
  });
}
