# Development Security Guidelines

## 🔒 Internal Security Practices

This document contains internal security guidelines for developers working on the Bubble Timer project.

## 📋 Security Infrastructure

### Automated Security Tools
- **GitHub Actions**: `.github/workflows/security-scan.yml` - Automated secret scanning
- **Gitleaks Configuration**: `.gitleaks.toml` - Custom secret detection rules
- **Cursor Rules**: `.cursor/rules/security-scanning.mdc` - Automated security scanning
- **Pre-commit Hooks**: Recommended git-secrets installation

### Security Documentation
- **Public Security Policy**: `SECURITY.md` - External vulnerability reporting
- **Internal Guidelines**: This file - Development security practices

## 🔧 Development Security Best Practices

### ✅ What's Safe to Commit
- Firebase `google-services.json` - Contains public API keys designed for client-side use
- Hardcoded API endpoints - Public AWS API Gateway URLs
- Configuration files without secrets
- Test data and mock credentials
- Documentation with example patterns

### ❌ Never Commit
- Keystore files (`*.jks`, `*.keystore`)
- Private keys (`*.pem`, `*.key`)
- Environment files (`.env`, `.env.local`)
- AWS access keys (AKIA...)
- GitHub personal access tokens (ghp_...)
- Database passwords
- API secrets
- FCM server keys

### 🔐 Credential Management

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

#### 3. Local Development
For local development, use:
- `.env.local` files (in `.gitignore`)
- Local keystore files (in `.gitignore`)
- Mock credentials for testing

### 🛡️ Security Scanning

#### Automated Scans
- **Weekly**: GitHub Actions scheduled security scans
- **On Push/PR**: Automated secret detection
- **Manual**: Trigger via GitHub Actions UI

#### Local Scanning
```bash
# Install git-secrets
brew install git-secrets

# Configure for AWS
git secrets --register-aws

# Scan before commit
git secrets --scan
```

#### Manual Security Audit
```bash
# Search for AWS access keys
grep -r "AKIA[0-9A-Z]{16}" . --exclude-dir=node_modules

# Search for GitHub tokens
grep -r "ghp_[a-zA-Z0-9]{36}" . --exclude-dir=node_modules

# Check for .env files
find . -name ".env*" -not -path "./node_modules/*"

# Check for keystore files
find . -name "*.jks" -o -name "*.keystore" -o -name "*.p12" -o -name "*.pfx"
```

### 📋 Pre-commit Checklist

Before committing code:
- [ ] No hardcoded credentials in code
- [ ] No keystore files committed
- [ ] No environment files committed
- [ ] No private keys or certificates committed
- [ ] API keys are public/client-side appropriate
- [ ] `.gitignore` properly configured
- [ ] Secrets stored in secure services (AWS Secrets Manager, etc.)
- [ ] Security scan passes locally

### 🚨 Emergency Procedures

#### If Secrets Are Exposed
1. **Immediate Actions:**
   - Rotate/revoke exposed credentials immediately
   - Remove from git history using `git filter-branch` or BFG Repo-Cleaner
   - Update any systems using the exposed credentials

2. **Prevention:**
   - Review commit history for other potential exposures
   - Implement automated secret scanning
   - Update security policies and procedures

#### Security Incident Response
1. **Assessment**: Determine scope and impact
2. **Containment**: Stop the exposure
3. **Eradication**: Remove from codebase and history
4. **Recovery**: Restore secure state
5. **Lessons Learned**: Update procedures

### 🔄 Continuous Improvement

#### Regular Reviews
- **Monthly**: Review security infrastructure
- **Quarterly**: Update security policies
- **Annually**: Comprehensive security audit

#### Security Updates
- Keep security tools updated
- Monitor for new vulnerability patterns
- Update scanning rules as needed
- Review and update allowlists

### 📞 Internal Security Contacts

For development security questions:
- Repository owner: [Your Contact Info]
- Security team: [Security Team Contact]
- DevOps team: [DevOps Contact]

---

**Note**: This document is for internal development use. For external security vulnerability reporting, see `SECURITY.md`.
