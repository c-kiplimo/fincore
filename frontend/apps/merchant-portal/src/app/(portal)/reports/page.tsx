import { Header } from '@/components/layout/Header';
import { ReportGenerator } from '@/components/features/reports/ReportGenerator';
import { ReportHistory } from '@/components/features/reports/ReportHistory';

export default function ReportsPage() {
  return (
    <div className="flex flex-col h-full">
      <Header title="Reports" />

      <div className="flex-1 p-6 space-y-6">
        <div>
          <h2 className="text-lg font-semibold">Generate Reports</h2>
          <p className="text-sm text-gray-600">
            Export transaction reports, revenue summaries, and tax reports
          </p>
        </div>

        <ReportGenerator />
        <ReportHistory />
      </div>
    </div>
  );
}
