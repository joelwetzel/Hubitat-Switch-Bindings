package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
* Basic tests for SwitchBindingInstance.groovy
*/
class BasicTests extends IntegrationAppSpecification {
    def switchFixture1 = SwitchFixtureFactory.create('s1')
    def switchFixture2 = SwitchFixtureFactory.create('s2')
    def switchFixture3 = SwitchFixtureFactory.create('s3')
    def switches = [switchFixture1, switchFixture2, switchFixture3]

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [nameOverride: "Custom Name", switches: switches, masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])
    }

    void "installed() logs the settings"() {
        when:
        appScript.installed()

        then:
        1 * log.info('Installed with settings: [nameOverride:Custom Name, switches:[GeneratedDevice(input: s1, type: t), GeneratedDevice(input: s2, type: t), GeneratedDevice(input: s3, type: t)], masterSwitchId:null, masterOnly:false, pollMaster:false, pollingInterval:5, responseTime:5000, enableLogging:true]')
    }

    void "initialize() subscribes to all events"() {
        when:
        appScript.initialize()

        then:
        1 * log.debug("Subscribing to all switch events")
        1 * appExecutor.subscribe(switches, 'switch.on', 'switchOnHandler')
        1 * appExecutor.subscribe(switches, 'switch.off', 'switchOffHandler')
        1 * appExecutor.subscribe(switches, 'level', 'levelHandler')
        1 * appExecutor.subscribe(switches, 'speed', 'speedHandler')
        1 * appExecutor.subscribe(switches, 'hue', 'hueHandler')
        1 * appExecutor.subscribe(switches, 'saturation', 'saturationHandler')
    }

    void "With pollMaster == false, we do not schedule a recurring resync"() {
        when:
        appScript.initialize()

        then:
        0 * appExecutor.schedule("0 */5 * * * ?", 'reSyncFromMaster')
    }

    void "initialize sets atomicState"() {
        when:
        appScript.initialize()

        then:
        appAtomicState.startInteractingMillis == 0
        appAtomicState.controllingDeviceId == 0
    }

}
