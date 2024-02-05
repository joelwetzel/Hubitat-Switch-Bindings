// Note:  I haven't built a ColorBulbFixtureFactory yet, so I'm not able to test the color bulb functionality properly yet.
// When I get to it, the fixture will need to implement the ColorControl capability.


// package joelwetzel.switch_bindings.tests

// import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
// import me.biocomp.hubitat_ci.util.device_fixtures.DimmerFixtureFactory
// import me.biocomp.hubitat_ci.util.IntegrationAppExecutor

// import me.biocomp.hubitat_ci.api.app_api.AppExecutor
// import me.biocomp.hubitat_ci.api.common_api.Log
// import me.biocomp.hubitat_ci.app.HubitatAppSandbox
// import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper
// import me.biocomp.hubitat_ci.api.common_api.InstalledAppWrapper
// import me.biocomp.hubitat_ci.capabilities.GeneratedCapability
// import me.biocomp.hubitat_ci.util.NullableOptional
// import me.biocomp.hubitat_ci.util.TimeKeeper
// import me.biocomp.hubitat_ci.validation.Flags

// import spock.lang.Specification

// /**
// * Hue tests for SwitchBindingInstance.groovy
// */
// class HueTests extends Specification {
//     private HubitatAppSandbox sandbox = new HubitatAppSandbox(new File('SwitchBindingInstance.groovy'))

//     def log = Mock(Log)

//     InstalledAppWrapper app = Mock{
//         _ * getName() >> "MyAppName"
//     }

//     def appState = [:]
//     def appAtomicState = [:]

//     def appExecutor = Spy(IntegrationAppExecutor) {
//         _*getLog() >> log
//         _*getApp() >> app
//         _*getState() >> appState
//         _*getAtomicState() >> appAtomicState
//     }

//     def switchFixture1 = SwitchFixtureFactory.create('s1')

//     def dimmerFixture1 = DimmerFixtureFactory.create('d1')
//     def dimmerFixture2 = DimmerFixtureFactory.create('d2')
//     def dimmerFixture3 = DimmerFixtureFactory.create('d3')

//     def switches = [switchFixture1, dimmerFixture1, dimmerFixture2, dimmerFixture3]

//     def appScript = sandbox.run(api: appExecutor,
//         validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
//         userSettingValues: [nameOverride: "Custom Name", switches: switches, masterSwitchId: null, masterOnly: false, pollMaster: false, pollingInterval: 5, responseTime: 5000, enableLogging: true])

//     def setup() {
//         appExecutor.setSubscribingScript(appScript)
//         appScript.initialize()
//     }

//     def cleanup() {
//     }

//     void "setLevel on one dimmer affects the others"() {
//         given:
//         switchFixture1.initialize(appExecutor, [switch: "off"])
//         dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])
//         dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])
//         dimmerFixture3.initialize(appExecutor, [switch: "off", level: 50])

//         when:
//         dimmerFixture1.setLevel(100)

//         then:
//         appAtomicState.controllingDeviceId == dimmerFixture1.deviceId
//         switchFixture1.state.switch == "on"
//         dimmerFixture1.state.switch == "on"
//         dimmerFixture2.state.switch == "on"
//         dimmerFixture3.state.switch == "on"
//         dimmerFixture1.state.level == 100
//         dimmerFixture2.state.level == 100
//         dimmerFixture3.state.level == 100
//     }

//     void "Switching one dimmer off affects the others, but not level"() {
//         given:
//         switchFixture1.initialize(appExecutor, [switch: "on"])
//         dimmerFixture1.initialize(appExecutor, [switch: "on", level: 50])
//         dimmerFixture2.initialize(appExecutor, [switch: "on", level: 50])
//         dimmerFixture3.initialize(appExecutor, [switch: "on", level: 50])

//         when:
//         dimmerFixture2.off()

//         then:
//         appAtomicState.controllingDeviceId == dimmerFixture2.deviceId
//         switchFixture1.state.switch == "off"
//         dimmerFixture1.state.switch == "off"
//         dimmerFixture2.state.switch == "off"
//         dimmerFixture3.state.switch == "off"
//         dimmerFixture1.state.level == 50
//         dimmerFixture2.state.level == 50
//         dimmerFixture3.state.level == 50
//     }

//     void "Can handle inconsistent initial state"() {
//         given:
//         switchFixture1.initialize(appExecutor, [switch: "off"])
//         dimmerFixture1.initialize(appExecutor, [switch: "on", level: 5])
//         dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])
//         dimmerFixture3.initialize(appExecutor, [switch: "off", level: 100])

//         when:
//         dimmerFixture2.setLevel(80)

//         then:
//         appAtomicState.controllingDeviceId == dimmerFixture2.deviceId
//         switchFixture1.state.switch == "on"
//         dimmerFixture1.state.switch == "on"
//         dimmerFixture2.state.switch == "on"
//         dimmerFixture3.state.switch == "on"
//         dimmerFixture1.state.level == 80
//         dimmerFixture2.state.level == 80
//         dimmerFixture3.state.level == 80
//     }

//     void "setLevel can turn a switch on, even if the dimmer was already on"() {
//         given:
//         switchFixture1.initialize(appExecutor, [switch: "off"])
//         dimmerFixture1.initialize(appExecutor, [switch: "on", level: 5])
//         dimmerFixture2.initialize(appExecutor, [switch: "on", level: 50])
//         dimmerFixture3.initialize(appExecutor, [switch: "on", level: 100])

//         when:
//         dimmerFixture2.setLevel(80)

//         then:
//         appAtomicState.controllingDeviceId == dimmerFixture2.deviceId
//         switchFixture1.state.switch == "on"
//         dimmerFixture1.state.switch == "on"
//         dimmerFixture2.state.switch == "on"
//         dimmerFixture3.state.switch == "on"
//         dimmerFixture1.state.level == 80
//         dimmerFixture2.state.level == 80
//         dimmerFixture3.state.level == 80
//     }

//     void "There's a response time, within which we do not propagate signals from other than the controlling device"() {
//         given:
//         switchFixture1.initialize(appExecutor, [switch: "off"])
//         dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])
//         dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])
//         dimmerFixture3.initialize(appExecutor, [switch: "off", level: 50])

//         when:
//         dimmerFixture1.setLevel(80)

//         then:
//         appAtomicState.controllingDeviceId == dimmerFixture1.deviceId
//         switchFixture1.state.switch == "on"
//         dimmerFixture1.state.switch == "on"
//         dimmerFixture2.state.switch == "on"
//         dimmerFixture3.state.switch == "on"
//         dimmerFixture1.state.level == 80
//         dimmerFixture2.state.level == 80
//         dimmerFixture3.state.level == 80

//         when:
//         TimeKeeper.advanceSeconds(1)

//         and:
//         dimmerFixture2.setLevel(20)

//         then:
//         1 * log.debug("Preventing level feedback loop")
//         appAtomicState.controllingDeviceId == dimmerFixture1.deviceId
//         switchFixture1.state.switch == "on"
//         dimmerFixture1.state.switch == "on"
//         dimmerFixture2.state.switch == "on"
//         dimmerFixture3.state.switch == "on"
//         dimmerFixture1.state.level == 80
//         dimmerFixture2.state.level == 20
//         dimmerFixture3.state.level == 80
//     }

//     void "Response time applies even for mixed commands"() {
//         given:
//         switchFixture1.initialize(appExecutor, [switch: "off"])
//         dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])
//         dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])
//         dimmerFixture3.initialize(appExecutor, [switch: "off", level: 50])

//         when:
//         dimmerFixture1.setLevel(80)

//         then:
//         appAtomicState.controllingDeviceId == dimmerFixture1.deviceId
//         switchFixture1.state.switch == "on"
//         dimmerFixture1.state.switch == "on"
//         dimmerFixture2.state.switch == "on"
//         dimmerFixture3.state.switch == "on"
//         dimmerFixture1.state.level == 80
//         dimmerFixture2.state.level == 80
//         dimmerFixture3.state.level == 80

//         when:
//         TimeKeeper.advanceSeconds(1)

//         and:
//         dimmerFixture2.off()

//         then:
//         1 * log.debug("Preventing switch feedback loop")
//         appAtomicState.controllingDeviceId == dimmerFixture1.deviceId
//         switchFixture1.state.switch == "on"
//         dimmerFixture1.state.switch == "on"
//         dimmerFixture2.state.switch == "off"
//         dimmerFixture3.state.switch == "on"
//         dimmerFixture1.state.level == 80
//         dimmerFixture2.state.level == 80
//         dimmerFixture3.state.level == 80
//     }

//     void "After the responseTime, other dimmers can become the controlling device"() {
//         given:
//         switchFixture1.initialize(appExecutor, [switch: "off"])
//         dimmerFixture1.initialize(appExecutor, [switch: "off", level: 50])
//         dimmerFixture2.initialize(appExecutor, [switch: "off", level: 50])
//         dimmerFixture3.initialize(appExecutor, [switch: "off", level: 50])

//         when:
//         dimmerFixture1.setLevel(80)

//         then:
//         appAtomicState.controllingDeviceId == dimmerFixture1.deviceId
//         switchFixture1.state.switch == "on"
//         dimmerFixture1.state.switch == "on"
//         dimmerFixture2.state.switch == "on"
//         dimmerFixture3.state.switch == "on"
//         dimmerFixture1.state.level == 80
//         dimmerFixture2.state.level == 80
//         dimmerFixture3.state.level == 80

//         when:
//         TimeKeeper.advanceSeconds(5)

//         and:
//         dimmerFixture2.setLevel(20)

//         then:
//         appAtomicState.controllingDeviceId == dimmerFixture2.deviceId
//         switchFixture1.state.switch == "on"
//         dimmerFixture1.state.switch == "on"
//         dimmerFixture2.state.switch == "on"
//         dimmerFixture3.state.switch == "on"
//         dimmerFixture1.state.level == 20
//         dimmerFixture2.state.level == 20
//         dimmerFixture3.state.level == 20
//     }

// }
