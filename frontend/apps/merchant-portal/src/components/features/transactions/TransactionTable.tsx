'use client';

import { Card, CardContent, Badge, Button } from '@fincore/ui';
import { PaymentStatus } from '@fincore/types';
import { useState } from 'react';

export function TransactionTable() {
  const [selectedTransaction, setSelectedTransaction] = useState<string | null>(null);

  // TODO: Fetch from API using @tanstack/react-query
  const transactions = [
    {
      id: 'pi_1abc123',
      amountMinor: 10000,
      currency: 'KES',
      status: PaymentStatus.CAPTURED,
      method: 'CARD',
      description: 'Payment from customer',
      capturedMinor: 10000,
      feeMinor: 250,
      netMinor: 9750,
      createdAt: '2026-10-29T10:30:00Z',
    },
    {
      id: 'pi_2def456',
      amountMinor: 5000,
      currency: 'KES',
      status: PaymentStatus.REFUNDED,
      method: 'MOBILE_MONEY',
      description: 'Refunded order #1234',
      capturedMinor: 5000,
      refundedMinor: 5000,
      feeMinor: 125,
      netMinor: 4875,
      createdAt: '2026-10-29T09:15:00Z',
    },
    {
      id: 'pi_3ghi789',
      amountMinor: 15000,
      currency: 'KES',
      status: PaymentStatus.CAPTURED,
      method: 'CARD',
      description: 'Subscription payment',
      capturedMinor: 15000,
      feeMinor: 375,
      netMinor: 14625,
      createdAt: '2026-10-29T08:00:00Z',
    },
  ];

  const formatCurrency = (amountMinor: number, currency: string) => {
    return `${(amountMinor / 100).toFixed(2)} ${currency}`;
  };

  const getStatusVariant = (status: PaymentStatus) => {
    const variants: Record<PaymentStatus, any> = {
      [PaymentStatus.AUTHORIZED]: 'processing',
      [PaymentStatus.CAPTURED]: 'completed',
      [PaymentStatus.PARTIALLY_REFUNDED]: 'warning',
      [PaymentStatus.REFUNDED]: 'canceled',
      [PaymentStatus.DISPUTED]: 'warning',
      [PaymentStatus.CHARGED_BACK]: 'failed',
      [PaymentStatus.CANCELED]: 'canceled',
      [PaymentStatus.DECLINED]: 'failed',
    };
    return variants[status];
  };

  const handleRefund = (transactionId: string) => {
    // TODO: Implement refund dialog
    console.log('Refund transaction:', transactionId);
  };

  return (
    <Card>
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="border-b border-gray-200 bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Transaction ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Description
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Amount
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Fee
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Net
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Date
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {transactions.map((txn) => (
                <tr
                  key={txn.id}
                  className="hover:bg-gray-50 cursor-pointer"
                  onClick={() => setSelectedTransaction(txn.id)}
                >
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-mono font-medium text-gray-900">
                      {txn.id}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="text-sm text-gray-900">{txn.description}</div>
                    <div className="text-sm text-gray-500">{txn.method}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">
                      {formatCurrency(txn.amountMinor, txn.currency)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-500">
                      {formatCurrency(txn.feeMinor, txn.currency)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">
                      {formatCurrency(txn.netMinor, txn.currency)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <Badge variant={getStatusVariant(txn.status)}>
                      {txn.status}
                    </Badge>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-500">
                      {new Date(txn.createdAt).toLocaleString('en-US', {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    {txn.status === PaymentStatus.CAPTURED && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleRefund(txn.id);
                        }}
                      >
                        Refund
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="flex items-center justify-between border-t border-gray-200 bg-white px-6 py-4">
          <div className="flex-1 flex justify-between sm:hidden">
            <Button variant="outline" size="sm">Previous</Button>
            <Button variant="outline" size="sm">Next</Button>
          </div>
          <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
            <div>
              <p className="text-sm text-gray-700">
                Showing <span className="font-medium">1</span> to <span className="font-medium">3</span> of{' '}
                <span className="font-medium">3</span> results
              </p>
            </div>
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
      </CardContent>
    </Card>
  );
}
