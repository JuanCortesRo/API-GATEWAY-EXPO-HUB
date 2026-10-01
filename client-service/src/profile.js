import { apiGet, apiPost } from './api.js';
import { fetchUserAttributes } from 'aws-amplify/auth';

export async function loadProfile() {
  const welcome = document.querySelector('.welcome h2');
  const patient = await getOrCreatePatient();
  const name = patient?.name;

  if (name && welcome) {
    welcome.textContent = `Bienvenido, ${name}`;
    return;
  }

  const attributes = await fetchAttributes();
  const fallback = attributes?.name || attributes?.email;

  if (fallback && welcome) {
    welcome.textContent = `Bienvenido, ${fallback}`;
  }
}

async function getOrCreatePatient() {
  try {
    return await apiGet('/patients/me');
  } catch (error) {
    console.warn('profile: GET /patients/me falló', error);
  }

  const attributes = await fetchAttributes();
  if (!attributes?.email) return null;

  try {
    return await apiPost('/patients/', {
      name: attributes.name || attributes.email,
      document_id: attributes['custom:cedula'] || 'N/A',
      email: attributes.email
    });
  } catch (error) {
    console.warn('profile: POST /patients/ falló', error);
    return null;
  }
}

async function fetchAttributes() {
  try {
    return await fetchUserAttributes();
  } catch (error) {
    console.warn('profile: fetchUserAttributes falló', error);
    return null;
  }
}
