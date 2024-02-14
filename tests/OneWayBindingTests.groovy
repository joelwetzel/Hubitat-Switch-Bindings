package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.DimmerFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
* Switch tests for one-way binding with SwitchBindingInstance.groovy
*/
class OneWayBindingTests extends IntegrationAppSpecification {
    def switchFixture1 = SwitchFixtureFactory.create('s1')
    def switchFixture2 = SwitchFixtureFactory.create('s2')
    def switchFixture3 = SwitchFixtureFactory.create('s3')
    def switches = [switchFixture1, switchFixture2, switchFixture3]

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [nameOverride: "Custom Name", switches: switches, masterSwitchId: switchFixture1.deviceId, masterOnly: true, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])
        appScript.installed()
    }


    void "Switching the master on affects the others"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "off"])
        switchFixture3.initialize(appExecutor, [switch: "off"])

        when:
        switchFixture1.on()

        then:
        appAtomicState.controllingDeviceId == switchFixture1.deviceId
        switchFixture1.currentValue('switch') == "on"
        switchFixture2.currentValue('switch') == "on"
        switchFixture3.currentValue('switch') == "on"
    }

    void "Switching the master off affects the others"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "on"])
        switchFixture2.initialize(appExecutor, [switch: "on"])
        switchFixture3.initialize(appExecutor, [switch: "on"])

        when:
        switchFixture1.off()

        then:
        appAtomicState.controllingDeviceId == switchFixture1.deviceId
        switchFixture1.currentValue('switch') == "off"
        switchFixture2.currentValue('switch') == "off"
        switchFixture3.currentValue('switch') == "off"
    }

    void "Commands on non-master devices do not cause binding"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "off"])
        switchFixture3.initialize(appExecutor, [switch: "off"])

        when:
        switchFixture2.on()

        then:
        switchFixture1.currentValue('switch') == "off"
        switchFixture2.currentValue('switch') == "on"
        switchFixture3.currentValue('switch') == "off"
    }
}
