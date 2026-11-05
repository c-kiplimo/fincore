'use client';

import { useState } from 'react';
import { useSession } from 'next-auth/react';
import { Card, CardContent, Badge, Button, Skeleton } from '@fincore/ui';
import { usePayments } from '@fincore/api-client/hooks';
import { formatCurrency, formatDateTime, getPaymentStatusVariant } from '@fincore/utils';
import { paymentsApi } from '@/lib/api';
import { RefundDialog } from './RefundDialog';
import type { PaymentIntent, PaymentStatus } from '@fincore/types';

interface TransactionTableProps {
  filters?: {
    search?: string;
    status?: string;
    startDate?: string;
    endDate?: string;
  };
}

export function TransactionTable({ filters }: TransactionTableProps) {
  const { data: session } = useSession();
  const merchantId = session?.user?.merchantId;

  const [page, setPage] = useState(1);
  const [pageSize] = useState(20);
  const [refundingPayment, setRefundingPayment] = useState<PaymentIntent | null>(null);

  const { data: paymentsData, isLoading, error } = usePayments(
    paymentsApi,
    {
      merchantId,
      page,
      pageSize,
      status: filters?.status,
      startDate: filters?.startDate,
      endDate: filters?.endDate,
    },
    { enabled: !!merchantId }
  );

  const transactions = paymentsData?.items || [];
  const totalPages = paymentsData?.totalPages || 1;
  const totalItems = paymentsData?.total || 0;

  // Client-side search filtering (if search term provided)
  const filteredTransactions = filters?.search
    ? transactions.filter((txn) =>
        txn.id.toLowerCase().includes(filters.search!.toLowerCase()) ||
        txn.description?.toLowerCase().includes(filters.search!.toLowerCase())
      )
    : transactions;

  const canRefund = (payment: PaymentIntent): boolean => {
    return payment.status === 'CAPTURED' &&
           (payment.refundedMinor === 0 || payment.refundedMinor < (payment.capturedMinor || payment.amountMinor));
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="space-y-4">
            {[1, 2, 3, 4, 5].map((i) => (
              <Skeleton key={i} className="h-16 w-full" />
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent className="p-6">
          <p className="text-sm text-red-600 text-center">
            Failed to load transactions. Please try again.
          </p>
        </CardContent>
      </Card>
    );
  }

  if (filteredTransactions.length === 0) {
    return (
      <Card>
        <CardContent className="p-12">
          <p className="text-sm text-gray-500 text-center">
            {filters?.search || filters?.status
              ? 'No transactions match your filters.'
              : 'No transactions yet. Your payments will appear here.'}
          </p>
        </CardContent>
      </Card>
    );
  }

  const startItem = (page - 1) * pageSize + 1;
  const endItem = Math.min(page * pageSize, totalItems);

  return (
    <>
      <Card>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="border-b border-gray-200 bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Transaction ID
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Description
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Amount
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Date
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredTransactions.map((txn) => (
                  <tr
                    key={txn.id}
                    className="hover:bg-gray-50"
                  >
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-mono font-medium text-gray-900">
                        {txn.id}
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="text-sm text-gray-900">{txn.description || 'Payment'}</div>
                      <div className="text-sm text-gray-500">{txn.method || 'N/A'}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">
                        {formatCurrency(txn.amountMinor, txn.currency)}
                      </div>
                      {txn.refundedMinor > 0 && (
                        <div className="text-xs text-red-600">
                          Refunded: {formatCurrency(txn.refundedMinor, txn.currency)}
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <Badge variant={getPaymentStatusVariant(txn.status)}>
                        {txn.status}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-500">
                        {formatDateTime(txn.createdAt)}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      {canRefund(txn) && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setRefundingPayment(txn)}
                        >
                          Refund
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="flex items-center justify-between border-t border-gray-200 bg-white px-6 py-4">
            <div className="flex-1 flex justify-between sm:hidden">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={page === 1}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                disabled={page === totalPages}
              >
                Next
              </Button>
            </div>
            <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
              <div>
                <p className="text-sm text-gray-700">
                  Showing <span className="font-medium">{startItem}</span> to{' '}
                  <span className="font-medium">{endItem}</span> of{' '}
                  <span className="font-medium">{totalItems}</span> results
                </p>
              </div>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => Math.max(1, p - 1))}
                  disabled={page === 1}
                >
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                  disabled={page === totalPages}
                >
                  Next
                </Button>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <RefundDialog
        payment={refundingPayment}
        open={!!refundingPayment}
        onClose={() => setRefundingPayment(null)}
      />
    </>
  );
}
