/**
 * Dispute types
 */

export type DisputeStatus = 'OPEN' | 'PENDING' | 'WON' | 'LOST' | 'RESOLVED' | 'CLOSED';

export interface Dispute {
  id: string;
  paymentIntentId: string;
  merchantId: string;
  amountMinor: number;
  currency: string;
  reason: string;
  status: DisputeStatus;
  evidenceUrl?: string;
  createdAt: string;
  updatedAt?: string;
  resolvedAt?: string;
}

export interface CreateDisputeRequest {
  paymentIntentId: string;
  reason: string;
}

export interface ResolveDisputeRequest {
  outcome: 'WON' | 'LOST';
  notes?: string;
}

export interface UploadEvidenceRequest {
  disputeId: string;
  file: File;
  description?: string;
}
