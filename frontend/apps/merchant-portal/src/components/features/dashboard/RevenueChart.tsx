'use client';

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@payments-platform/ui';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export function RevenueChart() {
  // TODO: Fetch from API
  const data = [
    { date: 'Jan 1', value: 400 },
    { date: 'Jan 2', value: 300 },
    { date: 'Jan 3', value: 600 },
    { date: 'Jan 4', value: 800 },
    { date: 'Jan 5', value: 500 },
    { date: 'Jan 6', value: 700 },
    { date: 'Jan 7', value: 1250 },
  ];

  return (
    <Card>
      <CardHeader>
        <CardTitle>Revenue (Last 7 Days)</CardTitle>
        <CardDescription>Daily revenue in KES</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="h-[300px]">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data}>
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
                          Revenue: {(payload[0].value as number).toFixed(2)} KES
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
