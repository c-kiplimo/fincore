import { ProfileForm } from '@/components/features/settings/ProfileForm';

export default function ProfilePage() {
  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold">Business Profile</h2>
        <p className="text-sm text-gray-600">
          Manage your business information and contact details
        </p>
      </div>

      <ProfileForm />
    </div>
  );
}
