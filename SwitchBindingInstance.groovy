/**
 *  Switch Binding Instance v1.1
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
	parent: 	"joelwetzel:Switch Bindings",
    name: 		"Switch Binding Instance",
    namespace: 	"joelwetzel",
    author: 	"Joel Wetzel",
    description: "Child app that is instantiated by the Switch Bindings app.",
    category: 	"Convenience",
	iconUrl: 	"",
    iconX2Url: 	"",
    iconX3Url: 	"")

preferences {
	page(name: "mainPage")
}


def mainPage() {
	dynamicPage(name: "mainPage", title: "Preferences", install: true, uninstall: true) {
        if (!app.label) {
			app.updateLabel(app.name)
		}
		section(getFormat("title", (app?.label ?: app?.name).toString())) {
			input(name:	"nameOverride", type: "text", title: "Custom name for this ${app.name}?", multiple: false, required: false, submitOnChange: true)

			if (settings.nameOverride) {
				app.updateLabel(settings.nameOverride)
			}
		}
		section("") {
			input(name:	"switches",	type: "capability.switch", title: "Switches to Bind", description: "Select the switches to bind.", multiple: true, required: true, submitOnChange: true)
		}
		section ("<b>Advanced Settings</b>", hideable: true, hidden: false) {
			def masterChoices = [:]

			settings?.switches?.each {
				masterChoices << [(it.deviceId.toString()): it.displayName.toString()]
			}

			input(name:	"masterSwitchId",	type: "enum", title: "Select an (optional) 'Master' switch", multiple: false, required: false, submitOnChange: true, options: (masterChoices))
			def masterSwitch
            if (masterSwitchId != null) {
                masterSwitch = settings?.switches?.find { it?.deviceId?.toString() == settings?.masterSwitchId.toString() }
            }
			if (masterSwitch != null) {
				input(name:	"masterOnly", type:	"bool", title: "Bind to changes on ${masterSwitch?.displayName} only? (One-way binding instead of the normal two-way binding.)", multiple: false, defaultValue: false, submitOnChange: true)
				input(name:	'pollMaster', type:	'bool', title: "Poll ${masterSwitch.displayName} and synchronize all the devices?", defaultValue: false, required: true, submitOnChange: true)
				if (settings?.pollMaster) {
					input(name: "pollingInterval", title:"Polling Interval (in minutes)?", type: "enum", required:false, multiple:false, defaultValue:"5", submitOnChange: true,
						  options:["1", "2", "3", "5", "10", "15", "30"])
					if (settings.pollingInterval == null) {
                        app.updateSetting('pollingInterval', "5"); settings.pollingInterval = "5";
                    }
				}
			}
			paragraph "<br/><b>WARNING:</b> Only adjust Estimated Switch Response Time if you know what you are doing! Some dimmers don't report their new status until after they have slowly dimmed. " +
					  "The app uses this estimated duration to make sure that the bound switches don't infinitely trigger each other. Only reduce this value if you are using very fast switches, " +
					  "and you regularly physically toggle 2 (or more) of them right after each other (not a common case)."
			input(name:	"responseTime",	type: "number", title: "Estimated Switch Response Time (in milliseconds)", defaultValue: 5000, required: true)
		}
		section () {
			input(name:	"enableLogging", type: "bool", title: "Enable Debug Logging?", defaultValue: false,	required: true)
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
    log "initialize()"

	def masterSwitch = settings.switches.find { it.deviceId.toString() == settings.masterSwitchId?.toString() }

	if (settings.masterSwitchId && settings.masterOnly) {
        // If "Master Only" is set, only subscribe to events on the  master switch.
        log "Subscribing only to master switch events"
		subscribe(masterSwitch, "switch.on", 		'switchOnHandler')
		subscribe(masterSwitch, "switch.off", 		'switchOffHandler')
		subscribe(masterSwitch, "level", 			'levelHandler')
		subscribe(masterSwitch, "speed", 			'speedHandler')
        subscribe(masterSwitch, "hue",              'hueHandler')
	} else {
        log "Subscribing to all switch events"
		subscribe(switches, 	"switch.on", 		'switchOnHandler')
		subscribe(switches, 	"switch.off", 		'switchOffHandler')
		subscribe(switches, 	"level", 			'levelHandler')
		subscribe(switches, 	"speed", 			'speedHandler')
        subscribe(switches,     "hue",              'hueHandler')
    }

	// Generate a label for this child app
	String newLabel
	if (settings.nameOverride && settings.nameOverride.size() > 0) {
		newLabel = settings.nameOverride
	} else {
		newLabel = "Bind"
		def switchList = []

		if (masterSwitch != null) {
			switches.each {
                if (it.deviceId.toString() != masterSwitchId.toString()) {
                    switchList << it
                }
			}
		} else {
			switchList = switches
		}

		//log "switches: ${switches}, switchList: ${switchList}"

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

        if (masterSwitch) {
            newLabel = newLabel + ' to ' + masterSwitch.displayName
        }
	}
	app.updateLabel(newLabel)

	atomicState.startInteractingMillis = 0 as long
	atomicState.controllingDeviceId = 0

	// If a master switch is set, then periodically resync
    if (settings.masterSwitchId && settings.pollMaster) {
		runEvery5Minutes('reSyncFromMaster')
	}
}


def reSyncFromMaster() {
	log "reSyncFromMaster()"

	// Is masterSwitch set?
	if (settings.masterSwitchId == null) {
		log "reSyncFromMaster: Master Switch not set"
		return
	}

    def masterSwitch = settings.switches.find { it.deviceId.toString() == settings.masterSwitchId.toString() }
    //log "masterSwitchId: ${settings.masterSwitchId.toString()}"
    //log "masterSwitch: ${masterSwitch}"

	if ((now() - atomicState.startInteractingMillis as long) < 1000 * 60) {
		// I don't want resync happening while someone is standing at a switch fiddling with it.
		// Wait until the system has been stable for a bit.
		log "reSyncFromMaster: Skipping reSync because there has been a recent user interaction."
		return
	}

	def onOrOff = (masterSwitch.currentValue("switch") == "on")

	syncSwitchState(masterSwitchId, onOrOff)
}


def switchOnHandler(evt) {
	log "BINDING: ${evt.device.displayName} SWITCH On detected"

    if (checkForFeedbackLoop(evt.deviceId)) {
        return
    }

	syncSwitchState(evt.deviceId, true)
	syncLevelState(evt.deviceId)		// Double check that the level is correct
}


def switchOffHandler(evt) {
	log "BINDING: ${evt.device.displayName} SWITCH Off detected"

    if (checkForFeedbackLoop(evt.deviceId)) {
        return
    }

	syncSwitchState(evt.deviceId, false)
}


def levelHandler(evt) {
	// Only reflect level events while the switch is on (workaround for Zigbee driver problem that sends level immediately after turning off)
	if (evt.device.currentValue('switch', true) == 'off') return

    log "BINDING: ${evt.device.displayName} LEVEL ${evt.value} detected"

    if (checkForFeedbackLoop(evt.deviceId)) {
        return
    }

	syncLevelState(evt.deviceId)
}


def speedHandler(evt) {
    log "BINDING: ${evt.device.displayName} SPEED ${evt.value} detected"

    if (checkForFeedbackLoop(evt.deviceId)) {
        return
    }

	syncSpeedState(evt.deviceId)
}


def hueHandler(evt) {
    log "BINDING: ${evt.device.displayName} HUE ${evt.value} detected"

    if (checkForFeedbackLoop(evt.deviceId)) {
        return
    }

	syncHueState(evt.deviceId)
}

boolean checkForFeedbackLoop(triggeredDeviceId) {
    long now = (new Date()).getTime()

    // Don't allow feedback and event cycles.  If this isn't the controlling device and we're still within the characteristic
    // response time, don't sync this event to the other devices.
    if (triggeredDeviceId != atomicState.controllingDeviceId &&
        (now - atomicState.startInteractingMillis as long) < (responseTime as long)) {
        log "checkForFeedbackLoop: Preventing feedback loop"
        //log "preventing feedback loop variables: ${now - atomicState.startInteractingMillis as long} ${triggeredDeviceId} ${atomicState.controllingDeviceId}"
        return true
    }

    atomicState.controllingDeviceId = triggeredDeviceId
	atomicState.startInteractingMillis = now

    return false
}


def syncSwitchState(triggeredDeviceId, onOrOff) {
	// Push the event out to every switch except the one that triggered this.
	switches.each { s ->
		if (s.deviceId == triggeredDeviceId) {
            return
        }

        if (onOrOff) {
            if (s.currentValue('switch', true) != 'on') {
                s.on()
            }
        }
        else {
            if (s.currentValue('switch', true) != 'off') {
                s.off()
            }
        }
	}
}


def syncLevelState(triggeredDeviceId) {
	def triggeredDevice = switches.find { it.deviceId == triggeredDeviceId }

	def newLevel = triggeredDevice.hasAttribute('level') ? triggeredDevice.currentValue("level", true) : null
    if (newLevel == null) {
        return
    }

	// Push the event out to every switch except the one that triggered this.
	switches.each { s ->
		if (s.deviceId == triggeredDeviceId) {
            return
        }

        if (s.hasCommand('setLevel')) {
            if (s.currentValue('level', true) != newLevel) {
                s.setLevel(newLevel)
            }
        } else if (s.currentValue('switch') == 'off' && newLevel > 0) {
            s.on()
        }
	}
}


def syncHueState(triggeredDeviceId) {
	def triggeredDevice = switches.find { it.deviceId == triggeredDeviceId }

	def newHue = triggeredDevice.hasAttribute('hue') ? triggeredDevice.currentValue("hue", true) : null
    if (newHue == null) {
        return
    }

	// Push the event out to every switch except the one that triggered this.
	switches.each { s ->
		if (s.deviceId == triggeredDeviceId) {
            return
        }

        if (s.hasCommand('setHue') && s.currentValue('hue', true) != newHue) {
            s.setHue(newHue)
        }
	}
}


def syncSpeedState(triggeredDeviceId) {
	def triggeredDevice = switches.find { it.deviceId == triggeredDeviceId }

	def newSpeed = triggeredDevice.hasAttribute('speed') ? triggeredDevice.currentValue("speed") : null
    if (newSpeed == null) {
        return
    }

	// Push the event out to every switch except the one that triggered this.
	switches.each { s ->
		if (s.deviceId == triggeredDeviceId) {
            return
        }

        if (s.hasCommand('setSpeed')) {
            if (s.currentValue('speed', true) != newSpeed) {
				s.setSpeed(newSpeed)
			}
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
