import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
} from 'react-native';
import { colors } from '@theme/colors';
import { spacing } from '@theme/spacing';
import { typography } from '@theme/typography';

export default function WalletScreen() {
  // TODO: Fetch from API
  const balance = 150000; // 1,500.00 KES
  const currency = 'KES';

  const recentTransactions = [
    {
      id: 'txn_1',
      description: 'Payment to Example Store',
      amount: -5000,
      createdAt: '2026-10-29T10:30:00Z',
      status: 'COMPLETED',
    },
    {
      id: 'txn_2',
      description: 'Top-up via M-Pesa',
      amount: 10000,
      createdAt: '2026-10-29T09:15:00Z',
      status: 'COMPLETED',
    },
  ];

  const formatBalance = (amountMinor: number) => {
    return (amountMinor / 100).toLocaleString('en', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  };

  return (
    <ScrollView style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>My Wallet</Text>
        <TouchableOpacity style={styles.notificationButton}>
          <Text style={{ fontSize: 24 }}>🔔</Text>
        </TouchableOpacity>
      </View>

      {/* Balance Card */}
      <View style={styles.balanceCard}>
        <Text style={styles.balanceLabel}>Available Balance</Text>
        <Text style={styles.balanceAmount}>
          {formatBalance(balance)} {currency}
        </Text>
        <Text style={styles.walletId}>Wallet ID: wal_abc123</Text>
      </View>

      {/* Quick Actions */}
      <View style={styles.quickActions}>
        <TouchableOpacity style={styles.actionButton}>
          <View style={styles.actionIcon}>
            <Text style={{ fontSize: 28 }}>💵</Text>
          </View>
          <Text style={styles.actionLabel}>Top Up</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionButton}>
          <View style={styles.actionIcon}>
            <Text style={{ fontSize: 28 }}>↗️</Text>
          </View>
          <Text style={styles.actionLabel}>Send</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionButton}>
          <View style={styles.actionIcon}>
            <Text style={{ fontSize: 28 }}>📱</Text>
          </View>
          <Text style={styles.actionLabel}>Request</Text>
        </TouchableOpacity>
      </View>

      {/* Recent Transactions */}
      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Recent Transactions</Text>
          <TouchableOpacity>
            <Text style={styles.viewAll}>View All</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.transactionList}>
          {recentTransactions.map(txn => (
            <TouchableOpacity key={txn.id} style={styles.transactionItem}>
              <View style={styles.transactionLeft}>
                <View
                  style={[
                    styles.transactionIcon,
                    {
                      backgroundColor:
                        txn.amount > 0 ? '#D1FAE5' : colors.gray100,
                    },
                  ]}>
                  <Text style={{ fontSize: 20 }}>{txn.amount > 0 ? '↓' : '💳'}</Text>
                </View>
                <View>
                  <Text style={styles.transactionDesc}>{txn.description}</Text>
                  <Text style={styles.transactionDate}>
                    {new Date(txn.createdAt).toLocaleDateString('en', {
                      month: 'short',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </Text>
                </View>
              </View>
              <View>
                <Text
                  style={[
                    styles.transactionAmount,
                    { color: txn.amount > 0 ? colors.success : colors.platformInk },
                  ]}>
                  {txn.amount > 0 ? '+' : ''}
                  {formatBalance(Math.abs(txn.amount))} {currency}
                </Text>
              </View>
            </TouchableOpacity>
          ))}
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
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: spacing.lg,
    backgroundColor: colors.platformWhite,
    borderBottomWidth: 1,
    borderBottomColor: colors.borderColor,
  },
  headerTitle: {
    fontSize: typography.fontSize.xl,
    fontWeight: typography.fontWeight.semibold,
    color: colors.platformInk,
  },
  notificationButton: {
    padding: spacing.sm,
  },
  balanceCard: {
    margin: spacing.lg,
    padding: spacing.xl,
    backgroundColor: colors.platformGreen,
    borderRadius: 16,
  },
  balanceLabel: {
    fontSize: typography.fontSize.sm,
    color: colors.platformWhite,
    opacity: 0.9,
  },
  balanceAmount: {
    fontSize: typography.fontSize['4xl'],
    fontWeight: typography.fontWeight.bold,
    color: colors.platformWhite,
    marginTop: spacing.sm,
  },
  walletId: {
    fontSize: typography.fontSize.sm,
    color: colors.platformWhite,
    opacity: 0.9,
    marginTop: spacing.lg,
    fontFamily: typography.fontFamily.mono,
  },
  quickActions: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingHorizontal: spacing.lg,
    marginBottom: spacing.xl,
  },
  actionButton: {
    alignItems: 'center',
  },
  actionIcon: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: colors.platformWhite,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  actionLabel: {
    fontSize: typography.fontSize.sm,
    fontWeight: typography.fontWeight.medium,
    color: colors.platformInk,
  },
  section: {
    marginTop: spacing.lg,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: spacing.lg,
    marginBottom: spacing.md,
  },
  sectionTitle: {
    fontSize: typography.fontSize.lg,
    fontWeight: typography.fontWeight.semibold,
    color: colors.platformInk,
  },
  viewAll: {
    fontSize: typography.fontSize.sm,
    color: colors.platformGreen,
    fontWeight: typography.fontWeight.medium,
  },
  transactionList: {
    backgroundColor: colors.platformWhite,
    borderTopWidth: 1,
    borderBottomWidth: 1,
    borderColor: colors.borderColor,
  },
  transactionItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: spacing.lg,
    borderBottomWidth: 1,
    borderBottomColor: colors.borderColor,
  },
  transactionLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  transactionIcon: {
    width: 40,
    height: 40,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
  },
  transactionDesc: {
    fontSize: typography.fontSize.sm,
    fontWeight: typography.fontWeight.medium,
    color: colors.platformInk,
  },
  transactionDate: {
    fontSize: typography.fontSize.xs,
    color: colors.gray500,
    marginTop: 2,
  },
  transactionAmount: {
    fontSize: typography.fontSize.base,
    fontWeight: typography.fontWeight.semibold,
  },
});
