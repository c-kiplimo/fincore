'use client';

import { Card, CardContent, CardHeader, CardTitle, Input, Button } from '@fincore/ui';
import { useState } from 'react';

export function WebhookConfig() {
  const [webhookUrl, setWebhookUrl] = useState('https://example.com/webhooks');
  const [secret, setSecret] = useState('whsec_••••••••••••••••');

  // TODO: Fetch actual config from API
  const events = [
    { id: 'payment.captured', name: 'Payment Captured', enabled: true },
    { id: 'payment.refunded', name: 'Payment Refunded', enabled: true },
    { id: 'payout.completed', name: 'Payout Completed', enabled: true },
    { id: 'payout.failed', name: 'Payout Failed', enabled: true },
    { id: 'dispute.opened', name: 'Dispute Opened', enabled: false },
  ];

  const handleSave = () => {
    // TODO: Save webhook configuration
    console.log('Save webhook config:', { webhookUrl, secret });
  };

  const handleTestWebhook = () => {
    // TODO: Send test webhook
    console.log('Test webhook');
  };

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

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              Webhook Secret
            </label>
            <div className="flex gap-2">
              <Input
                type="password"
                value={secret}
                onChange={(e) => setSecret(e.target.value)}
                className="flex-1"
              />
              <Button variant="outline">
                Regenerate
              </Button>
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Use this secret to verify webhook signatures
            </p>
          </div>

          <div className="flex gap-2 pt-2">
            <Button onClick={handleSave}>
              Save Configuration
            </Button>
            <Button variant="outline" onClick={handleTestWebhook}>
              Send Test Event
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
            {events.map((event) => (
              <label
                key={event.id}
                className="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 cursor-pointer"
              >
                <input
                  type="checkbox"
                  checked={event.enabled}
                  className="h-4 w-4 rounded border-gray-300 text-platform-green focus:ring-platform-green"
                  onChange={() => {
                    // TODO: Toggle event subscription
                    console.log('Toggle event:', event.id);
                  }}
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
