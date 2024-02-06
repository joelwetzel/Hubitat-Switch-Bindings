package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.DimmerFixtureFactory
import me.biocomp.hubitat_ci.util.IntegrationAppExecutor

import me.biocomp.hubitat_ci.api.app_api.AppExecutor
import me.biocomp.hubitat_ci.api.common_api.Log
import me.biocomp.hubitat_ci.app.HubitatAppSandbox
import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper
import me.biocomp.hubitat_ci.api.common_api.InstalledAppWrapper
import me.biocomp.hubitat_ci.capabilities.GeneratedCapability
import me.biocomp.hubitat_ci.util.NullableOptional
import me.biocomp.hubitat_ci.util.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
* Switch tests for one-way binding with SwitchBindingInstance.groovy
*/
class OneWayBindingTests extends Specification {
    private HubitatAppSandbox sandbox = new HubitatAppSandbox(new File('SwitchBindingInstance.groovy'))

    def log = Mock(Log)

    InstalledAppWrapper app = Mock{
        _ * getName() >> "MyAppName"
    }

    def appState = [:]
    def appAtomicState = [:]

    def appExecutor = Spy(IntegrationAppExecutor) {
        _*getLog() >> log
        _*getApp() >> app
        _*getState() >> appState
        _*getAtomicState() >> appAtomicState
    }

    def switchFixture1 = SwitchFixtureFactory.create('s1')
    def switchFixture2 = SwitchFixtureFactory.create('s2')
    def switchFixture3 = SwitchFixtureFactory.create('s3')

    def switches = [switchFixture1, switchFixture2, switchFixture3]

    def appScript = sandbox.run(api: appExecutor,
        validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
        userSettingValues: [nameOverride: "Custom Name", switches: switches, masterSwitchId: switchFixture1.deviceId, masterOnly: true, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])

    def setup() {
        appExecutor.setSubscribingScript(appScript)
        appScript.initialize()
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
        switchFixture1.state.switch == "on"
        switchFixture2.state.switch == "on"
        switchFixture3.state.switch == "on"
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
        switchFixture1.state.switch == "off"
        switchFixture2.state.switch == "off"
        switchFixture3.state.switch == "off"
    }

    void "Commands on non-master devices do not cause binding"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "off"])
        switchFixture3.initialize(appExecutor, [switch: "off"])

        when:
        switchFixture2.on()

        then:
        switchFixture1.state.switch == "off"
        switchFixture2.state.switch == "on"
        switchFixture3.state.switch == "off"
    }
}
