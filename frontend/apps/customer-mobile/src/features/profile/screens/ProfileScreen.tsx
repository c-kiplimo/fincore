import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { useProfile, useLinkedAccounts } from '@fincore/api-client/hooks';
import { settingsApi } from '@services/api';
import { clearAuthTokens } from '@services/api';
import { colors } from '@theme/colors';
import { spacing } from '@theme/spacing';
import { typography } from '@theme/typography';

// TODO: Get user ID from auth context/storage
const USER_ID = 'usr_example';

export default function ProfileScreen() {
  const {
    data: profile,
    isLoading: profileLoading,
    error: profileError,
  } = useProfile(settingsApi, USER_ID);

  const { data: linkedAccounts, isLoading: accountsLoading } = useLinkedAccounts(
    settingsApi,
    USER_ID
  );

  const handleSignOut = () => {
    Alert.alert(
      'Sign Out',
      'Are you sure you want to sign out?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Sign Out',
          style: 'destructive',
          onPress: async () => {
            try {
              await clearAuthTokens();
              // Navigation to login will be handled by auth state
              Alert.alert('Success', 'Signed out successfully');
            } catch (error) {
              Alert.alert('Error', 'Failed to sign out');
            }
          },
        },
      ]
    );
  };

  const MenuItem = ({ icon, title, subtitle, onPress }: any) => (
    <TouchableOpacity style={styles.menuItem} onPress={onPress}>
      <View style={styles.menuLeft}>
        <Text style={{ fontSize: 24 }}>{icon}</Text>
        <View>
          <Text style={styles.menuTitle}>{title}</Text>
          {subtitle && <Text style={styles.menuSubtitle}>{subtitle}</Text>}
        </View>
      </View>
      <Text style={styles.menuArrow}>›</Text>
    </TouchableOpacity>
  );

  if (profileLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color={colors.platformGreen} />
        <Text style={styles.loadingText}>Loading profile...</Text>
      </View>
    );
  }

  if (profileError || !profile) {
    return (
      <View style={styles.errorContainer}>
        <Text style={styles.errorText}>Failed to load profile</Text>
        <Text style={styles.errorSubtext}>Please try again</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Profile</Text>
      </View>

      {/* Profile Card */}
      <View style={styles.profileCard}>
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>{profile.name.charAt(0).toUpperCase()}</Text>
        </View>
        <Text style={styles.userName}>{profile.name}</Text>
        <Text style={styles.userEmail}>{profile.email}</Text>

        <TouchableOpacity style={styles.editButton}>
          <Text style={styles.editButtonText}>Edit Profile</Text>
        </TouchableOpacity>
      </View>

      {/* Security Section */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Security</Text>
        <View style={styles.menuList}>
          <MenuItem icon="🔒" title="Change Password" subtitle="Last changed 30 days ago" />
          <MenuItem icon="🔐" title="Two-Factor Authentication" subtitle="Not enabled" />
          <MenuItem icon="📱" title="Biometric Login" subtitle="Face ID enabled" />
        </View>
      </View>

      {/* Linked Accounts */}
      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Linked Bank Accounts</Text>
          <TouchableOpacity>
            <Text style={styles.addButton}>Add</Text>
          </TouchableOpacity>
        </View>
        {accountsLoading ? (
          <View style={styles.loadingSection}>
            <ActivityIndicator color={colors.platformGreen} />
          </View>
        ) : linkedAccounts && linkedAccounts.length > 0 ? (
          <View style={styles.accountList}>
            {linkedAccounts.map((account) => (
              <View key={account.id} style={styles.accountItem}>
                <View style={styles.accountLeft}>
                  <View style={styles.bankIcon}>
                    <Text style={{ fontSize: 20 }}>🏦</Text>
                  </View>
                  <Text style={styles.accountName}>
                    {account.bankName || 'Bank Account'} - {account.maskedNumber || account.accountNumber}
                  </Text>
                </View>
                {account.verified && (
                  <View style={styles.verifiedBadge}>
                    <Text style={styles.verifiedText}>Verified</Text>
                  </View>
                )}
              </View>
            ))}
          </View>
        ) : (
          <View style={styles.emptyState}>
            <Text style={styles.emptyText}>No linked bank accounts</Text>
          </View>
        )}
      </View>

      {/* Preferences */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Preferences</Text>
        <View style={styles.menuList}>
          <MenuItem icon="🔔" title="Notifications" subtitle="Email, Push, SMS" />
          <MenuItem icon="🌐" title="Language & Region" subtitle="English (Kenya)" />
          <MenuItem icon="💰" title="Default Currency" subtitle="KES" />
        </View>
      </View>

      {/* Help */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Help & Support</Text>
        <View style={styles.menuList}>
          <MenuItem icon="❓" title="Help Center" />
          <MenuItem icon="📧" title="Contact Support" />
          <MenuItem icon="📄" title="Terms & Privacy" />
        </View>
      </View>

      {/* Sign Out */}
      <TouchableOpacity style={styles.signOutButton} onPress={handleSignOut}>
        <Text style={styles.signOutText}>Sign Out</Text>
      </TouchableOpacity>

      <Text style={styles.version}>Version 1.0.0</Text>
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
  profileCard: {
    margin: spacing.lg,
    padding: spacing.xl,
    backgroundColor: colors.platformWhite,
    borderRadius: 12,
    alignItems: 'center',
  },
  avatar: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: colors.platformGreen,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  avatarText: {
    fontSize: typography.fontSize['2xl'],
    fontWeight: typography.fontWeight.semibold,
    color: colors.platformWhite,
  },
  userName: {
    fontSize: typography.fontSize.lg,
    fontWeight: typography.fontWeight.semibold,
    color: colors.platformInk,
    marginBottom: 4,
  },
  userEmail: {
    fontSize: typography.fontSize.sm,
    color: colors.gray600,
    marginBottom: spacing.lg,
  },
  editButton: {
    paddingHorizontal: spacing.xl,
    paddingVertical: spacing.sm,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: colors.borderColor,
  },
  editButtonText: {
    fontSize: typography.fontSize.sm,
    fontWeight: typography.fontWeight.medium,
    color: colors.platformInk,
  },
  section: {
    marginBottom: spacing.xl,
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
    paddingHorizontal: spacing.lg,
    marginBottom: spacing.md,
  },
  addButton: {
    fontSize: typography.fontSize.sm,
    color: colors.platformGreen,
    fontWeight: typography.fontWeight.medium,
  },
  menuList: {
    backgroundColor: colors.platformWhite,
    borderTopWidth: 1,
    borderBottomWidth: 1,
    borderColor: colors.borderColor,
  },
  menuItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: spacing.lg,
    borderBottomWidth: 1,
    borderBottomColor: colors.borderColor,
  },
  menuLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  menuTitle: {
    fontSize: typography.fontSize.sm,
    fontWeight: typography.fontWeight.medium,
    color: colors.platformInk,
  },
  menuSubtitle: {
    fontSize: typography.fontSize.xs,
    color: colors.gray500,
    marginTop: 2,
  },
  menuArrow: {
    fontSize: 24,
    color: colors.gray400,
  },
  accountList: {
    backgroundColor: colors.platformWhite,
    borderTopWidth: 1,
    borderBottomWidth: 1,
    borderColor: colors.borderColor,
    gap: spacing.sm,
    padding: spacing.lg,
  },
  accountItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: spacing.md,
    backgroundColor: colors.platformWhite,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: colors.borderColor,
  },
  accountLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  bankIcon: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.gray100,
    justifyContent: 'center',
    alignItems: 'center',
  },
  accountName: {
    fontSize: typography.fontSize.sm,
    fontWeight: typography.fontWeight.medium,
    color: colors.platformInk,
  },
  verifiedBadge: {
    backgroundColor: colors.success,
    paddingHorizontal: spacing.sm,
    paddingVertical: 4,
    borderRadius: 4,
  },
  verifiedText: {
    fontSize: typography.fontSize.xs,
    color: colors.platformWhite,
    fontWeight: typography.fontWeight.medium,
  },
  signOutButton: {
    margin: spacing.lg,
    padding: spacing.md,
    backgroundColor: colors.error,
    borderRadius: 8,
    alignItems: 'center',
  },
  signOutText: {
    color: colors.platformWhite,
    fontSize: typography.fontSize.base,
    fontWeight: typography.fontWeight.medium,
  },
  version: {
    textAlign: 'center',
    fontSize: typography.fontSize.xs,
    color: colors.gray500,
    paddingBottom: spacing.xl,
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
  loadingSection: {
    padding: spacing.xl,
    alignItems: 'center',
    backgroundColor: colors.platformWhite,
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
    backgroundColor: colors.platformWhite,
    borderTopWidth: 1,
    borderBottomWidth: 1,
    borderColor: colors.borderColor,
  },
  emptyText: {
    fontSize: typography.fontSize.sm,
    color: colors.gray500,
  },
});
