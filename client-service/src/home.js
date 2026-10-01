import './config.js';
import { signOut, getCurrentUser } from 'aws-amplify/auth';
import { loadAppointments, initAppointmentForm } from './appointments.js';
import { loadProfile } from './profile.js';

getCurrentUser()
  .then(() => {
    loadProfile();
    loadAppointments();
    initAppointmentForm();
  })
  .catch(() => {
    window.location.href = 'login.html';
  });

document.addEventListener('click', async (event) => {
  const link = event.target.closest('.logout-link');
  if (!link) return;

  event.preventDefault();

  try {
    await signOut();
  } catch (error) {
    console.error('signOut:', error);
  }

  window.location.href = 'login.html';
});
