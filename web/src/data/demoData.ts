import { Farm, LivestockItem, BatchItem, CropItem, FinancialItem, InventoryItem } from '../types';

export const DEMO_IS_MOCK_INDICATOR = true;

export const DEMO_FARMS: Farm[] = [
  {
    id: 'farm-001',
    name: 'Kazinga Valley Mixed Farm [DEMO DATA]',
    location: 'Mbarara District, Uganda',
    currency: 'UGX',
    role: 'OWNER',
    animalCount: 142,
    cropAcres: 45
  },
  {
    id: 'farm-002',
    name: 'Victoria Agro Enterprise [DEMO DATA]',
    location: 'Mukono District, Uganda',
    currency: 'UGX',
    role: 'MANAGER',
    animalCount: 85,
    cropAcres: 20
  }
];

export const DEMO_LIVESTOCK: LivestockItem[] = [
  { id: 'lv-01', tag: 'KZ-COW-01', species: 'Cattle', breed: 'Friesian Cross', dob: '2022-03-14', sex: 'FEMALE', status: 'ACTIVE', sireTag: 'KZ-BULL-09', damTag: 'KZ-COW-00' },
  { id: 'lv-02', tag: 'KZ-GOAT-04', species: 'Goat', breed: 'Boer x Local', dob: '2023-01-10', sex: 'MALE', status: 'ACTIVE' },
  { id: 'lv-03', tag: 'KZ-COW-05', species: 'Cattle', breed: 'Ankole Longhorn', dob: '2021-08-20', sex: 'FEMALE', status: 'ACTIVE' },
  { id: 'lv-04', tag: 'KZ-SHEEP-12', species: 'Sheep', breed: 'Dorper', dob: '2023-05-02', sex: 'FEMALE', status: 'ACTIVE' }
];

export const DEMO_BATCHES: BatchItem[] = [
  { id: 'bt-01', batchName: 'Kazinga Broiler Batch #4', species: 'Poultry (Broilers)', quantity: 500, housingUnit: 'House A2', arrivalDate: '2026-09-15' },
  { id: 'bt-02', batchName: 'Layer Flock 2026-B', species: 'Poultry (Layers)', quantity: 350, housingUnit: 'Layer Pen B', arrivalDate: '2026-04-01' }
];

export const DEMO_CROPS: CropItem[] = [
  { id: 'cr-01', fieldName: 'North Block A', cropType: 'Maize (Longe 5)', areaAcres: 15, plantingDate: '2026-08-20', status: 'GROWING', expectedHarvest: '2026-12-15' },
  { id: 'cr-02', fieldName: 'East Field 3', cropType: 'Beans (NABE 15)', areaAcres: 10, plantingDate: '2026-09-01', status: 'PLANTED', expectedHarvest: '2026-11-30' },
  { id: 'cr-03', fieldName: 'South Valley Plantation', cropType: 'Banana (Matooke)', areaAcres: 20, plantingDate: '2024-03-10', status: 'HARVESTED', expectedHarvest: 'Continuous' }
];

export const DEMO_FINANCIALS: FinancialItem[] = [
  { id: 'fn-01', type: 'INCOME', category: 'Milk Sales', amount: 1850000, date: '2026-10-24', notes: 'Weekly dairy supply to Mbarara collection center' },
  { id: 'fn-02', type: 'INCOME', category: 'Batch Livestock Sale', amount: 4500000, date: '2026-10-20', notes: 'Sold 150 Broilers to local hotel' },
  { id: 'fn-03', type: 'EXPENSE', category: 'Feed & Nutrition', amount: 920000, date: '2026-10-22', notes: '20 bags of Dairy Meal 16%' },
  { id: 'fn-04', type: 'EXPENSE', category: 'Veterinary Treatment', amount: 240000, date: '2026-10-18', notes: 'Deworming & East Coast Fever vaccinations' }
];

export const DEMO_INVENTORY: InventoryItem[] = [
  { id: 'inv-01', name: 'Dairy Meal 16% Protein', category: 'Feed', quantity: 45, unit: '50kg Bags', reorderThreshold: 10 },
  { id: 'inv-02', name: 'NPK Fertilizer 17:17:17', category: 'Fertilizer', quantity: 8, unit: '50kg Bags', reorderThreshold: 15 },
  { id: 'inv-03', name: 'Oxytetracycline 20% LA', category: 'Medicine', quantity: 12, unit: 'Vials (100ml)', reorderThreshold: 5 }
];
