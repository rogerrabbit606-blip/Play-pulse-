# Security and privacy

Thank you for helping keep PlayPulse and its users safe. Please do not report security issues publicly.

## Reporting a vulnerability

For a suspected vulnerability, contact the repository owner privately through GitHub rather than opening a public issue. Include the affected component, impact, and safe reproduction details. Do not include real user data, credentials, or exploit payloads that could harm others.

## Keep private information out of Git

Never commit:

- API keys intended to be secret, access tokens, passwords, or session cookies
- Firebase service-account keys, signing keys, keystores, or debug tokens
- Personal information, real user records, production exports, or private screenshots
- `.env` files or local machine configuration

Use local ignored files or a trusted secret manager for development values. Use GitHub Actions secrets for CI credentials, scope them narrowly, and do not print them in workflow logs. Example files should contain placeholders only, never working credentials.

Firebase Android client configuration is not a substitute for security rules. Protect data with least-privilege Firestore rules, restrict provider/API usage where possible, configure App Check appropriately, and avoid testing against production data.

## If a secret was committed

Deleting it in a new commit does not remove it from Git history, caches, forks, or existing checkouts. Revoke or rotate the exposed credential immediately, review provider access logs, and then coordinate history cleanup if appropriate. Do not post the exposed value in an issue or discussion.

## Privacy-minded development

Use synthetic test data. Minimize personal data in logs and AI prompts. Sanitize screenshots and diagnostic output before sharing. Validate access control with the Firebase emulator and rules tests before deploying changes.
