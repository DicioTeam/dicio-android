# Dicio Setup Guide

This guide provides detailed setup instructions for skills that require additional configuration.

## Table of Contents

- [Home Assistant Skill](#home-assistant-skill)

---

## Home Assistant Skill

The Home Assistant skill allows you to control and query your Home Assistant entities using voice commands.

### Prerequisites

- A running Home Assistant instance (local network or remote)
- Access to the Home Assistant web interface
- Home Assistant version 2021.1 or later (for REST API support)

### Step 1: Get Your Long-Lived Access Token

1. Open your Home Assistant web interface (e.g., `http://192.168.1.100:8123`)
2. Click on your **profile** icon in the bottom left corner
3. Scroll down to the **"Long-Lived Access Tokens"** section
4. Click **"Create Token"**
5. Give it a name (e.g., "Dicio Voice Assistant")
6. **Copy the token immediately** - you won't be able to see it again!
7. Store it securely

### Step 2: Find Your Entity IDs

You need to know the entity IDs of the devices you want to control:

1. In Home Assistant, go to **Developer Tools** (wrench icon in sidebar)
2. Click on the **"States"** tab
3. Browse or search for your entities
4. Note down the **Entity ID** (e.g., `light.living_room`, `switch.coffee_maker`, `person.john`)

Common entity ID patterns:
- Lights: `light.kitchen`, `light.bedroom_lamp`
- Switches: `switch.fan`, `switch.tv`
- Covers: `cover.garage_door`, `cover.blinds`
- Locks: `lock.front_door`
- Persons: `person.mark`, `person.sarah`

### Step 3: Configure Dicio

1. Open **Dicio** on your Android device
2. Go to **Settings** (gear icon)
3. Tap on **Skills**
4. Find and tap on **Home Assistant**
5. Enter your configuration:
   - **Base URL**: Your Home Assistant URL (e.g., `http://192.168.1.100:8123`)
     - Use `http://` for local network
     - Use `https://` for remote access (recommended for security)
   - **Access Token**: Paste the Long-Lived Access Token you created

### Step 4: Add Entity Mappings

Entity mappings connect the friendly names you say to the actual Home Assistant entity IDs.

1. In the Home Assistant skill settings, tap **"Add Entity Mapping"**
2. For each device you want to control:
   - **Friendly Name**: What you'll say (e.g., "living room light", "Mark")
   - **Entity ID**: The Home Assistant entity ID (e.g., `light.living_room`, `person.mark`)

#### Example Mappings:

| Friendly Name | Entity ID | Example Command |
|--------------|-----------|-----------------|
| living room light | light.living_room | "Turn living room light on" |
| kitchen light | light.kitchen | "Turn the kitchen light off" |
| bedroom lamp | light.bedroom_lamp | "Switch bedroom lamp on" |
| garage door | cover.garage_door | "Get status of garage door" |
| front door | lock.front_door | "Check front door" |
| Mark | person.mark | "Where is the person Mark" |
| Sarah | person.sarah | "What is Sarah location" |

### Step 5: Test Your Setup

Try these example commands:

**Controlling Lights:**
- "Turn living room light on"
- "Turn the kitchen light off"
- "Switch bedroom lamp on"

**Checking Status:**
- "Get status of garage door"
- "Check front door"
- "What is living room light"

**Person Location:**
- "Where is the person Mark"
- "What is Sarah location"

### Supported Commands

#### Turn Entities On/Off/Toggle

**Patterns:**
- `turn [the] <entity> on/off/toggle`
- `switch [the] <entity> on/off/toggle`

**Examples:**
- "Turn outside lights off"
- "Turn the kitchen light on"
- "Switch bedroom lamp off"
- "Turn office light toggle"

#### Check Entity Status

**Patterns:**
- `get status of <entity>`
- `what is [the] status for <entity>`
- `check [the] <entity>`
- `get [the] <entity>`

**Examples:**
- "Get status of living room light"
- "What is the status for garage door"
- "Check downstairs hallway lights"
- "Get front door"

#### Person Location

**Patterns:**
- `where is the person <name>`
- `what is <name> location`

**Examples:**
- "Where is the person Mark"
- "What is Sarah location"

### Supported Entity Types

The skill works with any Home Assistant entity that supports the following services:

- **Lights** (`light.*`): turn_on, turn_off, toggle
- **Switches** (`switch.*`): turn_on, turn_off, toggle
- **Covers** (`cover.*`): open_cover, close_cover, toggle
- **Locks** (`lock.*`): lock, unlock
- **Fans** (`fan.*`): turn_on, turn_off, toggle
- **Media Players** (`media_player.*`): turn_on, turn_off, toggle
- **Persons** (`person.*`): Get location/zone information
- **Any other entity**: Get status

### Troubleshooting

#### "Connection Failed"

**Possible causes:**
- Home Assistant is not running
- Wrong Base URL
- Network connectivity issues
- Firewall blocking the connection

**Solutions:**
1. Verify Home Assistant is accessible by opening the URL in a browser
2. Check that you're on the same network (for local URLs)
3. Try using the IP address instead of hostname
4. Ensure port 8123 is not blocked

#### "Authentication Failed"

**Possible causes:**
- Invalid or expired access token
- Token was copied incorrectly

**Solutions:**
1. Create a new Long-Lived Access Token
2. Make sure you copied the entire token (no spaces or line breaks)
3. Delete and re-enter the token in Dicio settings

#### "Entity Not Mapped"

**Possible causes:**
- No entity mapping exists for the spoken name
- Spoken name doesn't match any friendly name

**Solutions:**
1. Add an entity mapping for the device
2. Try using the exact friendly name you configured
3. Check for typos in your entity mappings

#### "Entity Not Found"

**Possible causes:**
- Entity ID doesn't exist in Home Assistant
- Entity was deleted or renamed

**Solutions:**
1. Verify the entity ID exists in Home Assistant (Developer Tools > States)
2. Update the entity mapping with the correct entity ID
3. Check for typos in the entity ID

#### "Invalid Action"

**Possible causes:**
- The entity doesn't support the requested action
- Wrong domain for the action

**Solutions:**
1. Check that the entity supports the action (e.g., locks don't support "toggle")
2. Use appropriate commands for the entity type
3. Verify the entity is working in Home Assistant

### Security Considerations

- **Use HTTPS**: When accessing Home Assistant remotely, always use HTTPS to encrypt your access token
- **Token Storage**: Dicio stores your access token securely on your device
- **Network Security**: For local access, ensure your home network is secure
- **Token Rotation**: Periodically create new access tokens and revoke old ones
- **Limited Scope**: Consider creating a separate Home Assistant user with limited permissions for Dicio

### Advanced Configuration

#### Using Home Assistant Cloud (Nabu Casa)

If you use Home Assistant Cloud, you can use your remote URL:

- **Base URL**: `https://your-instance.ui.nabu.casa`
- **Access Token**: Your Long-Lived Access Token

#### Using DuckDNS or Other Dynamic DNS

If you have a dynamic DNS setup:

- **Base URL**: `https://your-domain.duckdns.org:8123`
- Ensure your router forwards port 8123
- Use HTTPS with a valid SSL certificate

#### Custom Port

If Home Assistant runs on a custom port:

- **Base URL**: `http://192.168.1.100:8124` (replace 8124 with your port)

### Privacy

The Home Assistant skill:
- Processes all commands **on-device** (speech recognition)
- Only sends API requests to **your Home Assistant instance**
- Does **not** send any data to third-party servers
- Stores your access token **locally** on your device

### Getting Help

If you encounter issues:

1. Check the [Home Assistant REST API documentation](https://developers.home-assistant.io/docs/api/rest/)
2. Open an issue on [GitHub](https://github.com/Stypox/dicio-android/issues)
3. Join the [Matrix room](https://matrix.to/#/#dicio:matrix.org) for community support

---

*Last updated: December 2024*
