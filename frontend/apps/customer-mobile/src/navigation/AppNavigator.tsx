import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { TabNavigatorParamList, WalletStackParamList, PaymentsStackParamList, TransfersStackParamList, ProfileStackParamList } from './types';
import { colors } from '@theme/colors';

// Feature Screens (placeholders - will be implemented)
import WalletScreen from '@features/wallet/screens/WalletScreen';
import PaymentScreen from '@features/payments/screens/PaymentScreen';
import TransferScreen from '@features/transfers/screens/TransferScreen';
import ProfileScreen from '@features/profile/screens/ProfileScreen';

const Tab = createBottomTabNavigator<TabNavigatorParamList>();
const WalletStack = createNativeStackNavigator<WalletStackParamList>();
const PaymentsStack = createNativeStackNavigator<PaymentsStackParamList>();
const TransfersStack = createNativeStackNavigator<TransfersStackParamList>();
const ProfileStack = createNativeStackNavigator<ProfileStackParamList>();

// Stack Navigators
function WalletNavigator() {
  return (
    <WalletStack.Navigator>
      <WalletStack.Screen
        name="WalletHome"
        component={WalletScreen}
        options={{ headerShown: false }}
      />
    </WalletStack.Navigator>
  );
}

function PaymentsNavigator() {
  return (
    <PaymentsStack.Navigator>
      <PaymentsStack.Screen
        name="PaymentHome"
        component={PaymentScreen}
        options={{ headerShown: false }}
      />
    </PaymentsStack.Navigator>
  );
}

function TransfersNavigator() {
  return (
    <TransfersStack.Navigator>
      <TransfersStack.Screen
        name="TransferHome"
        component={TransferScreen}
        options={{ headerShown: false }}
      />
    </TransfersStack.Navigator>
  );
}

function ProfileNavigator() {
  return (
    <ProfileStack.Navigator>
      <ProfileStack.Screen
        name="ProfileHome"
        component={ProfileScreen}
        options={{ headerShown: false }}
      />
    </ProfileStack.Navigator>
  );
}

// Bottom Tab Navigator
export default function AppNavigator() {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.platformGreen,
        tabBarInactiveTintColor: colors.gray500,
        tabBarStyle: {
          borderTopColor: colors.borderColor,
          backgroundColor: colors.platformWhite,
        },
      }}
    >
      <Tab.Screen
        name="WalletTab"
        component={WalletNavigator}
        options={{
          tabBarLabel: 'Wallet',
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 24 }}>💰</Text>,
        }}
      />
      <Tab.Screen
        name="PaymentsTab"
        component={PaymentsNavigator}
        options={{
          tabBarLabel: 'Pay',
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 24 }}>💳</Text>,
        }}
      />
      <Tab.Screen
        name="TransfersTab"
        component={TransfersNavigator}
        options={{
          tabBarLabel: 'Transfer',
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 24 }}>↔️</Text>,
        }}
      />
      <Tab.Screen
        name="ProfileTab"
        component={ProfileNavigator}
        options={{
          tabBarLabel: 'Profile',
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 24 }}>👤</Text>,
        }}
      />
    </Tab.Navigator>
  );
}

// Import Text from React Native
import { Text } from 'react-native';
