'use client';

import { Card, CardContent, CardHeader, CardTitle, Badge } from '@payments-platform/ui';
import { PaymentStatus } from '@payments-platform/types';
import Link from 'next/link';

export function RecentTransactions() {
  // TODO: Fetch from API
  const transactions = [
    {
      id: 'pi_abc123',
      amountMinor: 10000,
      currency: 'KES',
      status: PaymentStatus.CAPTURED,
      description: 'Payment from customer',
      createdAt: '2026-10-29T10:30:00Z',
    },
    {
      id: 'pi_def456',
      amountMinor: 5000,
      currency: 'KES',
      status: PaymentStatus.REFUNDED,
      description: 'Refunded order #1234',
      createdAt: '2026-10-29T09:15:00Z',
    },
    {
      id: 'pi_ghi789',
      amountMinor: 15000,
      currency: 'KES',
      status: PaymentStatus.CAPTURED,
      description: 'Subscription payment',
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

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>Recent Transactions</CardTitle>
        <Link
          href="/transactions"
          className="text-sm font-medium text-platform-green hover:text-platform-green-dark"
        >
          View all →
        </Link>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {transactions.map((txn) => (
            <div
              key={txn.id}
              className="flex items-center justify-between border-b border-gray-100 pb-4 last:border-0 last:pb-0"
            >
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <p className="font-medium">{txn.description}</p>
                  <Badge variant={getStatusVariant(txn.status)}>
                    {txn.status}
                  </Badge>
                </div>
                <p className="text-sm text-gray-500 mt-1">
                  {txn.id} • {new Date(txn.createdAt).toLocaleString()}
                </p>
              </div>
              <div className="text-right">
                <p className="font-semibold">
                  {formatCurrency(txn.amountMinor, txn.currency)}
                </p>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
