import { Card, CardContent, CardHeader, CardTitle, Button, Input } from '@payments-platform/ui';

export default function PaymentsPage() {
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
          <CardTitle>Pay with Card</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Merchant ID
            </label>
            <Input placeholder="Enter merchant ID..." />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Amount (KES)
            </label>
            <Input type="number" placeholder="0.00" />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Description (Optional)
            </label>
            <Input placeholder="What's this for?" />
          </div>

          <Button variant="primary" className="w-full">
            Continue to Payment
          </Button>
        </CardContent>
      </Card>

      {/* Recent Merchants */}
      <div>
        <h2 className="text-lg font-semibold mb-4">Recent Merchants</h2>
        <div className="space-y-2">
          {[
            { name: 'Example Store', lastPaid: '2 days ago', id: 'mer_abc123' },
            { name: 'Coffee Shop', lastPaid: '1 week ago', id: 'mer_def456' },
          ].map((merchant) => (
            <Card key={merchant.id} className="hover:border-platform-green cursor-pointer">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-full bg-gray-200"></div>
                    <div>
                      <div className="font-medium text-sm">{merchant.name}</div>
                      <div className="text-xs text-gray-500">Last paid {merchant.lastPaid}</div>
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
      </div>
    </div>
  );
}
