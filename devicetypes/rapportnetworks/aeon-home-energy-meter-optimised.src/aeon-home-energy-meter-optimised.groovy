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
 *  Aeon Home Energy Meter
 *
 *  Author: SmartThings
 *
 *  Date: 2013-05-30
 *
 *	Optimised by Alasdair Thin, Rapport Network CIC, 2018.
 */
metadata {
	definition (name: "Aeon Home Energy Meter Optimised", namespace: "rapportnetworks", author: "Alasdair Thin") {
		capability "Energy Meter"
		capability "Power Meter"
		capability "Configuration"
		capability "Sensor"

		command "reset"

		fingerprint mfr: "0086", model: "001C"
		fingerprint mfr: "0086", prod: "0002", model: "001C"
		fingerprint mfr: "0086", model: "005F"
		fingerprint mfr: "0086", prod: "0002", model: "005F"
	}

	// simulator metadata
	simulator {
		for (int i = 0; i <= 10000; i += 1000) {
			status "power  ${i} W": new physicalgraph.zwave.Zwave().meterV1.meterReport(
				scaledMeterValue: i, precision: 3, meterType: 4, scale: 2, size: 4).incomingMessage()
		}
		for (int i = 0; i <= 100; i += 10) {
			status "energy  ${i} kWh": new physicalgraph.zwave.Zwave().meterV1.meterReport(
				scaledMeterValue: i, precision: 3, meterType: 0, scale: 0, size: 4).incomingMessage()
		}
	}

	// tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name:"power", type: "generic", width: 6, height: 4){
			tileAttribute("device.power", key: "PRIMARY_CONTROL") {
				attributeState("default", label:'${currentValue} W')
			}
			tileAttribute("device.energy", key: "SECONDARY_CONTROL") {
				attributeState("default", label:'${currentValue} kWh')
			}
		}
		standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat",width: 2, height: 2) {
			state "default", label:'reset kWh', action:"reset"
		}
		standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat",width: 2, height: 2) {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("configure", "device.power", inactiveLabel: false, decoration: "flat",width: 2, height: 2) {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}

		main (["power","energy"])
		details(["power","energy", "reset","refresh", "configure"])
	}
}

def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, [0x31: 1, 0x32: 1, 0x60: 3, 0x70: 1]) // *** added v1 for cc 70
	if (cmd) {
		result = zwaveEvent(cmd)
	}
	if (result) {
		result = createEvent(result)
		log.debug "Parse returned ${result?.descriptionText}"
		return result
	} else {
	}
}

def zwaveEvent(physicalgraph.zwave.commands.meterv1.MeterReport cmd) {
	if (cmd.scale == 2) {
		[name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W"]
	} else if (cmd.scale == 0) {
		[name: "energy", value: cmd.scaledMeterValue, unit: "kWh"]
	} else {
		[name: "volts", value: cmd.scaledMeterValue, unit: "V"]
	}
}

// *** added processing of configuration report command
def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	log.debug "ConfigurationReport: $cmd"
	def result = []
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
	return result
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def refresh() {
	delayBetween([
		zwave.meterV2.meterGet(scale: 0).format(),
		zwave.meterV2.meterGet(scale: 2).format()
	])
}

def reset() {
	// No V1 available
	return [
		zwave.meterV2.meterReset().format(),
		zwave.meterV2.meterGet(scale: 0).format()
	]
}

def configure() {
	def cmd = delayBetween([
		zwave.configurationV1.configurationSet(parameterNumber: 2, size: 1, scaledConfigurationValue: 0).format(),   // report absolute power, energy
		zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: 1).format(),   // enable selective reporting
		zwave.configurationV1.configurationSet(parameterNumber: 4, size: 2, scaledConfigurationValue: 10).format(),   // absolute threshold watts to trigger report (try 10W)
		zwave.configurationV1.configurationSet(parameterNumber: 8, size: 1, scaledConfigurationValue: 10).format(),   // relative threshold watts to trigger report (try 10%)
		zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 2).format(),   // combined power in watts
		zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 5).format(), // every 5 seconds
		zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 1).format(),   // combined energy in kWh
		zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 300).format(), // every 5 minutes
		zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0).format(),    // no third report
		zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: 300).format() // every 5 min
	], 1000)
	log.debug cmd
	cmd

	// get configuration report
	state.configuredParameters = [:] // *** define state variable (map) to store configuration reports received from sensor
	def request = []
	def params = [2, 3, 4, 8, 101, 102, 103, 111, 112, 113]
	params.each { n ->
		request << zwave.configurationV1.configurationGet(parameterNumber: n).format()
	}
	def report = delayBetween(request, 1000)
	log.debug "Requesting Configuration Report: ${report}"
	report
}