package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.DimmerFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.FanFixtureFactory
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
* Speed tests for SwitchBindingInstance.groovy
*/
class SpeedTests extends Specification {
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
    def dimmerFixture1 = DimmerFixtureFactory.create('d1')
    def fanFixture1 = FanFixtureFactory.create('f1')
    def fanFixture2 = FanFixtureFactory.create('f2')

    void "Two bound fans turn on"() {
        given:
            def appScript = sandbox.run(api: appExecutor,
                validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                userSettingValues: [nameOverride: "Custom Name", switches: [fanFixture1, fanFixture2], masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])
            appExecutor.setSubscribingScript(appScript)
            appScript.initialize()

        and:
            fanFixture1.initialize(appExecutor, [switch: "off", level: 0, speed: "off"])
            fanFixture2.initialize(appExecutor, [switch: "off", level: 0, speed: "off"])

        when:
            fanFixture1.on()

        then:
            fanFixture1.state.switch == "on"
            fanFixture2.state.switch == "on"
            fanFixture1.state.level == 100
            fanFixture2.state.level == 100
            fanFixture1.state.speed == "high"
            fanFixture2.state.speed == "high"
    }

    void "Two bound fans change speed"() {
        given:
            def appScript = sandbox.run(api: appExecutor,
                validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                userSettingValues: [nameOverride: "Custom Name", switches: [fanFixture1, fanFixture2], masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])
            appExecutor.setSubscribingScript(appScript)
            appScript.initialize()

        and:
            fanFixture1.initialize(appExecutor, [switch: "on", level: 100, speed: "high"])
            fanFixture2.initialize(appExecutor, [switch: "on", level: 100, speed: "high"])

        when:
            fanFixture1.setSpeed("low")

        then:
            fanFixture1.state.switch == "on"
            fanFixture1.state.speed == "low"
            fanFixture1.state.level == 16
            fanFixture2.state.switch == "on"
            fanFixture2.state.speed == "low"
            fanFixture2.state.level == 16
    }

    void "A bound switch can turn a fan on"() {
        given:
            def appScript = sandbox.run(api: appExecutor,
                validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                userSettingValues: [nameOverride: "Custom Name", switches: [fanFixture1, switchFixture1], masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])
            appExecutor.setSubscribingScript(appScript)
            appScript.initialize()

        and:
            fanFixture1.initialize(appExecutor, [switch: "off", level: 0, speed: "off"])
            switchFixture1.initialize(appExecutor, [switch: "off"])

        when:
            switchFixture1.on()

        then:
            fanFixture1.state.switch == "on"
            fanFixture1.state.level == 100
            fanFixture1.state.speed == "high"
    }

    void "A bound fan can turn a switch off"() {
        given:
            def appScript = sandbox.run(api: appExecutor,
                validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                userSettingValues: [nameOverride: "Custom Name", switches: [fanFixture1, switchFixture1], masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])
            appExecutor.setSubscribingScript(appScript)
            appScript.initialize()

        and:
            fanFixture1.initialize(appExecutor, [switch: "on", level: 100, speed: "high"])
            switchFixture1.initialize(appExecutor, [switch: "on"])

        when:
            fanFixture1.off()

        then:
            switchFixture1.state.switch == "off"
    }
}
