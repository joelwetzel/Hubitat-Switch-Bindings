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
// * Behavior tests for lockdown.groovy, if we have old locks that don't report back results reliably without a refresh.
// */
// class OldLockTests extends Specification {
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
//         validationFlags: [Flags.AllowWritingToSettings],
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

//     void "Simplified test that advances to the final state"() {
//         when: "App is triggered"
//         switchFixture.on()

//         and:
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)

//         then:
//         1 * log.debug('Lockdown: DONE')
//         lockFixture1.state.lock == 'locked'
//         lockFixture2.state.lock == 'locked'
//         lockFixture3.state.lock == 'locked'
//     }

//     void "An unresponsive lock will be skipped, but processing will continue and complete"() {
//         given:
//         lockFixture2.state.commandsToIgnore = 5

//         when: "App is triggered"
//         switchFixture.on()

//         and: "We need extra cycles, because it's going to retry the second lock 2 more times"
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)

//         then: "The second lock will be skipped, but the first and third will still be locked"
//         1 * log.debug('Lockdown: DONE')
//         lockFixture1.state.lock == 'locked'
//         lockFixture2.state.lock == 'unlocked'
//         lockFixture3.state.lock == 'locked'
//     }

//     void "If we have old locks that don't report back results without a refresh, and we don't force refreshes, lockdown will eventually finish without knowing the true states of the locks"() {
//         given:
//         lockFixture1.state.requireRefresh = true
//         lockFixture2.state.requireRefresh = true
//         lockFixture3.state.requireRefresh = true
//         appScript.forceRefresh = false

//         when: "App is triggered"
//         switchFixture.on()

//         and: "We need extra cycles, because it's going to retry all 3 locks 3 times each"
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)

//         then: "The app will finish, but the locks will still be in their initial states"
//         1 * log.debug('Lockdown: DONE')
//         lockFixture1.state.lock == 'unlocked'
//         lockFixture2.state.lock == 'unlocked'
//         lockFixture3.state.lock == 'unlocked'
//     }

//     void "If we have old locks that don't report back results without a refresh, having forceRefresh=true will ensure that lockdown will eventually finish with the true states of the locks"() {
//         given:
//         lockFixture1.state.requireRefresh = true
//         lockFixture2.state.requireRefresh = true
//         lockFixture3.state.requireRefresh = true
//         appScript.forceRefresh = true

//         when: "App is triggered"
//         switchFixture.on()

//         and: "Should only take normal amount of time, because the refreshes should complete before the next cycles."
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)
//         TimeKeeper.advanceMillis(5001)

//         then: "The app will finish, and the locks will be successfully locked."
//         1 * log.debug('Lockdown: DONE')
//         lockFixture1.state.lock == 'locked'
//         lockFixture2.state.lock == 'locked'
//         lockFixture3.state.lock == 'locked'
//     }

// }
