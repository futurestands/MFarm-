import React from 'react';
import { Farm } from '../types';
import { Building2, ArrowRight, ShieldCheck, MapPin } from 'lucide-react';

interface FarmSelectorPageProps {
  farms: Farm[];
  onSelectFarm: (farm: Farm) => void;
}

export const FarmSelectorPage: React.FC<FarmSelectorPageProps> = ({
  farms,
  onSelectFarm
}) => {
  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center p-4">
      <div className="max-w-xl w-full bg-white rounded-2xl shadow-lg p-6 sm:p-8">
        <div className="text-center mb-6">
          <Building2 className="w-12 h-12 mx-auto text-emerald-800 mb-2" />
          <h2 className="text-2xl font-bold text-gray-900">Select Active Farm</h2>
          <p className="text-sm text-gray-500 mt-1">
            Choose a farm workspace assigned to your account
          </p>
        </div>

        <div className="space-y-3">
          {farms.map((farm) => (
            <div
              key={farm.id}
              onClick={() => onSelectFarm(farm)}
              className="p-4 border border-gray-200 rounded-xl hover:border-emerald-600 hover:bg-emerald-50/50 cursor-pointer transition flex items-center justify-between group"
            >
              <div>
                <div className="flex items-center space-x-2">
                  <h3 className="font-semibold text-gray-900 text-base">{farm.name}</h3>
                  <span className="text-xs bg-emerald-100 text-emerald-800 font-bold px-2 py-0.5 rounded">
                    {farm.role}
                  </span>
                </div>
                <div className="flex items-center text-xs text-gray-500 mt-1 space-x-4">
                  <span className="flex items-center">
                    <MapPin className="w-3.5 h-3.5 mr-1" />
                    {farm.location}
                  </span>
                  <span>Animals: {farm.animalCount !== undefined ? farm.animalCount : 0}</span>
                  <span>Crops: {farm.cropAcres !== undefined ? farm.cropAcres : 0} Acres</span>
                </div>
              </div>

              <div className="bg-gray-100 group-hover:bg-emerald-600 text-gray-500 group-hover:text-white p-2 rounded-lg transition">
                <ArrowRight className="w-5 h-5" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
