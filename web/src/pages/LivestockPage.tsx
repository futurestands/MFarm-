import React, { useState } from 'react';
import { LivestockItem, BatchItem } from '../types';
import { Beef, Layers, Plus, Tag, Calendar, Dna } from 'lucide-react';

interface LivestockPageProps {
  livestock: LivestockItem[];
  batches: BatchItem[];
  onRegisterAnimal?: (tag: string, species: string, breed: string, sex: string) => void;
}

export const LivestockPage: React.FC<LivestockPageProps> = ({ livestock, batches, onRegisterAnimal }) => {
  const [activeTab, setActiveTab] = useState<'individual' | 'batches'>('individual');
  const [showModal, setShowModal] = useState(false);
  const [tag, setTag] = useState('');
  const [species, setSpecies] = useState('Goat');
  const [breed, setBreed] = useState('Boer');
  const [sex, setSex] = useState('FEMALE');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (onRegisterAnimal && tag) {
      onRegisterAnimal(tag, species, breed, sex);
      setTag('');
      setShowModal(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b pb-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Livestock & Herd Management</h1>
          <p className="text-sm text-gray-500">Track individual animals, lineage, vaccinations, and flock batches (PostgreSQL Live)</p>
        </div>
        <div className="mt-2 sm:mt-0 flex space-x-2">
          <button
            onClick={() => setShowModal(true)}
            className="bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-semibold px-3 py-2 rounded-lg flex items-center space-x-1"
          >
            <Plus className="w-4 h-4" />
            <span>Register Animal</span>
          </button>
        </div>
      </div>

      {showModal && (
        <div className="bg-emerald-50 border border-emerald-200 p-4 rounded-xl mb-4">
          <h3 className="font-bold text-emerald-900 mb-2 text-sm">Register New Animal (Direct to PostgreSQL)</h3>
          <form onSubmit={handleSubmit} className="grid grid-cols-1 sm:grid-cols-4 gap-3">
            <input
              type="text"
              placeholder="Tag / ID (e.g. KZ-GOAT-05)"
              required
              value={tag}
              onChange={(e) => setTag(e.target.value)}
              className="p-2 border rounded text-sm"
            />
            <input
              type="text"
              placeholder="Species (e.g. Goat)"
              required
              value={species}
              onChange={(e) => setSpecies(e.target.value)}
              className="p-2 border rounded text-sm"
            />
            <input
              type="text"
              placeholder="Breed (e.g. Boer)"
              required
              value={breed}
              onChange={(e) => setBreed(e.target.value)}
              className="p-2 border rounded text-sm"
            />
            <div className="flex space-x-2">
              <button type="submit" className="flex-1 bg-emerald-800 text-white font-semibold py-2 px-3 rounded text-xs">
                Save to DB
              </button>
              <button type="button" onClick={() => setShowModal(false)} className="bg-gray-300 text-gray-800 py-2 px-3 rounded text-xs">
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Tab Switcher */}
      <div className="flex space-x-2 border-b border-gray-200">
        <button
          onClick={() => setActiveTab('individual')}
          className={`pb-2.5 px-3 text-sm font-semibold flex items-center space-x-2 border-b-2 transition ${
            activeTab === 'individual'
              ? 'border-emerald-700 text-emerald-800'
              : 'border-transparent text-gray-500 hover:text-gray-800'
          }`}
        >
          <Beef className="w-4 h-4" />
          <span>Individual Animals ({livestock.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('batches')}
          className={`pb-2.5 px-3 text-sm font-semibold flex items-center space-x-2 border-b-2 transition ${
            activeTab === 'batches'
              ? 'border-emerald-700 text-emerald-800'
              : 'border-transparent text-gray-500 hover:text-gray-800'
          }`}
        >
          <Layers className="w-4 h-4" />
          <span>Poultry & Herd Batches ({batches.length})</span>
        </button>
      </div>

      {activeTab === 'individual' ? (
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-600 font-semibold text-xs uppercase border-b">
                <tr>
                  <th className="p-3">Tag / ID</th>
                  <th className="p-3">Species</th>
                  <th className="p-3">Breed</th>
                  <th className="p-3">Sex</th>
                  <th className="p-3">Date of Birth</th>
                  <th className="p-3">Sire / Dam Lineage</th>
                  <th className="p-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {livestock.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="p-6 text-center text-gray-400">
                      Zero records found in database for this farm.
                    </td>
                  </tr>
                ) : (
                  livestock.map((item) => (
                    <tr key={item.id} className="hover:bg-gray-50/50">
                      <td className="p-3 font-semibold text-emerald-900 flex items-center">
                        <Tag className="w-4 h-4 text-emerald-600 mr-1.5" />
                        {item.tag}
                      </td>
                      <td className="p-3 text-gray-700">{item.species}</td>
                      <td className="p-3 text-gray-700">{item.breed}</td>
                      <td className="p-3 text-gray-700">{item.sex}</td>
                      <td className="p-3 text-gray-500 text-xs">{item.dob}</td>
                      <td className="p-3 text-xs text-gray-500">
                        {item.sireTag || item.damTag ? (
                          <span className="flex items-center space-x-1 text-emerald-800">
                            <Dna className="w-3.5 h-3.5 text-emerald-600" />
                            <span>Sire: {item.sireTag || 'N/A'} | Dam: {item.damTag || 'N/A'}</span>
                          </span>
                        ) : (
                          <span className="text-gray-400">Unrecorded</span>
                        )}
                      </td>
                      <td className="p-3">
                        <span className="bg-emerald-100 text-emerald-800 text-xs font-bold px-2 py-0.5 rounded">
                          {item.status}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {batches.map((b) => (
            <div key={b.id} className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm space-y-2">
              <div className="flex justify-between items-start">
                <h3 className="font-bold text-gray-900 text-base">{b.batchName}</h3>
                <span className="text-xs bg-amber-100 text-amber-900 font-bold px-2 py-0.5 rounded">
                  {b.species}
                </span>
              </div>
              <div className="text-sm text-gray-600 space-y-1">
                <p>Quantity: <span className="font-semibold text-gray-900">{b.quantity} Birds / Animals</span></p>
                <p>Housing / Pen: <span className="font-semibold text-gray-900">{b.housingUnit}</span></p>
                <p className="text-xs text-gray-400">Arrival Date: {b.arrivalDate}</p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
