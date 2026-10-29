'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@payments-platform/ui';

export function MetricsCards() {
  // TODO: Fetch from API
  const metrics = {
    todayRevenue: 125000, // 1,250.00 KES in minor units
    pendingSettlement: 450000, // 4,500.00 KES
    transactionCount: 42,
    successRate: 98.5,
  };

  const formatCurrency = (amountMinor: number, currency = 'KES') => {
    const amount = amountMinor / 100;
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency,
      minimumFractionDigits: 2,
    }).format(amount);
  };

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-gray-600">
            Today's Revenue
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-semibold">
            {formatCurrency(metrics.todayRevenue)}
          </div>
          <p className="text-xs text-gray-500 mt-1">
            <span className="text-green-600">+12.5%</span> from yesterday
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
            {formatCurrency(metrics.pendingSettlement)}
          </div>
          <p className="text-xs text-gray-500 mt-1">
            Available for payout
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-gray-600">
            Transactions
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-semibold">
            {metrics.transactionCount}
          </div>
          <p className="text-xs text-gray-500 mt-1">
            Processed today
          </p>
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
            {metrics.successRate}%
          </div>
          <p className="text-xs text-gray-500 mt-1">
            Last 30 days
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
