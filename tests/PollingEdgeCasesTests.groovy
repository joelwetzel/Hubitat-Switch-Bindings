package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.DimmerFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
 * Tests for reSyncFromMaster edge cases and polling scenarios
 */
class PollingEdgeCasesTests extends IntegrationAppSpecification {
    def switchFixture1 = SwitchFixtureFactory.create('s1')
    def switchFixture2 = SwitchFixtureFactory.create('s2')
    def switchFixture3 = SwitchFixtureFactory.create('s3')
    def dimmerFixture1 = DimmerFixtureFactory.create('d1')
    def switches = [switchFixture1, switchFixture2, switchFixture3]

    void "reSyncFromMaster does nothing when masterSwitchId is null"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: null, pollMaster: false, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        switchFixture1.initialize(appExecutor, [switch: "on"])
        switchFixture2.initialize(appExecutor, [switch: "off"])
        switchFixture3.initialize(appExecutor, [switch: "off"])

        when:
        appScript.reSyncFromMaster(null)

        then:
        1 * log.debug("reSyncFromMaster: Master Switch not set")
        switchFixture2.currentValue('switch') == "off"  // Should not change
        switchFixture3.currentValue('switch') == "off"  // Should not change
    }

    void "reSyncFromMaster syncs all devices to master switch state when master is on"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: switchFixture1.deviceId, pollMaster: true, pollingInterval: 5, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        switchFixture1.initialize(appExecutor, [switch: "on"])
        switchFixture2.initialize(appExecutor, [switch: "off"])
        switchFixture3.initialize(appExecutor, [switch: "off"])

        and:
        // Advance time to ensure we're outside the interaction window
        TimeKeeper.advanceMinutes(2)

        when:
        appScript.reSyncFromMaster(null)

        then:
        switchFixture2.currentValue('switch') == "on"
        switchFixture3.currentValue('switch') == "on"
    }

    void "reSyncFromMaster syncs all devices to master switch state when master is off"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: switchFixture1.deviceId, pollMaster: true, pollingInterval: 5, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "on"])
        switchFixture3.initialize(appExecutor, [switch: "on"])

        and:
        // Advance time to ensure we're outside the interaction window
        TimeKeeper.advanceMinutes(2)

        when:
        appScript.reSyncFromMaster(null)

        then:
        switchFixture2.currentValue('switch') == "off"
        switchFixture3.currentValue('switch') == "off"
    }

    void "reSyncFromMaster skips sync when there has been recent user interaction"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: switchFixture1.deviceId, pollMaster: true, pollingInterval: 5, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        switchFixture1.initialize(appExecutor, [switch: "on"])
        switchFixture2.initialize(appExecutor, [switch: "off"])
        switchFixture3.initialize(appExecutor, [switch: "off"])

        when:
        switchFixture1.on()  // This creates a recent interaction

        and:
        TimeKeeper.advanceSeconds(30)  // Advance 30 seconds - less than 60 seconds

        and:
        appScript.reSyncFromMaster(null)

        then:
        1 * log.debug("reSyncFromMaster: Skipping reSync because there has been a recent user interaction.")
        // State should not change beyond the initial on() call
    }

    void "reSyncFromMaster works after interaction timeout has passed"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: switchFixture1.deviceId, pollMaster: true, pollingInterval: 5, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        switchFixture1.initialize(appExecutor, [switch: "on"])
        switchFixture2.initialize(appExecutor, [switch: "on"])
        switchFixture3.initialize(appExecutor, [switch: "on"])

        when:
        switchFixture2.off()  // Create interaction

        and:
        switchFixture1.off()  // Change master state

        and:
        TimeKeeper.advanceMinutes(2)  // Advance 2 minutes - more than 60 seconds

        and:
        appScript.reSyncFromMaster(null)

        then:
        // All should sync to master's off state
        switchFixture2.currentValue('switch') == "off"
        switchFixture3.currentValue('switch') == "off"
    }

    void "Different polling intervals schedule correctly"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: switchFixture1.deviceId, pollMaster: true, pollingInterval: 10, responseTime: 5000, enableLogging: true])

        when:
        appScript.installed()

        then:
        1 * appExecutor.schedule("0 */10 * * * ?", 'reSyncFromMaster')
    }

    void "Polling interval of 1 minute schedules correctly"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: switchFixture1.deviceId, pollMaster: true, pollingInterval: 1, responseTime: 5000, enableLogging: true])

        when:
        appScript.installed()

        then:
        1 * appExecutor.schedule("0 */1 * * * ?", 'reSyncFromMaster')
    }

    void "Polling interval of 30 minutes schedules correctly"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: switchFixture1.deviceId, pollMaster: true, pollingInterval: 30, responseTime: 5000, enableLogging: true])

        when:
        appScript.installed()

        then:
        1 * appExecutor.schedule("0 */30 * * * ?", 'reSyncFromMaster')
    }

    void "reSyncFromMaster syncs dimmer level from master"() {
        given:
        def dimmerSwitches = [dimmerFixture1, switchFixture2, switchFixture3]
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: dimmerSwitches, masterSwitchId: dimmerFixture1.deviceId, pollMaster: true, pollingInterval: 5, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 75])
        switchFixture2.initialize(appExecutor, [switch: "off"])
        switchFixture3.initialize(appExecutor, [switch: "off"])

        and:
        TimeKeeper.advanceMinutes(2)

        when:
        appScript.reSyncFromMaster(null)

        then:
        switchFixture2.currentValue('switch') == "on"
        switchFixture3.currentValue('switch') == "on"
    }
}
