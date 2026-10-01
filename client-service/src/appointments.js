import { apiGet, apiPost } from './api.js';

const widget = document.querySelector('[data-appointments-widget]');
const form = document.querySelector('#appointment-form');

let appointments = [];

function formatAppointmentDate(appointmentDate) {
  const date = new Date(appointmentDate);

  return {
    day: new Intl.DateTimeFormat('es-ES', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    }).format(date),
    time: new Intl.DateTimeFormat('es-ES', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    }).format(date)
  };
}

function renderWidget() {
  if (!widget) return;

  if (appointments.length === 0) {
    widget.innerHTML = '';
    widget.hidden = true;
    return;
  }

  widget.innerHTML = `
    <button class="appointments-toggle" type="button" aria-expanded="false" aria-controls="appointments-list">
      <span class="appointments-toggle-label">
        <i class="bi bi-calendar2-check" aria-hidden="true"></i>
        <span>Tus citas</span>
      </span>
      <span class="appointments-count" aria-label="${appointments.length} cita">${appointments.length}</span>
      <i class="bi bi-chevron-down appointments-toggle-icon" aria-hidden="true"></i>
    </button>
    <div id="appointments-list" class="appointments-list" hidden>
      ${appointments.map((appointment) => {
        const formattedDate = formatAppointmentDate(appointment.appointmentDate);

        return `
          <article class="appointment-card">
            <strong>${formattedDate.day}</strong>
            <span>${formattedDate.time}</span>
          </article>`;
      }).join('')}
    </div>`;

  widget.hidden = false;

  const toggle = widget.querySelector('.appointments-toggle');
  const list = widget.querySelector('.appointments-list');

  toggle.addEventListener('click', () => {
    const isExpanded = toggle.getAttribute('aria-expanded') === 'true';

    toggle.setAttribute('aria-expanded', String(!isExpanded));
    list.hidden = isExpanded;
    widget.classList.toggle('is-expanded', !isExpanded);
  });
}

export async function loadAppointments() {
  try {
    appointments = await apiGet('/appointments');
  } catch (error) {
    console.error('appointments:', error);
    appointments = [];
  }
  renderWidget();
}

function toBackendFormat(value) {
  if (!value || !value.includes('T')) return null;

  const [date, time = ''] = value.split('T');
  const parts = time.split(':');
  while (parts.length < 3) parts.push('00');

  return `${date}T${parts[0]}:${parts[1]}:${parts[2]}`;
}

function showFeedback(message, type) {
  let box = form.querySelector('.appointment-feedback');
  if (!box) {
    box = document.createElement('div');
    box.style.marginTop = '1rem';
    form.appendChild(box);
  }
  box.className = `appointment-feedback alert alert-${type}`;
  box.textContent = message;
}

export function initAppointmentForm() {
  if (!form) return;

  form.addEventListener('submit', async (event) => {
    event.preventDefault();

    const input = form.querySelector('[name="date"]');
    const appointmentDate = toBackendFormat(input.value);

    if (!appointmentDate) {
      showFeedback('Selecciona una fecha y hora válidas.', 'danger');
      return;
    }

    const submit = form.querySelector('[type="submit"]');
    const originalLabel = submit.textContent;
    submit.disabled = true;
    submit.textContent = 'Agendando...';

    try {
      await apiPost('/appointments', { appointmentDate });
      input.value = '';
      showFeedback('Cita solicitada correctamente.', 'success');
      await loadAppointments();
    } catch (error) {
      console.error('create appointment:', error);
      showFeedback('No se pudo agendar la cita. Intenta de nuevo.', 'danger');
    } finally {
      submit.disabled = false;
      submit.textContent = originalLabel;
    }
  });
}
