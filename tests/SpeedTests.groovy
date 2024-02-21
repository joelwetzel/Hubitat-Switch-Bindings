package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.DimmerFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.FanFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
* Speed tests for SwitchBindingInstance.groovy
*/
class SpeedTests extends IntegrationAppSpecification {
    def switchFixture1 = SwitchFixtureFactory.create('s1')
    def dimmerFixture1 = DimmerFixtureFactory.create('d1')
    def fanFixture1 = FanFixtureFactory.create('f1')
    def fanFixture2 = FanFixtureFactory.create('f2')

    void "Two bound fans turn on"() {
        given:
            super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                        validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                        userSettingValues: [nameOverride: "Custom Name", switches: [fanFixture1, fanFixture2], masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])
            appScript.initialize()

        and:
            fanFixture1.initialize(appExecutor, [switch: "off", level: 0, speed: "off"])
            fanFixture2.initialize(appExecutor, [switch: "off", level: 0, speed: "off"])

        when:
            fanFixture1.on()

        then:
            fanFixture1.currentValue('switch') == "on"
            fanFixture2.currentValue('switch') == "on"
            fanFixture1.currentValue('level') == 100
            fanFixture2.currentValue('level') == 100
            fanFixture1.currentValue('speed') == "high"
            fanFixture2.currentValue('speed') == "high"
    }

    void "Two bound fans change speed"() {
        given:
            super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                        validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                        userSettingValues: [nameOverride: "Custom Name", switches: [fanFixture1, fanFixture2], masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])
            appScript.initialize()

        and:
            fanFixture1.initialize(appExecutor, [switch: "on", level: 100, speed: "high"])
            fanFixture2.initialize(appExecutor, [switch: "on", level: 100, speed: "high"])

        when:
            fanFixture1.setSpeed("low")

        then:
            fanFixture1.currentValue('switch') == "on"
            fanFixture1.currentValue('speed') == "low"
            fanFixture1.currentValue('level') == 16
            fanFixture2.currentValue('switch') == "on"
            fanFixture2.currentValue('speed') == "low"
            fanFixture2.currentValue('level') == 16
    }

    void "A bound switch can turn a fan on"() {
        given:
            super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                        validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                        userSettingValues: [nameOverride: "Custom Name", switches: [fanFixture1, switchFixture1], masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])
            appScript.initialize()

        and:
            fanFixture1.initialize(appExecutor, [switch: "off", level: 0, speed: "off"])
            switchFixture1.initialize(appExecutor, [switch: "off"])

        when:
            switchFixture1.on()

        then:
            fanFixture1.currentValue('switch') == "on"
            fanFixture1.currentValue('level') == 100
            fanFixture1.currentValue('speed') == "high"
    }

    void "A bound fan can turn a switch off"() {
        given:
            super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                        validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                        userSettingValues: [nameOverride: "Custom Name", switches: [fanFixture1, switchFixture1], masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])
            appScript.initialize()

        and:
            fanFixture1.initialize(appExecutor, [switch: "on", level: 100, speed: "high"])
            switchFixture1.initialize(appExecutor, [switch: "on"])

        when:
            fanFixture1.off()

        then:
            switchFixture1.currentValue('switch') == "off"
    }

    void "A bound dimmer can adjust the speed of a fan"() {
        given:
            super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                        validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                        userSettingValues: [nameOverride: "Custom Name", switches: [fanFixture1, dimmerFixture1], masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])
            appScript.initialize()

        and:
            fanFixture1.initialize(appExecutor, [switch: "off", level: 0, speed: "off"])
            dimmerFixture1.initialize(appExecutor, [switch: "off", level: 0])

        when:
            dimmerFixture1.setLevel(100)

        then:
            fanFixture1.currentValue('switch') == "on"
            fanFixture1.currentValue('level') == 100
            fanFixture1.currentValue('speed') == "high"
    }

    void "A bound fan can adjust the level of a dimmer"() {
        given:
            super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                        validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                        userSettingValues: [nameOverride: "Custom Name", switches: [fanFixture1, dimmerFixture1], masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])
            appScript.initialize()

        and:
            fanFixture1.initialize(appExecutor, [switch: "off", level: 0, speed: "off"])
            dimmerFixture1.initialize(appExecutor, [switch: "off", level: 0])

        when:
            fanFixture1.setSpeed("medium")

        then:
            dimmerFixture1.currentValue('switch') == "on"
            dimmerFixture1.currentValue('level') == 50
    }
}
