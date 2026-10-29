import { Header } from '@/components/layout/Header';
import { TransactionTable } from '@/components/features/transactions/TransactionTable';
import { TransactionFilters } from '@/components/features/transactions/TransactionFilters';

export default function TransactionsPage() {
  return (
    <div className="flex flex-col h-full">
      <Header title="Transactions" />

      <div className="flex-1 p-6 space-y-6">
        <TransactionFilters />
        <TransactionTable />
      </div>
    </div>
  );
}
