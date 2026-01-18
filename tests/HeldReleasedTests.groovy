package joelwetzel.switch_bindings.tests

import me.biocomp.hubitat_ci.util.device_fixtures.DimmerFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
 * Tests for held/released button events that trigger level ramping
 */
class HeldReleasedTests extends IntegrationAppSpecification {
    def dimmerFixture1 = DimmerFixtureFactory.create('d1')
    def dimmerFixture2 = DimmerFixtureFactory.create('d2')
    def dimmerFixture3 = DimmerFixtureFactory.create('d3')

    void "When syncHeld is enabled, held event on up button triggers startLevelChange(up) on other dimmers"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2, dimmerFixture3], 
                                                       syncLevel: true, syncHeld: true, 
                                                       heldUpButtonNumber: 1, heldDownButtonNumber: 2,
                                                       responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "on", level: 50])
        dimmerFixture3.initialize(appExecutor, [switch: "on", level: 50])

        when:
        dimmerFixture1.sendEvent(name: 'held', value: '1')  // Button 1 held

        then:
        1 * dimmerFixture2.startLevelChange('up')
        1 * dimmerFixture3.startLevelChange('up')
        0 * dimmerFixture1.startLevelChange(_)  // Triggering device should not get command
    }

    void "When syncHeld is enabled, held event on down button triggers startLevelChange(down) on other dimmers"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2], 
                                                       syncLevel: true, syncHeld: true, 
                                                       heldUpButtonNumber: 1, heldDownButtonNumber: 2,
                                                       responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "on", level: 50])

        when:
        dimmerFixture1.sendEvent(name: 'held', value: '2')  // Button 2 held

        then:
        1 * dimmerFixture2.startLevelChange('down')
        0 * dimmerFixture1.startLevelChange(_)
    }

    void "When syncHeld is enabled, released event on configured button triggers stopLevelChange on other dimmers"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2, dimmerFixture3], 
                                                       syncLevel: true, syncHeld: true, 
                                                       heldUpButtonNumber: 1, heldDownButtonNumber: 2,
                                                       responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "on", level: 50])
        dimmerFixture3.initialize(appExecutor, [switch: "on", level: 50])

        when:
        dimmerFixture1.sendEvent(name: 'released', value: '1')  // Button 1 released

        then:
        1 * dimmerFixture2.stopLevelChange()
        1 * dimmerFixture3.stopLevelChange()
        0 * dimmerFixture1.stopLevelChange()
    }

    void "When syncHeld is disabled, held events are not propagated"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2], 
                                                       syncLevel: true, syncHeld: false,
                                                       responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "on", level: 50])

        when:
        dimmerFixture1.sendEvent(name: 'held', value: '1')

        then:
        0 * dimmerFixture2.startLevelChange(_)
    }

    void "When syncHeld is disabled, released events are not propagated"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2], 
                                                       syncLevel: true, syncHeld: false,
                                                       responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "on", level: 50])

        when:
        dimmerFixture1.sendEvent(name: 'released', value: '1')

        then:
        0 * dimmerFixture2.stopLevelChange()
    }

    void "Held event on unrecognized button number does not trigger level change"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2], 
                                                       syncLevel: true, syncHeld: true, 
                                                       heldUpButtonNumber: 1, heldDownButtonNumber: 2,
                                                       responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "on", level: 50])

        when:
        dimmerFixture1.sendEvent(name: 'held', value: '3')  // Button 3 not configured

        then:
        0 * dimmerFixture2.startLevelChange(_)
    }

    void "Released event on unrecognized button number does not trigger stopLevelChange"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2], 
                                                       syncLevel: true, syncHeld: true, 
                                                       heldUpButtonNumber: 1, heldDownButtonNumber: 2,
                                                       responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "on", level: 50])

        when:
        dimmerFixture1.sendEvent(name: 'released', value: '3')  // Button 3 not configured

        then:
        0 * dimmerFixture2.stopLevelChange()
    }

    void "Held events respect feedback loop prevention"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2], 
                                                       syncLevel: true, syncHeld: true, 
                                                       heldUpButtonNumber: 1, heldDownButtonNumber: 2,
                                                       responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "on", level: 50])

        when:
        dimmerFixture1.sendEvent(name: 'held', value: '1')

        then:
        1 * dimmerFixture2.startLevelChange('up')

        when:
        TimeKeeper.advanceSeconds(1)

        and:
        dimmerFixture2.sendEvent(name: 'held', value: '1')

        then:
        1 * log.debug("checkForFeedbackLoop: Preventing feedback loop")
        0 * dimmerFixture1.startLevelChange(_)  // Should be blocked by feedback loop
    }

    void "When button numbers are not configured, held events are ignored"() {
        given:
        super.initializeEnvironment(appScriptFilename: "SwitchBindingInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [switches: [dimmerFixture1, dimmerFixture2], 
                                                       syncLevel: true, syncHeld: true,
                                                       // No heldUpButtonNumber or heldDownButtonNumber set
                                                       responseTime: 5000, enableLogging: true])
        appScript.installed()

        and:
        dimmerFixture1.initialize(appExecutor, [switch: "on", level: 50])
        dimmerFixture2.initialize(appExecutor, [switch: "on", level: 50])

        when:
        dimmerFixture1.sendEvent(name: 'held', value: '1')

        then:
        0 * dimmerFixture2.startLevelChange(_)
    }
}
