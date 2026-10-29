import { Header } from '@/components/layout/Header';
import { DisputeList } from '@/components/features/disputes/DisputeList';

export default function DisputesPage() {
  return (
    <div className="flex flex-col h-full">
      <Header title="Disputes" />

      <div className="flex-1 p-6 space-y-6">
        <div>
          <h2 className="text-lg font-semibold">Your Disputes</h2>
          <p className="text-sm text-gray-600">
            Manage chargebacks and disputes, upload evidence, and track resolutions
          </p>
        </div>

        <DisputeList />
      </div>
    </div>
  );
}
