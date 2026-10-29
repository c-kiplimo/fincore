'use client';

import { Card, CardContent, CardHeader, CardTitle, Input, Button } from '@fincore/ui';
import { useState } from 'react';

export function ReportGenerator() {
  const [reportType, setReportType] = useState('transactions');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [format, setFormat] = useState('csv');

  const handleGenerate = () => {
    // TODO: Generate and download report
    console.log('Generate report:', { reportType, startDate, endDate, format });
    alert(`Generating ${reportType} report in ${format.toUpperCase()} format...`);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Create New Report</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              Report Type
            </label>
            <select
              value={reportType}
              onChange={(e) => setReportType(e.target.value)}
              className="flex h-10 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-platform-green"
            >
              <option value="transactions">Transaction Report</option>
              <option value="revenue">Revenue Summary</option>
              <option value="fees">Fee Breakdown</option>
              <option value="tax">Tax Report</option>
              <option value="payouts">Payout Report</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              Format
            </label>
            <select
              value={format}
              onChange={(e) => setFormat(e.target.value)}
              className="flex h-10 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-platform-green"
            >
              <option value="csv">CSV</option>
              <option value="pdf">PDF</option>
              <option value="xlsx">Excel (XLSX)</option>
            </select>
          </div>

          <Input
            label="Start Date"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            required
          />

          <Input
            label="End Date"
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            required
          />
        </div>

        <div className="mt-6 flex gap-2">
          <Button onClick={handleGenerate}>
            Generate Report
          </Button>
          <Button variant="outline">
            Schedule Automatic Report
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
