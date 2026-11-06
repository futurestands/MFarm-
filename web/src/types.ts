export type UserRole = 'OWNER' | 'MANAGER' | 'WORKER';

export interface Farm {
  id: string;
  name: string;
  location: string;
  currency: string;
  role: UserRole;
  animalCount: number;
  cropAcres: number;
}

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  assignedFarms: Farm[];
}

export interface LivestockItem {
  id: string;
  tag: string;
  species: string;
  breed: string;
  dob: string;
  sex: 'MALE' | 'FEMALE';
  status: 'ACTIVE' | 'SOLD' | 'DECEASED';
  sireTag?: string;
  damTag?: string;
}

export interface BatchItem {
  id: string;
  batchName: string;
  species: string;
  quantity: number;
  housingUnit: string;
  arrivalDate: string;
}

export interface CropItem {
  id: string;
  fieldName: string;
  cropType: string;
  areaAcres: number;
  plantingDate: string;
  status: 'PLANTED' | 'GROWING' | 'HARVESTED' | 'FAILED';
  expectedHarvest: string;
}

export interface FinancialItem {
  id: string;
  type: 'INCOME' | 'EXPENSE';
  category: string;
  amount: number;
  date: string;
  notes: string;
  isReversed?: boolean;
}

export interface InventoryItem {
  id: string;
  name: string;
  category: string;
  quantity: number;
  unit: string;
  reorderThreshold: number;
}
