package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.DimmerFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
* Level tests for SwitchBindingInstance.groovy
*/
class LevelTests extends IntegrationAppSpecification {
    def switchFixture1 = SwitchFixtureFactory.create('s1')
    def dimmerFixture1 = DimmerFixtureFactory.create('d1')
    def dimmerFixture2 = DimmerFixtureFactory.create('d2')
    def dimmerFixture3 = DimmerFixtureFactory.create('d3')
    def switches = [switchFixture1, dimmerFixture1, dimmerFixture2, dimmerFixture3]

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [nameOverride: "Custom Name", switches: switches, masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])
        appScript.installed()
    }

    void "setLevel on one dimmer affects the others"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture3.initialize(appExecutor, [switch: "off", level: 50])

        when:
        dimmerFixture1.setLevel(100)

        then:
        appAtomicState.controllingDeviceId == dimmerFixture1.deviceId
        switchFixture1.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"
        dimmerFixture2.currentValue('switch') == "on"
        dimmerFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('level') == 100
        dimmerFixture2.currentValue('level') == 100
        dimmerFixture3.currentValue('level') == 100
    }

    void "Switching one dimmer off affects the others, but not level"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "on"])
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "on", level: 50])
        dimmerFixture3.initialize(appExecutor, [switch: "on", level: 50])

        when:
        dimmerFixture2.off()

        then:
        appAtomicState.controllingDeviceId == dimmerFixture2.deviceId
        switchFixture1.currentValue('switch') == "off"
        dimmerFixture1.currentValue('switch') == "off"
        dimmerFixture2.currentValue('switch') == "off"
        dimmerFixture3.currentValue('switch') == "off"
        dimmerFixture1.currentValue('level') == 50
        dimmerFixture2.currentValue('level') == 50
        dimmerFixture3.currentValue('level') == 50
    }

    void "Can handle inconsistent initial state when setting level"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 5])
        dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture3.initialize(appExecutor, [switch: "off", level: 100])

        when:
        dimmerFixture2.setLevel(80)

        then:
        appAtomicState.controllingDeviceId == dimmerFixture2.deviceId
        switchFixture1.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"
        dimmerFixture2.currentValue('switch') == "on"
        dimmerFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('level') == 80
        dimmerFixture2.currentValue('level') == 80
        dimmerFixture3.currentValue('level') == 80
    }

    void "Can handle inconsistent initial state when turning on"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 5])
        dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture3.initialize(appExecutor, [switch: "off", level: 100])

        when:
        dimmerFixture3.on()

        then:
        appAtomicState.controllingDeviceId == dimmerFixture3.deviceId
        switchFixture1.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"
        dimmerFixture2.currentValue('switch') == "on"
        dimmerFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('level') == 100         // The level of the one that turned on should also be propagated to the other dimmers.
        dimmerFixture2.currentValue('level') == 100
        dimmerFixture3.currentValue('level') == 100
    }

    void "setLevel can turn a switch on, even if the dimmer was already on"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 5])
        dimmerFixture2.initialize(appExecutor, [switch: "on", level: 50])
        dimmerFixture3.initialize(appExecutor, [switch: "on", level: 100])

        when:
        dimmerFixture2.setLevel(80)

        then:
        appAtomicState.controllingDeviceId == dimmerFixture2.deviceId
        switchFixture1.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"
        dimmerFixture2.currentValue('switch') == "on"
        dimmerFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('level') == 80
        dimmerFixture2.currentValue('level') == 80
        dimmerFixture3.currentValue('level') == 80
    }

    void "There's a response time, within which we do not propagate signals from other than the controlling device"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture3.initialize(appExecutor, [switch: "off", level: 50])

        when:
        dimmerFixture1.setLevel(80)

        then:
        appAtomicState.controllingDeviceId == dimmerFixture1.deviceId
        switchFixture1.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"
        dimmerFixture2.currentValue('switch') == "on"
        dimmerFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('level') == 80
        dimmerFixture2.currentValue('level') == 80
        dimmerFixture3.currentValue('level') == 80

        when:
        TimeKeeper.advanceSeconds(1)

        and:
        dimmerFixture2.setLevel(20)

        then:
        1 * log.debug("checkForFeedbackLoop: Preventing feedback loop")
        appAtomicState.controllingDeviceId == dimmerFixture1.deviceId
        switchFixture1.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"
        dimmerFixture2.currentValue('switch') == "on"
        dimmerFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('level') == 80
        dimmerFixture2.currentValue('level') == 20
        dimmerFixture3.currentValue('level') == 80
    }

    void "Response time applies even for mixed commands"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture3.initialize(appExecutor, [switch: "off", level: 50])

        when:
        dimmerFixture1.setLevel(80)

        then:
        appAtomicState.controllingDeviceId == dimmerFixture1.deviceId
        switchFixture1.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"
        dimmerFixture2.currentValue('switch') == "on"
        dimmerFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('level') == 80
        dimmerFixture2.currentValue('level') == 80
        dimmerFixture3.currentValue('level') == 80

        when:
        TimeKeeper.advanceSeconds(1)

        and:
        dimmerFixture2.off()

        then:
        1 * log.debug("checkForFeedbackLoop: Preventing feedback loop")
        appAtomicState.controllingDeviceId == dimmerFixture1.deviceId
        switchFixture1.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"
        dimmerFixture2.currentValue('switch') == "off"
        dimmerFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('level') == 80
        dimmerFixture2.currentValue('level') == 80
        dimmerFixture3.currentValue('level') == 80
    }

    void "After the responseTime, other dimmers can become the controlling device"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])
        dimmerFixture3.initialize(appExecutor, [switch: "off", level: 50])

        when:
        dimmerFixture1.setLevel(80)

        then:
        appAtomicState.controllingDeviceId == dimmerFixture1.deviceId
        switchFixture1.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"
        dimmerFixture2.currentValue('switch') == "on"
        dimmerFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('level') == 80
        dimmerFixture2.currentValue('level') == 80
        dimmerFixture3.currentValue('level') == 80

        when:
        TimeKeeper.advanceSeconds(5)

        and:
        dimmerFixture2.setLevel(20)

        then:
        appAtomicState.controllingDeviceId == dimmerFixture2.deviceId
        switchFixture1.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"
        dimmerFixture2.currentValue('switch') == "on"
        dimmerFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('level') == 20
        dimmerFixture2.currentValue('level') == 20
        dimmerFixture3.currentValue('level') == 20
    }

}
