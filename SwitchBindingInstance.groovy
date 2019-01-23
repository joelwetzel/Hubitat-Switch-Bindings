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
    description: "Child app that is instantiated by the Switch Bindings app",
    category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


def switches = [
		name:				"switches",
		type:				"capability.switch",
		title:				"Switches",
		description:		"Select the switches to bind.",
		multiple:			true,
		required:			true
	]


def responseTime = [
		name:				"responseTime",
		type:				"long",
		title:				"Estimated Switch Response Time (in milliseconds)",
		defaultValue:		5000,
		required:			true
	]


preferences {
	page(name: "mainPage", title: "<b>Switches to Bind:</b>", install: true, uninstall: true) {
		section("") {
			input switches
		}
		section ("<b>Advanced Settings</b>") {
			paragraph "<b>WARNING:</b> Only change Estimated Switch Response Time if you know what you are doing!  Some dimmers don't report their new status until after they have slowly dimmed.  The app uses this estimated duration to make sure that the two bound switches don't infinitely trigger each other.  Only reduce this value if you are using two very fast switches, and you regularly physically toggle both of them right after each other.  (Not a common case!)"
			input responseTime
		}
	}
}


def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}


def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}


def initialize() {
	subscribe(switches, "switch.on", switchOnHandler)
	subscribe(switches, "switch.off", switchOffHandler)

	// Generate a label for this child app
	def newLabel = "Bind"
	for (def i = 0; i < switches.size(); i++) {
		if (i == (switches.size() - 1) && switches.size() > 1) {
			if (switches.size() == 2) {
				newLabel = newLabel + " to"
			}
			else {
				newLabel = newLabel + " and"
			}
		}
		newLabel = newLabel + " ${switches[i].displayName}"
		if (i != (switches.size() - 1) && switches.size() > 2) {
			newLabel = newLabel + ","	
		}
	}
	app.updateLabel(newLabel)
	
	state.startInteractingMillis = 0 as long
	state.controllingDeviceId = 0
}


def switchOnHandler(evt) {
	log.debug "SWITCH BINDING:ON detected"
	
	syncEvent(evt, true)
}


def switchOffHandler(evt) {
	log.debug "SWITCH BINDING:OFF detected"	

	syncEvent(evt, false)
}


def syncEvent(evt, onOrOff) {
	def switchedDeviceId = evt.deviceId
	long now = (new Date()).getTime()

	// Don't allow feedback and event cycles.  If this isn't the controlling device and we're still within the characteristic
	// response time, don't sync this event to others to the other devices.
	if (switchedDeviceId != state.controllingDeviceId &&
		(now - state.startInteractingMillis as long) < (responseTime as long)) {
		return
	}
	
	state.controllingDeviceId = switchedDeviceId
	state.startInteractingMillis = now
	
	// Push the event out to every switch except the one that triggered this.
	switches.each { s -> 
		if (s.deviceId != switchedDeviceId) {
			if (onOrOff) {
				s.on()
			}
			else {
				s.off()
			}
		}
	}
}














