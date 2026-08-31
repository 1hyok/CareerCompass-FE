import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

import { inspectManifest, inspectPrivacyDefaults } from './verify-android-manifest.mjs';

const disabledFirebaseAutoInit = `
    <meta-data android:name="firebase_analytics_collection_enabled" android:value="false" />
    <meta-data android:name="firebase_messaging_auto_init_enabled" android:value="false" />`;

const manifest = ({
  permissions = '',
  components = '',
  cleartext = 'false',
  allowBackup = 'false',
  backupAttributes = '',
  metadata = disabledFirebaseAutoInit,
} = {}) => `
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  ${permissions}
  <application
      ${cleartext === null ? '' : `android:usesCleartextTraffic="${cleartext}"`}
      android:allowBackup="${allowBackup}"
      ${backupAttributes}>
    ${metadata}
    ${components}
  </application>
</manifest>`;

test('current allowed permissions and protected exported components pass', () => {
  const violations = inspectManifest(
    manifest({
      permissions: `
        <uses-permission android:name="android.permission.INTERNET" />
        <uses-permission android:name="com.example.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" />`,
      components: `
        <activity android:name="com.cambridge.careercompass_fe.MainActivity" android:exported="true" />
        <service android:name="example.SystemService" android:exported="true"
                 android:permission="android.permission.BIND_JOB_SERVICE" />
        <provider android:name="example.PrivateProvider" android:exported="false" />`,
    }),
  );

  assert.deepEqual(violations, []);
});

test('new permissions fail closed until reviewed and allowlisted', () => {
  const violations = inspectManifest(
    manifest({
      permissions: '<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />',
    }),
  );

  assert.deepEqual(violations, [
    'permission is not allowlisted: android.permission.ACCESS_FINE_LOCATION',
  ]);
});

test('cleartext traffic must stay explicitly disabled', () => {
  assert.deepEqual(inspectManifest(manifest({ cleartext: 'true' })), [
    'application must explicitly set android:usesCleartextTraffic="false"',
  ]);
  assert.deepEqual(inspectManifest(manifest({ cleartext: null })), [
    'application must explicitly set android:usesCleartextTraffic="false"',
  ]);
});

test('backup stays disabled without backup-rule references', () => {
  assert.deepEqual(
    inspectPrivacyDefaults(
      manifest({
        allowBackup: 'true',
        backupAttributes:
          'android:dataExtractionRules="@xml/data_extraction_rules" android:fullBackupContent="@xml/backup_rules"',
      }),
    ),
    [
      'application must explicitly set android:allowBackup="false"',
      'application must not declare android:dataExtractionRules when backup is disabled',
      'application must not declare android:fullBackupContent when backup is disabled',
    ],
  );
});

test('Firebase Analytics and Messaging auto-init stay explicitly disabled', () => {
  assert.deepEqual(inspectPrivacyDefaults(manifest({ metadata: '' })), [
    'firebase_analytics_collection_enabled must explicitly set android:value="false"',
    'firebase_messaging_auto_init_enabled must explicitly set android:value="false"',
  ]);
  assert.deepEqual(
    inspectPrivacyDefaults(
      manifest({
        metadata: `
          <meta-data android:name="firebase_analytics_collection_enabled" android:value="true" />
          <meta-data android:name="firebase_messaging_auto_init_enabled" android:value="false" />`,
      }),
    ),
    ['firebase_analytics_collection_enabled must explicitly set android:value="false"'],
  );
});

test('source manifest keeps privacy-sensitive defaults fail closed', async () => {
  const sourceManifest = await readFile(
    new URL('../../app/src/main/AndroidManifest.xml', import.meta.url),
    'utf8',
  );

  assert.deepEqual(inspectPrivacyDefaults(sourceManifest), []);
});

test('unprotected exports and every exported provider fail closed', () => {
  const violations = inspectManifest(
    manifest({
      components: `
        <receiver android:name="example.OpenReceiver" android:exported="true" />
        <provider android:name="example.OpenProvider" android:exported="true"
                  android:permission="example.PRIVATE" />`,
    }),
  );

  assert.deepEqual(violations, [
    'unprotected exported receiver is not allowlisted: example.OpenReceiver',
    'exported provider is forbidden: example.OpenProvider',
  ]);
});
