# Minimal UI Design for GraphicalOutput

## Overview

Define simple, consistent Jetpack Compose UI for the select source feature outputs.

## Design Principles

1. **Consistency:** Match existing Home Assistant skill UI patterns
2. **Simplicity:** Text-based, no complex widgets
3. **Clarity:** Clear success/error messaging
4. **Accessibility:** Readable text sizes and contrast

## UI Components

### 1. SelectSourceSuccess

**Purpose:** Confirm successful source selection

**Layout:**
```
┌─────────────────────────────┐
│     Kitchen Radio           │  ← Headline (entity friendly name)
│                             │
│  Playing BBC Radio 2        │  ← Body (confirmation message)
└─────────────────────────────┘
```

**Implementation:**
```kotlin
data class SelectSourceSuccess(
    val entityId: String,
    val friendlyName: String,
    val sourceName: String
) : HomeAssistantOutput {
    override fun getSpeechOutput(ctx: SkillContext): String =
        ctx.getString(
            R.string.skill_home_assistant_select_source_success,
            sourceName,
            friendlyName
        )
    
    @Composable
    override fun GraphicalOutput(ctx: SkillContext) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = friendlyName,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = ctx.getString(
                    R.string.skill_home_assistant_select_source_success,
                    sourceName,
                    ""
                ).trim(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

**String Resource:**
```xml
<string name="skill_home_assistant_select_source_success">Playing %1$s on %2$s</string>
```

**Example Output:**
- Headline: "Kitchen Radio"
- Body: "Playing BBC Radio 2"

---

### 2. NoSourceList

**Purpose:** Inform user that media player doesn't support source selection

**Layout:**
```
┌─────────────────────────────┐
│     Kitchen Radio           │  ← Headline (entity friendly name)
│                             │
│  No sources available       │  ← Body (error message)
└─────────────────────────────┘
```

**Implementation:**
```kotlin
data class NoSourceList(
    val friendlyName: String
) : HomeAssistantOutput {
    override fun getSpeechOutput(ctx: SkillContext): String =
        ctx.getString(
            R.string.skill_home_assistant_no_source_list,
            friendlyName
        )
    
    @Composable
    override fun GraphicalOutput(ctx: SkillContext) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = friendlyName,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = ctx.getString(
                    R.string.skill_home_assistant_no_source_list,
                    ""
                ).trim(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

**String Resource:**
```xml
<string name="skill_home_assistant_no_source_list">%1$s does not have any available sources</string>
```

**Example Output:**
- Headline: "Kitchen Radio"
- Body: "No sources available" (in error color)

---

### 3. SourceNotFound

**Purpose:** Inform user that requested source doesn't match any available sources

**Layout:**
```
┌─────────────────────────────┐
│     Kitchen Radio           │  ← Headline (entity friendly name)
│                             │
│  Could not find             │  ← Body line 1
│  "Spotify"                  │  ← Body line 2 (requested source)
└─────────────────────────────┘
```

**Implementation:**
```kotlin
data class SourceNotFound(
    val requestedSource: String,
    val friendlyName: String
) : HomeAssistantOutput {
    override fun getSpeechOutput(ctx: SkillContext): String =
        ctx.getString(
            R.string.skill_home_assistant_source_not_found,
            requestedSource,
            friendlyName
        )
    
    @Composable
    override fun GraphicalOutput(ctx: SkillContext) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = friendlyName,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = ctx.getString(R.string.skill_home_assistant_source_not_found_short),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "\"$requestedSource\"",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

**String Resources:**
```xml
<string name="skill_home_assistant_source_not_found">Could not find source %1$s on %2$s</string>
<string name="skill_home_assistant_source_not_found_short">Could not find source</string>
```

**Example Output:**
- Headline: "Kitchen Radio"
- Body line 1: "Could not find source" (in error color)
- Body line 2: "Spotify" (in error color)

---

## Color Scheme

- **Success messages:** Default text color (MaterialTheme.colorScheme.onSurface)
- **Error messages:** Error color (MaterialTheme.colorScheme.error)
- **Headlines:** Default text color

## Typography

- **Headline:** MaterialTheme.typography.headlineMedium
- **Body:** MaterialTheme.typography.bodyLarge
- **Secondary body:** MaterialTheme.typography.bodyMedium

## Spacing

- **Padding:** 16.dp around entire content
- **Spacer:** 8.dp between headline and body

## Accessibility

- **Text size:** Uses Material3 typography scale (accessible by default)
- **Contrast:** Error messages use error color with sufficient contrast
- **Text alignment:** Center-aligned for consistency
- **Screen reader:** Text content is automatically read by TalkBack

## Alternative: Reuse HeadlineSpeechSkillOutput

For even simpler implementation, reuse the existing `HeadlineSpeechSkillOutput` helper:

```kotlin
data class SelectSourceSuccess(
    val entityId: String,
    val friendlyName: String,
    val sourceName: String
) : HomeAssistantOutput {
    override fun getSpeechOutput(ctx: SkillContext): String =
        ctx.getString(
            R.string.skill_home_assistant_select_source_success,
            sourceName,
            friendlyName
        )
    
    @Composable
    override fun GraphicalOutput(ctx: SkillContext) {
        HeadlineSpeechSkillOutput(
            headline = friendlyName,
            speechOutput = getSpeechOutput(ctx)
        )
    }
}
```

This provides a consistent, simple UI with minimal code.

## Recommendation

**Use custom Column layout** as shown above for consistency with existing `HomeAssistantOutput` implementations (GetStatusSuccess, SetStateSuccess).

This provides:
- Consistent look and feel with other Home Assistant skill outputs
- Better control over error message styling
- Clear visual hierarchy (headline → body)

## Future Enhancements (Out of Scope)

- Show list of available sources
- Interactive source selection buttons
- Media player artwork/icon
- Current playback status
- Volume controls
