import { db } from './firebase';
import { collection, getDocs, addDoc, query, where, doc, updateDoc } from 'firebase/firestore';

export async function getFirestoreCollection(farmId: string, collectionName: string) {
  const q = query(collection(db, collectionName), where('farm_id', '==', farmId));
  const snap = await getDocs(q);
  return snap.docs.map(d => ({ id: d.id, ...d.data() }));
}

export async function addFirestoreDocument(farmId: string, collectionName: string, payload: any) {
  const docRef = await addDoc(collection(db, collectionName), {
    farm_id: farmId,
    ...payload,
    created_at: new Date().toISOString()
  });
  return { id: docRef.id, status: 'success' };
}

export async function updateFirestoreRole(farmId: string, userId: string, newRole: string) {
  const memberDocId = `${farmId}_${userId}`;
  const docRef = doc(db, 'farm_members', memberDocId);
  await updateDoc(docRef, { role: newRole });
  return { status: 'success' };
}
