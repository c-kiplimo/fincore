import { Header } from '@/components/layout/Header';
import { Card, CardContent, CardHeader, CardTitle, Badge } from '@fincore/ui';

export default function SystemHealthPage() {
  // TODO: Fetch from actuator endpoints
  const services = [
    { name: 'API Gateway', port: 8080, status: 'UP', uptime: '15d 4h', version: '1.0.0' },
    { name: 'Ledger Service', port: 8081, status: 'UP', uptime: '15d 4h', version: '1.0.0' },
    { name: 'Pricing Service', port: 8082, status: 'UP', uptime: '15d 4h', version: '1.0.0' },
    { name: 'Payments Service', port: 8083, status: 'UP', uptime: '15d 4h', version: '1.0.0' },
    { name: 'Accounts Service', port: 8084, status: 'UP', uptime: '15d 4h', version: '1.0.0' },
    { name: 'Payouts Service', port: 8085, status: 'UP', uptime: '15d 4h', version: '1.0.0' },
    { name: 'Lending Service', port: 8086, status: 'UP', uptime: '15d 4h', version: '1.0.0' },
    { name: 'Reconciliation Service', port: 8087, status: 'UP', uptime: '15d 4h', version: '1.0.0' },
    { name: 'Rails Simulator', port: 8088, status: 'UP', uptime: '15d 4h', version: '1.0.0' },
  ];

  const infrastructure = [
    { name: 'PostgreSQL', type: 'Database', status: 'UP', connections: 42, maxConnections: 100 },
    { name: 'Redis', type: 'Cache', status: 'UP', memory: '450MB', maxMemory: '2GB' },
    { name: 'Kafka', type: 'Message Queue', status: 'UP', topics: 8, partitions: 24 },
  ];

  const kafkaTopics = [
    { name: 'payment-events', messages: 1420, lag: 0, consumers: 2 },
    { name: 'payout-events', messages: 340, lag: 0, consumers: 1 },
    { name: 'ledger-events', messages: 2800, lag: 0, consumers: 3 },
    { name: 'dispute-events', messages: 15, lag: 2, consumers: 1 },
  ];

  const outboxStatus = {
    pending: 0,
    processing: 0,
    published: 1520,
    failed: 3,
  };

  const getStatusVariant = (status: string) => {
    return status === 'UP' ? 'completed' : 'failed';
  };

  return (
    <div className="flex flex-col h-full">
      <Header title="System Health" />

      <div className="flex-1 p-6 space-y-6">
        {/* Overall Status */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              System Status
              <Badge variant="completed">All Systems Operational</Badge>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-sm text-gray-600">
              Last updated: {new Date().toLocaleString()}
            </div>
          </CardContent>
        </Card>

        {/* Microservices Status */}
        <Card>
          <CardHeader>
            <CardTitle>Microservices</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
              {services.map((service) => (
                <div
                  key={service.port}
                  className="border border-gray-200 rounded-lg p-4 hover:border-platform-green transition-colors"
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="font-medium">{service.name}</div>
                      <div className="text-sm text-gray-500">Port {service.port}</div>
                    </div>
                    <Badge variant={getStatusVariant(service.status)}>{service.status}</Badge>
                  </div>
                  <div className="mt-3 space-y-1">
                    <div className="text-xs text-gray-500">
                      Uptime: <span className="text-gray-900">{service.uptime}</span>
                    </div>
                    <div className="text-xs text-gray-500">
                      Version: <span className="text-gray-900">{service.version}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Infrastructure */}
        <Card>
          <CardHeader>
            <CardTitle>Infrastructure</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {infrastructure.map((infra) => (
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
