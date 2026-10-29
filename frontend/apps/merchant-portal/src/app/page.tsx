import { redirect } from 'next/navigation';

export default function HomePage() {
  // Redirect to dashboard (will be login page if not authenticated)
  redirect('/dashboard');
}
