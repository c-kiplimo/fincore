'use client';

import { useMemo } from 'react';
import { useSession } from 'next-auth/react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription, Skeleton } from '@fincore/ui';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { usePayments } from '@fincore/api-client/hooks';
import { PaymentStatus } from '@fincore/types';
import { paymentsApi } from '@/lib/api';

export function RevenueChart() {
  const { data: session } = useSession();
  const merchantId = session?.user?.merchantId;

  // Fetch last 7 days of payments
  const sevenDaysAgo = new Date();
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

  const { data: paymentsData, isLoading, error } = usePayments(
    paymentsApi,
    {
      merchantId,
      startDate: sevenDaysAgo.toISOString(),
      pageSize: 1000 // Get all payments from last 7 days
    },
    { enabled: !!merchantId }
  );

  // Aggregate payments by date
  const chartData = useMemo(() => {
    if (!paymentsData?.items) return [];

    // Create map of date -> total revenue
    const revenueByDate = new Map<string, number>();

    // Initialize all 7 days with 0
    for (let i = 6; i >= 0; i--) {
      const date = new Date();
      date.setDate(date.getDate() - i);
      const dateKey = date.toISOString().split('T')[0];
      revenueByDate.set(dateKey, 0);
    }

    // Sum up captured/succeeded payments
    paymentsData.items.forEach((payment) => {
      if (
        payment.status === PaymentStatus.CAPTURED ||
        payment.status === 'SUCCEEDED'
      ) {
        const dateKey = payment.createdAt.split('T')[0];
        const currentAmount = revenueByDate.get(dateKey) || 0;
        revenueByDate.set(dateKey, currentAmount + payment.amountMinor / 100);
      }
    });

    // Convert to array format for chart
    return Array.from(revenueByDate.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, value]) => ({
        date: new Date(date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
        value: Number(value.toFixed(2)),
      }));
  }, [paymentsData]);

  const currency = paymentsData?.items?.[0]?.currency || 'KES';

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Revenue (Last 7 Days)</CardTitle>
          <CardDescription>Daily revenue in {currency}</CardDescription>
        </CardHeader>
        <CardContent>
          <Skeleton className="h-[300px] w-full" />
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Revenue (Last 7 Days)</CardTitle>
          <CardDescription>Daily revenue in {currency}</CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-red-600">
            Failed to load revenue data. Please try again.
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Revenue (Last 7 Days)</CardTitle>
        <CardDescription>Daily revenue in {currency}</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="h-[300px]">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
              <XAxis
                dataKey="date"
                stroke="#6B7280"
                fontSize={12}
                tickLine={false}
              />
              <YAxis
                stroke="#6B7280"
                fontSize={12}
                tickLine={false}
                tickFormatter={(value) => `${value}`}
              />
              <Tooltip
                content={({ active, payload }) => {
                  if (active && payload && payload.length) {
                    return (
                      <div className="rounded-lg border border-gray-200 bg-white p-3 shadow-lg">
                        <p className="text-sm font-medium">{payload[0].payload.date}</p>
                        <p className="text-sm text-gray-600">
                          Revenue: {(payload[0].value as number).toFixed(2)} {currency}
                        </p>
                      </div>
                    );
                  }
                  return null;
                }}
              />
              <Line
                type="monotone"
                dataKey="value"
                stroke="#00D395"
                strokeWidth={2}
                dot={{ fill: '#00D395', r: 4 }}
                activeDot={{ r: 6 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}
