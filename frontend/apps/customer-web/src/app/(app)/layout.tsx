'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@payments-platform/ui';

const navigation = [
  { name: 'Wallet', href: '/wallet', icon: '💰' },
  { name: 'Pay', href: '/payments', icon: '💳' },
  { name: 'Transfer', href: '/transfers', icon: '↔️' },
  { name: 'Profile', href: '/profile', icon: '👤' },
];

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  return (
    <div className="flex flex-col h-screen bg-canvas">
      {/* Top Header */}
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-4xl mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="h-8 w-8 rounded-lg bg-platform-green"></div>
              <span className="text-lg font-semibold">My Wallet</span>
            </div>
            <button className="rounded-lg p-2 hover:bg-gray-100">
              <span className="text-xl">🔔</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 overflow-auto">
        <div className="max-w-4xl mx-auto">{children}</div>
      </main>

      {/* Bottom Navigation */}
      <nav className="bg-white border-t border-gray-200 safe-area-bottom">
        <div className="max-w-4xl mx-auto">
          <div className="grid grid-cols-4">
            {navigation.map((item) => {
              const isActive = pathname === item.href || pathname?.startsWith(item.href + '/');
              return (
                <Link
                  key={item.name}
                  href={item.href}
                  className={cn(
                    'flex flex-col items-center justify-center py-3 text-sm transition-colors',
                    isActive
                      ? 'text-platform-green'
                      : 'text-gray-600 hover:text-gray-900'
                  )}
                >
                  <span className="text-2xl mb-1">{item.icon}</span>
                  <span className="font-medium">{item.name}</span>
                </Link>
              );
            })}
          </div>
        </div>
      </nav>
    </div>
  );
}
