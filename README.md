# Bongo Android App

A modern Android TV application built with Android Leanback library.

## Features

- Android TV optimized UI
- Profile picture support with CircleImageView
- Material Design components
- Automated CI/CD with GitHub Actions

## GitHub Actions Workflows

This project includes several GitHub Actions workflows:

### 1. Android CI (`android-ci.yml`)
- Runs on push to `main`/`develop` and pull requests
- Executes unit tests and lint checks
- Builds debug APK
- Builds signed release APK (on main branch only)

### 2. Code Quality (`code-quality.yml`)
- Runs Detekt static analysis
- Performs lint checks
- Uploads reports as artifacts

### 3. Release (`release.yml`)
- Triggers on version tags (e.g., `v1.0.0`)
- Builds signed APK and AAB
- Creates GitHub release with assets

## Setup for GitHub Actions

### Required Secrets

Add these secrets to your GitHub repository:

1. **KEYSTORE_BASE64**: Base64 encoded keystore file
   \`\`\`bash
   base64 -i your-keystore.jks | pbcopy
   \`\`\`

2. **SIGNING_KEY_ALIAS**: Your keystore alias
3. **SIGNING_KEY_PASSWORD**: Your key password
4. **SIGNING_STORE_PASSWORD**: Your keystore password

### Creating a Keystore

\`\`\`bash
keytool -genkey -v -keystore bongo-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias bongo
\`\`\`

## Building

### Debug Build
\`\`\`bash
./gradlew assembleDebug
\`\`\`

### Release Build
\`\`\`bash
./gradlew assembleRelease
\`\`\`

### Running Tests
\`\`\`bash
./gradlew test
\`\`\`

### Code Quality Checks
\`\`\`bash
./gradlew detekt
./gradlew lintDebug
\`\`\`

## Dependencies

- AndroidX AppCompat
- Material Design Components
- CircleImageView for profile pictures
- AndroidX Leanback for TV UI
- Detekt for static analysis
- JUnit & Espresso for testing

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and quality checks
5. Submit a pull request

## License

[Add your license information here]
