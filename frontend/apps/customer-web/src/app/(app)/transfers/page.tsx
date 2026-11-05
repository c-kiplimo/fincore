'use client';

import { useState } from 'react';
import { useSession } from 'next-auth/react';
import { Card, CardContent, CardHeader, CardTitle, Button, Input, Badge, Skeleton } from '@fincore/ui';
import { useWallet, useTransferHistory, useTransfer, useWithdraw } from '@fincore/api-client/hooks';
import { formatCurrency, formatDateTime } from '@fincore/utils';
import { walletsApi } from '@/lib/api';
import { toast } from 'sonner';

export default function TransfersPage() {
  const { data: session } = useSession();
  const walletId = session?.user?.walletId;

  const [recipientId, setRecipientId] = useState('');
  const [transferAmount, setTransferAmount] = useState('');
  const [transferNote, setTransferNote] = useState('');

  const [bankAccountId, setBankAccountId] = useState('');
  const [withdrawAmount, setWithdrawAmount] = useState('');

  // Fetch wallet and transfer history
  const { data: wallet } = useWallet(walletsApi, walletId!, { enabled: !!walletId });
  const { data: transfers, isLoading: transfersLoading } = useTransferHistory(
    walletsApi,
    walletId!,
    { enabled: !!walletId }
  );

  const transferMutation = useTransfer(walletsApi, {
    onSuccess: () => {
      toast.success('Transfer completed successfully');
      setRecipientId('');
      setTransferAmount('');
      setTransferNote('');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Transfer failed');
    },
  });

  const withdrawMutation = useWithdraw(walletsApi, {
    onSuccess: () => {
      toast.success('Withdrawal initiated successfully');
      setBankAccountId('');
      setWithdrawAmount('');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Withdrawal failed');
    },
  });

  const handleTransfer = () => {
    if (!recipientId.trim()) {
      toast.error('Please enter recipient wallet ID or phone');
      return;
    }

    const amountValue = parseFloat(transferAmount);
    if (isNaN(amountValue) || amountValue <= 0) {
      toast.error('Please enter a valid amount');
      return;
    }

    const amountMinor = Math.round(amountValue * 100);
    if (wallet && amountMinor > wallet.balanceMinor) {
      toast.error('Insufficient balance');
      return;
    }

    transferMutation.mutate({
      fromWalletId: walletId!,
      data: {
        toWalletId: recipientId.trim(),
        amountMinor,
        currency: 'KES',
        description: transferNote.trim() || undefined,
      },
    });
  };

  const handleWithdraw = () => {
    if (!bankAccountId) {
      toast.error('Please select a bank account');
      return;
    }

    const amountValue = parseFloat(withdrawAmount);
    if (isNaN(amountValue) || amountValue <= 0) {
      toast.error('Please enter a valid amount');
      return;
    }

    const amountMinor = Math.round(amountValue * 100);
    const withdrawalFee = 2500; // 25 KES fee
    const totalDeduction = amountMinor + withdrawalFee;

    if (wallet && totalDeduction > wallet.balanceMinor) {
      toast.error('Insufficient balance (including fees)');
      return;
    }

    withdrawMutation.mutate({
      walletId: walletId!,
      data: {
        amountMinor,
        bankAccountId,
        currency: 'KES',
      },
    });
  };

  const recentTransfers = transfers?.slice(0, 10) || [];

  const transferFee = 0; // Free P2P transfers
  const withdrawalFee = 2500; // 25 KES withdrawal fee
  const transferTotal = transferAmount
    ? Math.round(parseFloat(transferAmount) * 100) + transferFee
    : 0;
  const withdrawTotal = withdrawAmount
    ? Math.round(parseFloat(withdrawAmount) * 100) + withdrawalFee
    : 0;

  return (
    <div className="p-4 space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-semibold">Transfer Money</h1>
        <p className="text-sm text-gray-600 mt-1">Send to friends or withdraw to bank</p>
      </div>

      {/* Transfer Options */}
      <div className="grid grid-cols-2 gap-3">
        <button className="flex flex-col items-center justify-center p-6 bg-white rounded-xl border border-gray-200 hover:border-platform-green transition-colors">
          <div className="h-16 w-16 rounded-full bg-platform-green bg-opacity-10 flex items-center justify-center mb-3">
            <span className="text-3xl">👥</span>
          </div>
          <span className="font-medium">Send to Friend</span>
          <span className="text-xs text-gray-500 mt-1">P2P Transfer</span>
        </button>
        <button className="flex flex-col items-center justify-center p-6 bg-white rounded-xl border border-gray-200 hover:border-platform-green transition-colors">
          <div className="h-16 w-16 rounded-full bg-platform-green bg-opacity-10 flex items-center justify-center mb-3">
            <span className="text-3xl">🏦</span>
          </div>
          <span className="font-medium">Withdraw</span>
          <span className="text-xs text-gray-500 mt-1">To Bank Account</span>
        </button>
      </div>

      {/* Send Money Form */}
      <Card>
        <CardHeader>
          <CardTitle>Send to Wallet</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Recipient Phone or Wallet ID
            </label>
            <Input
              placeholder="+254712345678 or wal_xyz..."
              value={recipientId}
              onChange={(e) => setRecipientId(e.target.value)}
              disabled={transferMutation.isPending}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Amount (KES)
            </label>
            <Input
              type="number"
              placeholder="0.00"
              value={transferAmount}
              onChange={(e) => setTransferAmount(e.target.value)}
              disabled={transferMutation.isPending}
              min="0"
              step="0.01"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Note (Optional)
            </label>
            <Input
              placeholder="What's this for?"
              value={transferNote}
              onChange={(e) => setTransferNote(e.target.value)}
              disabled={transferMutation.isPending}
            />
          </div>

          <div className="bg-gray-50 rounded-lg p-3 space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-600">Transfer Fee</span>
              <span className="font-medium">{formatCurrency(transferFee, 'KES')}</span>
            </div>
            <div className="flex justify-between border-t pt-2">
              <span className="font-medium">Total</span>
              <span className="font-semibold">
                {transferAmount ? formatCurrency(transferTotal, 'KES') : '0.00 KES'}
              </span>
            </div>
          </div>

          <Button
            variant="primary"
            className="w-full"
            onClick={handleTransfer}
            disabled={transferMutation.isPending}
          >
            {transferMutation.isPending ? 'Processing...' : 'Send Money'}
          </Button>
        </CardContent>
      </Card>

      {/* Withdraw to Bank */}
      <Card>
        <CardHeader>
          <CardTitle>Withdraw to Bank Account</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Bank Account
            </label>
            <select
              className="w-full h-10 rounded-lg border border-gray-300 px-3 text-sm"
              value={bankAccountId}
              onChange={(e) => setBankAccountId(e.target.value)}
              disabled={withdrawMutation.isPending}
            >
              <option value="">Select saved bank account...</option>
              <option value="bank_1">KCB Bank - ****1234</option>
              <option value="bank_2">Equity Bank - ****5678</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Amount (KES)
            </label>
            <Input
              type="number"
              placeholder="0.00"
              value={withdrawAmount}
              onChange={(e) => setWithdrawAmount(e.target.value)}
              disabled={withdrawMutation.isPending}
              min="0"
              step="0.01"
            />
          </div>

          <div className="bg-gray-50 rounded-lg p-3 space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-600">Withdrawal Fee</span>
              <span className="font-medium">{formatCurrency(withdrawalFee, 'KES')}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Processing Time</span>
              <span className="font-medium">1-2 business days</span>
            </div>
            <div className="flex justify-between border-t pt-2">
              <span className="font-medium">Total Deducted</span>
              <span className="font-semibold">
                {withdrawAmount ? formatCurrency(withdrawTotal, 'KES') : '0.00 KES'}
              </span>
            </div>
          </div>

          <Button
            variant="primary"
            className="w-full"
            onClick={handleWithdraw}
            disabled={withdrawMutation.isPending}
          >
            {withdrawMutation.isPending ? 'Processing...' : 'Withdraw to Bank'}
          </Button>
        </CardContent>
      </Card>

      {/* Transfer History */}
      <div>
        <h2 className="text-lg font-semibold mb-4">Recent Transfers</h2>
        <Card>
          <CardContent className="p-0">
            {transfersLoading ? (
              <div className="p-4 space-y-3">
                <Skeleton className="h-16 w-full" />
                <Skeleton className="h-16 w-full" />
                <Skeleton className="h-16 w-full" />
              </div>
            ) : recentTransfers.length === 0 ? (
              <div className="p-12 text-center">
                <p className="text-gray-500">No transfers yet</p>
                <p className="text-sm text-gray-400 mt-1">
                  Your transfer history will appear here
                </p>
              </div>
            ) : (
              <div className="divide-y divide-gray-200">
                {recentTransfers.map((transfer) => {
                  const isReceive = transfer.toWalletId === walletId;
                  const isWithdrawal = transfer.type === 'WITHDRAWAL';

                  return (
                    <div key={transfer.id} className="p-4 hover:bg-gray-50">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <div
                            className={`h-10 w-10 rounded-full flex items-center justify-center ${
                              isReceive
                                ? 'bg-green-100 text-green-600'
                                : 'bg-gray-100 text-gray-600'
                            }`}
                          >
                            <span className="text-lg">
                              {isReceive ? '↓' : isWithdrawal ? '🏦' : '↑'}
                            </span>
                          </div>
                          <div>
                            <div className="font-medium text-sm">
                              {isWithdrawal
                                ? 'Withdrawal to Bank'
                                : isReceive
                                ? `From ${transfer.fromWalletId?.slice(0, 8)}...`
                                : `To ${transfer.toWalletId?.slice(0, 8)}...`}
                            </div>
                            <div className="text-xs text-gray-500">
                              {formatDateTime(transfer.createdAt)}
                            </div>
                          </div>
                        </div>
                        <div className="text-right">
                          <div
                            className={`font-semibold ${
                              isReceive ? 'text-green-600' : 'text-gray-900'
                            }`}
                          >
                            {isReceive ? '+' : '-'}
                            {formatCurrency(transfer.amountMinor, transfer.currency)}
                          </div>
                          <Badge variant="completed" className="mt-1">
                            {transfer.status}
                          </Badge>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
