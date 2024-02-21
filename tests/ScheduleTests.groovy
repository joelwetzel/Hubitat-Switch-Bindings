package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.DimmerFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
* Verify the sync scheduling
*/
class ScheduleTests extends IntegrationAppSpecification {
    def switchFixture1 = SwitchFixtureFactory.create('s1')
    def switchFixture2 = SwitchFixtureFactory.create('s2')
    def switchFixture3 = SwitchFixtureFactory.create('s3')
    def switches = [switchFixture1, switchFixture2, switchFixture3]

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [nameOverride: "Custom Name", switches: switches, masterSwitchId: switchFixture1.deviceId, masterOnly: false, pollMaster: true, pollingInterval: 5, responseTime: 5000, enableLogging: true])
    }

    void "Resync happens every 5 minutes"() {
        given:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "off"])
        switchFixture3.initialize(appExecutor, [switch: "off"])

        when:
        appScript.installed()

        then:
        1 * appExecutor.schedule("0 */5 * * * ?", 'reSyncFromMaster')

        when:
        TimeKeeper.advanceMinutes(5)

        then:
        1 * log.info("reSyncFromMaster()")
    }

}
