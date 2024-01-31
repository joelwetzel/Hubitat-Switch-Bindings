// package joelwetzel.lockdown.tests

// import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
// import me.biocomp.hubitat_ci.util.device_fixtures.LockFixtureFactory
// import me.biocomp.hubitat_ci.util.IntegrationAppExecutor
// import me.biocomp.hubitat_ci.util.IntegrationScheduler
// import me.biocomp.hubitat_ci.util.TimeKeeper

// import me.biocomp.hubitat_ci.api.app_api.AppExecutor
// import me.biocomp.hubitat_ci.api.common_api.Log
// import me.biocomp.hubitat_ci.app.HubitatAppSandbox
// import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper
// import me.biocomp.hubitat_ci.api.common_api.InstalledAppWrapper
// import me.biocomp.hubitat_ci.capabilities.GeneratedCapability
// import me.biocomp.hubitat_ci.util.NullableOptional
// import me.biocomp.hubitat_ci.util.TimeKeeper
// import me.biocomp.hubitat_ci.validation.Flags

// import groovy.time.*

// import spock.lang.Specification

// /**
// * Behavior tests for lockdown.groovy
// */
// class BehaviorTests extends Specification {
//     private HubitatAppSandbox sandbox = new HubitatAppSandbox(new File('lockdown.groovy'))

//     def log = Mock(Log)

//     def installedApp = Mock(InstalledAppWrapper)

//     def appState = [:]
//     def appAtomicState = [:]

//     IntegrationScheduler scheduler = new IntegrationScheduler()

//     def appExecutor = Spy(IntegrationAppExecutor, constructorArgs: [scheduler: scheduler]) {
//         _*getLog() >> log
//         _*getApp() >> installedApp
//         _*getState() >> appState
//         _*getAtomicState() >> appAtomicState
//     }

//     def switchFixture = SwitchFixtureFactory.create('s1')

//     def lockFixture1 = LockFixtureFactory.create('l1')
//     def lockFixture2 = LockFixtureFactory.create('l1')
//     def lockFixture3 = LockFixtureFactory.create('l1')

//     def appScript = sandbox.run(api: appExecutor,
//         userSettingValues: [triggeringSwitch: switchFixture, selectedLocks: [lockFixture1, lockFixture2, lockFixture3], cycleTime: 5, maxCycles: 3, forceRefresh: true, refreshTime: 2])

//     def setup() {
//         TimeZone.setDefault(TimeZone.getTimeZone('UTC'))
//         TimeKeeper.removeAllListeners()

//         switchFixture.initialize(appExecutor, [switch:"off"])
//         lockFixture1.initialize(appExecutor, [lock:"unlocked"])
//         lockFixture2.initialize(appExecutor, [lock:"unlocked"])
//         lockFixture3.initialize(appExecutor, [lock:"unlocked"])

//         appExecutor.setSubscribingScript(appScript)
//         appScript.installed()
//     }

//     def cleanup() {
//         TimeKeeper.removeAllListeners()
//     }

//     void "Triggers are logged"() {
//         when: "App is triggered"
//         switchFixture.on()

//         then:
//         1 * log.debug('Lockdown: TRIGGERED')
//     }

//     void "Triggering will immediately lock the first lock and schedule a refresh and the next cycle"() {
//         when: "App is triggered"
//         switchFixture.on()

//         then:
//         1 * log.debug("Lockdown: ATTEMPTING TO LOCK ${lockFixture1.displayName}")
//         lockFixture1.state.lock == 'locked'
//         lockFixture2.state.lock == 'unlocked'
//         lockFixture3.state.lock == 'unlocked'

//         and:
//         1 * appExecutor.runIn(2, 'refreshHandler')
//         1 * appExecutor.runIn(5, 'cycleHandler')
//     }

//     void "The refreshHandler will fire 2 seconds after the cycleHandler"() {
//         when: "App is triggered"
//         switchFixture.on()

//         then:
//         1 * appExecutor.runIn(2, 'refreshHandler')

//         when:
//         TimeKeeper.advanceMillis(2001)

//         then:
//         1 * log.debug("Lockdown: REFRESHING ${lockFixture1.displayName}")
//     }

//     void "After the cycle time, the next lock will be locked and a refresh and the next cycle will be scheduled"() {
//         when: "App is triggered"
//         switchFixture.on()

//         then: "1st cycle"
//         1 * log.debug("Lockdown: ATTEMPTING TO LOCK ${lockFixture1.displayName}")
//         lockFixture1.state.lock == 'locked'
//         lockFixture2.state.lock == 'unlocked'
//         lockFixture3.state.lock == 'unlocked'

//         when:
//         TimeKeeper.advanceMillis(5001)

//         then: "2nd cycle"
//         1 * log.debug("Lockdown: ATTEMPTING TO LOCK ${lockFixture2.displayName}")
//         lockFixture1.state.lock == 'locked'
//         lockFixture2.state.lock == 'locked'
//         lockFixture3.state.lock == 'unlocked'

//         and:
//         1 * appExecutor.runIn(2, 'refreshHandler')
//         1 * appExecutor.runIn(5, 'cycleHandler')
//     }

//     void "Two cycles will result in the third lock being locked"() {
//         when: "App is triggered"
//         switchFixture.on()

//         then: "1st cycle"
//         1 * log.debug("Lockdown: ATTEMPTING TO LOCK ${lockFixture1.displayName}")
//         lockFixture1.state.lock == 'locked'
//         lockFixture2.state.lock == 'unlocked'
//         lockFixture3.state.lock == 'unlocked'

//         when:
//         TimeKeeper.advanceMillis(5001)

//         then: "2nd cycle"
//         1 * log.debug("Lockdown: ATTEMPTING TO LOCK ${lockFixture2.displayName}")
//         lockFixture1.state.lock == 'locked'
//         lockFixture2.state.lock == 'locked'
//         lockFixture3.state.lock == 'unlocked'

//         when:
//         TimeKeeper.advanceMillis(5001)

//         then: "3rd cycle"
//         1 * log.debug("Lockdown: ATTEMPTING TO LOCK ${lockFixture3.displayName}")
//         lockFixture1.state.lock == 'locked'
//         lockFixture2.state.lock == 'locked'
//         lockFixture3.state.lock == 'locked'

//         and: "One more cycle to check if there's anything left to do"
//         1 * appExecutor.runIn(2, 'refreshHandler')
//         1 * appExecutor.runIn(5, 'cycleHandler')

//         when:
//         TimeKeeper.advanceMillis(5001)

//         then: "No more locks to lock.  Finish and reset the triggering switch."
//         1 * log.debug('Lockdown: DONE')
//         0 * appExecutor.runIn(2, 'refreshHandler')
//         0 * appExecutor.runIn(5, 'cycleHandler')
//         switchFixture.state.switch == 'off'
//     }

//     void "Can be cancelled mid-cycle"() {
//         when: "App is triggered"
//         switchFixture.on()

//         then: "1st cycle"
//         1 * log.debug("Lockdown: ATTEMPTING TO LOCK ${lockFixture1.displayName}")
//         lockFixture1.state.lock == 'locked'
//         lockFixture2.state.lock == 'unlocked'
//         lockFixture3.state.lock == 'unlocked'

//         when: "App is cancelled"
//         switchFixture.off()

//         and:
//         TimeKeeper.advanceMillis(5001)

//         then:
//         0 * log.debug("Lockdown: ATTEMPTING TO LOCK ${lockFixture2.displayName}")
//         1 * log.debug('Lockdown: CANCELLED')
//         1 * log.debug('Lockdown: DONE')

//         and: "Final lock states"
//         lockFixture1.state.lock == 'locked'
//         lockFixture2.state.lock == 'unlocked'
//         lockFixture3.state.lock == 'unlocked'
//     }
// }
