const API_BASE_URL = 'http://localhost:4000/api';

export interface ApiClientConfig {
  token: string;
  farmId: string;
}

export async function fetchFarms(token: string) {
  const response = await fetch(`${API_BASE_URL}/farms`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch authorized farms from backend API');
  }
  const data = await response.json();
  return data.farms;
}

export async function fetchLivestock(config: ApiClientConfig) {
  const response = await fetch(`${API_BASE_URL}/livestock`, {
    headers: {
      'Authorization': `Bearer ${config.token}`,
      'x-farm-id': config.farmId
    }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch livestock from backend API');
  }
  const data = await response.json();
  return data.livestock;
}

export async function createLivestock(config: ApiClientConfig, payload: { tag: string; species: string; breed: string; sex?: string }) {
  const response = await fetch(`${API_BASE_URL}/livestock`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${config.token}`,
      'x-farm-id': config.farmId,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    const err = await response.json().catch(() => ({}));
    throw new Error(err.error || 'Failed to create livestock record');
  }
  return response.json();
}

export async function fetchFinancials(config: ApiClientConfig) {
  const response = await fetch(`${API_BASE_URL}/financials`, {
    headers: {
      'Authorization': `Bearer ${config.token}`,
      'x-farm-id': config.farmId
    }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch financials from backend API');
  }
  const data = await response.json();
  return data.financials;
}
