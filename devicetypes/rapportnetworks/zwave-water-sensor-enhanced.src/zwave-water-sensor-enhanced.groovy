/**
 *  Copyright 2015 SmartThings
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
 *  Generic Z-Wave Water Sensor
 *
 *  Author: SmartThings
 *  Date: 2013-03-05
 *
 *  Enhanced functionality added by Alasdair Thin, 2018-06-12
 */

metadata {
	definition(name: "Z-Wave Water Sensor Enhanced", namespace: "rapportnetworks", author: "Alasdair Thin", ocfDeviceType: "x.com.st.d.sensor.moisture") {
		capability "Contact Sensor"
		capability "Water Sensor"
		capability "Sensor"
		capability "Battery"
		capability "Health Check"
		capability "Configuration" // added

		attribute "composite", "string"

		fingerprint deviceId: '0xA102', inClusters: '0x30,0x9C,0x60,0x85,0x8E,0x72,0x70,0x86,0x80,0x84,0x7A'
		fingerprint mfr: "021F", prod: "0003", model: "0085", deviceJoinName: "Dome Leak Sensor"
		fingerprint mfr: "0086", prod: "0002", model: "002D", deviceJoinName: "Water Sensor Enhanced Bed Chair Toilet"
	}

	simulator {
		status "dry": "command: 3003, payload: 00"
		status "wet": "command: 3003, payload: FF"
		status "dry notification": "command: 7105, payload: 00 00 00 FF 05 FE 00 00"
		status "wet notification": "command: 7105, payload: 00 FF 00 FF 05 02 00 00"
		status "wake up": "command: 8407, payload: "
	}

	preferences {
		section {
			input("use", "enum", title: "What type of sensor do you want to use this device for?", description: "Tap to set", options: ["Bed", "Chair", "Toilet", "Water"], defaultValue: "Water", required: true, displayDuringSetup: true)
		}
	}

	tiles(scale: 2) {
		multiAttributeTile(name: "composite", type: "generic", width: 6, height: 4) {
			tileAttribute("device.composite", key: "PRIMARY_CONTROL") {
				attributeState("dry", label: '${name}', icon:"st.contact.contact.open", backgroundColor: "#ffffff")
				attributeState("empty", label: '${name}', icon:"st.contact.contact.open", backgroundColor: "#ffffff")
				attributeState("vacant", label: '${name}', icon:"st.contact.contact.open", backgroundColor: "#ffffff")
				attributeState("full", label: '${name}', icon:"st.contact.contact.open", backgroundColor: "#ffffff")

				attributeState("wet", label: '${name}', icon:"st.contact.contact.closed", backgroundColor: "#00a0dc")
				attributeState("occupied", label: '${name}', icon:"st.contact.contact.closed", backgroundColor: "#00a0dc")
				attributeState("flushing", label: '${name}', icon:"st.contact.contact.closed", backgroundColor: "#00a0dc")
			}
			tileAttribute("device.battery", key: "SECONDARY_CONTROL") {
				attributeState("battery", label: '${currentValue} % battery', unit: "")
			}
		}
	}
}

def updateDataValues() {
	def useStates = [
		Bed: [event: 'contact', inactive: 'empty', active: 'occupied'],
		Chair: [event: 'contact', inactive: 'vacant', active: 'occupied'],
		Toilet: [event: 'contact', inactive: 'full', active: 'flushing'],
		Water: [event: 'water', inactive: 'dry', active: 'wet']
	]
	def event = (use) ? useStates."${use}".event : 'water'
	def inactive = (use) ? useStates."${use}".inactive : 'dry'
	def active = (use) ? useStates."${use}".active : 'wet'
	updateDataValue("use", use)
	updateDataValue("event", event)
	updateDataValue("inactive", inactive)
	updateDataValue("active", active)
}

def installed() {
	// Dome Leak Sensor sends WakeUpNotification every 12 hours. Please add zwaveinfo.mfr check when adding other sensors with different interval.
	sendEvent(name: "checkInterval", value: (2 * 12 + 2) * 60 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
	updateDataValues()
}

def updated() {
	def result = []
	// Dome Leak Sensor sends WakeUpNotification every 12 hours. Please add zwaveinfo.mfr check when adding other sensors with different interval.
	sendEvent(name: "checkInterval", value: (2 * 12 + 2) * 60 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
	updateDataValues()

	log.debug "Updated with settings: ${settings}"
	state.configuredParameters = [:] // *** define state variable (map) to store configuration reports received from sensor
	setConfigured("false") //wait until the next time device wakeup to send configure command after user change preference

//	result << response(zwave.wakeUpV1.wakeUpIntervalSet(seconds: 2 * 3600, nodeid: zwaveHubNodeId).format())
//	result << response(zwave.manufacturerspecificv2.ManufacturerSpecificGet().format())

	return result
}

private getCommandClassVersions() {
	[0x20: 1, 0x30: 1, 0x31: 5, 0x70: 1, 0x71: 3, 0x80: 1, 0x84: 1, 0x9C: 1]
}

def parse(String description) {
	def result = null
	if (description.startsWith("Err")) {
		result = createEvent(descriptionText: description)
	} else {
		def cmd = zwave.parse(description, commandClassVersions)
		if (cmd) {
			result = zwaveEvent(cmd)
		} else {
			result = createEvent(value: description, descriptionText: description, isStateChange: false)
		}
	}
	log.debug "Parsed '$description' to $result"
	return result
}

def sensorValueEvent(value) {
	def eventValue = value ? "${getDataValue("active")}" : "${getDataValue("inactive")}"
	def event = createEvent(name: "${getDataValue("event")}", value: eventValue, descriptionText: "$device.displayName is $eventValue", display: false)
	def composite = createEvent(name: 'composite', value: eventValue, descriptionText: "$device.displayName is $eventValue", display: true)
	return [event, composite]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
	sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) {
	sensorValueEvent(cmd.sensorValue)
}

def zwaveEvent(physicalgraph.zwave.commands.sensoralarmv1.SensorAlarmReport cmd) {
	sensorValueEvent(cmd.sensorState)
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) {
	def result = []
	if (cmd.notificationType == 0x05) {
		switch (cmd.event) {
			case 0x00:
				if (cmd.eventParametersLength && cmd.eventParameter.size() && eventParameter[0] > 0x02) {
					result << createEvent(descriptionText: "Water alarm cleared", isStateChange: true)
				} else {
					result << createEvent(name: "water", value: "dry")
				}
				break
			case 0xFE:
				result << createEvent(name: "water", value: "dry")
				break
			case 0x01:
			case 0x02:
				result << createEvent(name: "water", value: "wet")
				break
			case 0x03:
			case 0x04:
				result << createEvent(descriptionText: "Water level dropped", isStateChange: true)
				break
			case 0x05:
				result << createEvent(descriptionText: "Replace water filter", isStateChange: true)
				break
			case 0x06:
				def level = ["alarm", "alarm", "below low threshold", "above high threshold", "max"][cmd.eventParameter[0]]
				result << createEvent(descriptionText: "Water flow $level", isStateChange: true)
				break
			case 0x07:
				def level = ["alarm", "alarm", "below low threshold", "above high threshold", "max"][cmd.eventParameter[0]]
				result << createEvent(descriptionText: "Water pressure $level", isStateChange: true)
				break
		}
	} else if (cmd.notificationType == 0x04) {
		if (cmd.event <= 0x02) {
			result << createEvent(descriptionText: "$device.displayName detected overheat", isStateChange: true)
		} else if (cmd.event <= 0x04) {
			result << createEvent(descriptionText: "$device.displayName detected rapid temperature rise", isStateChange: true)
		} else {
			result << createEvent(descriptionText: "$device.displayName detected low temperature", isStateChange: true)
		}
	} else if (cmd.notificationType == 0x07) {
		if (cmd.event == 0x03) {
			result << createEvent(descriptionText: "$device.displayName covering was removed", isStateChange: true)
			result << response([
				zwave.wakeUpV1.wakeUpIntervalSet(seconds: 4 * 3600, nodeid: zwaveHubNodeId).format(),
				zwave.batteryV1.batteryGet().format()]) // *** wake-up interval set
		}
	} else if (cmd.notificationType) {
		def text = "Notification $cmd.notificationType: event ${([cmd.event] + cmd.eventParameter).join(", ")}"
		result << createEvent(name: "notification$cmd.notificationType", value: "$cmd.event", descriptionText: text, displayed: false)
	} else {
		def value = cmd.v1AlarmLevel == 255 ? "active" : cmd.v1AlarmLevel ?: "inactive"
		result << createEvent(name: "alarm $cmd.v1AlarmType", value: value, displayed: false)
	}
	result
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd) {
	def result = [createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)]
	if (!isConfigured()) {
		log.debug("late configure")
		result << response(configure())
	} else if (!state.lastbat || (new Date().time) - state.lastbat > 53 * 60 * 60 * 1000) {
		result << response(zwave.batteryV1.batteryGet().format())
		result << "delay 1200"
		result << response(zwave.wakeUpV1.wakeUpNoMoreInformation().format()) // ? delay and wakeUpNoMoreInformation - so as to not disrupt configuredParameters report by sending to sleep after BatteryReport
		} else {
		log.debug("Device has been configured sending >> wakeUpNoMoreInformation()")
		result << response(zwave.wakeUpV1.wakeUpNoMoreInformation().format())
		result << createEvent(name: 'battery', value: device.latestValue("battery"), unit: '%', isStateChange: true, displayed: false) // added event to report battery (stored latest value)
	}
	result
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [name: "battery", unit: "%"]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "${device.displayName} has a low battery"
		map.isStateChange = true
	} else {
		map.value = cmd.batteryLevel
		map.isStateChange = true // force propogation of event
	}
	state.lastbat = new Date().time
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) {
	def map = [displayed: true, value: cmd.scaledSensorValue.toString()]
	switch (cmd.sensorType) {
		case 1:
			map.name = "temperature"
			map.unit = cmd.scale == 1 ? "F" : "C"
			break;
		case 5:
			map.name = "humidity"
			map.value = cmd.scaledSensorValue.toInteger().toString()
			map.unit = cmd.scale == 0 ? "%" : ""
			break;
	}
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand(commandClassVersions)
	// log.debug "encapsulated: $encapsulatedCommand"
	if (encapsulatedCommand) {
		state.sec = 1
		zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
	// def encapsulatedCommand = cmd.encapsulatedCommand(commandClassVersions)
	def version = commandClassVersions[cmd.commandClass as Integer]
	def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
	def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
	if (encapsulatedCommand) {
		return zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	def result = null
	def encapsulatedCommand = cmd.encapsulatedCommand(commandClassVersions)
	log.debug "Command from endpoint ${cmd.sourceEndPoint}: ${encapsulatedCommand}"
	if (encapsulatedCommand) {
		result = zwaveEvent(encapsulatedCommand)
	}
	result
}

def zwaveEvent(physicalgraph.zwave.commands.multicmdv1.MultiCmdEncap cmd) {
	log.debug "MultiCmd with $numberOfCommands inner commands"
	cmd.encapsulatedCommands(commandClassVersions).collect { encapsulatedCommand ->
		zwaveEvent(encapsulatedCommand)
	}.flatten()
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	createEvent(descriptionText: "$device.displayName: $cmd", displayed: false)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	def result = []

	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	log.debug "msr: $msr"
	updateDataValue("MSR", msr)

	if (msr == "0086-0002-002D") {  // Aeon Water Sensor needs to have wakeup interval set
		result << response(zwave.wakeUpV1.wakeUpIntervalSet(seconds: 4 * 3600, nodeid: zwaveHubNodeId).format()) // *** needs .format() ??? // *** wake-up interval set
	}
	result << createEvent(descriptionText: "$device.displayName MSR: $msr", isStateChange: false)
	result
}


def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	log.debug "ConfigurationReport: $cmd"
	def nparam = "${cmd.parameterNumber}"
	def nvalue = "${cmd.scaledConfigurationValue}"
	log.debug "Processing Configuration Report: (Parameter: $nparam, Value: $nvalue)"
	def cP = [:]
	cP = state.configuredParameters
	cP.put("${nparam}", "${nvalue}")
	def cPReport = cP.collectEntries { key, value -> [key.padLeft(3,"0"), value] }
    cPReport = cPReport.sort()
    def toKeyValue = { it.collect { /$it.key=$it.value/ } join "," }
    cPReport = toKeyValue(cPReport)
	updateDataValue("configuredParameters", cPReport)
	state.configuredParameters = cP
}

def configure() {
	log.debug "${device.displayName} is configuring its settings"
	def request = []
	//	set wakeup interval - *** leave for now
	request << zwave.configurationV1.configurationSet(parameterNumber: 2, size: 1, scaledConfigurationValue: 1)
	log.debug "Requesting Sensor Values"
	request << zwave.batteryV1.batteryGet()
	request << zwave.sensorBinaryV1.sensorBinaryGet()
	log.debug "Requesting Configuration Report"
	def params = [1, 2, 3, 121]
	params.each { n ->
		request << zwave.configurationV1.configurationGet(parameterNumber: n)
	}
	setConfigured("true")
	// def checkInterval = 14400
	// sendEvent(name: "checkInterval", value: checkInterval, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
	commands(request) + ["delay 5000", zwave.wakeUpV1.wakeUpNoMoreInformation().format()] // *** increased to ensure all reports come back
}

private setConfigured(configure) {
	updateDataValue("configured", configure)
}

private isConfigured() {
	getDataValue("configured") == "true"
}

private command(physicalgraph.zwave.Command cmd) {
	if (state.sec) {
		zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
	} else {
		cmd.format()
	}
}

private commands(commands, delay=1200) { // *** delay was 200
	log.info "sending commands: ${commands}"
	delayBetween(commands.collect{ command(it) }, delay)
}