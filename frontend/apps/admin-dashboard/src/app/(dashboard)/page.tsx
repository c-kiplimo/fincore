'use client';

import { Header } from '@/components/layout/Header';
import { Card, CardContent, CardHeader, CardTitle, Skeleton } from '@fincore/ui';
import { usePlatformMetrics } from '@fincore/api-client/hooks';
import { formatCurrency } from '@fincore/utils';
import { adminApi } from '@/lib/api';

export default function DashboardPage() {
  const { data: metrics, isLoading, error } = usePlatformMetrics(adminApi);

  if (isLoading) {
    return (
      <div className="flex flex-col h-full">
        <Header title="Platform Overview" />
        <div className="flex-1 p-6 space-y-6">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {[1, 2, 3, 4].map((i) => (
              <Card key={i}>
                <CardHeader>
                  <Skeleton className="h-4 w-32" />
                </CardHeader>
                <CardContent>
                  <Skeleton className="h-8 w-24 mb-2" />
                  <Skeleton className="h-3 w-20" />
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (error || !metrics) {
    return (
      <div className="flex flex-col h-full">
        <Header title="Platform Overview" />
        <div className="flex-1 p-6">
          <Card>
            <CardContent className="pt-6">
              <p className="text-sm text-red-600 text-center">
                Failed to load platform metrics. Please try again.
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      <Header title="Platform Overview" />

      <div className="flex-1 p-6 space-y-6">
        {/* Platform Metrics */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-sm font-medium text-gray-600">
                Total Platform Revenue
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-semibold">
                {formatCurrency(metrics.totalRevenueMinor, metrics.currency)}
              </div>
              <p className="text-xs text-gray-500 mt-1">All time</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-sm font-medium text-gray-600">
                Active Merchants
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-semibold">
                {metrics.activeMerchants}
              </div>
              <p className="text-xs text-gray-500 mt-1">
                Total merchants: {metrics.totalMerchants}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-sm font-medium text-gray-600">
                Total Transactions
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-semibold">
                {metrics.totalTransactions.toLocaleString()}
              </div>
              <p className="text-xs text-gray-500 mt-1">All time</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-sm font-medium text-gray-600">
                Success Rate
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-semibold">
                {metrics.successRate.toFixed(1)}%
              </div>
              <p className="text-xs text-gray-500 mt-1">All transactions</p>
            </CardContent>
          </Card>
        </div>

        {/* Recent Activity */}
        <Card>
          <CardHeader>
            <CardTitle>Platform Statistics</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-gray-600">Total Payouts:</span>
                <p className="font-semibold">{formatCurrency(metrics.totalPayoutsMinor, metrics.currency)}</p>
              </div>
              <div>
                <span className="text-gray-600">Total Disputes:</span>
                <p className="font-semibold">{metrics.totalDisputes}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
