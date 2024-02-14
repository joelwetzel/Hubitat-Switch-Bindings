package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.DimmerFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
* Switch tests for SwitchBindingInstance.groovy
*/
class SwitchTests extends IntegrationAppSpecification {
    def switchFixture1 = SwitchFixtureFactory.create('s1')
    def switchFixture2 = SwitchFixtureFactory.create('s2')
    def switchFixture3 = SwitchFixtureFactory.create('s3')
    def dimmerFixture1 = DimmerFixtureFactory.create('d1')
    def switches = [switchFixture1, switchFixture2, switchFixture3, dimmerFixture1]

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [nameOverride: "Custom Name", switches: switches, masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])
        appScript.installed()
    }

    void "Switching one switch on affects the others"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "off"])
        switchFixture3.initialize(appExecutor, [switch: "off"])
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])

        when:
        switchFixture1.on()

        then:
        appAtomicState.controllingDeviceId == switchFixture1.deviceId
        switchFixture1.currentValue('switch') == "on"
        switchFixture2.currentValue('switch') == "on"
        switchFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"
    }

    void "Switching one switch off affects the others"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "on"])
        switchFixture2.initialize(appExecutor, [switch: "on"])
        switchFixture3.initialize(appExecutor, [switch: "on"])
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 50])

        when:
        switchFixture2.off()

        then:
        appAtomicState.controllingDeviceId == switchFixture2.deviceId
        switchFixture1.currentValue('switch') == "off"
        switchFixture2.currentValue('switch') == "off"
        switchFixture3.currentValue('switch') == "off"
        dimmerFixture1.currentValue('switch') == "off"
    }

    void "Can handle inconsistent initial state"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "on"])
        switchFixture3.initialize(appExecutor, [switch: "off"])
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])

        when:
        switchFixture1.on()

        then:
        appAtomicState.controllingDeviceId == switchFixture1.deviceId
        switchFixture1.currentValue('switch') == "on"
        switchFixture2.currentValue('switch') == "on"
        switchFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"
    }

    void "There's a response time, within which we do not propagate signals from other than the controlling device"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "off"])
        switchFixture3.initialize(appExecutor, [switch: "off"])
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])

        when:
        switchFixture1.on()

        then:
        appAtomicState.controllingDeviceId == switchFixture1.deviceId
        switchFixture1.currentValue('switch') == "on"
        switchFixture2.currentValue('switch') == "on"
        switchFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"

        when:
        TimeKeeper.advanceSeconds(1)

        and:
        switchFixture2.off()

        then:
        1 * log.debug("checkForFeedbackLoop: Preventing feedback loop")
        appAtomicState.controllingDeviceId == switchFixture1.deviceId
        switchFixture1.currentValue('switch') == "on"
        switchFixture2.currentValue('switch') == "off"
        switchFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"
    }

    void "The controlling device can send followup commands during the responseTime"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "off"])
        switchFixture3.initialize(appExecutor, [switch: "off"])
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])

        when:
        switchFixture1.on()

        then:
        appAtomicState.controllingDeviceId == switchFixture1.deviceId
        switchFixture1.currentValue('switch') == "on"
        switchFixture2.currentValue('switch') == "on"
        switchFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"

        when:
        TimeKeeper.advanceSeconds(1)

        and:
        switchFixture1.off()

        then:
        appAtomicState.controllingDeviceId == switchFixture1.deviceId
        switchFixture1.currentValue('switch') == "off"
        switchFixture2.currentValue('switch') == "off"
        switchFixture3.currentValue('switch') == "off"
        dimmerFixture1.currentValue('switch') == "off"
    }

    void "After the responseTime, other switches can become the controlling device"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "off"])
        switchFixture3.initialize(appExecutor, [switch: "off"])
        dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])

        when:
        switchFixture1.on()

        then:
        appAtomicState.controllingDeviceId == switchFixture1.deviceId
        switchFixture1.currentValue('switch') == "on"
        switchFixture2.currentValue('switch') == "on"
        switchFixture3.currentValue('switch') == "on"
        dimmerFixture1.currentValue('switch') == "on"

        when:
        TimeKeeper.advanceSeconds(5)

        and:
        switchFixture2.off()

        then:
        appAtomicState.controllingDeviceId == switchFixture2.deviceId
        switchFixture1.currentValue('switch') == "off"
        switchFixture2.currentValue('switch') == "off"
        switchFixture3.currentValue('switch') == "off"
        dimmerFixture1.currentValue('switch') == "off"
    }

}
