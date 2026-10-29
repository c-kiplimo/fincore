/**
 * Ramp-Inspired Design System Colors
 *
 * Design Philosophy:
 * - Minimal, monochrome approach with one bold accent
 * - High contrast for accessibility
 * - Warm tones (not cold grays) for human-designed feel
 * - Conservative for fintech, modern for 2026
 */

export const colors = {
  // Primary Brand Colors
  platformGreen: '#00D395',      // Electric teal-green (signature color)
  platformGreenDark: '#00BA7F',  // Hover state
  platformInk: '#1C1B17',        // Warm near-black (never pure black)
  platformWhite: '#FFFFFF',
  canvas: '#FAFAF9',             // Warm off-white background

  // Grayscale
  gray: {
    50: '#F9FAFB',
    100: '#F3F4F6',
    200: '#E5E7EB',
    300: '#D1D5DB',
    400: '#9CA3AF',
    500: '#6B7280',
    600: '#4B5563',
    700: '#374151',
    800: '#1F2937',
    900: '#111827',
  },

  // Semantic Colors
  success: '#10B981',            // Green for success states
  successLight: '#D1FAE5',       // Success background

  warning: '#F59E0B',            // Amber for warnings
  warningLight: '#FEF3C7',       // Warning background

  error: '#EF4444',              // Red for errors
  errorLight: '#FEE2E2',         // Error background

  info: '#3B82F6',               // Blue for info
  infoLight: '#DBEAFE',          // Info background

  // Transaction Status Colors
  status: {
    pending: '#F59E0B',          // Amber
    processing: '#3B82F6',       // Blue
    completed: '#10B981',        // Green
    failed: '#EF4444',           // Red
    canceled: '#6B7280',         // Gray
  },
} as const;

export type Color = typeof colors;

/**
 * Tailwind CSS Custom Colors Configuration
 * Use this in tailwind.config.ts
 */
export const tailwindColors = {
  'platform-green': colors.platformGreen,
  'platform-green-dark': colors.platformGreenDark,
  'platform-ink': colors.platformInk,
  'canvas': colors.canvas,
  'semantic-success': colors.success,
  'semantic-warning': colors.warning,
  'semantic-error': colors.error,
  'semantic-info': colors.info,
};
