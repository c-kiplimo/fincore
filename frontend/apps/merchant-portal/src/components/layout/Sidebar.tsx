'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@fincore/ui';

const navigation = [
  { name: 'Dashboard', href: '/dashboard', icon: '📊' },
  { name: 'Transactions', href: '/transactions', icon: '💳' },
  { name: 'Payouts', href: '/payouts', icon: '💰' },
  { name: 'Disputes', href: '/disputes', icon: '⚠️' },
  { name: 'Settings', href: '/settings', icon: '⚙️' },
  { name: 'Reports', href: '/reports', icon: '📈' },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <div className="flex h-screen w-64 flex-col border-r border-gray-200 bg-white">
      {/* Logo */}
      <div className="flex h-16 items-center border-b border-gray-200 px-6">
        <div className="flex items-center gap-2">
          <div className="h-8 w-8 rounded-lg bg-platform-green"></div>
          <span className="text-xl font-semibold">Payments</span>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1 px-3 py-4">
        {navigation.map((item) => {
          const isActive = pathname === item.href || pathname?.startsWith(item.href + '/');
          return (
            <Link
              key={item.name}
              href={item.href}
              className={cn(
                'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-gray-100 text-platform-ink'
                  : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
              )}
            >
              <span className="text-lg">{item.icon}</span>
              {item.name}
            </Link>
          );
        })}
      </nav>

      {/* User section */}
      <div className="border-t border-gray-200 p-4">
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-full bg-gray-200"></div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium truncate">Merchant User</p>
            <p className="text-xs text-gray-500 truncate">merchant@example.com</p>
          </div>
        </div>
      </div>
    </div>
  );
}
