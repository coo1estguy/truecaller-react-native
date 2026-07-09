import { Button, SafeAreaView, ScrollView, Text, View, Alert, Platform } from 'react-native';
import * as Truecaller from 'truecaller-react-native';
import { useState, useEffect } from 'react';

export default function App() {
  const [result, setResult] = useState<any>(null);
  const [isUsable, setIsUsable] = useState(false);

  useEffect(() => {
    async function initTruecaller() {
      try {
        const res = await Truecaller.initializeAsync();
        setIsUsable(res.isUsable);
      } catch (e: any) {
        Alert.alert('Init Error', e.message);
      }
    }
    initTruecaller();
  }, []);

  const handleLogin = async () => {
    try {
      const res = await Truecaller.promptAuthAsync();
      setResult(res);
      
      if (Platform.OS === 'android') {
        const androidResult = res as Truecaller.TruecallerAuthResultAndroid;
        console.log("Android OAuth Code:", androidResult.authorizationCode);
      } else if (Platform.OS === 'ios') {
        const iosResult = res as Truecaller.TruecallerAuthResultIOS;
        console.log("iOS Profile Payload:", iosResult.payload);
      }
    } catch (e: any) {
      Alert.alert('Auth Error', e.message);
    }
  };

  const requestProfile = async () => {
    try {
      const profile = await Truecaller.requestProfileAsync();
      setResult(profile);
    } catch (e: any) {
      Alert.alert('Profile Error', e.message);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.container}>
        <Text style={styles.header}>TruecallerReactNative</Text>
        <Group name="Actions">
          <Text style={{ marginBottom: 10 }}>
            SDK Usable: {isUsable ? '✅ Yes' : '❌ No'}
          </Text>
          <Button 
            title="Login with Truecaller" 
            onPress={handleLogin} 
            disabled={!isUsable} 
          />
          <View style={{ height: 10 }} />
          <Button 
            title="Request Profile (iOS Only)" 
            onPress={requestProfile} 
          />
        </Group>
        <Group name="Result">
          <Text>{JSON.stringify(result, null, 2)}</Text>
        </Group>
      </ScrollView>
    </SafeAreaView>
  );
}

function Group(props: { name: string; children: React.ReactNode }) {
  return (
    <View style={styles.group}>
      <Text style={styles.groupHeader}>{props.name}</Text>
      {props.children}
    </View>
  );
}

const styles = {
  header: { fontSize: 30, margin: 20 },
  groupHeader: { fontSize: 20, marginBottom: 20 },
  group: { margin: 20, backgroundColor: '#fff', borderRadius: 10, padding: 20 },
  container: { flex: 1, backgroundColor: '#eee' },
  view: { flex: 1, height: 200 },
};
