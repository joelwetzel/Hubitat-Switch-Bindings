package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.DimmerFixtureFactory
import me.biocomp.hubitat_ci.util.IntegrationAppExecutor
import me.biocomp.hubitat_ci.util.IntegrationScheduler

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
* Verify the sync scheduling
*/
class ScheduleTests extends Specification {
    private HubitatAppSandbox sandbox = new HubitatAppSandbox(new File('SwitchBindingInstance.groovy'))

    def log = Mock(Log)

    InstalledAppWrapper app = Mock{
        _ * getName() >> "MyAppName"
    }

    def appState = [:]
    def appAtomicState = [:]

    def scheduler = new IntegrationScheduler()

    def appExecutor = Spy(IntegrationAppExecutor, constructorArgs: [scheduler: scheduler]) {
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
        userSettingValues: [nameOverride: "Custom Name", switches: switches, masterSwitchId: switchFixture1.deviceId, masterOnly: false, pollMaster: true, pollingInterval: 5, responseTime: 5000, enableLogging: true])

    def setup() {
        appExecutor.setSubscribingScript(appScript)
        appScript.initialize()
    }

    void "Resync happens every 5 minutes"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "off"])
        switchFixture3.initialize(appExecutor, [switch: "off"])

        when:
        appScript.updated()

        then:
        1 * appExecutor.runEvery5Minutes('reSyncFromMaster')

        when:
        TimeKeeper.advanceMinutes(6)

        then:
        1 * log.info("reSyncFromMaster()")
    }

}
