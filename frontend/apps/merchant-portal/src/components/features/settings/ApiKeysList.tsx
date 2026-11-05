'use client';

import { Card, CardContent, Badge, Button, Skeleton } from '@fincore/ui';
import { useApiKeys, useRevokeApiKey } from '@fincore/api-client/hooks';
import { formatDate } from '@fincore/utils';
import { settingsApi } from '@/lib/api';
import { toast } from 'sonner';

export function ApiKeysList() {
  const { data: apiKeys, isLoading, error } = useApiKeys(settingsApi);

  const revokeKeyMutation = useRevokeApiKey(settingsApi, {
    onSuccess: () => {
      toast.success('API key revoked successfully');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to revoke API key');
    },
  });

  const handleRevoke = (keyId: string, keyName: string) => {
    if (confirm(`Are you sure you want to revoke "${keyName}"? This action cannot be undone.`)) {
      revokeKeyMutation.mutate(keyId);
    }
  };

  const handleCopy = (prefix: string) => {
    // Note: Full key is only shown once during creation
    toast.info('Full API key is only available during creation. Showing prefix only.');
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="space-y-4">
            {[1, 2].map((i) => (
              <Skeleton key={i} className="h-24 w-full" />
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent className="p-6">
          <p className="text-sm text-red-600 text-center">
            Failed to load API keys. Please try again.
          </p>
        </CardContent>
      </Card>
    );
  }

  if (!apiKeys || apiKeys.length === 0) {
    return (
      <Card>
        <CardContent className="p-12">
          <div className="text-center">
            <p className="text-gray-500">No API keys yet</p>
            <p className="text-sm text-gray-400 mt-1">
              Create your first API key to start integrating with the platform
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

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
                    <Badge variant={key.revokedAt ? 'failed' : 'completed'}>
                      {key.revokedAt ? 'revoked' : 'active'}
                    </Badge>
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
                      Copy Prefix
                    </Button>
                  </div>

                  <div className="mt-3 flex items-center gap-4 text-sm text-gray-600">
                    <span>
                      Created: {formatDate(key.createdAt)}
                    </span>
                    {key.lastUsedAt && (
                      <span>
                        Last used: {formatDate(key.lastUsedAt)}
                      </span>
                    )}
                    {key.revokedAt && (
                      <span className="text-red-600">
                        Revoked: {formatDate(key.revokedAt)}
                      </span>
                    )}
                  </div>
                </div>

                {!key.revokedAt && (
                  <Button
                    variant="danger"
                    size="sm"
                    onClick={() => handleRevoke(key.id, key.name)}
                    disabled={revokeKeyMutation.isPending}
                  >
                    Revoke
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
