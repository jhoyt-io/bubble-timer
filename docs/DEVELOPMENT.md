# Development Workflow

## Overview

This document outlines the development workflow for the Bubble Timer backend, including testing, deployment, and best practices.

## Development Environment Setup

### Prerequisites
- Node.js 18+ 
- AWS CLI configured
- AWS CDK installed globally: `npm install -g aws-cdk`

### Local Development
```bash
# Install dependencies
npm install

# Build the project
npm run build

# Run tests
npm test

# Synthesize CDK
npx cdk synth
```

## Development Workflow

### 1. Before Making Changes

#### Check Git History
```bash
# Look at the original working implementation
git show <commit-before-refactor>:lib/bubble-timer-backend-stack.websocket.ts

# Compare current with original
git diff <original-commit> HEAD lib/bubble-timer-backend-stack.websocket.ts
```

#### Understand the Interface
- **Check frontend code**: See what message formats the Android app sends
- **Review documentation**: Read `docs/WEBSOCKET.md` for message formats
- **Test locally**: Verify current behavior before making changes

### 2. During Development

#### Incremental Changes
- **Make small changes**: Test each change before proceeding
- **Preserve behavior**: Ensure new implementation matches old behavior
- **Document decisions**: Add comments explaining why patterns are used

#### Testing Requirements
```bash
# Always run tests before committing
npm run build && npm test

# Always check CDK synthesis
npx cdk synth

# Fix any new errors introduced
```

### 3. After Changes

#### Verify Functionality
- **Test user flows**: Verify timer sharing and updates work end-to-end
- **Check logs**: Monitor CloudWatch logs for new errors
- **Validate integration**: Ensure frontend and backend still work together

## Testing Strategy

### Unit Tests
- **Location**: `__tests__/` directory
- **Coverage**: All major functions and error cases
- **Mocking**: Use `jest.setup.ts` for global mocks

### Integration Tests
- **WebSocket communication**: Test message sending and receiving
- **Database operations**: Test timer persistence and sharing
- **Error scenarios**: Test connection failures and invalid messages

### Test Commands
```bash
# Run all tests
npm test

# Run specific test file
npm test -- __tests__/websocket.test.ts

# Run with coverage
npm test -- --coverage
```

## Code Quality Standards

### Error Handling
```typescript
// CORRECT: Use ErrorHandler wrapper
export const handler = ErrorHandler.wrapHandler(async (event, context) => {
    // Handler logic
}, 'HandlerName');

// CORRECT: Use ValidationMiddleware
const data = ValidationMiddleware.validateWebSocketMessage(event.body);
```

### Logging
```typescript
// CORRECT: Use structured logging with context
const timerLogger = messageLogger.child('timerMessage', {
    messageType: data.type,
    timerId: data.timerId || data.timer?.id
});

// CORRECT: Include relevant metadata
timerLogger.info('Timer processed', {
    timerId,
    messageType,
    userId
});
```

### Configuration
```typescript
// CORRECT: Use Config module
const client = Config.database;
const tableName = Config.tables.userConnections;

// CORRECT: Validate environment variables
if (!Config.tables.userConnections) {
    throw new Error('TIMERS_TABLE_NAME environment variable is required');
}
```

## Deployment Process

### Pre-Deployment Checklist
- [ ] All tests pass: `npm test`
- [ ] CDK synthesizes: `npx cdk synth`
- [ ] Code review completed
- [ ] Documentation updated

### Deployment Commands
```bash
# Deploy to beta environment
npx cdk deploy --profile beta

# Deploy to production
npx cdk deploy --profile prod
```

### Post-Deployment Monitoring
- **CloudWatch logs**: Monitor for new errors
- **User flows**: Test timer sharing and updates
- **Performance**: Check response times and error rates

## Common Development Tasks

### Adding a New Message Type
1. **Update validation**: Add to `ValidationMiddleware.validateWebSocketMessage`
2. **Add handler**: Create handler function in WebSocket stack
3. **Add tests**: Create unit tests for the new message type
4. **Update documentation**: Document the new message format

### Modifying Database Schema
1. **Update CDK**: Modify table definitions in CDK stack
2. **Update code**: Modify data access patterns
3. **Add migration**: Create migration script if needed
4. **Test thoroughly**: Verify data integrity

### Adding New Environment Variables
1. **Update Config**: Add to appropriate config file
2. **Update CDK**: Add to Lambda environment variables
3. **Update tests**: Mock the new config in tests
4. **Update documentation**: Document the new variable

## Troubleshooting

### Common Issues

#### Tests Failing
```bash
# Check if it's a config issue
npm test -- --verbose

# Check if mocks are working
cat jest.setup.ts
```

#### CDK Synthesis Failing
```bash
# Check for environment variable issues
npx cdk synth --verbose

# Check for TypeScript errors
npm run build
```

#### WebSocket Issues
- **Check message formats**: Verify against `docs/WEBSOCKET.md`
- **Check connection cleanup**: Look for stale connection errors
- **Check broadcasting**: Verify messages are sent to correct users

### Debugging Commands
```bash
# View CloudWatch logs
aws logs tail /aws/lambda/bubble-timer-backend --follow

# Check Lambda function status
aws lambda get-function --function-name bubble-timer-backend

# Test WebSocket connection
wscat -c wss://your-websocket-endpoint/prod
```

## Best Practices

### Code Organization
- **Single responsibility**: Each function should do one thing well
- **Clear naming**: Use descriptive function and variable names
- **Consistent patterns**: Follow established patterns in the codebase

### Error Handling
- **Fail fast**: Validate inputs early
- **Log errors**: Include context for debugging
- **Graceful degradation**: Handle errors without breaking the system

### Performance
- **Minimize database calls**: Batch operations when possible
- **Use connection pooling**: Reuse database connections
- **Monitor metrics**: Track performance and error rates

## Remember

- **Preserve working behavior**: Don't break existing functionality
- **Test thoroughly**: Verify changes work end-to-end
- **Document changes**: Update documentation for future developers
- **Monitor after deployment**: Watch for issues in production
