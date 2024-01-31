package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.IntegrationAppExecutor

import me.biocomp.hubitat_ci.api.app_api.AppExecutor
import me.biocomp.hubitat_ci.api.common_api.Log
import me.biocomp.hubitat_ci.app.HubitatAppSandbox
import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper
import me.biocomp.hubitat_ci.api.common_api.InstalledAppWrapper
import me.biocomp.hubitat_ci.capabilities.GeneratedCapability
import me.biocomp.hubitat_ci.util.NullableOptional
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
* Basic tests for SwitchBindingInstance.groovy
*/
class BasicTests extends Specification {
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
        userSettingValues: [nameOverride: "Custom Name", switches: switches, masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])

    def setup() {
        appExecutor.setSubscribingScript(appScript)
    }

    void "installed() logs the settings"() {
        when:
        // Run installed() method on app script.
        appScript.installed()

        then:
        // Expect that log.info() was called with this string
        1 * log.info('Installed with settings: [nameOverride:Custom Name, switches:[GeneratedDevice(input: s1, type: t), GeneratedDevice(input: s2, type: t), GeneratedDevice(input: s3, type: t)], masterSwitchId:null, masterOnly:false, pollMaster:false, pollingInterval:5, responseTime:5000, enableLogging:true]')
    }

    void "initialize() subscribes to all events"() {
        when:
        appScript.initialize()

        then:
        // Expect that all events are subscribe to
        1 * log.debug("Subscribing to all switch events")
        1 * appExecutor.subscribe(switches, 'switch.on', 'switchOnHandler')
        1 * appExecutor.subscribe(switches, 'switch.off', 'switchOffHandler')
        1 * appExecutor.subscribe(switches, 'level', 'levelHandler')
        1 * appExecutor.subscribe(switches, 'speed', 'speedHandler')
        1 * appExecutor.subscribe(switches, 'hue', 'hueHandler')
    }

    void "If masterOnly is set, only subscribe to events on the master switch"() {
        given:
        appScript = sandbox.run(api: appExecutor,
        validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
        userSettingValues: [nameOverride: "Custom Name", switches: switches, masterSwitchId: switchFixture1.deviceId, masterOnly: true, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])

        when:
        appScript.initialize()

        then:
        1 * log.debug("Subscribing only to master switch events")
        1 * appExecutor.subscribe(switchFixture1, 'switch.on', 'switchOnHandler')
        1 * appExecutor.subscribe(switchFixture1, 'switch.off', 'switchOffHandler')
        1 * appExecutor.subscribe(switchFixture1, 'level', 'levelHandler')
        1 * appExecutor.subscribe(switchFixture1, 'speed', 'speedHandler')
        1 * appExecutor.subscribe(switchFixture1, 'hue', 'hueHandler')
    }

    void "If pollMaster is set, schedule a recurring resync"() {
        given:
        appScript = sandbox.run(api: appExecutor,
        validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
        userSettingValues: [nameOverride: "Custom Name", switches: switches, masterSwitchId: switchFixture1.deviceId, masterOnly: true, pollMaster: true, pollingInterval: 5, responseTime: 5000, enableLogging: true])

        when:
        appScript.initialize()

        then:
        1 * appExecutor.runEvery5Minutes('reSyncFromMaster')
    }

    void "With pollMaster == false, we do not schedule a recurring resync"() {
        when:
        appScript.initialize()

        then:
        0 * appExecutor.runEvery5Minutes('reSyncFromMaster')
    }

    void "initialize sets atomicState"() {
        when:
        appScript.initialize()

        then:
        appAtomicState.startInteractingMillis == 0
        appAtomicState.controllingDeviceId == 0
    }

}
