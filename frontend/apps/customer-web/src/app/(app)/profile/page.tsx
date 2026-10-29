import { Card, CardContent, CardHeader, CardTitle, Button, Badge } from '@payments-platform/ui';

export default function ProfilePage() {
  // TODO: Fetch from API
  const user = {
    name: 'Alex Johnson',
    email: 'alex.johnson@example.com',
    phone: '+254712345678',
    kycStatus: 'VERIFIED',
    accountCreated: '2026-01-15T10:00:00Z',
  };

  const linkedAccounts = [
    { id: 'bank_1', type: 'KCB Bank', number: '****1234', verified: true },
    { id: 'bank_2', type: 'Equity Bank', number: '****5678', verified: true },
  ];

  const savedCards = [
    { id: 'card_1', brand: 'Visa', last4: '4242', expiry: '12/27' },
    { id: 'card_2', brand: 'Mastercard', last4: '5555', expiry: '08/26' },
  ];

  return (
    <div className="p-4 space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-semibold">Profile</h1>
        <p className="text-sm text-gray-600 mt-1">Manage your account and preferences</p>
      </div>

      {/* Profile Information */}
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center gap-4 mb-6">
            <div className="h-16 w-16 rounded-full bg-platform-green flex items-center justify-center text-white text-2xl font-semibold">
              {user.name.charAt(0)}
            </div>
            <div>
              <h2 className="text-lg font-semibold">{user.name}</h2>
              <p className="text-sm text-gray-600">{user.email}</p>
            </div>
          </div>

          <div className="space-y-3">
            <div className="flex items-center justify-between py-2 border-b border-gray-200">
              <span className="text-sm text-gray-600">Phone Number</span>
              <span className="text-sm font-medium">{user.phone}</span>
            </div>
            <div className="flex items-center justify-between py-2 border-b border-gray-200">
              <span className="text-sm text-gray-600">KYC Status</span>
              <Badge variant="completed">{user.kycStatus}</Badge>
            </div>
            <div className="flex items-center justify-between py-2">
              <span className="text-sm text-gray-600">Member Since</span>
              <span className="text-sm font-medium">
                {new Date(user.accountCreated).toLocaleDateString('en', {
                  month: 'long',
                  year: 'numeric',
                })}
              </span>
            </div>
          </div>

          <Button variant="outline" className="w-full mt-4">
            Edit Profile
          </Button>
        </CardContent>
      </Card>

      {/* Security Settings */}
      <Card>
        <CardHeader>
          <CardTitle>Security</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <button className="w-full flex items-center justify-between p-3 rounded-lg hover:bg-gray-50">
            <div className="flex items-center gap-3">
              <span className="text-xl">🔒</span>
              <div className="text-left">
                <div className="text-sm font-medium">Change Password</div>
                <div className="text-xs text-gray-500">Last changed 30 days ago</div>
              </div>
            </div>
            <span className="text-gray-400">›</span>
          </button>

          <button className="w-full flex items-center justify-between p-3 rounded-lg hover:bg-gray-50">
            <div className="flex items-center gap-3">
              <span className="text-xl">🔐</span>
              <div className="text-left">
                <div className="text-sm font-medium">Two-Factor Authentication</div>
                <div className="text-xs text-gray-500">Not enabled</div>
              </div>
            </div>
            <span className="text-gray-400">›</span>
          </button>

          <button className="w-full flex items-center justify-between p-3 rounded-lg hover:bg-gray-50">
            <div className="flex items-center gap-3">
              <span className="text-xl">📱</span>
              <div className="text-left">
                <div className="text-sm font-medium">Biometric Login</div>
                <div className="text-xs text-gray-500">Face ID enabled</div>
              </div>
            </div>
            <span className="text-gray-400">›</span>
          </button>
        </CardContent>
      </Card>

      {/* Linked Bank Accounts */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Linked Bank Accounts</CardTitle>
            <Button variant="ghost" size="sm">
              Add
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-2">
          {linkedAccounts.map((account) => (
            <div
              key={account.id}
              className="flex items-center justify-between p-3 rounded-lg border border-gray-200"
            >
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-full bg-gray-100 flex items-center justify-center">
                  <span className="text-lg">🏦</span>
                </div>
                <div>
                  <div className="text-sm font-medium">{account.type}</div>
                  <div className="text-xs text-gray-500">Account {account.number}</div>
                </div>
              </div>
              {account.verified && (
                <Badge variant="completed" className="text-xs">
                  Verified
                </Badge>
              )}
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Saved Payment Methods */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Saved Cards</CardTitle>
            <Button variant="ghost" size="sm">
              Add
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-2">
          {savedCards.map((card) => (
            <div
              key={card.id}
              className="flex items-center justify-between p-3 rounded-lg border border-gray-200"
            >
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-lg bg-gray-900 flex items-center justify-center text-white text-xs font-semibold">
                  {card.brand === 'Visa' ? 'VISA' : 'MC'}
                </div>
                <div>
                  <div className="text-sm font-medium">
                    {card.brand} •••• {card.last4}
                  </div>
                  <div className="text-xs text-gray-500">Expires {card.expiry}</div>
                </div>
              </div>
              <Button variant="ghost" size="sm">
                Remove
              </Button>
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Preferences */}
      <Card>
        <CardHeader>
          <CardTitle>Preferences</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <button className="w-full flex items-center justify-between p-3 rounded-lg hover:bg-gray-50">
            <div className="flex items-center gap-3">
              <span className="text-xl">🔔</span>
              <div className="text-left">
                <div className="text-sm font-medium">Notifications</div>
                <div className="text-xs text-gray-500">Email, Push, SMS</div>
              </div>
            </div>
            <span className="text-gray-400">›</span>
          </button>

          <button className="w-full flex items-center justify-between p-3 rounded-lg hover:bg-gray-50">
            <div className="flex items-center gap-3">
              <span className="text-xl">🌐</span>
              <div className="text-left">
                <div className="text-sm font-medium">Language & Region</div>
                <div className="text-xs text-gray-500">English (Kenya)</div>
              </div>
            </div>
            <span className="text-gray-400">›</span>
          </button>

          <button className="w-full flex items-center justify-between p-3 rounded-lg hover:bg-gray-50">
            <div className="flex items-center gap-3">
              <span className="text-xl">💰</span>
              <div className="text-left">
                <div className="text-sm font-medium">Default Currency</div>
                <div className="text-xs text-gray-500">KES</div>
              </div>
            </div>
            <span className="text-gray-400">›</span>
          </button>
        </CardContent>
      </Card>

      {/* Help & Support */}
      <Card>
        <CardHeader>
          <CardTitle>Help & Support</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <button className="w-full flex items-center justify-between p-3 rounded-lg hover:bg-gray-50">
            <div className="flex items-center gap-3">
              <span className="text-xl">❓</span>
              <span className="text-sm font-medium">Help Center</span>
            </div>
            <span className="text-gray-400">›</span>
          </button>

          <button className="w-full flex items-center justify-between p-3 rounded-lg hover:bg-gray-50">
            <div className="flex items-center gap-3">
              <span className="text-xl">📧</span>
              <span className="text-sm font-medium">Contact Support</span>
            </div>
            <span className="text-gray-400">›</span>
          </button>

          <button className="w-full flex items-center justify-between p-3 rounded-lg hover:bg-gray-50">
            <div className="flex items-center gap-3">
              <span className="text-xl">📄</span>
              <span className="text-sm font-medium">Terms & Privacy</span>
            </div>
            <span className="text-gray-400">›</span>
          </button>
        </CardContent>
      </Card>

      {/* Sign Out */}
      <Button variant="danger" className="w-full">
        Sign Out
      </Button>

      <div className="text-center text-xs text-gray-500 pb-4">
        Version 1.0.0
      </div>
    </div>
  );
}
