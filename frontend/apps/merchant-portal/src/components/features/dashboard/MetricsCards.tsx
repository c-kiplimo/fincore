'use client';

import { useSession } from 'next-auth/react';
import { Card, CardContent, CardHeader, CardTitle, Skeleton } from '@fincore/ui';
import { useMerchantBalances } from '@fincore/api-client/hooks';
import { formatCurrency } from '@fincore/utils';
import { merchantsApi } from '@/lib/api';

export function MetricsCards() {
  const { data: session } = useSession();
  const merchantId = session?.user?.merchantId;

  const { data: balances, isLoading, error } = useMerchantBalances(
    merchantsApi,
    merchantId!,
    { enabled: !!merchantId }
  );

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[1, 2, 3, 4].map((i) => (
          <Card key={i}>
            <CardHeader>
              <Skeleton className="h-4 w-24" />
            </CardHeader>
            <CardContent>
              <Skeleton className="h-8 w-32 mb-2" />
              <Skeleton className="h-3 w-28" />
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  if (error || !balances) {
    return (
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="col-span-full">
          <CardContent className="pt-6">
            <p className="text-sm text-red-600">
              Failed to load metrics. Please try again.
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  // TODO: Backend - Create merchant metrics endpoint for transaction count and success rate
  // For now, showing balance-related metrics from the balances endpoint
  const metrics = {
    availableBalance: balances.availableForPayoutMinor,
    pendingSettlement: balances.payableMinor,
    reserveBalance: balances.reserveMinor,
    currency: balances.currency,
  };

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-gray-600">
            Available Balance
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-semibold">
            {formatCurrency(metrics.availableBalance, metrics.currency)}
          </div>
          <p className="text-xs text-gray-500 mt-1">
            Ready for payout
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-gray-600">
            Pending Settlement
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-semibold">
            {formatCurrency(metrics.pendingSettlement, metrics.currency)}
          </div>
          <p className="text-xs text-gray-500 mt-1">
            Processing payments
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-gray-600">
            Reserve Balance
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-semibold">
            {formatCurrency(metrics.reserveBalance, metrics.currency)}
          </div>
          <p className="text-xs text-gray-500 mt-1">
            Held for disputes
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-gray-600">
            Total Balances
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-semibold">
            {formatCurrency(
              metrics.availableBalance + metrics.pendingSettlement + metrics.reserveBalance,
              metrics.currency
            )}
          </div>
          <p className="text-xs text-gray-500 mt-1">
            All merchant funds
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
