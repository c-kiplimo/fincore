import { Header } from '@/components/layout/Header';
import { Card, CardContent, Badge, Button, Input } from '@payments-platform/ui';

export default function SettlementsPage() {
  // TODO: Fetch from API
  const settlements = [
    {
      id: 'settle_abc123',
      batchId: 'batch_20261029_001',
      totalAmountMinor: 500000,
      transactionCount: 42,
      currency: 'KES',
      status: 'COMPLETED',
      initiatedAt: '2026-10-29T00:00:00Z',
      completedAt: '2026-10-29T03:30:00Z',
    },
    {
      id: 'settle_def456',
      batchId: 'batch_20261028_001',
      totalAmountMinor: 750000,
      transactionCount: 68,
      currency: 'KES',
      status: 'COMPLETED',
      initiatedAt: '2026-10-28T00:00:00Z',
      completedAt: '2026-10-28T04:15:00Z',
    },
  ];

  const formatCurrency = (amountMinor: number, currency: string) => {
    return `${(amountMinor / 100).toFixed(2)} ${currency}`;
  };

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'completed';
      case 'PENDING':
      case 'IN_PROGRESS':
        return 'pending';
      case 'FAILED':
        return 'failed';
      default:
        return 'pending';
    }
  };

  const getProcessingTime = (initiatedAt: string, completedAt: string) => {
    const diff = new Date(completedAt).getTime() - new Date(initiatedAt).getTime();
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    return `${hours}h ${minutes}m`;
  };

  return (
    <div className="flex flex-col h-full">
      <Header title="Network Settlements" />

      <div className="flex-1 p-6 space-y-6">
        {/* Summary Cards */}
        <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
          <Card>
            <CardContent className="p-4">
              <div className="text-sm text-gray-600">Today's Settlements</div>
              <div className="text-2xl font-semibold mt-1">1</div>
              <div className="text-xs text-gray-500 mt-1">5,000.00 KES</div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="text-sm text-gray-600">Pending Amount</div>
              <div className="text-2xl font-semibold mt-1">2,300.00 KES</div>
              <div className="text-xs text-gray-500 mt-1">23 transactions</div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="text-sm text-gray-600">Last Settlement</div>
              <div className="text-2xl font-semibold mt-1">3h ago</div>
              <div className="text-xs text-gray-500 mt-1">Completed</div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="text-sm text-gray-600">Success Rate (30d)</div>
              <div className="text-2xl font-semibold mt-1 text-green-600">100%</div>
              <div className="text-xs text-gray-500 mt-1">30/30 successful</div>
            </CardContent>
          </Card>
        </div>

        {/* Actions */}
        <div className="flex gap-4">
          <Button variant="primary">
            Trigger Manual Settlement
          </Button>
          <Button variant="outline">
            View Pending Transactions
          </Button>
          <Button variant="outline">
            Download Report
          </Button>
        </div>

        {/* Filters */}
        <Card>
          <CardContent className="p-4">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
              <Input placeholder="Search by batch ID..." />
              <select className="h-10 rounded-lg border border-gray-300 px-3 text-sm">
                <option value="">All Statuses</option>
                <option value="PENDING">Pending</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="COMPLETED">Completed</option>
                <option value="FAILED">Failed</option>
              </select>
              <Input type="date" placeholder="From date" />
              <Input type="date" placeholder="To date" />
            </div>
          </CardContent>
        </Card>

        {/* Settlements Table */}
        <Card>
          <CardContent className="p-0">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="border-b border-gray-200 bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Batch ID
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Amount
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Transactions
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Initiated
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Processing Time
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {settlements.map((settlement) => (
                    <tr key={settlement.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-mono font-medium">
                          {settlement.batchId}
                        </div>
                        <div className="text-xs text-gray-500">{settlement.id}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-medium">
                          {formatCurrency(settlement.totalAmountMinor, settlement.currency)}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm">{settlement.transactionCount}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <Badge variant={getStatusVariant(settlement.status)}>
                          {settlement.status}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm">
                          {new Date(settlement.initiatedAt).toLocaleString()}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm">
                          {settlement.completedAt
                            ? getProcessingTime(settlement.initiatedAt, settlement.completedAt)
                            : '-'}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right space-x-2">
                        <Button variant="ghost" size="sm">
                          View Details
                        </Button>
                        <Button variant="ghost" size="sm">
                          Download Report
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
