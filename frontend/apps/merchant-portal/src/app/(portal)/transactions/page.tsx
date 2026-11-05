'use client';

import { useState } from 'react';
import { Header } from '@/components/layout/Header';
import { TransactionTable } from '@/components/features/transactions/TransactionTable';
import { TransactionFilters } from '@/components/features/transactions/TransactionFilters';

export default function TransactionsPage() {
  const [filters, setFilters] = useState({
    search: '',
    status: '',
    startDate: '',
    endDate: '',
  });

  return (
    <div className="flex flex-col h-full">
      <Header title="Transactions" />

      <div className="flex-1 p-6 space-y-6">
        <TransactionFilters filters={filters} onFilterChange={setFilters} />
        <TransactionTable filters={filters} />
      </div>
    </div>
  );
}
