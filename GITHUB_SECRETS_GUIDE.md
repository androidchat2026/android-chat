# GitHub Actions Secrets Setup

After creating the GitHub repository, add these two secrets:
Go to: Your repo → Settings → Secrets and variables → Actions → New repository secret

## Secret 1: FIREBASE_PROJECT_ID
Name:  FIREBASE_PROJECT_ID
Value: [paste your Firebase project ID here — e.g. android-chat-2026]

## Secret 2: FIREBASE_SERVICE_ACCOUNT_KEY
Name:  FIREBASE_SERVICE_ACCOUNT_KEY
Value: [paste the full JSON content of your service account key]

To get the service account key:
1. Firebase Console → Project Settings → Service Accounts
2. Click "Generate new private key"
3. Open the downloaded JSON file
4. Copy ALL the content and paste as the secret value

Once both secrets are added, the cleanup cron runs automatically every day at 02:00 UTC.
You can also trigger it manually: Actions tab → Firebase Daily Cleanup → Run workflow
