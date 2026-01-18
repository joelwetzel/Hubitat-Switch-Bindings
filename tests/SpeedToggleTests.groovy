package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.FanFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
 * Tests for syncSpeed toggle feature
 */
class SpeedToggleTests extends IntegrationAppSpecification {
    def fanFixture1 = FanFixtureFactory.create('f1')
    def fanFixture2 = FanFixtureFactory.create('f2')

    void "When syncSpeed is enabled, speed changes are synced"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [fanFixture1, fanFixture2], 
                                                       syncSpeed: true, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        fanFixture1.initialize(appExecutor, [switch: "on", level: 100, speed: "high"])
        fanFixture2.initialize(appExecutor, [switch: "on", level: 100, speed: "high"])

        when:
        fanFixture1.setSpeed("low")

        then:
        fanFixture2.currentValue('speed') == "low"
    }

    void "When syncSpeed is disabled, speed changes are not synced"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [fanFixture1, fanFixture2], 
                                                       syncSpeed: false, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        fanFixture1.initialize(appExecutor, [switch: "on", level: 100, speed: "high"])
        fanFixture2.initialize(appExecutor, [switch: "on", level: 100, speed: "high"])

        when:
        fanFixture1.setSpeed("low")

        then:
        fanFixture1.currentValue('speed') == "low"
        fanFixture2.currentValue('speed') == "high"  // Should NOT sync
    }

    void "When syncSpeed is false but syncOnOff is true, on/off still works"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [fanFixture1, fanFixture2], 
                                                       syncSpeed: false, syncOnOff: true, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        fanFixture1.initialize(appExecutor, [switch: "off", level: 0, speed: "off"])
        fanFixture2.initialize(appExecutor, [switch: "off", level: 0, speed: "off"])

        when:
        fanFixture1.on()

        then:
        fanFixture2.currentValue('switch') == "on"  // Should sync on/off
        // Speed should not be synced
    }

    void "When syncSpeed is false, speed events are handled but not propagated"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [fanFixture1, fanFixture2], 
                                                       syncSpeed: false, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        fanFixture1.initialize(appExecutor, [switch: "on", level: 100, speed: "high"])
        fanFixture2.initialize(appExecutor, [switch: "on", level: 50, speed: "medium"])

        when:
        fanFixture1.setSpeed("low")

        then:
        fanFixture1.currentValue('speed') == "low"
        fanFixture1.currentValue('level') == 16
        fanFixture2.currentValue('speed') == "medium"  // Should NOT change
        fanFixture2.currentValue('level') == 50  // Should NOT change
    }
}
