import { fetchAuthSession } from 'aws-amplify/auth';

export async function getToken() {
  const { tokens } = await fetchAuthSession();
  if (!tokens?.accessToken) {
    throw new Error('No hay sesión activa.');
  }
  return tokens.accessToken.toString();
}

export async function apiGet(path) {
  const token = await getToken();
  const response = await fetch(path, {
    headers: { Authorization: `Bearer ${token}` }
  });
  if (!response.ok) {
    throw new Error(`${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function apiPost(path, body) {
  const token = await getToken();
  const response = await fetch(path, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  });
  if (!response.ok) {
    throw new Error(`${response.status} ${await response.text()}`);
  }
  return response.json();
}
