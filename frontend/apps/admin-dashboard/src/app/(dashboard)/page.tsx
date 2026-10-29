import { Header } from '@/components/layout/Header';
import { Card, CardContent, CardHeader, CardTitle } from '@fincore/ui';

export default function DashboardPage() {
  // TODO: Fetch from API
  const platformMetrics = {
    totalRevenue: 5000000, // 50,000.00
    activemerchants: 125,
    todayTransactions: 842,
    successRate: 97.8,
  };

  const formatCurrency = (amountMinor: number) => {
    return `${(amountMinor / 100).toFixed(2)} KES`;
  };

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
                {formatCurrency(platformMetrics.totalRevenue)}
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
                {platformMetrics.activemerchants}
              </div>
              <p className="text-xs text-gray-500 mt-1">
                <span className="text-green-600">+8</span> this month
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-sm font-medium text-gray-600">
                Today's Transactions
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-semibold">
                {platformMetrics.todayTransactions}
              </div>
              <p className="text-xs text-gray-500 mt-1">Across all merchants</p>
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
                {platformMetrics.successRate}%
              </div>
              <p className="text-xs text-gray-500 mt-1">Last 30 days</p>
            </CardContent>
          </Card>
        </div>

        {/* Recent Activity */}
        <Card>
          <CardHeader>
            <CardTitle>Recent Platform Activity</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-gray-500">
              Activity feed coming soon...
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
