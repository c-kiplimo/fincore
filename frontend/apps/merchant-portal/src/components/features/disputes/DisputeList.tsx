'use client';

import { Card, CardContent, Badge, Button } from '@fincore/ui';
import { DisputeStatus } from '@fincore/types';
import { useState } from 'react';

export function DisputeList() {
  const [selectedDispute, setSelectedDispute] = useState<string | null>(null);

  // TODO: Fetch from API
  const disputes = [
    {
      id: 'disp_abc123',
      paymentId: 'pi_1abc123',
      amountMinor: 10000,
      feeMinor: 15000, // 150.00 dispute fee
      currency: 'KES',
      reason: 'Customer claims non-delivery',
      status: DisputeStatus.OPEN,
      createdAt: '2026-10-29T10:00:00Z',
    },
    {
      id: 'disp_def456',
      paymentId: 'pi_2def456',
      amountMinor: 5000,
      feeMinor: 15000,
      currency: 'KES',
      reason: 'Fraudulent transaction',
      status: DisputeStatus.WON,
      resolvedAt: '2026-10-28T15:00:00Z',
      createdAt: '2026-10-27T10:00:00Z',
    },
  ];

  const formatCurrency = (amountMinor: number, currency: string) => {
    return `${(amountMinor / 100).toFixed(2)} ${currency}`;
  };

  const getStatusVariant = (status: DisputeStatus) => {
    const variants: Record<DisputeStatus, any> = {
      [DisputeStatus.OPEN]: 'warning',
      [DisputeStatus.WON]: 'completed',
      [DisputeStatus.LOST]: 'failed',
    };
    return variants[status];
  };

  const handleUploadEvidence = (disputeId: string) => {
    // TODO: Implement evidence upload dialog
    console.log('Upload evidence for:', disputeId);
  };

  return (
    <Card>
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="border-b border-gray-200 bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Dispute ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Payment ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Amount
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Fee
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Reason
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Date
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {disputes.map((dispute) => (
                <tr
                  key={dispute.id}
                  className="hover:bg-gray-50 cursor-pointer"
                  onClick={() => setSelectedDispute(dispute.id)}
                >
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-mono font-medium">
                      {dispute.id}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-mono text-gray-600">
                      {dispute.paymentId}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium">
                      {formatCurrency(dispute.amountMinor, dispute.currency)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-red-600">
                      {formatCurrency(dispute.feeMinor, dispute.currency)}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="text-sm text-gray-900 max-w-xs truncate">
                      {dispute.reason}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <Badge variant={getStatusVariant(dispute.status)}>
                      {dispute.status}
                    </Badge>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-500">
                      {new Date(dispute.createdAt).toLocaleString('en-US', {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    {dispute.status === DisputeStatus.OPEN && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleUploadEvidence(dispute.id);
                        }}
                      >
                        Upload Evidence
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {disputes.length === 0 && (
          <div className="text-center py-12">
            <p className="text-gray-500">No disputes found</p>
            <p className="text-sm text-gray-400 mt-1">
              Disputes will appear here when customers raise chargebacks
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
