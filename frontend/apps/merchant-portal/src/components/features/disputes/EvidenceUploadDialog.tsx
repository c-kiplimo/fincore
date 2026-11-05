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
import { useUploadEvidence } from '@fincore/api-client/hooks';
import { disputesApi } from '@/lib/api';
import type { Dispute } from '@fincore/types';
import { formatCurrency } from '@fincore/utils';
import { toast } from 'sonner';

interface EvidenceUploadDialogProps {
  dispute: Dispute | null;
  open: boolean;
  onClose: () => void;
}

export function EvidenceUploadDialog({ dispute, open, onClose }: EvidenceUploadDialogProps) {
  const [file, setFile] = useState<File | null>(null);
  const [description, setDescription] = useState('');

  const uploadMutation = useUploadEvidence(disputesApi, {
    onSuccess: () => {
      toast.success('Evidence uploaded successfully');
      onClose();
      setFile(null);
      setDescription('');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to upload evidence');
    },
  });

  if (!dispute) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) {
      // Validate file size (max 10MB)
      if (selectedFile.size > 10 * 1024 * 1024) {
        toast.error('File size must be less than 10MB');
        return;
      }

      // Validate file type (allow common document and image formats)
      const allowedTypes = [
        'application/pdf',
        'image/jpeg',
        'image/png',
        'image/jpg',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      ];

      if (!allowedTypes.includes(selectedFile.type)) {
        toast.error('Only PDF, images (JPG/PNG), and Word documents are allowed');
        return;
      }

      setFile(selectedFile);
    }
  };

  const handleUpload = () => {
    if (!file) {
      toast.error('Please select a file to upload');
      return;
    }

    if (!description.trim()) {
      toast.error('Please provide a description for the evidence');
      return;
    }

    uploadMutation.mutate({
      paymentId: dispute.paymentId,
      disputeId: dispute.id,
      file,
      description,
    });
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Upload Evidence</DialogTitle>
          <DialogDescription>
            Provide supporting documents to contest this dispute.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="space-y-2 pb-4 border-b">
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Dispute ID:</span>
              <span className="font-mono font-medium">{dispute.id}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Payment ID:</span>
              <span className="font-mono font-medium">{dispute.paymentId}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Amount:</span>
              <span className="font-medium">
                {formatCurrency(dispute.amountMinor, dispute.currency)}
              </span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Reason:</span>
              <span className="text-gray-900">{dispute.reason}</span>
            </div>
          </div>

          <div>
            <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1.5">
              Evidence Description *
            </label>
            <Input
              id="description"
              type="text"
              placeholder="E.g., Delivery receipt, customer communication..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
            <p className="text-xs text-gray-500 mt-1">
              Provide a clear description of what this evidence proves
            </p>
          </div>

          <div>
            <label htmlFor="file" className="block text-sm font-medium text-gray-700 mb-1.5">
              Upload File *
            </label>
            <input
              id="file"
              type="file"
              accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
              onChange={handleFileChange}
              className="flex h-10 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm file:border-0 file:bg-transparent file:text-sm file:font-medium focus:outline-none focus:ring-2 focus:ring-platform-green"
            />
            {file && (
              <p className="text-xs text-gray-600 mt-1">
                Selected: {file.name} ({(file.size / 1024).toFixed(1)} KB)
              </p>
            )}
            <p className="text-xs text-gray-500 mt-1">
              Accepted: PDF, JPG, PNG, DOC, DOCX (max 10MB)
            </p>
          </div>

          <div className="bg-amber-50 border border-amber-200 rounded-lg p-3">
            <p className="text-xs text-amber-800">
              <strong>Tip:</strong> Include delivery confirmations, customer correspondence,
              product descriptions, or any documentation that proves you fulfilled the order.
            </p>
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={onClose}
            disabled={uploadMutation.isPending}
          >
            Cancel
          </Button>
          <Button
            onClick={handleUpload}
            disabled={uploadMutation.isPending || !file || !description.trim()}
          >
            {uploadMutation.isPending ? 'Uploading...' : 'Upload Evidence'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
