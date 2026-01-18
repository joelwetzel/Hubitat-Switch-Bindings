package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.DimmerFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
 * Tests for toggle feature settings (syncOnOff, syncLevel, etc.)
 */
class ToggleFeaturesTests extends IntegrationAppSpecification {
    def switchFixture1 = SwitchFixtureFactory.create('s1')
    def switchFixture2 = SwitchFixtureFactory.create('s2')
    def dimmerFixture1 = DimmerFixtureFactory.create('d1')
    def dimmerFixture2 = DimmerFixtureFactory.create('d2')

    void "When syncOnOff is false, switch on/off events are not synced"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [switchFixture1, switchFixture2], syncOnOff: false, syncLevel: true, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "off"])

        when:
        switchFixture1.on()

        then:
        switchFixture1.currentValue('switch') == "on"
        switchFixture2.currentValue('switch') == "off"  // Should NOT sync
    }

    void "When syncOnOff is false, switch off events are not synced"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [switchFixture1, switchFixture2], syncOnOff: false, syncLevel: true, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        switchFixture1.initialize(appExecutor, [switch: "on"])
        switchFixture2.initialize(appExecutor, [switch: "on"])

        when:
        switchFixture1.off()

        then:
        switchFixture1.currentValue('switch') == "off"
        switchFixture2.currentValue('switch') == "on"  // Should NOT sync
    }

    void "When syncLevel is false, level changes are not synced"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2], syncOnOff: true, syncLevel: false, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "on", level: 50])

        when:
        dimmerFixture1.setLevel(80)

        then:
        dimmerFixture1.currentValue('level') == 80
        dimmerFixture2.currentValue('level') == 50  // Should NOT sync
    }

    void "When syncLevel is false but syncOnOff is true, on/off still works"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2], syncOnOff: true, syncLevel: false, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])

        when:
        dimmerFixture1.on()

        then:
        dimmerFixture1.currentValue('switch') == "on"
        dimmerFixture2.currentValue('switch') == "on"  // Should sync
        dimmerFixture1.currentValue('level') == 50
        dimmerFixture2.currentValue('level') == 50  // Should NOT sync level
    }

    void "When syncOnOff is false, turning on dimmer with setLevel does not sync on state"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2], syncOnOff: false, syncLevel: true, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])

        when:
        dimmerFixture1.setLevel(80)

        then:
        dimmerFixture1.currentValue('switch') == "on"
        dimmerFixture1.currentValue('level') == 80
        dimmerFixture2.currentValue('switch') == "off"  // Should NOT sync on
        dimmerFixture2.currentValue('level') == 80  // Should sync level
    }

    void "When all sync features are false, nothing is synced"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2], syncOnOff: false, syncLevel: false, syncSpeed: false, syncHue: false, syncSaturation: false, syncColorTemperature: false, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])

        when:
        dimmerFixture1.on()

        then:
        dimmerFixture1.currentValue('switch') == "on"
        dimmerFixture2.currentValue('switch') == "off"  // Should NOT sync

        when:
        dimmerFixture1.setLevel(80)

        then:
        dimmerFixture1.currentValue('level') == 80
        dimmerFixture2.currentValue('level') == 50  // Should NOT sync
    }
}
