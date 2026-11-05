'use client';

import { Header } from '@/components/layout/Header';
import { Card, CardContent, CardHeader, CardTitle, Badge, Skeleton } from '@fincore/ui';
import { useSystemHealth } from '@fincore/api-client/hooks';
import { adminApi } from '@/lib/api';

export default function SystemHealthPage() {
  const { data: health, isLoading, error } = useSystemHealth(adminApi);

  if (isLoading) {
    return (
      <div className="flex flex-col h-full">
        <Header title="System Health" />
        <div className="flex-1 p-6 space-y-6">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardHeader>
                <Skeleton className="h-6 w-48" />
              </CardHeader>
              <CardContent>
                <Skeleton className="h-32 w-full" />
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  if (error || !health) {
    return (
      <div className="flex flex-col h-full">
        <Header title="System Health" />
        <div className="flex-1 p-6">
          <Card>
            <CardContent className="pt-6">
              <p className="text-sm text-red-600 text-center">
                Failed to load system health data. Please try again.
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  const getStatusVariant = (status: string) => {
    return status === 'UP' || status === 'HEALTHY' ? 'completed' : 'failed';
  };

  const overallStatus = health.services?.every(s => s.status === 'UP' || s.status === 'HEALTHY')
    ? 'All Systems Operational'
    : 'Some Systems Down';

  return (
    <div className="flex flex-col h-full">
      <Header title="System Health" />

      <div className="flex-1 p-6 space-y-6">
        {/* Overall Status */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              System Status
              <Badge variant={health.services?.every(s => s.status === 'UP' || s.status === 'HEALTHY') ? 'completed' : 'failed'}>
                {overallStatus}
              </Badge>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-sm text-gray-600">
              Last updated: {new Date().toLocaleString()}
            </div>
          </CardContent>
        </Card>

        {/* Microservices Status */}
        {health.services && health.services.length > 0 && (
          <Card>
            <CardHeader>
              <CardTitle>Microservices</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                {health.services.map((service) => (
                  <div
                    key={service.name}
                    className="border border-gray-200 rounded-lg p-4 hover:border-platform-green transition-colors"
                  >
                    <div className="flex items-start justify-between">
                      <div>
                        <div className="font-medium">{service.name}</div>
                        <div className="text-sm text-gray-500">{service.type || 'Service'}</div>
                      </div>
                      <Badge variant={getStatusVariant(service.status)}>{service.status}</Badge>
                    </div>
                    {service.details && (
                      <div className="mt-3 text-xs text-gray-500">
                        {service.details}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        )}

        {/* Infrastructure - Show if we have the data */}
        {health.infrastructure && health.infrastructure.length > 0 && (
          <Card>
            <CardHeader>
              <CardTitle>Infrastructure</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {health.infrastructure.map((infra) => (
                <div
                  key={infra.name}
                  className="flex items-center justify-between p-4 border border-gray-200 rounded-lg"
                >
                  <div>
                    <div className="font-medium">{infra.name}</div>
                    <div className="text-sm text-gray-500">{infra.type}</div>
                  </div>
                  <div className="flex items-center gap-4">
                    {infra.type === 'Database' && (
                      <div className="text-sm">
                        <span className="text-gray-500">Connections:</span>{' '}
                        <span className="font-medium">
                          {infra.connections}/{infra.maxConnections}
                        </span>
                      </div>
                    )}
                    {infra.type === 'Cache' && (
                      <div className="text-sm">
                        <span className="text-gray-500">Memory:</span>{' '}
                        <span className="font-medium">
                          {infra.memory}/{infra.maxMemory}
                        </span>
                      </div>
                    )}
                    {infra.type === 'Message Queue' && (
                      <div className="text-sm">
                        <span className="text-gray-500">Topics/Partitions:</span>{' '}
                        <span className="font-medium">
                          {infra.topics}/{infra.partitions}
                        </span>
                      </div>
                    )}
                    <Badge variant={getStatusVariant(infra.status)}>{infra.status}</Badge>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Kafka Topics */}
        <Card>
          <CardHeader>
            <CardTitle>Kafka Topics</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="border-b border-gray-200 bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Topic
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Messages (24h)
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Consumer Lag
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Active Consumers
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Status
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {kafkaTopics.map((topic) => (
                    <tr key={topic.name} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-mono font-medium">{topic.name}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm">{topic.messages.toLocaleString()}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div
                          className={`text-sm ${topic.lag > 0 ? 'text-red-600 font-medium' : ''}`}
                        >
                          {topic.lag}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm">{topic.consumers}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <Badge variant={topic.lag === 0 ? 'completed' : 'pending'}>
                          {topic.lag === 0 ? 'Healthy' : 'Lagging'}
                        </Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>

        {/* Transactional Outbox Status */}
        <Card>
          <CardHeader>
            <CardTitle>Transactional Outbox</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
              <div className="border border-gray-200 rounded-lg p-4">
                <div className="text-sm text-gray-600">Pending</div>
                <div className="text-2xl font-semibold mt-1">{outboxStatus.pending}</div>
              </div>
              <div className="border border-gray-200 rounded-lg p-4">
                <div className="text-sm text-gray-600">Processing</div>
                <div className="text-2xl font-semibold mt-1">{outboxStatus.processing}</div>
              </div>
              <div className="border border-gray-200 rounded-lg p-4">
                <div className="text-sm text-gray-600">Published (24h)</div>
                <div className="text-2xl font-semibold mt-1 text-green-600">
                  {outboxStatus.published}
                </div>
              </div>
              <div className="border border-gray-200 rounded-lg p-4">
                <div className="text-sm text-gray-600">Failed</div>
                <div
                  className={`text-2xl font-semibold mt-1 ${
                    outboxStatus.failed > 0 ? 'text-red-600' : ''
                  }`}
                >
                  {outboxStatus.failed}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
