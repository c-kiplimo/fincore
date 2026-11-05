'use client';

import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Button,
  Input,
} from '@fincore/ui';
import { useCreateApiKey } from '@fincore/api-client/hooks';
import { settingsApi } from '@/lib/api';
import { toast } from 'sonner';

interface CreateApiKeyDialogProps {
  open: boolean;
  onClose: () => void;
}

export function CreateApiKeyDialog({ open, onClose }: CreateApiKeyDialogProps) {
  const [name, setName] = useState('');
  const [createdKey, setCreatedKey] = useState<string | null>(null);

  const createKeyMutation = useCreateApiKey(settingsApi, {
    onSuccess: (data) => {
      setCreatedKey(data.key);
      toast.success('API key created successfully');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to create API key');
    },
  });

  const handleCreate = () => {
    if (!name.trim()) {
      toast.error('Please enter a name for your API key');
      return;
    }

    createKeyMutation.mutate({ name });
  };

  const handleClose = () => {
    setName('');
    setCreatedKey(null);
    onClose();
  };

  const handleCopyKey = () => {
    if (createdKey) {
      navigator.clipboard.writeText(createdKey);
      toast.success('API key copied to clipboard');
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Create API Key</DialogTitle>
          <DialogDescription>
            {createdKey
              ? 'Your API key has been created. Make sure to copy it now - you won\'t be able to see it again!'
              : 'Create a new API key for integrating with the platform.'}
          </DialogDescription>
        </DialogHeader>

        {!createdKey ? (
          <div className="space-y-4 py-4">
            <div>
              <Input
                label="Key Name"
                placeholder="e.g., Production Key, Test Key"
                value={name}
                onChange={(e) => setName(e.target.value)}
                autoFocus
              />
              <p className="text-xs text-gray-500 mt-1">
                Choose a descriptive name to help you identify this key
              </p>
            </div>

            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
              <p className="text-xs text-blue-800">
                <strong>Important:</strong> Keep your API keys secure and never share them publicly.
                Anyone with your key can make API requests on your behalf.
              </p>
            </div>
          </div>
        ) : (
          <div className="space-y-4 py-4">
            <div className="bg-amber-50 border border-amber-300 rounded-lg p-4">
              <p className="text-sm font-medium text-amber-900 mb-2">
                Copy your API key now
              </p>
              <p className="text-xs text-amber-800 mb-3">
                This is the only time you'll be able to see the full key. Store it securely.
              </p>
              <div className="flex gap-2">
                <code className="flex-1 text-sm bg-white px-3 py-2 rounded border border-amber-300 font-mono overflow-x-auto">
                  {createdKey}
                </code>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleCopyKey}
                >
                  Copy
                </Button>
              </div>
            </div>
          </div>
        )}

        <DialogFooter>
          {!createdKey ? (
            <>
              <Button
                variant="outline"
                onClick={handleClose}
                disabled={createKeyMutation.isPending}
              >
                Cancel
              </Button>
              <Button
                onClick={handleCreate}
                disabled={createKeyMutation.isPending || !name.trim()}
              >
                {createKeyMutation.isPending ? 'Creating...' : 'Create Key'}
              </Button>
            </>
          ) : (
            <Button onClick={handleClose}>
              Done
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
