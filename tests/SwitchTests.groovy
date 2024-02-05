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
* Switch tests for SwitchBindingInstance.groovy
*/
class SwitchTests extends Specification {
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

    def dimmerFixture1 = DimmerFixtureFactory.create('d1')

    def switches = [switchFixture1, switchFixture2, switchFixture3, dimmerFixture1]

    def appScript = sandbox.run(api: appExecutor,
        validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
        userSettingValues: [nameOverride: "Custom Name", switches: switches, masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])

    def setup() {
        appExecutor.setSubscribingScript(appScript)
        appScript.initialize()
    }

    def cleanup() {
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
        switchFixture1.state.switch == "on"
        switchFixture2.state.switch == "on"
        switchFixture3.state.switch == "on"
        dimmerFixture1.state.switch == "on"
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
        switchFixture1.state.switch == "off"
        switchFixture2.state.switch == "off"
        switchFixture3.state.switch == "off"
        dimmerFixture1.state.switch == "off"
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
        switchFixture1.state.switch == "on"
        switchFixture2.state.switch == "on"
        switchFixture3.state.switch == "on"
        dimmerFixture1.state.switch == "on"
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
        switchFixture1.state.switch == "on"
        switchFixture2.state.switch == "on"
        switchFixture3.state.switch == "on"
        dimmerFixture1.state.switch == "on"

        when:
        TimeKeeper.advanceSeconds(1)

        and:
        switchFixture2.off()

        then:
        1 * log.debug("Preventing switch feedback loop")
        appAtomicState.controllingDeviceId == switchFixture1.deviceId
        switchFixture1.state.switch == "on"
        switchFixture2.state.switch == "off"
        switchFixture3.state.switch == "on"
        dimmerFixture1.state.switch == "on"
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
        switchFixture1.state.switch == "on"
        switchFixture2.state.switch == "on"
        switchFixture3.state.switch == "on"
        dimmerFixture1.state.switch == "on"

        when:
        TimeKeeper.advanceSeconds(5)

        and:
        switchFixture2.off()

        then:
        appAtomicState.controllingDeviceId == switchFixture2.deviceId
        switchFixture1.state.switch == "off"
        switchFixture2.state.switch == "off"
        switchFixture3.state.switch == "off"
        dimmerFixture1.state.switch == "off"
    }

}
