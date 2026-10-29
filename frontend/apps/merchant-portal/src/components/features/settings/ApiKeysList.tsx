'use client';

import { Card, CardContent, Badge, Button } from '@fincore/ui';
import { useState } from 'react';

export function ApiKeysList() {
  // TODO: Fetch from API
  const apiKeys = [
    {
      id: 'key_abc123',
      name: 'Production Key',
      prefix: 'pk_live_',
      lastUsed: '2026-10-29T10:30:00Z',
      createdAt: '2026-10-01T10:00:00Z',
      status: 'active',
    },
    {
      id: 'key_def456',
      name: 'Test Key',
      prefix: 'pk_test_',
      lastUsed: null,
      createdAt: '2026-10-15T14:00:00Z',
      status: 'active',
    },
  ];

  const handleRevoke = (keyId: string) => {
    // TODO: Implement revoke confirmation dialog
    console.log('Revoke key:', keyId);
  };

  const handleCopy = (prefix: string) => {
    // In a real implementation, this would copy the full key
    console.log('Copy key:', prefix);
    alert('API key copied to clipboard (in real app)');
  };

  return (
    <Card>
      <CardContent className="p-0">
        <div className="divide-y divide-gray-200">
          {apiKeys.map((key) => (
            <div
              key={key.id}
              className="p-6 hover:bg-gray-50 transition-colors"
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3">
                    <h3 className="font-semibold">{key.name}</h3>
                    <Badge variant="completed">{key.status}</Badge>
                  </div>

                  <div className="mt-2 flex items-center gap-2">
                    <code className="text-sm bg-gray-100 px-2 py-1 rounded font-mono">
                      {key.prefix}••••••••••••••••
                    </code>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleCopy(key.prefix)}
                    >
                      Copy
                    </Button>
                  </div>

                  <div className="mt-3 flex items-center gap-4 text-sm text-gray-600">
                    <span>
                      Created: {new Date(key.createdAt).toLocaleDateString()}
                    </span>
                    {key.lastUsed && (
                      <span>
                        Last used: {new Date(key.lastUsed).toLocaleDateString()}
                      </span>
                    )}
                  </div>
                </div>

                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => handleRevoke(key.id)}
                >
                  Revoke
                </Button>
              </div>
            </div>
          ))}

          {apiKeys.length === 0 && (
            <div className="text-center py-12">
              <p className="text-gray-500">No API keys yet</p>
              <p className="text-sm text-gray-400 mt-1">
                Create your first API key to start accepting payments
              </p>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
