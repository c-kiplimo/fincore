'use client';

import { useState } from 'react';
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
import { useRefundPayment } from '@fincore/api-client/hooks';
import { paymentsApi } from '@/lib/api';
import type { PaymentIntent } from '@fincore/types';
import { formatCurrency } from '@fincore/utils';
import { toast } from 'sonner';

interface RefundDialogProps {
  payment: PaymentIntent | null;
  open: boolean;
  onClose: () => void;
}

export function RefundDialog({ payment, open, onClose }: RefundDialogProps) {
  const [refundAmount, setRefundAmount] = useState('');
  const [reason, setReason] = useState('');

  const refundMutation = useRefundPayment(paymentsApi, {
    onSuccess: () => {
      toast.success('Refund processed successfully');
      onClose();
      setRefundAmount('');
      setReason('');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to process refund');
    },
  });

  if (!payment) return null;

  const maxRefundAmount = (payment.capturedMinor || payment.amountMinor) / 100;
  const isPartialRefund = refundAmount !== '' && parseFloat(refundAmount) < maxRefundAmount;

  const handleRefund = () => {
    if (!refundAmount || parseFloat(refundAmount) <= 0) {
      toast.error('Please enter a valid refund amount');
      return;
    }

    if (parseFloat(refundAmount) > maxRefundAmount) {
      toast.error(`Refund amount cannot exceed ${formatCurrency(payment.capturedMinor || payment.amountMinor, payment.currency)}`);
      return;
    }

    refundMutation.mutate({
      paymentId: payment.id,
      data: {
        amountMinor: Math.round(parseFloat(refundAmount) * 100),
        reason: reason || 'Merchant-initiated refund',
      },
    });
  };

  const handleFullRefund = () => {
    setRefundAmount(maxRefundAmount.toString());
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Refund Payment</DialogTitle>
          <DialogDescription>
            Process a full or partial refund for this payment.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Payment ID:</span>
              <span className="font-mono font-medium">{payment.id}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Original Amount:</span>
              <span className="font-medium">
                {formatCurrency(payment.amountMinor, payment.currency)}
              </span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Captured Amount:</span>
              <span className="font-medium">
                {formatCurrency(payment.capturedMinor || payment.amountMinor, payment.currency)}
              </span>
            </div>
            {payment.refundedMinor > 0 && (
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Already Refunded:</span>
                <span className="font-medium text-red-600">
                  {formatCurrency(payment.refundedMinor, payment.currency)}
                </span>
              </div>
            )}
          </div>

          <div className="border-t pt-4 space-y-4">
            <div>
              <label htmlFor="refundAmount" className="block text-sm font-medium text-gray-700 mb-1.5">
                Refund Amount ({payment.currency})
              </label>
              <div className="flex gap-2">
                <Input
                  id="refundAmount"
                  type="number"
                  step="0.01"
                  min="0"
                  max={maxRefundAmount}
                  placeholder="0.00"
                  value={refundAmount}
                  onChange={(e) => setRefundAmount(e.target.value)}
                />
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleFullRefund}
                >
                  Full
                </Button>
              </div>
              {isPartialRefund && (
                <p className="text-xs text-amber-600 mt-1">
                  This is a partial refund. Remaining: {formatCurrency(
                    (payment.capturedMinor || payment.amountMinor) - Math.round(parseFloat(refundAmount) * 100),
                    payment.currency
                  )}
                </p>
              )}
            </div>

            <div>
              <label htmlFor="reason" className="block text-sm font-medium text-gray-700 mb-1.5">
                Reason (Optional)
              </label>
              <Input
                id="reason"
                type="text"
                placeholder="E.g., Customer request, duplicate charge..."
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={onClose}
            disabled={refundMutation.isPending}
          >
            Cancel
          </Button>
          <Button
            onClick={handleRefund}
            disabled={refundMutation.isPending || !refundAmount}
          >
            {refundMutation.isPending ? 'Processing...' : 'Process Refund'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
