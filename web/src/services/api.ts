const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:4001/api';

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

export async function fetchInventory(config: ApiClientConfig) {
  const response = await fetch(`${API_BASE_URL}/inventory`, {
    headers: {
      'Authorization': `Bearer ${config.token}`,
      'x-farm-id': config.farmId
    }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch inventory from backend API');
  }
  const data = await response.json();
  return data.inventory;
}

export async function createFinancialEntry(config: ApiClientConfig, payload: { entry_type: string; category: string; amount: number; notes?: string }) {
  const response = await fetch(`${API_BASE_URL}/financials`, {
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
    throw new Error(err.error || 'Failed to record financial ledger entry');
  }
  return response.json();
}

export async function createInventoryItem(config: ApiClientConfig, payload: { item_name: string; category: string; quantity: number; unit?: string; min_quantity?: number }) {
  const response = await fetch(`${API_BASE_URL}/inventory`, {
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
    throw new Error(err.error || 'Failed to add inventory item');
  }
  return response.json();
}

export async function fetchCrops(config: ApiClientConfig) {
  const response = await fetch(`${API_BASE_URL}/crops`, {
    headers: {
      'Authorization': `Bearer ${config.token}`,
      'x-farm-id': config.farmId
    }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch crops from backend API');
  }
  const data = await response.json();
  return data.crops;
}

export async function createCrop(config: ApiClientConfig, payload: { crop_type: string; field_name: string; area_acres?: number; planting_date?: string; expected_harvest?: string }) {
  const response = await fetch(`${API_BASE_URL}/crops`, {
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
    throw new Error(err.error || 'Failed to create crop enterprise');
  }
  return response.json();
}

export async function fetchFarmMembers(config: ApiClientConfig) {
  const response = await fetch(`${API_BASE_URL}/farms/members`, {
    headers: {
      'Authorization': `Bearer ${config.token}`,
      'x-farm-id': config.farmId
    }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch farm members from backend API');
  }
  const data = await response.json();
  return data.members;
}

export async function addFarmMember(config: ApiClientConfig, payload: { email: string; role?: string }) {
  const response = await fetch(`${API_BASE_URL}/farms/members`, {
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
    throw new Error(err.error || 'Failed to add farm member');
  }
  return response.json();
}

export async function updateMemberRole(config: ApiClientConfig, payload: { user_id: string; role: string }) {
  const response = await fetch(`${API_BASE_URL}/farms/members/role`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${config.token}`,
      'x-farm-id': config.farmId,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    const err = await response.json().catch(() => ({}));
    throw new Error(err.error || 'Failed to update member role');
  }
  return response.json();
}
