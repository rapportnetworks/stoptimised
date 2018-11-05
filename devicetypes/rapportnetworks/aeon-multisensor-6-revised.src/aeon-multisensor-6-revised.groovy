/**
 *  Copyright 2018 Alasdair Thin
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
 *	Based on ***
 */

metadata {
    definition(name: "Aeon Multisensor 6 Revised", namespace: "rapportnetworks", author: "Alasdair Thin") {
        capability "Sensor"
        capability "Battery" // attribute "battery", "number"
        capability "Configuration" // command "configure"
        capability "Health Check" // attribute "checkInterval", "number"; attribute "DeviceWatch-DeviceStatus", "string"; attribute "healthStatus", "?"; command "ping"
        capability "Illuminance Measurement" // attribute "illuminance", "number"
        capability "Motion Sensor" // attribute "motion", "enum", ["inactive", "active"]
        capability "Power Source" // attribute "powerSource", "enum", ["battery", "dc", "mains", "unknown"]
        capability "Relative Humidity Measurement" // attribute "humidity", "number"
        capability "Tamper Alert" // attribute "tamper", "enum", ["detected", "clear"]
        capability "Temperature Measurement" // attribute "temperature", "number"
        capability "Ultraviolet Index" // attribute "ultravioletIndex", "number"

        // Custom Attributes
        attribute "batteryStatus", "string"     // Indicates DC-power or battery %.
        attribute "logMessage", "string"        // Important log messages.
        attribute "syncPending", "number"       // Number of config items that need to be synced with the physical device.

        // Custom Commands:
        command "resetTamper"
        command "syncAll"
        command "test"

        fingerprint mfr: "0086", prod: "0102", model: "0064", deviceJoinName: "Aeotec MultiSensor 6"
    }

    tiles(scale: 2) {
        /*
        multiAttributeTile(name: "motion", type: "generic", width: 6, height: 4) {
            tileAttribute("device.motion", key: "PRIMARY_CONTROL") {
                attributeState "active", label: 'motion', icon: "st.motion.motion.active", backgroundColor: "#00A0DC"
                attributeState "inactive", label: 'no motion', icon: "st.motion.motion.inactive", backgroundColor: "#cccccc"
            }
        }
        */
        standardTile("motion", "device.motion", inactiveLabel: false, width: 2, height: 2) {
            state "active", label: 'motion', icon: "st.motion.motion.active", backgroundColor: "#00A0DC"
            state "inactive", label: 'no motion', icon: "st.motion.motion.inactive", backgroundColor: "#cccccc"
        }

        valueTile("temperature", "device.temperature", inactiveLabel: false, width: 2, height: 2) {
            state "temperature", label: '${currentValue}°C', unit: "°C",
                backgroundColors: [
                    [value: 0, color: "#153591"],
                    [value: 7, color: "#1e9cbb"],
                    [value: 15, color: "#90d2a7"],
                    [value: 23, color: "#44b621"],
                    [value: 29, color: "#f1d801"],
                    [value: 33, color: "#d04e00"],
                    [value: 37, color: "#bc2323"]
                ],  defaultState: true
        }
        valueTile("humidity", "device.humidity", inactiveLabel: false, width: 2, height: 2) {
            state "humidity", label: '${currentValue} % humidity', unit: "% humidity", defaultState: true
        }
        valueTile("illuminance", "device.illuminance", inactiveLabel: false, width: 2, height: 2) {
            state "illuminance", label: '${currentValue} lux', unit: "", defaultState: true
        }
        valueTile("ultravioletIndex", "device.ultravioletIndex", inactiveLabel: false, width: 2, height: 2) {
            state "ultravioletIndex", label: '${currentValue} UV index', unit: "", defaultState: true
        }
        valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "battery", label: '${currentValue}% battery', unit: "", defaultState: true
        }
        valueTile("batteryStatus", "device.batteryStatus", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "batteryStatus", label: '${currentValue}', unit: "", defaultState: true
        }
        valueTile("powerSource", "device.powerSource", height: 2, width: 2, decoration: "flat") {
            state "powerSource", label: '${currentValue} powered', backgroundColor: "#ffffff", defaultState: true
        }
        standardTile("tamper", "device.tamper", height: 2, width: 2, decoration: "flat") {
            state "clear", label: 'tamper clear', backgroundColor: "#ffffff", defaultState: true, action: "resetTamper" // *** remove action after testing
            state "detected", label: 'tampered', icon: 'st.secondary.tools', action: "resetTamper", backgroundColor: "#ff0000"
        }
        valueTile("syncPending", "device.syncPending", height: 2, width: 2, decoration: "flat") {
            state "syncPending", label: '${currentValue} to sync', action: "syncAll", backgroundColor: "#ffffff", defaultState: true
        }
        valueTile("logMessage", "device.logMessage", height: 2, width: 2, decoration: "flat") {
            state "clear", label: '${currentValue}', backgroundColor: "#ffffff", defaultState: true
        }
        standardTile("configure", "device.configure", height: 2, width: 2, decoration: "flat") {
            state "configure", label: 'configure', icon: 'st.secondary.tools', action: "configure", backgroundColor: "#ffffff", defaultState: true
        }
        standardTile("test", "device.test", height: 2, width: 2, decoration: "flat") {
            state "test", label: 'test', icon: 'st.secondary.tools', action: "test", backgroundColor: "#ffffff", defaultState: true
        }
        main(["motion", "temperature", "humidity", "illuminance", "ultravioletIndex"])
        details(["motion", "temperature", "humidity", "illuminance", "ultravioletIndex", "batteryStatus", "tamper", "syncPending", "logMessage", "configure", "test"])
    }

    preferences {
        if (configurationHandler()) input(name: 'paraGeneral', title: 'GENERAL', description: 'Device handler settings.', type: 'paragraph', element: 'paragraph')

        if ('deviceUse' in configurationHandler()) {
            def uses = configurationUseStates().keySet().sort() as List
            def defaultUse = configurationUseStates().find { it.value.default }.key
            input(name: 'configDeviceUse', title: 'What type of sensor do you want to use this device for?', type: 'enum', options: uses, defaultValue: defaultUse, required: true, displayDuringSetup: true)
        }

        if ('autoResetTamperDelay' in configurationHandler()) input(name: 'configAutoResetTamperDelay', title: 'Auto-Reset Tamper Alarm:\n' + 'Automatically reset tamper alarms after this time delay.\n' + 'Values: 0 = Auto-reset Disabled\n' + '1-86400 = Delay (s)\n' + 'Default Value: 30s', type: 'number', defaultValue: 30, required: false)

        if ('loggingLevelIDE' in configurationHandler()) input(name: 'configLoggingLevelIDE', title: 'IDE Live Logging Level: Messages with this level and higher will be logged to the IDE.', type: 'enum', options: [0: 'None', 1: 'Error', 2: 'Warning', 3: 'Info', 4: 'Debug', 5: 'Trace'], defaultValue: 3, required: false)

        if ('loggingLevelDevice' in configurationHandler()) input(name: 'configLoggingLevelDevice', title: 'Device Logging Level: Messages with this level and higher will be logged to the logMessage attribute.', type: 'enum', options: [0: 'None', 1: 'Error', 2: 'Warning'], defaultValue: 2, required: false)

        if ('wakeUpInterval' in configurationHandler()) input(name: 'configWakeUpInterval', title: 'WAKE UP INTERVAL:\n' + 'The device will wake up after each defined time interval to sync configuration parameters, ' + 'associations and settings.\n' + 'Values: 5-86399 = Interval (s)\n' + 'Default Value: 4000 (every 66 minutes)', type: 'number', defaultValue: 4000, required: false)

        if (configurationUser()) generatePrefsParams()
    }
}

/*****************************************************************************************************************
 *  SmartThings System Methods
 *****************************************************************************************************************/
def installed() {
    state.loggingLevelIDE = 5; state.loggingLevelDevice = 2
    logger('installed(): Performing initial setup', 'debug')
    sendEvent(name: 'checkInterval', value: configurationIntervals().checkIntervalDefault, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID], descriptionText: 'Default checkInterval')
    sendEvent(name: 'tamper', value: 'clear', descriptionText: 'Tamper cleared', displayed: false)
    deviceUseStates()
    sendEvent(name: "${getDataValue('event')}", value: "${getDataValue('inactiveState')}", displayed: false)
    if (listening()) {
        logger('Device is in listening mode (powered).', 'info')
        sendEvent(name: 'powerSource', value: 'dc', descriptionText: 'Device is connected to DC power supply.')
        sendEvent(name: 'batteryStatus', value: 'DC-power', displayed: false)
    }
    else {
        logger('Device is in sleepy mode (battery).', 'info')
        sendEvent(name: 'powerSource', value: 'battery', descriptionText: 'Device is using battery.')
    }
}

def configure() {
    logger('configure(): Configuring device', 'debug')
    device.updateSetting('autoResetTamperDelay', 30)
    device.updateSetting('configLoggingLevelIDE', 5) // set to 3 when finished debugging
    device.updateSetting('configLoggingLevelDevice', 2)
    if (!listening()) {
        def interval = (configurationIntervals()?.wakeUpIntervalSpecified) ?: configurationIntervals().wakeUpIntervalDefault
        state.wakeUpIntervalTarget = interval
        device.updateSetting('configWakeUpInterval', interval)
    }
    parametersMetadata().findAll( { it.id in configurationParameters() && !it.readonly } ).each {
        def csv = configurationSpecified()?.find { cs -> cs.id == it.id }?.specifiedValue
        def rv = (csv) ?: it.defaultValue
        (csv) ? logger("configure() Parameter: $it.id Specified value: $csv", 'debug') : logger("configure() Parameter: $it.id Reset value: $rv", 'debug')
        state."paramTarget${it.id}" = rv
        def id = it.id.toString().padLeft(3, "0")
        switch(it.type) {
            case "number":
                device.updateSetting("configParam${id}", rv)
                logger("configure() Parameter id: $id, reset preference (number) to: $rv", 'debug')
                break
            case "enum":
                device.updateSetting("configParam${id}", rv)
                logger("configure() Parameter id: $id, reset preference (enum) to: $rv", 'debug')
                break
            case "bool":
                device.updateSetting("configParam${id}", ((rv == it.trueValue) ? true : false))
                logger("configure() Parameter id: $id, reset preference (bool) to: ${(rv == it.trueValue) ? true : false}", 'debug')
                break
            case "flags":
                def flags = (configurationSpecified()?.find { csf -> csf.id == it.id }?.flags) ?: it.flags
                flags.each { f ->
                    def fv = (f?.specifiedValue) ?: f.defaultValue
                    device.updateSetting("configParam${id}${f.id}", ((fv == f.flagValue) ? true : false))
                    logger("configure() Parameter id: $id$f.id, reset preference (flag) to: ${(fv == f.flagValue) ? true : false}", 'debug')
                }
                break
        }
    }
    state.syncAll = true
    state.configurationReport = [:]
    updateDataValue('serialNumber', null)
    updated()
}

def updated() {
    logger('updated(): Updating device', 'debug')
    if (!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 2000) {
        state.updatedLastRanAt = now()
        state.autoResetTamperDelay = (settings.configAutoResetTamperDelay) ? settings.configAutoResetTamperDelay : 30
        state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE : 3
        state.loggingLevelDevice = (settings.configLoggingLevelDevice) ? settings.configLoggingLevelDevice : 2
        parametersMetadata().findAll( { it.id in configurationParameters() && !it.readonly } ).each {
            def id = it.id.toString().padLeft(3, "0")
            if (settings?."configParam${id}" != null || settings?."configParam${id}" == false || (settings?.find { s -> s.key == "configParam${id}a" })) {
                switch(it.type) {
                    case 'number':
                        state."paramTarget${it.id}" = settings."configParam${id}"
                        break
                    case 'enum':
                        state."paramTarget${it.id}" = settings."configParam${id}"
                        break
                    case 'bool':
                        state."paramTarget${it.id}" = (settings."configParam${id}") ? it.trueValue : it.falseValue
                        break
                    case 'flags':
                        def target = 0
                        settings.findAll { set -> set.key ==~ /configParam${it}[a-z]/ }.each{ k, v -> if (v) target += it.flags.find { f -> f.id == "${k.reverse().take(1)}" }.flagValue }
                        state."paramTarget${it.id}" = target
                        break
                }
            }
        }
        if (listening()) {
            sync()
        }
        else {
            state.queued = [] as Set
            state.queued.plus('sync()')
            if (settings.configWakeUpInterval) state.wakeUpIntervalTarget = settings.configWakeUpInterval
        }
    }
    else { logger('updated(): Ran within last 2 seconds so aborting update.', 'trace') }
}

def parse(String description) {
    logger("parse(): Parsing raw message: ${description}", 'trace')
    def result = []
    if (description.startsWith("Err")) {
        if (description.startsWith("Err 106")) {
            logger("parse() >> Err 106", "error")
            result = createEvent(name: "secureInclusion", value: "failed", isStateChange: true,
                    descriptionText: "This sensor failed to complete the network security key exchange. If you are unable to control it via SmartThings, you must remove it from your network and add it again.")
        }
        else {
            logger("parse(): Unknown Error. Raw message: ${description}", 'error')
        }
    }
    else if (description != "updated") {
        def cmd = zwave.parse(description, commandClassesVersions())
        if (cmd) {
            result = zwaveEvent(cmd)
            logger "After zwaveEvent(cmd) >> Parsed '${description}' to ${result.inspect()}", 'trace'
            if (listening() && (device.latestValue('syncPending') > 0) && (cmd.commandClassId in commandClassesUnsolicited())) sync()
        }
        else {
            logger("parse(): Could not parse raw message: ${description}","error")
        }
    }
    result
}

/*****************************************************************************************************************
 *  Zwave Application Events Handlers
*****************************************************************************************************************/
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) { // 0x20=1, // Basic
    logger("BasicSet(): Creating Motion event", "info")
    motionEvent(cmd.value) // responding to BasicSet - 2001 value FF
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd) { // 0x30=2, // Sensor Binary
    logger("SensorBinaryReport(): Creating Motion event", "info")
    motionEvent(cmd.sensorValue)
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) { // 0x71=5, // Notification
    def result = []
    if (cmd.notificationType == 7) {
        switch (cmd.event) {
            case 0:
                result << motionEvent(0)
                result << createEvent(name: "tamper", value: "clear")
                break
            case 3:
                result << createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName was tampered")
                if (state.autoResetTamperDelay > 0) runIn(state.autoResetTamperDelay, "resetTamper")
                break
            case 7: // ? shouldn't it be case 8: ?
                result << motionEvent(1)
                break
        }
    }
    else {
        logger("Need to handle this cmd.notificationType: ${cmd.notificationType}", 'warn')
        result << createEvent(descriptionText: cmd.toString(), isStateChange: false)
    }
    result
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) { // 0x31=5, // Sensor Multilevel
    def map = [: ]
    switch (cmd.sensorType) {
        case 1:
            map.name = "temperature"
            def cmdScale = cmd.scale == 1 ? "F" : "C"
            map.value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmdScale, cmd.precision)
            map.unit = getTemperatureScale()
            break
        case 3:
            map.name = "illuminance"
            map.value = cmd.scaledSensorValue.toInteger()
            map.unit = "lux"
            break
        case 5:
            map.name = "humidity"
            map.value = cmd.scaledSensorValue.toInteger()
            map.unit = "%"
            break
        case 0x1B:
            map.name = "ultravioletIndex"
            map.value = cmd.scaledSensorValue.toInteger()
            break
        default:
            map.descriptionText = cmd.toString()
    }
    createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) { // 0x70: 2, // Configuration
    logger("ConfigurationReport: $cmd", 'debug')
    def paramMd = parametersMetadata().find( { it.id == cmd.parameterNumber })
    def paramValue = (paramMd?.isSigned) ? cmd.scaledConfigurationValue : byteArrayToUInt(cmd.configurationValue)
    def signInfo = (paramMd?.isSigned) ? "SIGNED" : "UNSIGNED"
    state."paramCache${cmd.parameterNumber}" = paramValue
    logger("Parameter #${cmd.parameterNumber} [${paramMd?.name}] has value: ${paramValue} [${signInfo}]", 'info')
    updateSyncPending()

    def paramReport = cmd.parameterNumber.toString().padLeft(3, "0")
    def paramValueReport = paramValue.toString()
    logger("Processing Configuration Report: (Parameter: $paramReport, Value: $paramValueReport)", 'trace')
    state.configurationReport << [(paramReport): paramValueReport]
    if (state.configurationReport.size() == configurationParameters().size()) {
        logger('All Configuration Values Reported', 'info')
        def copy = state.configurationReport
        def report = state.configurationReport.sort().collect { it }.join(",")
        updateDataValue("configurationReport", report)
        state.configurationReport = copy
        logger("Configuration Report State: $state.configurationReport", 'debug')
    }
    def result = []
    if (cmd.parameterNumber == 9 && cmd.configurationValue[0] == 0) {
        result << createEvent(name: 'powerSource', value: 'dc', displayed: false)
        result << createEvent(name: 'batteryStatus', value: 'USB Cable', displayed: false) // ??is this needed??
    }
    else if (cmd.parameterNumber == 9 && cmd.configurationValue[0] == 1) {
        result << createEvent(name: 'powerSource', value: 'battery', displayed: false)
    }
    result
}

/*****************************************************************************************************************
 *  Zwave Management Events Handlers
*****************************************************************************************************************/
def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) { // 0x80=1, // Battery
    def map = [name: "battery", unit: "%"]
    if (cmd.batteryLevel == 0xFF) {
        map.value = 1
        map.descriptionText = "${device.displayName} battery is low"
        map.isStateChange = true
    }
    else {
        map.value = cmd.batteryLevel
        map.isStateChange = true // force propogation of event
    }
    state.timeLastBatteryReport = new Date().time
    createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) { // 0x72: 2
    logger('zwaveEvent(deviceSpecificReport): Serial number report received.', 'trace')
    logger("zwaveEvent(deviceSpecificReport): Serial number raw report: $cmd", 'info')
    def serialNumber = "0"
    // serialNumber = (cmd.deviceIdType == 1 && cmd.deviceIdDataFormat == 1) ? (cmd.deviceIdData.each { data -> serialNumber += "${String.format(" % 02 X ", data)}" }) : "0"
    updateDataValue('serialNumber', serialNumber)
    updateSyncPending()
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) { // 0x72=2, // Manufacturer Specific
    log.info "Executing zwaveEvent 72 (ManufacturerSpecificV2) : 05 (ManufacturerSpecificReport) with cmd: $cmd"
    log.debug "manufacturerId:   ${cmd.manufacturerId}"
    log.debug "manufacturerName: ${cmd.manufacturerName}"
    log.debug "productId:        ${cmd.productId}"
    log.debug "productTypeId:    ${cmd.productTypeId}"
    def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
    updateDataValue("MSR", msr)
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionCommandClassReport cmd) { // 0x86: 2, // Version
    def ccValue = Integer.toHexString(cmd.requestedCommandClass).toUpperCase()
    def ccVersion = cmd.commandClassVersion
    logger("Processing Command Class Version Report: (Command Class: $ccValue, Version: $ccVersion)", 'trace')
    state.commandClassVersions << [(ccValue): ccVersion]
    if (state.commandClassVersions.size() == commandClassesQuery().size()) {
        logger('All Command Class Versions Reported', 'debug')
        updateDataValue("commandClassVersions", state.commandClassVersions.findAll { it.value > 0 }.sort().collect { it }.join(","))
    }
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpIntervalReport cmd) { // 0x84=2, // Wake Up
    def result = []
    def wakeupInterval = cmd.seconds
    logger("wakeupInterval: $wakeupInterval", 'info')
    updateDataValue("wakeupInterval", "$wakeupInterval")
    result << createEvent(descriptionText: "$device.displayName wakeupInterval: $wakeupInterval", isStateChange: false)
    // *** need to create checkInterval event
    result
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd) { // // 0x84=2, // Wake Up
    def result = [createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)]
    def cmds = []
    /*
    if (!configured()) {
        logger("late configure", 'info')
        result << response(configure())
    } else {
        logger("Device has been configured sending >> wakeUpNoMoreInformation()", 'trace')
        cmds << zwave.wakeUpV1.wakeUpNoMoreInformation().format()
        result << response(cmds)
    }
    */
    result
}

/*****************************************************************************************************************
 *  Zwave Network Protocol Events Handlers
*****************************************************************************************************************/
def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd) { // 0x73=1, // Powerlevel
    logger("Powerlevel Report: $cmd", 'trace')
    def powerLevel = -1 * cmd.powerLevel //	def timeout = cmd.timeout (1-255 s) - omit
    logger("Processing Powerlevel Report: $powerLevel dBm", 'info')
    updateDataValue('powerLevel', "$powerLevel")
}

/*****************************************************************************************************************
 *  Zwave Transport Encapsulation Events Handlers
*****************************************************************************************************************/
def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) { // 0x98=1, Security
    def encapsulatedCommand = cmd.encapsulatedCommand([0x31: 5, 0x30: 2, 0x84: 1])
    state.sec = 1
    logger("encapsulated: ${encapsulatedCommand}", 'trace')
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand)
    }
    else {
        logger("Unable to extract encapsulated cmd from $cmd", 'warn')
        createEvent(descriptionText: cmd.toString())
    }
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.NetworkKeyVerify cmd) { // 0x98=1, Security
    state.sec = 1
    logger "Executing zwaveEvent 98 (SecurityV1): 07 (NetworkKeyVerify) with cmd: $cmd (node is securely included)"
    def result = [createEvent(name: "secureInclusion", value: "success", descriptionText: "Secure inclusion was successful", isStateChange: true)]
    result
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityCommandsSupportedReport cmd) { // 0x98=1, Security
    logger "Executing zwaveEvent 98 (SecurityV1): 03 (SecurityCommandsSupportedReport) with cmd: $cmd"
    state.sec = 1
}

/*****************************************************************************************************************
 *  Zwave General Event Handler
*****************************************************************************************************************/
def zwaveEvent(physicalgraph.zwave.Command cmd) {
    logger "General zwaveEvent cmd: ${cmd}"
    createEvent(descriptionText: cmd.toString(), isStateChange: false)
}

/*****************************************************************************************************************
 *  Capability-related Commands
*****************************************************************************************************************/
def ping() {
    if (listening()) sendCommandSequence(zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 0x01))
}

def refresh() {
}

def poll() { // depreciated
}

/*****************************************************************************************************************
 *  Custom Commands
*****************************************************************************************************************/
def resetTamper() {
    logger('resetTamper(): Resetting tamper alarm.', 'info')
    sendEvent(name: "tamper", value: "clear", descriptionText: "Tamper alarm cleared", displayed: true, isStateChange: true)
}

def syncAll() {
    logger('syncAll() called', 'debug')
    state.syncAll = true
    state.configurationReport = [:]
    (listening()) ? sync() : state.queued.plus('sync()')
}

def test() {
    logger('test() called', 'debug')
    (listening()) ? testNow() : state.queued.plus('testNow()')
}

private testNow() {
    logger('testRun() called', 'debug')
    def cmds = []
    logger('testRun(): Requesting Powerlevel Report.', 'trace')
    cmds << zwave.powerlevelV1.powerlevelGet()
    logger('testRun(): Requesting Command Class Report.', 'trace')
    state.commandClassVersions = [:]
    commandClassesQuery().each {
        cmds << zwave.versionV1.versionCommandClassGet(requestedCommandClass: it)
    }
    if ('testNow()' in state.queued) state.queued.minus('testNow()')
    logger('testNow(): Sending test commands.', 'trace')
    sendCommandSequence(cmds)
}

/*****************************************************************************************************************
 *  Generic Helper Methods
*****************************************************************************************************************/
private byteArrayToUInt(byteArray) {
    def i = 0
    byteArray.reverse().eachWithIndex { b, ix -> i += b * (0x100 ** ix) }
    i
}

private listening() {
    getZwaveInfo()?.zw?.startsWith("L")
}

private logger(msg, level = "debug") {
    switch(level) {
        case "error":
            if (state.loggingLevelIDE >= 1) log.error msg; sendEvent descriptionText: "Error: $msg", displayed: false, isStateChange: true
            if (state.loggingLevelDevice >= 1) sendEvent name: "logMessage", value: "Error: $msg", displayed: false, isStateChange: true
            break
        case "warn":
            if (state.loggingLevelIDE >= 2) log.warn msg; sendEvent descriptionText: "Warning: $msg", displayed: false, isStateChange: true
            if (state.loggingLevelDevice >= 2) sendEvent name: "logMessage", value: "Warning: $msg", displayed: false, isStateChange: true
            break
        case "info":
            if (state.loggingLevelIDE >= 3) log.info msg; sendEvent descriptionText: "Info: $msg", displayed: false, isStateChange: true
            break
        case "debug":
            if (state.loggingLevelIDE >= 4) log.debug msg; sendEvent descriptionText: "Debug: $msg", displayed: false, isStateChange: true
            break
        case "debug":
            if (state.loggingLevelIDE >= 5) log.trace msg; sendEvent descriptionText: "Trace: $msg", displayed: false, isStateChange: true
            break
        default:
            log.debug msg; sendEvent descriptionText: "Log: $msg", displayed: false, isStateChange: true
    }
}

private sync() {
    logger('sync(): Syncing configuration with the physical device.', 'debug')
    def cmds = []
    def syncPending = 0
    if (state.syncAll) {
        logger('sync(): Deleting all cached values.', 'trace')
        state.wakeUpIntervalCache = null
        parametersMetadata().findAll( { it.id in configurationParameters() && !it.readonly } ).each { state."paramCache${it.id}" = null }
        updateDataValue('serialNumber', null)
    }
    if (!listening() && state.wakeUpIntervalTarget != null && state.wakeUpIntervalTarget != state.wakeUpIntervalCache) {
        syncPending++
        logger("sync(): Syncing Wake Up Interval: New Value: ${state.wakeUpIntervalTarget}", 'info')
        cmds << zwave.wakeUpV1.wakeUpIntervalSet(seconds: state.wakeUpIntervalTarget, nodeid: zwaveHubNodeId)
        cmds << zwave.wakeUpV1.wakeUpIntervalGet()
    }
    parametersMetadata().each {
        if (it.id in configurationParameters() && !it.readonly && state."paramTarget${it.id}" != null && state."paramTarget${it.id}" != state."paramCache${it.id}") {
            syncPending++
            logger("sync(): Syncing parameter #${it.id} [${it.name}]: New Value: " + state."paramTarget${it.id}", 'info')
            cmds << zwave.configurationV1.configurationSet(parameterNumber: it.id, size: it.size, scaledConfigurationValue: state."paramTarget${it.id}")
            cmds << zwave.configurationV1.configurationGet(parameterNumber: it.id)
        }
        else if (state.syncAll && it.id in configurationParameters()) {
            cmds << zwave.configurationV1.configurationGet(parameterNumber: it.id)
        }
    }
    state.syncAll = false
    if (getDataValue('serialNumber') == null) {
        logger('sync(): Requesting device serial number.', 'trace')
        cmds << zwave.manufacturerSpecificV2.deviceSpecificGet(deviceIdType: 1)
        syncPending++
    }
    sendEvent(name: "syncPending", value: syncPending, displayed: false, descriptionText: "Change to syncPending.", isStateChange: true)
    logger('sync(): Sending sync commands.', 'debug')
    sendCommandSequence(cmds)
}

private updateSyncPending() {
    def syncPending = 0
    def userConfig = 0
    if (state.syncAll) {
        logger('updateSyncPending(): Deleting all cached values.', 'trace')
        state.wakeUpIntervalCache = null
        parametersMetadata().findAll( { it.id in configurationParameters() && !it.readonly } ).each { state."paramCache${it.id}" = null }
        updateDataValue('serialNumber', null)
        state.syncAll = false
    }
    if (!listening()) {
        def t = state.wakeUpIntervalTarget
        if ((t != null) && (target != state.wakeUpIntervalCache)) syncPending++
        if (t != configurationIntervals().wakeUpIntervalSpecified) userConfig++
    }
    parametersMetadata().findAll( { it.id in configurationParameters() && !it.readonly} ).each {
        if (state."paramTarget${it.id}" != null) {
            if (state."paramCache${it.id}" != state."paramTarget${it.id}") { syncPending++ }
            else if (state."paramCache${it.id}" != it.defaultValue) {
                def sv = configurationSpecified()?.find { cs -> cs.id == it.id }?.specifiedValue
                if (state."paramCache${it.id}"!= sv) userConfig++
            }
        }
    }
    if (getDataValue('serialNumber') == null) syncPending++
    logger("updateSyncPending(): syncPending: ${syncPending}", 'debug')
    // if ((syncPending == 0) && (device.latestValue('syncPending') > 0)) {
    if (syncPending == 0) {
        logger("Sync Complete.", "info")
        logger("updateSyncPending(): userconfig: $userConfig", 'debug')
        def ct = (userConfig > 0) ? 'user' : (configurationSpecified()) ? 'specified' : 'default'
        updateDataValue('configurationType', ct)
    }
    sendEvent(name: "syncPending", value: syncPending, displayed: false)
}

private generatePrefsParams() {
    input (name: 'paraParameters', title: 'DEVICE PARAMETERS:', description: 'Device parameters are used to customise the physical device. Refer to the product documentation for a full description of each parameter.', type: 'paragraph', element: 'paragraph')
    parametersMetadata().findAll{ !it.readonly }.each{
        if (configurationUser()[0] == 0 || it.id in configurationUser()) {
            def id = it.id.toString().padLeft(3, "0") // *** need to alter name: below
            def lb = (it.description.length() > 0) ? "\n" : ""
            def sv = configurationSpecified()?.find { cs -> cs.id == it.id }?.specifiedValue
            def dv = (sv) ? sv : it.defaultValue
            switch(it.type) {
                case "number":
                    input (
                        name: "configParam${id}",
                        title: "${it.id}: ${it.name}: \n" + it.description + lb +"Default Value: ${it.defaultValue}",
                        description: it.description,
                        type: it.type,
                        range: it.range,
                        defaultValue: dv,
                        required: it.required
                    )
                    break
                case "enum":
                    input (
                        name: "configParam${id}",
                        title: "${it.id}: ${it.name}: \n" + it.description + lb + "Default Value: ${it.defaultValue}",
                        description: it.description,
                        type: it.type,
                        options: it.options,
                        defaultValue: dv,
                        required: it.required
                    )
                    break
                case "bool":
                    input (
                        name: "configParam${id}",
                        title: "${it.id}: ${it.name}: \n" + it.description + lb + "Default Value: ${it.defaultValue}",
                        description: it.description,
                        type: it.type,
                        defaultValue: (dv == it.trueValue) ? true : false,
                        required: it.required
                    )
                    break
                case "flags":
                    input (
                        title: "${it.id}: ${it.name}",
                        description: it.description,
                        type: "paragraph", element: "paragraph"
                    )
                    if (sv) def svf = configurationSpecified()?.find { cs -> cs.id == it.id }?.flags
                    it.flags.each { f ->
                        def dvf = (sv) ? svf.find { s -> s.id == f.id }?.specifiedValue : f.defaultValue
                        input (
                            name: "configParam${id}${f.id}",
                            title: "${f.id}) ${f.description}",
                            type: 'bool',
                            defaultValue: (dvf == f.flagValue) ? true : false,
                            required: it.required
                        )
                    }
                    break
            }
        }
    }
}

/*****************************************************************************************************************
 * Send Zwave Commands
*****************************************************************************************************************/
private sendCommandSequence(commands, delay = 1200) {
    logger('sendCommandSequence(): Assembling commands.', 'debug')
    logger("sendCommandSequence(): Command sequence: $commands", 'debug')
    // delayBetween(commands.collect { encapsulate(it) }, delay)
    if (!listening()) commands << zwave.wakeUpV1.wakeUpNoMoreInformation()
    sendHubCommand(commands.collect { response(it) }, delay)
    // sendHubCommand(commands.collect { selectEncapsulation(response(it)) }, delay) // not sure of this code // not sure of this code
}

// private selectEncapsulation(physicalgraph.zwave.Command cmd) {
private selectEncapsulation(cmd) {
    logger('selectEncapsulation(): Selecting encapsulation method.', 'debug')
    // if (zwaveInfo?.zw.endsWith("s") && (cmd.commandClassId in commandClassesSecure())) {
    if (zwaveInfo?.zw.endsWith('s')) {
        secureEncapsulate(cmd)
    }
    else if (zwaveInfo?.cc.contains('56')) {
        crc16Encapsulate(cmd)
    }
    else {
        cmd.format()
    }
}

// private secureEncapsulate(physicalgraph.zwave.Command cmd) {
private secureEncapsulate(cmd) {
    logger('secureEncapsulate(): Encapsulating using secure method.', 'trace')
    zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

// private crc16Encapsulate(physicalgraph.zwave.Command cmd) {
private crc16Encapsulate(cmd) {
    logger('crc16Encapsulate(): Encapsulating using crc16 method.', 'trace')
    zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

/*****************************************************************************************************************
 *  Specific Helper Methods
*****************************************************************************************************************/
private deviceUseStates() {
    def use = settings?.configDeviceUse
    def useStates = configurationUseStates()?.find { it.key == use }
    def event = (use) ? useStates.event : 'water'
    def inactiveState = (use) ? useStates.inactive : 'dry'
    def activeState = (use) ? useStates.active : 'wet'
    updateDataValue('deviceUse', use)
    updateDataValue('event', event)
    updateDataValue('inactiveState', inactiveState)
    updateDataValue('activeState', activeState)
}

private motionEvent(value) {
    def map = [name: "motion"]
    if (value) {
        map.value = "active"
        map.descriptionText = "$device.displayName detected motion"
    }
    else {
        map.value = "inactive"
        map.descriptionText = "$device.displayName motion has stopped"
    }
    createEvent(map)
}

private sensorValueEvent(Short value) {
    def eventValue = null
    if (value == 0x00) {eventValue = "dry"}
    if (value == 0xFF) {eventValue = "wet"}
    def result = createEvent(name: "water", value: eventValue, displayed: true, isStateChange: true, descriptionText: "$device.displayName is $eventValue")
    return result
}

private getTimeOptionValueMap() { [
    "10 seconds": 10,
    "20 seconds": 20,
    "40 seconds": 40,
    "1 minute": 60,
    "2 minutes": 2 * 60,
    "3 minutes": 3 * 60,
    "4 minutes": 4 * 60,
    "5 minutes": 5 * 60,
    "8 minutes": 8 * 60,
    "15 minutes": 15 * 60,
    "30 minutes": 30 * 60,
    "1 hour": 1 * 60 * 60,
    "6 hours": 6 * 60 * 60,
    "12 hours": 12 * 60 * 60,
    "18 hours": 18 * 60 * 60,
    "24 hours": 24 * 60 * 60
] }

/*****************************************************************************************************************
 *  Matadata Functions
 *****************************************************************************************************************/
private commandClassesQuery() { [
    0x20, 0x22, 0x25, 0x26, 0x27, 0x2B, 0x30, 0x31, 0x32, 0x33, 0x56, 0x59, 0x5A, 0x5E, 0x60, 0x70, 0x71, 0x72, 0x73, 0x75, 0x7A, 0x80, 0x84, 0x85, 0x86, 0x8E, 0x98, 0x9C
] }

private commandClassesSecure() { [
    0x20, 0x2B, 0x30, 0x5A, 0x70, 0x71, 0x84, 0x85, 0x8E, 0x9C
] }

private commandClassesUnsolicited() { [
    0x20, 0x30, 0x31, 0x60, 0x71, 0x9C
] }

private commandClassesVersions() { [
    0x20: 1, // Basic
    0x30: 2, // Sensor Binary
    0x31: 5, // Sensor Multilevel
    0x59: 1, // Association Grp Info
    0x5A: 1, // Device Reset Locally
    0x5E: 2, // Zwave Plus Info (not supported)
    0x70: 2, // Configuration
    0x71: 3, // Notification - changed to v3
    0x72: 2, // Manufacturer Specific
    0x73: 1, // Powerlevel
    0x7A: 2, // Firmware Update Md
    0x80: 1, // Battery
    0x84: 1, // Wake Up - changed to v1
    0x85: 2, // Association
    0x86: 1, // Version - changed to v1
    0x98: 1 // Security
] }

/* Currently Unused
    0x59=1, // Association Grp Info
    0x5A=1, // Device Reset Locally
    0x5E=2, // Zwave Plus Info (not supported)
    0x7A=2, // Firmware Update Md
    0x85=2, // Association
*/

private configurationHandler() { [
    'deviceUse', 'autoResetTamperDelay', 'loggingLevelDevice', 'loggingLevelIDE', 'wakeUpInterval'
] }

private configurationUseStates() { [
    Bed: [event: 'contact', inactive: 'empty', active: 'occupied'],
    Chair: [event: 'contact', inactive: 'vacant', active: 'occupied'],
    Toilet: [event: 'contact', inactive: 'full', active: 'flushing'],
    Water: [event: 'water', inactive: 'dry', active: 'wet', default: true]
] }

private configurationIntervals() { [
    wakeUpIntervalDefault: 4_000,
    checkIntervalDefault: 8_500,
    wakeUpIntervalSpecified: 86_400,
    checkIntervalSpecified: 180_000,
    batteryRefresh: 604_800
] }

private configurationParameters() { [
    2, 3, 4, 5, 8, 9, 40, 81, 101, 102, 103, 111, 112, 113
] }

private configurationSpecified() { [
    [id: 2, size: 1, defaultValue: 0, specifiedValue: 1],
    [id: 3, size: 2, defaultValue: 240, specifiedValue: 60],
    [id: 4, size: 1, defaultValue: 5, specifiedValue: 5],
    [id: 40, size: 1, defaultValue: 0, specifiedValue: 0],
    [id: 81, size: 1, defaultValue: 0, specifiedValue: 2],
    [id: 101, size: 4, defaultValue: 241, specifiedValue: 240, flags: [[id: 'a', flagValue: 1, defaultValue: 1, specifiedValue: 0], [id: 'b', flagValue: 16, defaultValue: 16, specifiedValue: 16], [id: 'c', flagValue: 32, defaultValue: 32, specifiedValue: 32], [id: 'd', flagValue: 64, defaultValue: 64, specifiedValue: 64], [id: 'e', flagValue: 128, defaultValue: 128, specifiedValue: 128]]],
    [id: 102, size: 4, defaultValue: 0, specifiedValue: 0, flags: [[id: 'a', flagValue: 1, defaultValue: 0, specifiedValue: 0], [id: 'b', flagValue: 16, defaultValue: 0, specifiedValue: 0], [id: 'c', flagValue: 32, defaultValue: 0, specifiedValue: 0], [id: 'd', flagValue: 64, defaultValue: 0, specifiedValue: 0], [id: 'e', flagValue: 128, defaultValue: 0, specifiedValue: 0]]],
    [id: 103, size: 4, defaultValue: 0, specifiedValue: 0, flags: [[id: 'a', flagValue: 1, defaultValue: 0, specifiedValue: 0], [id: 'b', flagValue: 16, defaultValue: 0, specifiedValue: 0], [id: 'c', flagValue: 32, defaultValue: 0, specifiedValue: 0], [id: 'd', flagValue: 64, defaultValue: 0, specifiedValue: 0], [id: 'e', flagValue: 128, defaultValue: 0, specifiedValue: 0]]],
    [id: 111, size: 4, defaultValue: 3600, specifiedValue: 3600]
] }

private configurationUser() { [
    2, 3, 4, 5, 8, 81, 101, 102, 103, 111, 112, 113
] }

private parametersMetadata() { [
    [id:2,size:1,type:'bool',defaultValue:0,required:false,readonly:false,isSigned:false,name:'Enable waking up for 10 minutes',description:'when re-power on (battery mode) the MultiSensor',falseValue:0,trueValue:1],
    [id:3,size:2,type:'number',range: '10..3600',defaultValue:240,required:true,readonly:false,isSigned:false,name: 'PIR reset time',description:'Reset time for PIR sensor'],
    [id:4,size:1,type:'enum',defaultValue:5,required:false,readonly:false,isSigned:false,name:'',description:'',options:[0:'Off',1:'level 1 (minimum)',2:'level 2',3:'level 3',4:'level 4',5:'level 5 (maximum)']],
    [id:5,size:1,type:'enum',defaultValue:1,required:false,readonly:false,isSigned:false,name:'Which command?',description:'Command sent when the motion sensor triggered.',options:[1:'send Basic Set CC',2:'send Sensor Binary Report CC']],
    [id:8,size:1,type:'number',range: '15..60',defaultValue:15,required:false,readonly:false,isSigned:false,name: 'Timeout of after Wake Up',description:'Set the timeout of awake after the Wake Up CC is sent out'],
    [id:9,size:2,type:'flags',required:false,readonly:true,isSigned:false,name:'Report the current power mode and the product state for battery power mode',description:'Report the current power mode and the product state for battery power mode'],
    [id:40,size:1,type:'bool',defaultValue:0,required:false,readonly:false,isSigned:false,name:'Selective reporting',description:'Enable selective reporting',falseValue:0,trueValue:1],
    [id:81,size:1,type:'enum',defaultValue:0,required:false,readonly:false,isSigned:false,name:'Enable LED',description:'Enable/disable the LED blinking',options:[0:'Enable LED blinking',1:'Disable LED blinking only when the PIR is triggered',2:'Completely disable LED for motion; wakeup; and sensor report']],
    [id:101,size:4,type:'flags',defaultValue:241,required:false,readonly:false,isSigned:false,name:'Group 1 Report',description:'Which report needs to be sent in Report group 1',flags:[[id:'a',description:'enable battery',flagValue:1,defaultValue:1],[id:'b',description:'enable ultraviolet',flagValue:16,defaultValue:16],[id:'c',description:'enable temperature',flagValue:32,defaultValue:32],[id:'d',description:'enable humidity',flagValue:64,defaultValue:64],[id:'e',description:'enable luminance',flagValue:128,defaultValue:128]]],
    [id:102,size:4,type:'flags',defaultValue:0,required:false,readonly:false,isSigned:false,name:'Group 2 Report',description:'Which report needs to be sent in Report group 2',flags:[[id:'a',description:'enable battery',flagValue:1,defaultValue:0],[id:'b',description:'enable ultraviolet',flagValue:16,defaultValue:0],[id:'c',description:'enable temperature',flagValue:32,defaultValue:0],[id:'d',description:'enable humidity',flagValue:64,defaultValue:0],[id:'e',description:'enable luminance',flagValue:128,defaultValue:0]]],
    [id:103,size:4,type:'flags',defaultValue:0,required:false,readonly:false,isSigned:false,name:'Group 3 Report',description:'Which report needs to be sent in Report group 3',flags:[[id:'a',description:'enable battery',flagValue:1,defaultValue:0],[id:'b',description:'enable ultraviolet',flagValue:16,defaultValue:0],[id:'c',description:'enable temperature',flagValue:32,defaultValue:0],[id:'d',description:'enable humidity',flagValue:64,defaultValue:0],[id:'e',description:'enable luminance',flagValue:128,defaultValue:0]]],
    [id:111,size:4,type:'number',range: '300..12000',defaultValue:3600,required:false,readonly:false,isSigned:false,name: 'Time interval of group 1 report',description:'The interval time of sending reports in group 1'],
    [id:112,size:4,type:'number',range: '300..12000',defaultValue:3600,required:false,readonly:false,isSigned:false,name: 'Time interval of group 2 report',description:'The interval time of sending reports in group 2'],
    [id:113,size:4,type:'number',range: '300..12000',defaultValue:3600,required:false,readonly:false,isSigned:false,name: 'Time interval of group 3 report',description:'The interval time of sending reports in group 3']
] }