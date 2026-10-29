import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  TextInput,
} from 'react-native';
import { colors } from '@theme/colors';
import { spacing } from '@theme/spacing';
import { typography } from '@theme/typography';

export default function TransferScreen() {
  return (
    <ScrollView style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Transfer Money</Text>
      </View>

      {/* Transfer Options */}
      <View style={styles.options}>
        <TouchableOpacity style={styles.optionCard}>
          <View style={styles.optionIcon}>
            <Text style={{ fontSize: 32 }}>👥</Text>
          </View>
          <Text style={styles.optionTitle}>Send to Friend</Text>
          <Text style={styles.optionSubtitle}>P2P Transfer</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.optionCard}>
          <View style={styles.optionIcon}>
            <Text style={{ fontSize: 32 }}>🏦</Text>
          </View>
          <Text style={styles.optionTitle}>Withdraw</Text>
          <Text style={styles.optionSubtitle}>To Bank Account</Text>
        </TouchableOpacity>
      </View>

      {/* Send Money Form */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Send to Wallet</Text>
        <View style={styles.form}>
          <Text style={styles.label}>Recipient Phone or Wallet ID</Text>
          <TextInput
            style={styles.input}
            placeholder="+254712345678 or wal_xyz..."
            placeholderTextColor={colors.placeholder}
          />

          <Text style={styles.label}>Amount (KES)</Text>
          <TextInput
            style={styles.input}
            placeholder="0.00"
            keyboardType="numeric"
            placeholderTextColor={colors.placeholder}
          />

          <Text style={styles.label}>Note (Optional)</Text>
          <TextInput
            style={styles.input}
            placeholder="What's this for?"
            placeholderTextColor={colors.placeholder}
          />

          <View style={styles.summary}>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>Transfer Fee</Text>
              <Text style={styles.summaryValue}>0.00 KES</Text>
            </View>
            <View style={[styles.summaryRow, styles.summaryTotal]}>
              <Text style={styles.summaryTotalLabel}>Total</Text>
              <Text style={styles.summaryTotalValue}>0.00 KES</Text>
            </View>
          </View>

          <TouchableOpacity style={styles.primaryButton}>
            <Text style={styles.primaryButtonText}>Send Money</Text>
          </TouchableOpacity>
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.canvas,
  },
  header: {
    padding: spacing.lg,
    backgroundColor: colors.platformWhite,
    borderBottomWidth: 1,
    borderBottomColor: colors.borderColor,
  },
  headerTitle: {
    fontSize: typography.fontSize['2xl'],
    fontWeight: typography.fontWeight.semibold,
    color: colors.platformInk,
  },
  options: {
    flexDirection: 'row',
    gap: spacing.md,
    padding: spacing.lg,
  },
  optionCard: {
    flex: 1,
    padding: spacing.xl,
    backgroundColor: colors.platformWhite,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.borderColor,
    alignItems: 'center',
  },
  optionIcon: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: colors.gray50,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  optionTitle: {
    fontSize: typography.fontSize.base,
    fontWeight: typography.fontWeight.medium,
    color: colors.platformInk,
    marginBottom: 4,
  },
  optionSubtitle: {
    fontSize: typography.fontSize.xs,
    color: colors.gray500,
  },
  card: {
    margin: spacing.lg,
    marginTop: 0,
    padding: spacing.lg,
    backgroundColor: colors.platformWhite,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.borderColor,
  },
  cardTitle: {
    fontSize: typography.fontSize.lg,
    fontWeight: typography.fontWeight.semibold,
    color: colors.platformInk,
    marginBottom: spacing.lg,
  },
  form: {
    gap: spacing.md,
  },
  label: {
    fontSize: typography.fontSize.sm,
    fontWeight: typography.fontWeight.medium,
    color: colors.gray700,
    marginBottom: 4,
  },
  input: {
    height: 40,
    borderWidth: 1,
    borderColor: colors.inputBorder,
    borderRadius: 8,
    paddingHorizontal: spacing.md,
    fontSize: typography.fontSize.base,
    backgroundColor: colors.platformWhite,
  },
  summary: {
    backgroundColor: colors.gray50,
    borderRadius: 8,
    padding: spacing.md,
    gap: spacing.sm,
  },
  summaryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  summaryLabel: {
    fontSize: typography.fontSize.sm,
    color: colors.gray600,
  },
  summaryValue: {
    fontSize: typography.fontSize.sm,
    fontWeight: typography.fontWeight.medium,
    color: colors.platformInk,
  },
  summaryTotal: {
    borderTopWidth: 1,
    borderTopColor: colors.gray200,
    paddingTop: spacing.sm,
  },
  summaryTotalLabel: {
    fontSize: typography.fontSize.sm,
    fontWeight: typography.fontWeight.medium,
    color: colors.platformInk,
  },
  summaryTotalValue: {
    fontSize: typography.fontSize.sm,
    fontWeight: typography.fontWeight.semibold,
    color: colors.platformInk,
  },
  primaryButton: {
    backgroundColor: colors.platformGreen,
    paddingVertical: spacing.md,
    borderRadius: 8,
    alignItems: 'center',
  },
  primaryButtonText: {
    color: colors.platformWhite,
    fontSize: typography.fontSize.base,
    fontWeight: typography.fontWeight.medium,
  },
});
