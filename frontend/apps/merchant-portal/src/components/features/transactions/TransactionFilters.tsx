'use client';

import { Card, CardContent, Input } from '@fincore/ui';

export function TransactionFilters() {
  return (
    <Card>
      <CardContent className="pt-6">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-4">
          <Input
            type="search"
            placeholder="Search by ID, customer..."
            label="Search"
          />

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              Status
            </label>
            <select className="flex h-10 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-platform-green">
              <option value="">All statuses</option>
              <option value="CAPTURED">Captured</option>
              <option value="REFUNDED">Refunded</option>
              <option value="DISPUTED">Disputed</option>
              <option value="DECLINED">Declined</option>
            </select>
          </div>

          <Input
            type="date"
            label="Start Date"
          />

          <Input
            type="date"
            label="End Date"
          />
        </div>
      </CardContent>
    </Card>
  );
}
