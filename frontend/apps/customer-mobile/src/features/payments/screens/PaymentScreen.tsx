import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  TextInput,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { usePayments, useCreatePayment } from '@fincore/api-client/hooks';
import { formatRelativeDate } from '@fincore/utils';
import { paymentsApi } from '@services/api';
import { colors } from '@theme/colors';
import { spacing } from '@theme/spacing';
import { typography } from '@theme/typography';

export default function PaymentScreen() {
  const [merchantId, setMerchantId] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');

  const { data: paymentsData, isLoading: paymentsLoading } = usePayments(
    paymentsApi,
    { limit: 20 }
  );

  const createPaymentMutation = useCreatePayment(paymentsApi, {
    onSuccess: () => {
      Alert.alert('Success', 'Payment initiated successfully');
      setMerchantId('');
      setAmount('');
      setDescription('');
    },
    onError: (error: any) => {
      Alert.alert('Error', error.message || 'Failed to create payment');
    },
  });

  // Extract unique recent merchants from payment history
  const recentMerchants = paymentsData?.items
    ? Array.from(
        new Map(
          paymentsData.items
            .filter((p) => p.merchantId)
            .map((p) => [
              p.merchantId,
              {
                id: p.merchantId,
                name: p.merchantName || `Merchant ${p.merchantId.slice(0, 8)}`,
                lastPaid: p.createdAt,
              },
            ])
        ).values()
      ).slice(0, 5)
    : [];

  const handleCreatePayment = () => {
    if (!merchantId.trim()) {
      Alert.alert('Validation Error', 'Please enter a merchant ID');
      return;
    }

    const amountValue = parseFloat(amount);
    if (isNaN(amountValue) || amountValue <= 0) {
      Alert.alert('Validation Error', 'Please enter a valid amount');
      return;
    }

    createPaymentMutation.mutate({
      data: {
        merchantId: merchantId.trim(),
        amountMinor: Math.round(amountValue * 100),
        currency: 'KES',
        description: description.trim() || undefined,
      },
    });
  };

  const handlePayMerchant = (merchantIdValue: string) => {
    setMerchantId(merchantIdValue);
  };

  return (
    <ScrollView style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Make a Payment</Text>
      </View>

      {/* QR Scanner */}
      <View style={styles.card}>
        <View style={styles.qrScanner}>
          <View style={styles.qrIcon}>
            <Text style={{ fontSize: 64 }}>📷</Text>
          </View>
          <Text style={styles.scanTitle}>Scan QR Code</Text>
          <Text style={styles.scanDescription}>
            Scan merchant's QR code to pay instantly
          </Text>
          <TouchableOpacity style={styles.primaryButton}>
            <Text style={styles.primaryButtonText}>Open Camera</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Manual Payment */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Pay Merchant</Text>
        <View style={styles.form}>
          <Text style={styles.label}>Merchant ID</Text>
          <TextInput
            style={styles.input}
            placeholder="Enter merchant ID..."
            placeholderTextColor={colors.placeholder}
            value={merchantId}
            onChangeText={setMerchantId}
            editable={!createPaymentMutation.isPending}
          />

          <Text style={styles.label}>Amount (KES)</Text>
          <TextInput
            style={styles.input}
            placeholder="0.00"
            keyboardType="numeric"
            placeholderTextColor={colors.placeholder}
            value={amount}
            onChangeText={setAmount}
            editable={!createPaymentMutation.isPending}
          />

          <Text style={styles.label}>Description (Optional)</Text>
          <TextInput
            style={styles.input}
            placeholder="What's this for?"
            placeholderTextColor={colors.placeholder}
            value={description}
            onChangeText={setDescription}
            editable={!createPaymentMutation.isPending}
          />

          <TouchableOpacity
            style={[
              styles.primaryButton,
              createPaymentMutation.isPending && styles.buttonDisabled,
            ]}
            onPress={handleCreatePayment}
            disabled={createPaymentMutation.isPending}
          >
            {createPaymentMutation.isPending ? (
              <ActivityIndicator color={colors.platformWhite} />
            ) : (
              <Text style={styles.primaryButtonText}>Continue to Payment</Text>
            )}
          </TouchableOpacity>
        </View>
      </View>

      {/* Recent Merchants */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Recent Merchants</Text>
        {paymentsLoading ? (
          <View style={styles.loadingContainer}>
            <ActivityIndicator color={colors.platformGreen} />
          </View>
        ) : recentMerchants.length === 0 ? (
          <View style={styles.emptyState}>
            <Text style={styles.emptyText}>No recent merchants</Text>
            <Text style={styles.emptySubtext}>Your payment history will appear here</Text>
          </View>
        ) : (
          recentMerchants.map((merchant) => (
            <TouchableOpacity
              key={merchant.id}
              style={styles.merchantItem}
              onPress={() => handlePayMerchant(merchant.id)}
            >
              <View style={styles.merchantLeft}>
                <View style={styles.merchantAvatar}>
                  <Text style={styles.merchantAvatarText}>
                    {merchant.name.charAt(0).toUpperCase()}
                  </Text>
                </View>
                <View>
                  <Text style={styles.merchantName}>{merchant.name}</Text>
                  <Text style={styles.merchantDetail}>
                    Last paid {formatRelativeDate(merchant.lastPaid)}
                  </Text>
                </View>
              </View>
              <TouchableOpacity style={styles.payButton}>
                <Text style={styles.payButtonText}>Pay</Text>
              </TouchableOpacity>
            </TouchableOpacity>
          ))
        )}
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
  card: {
    margin: spacing.lg,
    padding: spacing.lg,
    backgroundColor: colors.platformWhite,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.borderColor,
  },
  qrScanner: {
    alignItems: 'center',
    paddingVertical: spacing.xl,
  },
  qrIcon: {
    width: 128,
    height: 128,
    borderRadius: 12,
    backgroundColor: colors.gray100,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: spacing.lg,
  },
  scanTitle: {
    fontSize: typography.fontSize.base,
    fontWeight: typography.fontWeight.semibold,
    color: colors.platformInk,
    marginBottom: spacing.sm,
  },
  scanDescription: {
    fontSize: typography.fontSize.sm,
    color: colors.gray600,
    textAlign: 'center',
    marginBottom: spacing.lg,
  },
  primaryButton: {
    backgroundColor: colors.platformGreen,
    paddingHorizontal: spacing.xl,
    paddingVertical: spacing.md,
    borderRadius: 8,
  },
  primaryButtonText: {
    color: colors.platformWhite,
    fontSize: typography.fontSize.base,
    fontWeight: typography.fontWeight.medium,
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
  section: {
    marginTop: spacing.lg,
    paddingHorizontal: spacing.lg,
  },
  sectionTitle: {
    fontSize: typography.fontSize.lg,
    fontWeight: typography.fontWeight.semibold,
    color: colors.platformInk,
    marginBottom: spacing.md,
  },
  merchantItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: colors.platformWhite,
    padding: spacing.lg,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.borderColor,
    marginBottom: spacing.sm,
  },
  merchantLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  merchantAvatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.platformGreen,
    opacity: 0.1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  merchantAvatarText: {
    fontSize: typography.fontSize.base,
    fontWeight: typography.fontWeight.semibold,
    color: colors.platformGreen,
  },
  merchantName: {
    fontSize: typography.fontSize.sm,
    fontWeight: typography.fontWeight.medium,
    color: colors.platformInk,
  },
  merchantDetail: {
    fontSize: typography.fontSize.xs,
    color: colors.gray500,
  },
  payButton: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  payButtonText: {
    fontSize: typography.fontSize.sm,
    color: colors.platformGreen,
    fontWeight: typography.fontWeight.medium,
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  loadingContainer: {
    padding: spacing.xl,
    alignItems: 'center',
  },
  emptyState: {
    padding: spacing.xl,
    alignItems: 'center',
    backgroundColor: colors.platformWhite,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.borderColor,
  },
  emptyText: {
    fontSize: typography.fontSize.sm,
    color: colors.gray500,
    marginBottom: spacing.xs,
  },
  emptySubtext: {
    fontSize: typography.fontSize.xs,
    color: colors.gray400,
    textAlign: 'center',
  },
});
