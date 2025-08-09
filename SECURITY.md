# Security Policy

## 🚨 Reporting Security Vulnerabilities

**We take security seriously.** If you discover a security vulnerability, please follow these guidelines:

### 🔒 **Private Disclosure**
- **DO NOT** create a public GitHub issue for security vulnerabilities
- **DO** email security reports to: [security@yourdomain.com]
- **DO** include detailed information about the vulnerability
- **DO** allow us time to investigate and fix before public disclosure

### 📧 **What to Include in Security Reports**
- Description of the vulnerability
- Steps to reproduce
- Potential impact assessment
- Suggested fix (if any)
- Your contact information for follow-up

### ⏱️ **Response Timeline**
- **Initial response**: Within 48 hours
- **Status update**: Within 1 week
- **Resolution**: As quickly as possible, typically 2-4 weeks

### 🏆 **Recognition**
Security researchers who responsibly disclose vulnerabilities will be:
- Listed in our security acknowledgments
- Given credit in security advisories
- Potentially eligible for our security bounty program

---

## 🔒 Development Security Guidelines

### ✅ What's Safe to Commit
- Firebase `google-services.json` - Contains public API keys designed for client-side use
- Hardcoded API endpoints - Public AWS API Gateway URLs
- Configuration files without secrets
- Test data and mock credentials

### ❌ Never Commit
- Keystore files (`*.jks`, `*.keystore`)
- Private keys (`*.pem`, `*.key`)
- Environment files (`.env`, `.env.local`)
- AWS access keys (AKIA...)
- GitHub personal access tokens (ghp_...)
- Database passwords
- API secrets
- FCM server keys

### 🔧 Security Best Practices

#### 1. Environment Variables
Use environment variables for sensitive configuration:
```bash
# Example .env file (DO NOT COMMIT)
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
DATABASE_URL=your_database_url
```

#### 2. AWS Secrets Manager
Store sensitive data in AWS Secrets Manager:
```bash
aws secretsmanager create-secret \
  --name my-app-secret \
  --description "Application secret" \
  --secret-string "your-secret-value"
```

#### 3. GitHub Secret Scanning
Enable GitHub secret scanning in repository settings to automatically detect exposed secrets.

#### 4. Pre-commit Hooks
Consider implementing pre-commit hooks to scan for secrets before committing:
```bash
# Install git-secrets
brew install git-secrets

# Configure for AWS
git secrets --register-aws
```

### 📋 Security Checklist

Before pushing to public repositories:
- [ ] No hardcoded credentials in code
- [ ] No keystore files committed
- [ ] No environment files committed
- [ ] No private keys or certificates committed
- [ ] API keys are public/client-side appropriate
- [ ] `.gitignore` properly configured
- [ ] Secrets stored in secure services (AWS Secrets Manager, etc.)

### 🚨 If Secrets Are Exposed

1. **Immediate Actions:**
   - Rotate/revoke exposed credentials immediately
   - Remove from git history using `git filter-branch` or BFG Repo-Cleaner
   - Update any systems using the exposed credentials

2. **Prevention:**
   - Review commit history for other potential exposures
   - Implement automated secret scanning
   - Update security policies and procedures

### 📞 Security Contacts

**For security vulnerabilities:**
- Email: [security@yourdomain.com]
- **DO NOT** create public GitHub issues for security issues

**For development security questions:**
- Repository owner: [Your Contact Info]
- Security team: [Security Team Contact]

---

## 🔗 Related Security Resources

- **Automated Security Scanning**: See `.github/workflows/security-scan.yml`
- **Security Rules**: See `.cursor/rules/security-scanning.mdc`
- **Security Configuration**: See `.gitleaks.toml`

**Remember:** When in doubt, err on the side of caution. It's better to be overly careful with security than to expose sensitive data.
