import { Header } from '@/components/layout/Header';
import { Card, CardContent, Badge, Button, Input } from '@payments-platform/ui';

export default function DisputesPage() {
  // TODO: Fetch from API
  const disputes = [
    {
      id: 'disp_abc123',
      paymentId: 'pi_xyz789',
      merchantId: 'mer_abc123',
      merchantName: 'Example Business Ltd',
      amountMinor: 10000,
      currency: 'KES',
      reason: 'FRAUDULENT',
      status: 'OPEN',
      createdAt: '2026-10-28T14:00:00Z',
      dueDate: '2026-11-11T23:59:59Z',
    },
    {
      id: 'disp_def456',
      paymentId: 'pi_abc123',
      merchantId: 'mer_def456',
      merchantName: 'Tech Startup Inc',
      amountMinor: 25000,
      currency: 'KES',
      reason: 'PRODUCT_NOT_RECEIVED',
      status: 'UNDER_REVIEW',
      createdAt: '2026-10-25T09:30:00Z',
      dueDate: '2026-11-08T23:59:59Z',
    },
  ];

  const formatCurrency = (amountMinor: number, currency: string) => {
    return `${(amountMinor / 100).toFixed(2)} ${currency}`;
  };

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'OPEN':
        return 'pending';
      case 'UNDER_REVIEW':
        return 'pending';
      case 'WON':
        return 'completed';
      case 'LOST':
        return 'failed';
      default:
        return 'pending';
    }
  };

  const getDaysRemaining = (dueDate: string) => {
    const days = Math.ceil(
      (new Date(dueDate).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24)
    );
    return days;
  };

  return (
    <div className="flex flex-col h-full">
      <Header title="Dispute Management" />

      <div className="flex-1 p-6 space-y-6">
        {/* Summary Cards */}
        <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
          <Card>
            <CardContent className="p-4">
              <div className="text-sm text-gray-600">Open Disputes</div>
              <div className="text-2xl font-semibold mt-1">2</div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="text-sm text-gray-600">Under Review</div>
              <div className="text-2xl font-semibold mt-1">1</div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="text-sm text-gray-600">Won (30 days)</div>
              <div className="text-2xl font-semibold mt-1 text-green-600">12</div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="text-sm text-gray-600">Lost (30 days)</div>
              <div className="text-2xl font-semibold mt-1 text-red-600">3</div>
            </CardContent>
          </Card>
        </div>

        {/* Filters */}
        <Card>
          <CardContent className="p-4">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
              <Input placeholder="Search by ID, merchant..." />
              <select className="h-10 rounded-lg border border-gray-300 px-3 text-sm">
                <option value="">All Statuses</option>
                <option value="OPEN">Open</option>
                <option value="UNDER_REVIEW">Under Review</option>
                <option value="WON">Won</option>
                <option value="LOST">Lost</option>
              </select>
              <select className="h-10 rounded-lg border border-gray-300 px-3 text-sm">
                <option value="">All Reasons</option>
                <option value="FRAUDULENT">Fraudulent</option>
                <option value="PRODUCT_NOT_RECEIVED">Product Not Received</option>
                <option value="DUPLICATE">Duplicate</option>
                <option value="NOT_AS_DESCRIBED">Not as Described</option>
              </select>
              <Input type="date" />
            </div>
          </CardContent>
        </Card>

        {/* Disputes Table */}
        <Card>
          <CardContent className="p-0">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="border-b border-gray-200 bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Dispute ID
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Merchant
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Amount
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Reason
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Due Date
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {disputes.map((dispute) => {
                    const daysRemaining = getDaysRemaining(dispute.dueDate);
                    return (
                      <tr key={dispute.id} className="hover:bg-gray-50">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm font-mono font-medium">{dispute.id}</div>
                          <div className="text-xs text-gray-500">{dispute.paymentId}</div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="text-sm font-medium">{dispute.merchantName}</div>
                          <div className="text-xs text-gray-500">{dispute.merchantId}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm font-medium">
                            {formatCurrency(dispute.amountMinor, dispute.currency)}
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="text-sm">{dispute.reason.replace(/_/g, ' ')}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <Badge variant={getStatusVariant(dispute.status)}>
                            {dispute.status.replace(/_/g, ' ')}
                          </Badge>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm">
                            {new Date(dispute.dueDate).toLocaleDateString()}
                          </div>
                          <div
                            className={`text-xs ${
                              daysRemaining <= 3 ? 'text-red-600' : 'text-gray-500'
                            }`}
                          >
                            {daysRemaining} days left
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right space-x-2">
                          <Button variant="ghost" size="sm">
                            View
                          </Button>
                          {dispute.status === 'OPEN' && (
                            <>
                              <Button variant="ghost" size="sm">
                                Upload Evidence
                              </Button>
                              <Button variant="ghost" size="sm">
                                Resolve
                              </Button>
                            </>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
