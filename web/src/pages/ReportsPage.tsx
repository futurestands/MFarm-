import React from 'react';
import { FileSpreadsheet, Download, FileText, CheckCircle2 } from 'lucide-react';

export const ReportsPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="border-b pb-4">
        <h1 className="text-2xl font-bold text-gray-900">Reports & Export Hub</h1>
        <p className="text-sm text-gray-500">Generate downloadable PDF summaries and Excel workbooks [DEMO DATA]</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm space-y-3">
          <div className="flex items-center space-x-3 text-emerald-800">
            <FileSpreadsheet className="w-6 h-6" />
            <h3 className="font-bold text-base text-gray-900">Financial Ledger Export (.xlsx)</h3>
          </div>
          <p className="text-xs text-gray-500">
            Generates a complete multi-tab Excel workbook containing all expense entries, income logs, and batch sales.
          </p>
          <button
            onClick={() => alert('[DEMO MODE] Downloading Excel Financial Summary sample.')}
            className="w-full bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-semibold py-2 px-3 rounded-lg flex items-center justify-center space-x-2 transition"
          >
            <Download className="w-4 h-4" />
            <span>Download Excel Workbook</span>
          </button>
        </div>

        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm space-y-3">
          <div className="flex items-center space-x-3 text-emerald-800">
            <FileText className="w-6 h-6" />
            <h3 className="font-bold text-base text-gray-900">Livestock & Herd Health Report (.pdf)</h3>
          </div>
          <p className="text-xs text-gray-500">
            Generates a PDF compliance document listing animal counts, vaccination due dates, and pedigree details.
          </p>
          <button
            onClick={() => alert('[DEMO MODE] Downloading PDF Livestock Report sample.')}
            className="w-full bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-semibold py-2 px-3 rounded-lg flex items-center justify-center space-x-2 transition"
          >
            <Download className="w-4 h-4" />
            <span>Download PDF Summary</span>
          </button>
        </div>
      </div>
    </div>
  );
};
