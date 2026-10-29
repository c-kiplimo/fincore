import { Header } from '@/components/layout/Header';
import { MetricsCards } from '@/components/features/dashboard/MetricsCards';
import { RevenueChart } from '@/components/features/dashboard/RevenueChart';
import { RecentTransactions } from '@/components/features/dashboard/RecentTransactions';

export default function DashboardPage() {
  return (
    <div className="flex flex-col h-full">
      <Header title="Dashboard" />

      <div className="flex-1 p-6 space-y-6">
        {/* Metrics Cards */}
        <MetricsCards />

        {/* Revenue Chart */}
        <RevenueChart />

        {/* Recent Transactions */}
        <RecentTransactions />
      </div>
    </div>
  );
}
