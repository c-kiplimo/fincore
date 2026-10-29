import { WebhookConfig } from '@/components/features/settings/WebhookConfig';

export default function WebhooksPage() {
  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold">Webhook Configuration</h2>
        <p className="text-sm text-gray-600">
          Configure webhook endpoints to receive real-time notifications
        </p>
      </div>

      <WebhookConfig />
    </div>
  );
}
