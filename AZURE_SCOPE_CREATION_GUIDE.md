# Azure: Create API Scope - Step-by-Step Guide

## 🎯 Goal

Create an `access_as_user` scope in your **Backend API** app registration so the frontend can request tokens for it.

## 📋 Prerequisites

- You should have already created a Backend API app registration in Azure Portal
- You need the Application (client) ID of your backend app

## 🚀 Step-by-Step Instructions

### Step 1: Open Your Backend App Registration

```
1. Go to: https://portal.azure.com
2. Search for: "App registrations" (in the top search bar)
3. Click: "App registrations" service
4. Find and click: Your backend app (e.g., "MyApp-Backend-API")
```

---

### Step 2: Navigate to "Expose an API"

```
1. In the left menu, find and click: "Expose an API"
2. You'll see a page with sections:
   - Application ID URI
   - Scopes defined by this API
   - Authorized client applications
```

---

### Step 3: Set Application ID URI

```
1. Look for "Application ID URI" section at the top
2. Click the "Add" or "Set" button next to it

You'll see a text box with a suggested URI:
   api://your-backend-client-id

Examples:
   api://12345678-1234-1234-1234-123456789012
   OR custom: api://myapp-backend

3. Keep the default OR change to something custom
4. Click "Save"

✅ You should now see:
   Application ID URI: api://12345678-1234-1234-1234-123456789012
```

---

### Step 4: Add a Scope

```
1. Look for "Scopes defined by this API" section
2. Click: "+ Add a scope" button
3. A panel opens on the right side
```

---

### Step 5: Fill in Scope Details

In the panel that opened, fill in:

```
Scope name:
   access_as_user

Who can consent:
   ○ Admins only
   ● Admins and users  ← Select this

Admin consent display name:
   Access the backend API as a user

Admin consent description:
   Allows the application to access the backend API on behalf of the signed-in user

User consent display name:
   Access the backend on your behalf

User consent description:
   Allows the app to access the backend API using your identity

State:
   ● Enabled  ← Make sure this is selected
```

### Step 6: Save the Scope

```
1. Scroll down in the panel
2. Click: "Add scope" button (bottom of panel)

✅ You should now see in the scopes list:
   api://your-backend-client-id/access_as_user
   Status: Enabled
```

---

### Step 7: Copy the Full Scope Value

```
1. In the "Scopes defined by this API" section
2. You should see your scope listed

It will look like:
   api://12345678-1234-1234-1234-123456789012/access_as_user

3. COPY THIS ENTIRE VALUE
4. This is what you'll use as: NUXT_PUBLIC_AZURE_API_SCOPE
```

---

## ✅ Verification

After creating the scope, verify:

```
✓ Application ID URI is set: api://your-backend-id
✓ Scope is created: access_as_user
✓ Scope status: Enabled
✓ Full scope value: api://your-backend-id/access_as_user
```

---

## 🔧 Configure Your Application

Now that you have the scope, set your environment variables:

### Backend

```bash
# Use the BACKEND app's client ID
export BE_CLIENT_ID=12345678-1234-1234-1234-123456789012
export BE_TENANT_ID=your-tenant-id-here
```

### Frontend

```bash
# Use the FRONTEND app's client ID (you'll create this next)
export NUXT_PUBLIC_AZURE_CLIENT_ID=your-frontend-client-id
export NUXT_PUBLIC_AZURE_AUTHORITY=https://login.microsoftonline.com/your-tenant-id

# Use the FULL scope you just copied
export NUXT_PUBLIC_AZURE_API_SCOPE=api://12345678-1234-1234-1234-123456789012/access_as_user
```

---

## 📸 Visual Checklist

When you're done, your "Expose an API" page should look like:

```
┌─────────────────────────────────────────────────────────┐
│ Expose an API                                            │
├─────────────────────────────────────────────────────────┤
│                                                          │
│ Application ID URI                                       │
│ api://12345678-1234-1234-1234-123456789012      [Edit]  │
│                                                          │
├─────────────────────────────────────────────────────────┤
│                                                          │
│ Scopes defined by this API              [+ Add a scope] │
│                                                          │
│ ┌──────────────────────────────────────────────────┐   │
│ │ Scope name: access_as_user                       │   │
│ │ Full scope: api://12345.../access_as_user        │   │
│ │ Status: ● Enabled                                │   │
│ │ Who can consent: Admins and users                │   │
│ └──────────────────────────────────────────────────┘   │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## ⚠️ Common Mistakes

### Mistake 1: Using Wrong Client ID

```
❌ WRONG: Using FRONTEND client ID in the scope
   api://frontend-client-id/access_as_user

✅ CORRECT: Using BACKEND client ID in the scope
   api://backend-client-id/access_as_user
```

### Mistake 2: Typo in Scope Name

```
❌ WRONG: access_user, accessAsUser, access-as-user

✅ CORRECT: access_as_user (exactly)
```

### Mistake 3: Not Setting State to Enabled

```
Make sure: State = ● Enabled
```

---

## 🔄 Next: Create Frontend App Registration

After creating the backend scope, you need to create the frontend app:

See [AZURE_BACKEND_API_SETUP.md](AZURE_BACKEND_API_SETUP.md) Part 2 for:
- Creating Frontend SPA app registration
- Adding API permissions
- Granting consent

---

## 💡 Quick Reference

### What You Need to Copy

From Backend API app:
1. ✅ **Application (client) ID** → `BE_CLIENT_ID`
2. ✅ **Directory (tenant) ID** → `BE_TENANT_ID`  
3. ✅ **Full scope value** → `NUXT_PUBLIC_AZURE_API_SCOPE`

### Scope Format

```
api://[BACKEND_CLIENT_ID]/access_as_user
      ^^^^^^^^^^^^^^^^^^^
      This is your backend app's client ID
```

---

## 🆘 If You Get Stuck

### Can't find "Expose an API"?

- Make sure you're in the correct app registration (backend app)
- Look in the left menu under "Manage" section
- It's between "Authentication" and "App roles"

### "Add scope" button disabled?

- You need to set Application ID URI first
- Click "Set" or "Add" next to "Application ID URI"
- Save it, then "Add a scope" will be enabled

### Scope not appearing?

- Make sure you clicked "Add scope" at the bottom of the panel
- Check that State is "Enabled"
- Refresh the page

---

**Need help?** Share a screenshot of your "Expose an API" page and I can guide you!

**Ready?** After creating the scope, proceed to create the Frontend app and configure permissions!

