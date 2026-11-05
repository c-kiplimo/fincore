'use client';

import { Card, CardContent, CardHeader, CardTitle, Input, Button, Skeleton } from '@fincore/ui';
import { useState, useEffect } from 'react';
import { useWebhookConfig, useUpdateWebhookConfig, useTestWebhook } from '@fincore/api-client/hooks';
import { settingsApi } from '@/lib/api';
import { toast } from 'sonner';

export function WebhookConfig() {
  const { data: config, isLoading, error } = useWebhookConfig(settingsApi);

  const [webhookUrl, setWebhookUrl] = useState('');
  const [enabledEvents, setEnabledEvents] = useState<string[]>([]);

  const updateConfigMutation = useUpdateWebhookConfig(settingsApi, {
    onSuccess: () => {
      toast.success('Webhook configuration updated successfully');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to update webhook configuration');
    },
  });

  const testWebhookMutation = useTestWebhook(settingsApi, {
    onSuccess: () => {
      toast.success('Test webhook sent successfully');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to send test webhook');
    },
  });

  // Update form when config data loads
  useEffect(() => {
    if (config) {
      setWebhookUrl(config.url || '');
      setEnabledEvents(config.enabledEvents || []);
    }
  }, [config]);

  const availableEvents = [
    { id: 'payment.captured', name: 'Payment Captured' },
    { id: 'payment.refunded', name: 'Payment Refunded' },
    { id: 'payment.failed', name: 'Payment Failed' },
    { id: 'payout.completed', name: 'Payout Completed' },
    { id: 'payout.failed', name: 'Payout Failed' },
    { id: 'dispute.opened', name: 'Dispute Opened' },
    { id: 'dispute.won', name: 'Dispute Won' },
    { id: 'dispute.lost', name: 'Dispute Lost' },
  ];

  const handleSave = () => {
    if (!webhookUrl.trim()) {
      toast.error('Please enter a webhook URL');
      return;
    }

    if (!webhookUrl.startsWith('https://')) {
      toast.error('Webhook URL must use HTTPS');
      return;
    }

    updateConfigMutation.mutate({
      url: webhookUrl,
      enabledEvents,
    });
  };

  const handleTestWebhook = () => {
    if (!config?.url) {
      toast.error('Please save your webhook configuration first');
      return;
    }
    testWebhookMutation.mutate();
  };

  const toggleEvent = (eventId: string) => {
    setEnabledEvents((prev) =>
      prev.includes(eventId)
        ? prev.filter((id) => id !== eventId)
        : [...prev, eventId]
    );
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Card>
          <CardContent className="pt-6">
            <div className="space-y-4">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent className="pt-6">
          <p className="text-sm text-red-600">
            Failed to load webhook configuration. Please try again.
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Endpoint Configuration</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <Input
            label="Webhook URL"
            type="url"
            value={webhookUrl}
            onChange={(e) => setWebhookUrl(e.target.value)}
            placeholder="https://your-domain.com/webhooks"
          />

          {config?.secret && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">
                Webhook Secret
              </label>
              <div className="flex gap-2">
                <code className="flex-1 text-sm bg-gray-100 px-3 py-2 rounded border border-gray-300 font-mono">
                  {config.secret}
                </code>
                <Button
                  variant="outline"
                  onClick={() => {
                    navigator.clipboard.writeText(config.secret);
                    toast.success('Secret copied to clipboard');
                  }}
                >
                  Copy
                </Button>
              </div>
              <p className="text-xs text-gray-500 mt-1">
                Use this secret to verify webhook signatures (HMAC SHA-256)
              </p>
            </div>
          )}

          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
            <p className="text-xs text-blue-800">
              <strong>Note:</strong> Your webhook endpoint must respond with a 2xx status code within 5 seconds.
              Failed deliveries will be retried with exponential backoff.
            </p>
          </div>

          <div className="flex gap-2 pt-2">
            <Button
              onClick={handleSave}
              disabled={updateConfigMutation.isPending}
            >
              {updateConfigMutation.isPending ? 'Saving...' : 'Save Configuration'}
            </Button>
            <Button
              variant="outline"
              onClick={handleTestWebhook}
              disabled={testWebhookMutation.isPending || !config?.url}
            >
              {testWebhookMutation.isPending ? 'Sending...' : 'Send Test Event'}
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Event Subscriptions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {availableEvents.map((event) => (
              <label
                key={event.id}
                className="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 cursor-pointer"
              >
                <input
                  type="checkbox"
                  checked={enabledEvents.includes(event.id)}
                  className="h-4 w-4 rounded border-gray-300 text-platform-green focus:ring-platform-green"
                  onChange={() => toggleEvent(event.id)}
                />
                <div className="flex-1">
                  <div className="font-medium">{event.name}</div>
                  <div className="text-sm text-gray-500">{event.id}</div>
                </div>
              </label>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
