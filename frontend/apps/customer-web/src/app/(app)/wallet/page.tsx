import { Card, CardContent, Button, Badge } from '@payments-platform/ui';

export default function WalletPage() {
  // TODO: Fetch from API
  const wallet = {
    balanceMinor: 150000, // 1,500.00
    currency: 'KES',
    status: 'ACTIVE',
  };

  const recentTransactions = [
    {
      id: 'txn_1',
      type: 'PAYMENT',
      description: 'Payment to Example Store',
      amountMinor: -5000,
      currency: 'KES',
      createdAt: '2026-10-29T10:30:00Z',
      status: 'COMPLETED',
    },
    {
      id: 'txn_2',
      type: 'TOP_UP',
      description: 'Top-up via M-Pesa',
      amountMinor: 10000,
      currency: 'KES',
      createdAt: '2026-10-29T09:15:00Z',
      status: 'COMPLETED',
    },
    {
      id: 'txn_3',
      type: 'TRANSFER',
      description: 'Transfer to John Doe',
      amountMinor: -2000,
      currency: 'KES',
      createdAt: '2026-10-28T16:45:00Z',
      status: 'COMPLETED',
    },
  ];

  const formatCurrency = (amountMinor: number, currency: string) => {
    return `${(Math.abs(amountMinor) / 100).toFixed(2)} ${currency}`;
  };

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
            Wallet ID: <span className="font-mono">wal_abc123</span>
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
            <div className="divide-y divide-gray-200">
              {recentTransactions.map((txn) => (
                <div key={txn.id} className="p-4 hover:bg-gray-50 cursor-pointer">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div
                        className={`h-10 w-10 rounded-full flex items-center justify-center ${
                          txn.amountMinor > 0
                            ? 'bg-green-100 text-green-600'
                            : 'bg-gray-100 text-gray-600'
                        }`}
                      >
                        <span className="text-lg">
                          {txn.type === 'TOP_UP'
                            ? '↓'
                            : txn.type === 'TRANSFER'
                            ? '↔️'
                            : '💳'}
                        </span>
                      </div>
                      <div>
                        <div className="font-medium text-sm">{txn.description}</div>
                        <div className="text-xs text-gray-500">
                          {new Date(txn.createdAt).toLocaleDateString('en', {
                            month: 'short',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                        </div>
                      </div>
                    </div>
                    <div className="text-right">
                      <div
                        className={`font-semibold ${
                          txn.amountMinor > 0 ? 'text-green-600' : 'text-gray-900'
                        }`}
                      >
                        {txn.amountMinor > 0 ? '+' : '-'}
                        {formatCurrency(txn.amountMinor, txn.currency)}
                      </div>
                      <Badge variant="completed" className="mt-1">
                        {txn.status}
                      </Badge>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
