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


def switch1 = [
		name:				"switch1",
		type:				"capability.switch",
		title:				"Switch 1",
		description:		"Select the first switch.",
		multiple:			false,
		required:			true
	]

def switch2 = [
		name:				"switch2",
		type:				"capability.switch",
		title:				"Switch 2",
		description:		"Select the second switch.",
		multiple:			false,
		required:			true
	]

def responseTime = [
		name:				"responseTime",
		type:				"long",
		title:				"Estimated Switch Response Time (in milliseconds)",
		defaultValue:		5000,
		required:			true
	]


def getFormat(type, myText=""){
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
	if(type == "title") return "<div style='color:blue;font-weight: bold'>${myText}</div>"
}

preferences {
	page(name: "mainPage", title: "<b>Switches to Bind:</b>", install: true, uninstall: true) {
		section("") {
			input switch1
			input switch2
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
	subscribe(switch1, "switch.on", switch1OnHandler)
	subscribe(switch1, "switch.off", switch1OffHandler)
	subscribe(switch2, "switch.on", switch2OnHandler)
	subscribe(switch2, "switch.off", switch2OffHandler)
	
	app.updateLabel("Bind ${switch1.displayName} to ${switch2.displayName}")
	
	state.switch1ForcedOnMillis = 0 as long
	state.switch1ForcedOffMillis = 0 as long
	state.switch2ForcedOnMillis = 0 as long
	state.switch2ForcedOffMillis = 0 as long
}



def switch1OnHandler(evt) {
	log.debug "SWITCH BINDING: ${switch1.displayName}:ON detected"
	
	long now = (new Date()).getTime()

	if ((now - state.switch1ForcedOnMillis as long) > (responseTime as long)) {
		log.debug "SWITCH BINDING: Turning on ${switch2.displayName}"
		state.switch2ForcedOnMillis = now
		switch2.on()
	}
}

def switch1OffHandler(evt) {
	log.debug "SWITCH BINDING: ${switch1.displayName}:OFF detected"	

	long now = (new Date()).getTime()
	
	if ((now - state.switch1ForcedOffMillis as long) > (responseTime as long)) {
		log.debug "SWITCH BINDING: Turning off ${switch2.displayName}"
		state.switch2ForcedOffMillis = now
		switch2.off()
	}
}

def switch2OnHandler(evt) {
	log.debug "SWITCH BINDING: ${switch2.displayName}:ON detected"
	
	long now = (new Date()).getTime()
	
	if ((now - state.switch2ForcedOnMillis as long) > (responseTime as long)) {
		log.debug "SWITCH BINDING: Turning on ${switch1.displayName}"
		state.switch1ForcedOnMillis = now
		switch1.on()
	}
}

def switch2OffHandler(evt) {
	log.debug "SWITCH BINDING: ${switch2.displayName}:OFF detected"
	
	long now = (new Date()).getTime()
	
	if ((now - state.switch2ForcedOffMillis as long) > (responseTime as long)) {
		log.debug "SWITCH BINDING: Turning off ${switch1.displayName}"
		state.switch1ForcedOffMillis = now
		switch1.off()
	}
}













