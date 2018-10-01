/**
 *  Fibaro Door/Window Sensor ZW5
 *
 *  Copyright 2016 Fibar Group S.A.
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
 *    Modified 2018 Alasdair Thin, Rapport Networks CIC
 */
metadata {
    definition(name: "Fibaro Door/Window Sensor ZW5 Flush Reporting", namespace: "rapportnetworks", author: "Alasdair Thin", ocfDeviceType: "x.com.st.d.sensor.contact") {
        capability "Sensor"
        capability "Battery"
        capability "Configuration"
        capability "Contact Sensor"
        capability "Health Check"
        capability "Tamper Alert"

        fingerprint deviceId: "0x0701", inClusters: "0x5E, 0x85, 0x59, 0x22, 0x20, 0x80, 0x70, 0x56, 0x5A, 0x7A, 0x72, 0x8E, 0x71, 0x73, 0x98, 0x2B, 0x9C, 0x30, 0x86, 0x84", outClusters: ""
    }

    simulator {

    }

    tiles(scale: 2) {
        multiAttributeTile(name: "FGK", type: "lighting", width: 6, height: 4) { //with generic type secondary control text is not displayed in Android app
            tileAttribute("device.contact", key: "PRIMARY_CONTROL") {
                attributeState("flushing", label: "Flushing", icon: "st.contact.contact.open", backgroundColor: "#e86d13")
                attributeState("full", label: "Full", icon: "st.contact.contact.closed", backgroundColor: "#00a0dc")
            }

            tileAttribute("device.tamper", key: "SECONDARY_CONTROL") {
                attributeState("detected", label: 'tampered', backgroundColor: "#00A0DC")
                attributeState("clear", label: 'tamper clear', backgroundColor: "#CCCCCC")
            }
        }

        valueTile("battery", "device.battery", inactiveLabel: false, width: 2, height: 2, decoration: "flat") {
            state "battery", label: '${currentValue}% battery', unit: ""
        }

        main "FGK"
        details(["FGK", "battery"])
    }
}

// create debug option tile
/*
    If you’re returning zwave commands from the “update” or “parse” methods you have to use the response method.
        return response(zwave.basicV1.basicGet().format())
    You can also use response like:
        def cmds = []
        cmds << zwave.basicV1.basicGet().format()
        return response(cmds)
*/

    // sendCommand ?? vs respons()

def installed() {
    setConfigured("false")
    sendEvent(name: "tamper", value: "clear", displayed: false) // should be createEvent?
    sendEvent(name: "checkInterval", value: 2 * 4 * 60 * 60 + 1 * 60 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID]) // initial value twice default Wake Up Interval
}

def updated() {
    setConfigured("false")
    def tamperValue = device.latestValue("tamper") // is this needed
    if (tamperValue == "active") {
        sendEvent(name: "tamper", value: "detected", displayed: false) // should be createEvent?
    } else if (tamperValue == "inactive") {
        sendEvent(name: "tamper", value: "clear", displayed: false)
    }
}

def configure() {
    log.debug "Executing configure" // trace
    setConfigured("false")
    def request = []
    association(request)
    wakeup(request)
    configuration(request)
    powerlevel(request)
    manufacturerSpecific(request)
    version(request)
    battery(request)
    sensorBinary(request)
    // zwave.associationGrpInfoV1.associationGroupInfoGet
    // zwave.securityV1.securityCommandsSupportedGet
    // zwavePlusInfo ?
    wakeUpNoMoreInformation(request)
    runIn(120, syncCheck)
    assembleCommands(request, 1200)
}

/*
    if (description.startsWith("Err 106") && !state.sec) {
        state.sec = 0
    }
*/

def parse(String description) {
    log.debug "Parsing '${description}'"
    def result = []
    if (description.startsWith("Err 106")) {
        if (state.sec) {
            result = createEvent(descriptionText: description, displayed: false)
        } else {
            result = createEvent(
                descriptionText: "FGK failed to complete the network security key exchange. If you are unable to receive data from it, you must remove it from your network and add it again.",
                eventType: "ALERT",
                name: "secureInclusion",
                value: "failed",
                displayed: true
            )
        }
    } else if (description == "updated") {
        return null
    } else {
        def cmd = zwave.parse(description, commandClassVersions)
        if (cmd) {
            log.debug "Parsed '${cmd}'"
            zwaveEvent(cmd)
        }
    }
}

// *** Application Events ***
// physicalgraph.zwave.commands.basicv1.BasicReport // 0x20: 1
// Alarm Sensor // 0x9C: 1

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd) { // 0x30: 2
    log.debug "Processing Sensor Binary Report: $cmd"
    def map = [:]
    map.name = 'contact'
    map.value = cmd.sensorValue ? 'full' : 'flushing'
    if (map.value == 'full') {
        map.descriptionText = "${device.displayName} is full"
    } else {
        map.descriptionText = "${device.displayName} is flushing"
    }
    createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) { // 0x71: 3
    log.debug "Processing Notification Report: $cmd" // assumed that default notification events are used (i.e. parameter 20 = 0)
    def map = [:]
    if (cmd.notificationType == 6) {
        switch (cmd.event) {
            case 22:
                map.name = 'contact'
                map.value = 'full'
                map.descriptionText = "${device.displayName} is full"
                break
            case 23:
                map.name = 'contact'
                map.value = 'flushing'
                map.descriptionText = "${device.displayName} is flushing"
                break
        }
    } else if (cmd.notificationType == 7) {
        switch (cmd.event) {
            case 0:
                map.name = 'tamper'
                map.value = 'clear'
                map.descriptionText = 'Tamper alert cleared'
                break
            case 3:
                map.name = 'tamper'
                map.value = 'detected'
                map.descriptionText = 'Tamper alert: sensor removed or covering opened'
                break
        }
    }
    createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) { // 0x70: 2
    def param = cmd.parameterNumber.toString().padLeft(3, "0")
    def paramValue = cmd.scaledConfigurationValue.toString()
    log.debug "Processing Configuration Report: (Parameter: $param, Value: $paramValue)" // trace
    def untransformed = state.configurationReport << [(param): paramValue]
    if (untransformed.size() == getConfigurationParameters().size()) {
        log.debug "All Configuration Values Reported" // trace
        updateDataValue("configurationReport", state.configurationReport.collect {
            it
        }.join(","))
    }
    state.configurationReport = untransformed
    def parameter = getConfigurationParameterValues.find{ it.parameterNumber == cmd.parameterNumber }
    if (parameter) { state."$parameter.parameterNumber".state = (parameter.scaledConfigurationValue == cmd.scaledConfigurationValue) ? "synced" : "notSynced" }
    createEvent(descriptionText: "${device.displayName} Configuration Report", isStateChange: true)
}

// *** Management Events ***
def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd) { // 0x84: 2
    def events = [createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)]
    def cmds = []
    if (!isConfigured()) {
        cmds << configure()
    } else if (!state.timeLastBatteryReport || (new Date().time) - state.timeLastBatteryReport > 7 * 24 * 60 * 60 * 1000) {
        cmds << zwave.batteryV1.batteryGet().format() // battery(result)
        cmds << "delay 1200"
        cmds << zwave.wakeUpV1.wakeUpNoMoreInformation().format() // wakeUpNoMoreInformation(result) ??? assembleCommands(request, 1200)
    } else {
        events << createEvent(name: 'battery', value: device.latestValue("battery"), unit: '%', isStateChange: true, displayed: false)
        cmds << zwave.wakeUpV1.wakeUpNoMoreInformation().format() // wakeUpNoMoreInformation(result) ??? assembleCommands(request, 1200)
    }
    [events, response(cmds)] // need to streamline above commands - but using "response", so encapsulation might be handled automatically ??? assembleCommands(request, 1200)
}

// physicalgraph.zwave.commands.applicationstatusv1.ApplicationBusy // 0x22: 1
// physicalgraph.zwave.commands.applicationstatusv1.ApplicationRejectedRequest // 0x22: 1
// ST duplication: physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport
// scene activation // 0x2B: 1

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) { // 0x80: 1
    def map = [name: 'battery', unit: '%']
    if (cmd.batteryLevel == 0xFF) {
        map.value = 1
        map.descriptionText = "${device.displayName} has a low battery"
        map.isStateChange = true
    } else {
        map.value = cmd.batteryLevel
        map.isStateChange = true
    }
    state.timeLastBatteryReport = new Date().time
    createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) { // 0x72: 2
    if (cmd.deviceIdType == 1 && cmd.deviceIdDataFormat == 1) { //serial number in binary format
        def serialNumber = ""
        cmd.deviceIdData.each {
            data - >
                serialNumber += "${String.format(" % 02 X ", data)}"
        }
        updateDataValue("serialNumber", serialNumber)
    } else {
        updateDataValue("serialNumber", 0)
    }
    createEvent(descriptionText: "${device.displayName} Device Specific Report", isStateChange: true, data: [deviceIdData: cmd.deviceIdData, deviceIdDataFormat: cmd.deviceIdDataFormat, deviceIdDataLengthIndicator: cmd.deviceIdDataLengthIndicator, deviceIdType: deviceIdType])
}

// *** Management Events ***
// ST duplication: physicalgraph.zwave.commands.versionv1.VersionReport
// Association Group Information // 0x59: 1
// Zwave Plus Info // 0x5E: 2
// Firmware Update Metadata // 0x7A: 2
// Association // 0x85: 2
// MultiChannelAssociation // 0x8E: 2

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionCommandClassReport cmd) { // 0x86: 1
    def ccValue = Integer.toHexString(cmd.requestedCommandClass).toUpperCase()
    def ccVersion = cmd.commandClassVersion
    log.debug "Processing Version Command Class Report: (Command Class: $ccValue, Version: $ccVersion)" // trace
    def untransformed = state.commandClassVersions << [(ccValue): ccVersion]
    if (untransformed.size() == getCommandClasses().size()) {
        log.debug "All Command Class Versions Reported" // trace
        updateDataValue("commandClassVersions", state.commandClassVersions.findAll {
            it.value > 0
        }.sort().collect {
            it
        }.join(","))
    }
    state.commandClassVersions = untransformed
    createEvent(descriptionText: "${device.displayName} Version Command Class Report", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpIntervalReport cmd) {
    updateDataValue("wakeupInterval", cmd.seconds)
    createEvent(descriptionText: "${device.displayName} Wake Up Interval Report", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) { // 0x5A: 1
    log.debug "${device.displayName}: received command: $cmd - device has reset itself" // trace
    createEvent(descriptionText: "${device.displayName} Device Reset Locally Notification", isStateChange: true)
}

// *** Network Protocol Events ***
def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd) { // 0x73: 1
    log.debug "Powerlevel Report: $cmd" // trace
    def powerLevel = -1 * cmd.powerLevel // omitted cmd.timeout (1-255 s)
    log.debug "Processing Powerlevel Report: (Powerlevel: $powerLevel dBm)" // trace
    updateDataValue("powerLevelReport", "powerLevel=${powerLevel}")
    createEvent(descriptionText: "${device.displayName} Powerlevel Report", isStateChange: true)
}

// *** Transport Encapsulation Events ***
def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) { // 0x56: 1
    def version = CommandClassVersions[cmd.commandClass as Integer]
    def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
    def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
    if (!encapsulatedCommand) {
        log.debug "Could not extract command from $cmd"
    } else {
        zwaveEvent(encapsulatedCommand)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) { // 0x98: 1
    state.sec = 1
    def encapsulatedCommand = cmd.encapsulatedCommand(CommandClassVersions)
    if (encapsulatedCommand) {
        return zwaveEvent(encapsulatedCommand)
    } else {
        log.warn "Unable to extract encapsulated cmd from $cmd"
        createEvent(descriptionText: cmd.toString())
    }
}

// *** Catch Event ***
def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.debug "Catchall reached for cmd: $cmd" // trace
    createEvent(descriptionText: "${device.displayName} Catchall Report: $cmd", isStateChange: true)
}

// *** Send Commands ***
private assembleCommands(commands, delay = 1200) {
    delayBetween(commands.collect {
        encapsulate(it)
    }, delay)
}

// *** need to check if command can be sent securely *** getSecureClasses()
private encapsulate(physicalgraph.zwave.Command cmd) {
    if (zwaveInfo.zw.endsWith("s") && getSecureClasses.find{ it == cmd.commandClassId }) {
        secureEncapsulate(cmd)
    } else if (zwaveInfo.cc.contains("56")){
        crc16Encapsulate(cmd)
    } else {
        cmd.format()
    }
}

private secureEncapsulate(physicalgraph.zwave.Command cmd) {
    zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crc16Encapsulate(physicalgraph.zwave.Command cmd) {
    zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

// *** Helper Methods ***
private getCommandClassVersions() { [
    0x20: 1, 0x22: 1, 0x2B: 1, 0x30: 2, 0x56: 1, 0x59: 1, 0x5A: 1, 0x5E: 2, 0x70: 2, 0x71: 3, 0x72: 2, 0x73: 1, 0x7A: 2, 0x80: 1, 0x84: 2, 0x85: 2, 0x86: 1, 0x8E: 2, 0x98: 1, 0x9C: 1
] }

private getSecureClasses() { [
    0x20, 0x2B, 0x30, 0x5A, 0x70, 0x71, 0x84, 0x85, 0x8E, 0x9C
] }

private getCommandClasses() { [
    0x20, 0x22, 0x25, 0x26, 0x27, 0x2B, 0x30, 0x31, 0x32, 0x33, 0x56, 0x59, 0x5A, 0x5E, 0x60, 0x70, 0x71, 0x72, 0x73, 0x75, 0x7A, 0x80, 0x84, 0x85, 0x86, 0x8E, 0x98, 0x9C
] }

private getConfigurationParameters() { [
    1, 2, 3, 4, 10, 11, 12, 13, 14, 15, 20, 30, 31, 50, 51, 52, 53, 54, 55, 56, 70, 71, 72
] }

private getConfigurationParameterValues() { [
    [parameterNumber: 3, size: 1, scaledConfigurationValue: 0],
    [parameterNumber: 50, size: 2, scaledConfigurationValue: 0]
] }

private setConfigured(configure) {
    updateDataValue("configured", configure)
}

private isConfigured() {
    getDataValue("configured") == "true"
}

def syncCheck() {
    createEvent(descriptionText: "${device.displayName} Executing Sync Check", isStateChange: true)

    def statesCheck = [:]
    statesCheck << getConfigurationParameterValues.each { it.parameterNumber }
    def count = 0
    statesCheck.each {
        if  (state."$statesCheck".state == "synced") count += 1
    }
    if (count == statesCheck.size() && getDataValue("")) setConfigured("true")
    getDataValue("powerLevelReport")
    getDataValue("serialNumber")
    getDataValue("")
}

// Configuration Submethods
private association(request) {
    log.debug "Setting 1st Association Group"  // trace
    request << createEvent(descriptionText: "Setting 1st Association Group", isStateChange: true)
    request << zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: [zwaveHubNodeId])
    request
}

private battery(request) {
    if (!state.timeLastBatteryReport || (new Date().time) - state.timeLastBatteryReport > 7 * 24 * 60 * 60 * 1000) {
        log.debug "Requesting Battery Report"
        request << createEvent(descriptionText: "Requesting Battery Report", isStateChange: true)
        request << zwave.batteryV1.batteryGet()
    }
    request
}

private configuration(request) {
    log.debug "Setting Configuration Parameters"  // trace
    request << createEvent(descriptionText: "Setting Configuration Parameters", isStateChange: true)
    getConfigurationParameterValues().each {
        state."$it.parameterNumber".state = "notSynced"
        request << zwave.configurationV1.configurationSet(it)
    }
    log.debug "Requesting Configuration Report" // trace
    request << createEvent(descriptionText: "Requesting Configuration Report", isStateChange: true)
    state.configurationReport = [:]
    getConfigurationParameters().each {
        request << zwave.configurationV1.configurationGet(parameterNumber: it)
    }
    request
}

private manufacturerSpecific(request) {
    // if (!getDataValue("serialNumber")) { // leave for now so as to reset format of serialNumber
        log.debug "Requesting Device Specific Report"
        request << createEvent(descriptionText: "Requesting Device Specific Report", isStateChange: true)
        request << zwave.manufacturerSpecificV2.deviceSpecificGet(deviceIdType: 1)
    // }
    request
}

private powerlevel(request) {
    if (!state.timeLastBatteryReport || (new Date().time) - state.timeLastBatteryReport > 7 * 24 * 60 * 60 * 1000) {
        log.debug "Requesting Powerlevel Report"
        request << createEvent(descriptionText: "Requesting Powerlevel Report", isStateChange: true)
        request << zwave.powerlevelV1.powerlevelGet()
    }
    request
}

private sensorBinary(request) {
    log.debug "Requesting Binary Sensor Report"
    request << createEvent(descriptionText: "Requesting Binary Sensor Report", isStateChange: true)
    request << zwave.sensorBinaryV2.sensorBinaryGet()
    request
}

private version(request) {
    if (!getDataValue("commandClassVersions")) {
        log.debug "Requesting Command Class Report" // trace
        request << createEvent(descriptionText: "Requesting Command Class Report", isStateChange: true)
        state.commandClassVersions = [:]
        getCommandClasses().each {
            request << zwave.versionV1.versionCommandClassGet(requestedCommandClass: it)
        }
    }
    request
}

private wakeup(request) {
    log.debug "Setting Wake Up Interval" // trace
    request << createEvent(descriptionText: "Setting Wake Up Interval", isStateChange: true)
    request << zwave.wakeUpV2.wakeUpIntervalSet(seconds: 18 * 60 * 60, nodeid: zwaveHubNodeId)
    sendEvent(name: "checkInterval", value: 2 * 18 * 60 * 60 + 1 * 60 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])

    log.debug "Requesting Wake Up Interval Report"
    request << createEvent(descriptionText: "Requesting Wake Up Interval Report", isStateChange: true)
    request << zwave.wakeupV2.wakeUpIntervalGet()
    request
}

private wakeUpNoMoreInformation(request) {
    request << zwave.wakeUpV2.wakeUpNoMoreInformation()
    request
}