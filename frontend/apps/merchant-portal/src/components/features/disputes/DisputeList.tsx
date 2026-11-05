'use client';

import { useState } from 'react';
import { Card, CardContent, Badge, Button, Skeleton } from '@fincore/ui';
import { useDisputes } from '@fincore/api-client/hooks';
import { formatCurrency, formatDateTime, getDisputeStatusVariant } from '@fincore/utils';
import { disputesApi } from '@/lib/api';
import { EvidenceUploadDialog } from './EvidenceUploadDialog';
import type { Dispute, DisputeStatus } from '@fincore/types';

export function DisputeList() {
  const [uploadingEvidence, setUploadingEvidence] = useState<Dispute | null>(null);

  const { data: disputesData, isLoading, error } = useDisputes(disputesApi, {
    pageSize: 50,
  });

  const disputes = disputesData?.items || [];

  if (isLoading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-16 w-full" />
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
            Failed to load disputes. Please try again.
          </p>
        </CardContent>
      </Card>
    );
  }

  if (disputes.length === 0) {
    return (
      <Card>
        <CardContent className="p-12">
          <div className="text-center">
            <p className="text-gray-500">No disputes found</p>
            <p className="text-sm text-gray-400 mt-1">
              Disputes will appear here when customers raise chargebacks
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <>
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
                    className="hover:bg-gray-50"
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
                      <Badge variant={getDisputeStatusVariant(dispute.status)}>
                        {dispute.status}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-500">
                        {formatDateTime(dispute.createdAt)}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      {dispute.status === 'OPEN' && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setUploadingEvidence(dispute)}
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
        </CardContent>
      </Card>

      <EvidenceUploadDialog
        dispute={uploadingEvidence}
        open={!!uploadingEvidence}
        onClose={() => setUploadingEvidence(null)}
      />
    </>
  );
}
