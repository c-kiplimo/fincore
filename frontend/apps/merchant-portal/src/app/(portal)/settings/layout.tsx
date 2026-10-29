'use client';

import { Header } from '@/components/layout/Header';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@payments-platform/ui';

const settingsTabs = [
  { name: 'API Keys', href: '/settings/api-keys' },
  { name: 'Webhooks', href: '/settings/webhooks' },
  { name: 'Profile', href: '/settings/profile' },
];

export default function SettingsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();

  return (
    <div className="flex flex-col h-full">
      <Header title="Settings" />

      <div className="flex-1 p-6">
        <div className="mb-6">
          <nav className="flex space-x-4 border-b border-gray-200">
            {settingsTabs.map((tab) => {
              const isActive = pathname === tab.href;
              return (
                <Link
                  key={tab.name}
                  href={tab.href}
                  className={cn(
                    'px-4 py-2 text-sm font-medium border-b-2 transition-colors',
                    isActive
                      ? 'border-platform-green text-platform-green'
                      : 'border-transparent text-gray-600 hover:text-gray-900 hover:border-gray-300'
                  )}
                >
                  {tab.name}
                </Link>
              );
            })}
          </nav>
        </div>

        {children}
      </div>
    </div>
  );
}
