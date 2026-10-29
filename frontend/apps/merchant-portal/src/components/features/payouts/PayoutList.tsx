'use client';

import { Card, CardContent, Badge } from '@payments-platform/ui';
import { PayoutStatus } from '@payments-platform/types';

export function PayoutList() {
  // TODO: Fetch from API
  const payouts = [
    {
      id: 'po_abc123',
      amountMinor: 450000,
      currency: 'KES',
      feeMinor: 2250,
      totalDebitedMinor: 452250,
      status: PayoutStatus.PAID,
      destinationType: 'BANK',
      destinationAccount: '****1234',
      createdAt: '2026-10-28T14:00:00Z',
    },
    {
      id: 'po_def456',
      amountMinor: 320000,
      currency: 'KES',
      feeMinor: 1600,
      totalDebitedMinor: 321600,
      status: PayoutStatus.SENDING,
      destinationType: 'BANK',
      destinationAccount: '****1234',
      createdAt: '2026-10-29T10:00:00Z',
    },
  ];

  const formatCurrency = (amountMinor: number, currency: string) => {
    return `${(amountMinor / 100).toFixed(2)} ${currency}`;
  };

  const getStatusVariant = (status: PayoutStatus) => {
    const variants: Record<PayoutStatus, any> = {
      [PayoutStatus.CREATED]: 'pending',
      [PayoutStatus.SENDING]: 'processing',
      [PayoutStatus.SENT]: 'processing',
      [PayoutStatus.PAID]: 'completed',
      [PayoutStatus.FAILED]: 'failed',
    };
    return variants[status];
  };

  return (
    <Card>
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="border-b border-gray-200 bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Payout ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Amount
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Fee
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Total
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Destination
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Date
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {payouts.map((payout) => (
                <tr key={payout.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-mono font-medium">
                      {payout.id}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium">
                      {formatCurrency(payout.amountMinor, payout.currency)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-500">
                      {formatCurrency(payout.feeMinor, payout.currency)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium">
                      {formatCurrency(payout.totalDebitedMinor, payout.currency)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">
                      {payout.destinationType}
                    </div>
                    <div className="text-sm text-gray-500">
                      {payout.destinationAccount}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <Badge variant={getStatusVariant(payout.status)}>
                      {payout.status}
                    </Badge>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-500">
                      {new Date(payout.createdAt).toLocaleString('en-US', {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </CardContent>
    </Card>
  );
}
