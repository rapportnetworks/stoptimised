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

    // configuration() - create lists - events, commands
    // (installed) updated & configure call configuration()
    // events, commands returned
    // (updatd), installed() goes through lists and uses createEvent(event.it) and response(commands) - don't need to iterate
    // configure() uses sendEvent(event.it) and commands (without response helper)


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
    sendEvent(description: "Configuration Command Sent")
    // for Sleepy devices (on battery)
    setConfigured("false") // will call parseConfigure()
    // for Listening devices - call "configuration"
    // commandConfigure()


    // move methods to configuration() - then get wrapped in response() when called from WakeUpNotification
    log.debug "Executing configure" // trace
    setConfigured("false")

    def comands = []
    def events = []
    association(commands, events)
    wakeup(commands, events)
    configuration(commands, events)
    powerlevel(commands, events)
    manufacturerSpecific(commands, events)
    version(commands, events)
    battery(commands, events)
    sensorBinary(commands, events)
    // zwave.associationGrpInfoV1.associationGroupInfoGet
    // zwave.securityV1.securityCommandsSupportedGet
    // zwavePlusInfo ?
    wakeUpNoMoreInformation(commands, events)

    runIn(120, syncCheck)
    assembleCommands(request, 1200)

    return (commands, events)



    build("configuration", "command")
}

// returned via "parse" - build("configuration", "parse")

private build(method, caller) {
    def (List commands, List events) = "${method}()"
    def result = []
    if (caller == "parse") {
        result = reponse(assembleCommands(commands)) // need to use reponse() handler to ensure
        events.each { result << createEvent(it) }
    } else {
        result = assembleCommands(commands)
        events.each { sendEvent(it) } // need to use sendEvent rather than createEvent
    }
    result
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

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) { // 0x70: 2
    def param = cmd.parameterNumber.toString().padLeft(3, "0")
    def paramValue = cmd.scaledConfigurationValue.toString()
    logger("Processing Configuration Report: (Parameter: $param, Value: $paramValue)", "info")
    def untransformed = state.configurationReport << [(param): paramValue]
    if (untransformed.size() == configurationParameters().size()) {
        log.debug "All Configuration Values Reported" // trace
        updateDataValue("configurationReport", state.configurationReport.collect {
            it
        }.join(","))
    }
    state.configurationReport = untransformed
    def parameter = getConfigurationParameterValues.find{ it.parameterNumber == cmd.parameterNumber }
    if (parameter) { state."$parameter.parameterNumber".state = (parameter.scaledConfigurationValue == cmd.scaledConfigurationValue) ? "synced" : "notSynced" }
    createEvent(descriptionText: "Configuration Report", isStateChange: true)
}

// *** Management Events ***
def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd) { // 0x84: 2
    def events = [createEvent(descriptionText: "Wake Up Notification", isStateChange: false)]
    def cmds = []
    if (!isConfigured()) {
        cmds << configure() // build("configuration", "parse") - this will upset below - response() [events, response(cmds)]
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
    createEvent(descriptionText: "Device Specific Report", isStateChange: true, data: [deviceIdData: cmd.deviceIdData, deviceIdDataFormat: cmd.deviceIdDataFormat, deviceIdDataLengthIndicator: cmd.deviceIdDataLengthIndicator, deviceIdType: deviceIdType])
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
    logger("Processing Version Command Class Report: (Command Class: $ccValue, Version: $ccVersion)", "info")
    def untransformed = state.commandClassVersions << [(ccValue): ccVersion]
    if (untransformed.size() == commandClasses().size()) {
        log.debug "All Command Class Versions Reported" // trace
        updateDataValue("commandClassVersions", state.commandClassVersions.findAll {
            it.value > 0
        }.sort().collect { // shouldn't itbe collectEntries ?? - what does join do?
            it
        }.join(","))
    }
    state.commandClassVersions = untransformed
    createEvent(descriptionText: "Version Command Class Report", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpIntervalReport cmd) {
    updateDataValue("wakeupInterval", cmd.seconds)
    createEvent(descriptionText: "Wake Up Interval Report", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) { // 0x5A: 1
    createEvent(descriptionText: "Device Reset Locally Notification", isStateChange: true)
}

// *** Network Protocol Events ***
def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd) { // 0x73: 1
    def powerLevel = 10.power(cmd.powerLevel / 10).multiply(1000).round()
    updateDataValue("powerLevelReport", "$powerLevel") //omitted cmd.timeout (1-255 s)
    createEvent(descriptionText: "Powerlevel Report", isStateChange: true)
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
        logger("Unable to extract encapsulated cmd from $cmd", "warn")
        createEvent(descriptionText: cmd.toString())
    }
}

// *** Catch Event ***
def zwaveEvent(physicalgraph.zwave.Command cmd) {
    logger("Catchall reached for cmd: $cmd", "debug")
    createEvent(descriptionText: "Catchall Report: $cmd", isStateChange: true)
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

// Configuration Submethods
private association(commands, events) {
    commands << zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: [zwaveHubNodeId])
    events << [descriptionText: "Setting 1st Association Group", isStateChange: true]
    commands, events
}

private battery(commands, events) {
    if (!state.timeLastBatteryReport || (new Date().time) - state.timeLastBatteryReport > 604_800 * 1000) {
        commands << zwave.batteryV1.batteryGet()
        events << [descriptionText: "Requesting Battery Report", isStateChange: true]
    }
    commands, events
}

private configuration(commands, events) {
    events << [descriptionText: "Setting Configuration Parameters", isStateChange: true]
    getConfigurationParameterValues().each {
        state."$it.parameterNumber".state = "notSynced"
        commands << zwave.configurationV1.configurationSet(it)
    }
    events << [descriptionText: "Requesting Configuration Report", isStateChange: true]
    state.configurationReport = [:]
    getConfigurationParameters().each {
        commands << zwave.configurationV1.configurationGet(parameterNumber: it)
    }
    commands, events
}

private manufacturerSpecific(commands, events) {
    // if (!getDataValue("serialNumber")) { // leave for now so as to reset format of serialNumber
        commands << zwave.manufacturerSpecificV2.deviceSpecificGet(deviceIdType: 1)
        events << [descriptionText: "Requesting Device Specific Report", isStateChange: true]
    // }
    commands, events
}

private powerlevel(commands, events) {
    if (!state.timeLastBatteryReport || (new Date().time) - state.timeLastBatteryReport > 604_800 * 1000) {
        commands << zwave.powerlevelV1.powerlevelGet()
        events << [descriptionText: "Requesting Powerlevel Report", isStateChange: true]
    }
    commands, events
}

private sensorBinary(commands, events) {
    commands << zwave.sensorBinaryV2.sensorBinaryGet()
    events << [descriptionText: "Requesting Binary Sensor Report", isStateChange: true]
    commands, events
}

private version(commands, events) {
    if (!getDataValue("commandClassVersions")) {
        events << [descriptionText: "Requesting Command Class Report", isStateChange: true]
        state.commandClassVersions = [:]
        getCommandClasses().each {
            commands << zwave.versionV1.versionCommandClassGet(requestedCommandClass: it)
        }
    }
    commands, events
}

private wakeup(commands, events) {
    commands << zwave.wakeUpV2.wakeUpIntervalSet(seconds: wakeUpInterval, nodeid: zwaveHubNodeId)
    events << [descriptionText: "Setting Wake Up Interval", isStateChange: true]
    events << [name: "checkInterval", value: checkInterval, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID]]
    commands << zwave.wakeupV2.wakeUpIntervalGet()
    events << [descriptionText: "Requesting Wake Up Interval Report", isStateChange: true]
    commands, events
}

private wakeUpNoMoreInformation(commands, events) {
    commands << zwave.wakeUpV2.wakeUpNoMoreInformation()
    commands, events
}