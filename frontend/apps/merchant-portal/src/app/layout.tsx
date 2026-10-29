import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Merchant Portal - Payments Platform',
  description: 'Manage your payments, payouts, and business operations',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
