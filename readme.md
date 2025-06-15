# Vault Token Manager

A JetBrains IDE plugin that simplifies the management of HashiCorp Vault tokens generated via OpenID Connect (OIDC)
authentication.

<img class="light-icon" src="./src/main/resources/META-INF/pluginIcon.svg" alt="Vault Token Manager Icon" width="100" />

## Features

- Generate and renew Vault tokens without leaving the IDE
- View token expiration status
- Configure OIDC address and expiration settings

## Requirements

- JetBrains IDE (IntelliJ IDEA, WebStorm, PyCharm, etc.)
- HashiCorp Vault executable (executable location is configurable)
- Vault server with OIDC authentication configured

## Installation

### Build from Source

1. Clone this repository
2. Run `gradle buildPlugin`
3. Install the plugin from disk in your IDE:
    - Go to Settings/Preferences → Plugins → ⚙️ → Install Plugin from Disk...
    - Select `build/distributions/vault-token-manager-[version].zip`
4. Restart your IDE

### Configuration

1. Open the Vault Token Manager tool window (right sidebar)
2. Configure your Vault settings:
    - Vault server address
    - Token validity period
    - Login timeout
    - Path to Vault executable

## Development

This project uses:

- Kotlin
- Gradle
- JetBrains Platform SDK
- Java Swing for UI

## Contributing

Contributions are welcome! Feel free to open issues or submit pull requests.

