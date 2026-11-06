import React from 'react';
import { CropItem } from '../types';
import { Sprout, Calendar, MapPin, Plus } from 'lucide-react';

interface CropsPageProps {
  crops: CropItem[];
}

export const CropsPage: React.FC<CropsPageProps> = ({ crops }) => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b pb-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Crop Enterprises & Field Manager</h1>
          <p className="text-sm text-gray-500">Track field plots, planting cycles, spray schedules, and harvest yields</p>
        </div>
        <button className="bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-semibold px-3 py-2 rounded-lg flex items-center space-x-1 mt-2 sm:mt-0">
          <Plus className="w-4 h-4" />
          <span>New Crop Enterprise</span>
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {crops.map((crop) => (
          <div key={crop.id} className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm space-y-3">
            <div className="flex justify-between items-start">
              <div>
                <h3 className="font-bold text-gray-900 text-lg">{crop.cropType}</h3>
                <p className="text-xs text-gray-500 flex items-center mt-0.5">
                  <MapPin className="w-3.5 h-3.5 mr-1 text-emerald-600" />
                  {crop.fieldName} ({crop.areaAcres} Acres)
                </p>
              </div>
              <span className="text-xs bg-emerald-100 text-emerald-800 font-bold px-2.5 py-1 rounded-full">
                {crop.status}
              </span>
            </div>

            <div className="border-t pt-3 text-xs text-gray-600 space-y-1.5">
              <div className="flex justify-between">
                <span>Planting Date:</span>
                <span className="font-semibold text-gray-800">{crop.plantingDate}</span>
              </div>
              <div className="flex justify-between">
                <span>Expected Harvest:</span>
                <span className="font-semibold text-gray-800">{crop.expectedHarvest}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
