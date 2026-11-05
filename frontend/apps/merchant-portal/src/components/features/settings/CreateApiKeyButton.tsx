'use client';

import { useState } from 'react';
import { Button } from '@fincore/ui';
import { CreateApiKeyDialog } from './CreateApiKeyDialog';

export function CreateApiKeyButton() {
  const [dialogOpen, setDialogOpen] = useState(false);

  return (
    <>
      <Button onClick={() => setDialogOpen(true)}>
        Create API Key
      </Button>

      <CreateApiKeyDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
      />
    </>
  );
}
