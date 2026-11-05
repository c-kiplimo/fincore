'use client';

import { useSession } from 'next-auth/react';
import { Card, CardContent, Button, Badge, Skeleton } from '@fincore/ui';
import { useWallet, useTransferHistory } from '@fincore/api-client/hooks';
import { formatCurrency, formatDateTime } from '@fincore/utils';
import { walletsApi } from '@/lib/api';

export default function WalletPage() {
  const { data: session } = useSession();
  const walletId = session?.user?.walletId; // Assuming wallet ID is stored in session

  const { data: wallet, isLoading: walletLoading, error: walletError } = useWallet(
    walletsApi,
    walletId!,
    { enabled: !!walletId }
  );

  const { data: transactions, isLoading: txnLoading } = useTransferHistory(
    walletsApi,
    walletId!,
    { enabled: !!walletId }
  );

  const recentTransactions = transactions?.slice(0, 5) || [];

  if (walletLoading) {
    return (
      <div className="p-4 space-y-6">
        <Card>
          <CardContent className="p-6">
            <Skeleton className="h-24 w-full" />
          </CardContent>
        </Card>
        <Skeleton className="h-48 w-full" />
      </div>
    );
  }

  if (walletError || !wallet) {
    return (
      <div className="p-4">
        <Card>
          <CardContent className="p-6">
            <p className="text-sm text-red-600 text-center">
              Failed to load wallet. Please try again.
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  const formatBalance = (amountMinor: number, currency: string) => {
    return `${(amountMinor / 100).toLocaleString('en', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })} ${currency}`;
  };

  return (
    <div className="p-4 space-y-6">
      {/* Balance Card */}
      <Card className="bg-gradient-to-br from-platform-green to-platform-green-dark text-white border-none">
        <CardContent className="p-6">
          <div className="text-sm opacity-90">Available Balance</div>
          <div className="text-4xl font-bold mt-2">
            {formatBalance(wallet.balanceMinor, wallet.currency)}
          </div>
          <div className="text-sm mt-4 opacity-90">
            Wallet ID: <span className="font-mono">{wallet.id}</span>
          </div>
          <div className="mt-2">
            <Badge variant={wallet.status === 'ACTIVE' ? 'completed' : 'failed'} className="bg-white bg-opacity-20 text-white">
              {wallet.status}
            </Badge>
          </div>
        </CardContent>
      </Card>

      {/* Quick Actions */}
      <div className="grid grid-cols-3 gap-3">
        <button className="flex flex-col items-center justify-center p-4 bg-white rounded-xl border border-gray-200 hover:border-platform-green transition-colors">
          <div className="h-12 w-12 rounded-full bg-platform-green bg-opacity-10 flex items-center justify-center mb-2">
            <span className="text-2xl">💵</span>
          </div>
          <span className="text-sm font-medium">Top Up</span>
        </button>
        <button className="flex flex-col items-center justify-center p-4 bg-white rounded-xl border border-gray-200 hover:border-platform-green transition-colors">
          <div className="h-12 w-12 rounded-full bg-platform-green bg-opacity-10 flex items-center justify-center mb-2">
            <span className="text-2xl">↗️</span>
          </div>
          <span className="text-sm font-medium">Send</span>
        </button>
        <button className="flex flex-col items-center justify-center p-4 bg-white rounded-xl border border-gray-200 hover:border-platform-green transition-colors">
          <div className="h-12 w-12 rounded-full bg-platform-green bg-opacity-10 flex items-center justify-center mb-2">
            <span className="text-2xl">📱</span>
          </div>
          <span className="text-sm font-medium">Request</span>
        </button>
      </div>

      {/* Recent Transactions */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold">Recent Transactions</h2>
          <Button variant="ghost" size="sm">
            View All
          </Button>
        </div>

        <Card>
          <CardContent className="p-0">
            {txnLoading ? (
              <div className="p-6">
                <Skeleton className="h-16 w-full mb-3" />
                <Skeleton className="h-16 w-full mb-3" />
                <Skeleton className="h-16 w-full" />
              </div>
            ) : recentTransactions.length === 0 ? (
              <div className="p-12 text-center">
                <p className="text-gray-500">No transactions yet</p>
                <p className="text-sm text-gray-400 mt-1">Your wallet activity will appear here</p>
              </div>
            ) : (
              <div className="divide-y divide-gray-200">
                {recentTransactions.map((txn) => {
                  const isCredit = txn.toWalletId === walletId;
                  const amountDisplay = txn.amountMinor;

                  return (
                    <div key={txn.id} className="p-4 hover:bg-gray-50 cursor-pointer">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <div
                            className={`h-10 w-10 rounded-full flex items-center justify-center ${
                              isCredit
                                ? 'bg-green-100 text-green-600'
                                : 'bg-gray-100 text-gray-600'
                            }`}
                          >
                            <span className="text-lg">
                              {txn.type === 'TOP_UP' || txn.type === 'TOP_UP'
                                ? '↓'
                                : txn.type === 'TRANSFER' || txn.type === 'P2P'
                                ? '↔️'
                                : '💳'}
                            </span>
                          </div>
                          <div>
                            <div className="font-medium text-sm">
                              {txn.type === 'TOP_UP' ? 'Top-up' :
                               txn.type === 'TRANSFER' || txn.type === 'P2P' ? 'Transfer' :
                               txn.type === 'PAYMENT' ? 'Payment' :
                               txn.type}
                            </div>
                            <div className="text-xs text-gray-500">
                              {formatDateTime(txn.createdAt)}
                            </div>
                          </div>
                        </div>
                        <div className="text-right">
                          <div
                            className={`font-semibold ${
                              isCredit ? 'text-green-600' : 'text-gray-900'
                            }`}
                          >
                            {isCredit ? '+' : '-'}
                            {formatCurrency(Math.abs(amountDisplay), txn.currency)}
                          </div>
                          <Badge variant="completed" className="mt-1">
                            {txn.status}
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
