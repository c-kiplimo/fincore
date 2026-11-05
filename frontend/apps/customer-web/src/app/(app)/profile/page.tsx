'use client';

import { useSession, signOut } from 'next-auth/react';
import { Card, CardContent, CardHeader, CardTitle, Button, Badge, Skeleton } from '@fincore/ui';
import { useProfile, useLinkedAccounts, usePaymentMethods } from '@fincore/api-client/hooks';
import { formatDate } from '@fincore/utils';
import { settingsApi } from '@/lib/api';
import { toast } from 'sonner';

export default function ProfilePage() {
  const { data: session } = useSession();
  const userId = session?.user?.id;

  const { data: profile, isLoading: profileLoading } = useProfile(
    settingsApi,
    userId!,
    { enabled: !!userId }
  );

  const { data: linkedAccounts, isLoading: accountsLoading } = useLinkedAccounts(
    settingsApi,
    userId!,
    { enabled: !!userId }
  );

  const { data: paymentMethods, isLoading: cardsLoading } = usePaymentMethods(
    settingsApi,
    userId!,
    { enabled: !!userId }
  );

  const handleSignOut = async () => {
    try {
      await signOut({ callbackUrl: '/login' });
      toast.success('Signed out successfully');
    } catch (error) {
      toast.error('Failed to sign out');
    }
  };

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
          {profileLoading ? (
            <div>
              <div className="flex items-center gap-4 mb-6">
                <Skeleton className="h-16 w-16 rounded-full" />
                <div className="flex-1">
                  <Skeleton className="h-6 w-32 mb-2" />
                  <Skeleton className="h-4 w-48" />
                </div>
              </div>
              <Skeleton className="h-32 w-full" />
            </div>
          ) : profile ? (
            <>
              <div className="flex items-center gap-4 mb-6">
                <div className="h-16 w-16 rounded-full bg-platform-green flex items-center justify-center text-white text-2xl font-semibold">
                  {profile.name.charAt(0).toUpperCase()}
                </div>
                <div>
                  <h2 className="text-lg font-semibold">{profile.name}</h2>
                  <p className="text-sm text-gray-600">{profile.email}</p>
                </div>
              </div>

              <div className="space-y-3">
                <div className="flex items-center justify-between py-2 border-b border-gray-200">
                  <span className="text-sm text-gray-600">Phone Number</span>
                  <span className="text-sm font-medium">{profile.phone || 'Not set'}</span>
                </div>
                <div className="flex items-center justify-between py-2 border-b border-gray-200">
                  <span className="text-sm text-gray-600">KYC Status</span>
                  <Badge
                    variant={
                      profile.kycStatus === 'VERIFIED'
                        ? 'completed'
                        : profile.kycStatus === 'PENDING'
                        ? 'warning'
                        : 'default'
                    }
                  >
                    {profile.kycStatus || 'NOT_VERIFIED'}
                  </Badge>
                </div>
                <div className="flex items-center justify-between py-2">
                  <span className="text-sm text-gray-600">Member Since</span>
                  <span className="text-sm font-medium">
                    {profile.createdAt
                      ? new Date(profile.createdAt).toLocaleDateString('en', {
                          month: 'long',
                          year: 'numeric',
                        })
                      : 'N/A'}
                  </span>
                </div>
              </div>

              <Button variant="outline" className="w-full mt-4">
                Edit Profile
              </Button>
            </>
          ) : (
            <div className="text-center py-8">
              <p className="text-gray-500 text-sm">Failed to load profile</p>
            </div>
          )}
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
          {accountsLoading ? (
            <div>
              <Skeleton className="h-16 w-full mb-2" />
              <Skeleton className="h-16 w-full" />
            </div>
          ) : linkedAccounts && linkedAccounts.length > 0 ? (
            linkedAccounts.map((account) => (
              <div
                key={account.id}
                className="flex items-center justify-between p-3 rounded-lg border border-gray-200"
              >
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-full bg-gray-100 flex items-center justify-center">
                    <span className="text-lg">🏦</span>
                  </div>
                  <div>
                    <div className="text-sm font-medium">{account.bankName || 'Bank Account'}</div>
                    <div className="text-xs text-gray-500">
                      Account {account.accountNumber || account.maskedNumber}
                    </div>
                  </div>
                </div>
                {account.verified && (
                  <Badge variant="completed" className="text-xs">
                    Verified
                  </Badge>
                )}
              </div>
            ))
          ) : (
            <div className="text-center py-6 text-sm text-gray-500">
              No linked bank accounts
            </div>
          )}
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
          {cardsLoading ? (
            <div>
              <Skeleton className="h-16 w-full mb-2" />
              <Skeleton className="h-16 w-full" />
            </div>
          ) : paymentMethods && paymentMethods.length > 0 ? (
            paymentMethods.map((card) => (
              <div
                key={card.id}
                className="flex items-center justify-between p-3 rounded-lg border border-gray-200"
              >
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-lg bg-gray-900 flex items-center justify-center text-white text-xs font-semibold">
                    {card.brand === 'Visa' ? 'VISA' : card.brand === 'Mastercard' ? 'MC' : 'CARD'}
                  </div>
                  <div>
                    <div className="text-sm font-medium">
                      {card.brand} •••• {card.last4}
                    </div>
                    <div className="text-xs text-gray-500">
                      Expires {card.expiryMonth}/{card.expiryYear}
                    </div>
                  </div>
                </div>
                <Button variant="ghost" size="sm">
                  Remove
                </Button>
              </div>
            ))
          ) : (
            <div className="text-center py-6 text-sm text-gray-500">No saved cards</div>
          )}
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
      <Button variant="danger" className="w-full" onClick={handleSignOut}>
        Sign Out
      </Button>

      <div className="text-center text-xs text-gray-500 pb-4">
        Version 1.0.0
      </div>
    </div>
  );
}
