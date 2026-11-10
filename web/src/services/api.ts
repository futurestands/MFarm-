import { fetchUserFarms } from './firebaseAuth';
import { getFirestoreCollection, addFirestoreDocument, updateFirestoreRole } from './firebaseFirestore';
import { auth } from './firebase';

export interface ApiClientConfig {
  token: string;
  farmId: string;
}

export async function fetchFarms(token: string) {
  const user = auth.currentUser;
  if (!user) {
    return [
      { id: 'default-farm', name: 'My Farm', location: 'Uganda', currency: 'UGX', role: 'OWNER' }
    ];
  }
  const farms = await fetchUserFarms(user.uid);
  if (farms.length === 0) {
    return [
      { id: 'default-farm', name: 'My Farm', location: 'Uganda', currency: 'UGX', role: 'OWNER' }
    ];
  }
  return farms;
}

export async function fetchLivestock(config: ApiClientConfig) {
  return await getFirestoreCollection(config.farmId, 'livestock');
}

export async function createLivestock(config: ApiClientConfig, payload: { tag: string; species: string; breed: string; sex?: string }) {
  return await addFirestoreDocument(config.farmId, 'livestock', payload);
}

export async function fetchFinancials(config: ApiClientConfig) {
  return await getFirestoreCollection(config.farmId, 'financial_ledgers');
}

export async function createFinancialEntry(config: ApiClientConfig, payload: { entry_type: string; category: string; amount: number; notes?: string }) {
  return await addFirestoreDocument(config.farmId, 'financial_ledgers', payload);
}

export async function fetchInventory(config: ApiClientConfig) {
  return await getFirestoreCollection(config.farmId, 'inventory');
}

export async function createInventoryItem(config: ApiClientConfig, payload: { item_name: string; category: string; quantity: number; unit?: string; min_quantity?: number }) {
  return await addFirestoreDocument(config.farmId, 'inventory', payload);
}

export async function fetchCrops(config: ApiClientConfig) {
  return await getFirestoreCollection(config.farmId, 'crops');
}

export async function createCrop(config: ApiClientConfig, payload: { crop_type: string; field_name: string; area_acres?: number; planting_date?: string; expected_harvest?: string }) {
  return await addFirestoreDocument(config.farmId, 'crops', payload);
}

export async function fetchFarmMembers(config: ApiClientConfig) {
  return await getFirestoreCollection(config.farmId, 'farm_members');
}

export async function addFarmMember(config: ApiClientConfig, payload: { email: string; role?: string }) {
  return await addFirestoreDocument(config.farmId, 'farm_members', payload);
}

export async function updateMemberRole(config: ApiClientConfig, payload: { user_id: string; role: string }) {
  return await updateFirestoreRole(config.farmId, payload.user_id, payload.role);
}
