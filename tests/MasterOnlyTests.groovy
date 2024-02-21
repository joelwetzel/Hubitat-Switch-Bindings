package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
* Basic tests for SwitchBindingInstance.groovy
*/
class MasterOnlyTests extends IntegrationAppSpecification {
    def switchFixture1 = SwitchFixtureFactory.create('s1')
    def switchFixture2 = SwitchFixtureFactory.create('s2')
    def switchFixture3 = SwitchFixtureFactory.create('s3')
    def switches = [switchFixture1, switchFixture2, switchFixture3]

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [nameOverride: "Custom Name", switches: switches, masterSwitchId: switchFixture1.deviceId, masterOnly: true, pollMaster: true, pollingInterval: 5, responseTime: 5000, enableLogging: true])
    }

    void "If masterOnly is set, only subscribe to events on the master switch"() {
        when:
        appScript.initialize()

        then:
        1 * log.debug("Subscribing only to master switch events")
        1 * appExecutor.subscribe([switchFixture1], 'switch.on', 'switchOnHandler')
        1 * appExecutor.subscribe([switchFixture1], 'switch.off', 'switchOffHandler')
        1 * appExecutor.subscribe([switchFixture1], 'level', 'levelHandler')
        1 * appExecutor.subscribe([switchFixture1], 'speed', 'speedHandler')
        1 * appExecutor.subscribe([switchFixture1], 'hue', 'hueHandler')
        1 * appExecutor.subscribe([switchFixture1], 'saturation', 'saturationHandler')
    }

    void "If pollMaster is set, schedule a recurring resync"() {
        when:
        appScript.initialize()

        then:
        1 * appExecutor.schedule("0 */5 * * * ?", 'reSyncFromMaster')
    }
}
