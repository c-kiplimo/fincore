import { ApiKeysList } from '@/components/features/settings/ApiKeysList';
import { CreateApiKeyButton } from '@/components/features/settings/CreateApiKeyButton';

export default function ApiKeysPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold">API Keys</h2>
          <p className="text-sm text-gray-600">
            Manage your API keys for integrating with the platform
          </p>
        </div>
        <CreateApiKeyButton />
      </div>

      <ApiKeysList />
    </div>
  );
}
