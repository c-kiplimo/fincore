'use client';

import { useState } from 'react';
import { useSession } from 'next-auth/react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Button,
  Input,
} from '@fincore/ui';
import { useCreatePayout, useMerchantBalances } from '@fincore/api-client/hooks';
import { payoutsApi, merchantsApi } from '@/lib/api';
import { formatCurrency } from '@fincore/utils';
import { toast } from 'sonner';

interface RequestPayoutDialogProps {
  open: boolean;
  onClose: () => void;
}

export function RequestPayoutDialog({ open, onClose }: RequestPayoutDialogProps) {
  const { data: session } = useSession();
  const merchantId = session?.user?.merchantId;

  const [amount, setAmount] = useState('');

  const { data: balances } = useMerchantBalances(
    merchantsApi,
    merchantId!,
    { enabled: !!merchantId && open }
  );

  const createPayoutMutation = useCreatePayout(payoutsApi, {
    onSuccess: () => {
      toast.success('Payout requested successfully');
      onClose();
      setAmount('');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to request payout');
    },
  });

  if (!balances) return null;

  const availableBalance = balances.availableForPayoutMinor / 100;
  const currency = balances.currency;

  const handleRequestPayout = () => {
    if (!amount || parseFloat(amount) <= 0) {
      toast.error('Please enter a valid amount');
      return;
    }

    if (parseFloat(amount) > availableBalance) {
      toast.error(`Amount cannot exceed available balance of ${formatCurrency(balances.availableForPayoutMinor, currency)}`);
      return;
    }

    if (!merchantId) {
      toast.error('Merchant ID not found');
      return;
    }

    createPayoutMutation.mutate({
      data: {
        merchantId,
        amountMinor: Math.round(parseFloat(amount) * 100),
        currency,
        destinationType: 'BANK', // Default to bank
      },
    });
  };

  const handleMaxAmount = () => {
    setAmount(availableBalance.toString());
  };

  // Calculate estimated fee (0.5%)
  const estimatedFee = amount ? (parseFloat(amount) * 0.005) : 0;
  const totalDeduction = amount ? (parseFloat(amount) + estimatedFee) : 0;

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Request Payout</DialogTitle>
          <DialogDescription>
            Transfer funds from your available balance to your bank account.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="space-y-2 pb-4 border-b">
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Available Balance:</span>
              <span className="font-semibold text-platform-green">
                {formatCurrency(balances.availableForPayoutMinor, currency)}
              </span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Pending Settlement:</span>
              <span className="font-medium">
                {formatCurrency(balances.payableMinor, currency)}
              </span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Reserve:</span>
              <span className="font-medium">
                {formatCurrency(balances.reserveMinor, currency)}
              </span>
            </div>
          </div>

          <div>
            <label htmlFor="amount" className="block text-sm font-medium text-gray-700 mb-1.5">
              Payout Amount ({currency})
            </label>
            <div className="flex gap-2">
              <Input
                id="amount"
                type="number"
                step="0.01"
                min="0"
                max={availableBalance}
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
              <Button
                type="button"
                variant="outline"
                onClick={handleMaxAmount}
              >
                Max
              </Button>
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Minimum payout: {formatCurrency(1000, currency)}
            </p>
          </div>

          {amount && parseFloat(amount) > 0 && (
            <div className="space-y-2 p-3 bg-gray-50 rounded-lg">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Payout Amount:</span>
                <span className="font-medium">{currency} {parseFloat(amount).toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Processing Fee (0.5%):</span>
                <span className="font-medium text-gray-600">
                  {currency} {estimatedFee.toFixed(2)}
                </span>
              </div>
              <div className="flex justify-between text-sm border-t pt-2">
                <span className="font-medium">Total Deduction:</span>
                <span className="font-semibold">
                  {currency} {totalDeduction.toFixed(2)}
                </span>
              </div>
            </div>
          )}

          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
            <p className="text-xs text-blue-800">
              <strong>Note:</strong> Payouts are typically processed within 1-3 business days.
              You'll receive funds in your registered bank account ending in ****1234.
            </p>
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={onClose}
            disabled={createPayoutMutation.isPending}
          >
            Cancel
          </Button>
          <Button
            onClick={handleRequestPayout}
            disabled={createPayoutMutation.isPending || !amount || parseFloat(amount) <= 0}
          >
            {createPayoutMutation.isPending ? 'Processing...' : 'Request Payout'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
