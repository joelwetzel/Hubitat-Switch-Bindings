/**
 *  Switch Binding Instance v2.0.6
 *
 *  Copyright 2024 Joel Wetzel
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

            paragraph "<br/>Select attributes/events to sync between switches:"
            input(name: "syncOnOff", type: "bool", title: "Switch On/Off", defaultValue: true, required: true)
            input(name: "syncLevel", type: "bool", title: "Switch Level", defaultValue: true, required: true, submitOnChange: true)
            if (syncLevel) {
                input(name: "syncHeld", type: "bool", title: "Does your switch implement HELD events as button presses?", defaultValue: false, required: false, submitOnChange: true)
                if (syncLevel && syncHeld) {
                    input(name: "heldUpButtonNumber", type: "number", title: "Button number for holding UP", defaultValue: 1, required: false)
                    input(name: "heldDownButtonNumber", type: "number", title: "Button number for holding DOWN", defaultValue: 2, required: false)
                }
            }
            input(name: "syncSpeed", type: "bool", title: "Fan Speed", defaultValue: false, required: true)
            paragraph "<i>Note: Most fans also respond to level and translate it into speed.  So if syncing a dimmer and a fan, you may not need to sync speed.</i>"
            input(name: "syncHue", type: "bool", title: "Hue", defaultValue: false, required: true)
            input(name: "syncSaturation", type: "bool", title: "Saturation", defaultValue: false, required: true)
            input(name: "syncColorTemperature", type: "bool", title: "Color Temperature", defaultValue: false, required: true)
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
						  options:["1", "5", "10", "15", "30"])
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

	if (masterSwitch != null && settings.masterOnly) {
        // If "Master Only" is set, only subscribe to events on the  master switch.
        log "Subscribing only to master switch events"

        subscribeToEvents([masterSwitch])
    } else {
        log "Subscribing to all switch events"

        subscribeToEvents(switches)
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
	atomicState.controllingDeviceId = null
	atomicState.lastOffEventMillis = 0 as long
	atomicState.lastOffDeviceId = null

	// If a master switch is set, then periodically resync
    if (settings.masterSwitchId && settings.pollMaster) {
		schedule("0 */${settings.pollingInterval} * * * ?", "reSyncFromMaster")
	}
}


def subscribeToEvents(subscriberList) {
    subscribe(subscriberList, "switch.on", 		'switchOnHandler')
    subscribe(subscriberList, "switch.off", 	'switchOffHandler')
    subscribe(subscriberList, "level", 			'levelHandler')
    subscribe(subscriberList, "speed", 			'speedHandler')
    subscribe(subscriberList, "hue",            'hueHandler')
    subscribe(subscriberList, "saturation",     'saturationHandler')
    subscribe(subscriberList, "colorTemperature", 'colorTemperatureHandler')
    subscribe(subscriberList, "held",           "heldHandler")
    subscribe(subscriberList, "released",       "releasedHandler")
}


void reSyncFromMaster(evt) {
	log.info "reSyncFromMaster()"

	// Is masterSwitch set?
	if (settings.masterSwitchId == null) {
		log "reSyncFromMaster: Master Switch not set"
		return
	}

    def masterSwitch = settings.switches.find { it.deviceId.toString() == settings.masterSwitchId.toString() }

	if (masterSwitch == null) {
		log "reSyncFromMaster: Master Switch not found in switches list"
		return
	}

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
	log "SWITCH On detected - ${evt.device.displayName}"

    if (checkForFeedbackLoop(evt.deviceId)) {
        return
    }

	syncSwitchState(evt.deviceId, true)
}


def switchOffHandler(evt) {
	log "SWITCH Off detected - ${evt.device.displayName}"

    if (checkForFeedbackLoop(evt.deviceId)) {
        return
    }

	// Track when this device was turned off to prevent spurious level events immediately after
	atomicState.lastOffEventMillis = (new Date()).getTime()
	atomicState.lastOffDeviceId = evt.deviceId

	syncSwitchState(evt.deviceId, false)
}


def levelHandler(evt) {
	// Only reflect level events while the switch is on, OR if significant time has passed since turning off.
	// This is a workaround for Zigbee driver problem that sends level immediately after turning off,
	// but we don't want to block level events that are part of turning on (e.g., Eaton RF9640/RF9642).
	long now = (new Date()).getTime()
	if (evt.device.currentValue('switch', true) == 'off') {
		// If this device was just turned off (within 1 second), ignore the level event
		if (evt.deviceId == atomicState.lastOffDeviceId && 
		    (now - atomicState.lastOffEventMillis as long) < 1000) {
			log "levelHandler: Ignoring level event immediately after turn-off for ${evt.device.displayName}"
			return
		}
	}

    log "LEVEL ${evt.value} detected - ${evt.device.displayName}"

    if (checkForFeedbackLoop(evt.deviceId)) {
        return
    }

	syncLevelState(evt.deviceId)
}


def speedHandler(evt) {
    log "SPEED ${evt.value} detected - ${evt.device.displayName}"

    if (checkForFeedbackLoop(evt.deviceId)) {
        return
    }

	syncSpeedState(evt.deviceId)
}


def hueHandler(evt) {
    log "HUE ${evt.value} detected - ${evt.device.displayName}"

    if (checkForFeedbackLoop(evt.deviceId)) {
        return
    }

	syncHueState(evt.deviceId)
}


def saturationHandler(evt) {
    log "SATURATION ${evt.value} detected - ${evt.device.displayName}"

    if (checkForFeedbackLoop(evt.deviceId)) {
        return
    }

    syncSaturationState(evt.deviceId)
}

def colorTemperatureHandler(evt) {
    log "COLOR TEMPERATURE ${evt.value} detected - ${evt.device.displayName}"

    if (checkForFeedbackLoop(evt.deviceId)) {
        return
    }

    syncColorTemperatureState(evt.deviceId)
}

def heldHandler(evt) {
    log "HELD ${evt.value} detected - ${evt.device.displayName}"

    if (checkForFeedbackLoop(evt.deviceId)) {
        return
    }

    startLevelChange(evt.deviceId, evt.value)
}

def releasedHandler(evt) {
    log "RELEASED ${evt.value} detected - ${evt.device.displayName}"

    if (checkForFeedbackLoop(evt.deviceId)) {
        return
    }

    stopLevelChange(evt.deviceId, evt.value)
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
    if ((settings.syncOnOff != null) && !settings.syncOnOff) {
        return
    }

    def triggeredDevice = switches.find { triggeredDeviceId != null && it.deviceId.toString() == triggeredDeviceId.toString() }

	if (triggeredDevice == null) {
		log "syncSwitchState: Triggered device not found"
		return
	}

	def newLevel = triggeredDevice.hasAttribute('level') ? triggeredDevice.currentValue("level", true) : null        // If the triggered device has a level, then we're going to push it out to the other devices too.
    if (newLevel != null && newLevel < 5) {
        newLevel = 5
    }

	// Push the event out to every switch except the one that triggered this.
	switches.each { s ->
		if (s.deviceId == triggeredDeviceId) {
            return
        }

        if (onOrOff) {
            // Special case for Hue bulbs.  They have a device setting for transitionTime, and to honor that, we need to setLevel with the transitionTime, instead of just turning on.
            if (s.currentValue('switch', true) != 'on' && s.getSetting("transitionTime") != null && s.hasAttribute('level') && newLevel != null) {
                def transitionTime = s.getSetting("transitionTime")
                s.setLevel(newLevel, transitionTime)
                return
            }

            if (s.currentValue('switch', true) != 'on') {
                s.on()
            }

            if (s.hasCommand('setLevel') && newLevel != null && s.currentValue('level', true) != newLevel) {        // Push the level of the triggering device (if it has one) out to the other devices. (If they support it)
                s.setLevel(newLevel)
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
    if ((settings.syncLevel != null) && !settings.syncLevel) {
        return
    }

	def triggeredDevice = switches.find { it.deviceId == triggeredDeviceId }

	if (triggeredDevice == null) {
		log "syncLevelState: Triggered device not found"
		return
	}

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
            if (newLevel != null && s.currentValue('level', true) != newLevel) {
                s.setLevel(newLevel)
            }
        } else if (s.currentValue('switch') == 'off' && newLevel > 0) {
            s.on()
        }
	}
}


def syncHueState(triggeredDeviceId) {
    if ((settings.syncHue != null) && !settings.syncHue) {
        return
    }

	def triggeredDevice = switches.find { it.deviceId == triggeredDeviceId }

	if (triggeredDevice == null) {
		log "syncHueState: Triggered device not found"
		return
	}

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


def syncSaturationState(triggeredDeviceId) {
    if ((settings.syncSaturation != null) && !settings.syncSaturation) {
        return
    }

	def triggeredDevice = switches.find { it.deviceId == triggeredDeviceId }

	if (triggeredDevice == null) {
		log "syncSaturationState: Triggered device not found"
		return
	}

	def newSaturation = triggeredDevice.hasAttribute('saturation') ? triggeredDevice.currentValue("saturation", true) : null
    if (newSaturation == null) {
        return
    }

	// Push the event out to every switch except the one that triggered this.
	switches.each { s ->
		if (s.deviceId == triggeredDeviceId) {
            return
        }

        if (s.hasCommand('setSaturation') && s.currentValue('saturation', true) != newSaturation) {
            s.setSaturation(newSaturation)
        }
	}
}


def syncColorTemperatureState(triggeredDeviceId) {
    if ((settings.syncColorTemperature != null) && !settings.syncColorTemperature) {
        return
    }

	def triggeredDevice = switches.find { it.deviceId == triggeredDeviceId }

	if (triggeredDevice == null) {
		log "syncColorTemperatureState: Triggered device not found"
		return
	}

	def newColorTemperature = triggeredDevice.hasAttribute('colorTemperature') ? triggeredDevice.currentValue("colorTemperature", true) : null
    if (newColorTemperature == null) {
        return
    }

	// Push the event out to every switch except the one that triggered this.
	switches.each { s ->
		if (s.deviceId == triggeredDeviceId) {
            return
        }

        if (s.hasCommand('setColorTemperature') && s.currentValue('colorTemperature', true) != newColorTemperature) {
            s.setColorTemperature(newColorTemperature)
        }
	}
}


def syncSpeedState(triggeredDeviceId) {
    if ((settings.syncSpeed != null) && !settings.syncSpeed) {
        return
    }

	def triggeredDevice = switches.find { it.deviceId == triggeredDeviceId }

	if (triggeredDevice == null) {
		log "syncSpeedState: Triggered device not found"
		return
	}

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


def startLevelChange(triggeredDeviceId, buttonNumber) {
    if (settings.syncHeld == null || !settings.syncHeld || settings.heldUpButtonNumber == null || settings.heldDownButtonNumber == null) {
        return
    }

    def direction = 'none'

    if (buttonNumber == settings.heldUpButtonNumber.toString()) {
        direction = 'up'
    }
    else if (buttonNumber == settings.heldDownButtonNumber.toString()) {
        direction = 'down'
    }

    if (direction == 'none') {
        return
    }

    // Push the event out to every switch except the one that triggered this.
	switches.each { s ->
		if (s.deviceId == triggeredDeviceId) {
            return
        }

        if (s.hasCommand('startLevelChange')) {
            s.startLevelChange(direction)
        }
	}
}


def stopLevelChange(triggeredDeviceId, buttonNumber) {
    if (settings.syncHeld == null || !settings.syncHeld || settings.heldUpButtonNumber == null || settings.heldDownButtonNumber == null) {
        return
    }

    // Only process if the button number matches one of the configured held buttons
    if (buttonNumber != settings.heldUpButtonNumber.toString() && buttonNumber != settings.heldDownButtonNumber.toString()) {
        return
    }

    // Push the event out to every switch except the one that triggered this.
	switches.each { s ->
		if (s.deviceId == triggeredDeviceId) {
            return
        }

        if (s.hasCommand('stopLevelChange')) {
            s.stopLevelChange()
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
