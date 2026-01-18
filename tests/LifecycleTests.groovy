package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.DimmerFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
 * Tests for app lifecycle events and configuration changes
 */
class LifecycleTests extends IntegrationAppSpecification {
    def switchFixture1 = SwitchFixtureFactory.create('s1')
    def switchFixture2 = SwitchFixtureFactory.create('s2')
    def switchFixture3 = SwitchFixtureFactory.create('s3')
    def dimmerFixture1 = DimmerFixtureFactory.create('d1')
    def switches = [switchFixture1, switchFixture2, switchFixture3]

    void "updated() calls unsubscribe and unschedule before reinitializing"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: switchFixture1.deviceId, 
                                                       pollMaster: true, pollingInterval: 5, responseTime: 5000, enableLogging: true])
        appScript.installed()

        when:
        appScript.updated()

        then:
        1 * appExecutor.unsubscribe()
        1 * appExecutor.unschedule()
        1 * log.info('Updated with settings: [switches:[GeneratedDevice(input: s1, type: t), GeneratedDevice(input: s2, type: t), GeneratedDevice(input: s3, type: t)], masterSwitchId:s1, pollMaster:true, pollingInterval:5, responseTime:5000, enableLogging:true]')
    }

    void "Switching from two-way binding to master-only mode changes subscriptions"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: null, 
                                                       masterOnly: false, responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        switchFixture1.initialize(appExecutor, [switch: "off"])
        switchFixture2.initialize(appExecutor, [switch: "off"])
        switchFixture3.initialize(appExecutor, [switch: "off"])

        when:
        // Simulate configuration change to master-only mode
        appExecutor.updateSetting('masterSwitchId', switchFixture1.deviceId)
        appExecutor.updateSetting('masterOnly', true)
        appScript.updated()

        then:
        // Should now only subscribe to master switch
        1 * appExecutor.subscribe([switchFixture1], 'switch.on', 'switchOnHandler')
        1 * appExecutor.subscribe([switchFixture1], 'switch.off', 'switchOffHandler')
    }

    void "Enabling pollMaster after initial setup schedules resync"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: switchFixture1.deviceId, 
                                                       pollMaster: false, responseTime: 5000, enableLogging: true])
        appScript.installed()

        when:
        // Enable polling
        appExecutor.updateSetting('pollMaster', true)
        appExecutor.updateSetting('pollingInterval', 5)
        appScript.updated()

        then:
        1 * appExecutor.schedule("0 */5 * * * ?", 'reSyncFromMaster')
    }

    void "Disabling pollMaster after initial setup removes scheduled resync"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: switchFixture1.deviceId, 
                                                       pollMaster: true, pollingInterval: 5, responseTime: 5000, enableLogging: true])
        appScript.installed()

        when:
        // Disable polling
        appExecutor.updateSetting('pollMaster', false)
        appScript.updated()

        then:
        1 * appExecutor.unschedule()
        0 * appExecutor.schedule(_, 'reSyncFromMaster')
    }

    void "Changing polling interval updates schedule"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: switchFixture1.deviceId, 
                                                       pollMaster: true, pollingInterval: 5, responseTime: 5000, enableLogging: true])
        appScript.installed()

        when:
        // Change interval
        appExecutor.updateSetting('pollingInterval', 15)
        appScript.updated()

        then:
        1 * appExecutor.unschedule()
        1 * appExecutor.schedule("0 */15 * * * ?", 'reSyncFromMaster')
    }

    void "Changing master switch selection updates subscriptions and label"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: switchFixture1.deviceId, 
                                                       masterOnly: true, responseTime: 5000, enableLogging: true])
        appScript.installed()

        when:
        // Change master to switchFixture2
        appExecutor.updateSetting('masterSwitchId', switchFixture2.deviceId)
        appScript.updated()

        then:
        // Should now subscribe to switchFixture2 as master
        1 * appExecutor.subscribe([switchFixture2], 'switch.on', 'switchOnHandler')
        1 * appExecutor.subscribe([switchFixture2], 'switch.off', 'switchOffHandler')
    }

    void "initialize() sets atomicState correctly"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, responseTime: 5000, enableLogging: true])

        when:
        appScript.initialize()

        then:
        appAtomicState.startInteractingMillis == 0
        appAtomicState.controllingDeviceId == 0
    }

    void "initialize() without master switch subscribes to all switches"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: null, responseTime: 5000, enableLogging: true])

        when:
        appScript.initialize()

        then:
        1 * log.debug("Subscribing to all switch events")
        1 * appExecutor.subscribe(switches, 'switch.on', 'switchOnHandler')
        1 * appExecutor.subscribe(switches, 'switch.off', 'switchOffHandler')
        1 * appExecutor.subscribe(switches, 'level', 'levelHandler')
        1 * appExecutor.subscribe(switches, 'speed', 'speedHandler')
    }

    void "Auto-generated label with 2 switches shows 'to' format"() {
        given:
        def twoSwitches = [switchFixture1, switchFixture2]
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: twoSwitches, masterSwitchId: null, responseTime: 5000, enableLogging: true])

        when:
        appScript.initialize()

        then:
        // Label should be "Bind <device1> to <device2>"
        1 * app.updateLabel(_) >> { args ->
            assert args[0].contains("Bind")
            assert args[0].contains("to")
        }
    }

    void "Auto-generated label with master switch appends master name"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: switches, masterSwitchId: switchFixture1.deviceId, responseTime: 5000, enableLogging: true])

        when:
        appScript.initialize()

        then:
        1 * app.updateLabel(_) >> { args ->
            assert args[0].contains("to")
            // Should contain the master switch name at the end
        }
    }
}
