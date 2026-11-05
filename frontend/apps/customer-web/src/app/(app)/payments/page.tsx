'use client';

import { useState } from 'react';
import { useSession } from 'next-auth/react';
import { Card, CardContent, CardHeader, CardTitle, Button, Input, Skeleton } from '@fincore/ui';
import { usePayments, useCreatePayment } from '@fincore/api-client/hooks';
import { formatCurrency, formatRelativeDate } from '@fincore/utils';
import { paymentsApi } from '@/lib/api';
import { toast } from 'sonner';

export default function PaymentsPage() {
  const { data: session } = useSession();
  const walletId = session?.user?.walletId;

  const [merchantId, setMerchantId] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');

  // Fetch recent payments to show recent merchants
  const { data: paymentsData, isLoading: paymentsLoading } = usePayments(
    paymentsApi,
    { limit: 20 },
    { enabled: !!walletId }
  );

  const createPaymentMutation = useCreatePayment(paymentsApi, {
    onSuccess: () => {
      toast.success('Payment initiated successfully');
      setMerchantId('');
      setAmount('');
      setDescription('');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to create payment');
    },
  });

  // Extract unique recent merchants from payment history
  const recentMerchants = paymentsData?.items
    ? Array.from(
        new Map(
          paymentsData.items
            .filter((p) => p.merchantId)
            .map((p) => [
              p.merchantId,
              {
                id: p.merchantId,
                name: p.merchantName || `Merchant ${p.merchantId.slice(0, 8)}`,
                lastPaid: p.createdAt,
              },
            ])
        ).values()
      ).slice(0, 5)
    : [];

  const handleCreatePayment = () => {
    if (!merchantId.trim()) {
      toast.error('Please enter a merchant ID');
      return;
    }

    const amountValue = parseFloat(amount);
    if (isNaN(amountValue) || amountValue <= 0) {
      toast.error('Please enter a valid amount');
      return;
    }

    createPaymentMutation.mutate({
      data: {
        merchantId: merchantId.trim(),
        amountMinor: Math.round(amountValue * 100),
        currency: 'KES',
        description: description.trim() || undefined,
      },
    });
  };

  const handlePayRecentMerchant = (merchantIdValue: string) => {
    setMerchantId(merchantIdValue);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="p-4 space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-semibold">Make a Payment</h1>
        <p className="text-sm text-gray-600 mt-1">Pay merchants or scan QR code</p>
      </div>

      {/* QR Code Scanner */}
      <Card>
        <CardContent className="p-6">
          <div className="flex flex-col items-center justify-center py-8">
            <div className="h-32 w-32 rounded-xl bg-gray-100 flex items-center justify-center mb-4">
              <span className="text-6xl">📷</span>
            </div>
            <h3 className="font-semibold mb-2">Scan QR Code</h3>
            <p className="text-sm text-gray-600 text-center mb-4">
              Scan merchant's QR code to pay instantly
            </p>
            <Button variant="primary">Open Camera</Button>
          </div>
        </CardContent>
      </Card>

      {/* Manual Payment */}
      <Card>
        <CardHeader>
          <CardTitle>Pay Merchant</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Merchant ID
            </label>
            <Input
              placeholder="Enter merchant ID..."
              value={merchantId}
              onChange={(e) => setMerchantId(e.target.value)}
              disabled={createPaymentMutation.isPending}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Amount (KES)
            </label>
            <Input
              type="number"
              placeholder="0.00"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              disabled={createPaymentMutation.isPending}
              min="0"
              step="0.01"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Description (Optional)
            </label>
            <Input
              placeholder="What's this for?"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={createPaymentMutation.isPending}
            />
          </div>

          <Button
            variant="primary"
            className="w-full"
            onClick={handleCreatePayment}
            disabled={createPaymentMutation.isPending}
          >
            {createPaymentMutation.isPending ? 'Processing...' : 'Continue to Payment'}
          </Button>
        </CardContent>
      </Card>

      {/* Recent Merchants */}
      <div>
        <h2 className="text-lg font-semibold mb-4">Recent Merchants</h2>
        {paymentsLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-20 w-full" />
            <Skeleton className="h-20 w-full" />
          </div>
        ) : recentMerchants.length === 0 ? (
          <Card>
            <CardContent className="p-6 text-center">
              <p className="text-gray-500 text-sm">No recent merchants</p>
              <p className="text-xs text-gray-400 mt-1">
                Your payment history will appear here
              </p>
            </CardContent>
          </Card>
        ) : (
          <div className="space-y-2">
            {recentMerchants.map((merchant) => (
              <Card
                key={merchant.id}
                className="hover:border-platform-green cursor-pointer"
                onClick={() => handlePayRecentMerchant(merchant.id)}
              >
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="h-10 w-10 rounded-full bg-platform-green bg-opacity-10 flex items-center justify-center">
                        <span className="text-platform-green font-semibold">
                          {merchant.name.charAt(0).toUpperCase()}
                        </span>
                      </div>
                      <div>
                        <div className="font-medium text-sm">{merchant.name}</div>
                        <div className="text-xs text-gray-500">
                          Last paid {formatRelativeDate(merchant.lastPaid)}
                        </div>
                      </div>
                    </div>
                    <Button variant="ghost" size="sm">
                      Pay
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
