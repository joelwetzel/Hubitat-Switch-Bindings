# Switch Bindings - AI Coding Instructions

## Architecture Overview

This is a **Hubitat home automation app** written in Groovy that creates bi-directional switch bindings. The architecture follows Hubitat's parent-child app pattern:

- **[SwitchBindings.groovy](../SwitchBindings.groovy)** - Parent app container that users install once
- **[SwitchBindingInstance.groovy](../SwitchBindingInstance.groovy)** - Child app that implements each individual binding (users create multiple instances)

Each binding instance synchronizes switch/dimmer/fan state across multiple devices through event subscriptions.

## Critical Integration Details

### Hubitat Platform Patterns

This app integrates with the Hubitat platform, not a standalone application:

- Apps use `definition()` blocks with namespace `joelwetzel` for metadata
- `preferences` define the UI that users interact with on the Hubitat hub web interface
- Device interactions happen via `subscribe()` event handlers, not direct method calls
- State management uses `atomicState` for thread-safe cross-invocation persistence
- Use `log.info()` and `log.debug()` (not `println`) - logs appear in Hubitat's web UI

### Event Synchronization Logic

The binding prevents infinite event loops using timing-based guards in [SwitchBindingInstance.groovy](../SwitchBindingInstance.groovy):

- `atomicState.startInteractingMillis` tracks when a device interaction started
- `atomicState.controllingDeviceId` identifies which device triggered the current sync
- `settings.responseTime` (default 5000ms) defines the guard window
- Events from other devices are ignored during the guard period to prevent cascading updates

### Master-Only Mode

When `masterOnly` is enabled, the binding becomes one-way:
- Only the master switch's events trigger synchronization
- Other devices in the binding don't trigger events (no subscriptions)
- Optional polling with `pollMaster` periodically resyncs from master using cron: `schedule("0 */${pollingInterval} * * * ?", "reSyncFromMaster")`

## Testing Framework

Tests use a **forked version** of biocomp's hubitat_ci that mocks the Hubitat runtime environment. Key patterns:

### Test Structure (Spock Framework)

All tests extend `IntegrationAppSpecification` from `me.biocomp.hubitat_ci.util.integration`:

```groovy
class BasicTests extends IntegrationAppSpecification {
    def switchFixture1 = SwitchFixtureFactory.create('s1')

    @Override
    def setup() {
        super.initializeEnvironment(
            appScriptFilename: "SwitchBindingInstance.groovy",
            validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
            userSettingValues: [switches: switches, responseTime: 5000, enableLogging: true])
        appScript.installed()  // Initialize after setup
    }
}
```

### Device Fixtures

Use factory methods to create mock devices:
- `SwitchFixtureFactory.create('id')` - Simple switches
- `DimmerFixtureFactory.create('id')` - Dimmers with level control
- Initialize state with `fixture.initialize(appExecutor, [switch: "off", level: 50])`

### Test Execution Pattern

```groovy
void "Test description"() {
    given:  // Set up device initial states
    when:   // Trigger the action
    then:   // Assert expectations, including log call counts
}
```

## Developer Workflow

### Environment Setup

**CRITICAL**: Set the MAVEN_GITHUB_REPOSITORY endpoint before running tests:

```bash
export MAVEN_GITHUB_REPOSITORY=joelwetzel/hubitat_ci
```

This is required in [build.gradle](../build.gradle#L19) to pull the custom hubitat_ci integration testing framework.

### Running Tests

```bash
./gradlew test                    # Run all tests
./gradlew clean                   # Clean build artifacts
./gradlew test --tests ClassName  # Run specific test class
```

Test reports: [build/reports/tests/test/index.html](../build/reports/tests/test/index.html)

### Manual Installation on Hubitat

1. Copy [SwitchBindings.groovy](../SwitchBindings.groovy) → Hubitat hub "Apps Code" page
2. Copy [SwitchBindingInstance.groovy](../SwitchBindingInstance.groovy) → Hubitat hub "Apps Code" page
3. Add app via "Apps" → "+ Add User App" → "Switch Bindings"

## Project Conventions

### File Organization

- `tests/` - Test files using Spock, mirroring production code organization
- Root `.groovy` files - Production Hubitat app code (deployed to hub)
- `build/` - Generated test reports and build artifacts (gitignored)

### Synchronization Toggles

Users can selectively enable/disable synchronization via settings:
- `syncOnOff` - Switch on/off events
- `syncLevel` - Dimmer level changes
- `syncSpeed` - Fan speed control
- `syncHue`, `syncSaturation`, `syncColorTemperature` - Color bulb attributes
- `syncHeld` - Button held/released events for ramping

Check these settings in handlers before syncing: `if (settings.syncLevel) { ... }`

### Version Updates

Update version strings in **three places**:
1. [SwitchBindings.groovy](../SwitchBindings.groovy) - File header comment
2. [SwitchBindingInstance.groovy](../SwitchBindingInstance.groovy) - File header comment
3. [packageManifest.json](../packageManifest.json) - `version` and `dateReleased` fields
