'use client';

import { useState } from 'react';
import { Button } from '@fincore/ui';
import { RequestPayoutDialog } from './RequestPayoutDialog';

export function RequestPayoutButton() {
  const [dialogOpen, setDialogOpen] = useState(false);

  return (
    <>
      <Button onClick={() => setDialogOpen(true)}>
        Request Payout
      </Button>

      <RequestPayoutDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
      />
    </>
  );
}
