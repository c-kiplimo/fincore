'use client';

import { Button } from '@payments-platform/ui';

export function CreateApiKeyButton() {
  const handleCreateKey = () => {
    // TODO: Open dialog to create new API key
    console.log('Create new API key');
  };

  return (
    <Button onClick={handleCreateKey}>
      Create API Key
    </Button>
  );
}
