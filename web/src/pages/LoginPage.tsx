import React, { useState } from 'react';
import { Sprout, Lock, Mail, User, Building, ArrowRight, AlertCircle, Loader2 } from 'lucide-react';

interface LoginPageProps {
  onLoginSuccess: (token: string, email: string) => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess }) => {
  const [isRegistering, setIsRegistering] = useState(false);
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [farmName, setFarmName] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:4001/api';
      const endpoint = isRegistering
        ? `${API_BASE_URL}/auth/register`
        : `${API_BASE_URL}/auth/login`;

      const payload = isRegistering
        ? { full_name: fullName, email, password, farm_name: farmName || undefined }
        : { email, password };

      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        const errData = await response.json().catch(() => ({}));
        throw new Error(errData.error || (isRegistering ? 'Registration failed' : 'Authentication failed against MFarm API'));
      }

      const data = await response.json();
      setIsLoading(false);
      onLoginSuccess(data.token, data.email);
    } catch (err: any) {
      setIsLoading(false);
      setError(err.message || 'Unable to connect to MFarm backend API');
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-xl overflow-hidden">
        {/* Header Banner */}
        <div className="bg-emerald-900 p-6 text-center text-white">
          <div className="inline-flex bg-emerald-500 p-3 rounded-xl text-emerald-950 font-bold mb-3">
            <Sprout className="w-8 h-8" />
          </div>
          <h1 className="text-2xl font-bold">MFarm Platform</h1>
          <p className="text-emerald-200 text-sm mt-1">
            {isRegistering ? 'Create Your Farm Account' : 'PostgreSQL Authenticated Backend'}
          </p>
        </div>

        {error && (
          <div className="bg-red-50 border-b border-red-200 p-3 text-center text-xs text-red-800 font-medium flex items-center justify-center space-x-1">
            <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {isRegistering && (
            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                Full Name
              </label>
              <div className="relative">
                <User className="w-5 h-5 absolute left-3 top-2.5 text-gray-400" />
                <input
                  type="text"
                  required
                  placeholder="e.g. John Mugarura"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-600"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
              Email Address
            </label>
            <div className="relative">
              <Mail className="w-5 h-5 absolute left-3 top-2.5 text-gray-400" />
              <input
                type="email"
                required
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-600"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
              Password {isRegistering && <span className="text-gray-400 font-normal">(min 6 chars)</span>}
            </label>
            <div className="relative">
              <Lock className="w-5 h-5 absolute left-3 top-2.5 text-gray-400" />
              <input
                type="password"
                required
                minLength={6}
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-600"
              />
            </div>
          </div>

          {isRegistering && (
            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                Farm Name <span className="text-gray-400 font-normal">(Optional)</span>
              </label>
              <div className="relative">
                <Building className="w-5 h-5 absolute left-3 top-2.5 text-gray-400" />
                <input
                  type="text"
                  placeholder="e.g. Kazinga Farm"
                  value={farmName}
                  onChange={(e) => setFarmName(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-600"
                />
              </div>
            </div>
          )}

          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-emerald-800 hover:bg-emerald-700 text-white font-semibold py-2.5 px-4 rounded-lg flex items-center justify-center space-x-2 transition shadow-md disabled:opacity-50"
          >
            {isLoading ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              <>
                <span>{isRegistering ? 'Register & Create Farm' : 'Sign In to MFarm API'}</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>

          <div className="text-center pt-2">
            <button
              type="button"
              onClick={() => {
                setIsRegistering(!isRegistering);
                setError('');
              }}
              className="text-xs text-emerald-700 hover:text-emerald-900 font-medium transition"
            >
              {isRegistering
                ? 'Already have an account? Sign in here'
                : "Don't have an account? Register a new farm"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
