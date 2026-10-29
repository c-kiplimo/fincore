import { Header } from '@/components/layout/Header';
import { PayoutList } from '@/components/features/payouts/PayoutList';
import { RequestPayoutButton } from '@/components/features/payouts/RequestPayoutButton';

export default function PayoutsPage() {
  return (
    <div className="flex flex-col h-full">
      <Header title="Payouts" />

      <div className="flex-1 p-6 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold">Your Payouts</h2>
            <p className="text-sm text-gray-600">
              Manage your payout history and request new payouts
            </p>
          </div>
          <RequestPayoutButton />
        </div>

        <PayoutList />
      </div>
    </div>
  );
}
