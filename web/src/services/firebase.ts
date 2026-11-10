import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "AIzaSyCsj81-yOhA6b3vPUu3DymFozZpvtattk8",
  authDomain: "mfarm-platform-prod.firebaseapp.com",
  projectId: "mfarm-platform-prod",
  storageBucket: "mfarm-platform-prod.firebasestorage.app",
  messagingSenderId: "626693722750",
  appId: "1:626693722750:web:mfarmplatform"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
