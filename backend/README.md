# GitHub Device Flow Backend (Spring Boot)

Backend API for GitHub OAuth Device Flow authentication.

## 🏗️ Technology Stack

- **Spring Boot 3.2.0**
- **Java 17**
- **WebFlux** (for HTTP client)
- **Lombok** (for boilerplate reduction)
- **Gradle** (build tool)

## 📁 Project Structure

```
backend/
├── src/
│   └── main/
│       ├── java/com/github/deviceflow/
│       │   ├── DeviceFlowApplication.java    # Main application
│       │   ├── controller/
│       │   │   └── AuthController.java       # REST API endpoints
│       │   ├── service/
│       │   │   └── GitHubDeviceFlowService.java  # Business logic
│       │   ├── model/
│       │   │   ├── DeviceCodeResponse.java
│       │   │   ├── AccessTokenResponse.java
│       │   │   ├── GitHubUser.java
│       │   │   └── PollStatusResponse.java
│       │   └── config/
│       │       ├── GitHubOAuthConfig.java    # OAuth configuration
│       │       └── WebConfig.java            # CORS configuration
│       └── resources/
│           └── application.yml               # Application config
├── build.gradle
└── gradlew
```

## 🔑 Environment Variables

Set these before running the application:

```bash
export GITHUB_CLIENT_ID=your_github_oauth_app_client_id
export GITHUB_CLIENT_SECRET=your_github_oauth_app_client_secret
```

## 🚀 Running the Application

### Using Gradle Wrapper

```bash
./gradlew bootRun
```

### Using JAR

```bash
# Build
./gradlew build

# Run
java -jar build/libs/github-device-flow-backend-1.0.0.jar
```

The server will start on `http://localhost:8080`

## 🔌 API Endpoints

### 1. Initiate Device Flow

**POST** `/api/auth/device/code`

Starts the OAuth device flow by requesting a device code from GitHub.

**Request:**
```bash
curl -X POST http://localhost:8080/api/auth/device/code
```

**Response:**
```json
{
  "deviceCode": "3584d83530557fdd1f46af8289938c8ef79f9dc5",
  "userCode": "WDJB-MJHT",
  "verificationUri": "https://github.com/login/device",
  "expiresIn": 900,
  "interval": 5
}
```

### 2. Poll for Authorization

**GET** `/api/auth/device/poll?device_code={deviceCode}`

Checks if the user has authorized the device.

**Request:**
```bash
curl "http://localhost:8080/api/auth/device/poll?device_code=3584d83530557fdd1f46af8289938c8ef79f9dc5"
```

**Response (Pending):**
```json
{
  "status": "pending",
  "message": "Waiting for user authorization"
}
```

**Response (Authorized):**
```json
{
  "status": "authorized",
  "accessToken": "gho_xxxxxxxxxxxx",
  "user": {
    "login": "username",
    "id": 12345,
    "avatarUrl": "https://avatars.githubusercontent.com/u/12345",
    "name": "User Name",
    "email": "user@example.com",
    "bio": "Developer",
    "location": "San Francisco",
    "publicRepos": 42,
    "followers": 100,
    "following": 50,
    "createdAt": "2020-01-01T00:00:00Z"
  },
  "message": "Authorization successful"
}
```

### 3. Verify Token

**GET** `/api/auth/verify`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Request:**
```bash
curl -H "Authorization: Bearer gho_xxxxxxxxxxxx" \
     http://localhost:8080/api/auth/verify
```

**Response:**
```json
{
  "login": "username",
  "id": 12345,
  "avatarUrl": "https://avatars.githubusercontent.com/u/12345",
  "name": "User Name",
  "email": "user@example.com"
}
```

### 4. Health Check

**GET** `/api/auth/health`

```bash
curl http://localhost:8080/api/auth/health
```

**Response:**
```
OK
```

## ⚙️ Configuration

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

github:
  oauth:
    client-id: ${GITHUB_CLIENT_ID}
    client-secret: ${GITHUB_CLIENT_SECRET}
    device-code-url: https://github.com/login/device/code
    token-url: https://github.com/login/oauth/access_token
    user-api-url: https://api.github.com/user

cors:
  allowed-origins: http://localhost:3000
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS
  allowed-headers: "*"
  allow-credentials: true
```

## 🔒 Security

- **No Session Storage**: The backend is completely stateless
- **CORS Protection**: Only configured origins can access the API
- **Client Secret Protection**: Secret never leaves the backend
- **In-Memory Storage**: Device codes stored temporarily in ConcurrentHashMap
  - For production, use Redis or similar

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

## 📦 Building

```bash
# Clean and build
./gradlew clean build

# Build without tests
./gradlew build -x test
```

The JAR will be created in `build/libs/`

## 🐛 Troubleshooting

### Port already in use

```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>
```

### Environment variables not set

```bash
# Check if set
echo $GITHUB_CLIENT_ID
echo $GITHUB_CLIENT_SECRET

# Set them
export GITHUB_CLIENT_ID=your_client_id
export GITHUB_CLIENT_SECRET=your_client_secret
```

### CORS errors

Update `application.yml` to include your frontend URL:
```yaml
cors:
  allowed-origins: http://localhost:3000,https://your-frontend.com
```

## 📚 Dependencies

Key dependencies used:

- `spring-boot-starter-web` - REST API
- `spring-boot-starter-webflux` - HTTP client
- `spring-boot-starter-validation` - Request validation
- `lombok` - Reduce boilerplate
- `jackson-databind` - JSON processing

## 🔄 Device Flow Lifecycle

1. **Initiation**: Frontend requests device code
2. **Storage**: Backend stores device code temporarily
3. **Polling**: Frontend polls backend every 5 seconds
4. **Backend Polling**: Backend polls GitHub
5. **Authorization**: User authorizes on GitHub
6. **Token Exchange**: Backend receives access token
7. **User Fetch**: Backend fetches user info
8. **Cleanup**: Backend removes device code from storage
9. **Response**: Backend returns token and user to frontend

## 📝 Notes

- Device codes expire after 15 minutes
- Polling interval is 5 seconds (configurable)
- Access tokens are returned to frontend (frontend manages storage)
- Backend does not store access tokens

## 🚀 Production Deployment

### Recommendations

1. **Use Redis** for device code storage instead of in-memory
2. **Add Rate Limiting** to prevent abuse
3. **Enable HTTPS** for secure communication
4. **Configure Logging** for monitoring
5. **Set up Health Checks** for load balancers
6. **Use Environment-Specific Configs** (dev, staging, prod)

### Docker Deployment

Create `Dockerfile`:
```dockerfile
FROM openjdk:17-slim
COPY build/libs/github-device-flow-backend-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

Build and run:
```bash
docker build -t github-device-flow-backend .
docker run -p 8080:8080 \
  -e GITHUB_CLIENT_ID=xxx \
  -e GITHUB_CLIENT_SECRET=xxx \
  github-device-flow-backend
```

---

Built with ❤️ using Spring Boot

