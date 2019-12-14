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
	dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        if (!app.label) {
			app.updateLabel(app.name)
		}
		section(getFormat("title", (app?.label ?: app?.name).toString())) {
			input(name:	"nameOverride", type: "string", title: "Custom name for this ${app.name}?", multiple: false, required: false, submitOnChange: true)
            
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
            
			if (settings.masterSwitch) {
				// Installed over an older version - convert the settings
				def masterId = settings.masterSwitch.deviceId.toString()
				app.updateSetting('masterSwitchId', masterId)
				settings.masterSwitchId = masterId
				app.updateSetting('pollMaster', true)
				settings.pollMaster = true
				app.removeSetting('masterSwitch')
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
			input(name:	"responseTime",	type: "long", title: "Estimated Switch Response Time (in milliseconds)", defaultValue: 5000, required: true)
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
	def masterSwitch 
    
	if ((settings.masterSwitchId == null) || !settings.masterOnly) {
		subscribe(switches, 	"switch.on", 		switchOnHandler)
		subscribe(switches, 	"switch.off", 		switchOffHandler)
		subscribe(switches, 	"level", 			levelHandler)
		subscribe(switches, 	"switch.setLevel", 	levelHandler)
		subscribe(switches, 	"speed", 			speedHandler)
	} else if (settings.masterSwitchId && settings.masterOnly) {
		masterSwitch = settings.switches.find { it.deviceId.toString() == settings.masterSwitchId.toString() }
		
		subscribe(masterSwitch, "switch.on", 		switchOnHandler)
		subscribe(masterSwitch, "switch.off", 		switchOffHandler)
		subscribe(masterSwitch, "level", 			levelHandler)
		subscribe(masterSwitch, "switch.setLevel", 	levelHandler)
		subscribe(masterSwitch, "speed", 			speedHandler)
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
        
		log "switches: ${switches}, switchList: ${switchList}"
        
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
	if ((masterSwitch != null) && settings.pollMaster) {
		runEvery5Minutes(reSyncFromMaster)	
	}
}


def reSyncFromMaster() {
	// Is masterSwitch set?
	if (masterSwitchId == null) {
		log "BINDING: Master Switch not set"
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
	syncSwitchState(masterSwitchId, onOrOff)
}


def switchOnHandler(evt) {
	log "BINDING: ${evt.displayName} ON detected"	
	
	syncSwitchState(evt.deviceId, true)
	syncLevelState(evt.deviceId)		// Double check that the level is correct
}


def switchOffHandler(evt) {
	log "BINDING: ${evt.displayName} OFF detected"
	
	syncSwitchState(evt.deviceId, false)
}


def levelHandler(evt) {
	// Only reflect level events while the switch is on (workaround Zigbee driver problem that sends level immediately after turning off)
	if (evt.device.currentValue('switch', true) == 'off') return

	syncLevelState(evt.deviceId)
}


def speedHandler(evt) {
	syncSpeedState(evt.deviceId)
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
				log "BINDING: ${s.displayName} -> on()"
                if (s.currentValue('switch', true) != 'on') {
                    s.on()
                }
			}
			else {
				log "BINDING: ${s.displayName} -> off()"
                if (s.currentValue('switch', true) != 'off') {
                    s.off()
                }
			}
		}
	}
}


def syncLevelState(triggeredDeviceId) {
	long now = (new Date()).getTime()
	
	// Don't allow feedback and event cycles.  If this isn't the controlling device and we're still within the characteristic
	// response time, don't sync this event to the other devices.
	if (triggeredDeviceId != atomicState.controllingDeviceId &&
		(now - atomicState.startInteractingMillis as long) < (responseTime as long)) {
		//log "preventing feedback loop ${now - atomicState.startInteractingMillis as long} ${triggeredDeviceId} ${atomicState.controllingDeviceId}"
		return
	}
	
	def triggeredDevice = switches.find { it.deviceId == triggeredDeviceId }
	def newLevel = triggeredDevice.hasAttribute('level') ? triggeredDevice.currentValue("level", true) : null
	
    if (newLevel == null) {
        return
    }

  	log "BINDING: ${triggeredDevice.displayName} LEVEL ${newLevel} detected"

	atomicState.controllingDeviceId = triggeredDeviceId
	atomicState.startInteractingMillis = (new Date()).getTime()
	
	// Push the event out to every switch except the one that triggered this.
	switches.each { s -> 
		if ((s.deviceId != triggeredDeviceId) && s.hasCommand('setLevel')) {
			if (s.currentValue('level', true) != newLevel) {
				log "BINDING: ${s.displayName} -> setLevel($newLevel)"
				s.setLevel(newLevel, 1)
			} else {
				log "BINDING: ${s.displayName} is already at level $newLevel"
			}
		} else {
            if (s.deviceId != triggeredDeviceId) {
                log "BINDING: ${s.displayName} does not support setLevel()"	
            }
		}
	}
}


def syncSpeedState(triggeredDeviceId) {
	long now = (new Date()).getTime()
	
	// Don't allow feedback and event cycles.  If this isn't the controlling device and we're still within the characteristic
	// response time, don't sync this event to the other devices.
	if (triggeredDeviceId != atomicState.controllingDeviceId &&
		(now - atomicState.startInteractingMillis as long) < (responseTime as long)) {
		//log "preventing feedback loop ${now - atomicState.startInteractingMillis as long} ${triggeredDeviceId} ${atomicState.controllingDeviceId}"
		return
	}

	def triggeredDevice = switches.find { it.deviceId == triggeredDeviceId }
	def newSpeed = triggeredDevice.hasAttribute('speed') ? triggeredDevice.currentValue("speed") : null

    if (newSpeed == null) {
        return
    }

   	log "BINDING: ${triggeredDevice.displayName} SPEED ${newSpeed} detected"

	atomicState.controllingDeviceId = triggeredDeviceId
	atomicState.startInteractingMillis = (new Date()).getTime()
	
	// Push the event out to every switch except the one that triggered this.
	switches.each { s -> 
		if ((s.deviceId != triggeredDeviceId) && s.hasCommand('setSpeed')){
			if (s.currentValue('speed', true) != newSpeed) {
				log "BINDING: ${s.displayName} -> setSpeed($newSpeed)"
				s.setSpeed(newSpeed)
			} else {
				log "BINDING: ${s.displayName} is already at speed $newSpeed"	
			}
		} else {
            if (s.deviceId != triggeredDeviceId) {
                log "BINDING: ${s.displayName} does not support setSpeed()"	
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



















