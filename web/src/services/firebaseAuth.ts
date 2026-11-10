import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
  User
} from 'firebase/auth';
import { auth, db } from './firebase';
import { doc, setDoc, getDoc, collection, getDocs, query, where } from 'firebase/firestore';

export async function loginWithFirebase(email: string, pass: string) {
  const cred = await signInWithEmailAndPassword(auth, email, pass);
  const token = await cred.user.getIdToken();
  return { token, email: cred.user.email || email, uid: cred.user.uid };
}

export async function registerWithFirebase(email: string, pass: string, fullName: string, farmName?: string) {
  const cred = await createUserWithEmailAndPassword(auth, email, pass);
  const uid = cred.user.uid;

  const farmRef = doc(collection(db, 'farms'));
  const farmId = farmRef.id;

  await setDoc(farmRef, {
    name: farmName || `${fullName}'s Farm`,
    location: 'Default Location',
    currency: 'UGX',
    createdAt: new Date().toISOString()
  });

  await setDoc(doc(db, 'farm_members', `${farmId}_${uid}`), {
    farmId,
    userId: uid,
    email,
    fullName,
    role: 'OWNER'
  });

  const token = await cred.user.getIdToken();
  return { token, email, uid, farm_id: farmId };
}

export async function fetchUserFarms(uid: string) {
  const q = query(collection(db, 'farm_members'), where('userId', '==', uid));
  const snap = await getDocs(q);
  const farms: any[] = [];
  for (const memberDoc of snap.docs) {
    const data = memberDoc.data();
    const farmDoc = await getDoc(doc(db, 'farms', data.farmId));
    if (farmDoc.exists()) {
      farms.push({
        id: farmDoc.id,
        ...farmDoc.data(),
        role: data.role
      });
    }
  }
  return farms;
}
