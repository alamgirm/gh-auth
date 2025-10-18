# Azure Authentication Debugging Guide

## 🔍 Debugging 401 Unauthorized Error

If you're getting a 401 error when calling `/api/auth/azure/login`, follow these steps:

## Step 1: Check Backend Logs

Look for these log messages in your backend terminal:

### Expected Log Output
```
Validating Azure Entra token
Token claims - Subject: abc-123-def, Issuer: https://login.microsoftonline.com/xxx/v2.0, Audience: [your-client-id]
Configured Client ID: your-client-id
Configured Tenant ID: your-tenant-id
Validating issuer: https://login.microsoftonline.com/xxx/v2.0
Token validated successfully
Azure token stored for user: user@example.com
```

### Common Errors

#### Error 1: "AZURE_CLIENT_ID is not configured!"
```
ERROR - Invalid audience. Token audience: [abc-123], Expected: null
ERROR - AZURE_CLIENT_ID is not configured!
```

**Solution**:
```bash
export AZURE_CLIENT_ID=your_azure_client_id_from_portal
# Must match the client ID in Azure app registration!
```

#### Error 2: "Invalid audience"
```
ERROR - Invalid audience. Token audience: [abc-123-def], Expected: [xyz-456-ghi]
```

**Solution**: The token's audience doesn't match your configured client ID.

**Check**:
1. Frontend MSAL is using correct client ID
2. Backend config has correct client ID
3. Both must match!

```bash
# These MUST be the SAME:
export AZURE_CLIENT_ID=abc-123-def
export NUXT_PUBLIC_AZURE_CLIENT_ID=abc-123-def
```

#### Error 3: "Invalid issuer"
```
ERROR - Invalid issuer: https://login.microsoftonline.com/12345/v2.0
```

**Solution**: Tenant ID mismatch.

**Check backend**:
```bash
export AZURE_TENANT_ID=12345-tenant-id-from-token
# OR for multi-tenant:
export AZURE_TENANT_ID=common
```

#### Error 4: JWKS URI error
```
ERROR - Error initializing JWT processor
```

**Solution**: JWKS URI is wrong or unreachable.

**Fix**: Use the tenant-specific JWKS URI:
```bash
# If your tenant ID is 12345-67890
export AZURE_TENANT_ID=12345-67890

# JWKS URI will be auto-configured as:
# https://login.microsoftonline.com/12345-67890/discovery/v2.0/keys
```

## Step 2: Verify Configuration

### Check Backend Configuration

Start your backend and look for startup logs:

```bash
cd backend
./gradlew bootRun

# Look for:
Azure Entra Config:
  Client ID: your-client-id  ← Should NOT be empty
  Tenant ID: your-tenant-id or 'common'
  Authority: https://login.microsoftonline.com/xxx
```

### Check Frontend Token

In your browser console, you should see the access token. Decode it:

1. Copy the access token from console
2. Go to https://jwt.ms (Microsoft's JWT decoder)
3. Paste the token
4. Check these fields:
   ```
   aud: Should match your AZURE_CLIENT_ID
   iss: Should match https://login.microsoftonline.com/TENANT_ID/v2.0
   sub: User's Azure object ID
   ```

## Step 3: Common Fixes

### Fix 1: Client ID Mismatch

**Frontend uses one client ID, backend expects another!**

```bash
# Verify they match:
echo $AZURE_CLIENT_ID                    # Backend
echo $NUXT_PUBLIC_AZURE_CLIENT_ID       # Frontend

# Should print the SAME value!

# If different, set them:
export AZURE_CLIENT_ID=your_client_id
export NUXT_PUBLIC_AZURE_CLIENT_ID=your_client_id
```

### Fix 2: Tenant ID Not Set

```bash
# Check if tenant ID is set:
echo $AZURE_TENANT_ID

# If empty, set it:
# Get from Azure Portal → Azure AD → Overview → Tenant ID
export AZURE_TENANT_ID=your-tenant-id

# OR for multi-tenant:
export AZURE_TENANT_ID=common
```

### Fix 3: Extract Tenant from Token

If you have the access token, decode it and find the tenant:

```javascript
// In browser console after successful MSAL login:
const token = "your-access-token"
const payload = JSON.parse(atob(token.split('.')[1]))
console.log('Issuer:', payload.iss)
console.log('Audience:', payload.aud)
console.log('Tenant:', payload.tid)

// Use the tenant ID from payload.tid or extract from payload.iss
```

The tenant ID is in the `iss` (issuer) field:
```
https://login.microsoftonline.com/TENANT_ID_HERE/v2.0
                                    ^^^^^^^^^^^^^^^^
```

## Step 4: Quick Test Configuration

Use this minimal configuration for testing:

### Backend `.env`
```bash
# Use the values from your Azure app registration
AZURE_CLIENT_ID=a1b2c3d4-e5f6-7890-abcd-ef1234567890
AZURE_TENANT_ID=common

# The app will auto-generate:
# AZURE_AUTHORITY=https://login.microsoftonline.com/common
# AZURE_JWKS_URI=https://login.microsoftonline.com/common/discovery/v2.0/keys
```

### Frontend `.env`
```bash
# MUST match backend AZURE_CLIENT_ID exactly!
NUXT_PUBLIC_AZURE_CLIENT_ID=a1b2c3d4-e5f6-7890-abcd-ef1234567890
NUXT_PUBLIC_AZURE_AUTHORITY=https://login.microsoftonline.com/common
```

## Step 5: Test with Detailed Logging

### Enable Debug Logging

Update `backend/src/main/resources/application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.github.deviceflow: DEBUG
    com.nimbusds: DEBUG  # ← Add this for JWT debugging
```

Restart backend and try login again. You'll see detailed JWT validation logs.

## Step 6: Decode Your Token

Add this to frontend after MSAL login success:

```typescript
// In useAzureAuth.ts, after getting response
console.log('Access Token:', response.accessToken)

// Decode JWT (in browser console)
const parts = response.accessToken.split('.')
const payload = JSON.parse(atob(parts[1]))
console.log('Decoded Token:', payload)
console.log('Audience (aud):', payload.aud)
console.log('Issuer (iss):', payload.iss)
console.log('Tenant (tid):', payload.tid)
```

Then compare:
- `payload.aud` should equal `AZURE_CLIENT_ID` (backend)
- `payload.iss` should match expected issuer format
- `payload.tid` is your actual tenant ID

## 🔧 Most Likely Issues

### Issue 1: Client ID Not Set (90% of cases)

```bash
# Check if set:
echo $AZURE_CLIENT_ID

# If empty:
export AZURE_CLIENT_ID=your_client_id_from_azure_portal

# Restart backend
cd backend && ./gradlew bootRun
```

### Issue 2: Client ID Mismatch (5% of cases)

```bash
# Frontend and backend must use SAME client ID
export AZURE_CLIENT_ID=abc-123
export NUXT_PUBLIC_AZURE_CLIENT_ID=abc-123  # ← Must match!

# Restart both servers
```

### Issue 3: Wrong Tenant ID (3% of cases)

```bash
# Decode your token to find real tenant ID
# Then set:
export AZURE_TENANT_ID=your_real_tenant_id

# Restart backend
```

### Issue 4: JWKS URI Unreachable (2% of cases)

```bash
# Test if JWKS URI is accessible:
curl https://login.microsoftonline.com/common/discovery/v2.0/keys

# Should return JSON with keys
# If not, check your network/firewall
```

## 🎯 Quick Fix Checklist

Run through this checklist:

```bash
# 1. Check environment variables are set
echo "Client ID: $AZURE_CLIENT_ID"
echo "Tenant ID: $AZURE_TENANT_ID"
echo "Frontend Client ID: $NUXT_PUBLIC_AZURE_CLIENT_ID"

# 2. Verify they're correct (from Azure Portal)
# Go to portal.azure.com → Your app → Overview
# Compare Application (client) ID

# 3. Restart backend with correct values
cd backend
export AZURE_CLIENT_ID=your_correct_value
export AZURE_TENANT_ID=common
./gradlew bootRun

# 4. Restart frontend
cd frontend  
export NUXT_PUBLIC_AZURE_CLIENT_ID=your_correct_value
npm run dev

# 5. Try login again
open http://localhost:3000
# Click "Login with Microsoft"
# Check backend logs carefully
```

## 📊 Debugging Output

After you try to login, share these logs:

### From Backend Terminal
```
Look for lines containing:
- "Validating Azure Entra token"
- "Token claims - Subject:"
- "Configured Client ID:"
- "Invalid audience" or "Token validated successfully"
```

### From Frontend Console
```javascript
// Decode and log the token:
const token = "paste-token-here"
const payload = JSON.parse(atob(token.split('.')[1]))
console.log(payload)

// Share:
// - payload.aud (audience)
// - payload.iss (issuer)
// - payload.tid (tenant ID)
```

## 🚨 Emergency: Skip Validation (Testing Only)

**ONLY for debugging**, you can temporarily disable audience validation:

```java
// In AzureTokenValidationService.java
// Comment out audience check:

// if (!claims.getAudience().contains(config.getClientId())) {
//     log.error("Invalid audience...");
//     return null;
// }

log.warn("AUDIENCE VALIDATION DISABLED - FOR TESTING ONLY!");
```

This will help identify if the issue is with audience validation.

**Remember to re-enable it after debugging!**

## 📝 Next Steps

1. **Check backend logs** - Look for the exact error
2. **Verify client IDs match** - Frontend and backend
3. **Decode the token** - Use jwt.ms to see what's inside
4. **Set tenant ID correctly** - From token's `iss` or `tid` field
5. **Share the logs** - We can debug together

---

**Most likely fix**: Set `AZURE_CLIENT_ID` environment variable and restart backend!

