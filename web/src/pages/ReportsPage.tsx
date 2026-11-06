import React from 'react';
import { FileSpreadsheet, Download, FileText } from 'lucide-react';
import { DEMO_FINANCIALS, DEMO_LIVESTOCK } from '../data/demoData';

export const ReportsPage: React.FC = () => {
  const handleDownloadCsv = () => {
    const headers = ['id', 'type', 'category', 'amount', 'date', 'notes'];
    const rows = DEMO_FINANCIALS.map(f => [
      f.id,
      f.type,
      `"${f.category}"`,
      f.amount,
      f.date,
      `"${f.notes}"`
    ]);
    const csvContent = "data:text/csv;charset=utf-8," + [headers.join(','), ...rows.map(e => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `MFarm_Financial_Ledger_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleDownloadJson = () => {
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify({
      report: "MFarm Livestock Health & Pedigree Compliance",
      generatedAt: new Date().toISOString(),
      livestock: DEMO_LIVESTOCK
    }, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute("href", dataStr);
    downloadAnchor.setAttribute("download", `MFarm_Livestock_Report_${Date.now()}.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  return (
    <div className="space-y-6">
      <div className="border-b pb-4">
        <h1 className="text-2xl font-bold text-gray-900">Reports & Export Hub</h1>
        <p className="text-sm text-gray-500">Generate downloadable data exports and compliance summaries [DEMO DATA]</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm space-y-3">
          <div className="flex items-center space-x-3 text-emerald-800">
            <FileSpreadsheet className="w-6 h-6" />
            <h3 className="font-bold text-base text-gray-900">Financial Ledger Export (.csv)</h3>
          </div>
          <p className="text-xs text-gray-500">
            Downloads a formatted CSV spreadsheet containing all expense entries, income logs, and notes for accounting.
          </p>
          <button
            onClick={handleDownloadCsv}
            className="w-full bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-semibold py-2 px-3 rounded-lg flex items-center justify-center space-x-2 transition shadow-sm"
          >
            <Download className="w-4 h-4" />
            <span>Download CSV Spreadsheet</span>
          </button>
        </div>

        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm space-y-3">
          <div className="flex items-center space-x-3 text-emerald-800">
            <FileText className="w-6 h-6" />
            <h3 className="font-bold text-base text-gray-900">Livestock Health Report (.json / .pdf)</h3>
          </div>
          <p className="text-xs text-gray-500">
            Downloads a structured compliance record listing animal counts, tags, breeds, and lineage pedigrees.
          </p>
          <button
            onClick={handleDownloadJson}
            className="w-full bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-semibold py-2 px-3 rounded-lg flex items-center justify-center space-x-2 transition shadow-sm"
          >
            <Download className="w-4 h-4" />
            <span>Download Compliance Report</span>
          </button>
        </div>
      </div>
    </div>
  );
};
