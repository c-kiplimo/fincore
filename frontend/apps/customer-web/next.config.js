/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  transpilePackages: ['@payments-platform/ui', '@payments-platform/types', '@payments-platform/api-client'],
  experimental: {
    optimizePackageImports: ['@payments-platform/ui'],
  },
};

module.exports = nextConfig;
