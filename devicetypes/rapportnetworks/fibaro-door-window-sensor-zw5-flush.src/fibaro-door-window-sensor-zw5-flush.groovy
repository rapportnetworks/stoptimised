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

        // Standard (Capability) Attributes

        // Custom Attributes

        // Custom Commands
        command "resetTamper"
        // command "sync"
        command "test"

        // Fingerprints
        // fingerprint mfr: "010F", prod: "0B00", model: "1001"
        fingerprint deviceId: "0x0701", inClusters: "0x5E, 0x85, 0x59, 0x22, 0x20, 0x80, 0x70, 0x56, 0x5A, 0x7A, 0x72, 0x8E, 0x71, 0x73, 0x98, 0x2B, 0x9C, 0x30, 0x86, 0x84", outClusters: ""
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
        standardTile("tamper", "device.tamper", decoration: "flat", width: 2, height: 2) {
            state("default", label:"tampered", icon:"st.security.alarm.alarm", backgroundColor:"#FF6600", action: "resetTamper")
            state("clear", label:"clear", icon:"st.security.alarm.clear", backgroundColor:"#ffffff")
        }
        standardTile("test", "device.test", decoration: "flat", width: 2, height: 2) {
            state "default", label:'Test', action:"test"
        }
        main "FGK"
        details(["FGK", "battery", "tamper", "test"])
    }
    preferences {
        // create debug option
    }
}

def installed() {
    setConfigured("false")
    sendEvent(name: "tamper", value: "clear", displayed: false) // should be createEvent?
    sendEvent(name: "checkInterval", value: 2 * 4 * 60 * 60 + 1 * 60 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID]) // initial value twice default Wake Up Interval
}

def updated() {

    if (!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 2000) {
        state.updatedLastRanAt = now()

        setConfigured("false")
        def tamperValue = device.latestValue("tamper") // is this needed
        if (tamperValue == "active") {
            sendEvent(name: "tamper", value: "detected", displayed: false) // should be createEvent?
        }
        else if (tamperValue == "inactive") {
            sendEvent(name: "tamper", value: "clear", displayed: false)
        }

        // for Sleepy devices (on battery)
        setConfigured("false") // will call parseConfigure()
        // for Listening devices - call "configuration"
        // commandConfigure()

        // move methods to configuration() - then get wrapped in response() when called from WakeUpNotification
        log.debug "Executing configure" // trace
        setConfigured("false")

        def commands = []
        association(commands) // set 1st Association Group
        wakeup(commands) // set & get Wake Up Interval
        configuration(commands) // set & get Configuration Values
        powerlevel(commands) // get Powerlevel Report
        manufacturerSpecific(commands) // get Device Specific Report
        battery(commands) // get Battery Report
        sensorBinary(commands) // get Sensor Binary Report
        // zwave.associationGrpInfoV1.associationGroupInfoGet
        // zwave.securityV1.securityCommandsSupportedGet
        wakeUpNoMoreInformation(commands) // send Wake Up No More Information

        // runIn(120, syncCheck)
        response(assembleCommands(commands))
    }
    else {
        logger("updated(): Ran within last 2 seconds so aborting.","debug")
    }
}

def configure() { // just send to updated() - only value here is that tile press or smartapp can send configuration command to device to trigger it - it doesn't get called if a device is replaced
    sendEvent(descriptionText: "Configuration Command Received", displayed: false)
    updated()
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

/*****************************************************************************************************************
* Application Events
*****************************************************************************************************************/
def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) { // 0x70: 2
    def param = cmd.parameterNumber.toString().padLeft(3, "0")
    def paramValue = cmd.scaledConfigurationValue
    state.configurationReport << [(param): paramValue]
    if (state.configurationReport.size() == configurationParameters().size()) {
        updateDataValue("configuredParameters", state.configurationReport.collect { it }.join(","))
    }
    def parameter = configurationParameterValues.find{ it.parameterNumber == cmd.parameterNumber }
    if (parameter) { state."${parameter.parameterNumber}".state = (parameter.scaledConfigurationValue == cmd.scaledConfigurationValue) ? "synced" : "notSynced" }
    createEvent(descriptionText: 'Configuration Report', displayed: false, isStateChange: true, data: [name: 'Configuration Report', parameterNumber: param, scaledConfigurationValue: paramValue, state: state."${parameter.parameterNumber}".state])
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) { // 0x71: 3
    logger("Processing Notification Report: $cmd", "info") // assumed that default notification events are used (i.e. parameter 20 = 0)
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

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd) { // 0x30: 2
    logger("Processing Sensor Binary Report: $cmd", "info")
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

// physicalgraph.zwave.commands.basicv1.BasicReport // 0x20: 1
// Alarm Sensor // 0x9C: 1

/*****************************************************************************************************************
* Management Events
*****************************************************************************************************************/
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
    state.timeLastBatteryReport = now()
    createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) { // 0x5A: 1
    createEvent(descriptionText: "Device Reset Locally Notification", displayed: false, isStateChange: true)
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
    createEvent(descriptionText: "Device Specific Report", isStateChange: true, data: [deviceIdData: cmd.deviceIdData, deviceIdDataFormat: cmd.deviceIdDataFormat, deviceIdDataLengthIndicator: cmd.deviceIdDataLengthIndicator, deviceIdType: deviceIdType])
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionCommandClassReport cmd) { // 0x86: 1
    def ccValue = Integer.toHexString(cmd.requestedCommandClass).toUpperCase()
    def ccVersion = cmd.commandClassVersion
    state.commandClassVersions << [(ccValue): ccVersion]
    if (state.commandClassVersions.size() == commandClasses().size()) {
        updateDataValue("commandClassVersions", state.commandClassVersions.findAll { it.value > 0 }.sort().collect { it }.join(","))
    }
    createEvent(descriptionText: 'Version Command Class Report', displayed: false, isStateChange: true, data: [name: 'Version Command Class Report', requestedCommandClass: ccValue, commandClassVersion: ccVersion])
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpIntervalReport cmd) {
    updateDataValue("wakeupInterval", cmd.seconds)
    createEvent(descriptionText: "Wake Up Interval Report", displayed: false, isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd) { // 0x84: 2
    def events = [createEvent(descriptionText: "Wake Up Notification", isStateChange: false)]
    def commands = []
    if (!isConfigured()) {
        commands << configure() // build("configuration", "parse") - this will upset below - response() [events, response(cmds)]
    } else if (state.testPending) {
        testRun(commands)
    } else if (!state.timeLastBatteryReport || now() > state.timeLastBatteryReport + batteryRefreshPeriod()) {
        battery(commands) // get Battery Report
        powerlevel(commands) // get Powerlevel Report - ? should come from the Battery Report handler
        wakeUpNoMoreInformation(commands) //- ? should come from the Battery Report/Powerlevel handlers
    } else {
        events << createEvent(name: 'battery', value: device.latestValue("battery"), unit: '%', isStateChange: true, displayed: false)
        wakeUpNoMoreInformation(commands)
    }
    [response(assembleCommands(commands)), events]
}

// physicalgraph.zwave.commands.applicationstatusv1.ApplicationBusy // 0x22: 1 - could use to check status - listening devices? - used in Energy meter
// physicalgraph.zwave.commands.applicationstatusv1.ApplicationRejectedRequest // 0x22: 1 - could use to check status - listening devices? - used in Energy meter
// Association // 0x85: 2 - could request Association Report to check set properly
// Association Group Information // 0x59: 1
// Firmware Update Metadata // 0x7A: 2
// physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport - omit as it duplicates ST information
// MultiChannelAssociation // 0x8E: 2
// scene activation // 0x2B: 1
// physicalgraph.zwave.commands.versionv1.VersionReport - omit as it duplicates ST information
// Zwave Plus Info // 0x5E: 2 - not supported by ST

/*****************************************************************************************************************
* Network Protocol Events
*****************************************************************************************************************/
def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd) { // 0x73: 1
    def powerLevel = 10.power(cmd.powerLevel / 10).multiply(1000).round()
    updateDataValue("powerLevelReport", "$powerLevel") //omitted cmd.timeout (1-255 s)
    createEvent(descriptionText: "Powerlevel Report", displayed: false, isStateChange: true)
}

/*****************************************************************************************************************
* Transport Encapsulation Events
*****************************************************************************************************************/
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
        logger("Unable to extract encapsulated cmd from $cmd", "warn")
        createEvent(descriptionText: cmd.toString())
    }
}

/*****************************************************************************************************************
* Catch Event
*****************************************************************************************************************/
def zwaveEvent(physicalgraph.zwave.Command cmd) {
    logger("Catchall reached for cmd: $cmd", "debug")
    createEvent(descriptionText: "Catchall Report: $cmd", isStateChange: true)
}

/*****************************************************************************************************************
* Send Commands
*****************************************************************************************************************/
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

/*****************************************************************************************************************
 *  Custom Commands:
 *****************************************************************************************************************/
def resetTamper() {
    logger("resetTamper(): Resetting tamper alarm.","info")
    sendEvent(name: "tamper", value: "clear", descriptionText: "Tamper alarm cleared", displayed: true)
}

/*****************************************************************************************************************
* Test Command - called from 'test' tile
*****************************************************************************************************************/
private test() {
    logger("test()","trace")
    state.testPending = true
    // immediate test actions:
    // def cmds = []
    // cmds << ...
    // if (cmds) sendSequence(cmds,200)
}

//  testRun() - Async Testing method. Called when device wakes up and state.testPending = true.
private testRun(commands) {
    logger("testRun()","trace")
    state.testPending = false
    version(commands) // get Command Class Versions Report
    commands
}
/*
private sendSequence(commands, delay = 200) {
    sendHubCommand(commands.collect{ response(it) }, delay)
}
*/
/*****************************************************************************************************************
* Helper Methods
*****************************************************************************************************************/
private commandClassVersions() { [
    0x20: 1, 0x22: 1, 0x2B: 1, 0x30: 2, 0x56: 1, 0x59: 1, 0x5A: 1, 0x5E: 2, 0x70: 2, 0x71: 3, 0x72: 2, 0x73: 1, 0x7A: 2, 0x80: 1, 0x84: 2, 0x85: 2, 0x86: 1, 0x8E: 2, 0x98: 1, 0x9C: 1
] }

private secureCommandClasses() { [
    0x20, 0x2B, 0x30, 0x5A, 0x70, 0x71, 0x84, 0x85, 0x8E, 0x9C
] }

private commandClasses() { [
    0x20, 0x22, 0x25, 0x26, 0x27, 0x2B, 0x30, 0x31, 0x32, 0x33, 0x56, 0x59, 0x5A, 0x5E, 0x60, 0x70, 0x71, 0x72, 0x73, 0x75, 0x7A, 0x80, 0x84, 0x85, 0x86, 0x8E, 0x98, 0x9C
] }

private configurationParameters() { [
    1, 2, 3, 4, 10, 11, 12, 13, 14, 15, 20, 30, 31, 50, 51, 52, 53, 54, 55, 56, 70, 71, 72
] }

private configurationParameterValues() { [
    [parameterNumber: 3, size: 1, scaledConfigurationValue: 0],
    [parameterNumber: 50, size: 2, scaledConfigurationValue: 0]
] }

private wakeUpInterval() {
    64_800
}

private checkInterval() {
    132_000
}

private batteryRefreshPeriod() {
    604_800 * 1000
}

private setConfigured(configure) {
    updateDataValue("configured", configure)
}

private isConfigured() {
    getDataValue("configured") == "true"
}

private logging(msg, level = "debug") {
    switch(level) {
        case "error":
            if (state.loggingLevelIDE >= 1) log.error msg
            break
        case "warn":
            if (state.loggingLevelIDE >= 2) log.warn msg
            break
        case "info":
            if (state.loggingLevelIDE >= 3) log.info msg
            break
        case "debug":
            if (state.loggingLevelIDE >= 4) log.debug msg
            break
        case "trace":
            if (state.loggingLevelIDE >= 5) log.trace msg
            break
        default:
            log.debug msg
            break
    }
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

/*****************************************************************************************************************
* Configuration Submethods
*****************************************************************************************************************/
private association(commands) {
    sendEvent(descriptionText: "Setting 1st Association Group", displayed: false)
    commands << zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: [zwaveHubNodeId])
}

private battery(commands) {
    sendEvent(descriptionText: "Requesting Battery Report", displayed: false)
    commands << zwave.batteryV1.batteryGet()
}

private configuration(commands) {
    sendEvent(descriptionText: "Setting Configuration Parameters", displayed: false)
    configurationParameterValues().each {
        state."$it.parameterNumber".state = "notSynced"
        commands << zwave.configurationV1.configurationSet(it)
    }
    sendEvent(descriptionText: "Requesting Configuration Report", displayed: false)
    state.configurationReport = [:]
    configurationParameters().each {
        commands << zwave.configurationV1.configurationGet(parameterNumber: it)
    }
    commands
}

private manufacturerSpecific(commands) {
    sendEvent(descriptionText: "Requesting Device Specific Report", displayed: false)
    commands << zwave.manufacturerSpecificV2.deviceSpecificGet(deviceIdType: 1)
}

private powerlevel(commands) {
    sendEvent(descriptionText: "Requesting Powerlevel Report", displayed: false)
    commands << zwave.powerlevelV1.powerlevelGet()
}

private sensorBinary(commands) {
    sendEvent(descriptionText: "Requesting Binary Sensor Report", displayed: false)
    commands << zwave.sensorBinaryV2.sensorBinaryGet()
}

private version(commands) {
    if (!getDataValue("commandClassVersions")) {
        sendEvent(descriptionText: "Requesting Command Class Report", displayed: false)
        state.commandClassVersions = [:]
        getCommandClasses().each {
            commands << zwave.versionV1.versionCommandClassGet(requestedCommandClass: it)
        }
    }
    commands
}

private wakeup(commands) {
    sendEvent(descriptionText: "Setting Wake Up Interval", displayed: false)
    commands << zwave.wakeUpV2.wakeUpIntervalSet(seconds: wakeUpInterval(), nodeid: zwaveHubNodeId)
    sendEvent(descriptionText: "Requesting Wake Up Interval Report", displayed: false)
    commands << zwave.wakeupV2.wakeUpIntervalGet()
    sendEvent(name: "checkInterval", value: checkInterval(), displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
    commands
}

private wakeUpNoMoreInformation(commands) {
    sendEvent(descriptionText: "Sending Wake Up No More Information", displayed: false)
    commands << zwave.wakeUpV2.wakeUpNoMoreInformation()
}