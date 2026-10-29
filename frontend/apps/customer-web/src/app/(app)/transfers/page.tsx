import { Card, CardContent, CardHeader, CardTitle, Button, Input, Badge } from '@payments-platform/ui';

export default function TransfersPage() {
  // TODO: Fetch from API
  const transferHistory = [
    {
      id: 'tfr_1',
      type: 'P2P_SEND',
      recipient: 'John Doe',
      amountMinor: 5000,
      currency: 'KES',
      status: 'COMPLETED',
      createdAt: '2026-10-28T14:30:00Z',
    },
    {
      id: 'tfr_2',
      type: 'WITHDRAW',
      recipient: 'Bank Account ****1234',
      amountMinor: 20000,
      currency: 'KES',
      status: 'COMPLETED',
      createdAt: '2026-10-25T10:15:00Z',
    },
    {
      id: 'tfr_3',
      type: 'P2P_RECEIVE',
      recipient: 'Jane Smith',
      amountMinor: 3000,
      currency: 'KES',
      status: 'COMPLETED',
      createdAt: '2026-10-22T16:45:00Z',
    },
  ];

  const formatCurrency = (amountMinor: number, currency: string) => {
    return `${(amountMinor / 100).toFixed(2)} ${currency}`;
  };

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
            <Input placeholder="+254712345678 or wal_xyz..." />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Amount (KES)
            </label>
            <Input type="number" placeholder="0.00" />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Note (Optional)
            </label>
            <Input placeholder="What's this for?" />
          </div>

          <div className="bg-gray-50 rounded-lg p-3 space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-600">Transfer Fee</span>
              <span className="font-medium">0.00 KES</span>
            </div>
            <div className="flex justify-between border-t pt-2">
              <span className="font-medium">Total</span>
              <span className="font-semibold">0.00 KES</span>
            </div>
          </div>

          <Button variant="primary" className="w-full">
            Send Money
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
            <select className="w-full h-10 rounded-lg border border-gray-300 px-3 text-sm">
              <option value="">Select saved bank account...</option>
              <option value="bank_1">KCB Bank - ****1234</option>
              <option value="bank_2">Equity Bank - ****5678</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Amount (KES)
            </label>
            <Input type="number" placeholder="0.00" />
          </div>

          <div className="bg-gray-50 rounded-lg p-3 space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-600">Withdrawal Fee</span>
              <span className="font-medium">25.00 KES</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Processing Time</span>
              <span className="font-medium">1-2 business days</span>
            </div>
            <div className="flex justify-between border-t pt-2">
              <span className="font-medium">Total Deducted</span>
              <span className="font-semibold">0.00 KES</span>
            </div>
          </div>

          <Button variant="primary" className="w-full">
            Withdraw to Bank
          </Button>
        </CardContent>
      </Card>

      {/* Transfer History */}
      <div>
        <h2 className="text-lg font-semibold mb-4">Recent Transfers</h2>
        <Card>
          <CardContent className="p-0">
            <div className="divide-y divide-gray-200">
              {transferHistory.map((transfer) => (
                <div key={transfer.id} className="p-4 hover:bg-gray-50">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div
                        className={`h-10 w-10 rounded-full flex items-center justify-center ${
                          transfer.type === 'P2P_RECEIVE'
                            ? 'bg-green-100 text-green-600'
                            : 'bg-gray-100 text-gray-600'
                        }`}
                      >
                        <span className="text-lg">
                          {transfer.type === 'P2P_RECEIVE'
                            ? '↓'
                            : transfer.type === 'WITHDRAW'
                            ? '🏦'
                            : '↑'}
                        </span>
                      </div>
                      <div>
                        <div className="font-medium text-sm">
                          {transfer.type === 'P2P_RECEIVE' ? 'From' : 'To'} {transfer.recipient}
                        </div>
                        <div className="text-xs text-gray-500">
                          {new Date(transfer.createdAt).toLocaleDateString('en', {
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
                          transfer.type === 'P2P_RECEIVE' ? 'text-green-600' : 'text-gray-900'
                        }`}
                      >
                        {transfer.type === 'P2P_RECEIVE' ? '+' : '-'}
                        {formatCurrency(transfer.amountMinor, transfer.currency)}
                      </div>
                      <Badge variant="completed" className="mt-1">
                        {transfer.status}
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
