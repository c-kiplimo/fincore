'use client';

import { Button } from '@fincore/ui';

export function RequestPayoutButton() {
  const handleRequestPayout = () => {
    // TODO: Open dialog to request payout
    console.log('Request payout clicked');
  };

  return (
    <Button onClick={handleRequestPayout}>
      Request Payout
    </Button>
  );
}
