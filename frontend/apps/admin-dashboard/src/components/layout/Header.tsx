'use client';

export function Header({ title }: { title: string }) {
  return (
    <header className="border-b border-gray-200 bg-white">
      <div className="flex h-16 items-center justify-between px-6">
        <h1 className="text-2xl font-semibold">{title}</h1>
        <div className="flex items-center gap-4">
          <button className="rounded-lg p-2 hover:bg-gray-100">
            <span className="text-xl">🔔</span>
          </button>
          <button className="text-sm font-medium text-gray-700 hover:text-gray-900">
            Sign out
          </button>
        </div>
      </div>
    </header>
  );
}
