import { Header } from '@/components/layout/Header';
import { Card, CardContent, CardHeader, CardTitle, Button, Input } from '@fincore/ui';

export default function ReportsPage() {
  const reportTypes = [
    {
      id: 'platform-revenue',
      name: 'Platform Revenue Report',
      description: 'Total fees collected across all merchants',
      frequency: 'Daily, Weekly, Monthly',
    },
    {
      id: 'transaction-volume',
      name: 'Transaction Volume Report',
      description: 'Number and value of transactions by status',
      frequency: 'Daily, Weekly, Monthly',
    },
    {
      id: 'merchant-performance',
      name: 'Merchant Performance Report',
      description: 'Revenue breakdown by merchant',
      frequency: 'Daily, Weekly, Monthly',
    },
    {
      id: 'success-rate',
      name: 'Success Rate Analysis',
      description: 'Authorization and capture success rates',
      frequency: 'Daily, Weekly',
    },
    {
      id: 'dispute-summary',
      name: 'Dispute Summary Report',
      description: 'All disputes with win/loss ratios',
      frequency: 'Weekly, Monthly',
    },
    {
      id: 'settlement-report',
      name: 'Settlement Report',
      description: 'Network settlement batches and amounts',
      frequency: 'Daily',
    },
    {
      id: 'payout-report',
      name: 'Payout Report',
      description: 'All merchant payouts with fees',
      frequency: 'Daily, Weekly, Monthly',
    },
    {
      id: 'chargeback-analysis',
      name: 'Chargeback Analysis',
      description: 'Chargeback rates by merchant and reason',
      frequency: 'Monthly',
    },
  ];

  const recentReports = [
    {
      id: 'rpt_abc123',
      name: 'Platform Revenue Report - October 2026',
      type: 'platform-revenue',
      generatedAt: '2026-10-29T08:00:00Z',
      size: '2.4 MB',
      format: 'PDF',
    },
    {
      id: 'rpt_def456',
      name: 'Transaction Volume Report - Week 43',
      type: 'transaction-volume',
      generatedAt: '2026-10-28T09:00:00Z',
      size: '1.8 MB',
      format: 'CSV',
    },
    {
      id: 'rpt_ghi789',
      name: 'Merchant Performance Report - October 2026',
      type: 'merchant-performance',
      generatedAt: '2026-10-27T10:00:00Z',
      size: '3.1 MB',
      format: 'Excel',
    },
  ];

  return (
    <div className="flex flex-col h-full">
      <Header title="Reports & Analytics" />

      <div className="flex-1 p-6 space-y-6">
        {/* Quick Stats */}
        <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
          <Card>
            <CardContent className="p-4">
              <div className="text-sm text-gray-600">Reports Generated (30d)</div>
              <div className="text-2xl font-semibold mt-1">142</div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="text-sm text-gray-600">Scheduled Reports</div>
              <div className="text-2xl font-semibold mt-1">8</div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="text-sm text-gray-600">Total Data Exported</div>
              <div className="text-2xl font-semibold mt-1">245 MB</div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="text-sm text-gray-600">Last Generated</div>
              <div className="text-2xl font-semibold mt-1">2h ago</div>
            </CardContent>
          </Card>
        </div>

        {/* Generate New Report */}
        <Card>
          <CardHeader>
            <CardTitle>Generate New Report</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Report Type
                </label>
                <select className="w-full h-10 rounded-lg border border-gray-300 px-3 text-sm">
                  <option value="">Select report type...</option>
                  {reportTypes.map((type) => (
                    <option key={type.id} value={type.id}>
                      {type.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Format
                </label>
                <select className="w-full h-10 rounded-lg border border-gray-300 px-3 text-sm">
                  <option value="pdf">PDF</option>
                  <option value="csv">CSV</option>
                  <option value="excel">Excel (XLSX)</option>
                  <option value="json">JSON</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Start Date
                </label>
                <Input type="date" defaultValue="2026-10-01" />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  End Date
                </label>
                <Input type="date" defaultValue="2026-10-29" />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Additional Filters (Optional)
              </label>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                <select className="h-10 rounded-lg border border-gray-300 px-3 text-sm">
                  <option value="">All Merchants</option>
                  <option value="mer_abc123">Example Business Ltd</option>
                  <option value="mer_def456">Tech Startup Inc</option>
                </select>
                <select className="h-10 rounded-lg border border-gray-300 px-3 text-sm">
                  <option value="">All Currencies</option>
                  <option value="KES">KES</option>
                  <option value="USD">USD</option>
                </select>
                <select className="h-10 rounded-lg border border-gray-300 px-3 text-sm">
                  <option value="">All Statuses</option>
                  <option value="CAPTURED">Captured</option>
                  <option value="REFUNDED">Refunded</option>
                </select>
              </div>
            </div>

            <div className="flex gap-3">
              <Button variant="primary">Generate Report</Button>
              <Button variant="outline">Schedule Report</Button>
            </div>
          </CardContent>
        </Card>

        {/* Available Report Types */}
        <Card>
          <CardHeader>
            <CardTitle>Available Report Types</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              {reportTypes.map((report) => (
                <div
                  key={report.id}
                  className="border border-gray-200 rounded-lg p-4 hover:border-platform-green transition-colors"
                >
                  <div className="font-medium">{report.name}</div>
                  <div className="text-sm text-gray-600 mt-1">{report.description}</div>
                  <div className="text-xs text-gray-500 mt-2">
                    Available: {report.frequency}
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Recent Reports */}
        <Card>
          <CardHeader>
            <CardTitle>Recent Reports</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="border-b border-gray-200 bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Report Name
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Generated
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Format
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Size
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {recentReports.map((report) => (
                    <tr key={report.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4">
                        <div className="text-sm font-medium">{report.name}</div>
                        <div className="text-xs text-gray-500">{report.id}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-500">
                          {new Date(report.generatedAt).toLocaleString()}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm">{report.format}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-500">{report.size}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right space-x-2">
                        <Button variant="ghost" size="sm">
                          Download
                        </Button>
                        <Button variant="ghost" size="sm">
                          Delete
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>

        {/* Scheduled Reports */}
        <Card>
          <CardHeader>
            <CardTitle>Scheduled Reports</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <div className="flex items-center justify-between p-4 border border-gray-200 rounded-lg">
                <div>
                  <div className="font-medium">Daily Platform Revenue Report</div>
                  <div className="text-sm text-gray-500">Runs daily at 00:00 UTC</div>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-sm text-green-600">Active</span>
                  <Button variant="ghost" size="sm">
                    Edit
                  </Button>
                </div>
              </div>
              <div className="flex items-center justify-between p-4 border border-gray-200 rounded-lg">
                <div>
                  <div className="font-medium">Weekly Transaction Volume Report</div>
                  <div className="text-sm text-gray-500">Runs Mondays at 06:00 UTC</div>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-sm text-green-600">Active</span>
                  <Button variant="ghost" size="sm">
                    Edit
                  </Button>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
