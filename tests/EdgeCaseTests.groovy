package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.DimmerFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
 * Tests for edge cases: level thresholds, feedback loop boundaries, off-switch level events
 */
class EdgeCaseTests extends IntegrationAppSpecification {
    def switchFixture1 = SwitchFixtureFactory.create('s1')
    def switchFixture2 = SwitchFixtureFactory.create('s2')
    def dimmerFixture1 = DimmerFixtureFactory.create('d1')
    def dimmerFixture2 = DimmerFixtureFactory.create('d2')
    def dimmerFixture3 = DimmerFixtureFactory.create('d3')

    void "Level below 5 is converted to 5 when turning on"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2, dimmerFixture3], responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 3])
        dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture3.initialize(appExecutor, [switch: "off", level: 50])

        when:
        dimmerFixture1.on()

        then:
        dimmerFixture2.currentValue('switch') == "on"
        dimmerFixture3.currentValue('switch') == "on"
        dimmerFixture2.currentValue('level') == 5  // Should be 5, not 3
        dimmerFixture3.currentValue('level') == 5  // Should be 5, not 3
    }

    void "Level of exactly 5 is not modified"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2], responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 5])
        dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])

        when:
        dimmerFixture1.on()

        then:
        dimmerFixture2.currentValue('level') == 5  // Should remain 5
    }

    void "Level of 0 is converted to 5 when turning on"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2], responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 0])
        dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])

        when:
        dimmerFixture1.on()

        then:
        dimmerFixture2.currentValue('level') == 5  // Should be 5, not 0
    }

    void "Level events from switches that are off are ignored"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2], responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "on", level: 80])

        when:
        // Simulate a level event while dimmerFixture1 is off (Zigbee driver workaround)
        dimmerFixture1.sendEvent(name: 'level', value: 30)

        then:
        dimmerFixture2.currentValue('level') == 80  // Should not change
    }

    void "Dimmer without setLevel command can still receive on/off commands"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, switchFixture1], responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])
        switchFixture1.initialize(appExecutor, [switch: "off"])

        when:
        dimmerFixture1.on()

        then:
        switchFixture1.currentValue('switch') == "on"
    }

    void "Switch without level attribute can be turned on by dimmer"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [switchFixture1, dimmerFixture1], responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])

        when:
        switchFixture1.on()

        then:
        dimmerFixture1.currentValue('switch') == "on"
        // switchFixture1 has no level, so no level should be set on dimmerFixture1
    }

    void "Level change on switch without setLevel triggers on() when level > 0"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, switchFixture1], responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])
        switchFixture1.initialize(appExecutor, [switch: "off"])

        when:
        dimmerFixture1.setLevel(80)

        then:
        switchFixture1.currentValue('switch') == "on"  // Should turn on
    }

    void "Very rapid sequential events from same device all process"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2], responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])

        when:
        dimmerFixture1.on()
        dimmerFixture1.setLevel(80)
        dimmerFixture1.setLevel(90)

        then:
        appAtomicState.controllingDeviceId == dimmerFixture1.deviceId
        dimmerFixture2.currentValue('switch') == "on"
        dimmerFixture2.currentValue('level') == 90
    }

    void "Custom response time of 1000ms allows faster device switching"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [switchFixture1, switchFixture2], responseTime: 1000, enableLogging: true])
        appScript.installed()

        and:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "off"])

        when:
        switchFixture1.on()

        then:
        switchFixture2.currentValue('switch') == "on"

        when:
        TimeKeeper.advanceSeconds(2)  // 2 seconds is > 1000ms

        and:
        switchFixture2.off()

        then:
        0 * log.debug("checkForFeedbackLoop: Preventing feedback loop")
        switchFixture1.currentValue('switch') == "off"  // Should switch control
    }

    void "Custom response time of 10000ms has longer feedback protection"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [switchFixture1, switchFixture2], responseTime: 10000, enableLogging: true])
        appScript.installed()

        and:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "off"])

        when:
        switchFixture1.on()

        then:
        switchFixture2.currentValue('switch') == "on"

        when:
        TimeKeeper.advanceSeconds(5)  // 5 seconds is < 10000ms

        and:
        switchFixture2.off()

        then:
        1 * log.debug("checkForFeedbackLoop: Preventing feedback loop")
        switchFixture1.currentValue('switch') == "on"  // Should NOT switch
    }

    void "Handling null level attribute gracefully"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [switchFixture1, switchFixture2], responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "off"])

        when:
        switchFixture1.on()

        then:
        switchFixture2.currentValue('switch') == "on"
        // No level should be propagated since switchFixture1 has no level attribute
    }
}
