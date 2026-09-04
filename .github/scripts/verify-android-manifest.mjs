#!/usr/bin/env node

import path from 'node:path';
import process from 'node:process';
import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';

export const ALLOWED_PERMISSIONS = new Set([
  'android.permission.ACCESS_NETWORK_STATE',
  'android.permission.FOREGROUND_SERVICE',
  'android.permission.INTERNET',
  'android.permission.POST_NOTIFICATIONS',
  'android.permission.RECEIVE_BOOT_COMPLETED',
  'android.permission.USE_BIOMETRIC',
  'android.permission.USE_FINGERPRINT',
  'android.permission.WAKE_LOCK',
  'com.google.android.c2dm.permission.RECEIVE',
]);

export const ALLOWED_UNPROTECTED_EXPORTED_COMPONENTS = new Set([
  'androidx.activity.ComponentActivity',
  'androidx.compose.ui.tooling.PreviewActivity',
  'com.careercompass.careercompass_fe.MainActivity',
  'com.careercompass.careercompass_fe.debug.DebugSettingsActivity',
  'com.kakao.sdk.auth.AuthCodeHandlerActivity',
]);

export const DISABLED_FIREBASE_AUTO_INIT = new Set([
  'firebase_analytics_collection_enabled',
  'firebase_messaging_auto_init_enabled',
]);

function attributes(source) {
  return Object.fromEntries(
    [...source.matchAll(/android:([A-Za-z][A-Za-z0-9]*)\s*=\s*"([^"]*)"/g)].map((match) => [
      match[1],
      match[2],
    ]),
  );
}

export function inspectPrivacyDefaults(source) {
  const violations = [];
  const application = /<application\b([^>]*)>/s.exec(source);
  if (!application) {
    return ['application declaration is missing'];
  }

  const applicationAttributes = attributes(application[1]);
  if (applicationAttributes.allowBackup !== 'false') {
    violations.push('application must explicitly set android:allowBackup="false"');
  }
  for (const backupAttribute of ['dataExtractionRules', 'fullBackupContent']) {
    if (backupAttribute in applicationAttributes) {
      violations.push(
        `application must not declare android:${backupAttribute} when backup is disabled`,
      );
    }
  }

  const metadataValues = new Map();
  for (const match of source.matchAll(/<meta-data\b([^>]*)\/?\s*>/gs)) {
    const metadata = attributes(match[1]);
    if (!metadata.name) continue;
    const values = metadataValues.get(metadata.name) ?? [];
    values.push(metadata.value);
    metadataValues.set(metadata.name, values);
  }
  for (const name of DISABLED_FIREBASE_AUTO_INIT) {
    const values = metadataValues.get(name) ?? [];
    if (values.length === 0 || values.some((value) => value !== 'false')) {
      violations.push(`${name} must explicitly set android:value="false"`);
    }
  }

  return violations;
}

export function inspectManifest(
  source,
  {
    allowedPermissions = ALLOWED_PERMISSIONS,
    allowedUnprotectedExportedComponents = ALLOWED_UNPROTECTED_EXPORTED_COMPONENTS,
  } = {},
) {
  const violations = [];
  const application = /<application\b([^>]*)>/s.exec(source);
  if (!application) {
    violations.push('application declaration is missing');
  } else {
    const applicationAttributes = attributes(application[1]);
    if (applicationAttributes.usesCleartextTraffic !== 'false') {
      violations.push('application must explicitly set android:usesCleartextTraffic="false"');
    }
    violations.push(...inspectPrivacyDefaults(source));
  }

  for (const match of source.matchAll(/<uses-permission\b([^>]*)\/?\s*>/gs)) {
    const permission = attributes(match[1]).name;
    if (!permission) {
      violations.push('uses-permission without android:name');
      continue;
    }
    const isAndroidxDynamicReceiverPermission = permission.endsWith('.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION');
    if (!allowedPermissions.has(permission) && !isAndroidxDynamicReceiverPermission) {
      violations.push(`permission is not allowlisted: ${permission}`);
    }
  }

  for (const match of source.matchAll(/<(activity|activity-alias|service|receiver|provider)\b([^>]*)\/?\s*>/gs)) {
    const [, kind, rawAttributes] = match;
    const component = attributes(rawAttributes);
    if (component.exported !== 'true') {
      continue;
    }
    const name = component.name ?? '(missing android:name)';
    if (kind === 'provider') {
      violations.push(`exported provider is forbidden: ${name}`);
      continue;
    }
    if (!component.permission && !allowedUnprotectedExportedComponents.has(name)) {
      violations.push(`unprotected exported ${kind} is not allowlisted: ${name}`);
    }
  }

  return violations;
}

async function main() {
  const manifestPath = process.argv[2];
  if (!manifestPath || process.argv.length !== 3) {
    throw new Error('Usage: verify-android-manifest.mjs <merged-AndroidManifest.xml>');
  }

  const source = await readFile(manifestPath, 'utf8');
  const violations = inspectManifest(source);
  if (violations.length > 0) {
    throw new Error(violations.map((violation) => `Manifest policy: ${violation}`).join('\n'));
  }
  console.log(`Manifest policy: passed (${manifestPath})`);
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : '';
if (import.meta.url === invokedPath) {
  main().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
