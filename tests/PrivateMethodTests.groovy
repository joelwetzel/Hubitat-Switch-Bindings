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
// * Tests of private methods for lockdown.groovy
// */
// class PrivateMethodTests extends Specification {
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
//         userSettingValues: [triggeringSwitch: switchFixture, selectedLocks: [lockFixture1, lockFixture2, lockFixture3], cycleTime: 5, maxCycles: 3, forceRefresh: true, refreshTime: 5])

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

//     void "findNextIndex returns 0 when all locks are unlocked"() {
//         when:
//         def result = appScript.findNextIndex()

//         then:
//         result == 0
//     }

//     void "findNextIndex returns -1 when all locks are locked"() {
//         given:
//         lockFixture1.lock()
//         lockFixture2.lock()
//         lockFixture3.lock()

//         when:
//         def result = appScript.findNextIndex()

//         then:
//         result == -1
//     }

//     void "findNextIndex returns 1 when first lock is locked"() {
//         given:
//         lockFixture1.lock()

//         when:
//         def result = appScript.findNextIndex()

//         then:
//         result == 1
//     }

//     void "findNextIndex returns 1 when the first lock has had too many retries"() {
//         given:
//         appAtomicState.lockMap = [3, 0, 0]

//         when:
//         def result = appScript.findNextIndex()

//         then:
//         result == 1
//     }

//     void "findNextIndex returns 2 when the first two locks have had too many retries"() {
//         given:
//         appAtomicState.lockMap = [3, 3, 0]

//         when:
//         def result = appScript.findNextIndex()

//         then:
//         result == 2
//     }
// }
