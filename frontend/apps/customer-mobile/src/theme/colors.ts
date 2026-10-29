/**
 * Ramp-inspired color palette for the Payments Platform mobile app.
 * Matches the design system used across web applications.
 */

export const colors = {
  // Primary Colors
  platformGreen: '#00D395',
  platformGreenDark: '#00BA7F',
  platformInk: '#1C1B17',
  platformWhite: '#FFFFFF',
  canvas: '#FAFAF9',

  // Grayscale
  gray50: '#F9FAFB',
  gray100: '#F3F4F6',
  gray200: '#E5E7EB',
  gray300: '#D1D5DB',
  gray400: '#9CA3AF',
  gray500: '#6B7280',
  gray600: '#4B5563',
  gray700: '#374151',
  gray800: '#1F2937',
  gray900: '#111827',

  // Semantic Colors
  success: '#10B981',
  warning: '#F59E0B',
  error: '#EF4444',
  info: '#3B82F6',

  // Component-specific
  cardBackground: '#FFFFFF',
  borderColor: '#E5E7EB',
  inputBackground: '#F9FAFB',
  inputBorder: '#D1D5DB',
  placeholder: '#9CA3AF',

  // Status colors
  statusPending: '#F59E0B',
  statusCompleted: '#10B981',
  statusFailed: '#EF4444',
  statusProcessing: '#3B82F6',

  // Gradients (for balance cards, etc.)
  gradientStart: '#00D395',
  gradientEnd: '#00BA7F',
};

export type Colors = typeof colors;
