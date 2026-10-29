import { auth } from '@/lib/auth';
import { NextResponse } from 'next/server';

export default auth((req) => {
  const { pathname } = req.nextUrl;

  // Public routes that don't require authentication
  const publicRoutes = ['/login', '/signup'];
  const isPublicRoute = publicRoutes.some((route) => pathname.startsWith(route));

  // If user is not authenticated and trying to access protected route
  if (!req.auth && !isPublicRoute) {
    const loginUrl = new URL('/login', req.url);
    loginUrl.searchParams.set('callbackUrl', pathname);
    return NextResponse.redirect(loginUrl);
  }

  // If user is authenticated and trying to access login/signup, redirect to dashboard
  if (req.auth && isPublicRoute) {
    return NextResponse.redirect(new URL('/dashboard', req.url));
  }

  return NextResponse.next();
});

export const config = {
  matcher: ['/((?!api|_next/static|_next/image|favicon.ico).*)'],
};
