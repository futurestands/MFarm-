import React, { useState } from 'react';
import { CropItem } from '../types';
import { Sprout, Calendar, MapPin, Plus, X } from 'lucide-react';

interface CropsPageProps {
  crops: CropItem[];
  onCreateCrop?: (cropType: string, fieldName: string, areaAcres: number, plantingDate: string, expectedHarvest: string) => void;
}

export const CropsPage: React.FC<CropsPageProps> = ({ crops, onCreateCrop }) => {
  const [showModal, setShowModal] = useState(false);
  const [cropType, setCropType] = useState('Maize / Corn');
  const [fieldName, setFieldName] = useState('');
  const [areaAcres, setAreaAcres] = useState('2.5');
  const [plantingDate, setPlantingDate] = useState(new Date().toISOString().split('T')[0]);
  const [expectedHarvest, setExpectedHarvest] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (onCreateCrop && fieldName) {
      onCreateCrop(cropType, fieldName, parseFloat(areaAcres || '1'), plantingDate, expectedHarvest);
      setFieldName('');
      setExpectedHarvest('');
      setShowModal(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b pb-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Crop Enterprises & Field Manager</h1>
          <p className="text-sm text-gray-500">Track field plots, planting cycles, spray schedules, and harvest yields (PostgreSQL Live)</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-semibold px-3 py-2 rounded-lg flex items-center space-x-1 mt-2 sm:mt-0"
        >
          <Plus className="w-4 h-4" />
          <span>New Crop Enterprise</span>
        </button>
      </div>

      {showModal && (
        <div className="bg-emerald-50 border border-emerald-200 p-5 rounded-xl mb-4 space-y-3">
          <div className="flex justify-between items-center border-b pb-2">
            <h3 className="font-bold text-emerald-900 text-sm flex items-center">
              <Sprout className="w-4 h-4 mr-1.5 text-emerald-700" />
              Add New Crop Enterprise (PostgreSQL)
            </h3>
            <button onClick={() => setShowModal(false)} className="text-gray-400 hover:text-gray-600">
              <X className="w-4 h-4" />
            </button>
          </div>
          <form onSubmit={handleSubmit} className="grid grid-cols-1 sm:grid-cols-5 gap-3">
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Crop Type</label>
              <input
                type="text"
                placeholder="e.g. Maize, Coffee, Beans"
                required
                value={cropType}
                onChange={(e) => setCropType(e.target.value)}
                className="w-full p-2 border rounded text-sm bg-white"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Field / Plot Name</label>
              <input
                type="text"
                placeholder="e.g. North Field Plot 1"
                required
                value={fieldName}
                onChange={(e) => setFieldName(e.target.value)}
                className="w-full p-2 border rounded text-sm bg-white"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Area (Acres)</label>
              <input
                type="number"
                step="0.1"
                placeholder="e.g. 5.0"
                required
                value={areaAcres}
                onChange={(e) => setAreaAcres(e.target.value)}
                className="w-full p-2 border rounded text-sm bg-white"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Planting Date</label>
              <input
                type="date"
                required
                value={plantingDate}
                onChange={(e) => setPlantingDate(e.target.value)}
                className="w-full p-2 border rounded text-sm bg-white"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Expected Harvest</label>
              <input
                type="date"
                value={expectedHarvest}
                onChange={(e) => setExpectedHarvest(e.target.value)}
                className="w-full p-2 border rounded text-sm bg-white"
              />
            </div>
            <div className="sm:col-span-5 flex justify-end space-x-2 pt-2">
              <button
                type="button"
                onClick={() => setShowModal(false)}
                className="bg-gray-300 hover:bg-gray-400 text-gray-800 font-medium py-1.5 px-4 rounded text-xs"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="bg-emerald-800 hover:bg-emerald-700 text-white font-semibold py-1.5 px-4 rounded text-xs"
              >
                Save Crop Enterprise
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {crops.length === 0 ? (
          <div className="col-span-full bg-white p-8 rounded-xl border border-gray-200 text-center text-gray-400">
            No crop enterprises recorded yet. Click "New Crop Enterprise" above to add your first field plot.
          </div>
        ) : (
          crops.map((crop) => (
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
                  <span className="font-semibold text-gray-800">{crop.expectedHarvest || 'TBD'}</span>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
