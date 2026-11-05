'use client';

import { useSession } from 'next-auth/react';
import { Card, CardContent, CardHeader, CardTitle, Badge, Skeleton } from '@fincore/ui';
import { usePayments } from '@fincore/api-client/hooks';
import { formatCurrency, formatDateTime, getPaymentStatusVariant } from '@fincore/utils';
import { paymentsApi } from '@/lib/api';
import Link from 'next/link';

export function RecentTransactions() {
  const { data: session } = useSession();
  const merchantId = session?.user?.merchantId;

  const { data: paymentsData, isLoading, error } = usePayments(
    paymentsApi,
    { merchantId, pageSize: 5 },
    { enabled: !!merchantId }
  );

  if (isLoading) {
    return (
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>Recent Transactions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="border-b border-gray-100 pb-4">
                <Skeleton className="h-5 w-48 mb-2" />
                <Skeleton className="h-4 w-64" />
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error || !paymentsData) {
    return (
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>Recent Transactions</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-red-600">
            Failed to load transactions. Please try again.
          </p>
        </CardContent>
      </Card>
    );
  }

  const transactions = paymentsData.items || [];

  if (transactions.length === 0) {
    return (
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>Recent Transactions</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-gray-500 text-center py-8">
            No transactions yet. Your recent payments will appear here.
          </p>
        </CardContent>
      </Card>
    );
  }

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
                  <p className="font-medium">{txn.description || 'Payment'}</p>
                  <Badge variant={getPaymentStatusVariant(txn.status)}>
                    {txn.status}
                  </Badge>
                </div>
                <p className="text-sm text-gray-500 mt-1">
                  {txn.id} • {formatDateTime(txn.createdAt)}
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
