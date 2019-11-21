/**
 *  Switch Binding Instance
 *
 *  Copyright 2019 Joel Wetzel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

import groovy.json.*
	
definition(
	parent: "joelwetzel:Switch Bindings",
    name: "Switch Binding Instance",
    namespace: "joelwetzel",
    author: "Joel Wetzel",
    description: "Child app that is instantiated by the Switch Bindings app.",
    category: "Convenience",
	iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")


def switches = [
		name:				"switches",
		type:				"capability.switch",
		title:				"Switches to Bind",
		description:		"Select the switches to bind.",
		multiple:			true,
		required:			true,
		submitOnChange: 	true
	]


def masterSwitch = [
		name:				"masterSwitch",
		type:				"capability.switch",
		title:				"Master Switch",
		multiple:			false,
		required:			false,
		submitOnChange: 	true
	]

def pollMaster = [
		name:				'pollMaster',
		type:				'bool',
		title:				'Poll master and synchronize all the devices',
		defaultValue:		false,
		required:			true
	]

def nameOverride = [
		name:				"nameOverride",
		type:				"string",
		title:				"Binding Name",
		multiple:			false,
		required:			false
	]

def responseTime = [
		name:				"responseTime",
		type:				"long",
		title:				"Estimated Switch Response Time (in milliseconds)",
		defaultValue:		5000,
		required:			true
	]


def enableLogging = [
		name:				"enableLogging",
		type:				"bool",
		title:				"Enable Debug Logging?",
		defaultValue:		false,
		required:			true
	]


preferences {
	page(name: "mainPage", title: "", install: true, uninstall: true) {
		section(getFormat("title", "Switch Bindings Instance")) {
		}
		section("") {
			input switches
		}
		section ("<b>Advanced Settings</b>", hideable: true, hidden: false) {
			paragraph "<br/><b>WARNING:</b> Only adjust Estimated Switch Response Time if you know what you are doing!  Some dimmers don't report their new status until after they have slowly dimmed.  The app uses this estimated duration to make sure that the two bound switches don't infinitely trigger each other.  Only reduce this value if you are using two very fast switches, and you regularly physically toggle both of them right after each other.  (Not a common case!)"
			input responseTime
			paragraph "<br/><b>OPTIONAL:</b> Set a master switch.  (It should be one of the switches you selected above). If set, only changes to this device will be reflected to the other devices."
			input masterSwitch
			if ( !(settings?.switches?.find { it?.deviceId == settings?.masterSwitch?.deviceId })) {
				paragraph "ERROR: ${masterSwitch.displayName} is not one of the selected switches! Please try again"
				settings?.masterSwitch == null
				app.clearSetting('masterSwitch')
			}
			if (masterSwitch != null) {
				paragraph "If set, the binding will do a re-sync to the master switch's state every 5 minutes. Only use this setting if one of your devices is unreliable."
				input pollMaster
			}
			paragraph "<br/><b>OPTIONAL:</b> Override the displayed name of the binding."
			input nameOverride
		}
		section () {
			input enableLogging
		}
	}
}


def installed() {
	log.info "Installed with settings: ${settings}"

	initialize()
}


def updated() {
	log.info "Updated with settings: ${settings}"

	unsubscribe()
	unschedule()
	initialize()
}


def initialize() {
	if (masterSwitch == null) {
		subscribe(switches, "switch.on", switchOnHandler)
		subscribe(switches, "switch.off", switchOffHandler)
		subscribe(switches, "level", levelHandler)
		subscribe(switches, "switch.setLevel", levelHandler)
		subscribe(switches, "speed", speedHandler)
	} else {
		// Is the masterSwitch one of our selected switches?
		def s = switches.find { it.deviceId == masterSwitch.deviceId }
		if (s == null) {
			log "BINDING: The selected master switch was not in the list of bound switches."
			return false
		}
		
		subscribe(masterSwitch, "switch.on", switchOnHandler)
		subscribe(masterSwitch, "switch.off", switchOffHandler)
		subscribe(masterSwitch, "level", levelHandler)
		subscribe(masterSwitch, "switch.setLevel", levelHandler)
		subscribe(masterSwitch, "speed", speedHandler)
	}
	// Generate a label for this child app
	String newLabel
	if (nameOverride && nameOverride.size() > 0) {
		newLabel = nameOverride	
	} else {
		newLabel = "Bind"
		def switchList = []
		if (masterSwitch != null) {
			switches.each {
				if (it.deviceId != masterSwitch.deviceId) switchList << it
			}
		} else {
			switchList = switches
		}
		log.debug "switches: ${switches}, switchList: ${switchList}"
		def ss = switchList.size()
		for (def i = 0; i < ss; i++) {
			if ((i == (ss - 1)) && (ss > 1)) {
				if ((masterSwitch == null) && (ss == 2)) {
					newLabel = newLabel + " to"
				}
				else {
					newLabel = newLabel + " and"
				}
			}
			newLabel = newLabel + " ${switchList[i].displayName}"
			if ((i != (ss - 1)) && (ss > 2)) {
				newLabel = newLabel + ","	
			}
		}
		if (masterSwitch) newLabel = newLabel + ' to ' + masterSwitch.displayName
	}
	app.updateLabel(newLabel)
	
	atomicState.startInteractingMillis = 0 as long
	atomicState.controllingDeviceId = 0
	
	// If a master switch is set, then periodically resync
	if ((masterSwitch != null) && pollMaster) {
		runEvery5Minutes(reSyncFromMaster)	
	}
}


def reSyncFromMaster() {
	// Is masterSwitch set?
	if (masterSwitch == null) {
		log "BINDING: Master Switch not set"
		return
	}
	
	// Is it one of our selected switches?
	def s = switches.find { it.deviceId == masterSwitch.deviceId }
	
	if (s == null) {
		log "BINDING: The selected master switch was not in the list of bound switches."
		return
	}
	
	if ((now() - atomicState.startInteractingMillis as long) < 1000 * 60) {
		// I don't want resync happening while someone is standing at a switch fiddling with it.
		// Wait until the system has been stable for a bit.
		log "BINDING: Skipping reSync because there has been a recent user interaction."
		return
	}
	
	log "BINDING: reSyncFromMaster()"
	def onOrOff = (masterSwitch.currentValue("switch") == "on")
	syncSwitchState(masterSwitch.deviceId, onOrOff)
}


def switchOnHandler(evt) {
	def triggeredDeviceId = evt.deviceId
	def triggeredDevice = switches.find { it.deviceId == triggeredDeviceId }
	log "BINDING: ${triggeredDevice.displayName} ON detected"	
	
	syncSwitchState(triggeredDeviceId, true)
}


def switchOffHandler(evt) {
	def triggeredDeviceId = evt.deviceId
	def triggeredDevice = switches.find { it.deviceId == triggeredDeviceId }
	log "BINDING: ${triggeredDevice.displayName} OFF detected"
	
	syncSwitchState(triggeredDeviceId, false)
}


def levelHandler(evt) {
	def triggeredDeviceId = evt.deviceId
	// Only reflect level events while the switch is on (workaround Zigbee driver problem that sends level immediately after turning off)
	if (evt.device.currentValue('switch', true) == 'off') return
	syncLevelState(triggeredDeviceId)
}


def speedHandler(evt) {
	def triggeredDeviceId = evt.deviceId
	syncSpeedState(triggeredDeviceId)
}


def syncSwitchState(triggeredDeviceId, onOrOff) {
	long now = (new Date()).getTime()
	
	// Don't allow feedback and event cycles.  If this isn't the controlling device and we're still within the characteristic
	// response time, don't sync this event to the other devices.
	if (triggeredDeviceId != atomicState.controllingDeviceId &&
		(now - atomicState.startInteractingMillis as long) < (responseTime as long)) {
		//log "preventing feedback loop ${now - atomicState.startInteractingMillis as long} ${triggeredDeviceId} ${atomicState.controllingDeviceId}"
		return
	}

	atomicState.controllingDeviceId = triggeredDeviceId
	atomicState.startInteractingMillis = (new Date()).getTime()
	
	// Push the event out to every switch except the one that triggered this.
	switches.each { s -> 
		if (s.deviceId != triggeredDeviceId) {
			if (onOrOff) {
				log "BINDING: ${s.displayName}.on()"
				if (s.currentValue('switch', true) != 'on') s.on()
			}
			else {
				log "BINDING: ${s.displayName}.off()"
				if (s.currentValue('switch', true) != 'off') s.off()
			}
		}
	}
}

def syncLevelState(triggeredDeviceId) {
	def triggeredDevice = switches.find { it.deviceId == triggeredDeviceId }
	long now = (new Date()).getTime()
	
	// Don't allow feedback and event cycles.  If this isn't the controlling device and we're still within the characteristic
	// response time, don't sync this event to the other devices.
	if (triggeredDeviceId != atomicState.controllingDeviceId &&
		(now - atomicState.startInteractingMillis as long) < (responseTime as long)) {
		//log "preventing feedback loop ${now - atomicState.startInteractingMillis as long} ${triggeredDeviceId} ${atomicState.controllingDeviceId}"
		return
	}

	def newLevel = triggeredDevice.hasAttribute('level') ? triggeredDevice.currentValue("level", true) : null
	log "BINDING: ${triggeredDevice.displayName} LEVEL ${newLevel} detected"
	if (newLevel == null) return

	atomicState.controllingDeviceId = triggeredDeviceId
	atomicState.startInteractingMillis = (new Date()).getTime()
	
	// Push the event out to every switch except the one that triggered this.
	switches.each { s -> 
		if ((s.deviceId != triggeredDeviceId) && s.hasCommand('setLevel')) {
			if (s.currentValue('level', true) != newLevel) {
				log "BINDING: ${s.displayName}.setLevel($newLevel)"
				s.setLevel(newLevel)
			} else {
				log "BINDING: ${s.displayName} is already at level $newLevel"
			}
		} else {
			log "BINDING: ${s.displayName} does not support setLevel()"	
		}
	}
}


def syncSpeedState(triggeredDeviceId) {
	def triggeredDevice = switches.find { it.deviceId == triggeredDeviceId }
	long now = (new Date()).getTime()
	
	// Don't allow feedback and event cycles.  If this isn't the controlling device and we're still within the characteristic
	// response time, don't sync this event to the other devices.
	if (triggeredDeviceId != atomicState.controllingDeviceId &&
		(now - atomicState.startInteractingMillis as long) < (responseTime as long)) {
		//log "preventing feedback loop ${now - atomicState.startInteractingMillis as long} ${triggeredDeviceId} ${atomicState.controllingDeviceId}"
		return
	}

	def newSpeed = triggeredDevice.currentValue("speed")
	log "BINDING: ${triggeredDevice.displayName} SPEED ${newSpeed} detected"	

	atomicState.controllingDeviceId = triggeredDeviceId
	atomicState.startInteractingMillis = (new Date()).getTime()
	
	// Push the event out to every switch except the one that triggered this.
	switches.each { s -> 
		if ((s.deviceId != triggeredDeviceId) && s.hasCommand('setSpeed')){
			if (s.currentValue('speed', true) != newSpeed) {
				log "BINDING: ${s.displayName}.setSpeed($newSpeed)"
				s.setSpeed(newSpeed)
			} else {
				log "BINDING: ${s.displayName} is already at speed $newSpeed"	
			}
		} else {
			log "BINDING: ${s.displayName} does not support setSpeed()"	
		}
	}
}


def getFormat(type, myText=""){
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
	if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}


def log(msg) {
	if (enableLogging) {
		log.debug msg
	}
}


















