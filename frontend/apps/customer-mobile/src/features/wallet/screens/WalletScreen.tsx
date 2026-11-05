import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
} from 'react-native';
import { useWallet, useTransferHistory } from '@fincore/api-client/hooks';
import { formatCurrency, formatDateTime } from '@fincore/utils';
import { walletsApi } from '@services/api';
import { colors } from '@theme/colors';
import { spacing } from '@theme/spacing';
import { typography } from '@theme/typography';

// TODO: Get wallet ID from auth context/storage
const WALLET_ID = 'wal_example';

export default function WalletScreen() {
  const {
    data: wallet,
    isLoading: walletLoading,
    error: walletError,
  } = useWallet(walletsApi, WALLET_ID);

  const {
    data: transactions,
    isLoading: txnLoading,
  } = useTransferHistory(walletsApi, WALLET_ID);

  const recentTransactions = transactions?.slice(0, 5) || [];

  if (walletLoading || txnLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color={colors.platformGreen} />
        <Text style={styles.loadingText}>Loading wallet...</Text>
      </View>
    );
  }

  if (walletError || !wallet) {
    return (
      <View style={styles.errorContainer}>
        <Text style={styles.errorText}>Failed to load wallet</Text>
        <Text style={styles.errorSubtext}>Please try again</Text>
      </View>
    );
  }

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
          {formatCurrency(wallet.balanceMinor, wallet.currency)}
        </Text>
        <Text style={styles.walletId}>Wallet ID: {wallet.id}</Text>
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
          {recentTransactions.length === 0 ? (
            <View style={styles.emptyState}>
              <Text style={styles.emptyText}>No transactions yet</Text>
              <Text style={styles.emptySubtext}>
                Your wallet activity will appear here
              </Text>
            </View>
          ) : (
            recentTransactions.map(txn => {
              const isCredit = txn.toWalletId === WALLET_ID;
              const typeLabel =
                txn.type === 'TOP_UP'
                  ? 'Top-up'
                  : txn.type === 'TRANSFER' || txn.type === 'P2P'
                  ? 'Transfer'
                  : 'Payment';

              return (
                <TouchableOpacity key={txn.id} style={styles.transactionItem}>
                  <View style={styles.transactionLeft}>
                    <View
                      style={[
                        styles.transactionIcon,
                        {
                          backgroundColor: isCredit ? '#D1FAE5' : colors.gray100,
                        },
                      ]}>
                      <Text style={{ fontSize: 20 }}>{isCredit ? '↓' : '💳'}</Text>
                    </View>
                    <View>
                      <Text style={styles.transactionDesc}>{typeLabel}</Text>
                      <Text style={styles.transactionDate}>
                        {formatDateTime(txn.createdAt)}
                      </Text>
                    </View>
                  </View>
                  <View>
                    <Text
                      style={[
                        styles.transactionAmount,
                        { color: isCredit ? colors.success : colors.platformInk },
                      ]}>
                      {isCredit ? '+' : '-'}
                      {formatCurrency(Math.abs(txn.amountMinor), txn.currency)}
                    </Text>
                  </View>
                </TouchableOpacity>
              );
            })
          )}
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
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: colors.canvas,
  },
  loadingText: {
    marginTop: spacing.md,
    fontSize: typography.fontSize.base,
    color: colors.gray500,
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: colors.canvas,
    padding: spacing.xl,
  },
  errorText: {
    fontSize: typography.fontSize.lg,
    fontWeight: typography.fontWeight.semibold,
    color: colors.platformInk,
    marginBottom: spacing.sm,
  },
  errorSubtext: {
    fontSize: typography.fontSize.sm,
    color: colors.gray500,
    textAlign: 'center',
  },
  emptyState: {
    padding: spacing.xl,
    alignItems: 'center',
  },
  emptyText: {
    fontSize: typography.fontSize.base,
    color: colors.gray500,
    marginBottom: spacing.xs,
  },
  emptySubtext: {
    fontSize: typography.fontSize.sm,
    color: colors.gray400,
    textAlign: 'center',
  },
});
