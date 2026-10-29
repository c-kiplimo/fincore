import { Header } from '@/components/layout/Header';
import { Card, CardContent, Badge, Button, Input } from '@payments-platform/ui';

export default function TransactionsPage() {
  // TODO: Fetch from API
  const transactions = [
    {
      id: 'pi_abc123',
      merchantId: 'mer_abc123',
      merchantName: 'Example Business Ltd',
      amountMinor: 10000,
      currency: 'KES',
      status: 'CAPTURED',
      method: 'CARD',
      createdAt: '2026-10-29T10:30:00Z',
    },
    {
      id: 'pi_def456',
      merchantId: 'mer_def456',
      merchantName: 'Tech Startup Inc',
      amountMinor: 25000,
      currency: 'KES',
      status: 'AUTHORIZED',
      method: 'CARD',
      createdAt: '2026-10-29T11:15:00Z',
    },
    {
      id: 'pi_ghi789',
      merchantId: 'mer_abc123',
      merchantName: 'Example Business Ltd',
      amountMinor: 5000,
      currency: 'KES',
      status: 'REFUNDED',
      method: 'MOBILE_MONEY',
      createdAt: '2026-10-29T09:00:00Z',
    },
  ];

  const formatCurrency = (amountMinor: number, currency: string) => {
    return `${(amountMinor / 100).toFixed(2)} ${currency}`;
  };

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'CAPTURED':
        return 'completed';
      case 'AUTHORIZED':
        return 'pending';
      case 'REFUNDED':
      case 'CANCELED':
        return 'failed';
      default:
        return 'pending';
    }
  };

  return (
    <div className="flex flex-col h-full">
      <Header title="Platform Transactions" />

      <div className="flex-1 p-6 space-y-6">
        {/* Filters */}
        <Card>
          <CardContent className="p-4">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
              <Input placeholder="Search by ID, merchant..." />
              <select className="h-10 rounded-lg border border-gray-300 px-3 text-sm">
                <option value="">All Statuses</option>
                <option value="AUTHORIZED">Authorized</option>
                <option value="CAPTURED">Captured</option>
                <option value="REFUNDED">Refunded</option>
                <option value="DISPUTED">Disputed</option>
              </select>
              <select className="h-10 rounded-lg border border-gray-300 px-3 text-sm">
                <option value="">All Methods</option>
                <option value="CARD">Card</option>
                <option value="MOBILE_MONEY">Mobile Money</option>
                <option value="BANK_TRANSFER">Bank Transfer</option>
              </select>
              <Input type="date" />
            </div>
          </CardContent>
        </Card>

        {/* Transactions Table */}
        <Card>
          <CardContent className="p-0">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="border-b border-gray-200 bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Transaction ID
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Merchant
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Amount
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Method
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Date
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {transactions.map((tx) => (
                    <tr key={tx.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-mono font-medium">{tx.id}</div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="text-sm font-medium">{tx.merchantName}</div>
                        <div className="text-xs text-gray-500">{tx.merchantId}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-medium">
                          {formatCurrency(tx.amountMinor, tx.currency)}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm">{tx.method}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <Badge variant={getStatusVariant(tx.status)}>{tx.status}</Badge>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-500">
                          {new Date(tx.createdAt).toLocaleString()}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right space-x-2">
                        <Button variant="ghost" size="sm">
                          View
                        </Button>
                        {tx.status === 'AUTHORIZED' && (
                          <Button variant="ghost" size="sm">
                            Retry
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>

        {/* Pagination */}
        <div className="flex items-center justify-between">
          <div className="text-sm text-gray-500">Showing 1-3 of 3 transactions</div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled>
              Previous
            </Button>
            <Button variant="outline" size="sm" disabled>
              Next
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
