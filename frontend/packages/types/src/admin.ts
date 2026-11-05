/**
 * Admin dashboard types
 */

export interface PlatformMetrics {
  totalRevenue: number;
  totalTransactions: number;
  activeMerchants: number;
  activeCustomers: number;
  successRate: number;
  averageTransactionValue: number;
  chartData: ChartDataPoint[];
}

export interface ChartDataPoint {
  date: string;
  value: number;
  label?: string;
}

export interface SystemHealth {
  services: ServiceStatus[];
  kafka: KafkaStatus;
  database: DatabaseStatus;
  outbox: OutboxStatus;
  overallStatus: 'HEALTHY' | 'DEGRADED' | 'DOWN';
}

export interface ServiceStatus {
  name: string;
  status: 'UP' | 'DOWN' | 'UNKNOWN';
  url: string;
  responseTime?: number;
  lastChecked: string;
  version?: string;
}

export interface KafkaStatus {
  status: 'CONNECTED' | 'DISCONNECTED';
  brokers: number;
  topics: KafkaTopic[];
}

export interface KafkaTopic {
  name: string;
  partitions: number;
  messages: number;
}

export interface DatabaseStatus {
  status: 'CONNECTED' | 'DISCONNECTED';
  connections: number;
  maxConnections: number;
  responseTime?: number;
}

export interface OutboxStatus {
  pendingEvents: number;
  oldestPendingAge?: number;
  publishRate: number;
  failureRate: number;
}

export interface MerchantListItem {
  id: string;
  name: string;
  country: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'INACTIVE';
  totalRevenue: number;
  transactionCount: number;
  createdAt: string;
}

export interface SettlementBatch {
  id: string;
  entityId: string;
  batchNumber: number;
  totalAmountMinor: number;
  currency: string;
  transactionCount: number;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  createdAt: string;
  completedAt?: string;
}
