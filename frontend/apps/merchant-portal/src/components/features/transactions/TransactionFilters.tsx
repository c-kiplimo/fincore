'use client';

import { Card, CardContent, Input } from '@fincore/ui';

interface TransactionFiltersProps {
  onFilterChange: (filters: {
    search: string;
    status: string;
    startDate: string;
    endDate: string;
  }) => void;
  filters: {
    search: string;
    status: string;
    startDate: string;
    endDate: string;
  };
}

export function TransactionFilters({ onFilterChange, filters }: TransactionFiltersProps) {
  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onFilterChange({ ...filters, search: e.target.value });
  };

  const handleStatusChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    onFilterChange({ ...filters, status: e.target.value });
  };

  const handleStartDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onFilterChange({ ...filters, startDate: e.target.value });
  };

  const handleEndDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onFilterChange({ ...filters, endDate: e.target.value });
  };

  return (
    <Card>
      <CardContent className="pt-6">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-4">
          <Input
            type="search"
            placeholder="Search by ID, description..."
            label="Search"
            value={filters.search}
            onChange={handleSearchChange}
          />

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              Status
            </label>
            <select
              className="flex h-10 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-platform-green"
              value={filters.status}
              onChange={handleStatusChange}
            >
              <option value="">All statuses</option>
              <option value="AUTHORIZED">Authorized</option>
              <option value="CAPTURED">Captured</option>
              <option value="PARTIALLY_REFUNDED">Partially Refunded</option>
              <option value="REFUNDED">Refunded</option>
              <option value="DISPUTED">Disputed</option>
              <option value="CHARGED_BACK">Charged Back</option>
              <option value="CANCELED">Canceled</option>
              <option value="DECLINED">Declined</option>
            </select>
          </div>

          <Input
            type="date"
            label="Start Date"
            value={filters.startDate}
            onChange={handleStartDateChange}
          />

          <Input
            type="date"
            label="End Date"
            value={filters.endDate}
            onChange={handleEndDateChange}
          />
        </div>
      </CardContent>
    </Card>
  );
}
