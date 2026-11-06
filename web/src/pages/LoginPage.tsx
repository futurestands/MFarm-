import React, { useState } from 'react';
import { Sprout, Lock, Mail, ArrowRight } from 'lucide-react';

interface LoginPageProps {
  onLoginSuccess: (email: string) => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState('mugarura@mfarm.dev');
  const [password, setPassword] = useState('demo1234');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onLoginSuccess(email);
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
          <p className="text-emerald-200 text-sm mt-1">Integrated Farm Management Hub</p>
        </div>

        {/* Demo Warning Banner */}
        <div className="bg-amber-50 border-b border-amber-200 p-3 text-center text-xs text-amber-800 font-medium">
          [DEMO MODE] Enter any email and password to access the test interface.
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
              Email Address
            </label>
            <div className="relative">
              <Mail className="w-5 h-5 absolute left-3 top-2.5 text-gray-400" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-600"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
              Password
            </label>
            <div className="relative">
              <Lock className="w-5 h-5 absolute left-3 top-2.5 text-gray-400" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-600"
              />
            </div>
          </div>

          <button
            type="submit"
            className="w-full bg-emerald-800 hover:bg-emerald-700 text-white font-semibold py-2.5 px-4 rounded-lg flex items-center justify-center space-x-2 transition shadow-md"
          >
            <span>Sign In to MFarm</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>
      </div>
    </div>
  );
};
