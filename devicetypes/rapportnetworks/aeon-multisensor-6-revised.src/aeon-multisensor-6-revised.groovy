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
            state "clear", label: 'tamper clear', action: "resetTamper", backgroundColor: "#ffffff", defaultState: true // remove action after testing
            state "detected", label: 'tampered', action: "resetTamper", backgroundColor: "#ff0000"
        }
        valueTile("syncPending", "device.syncPending", height: 2, width: 2, decoration: "flat") {
            state "syncPending", label: '${currentValue}', action: "syncAll", backgroundColor: "#ffffff", defaultState: true
        }
        valueTile("logMessage", "device.logMessage", height: 2, width: 2, decoration: "flat") {
            state "clear", label: '${currentValue}', backgroundColor: "#ffffff", defaultState: true
        }
        standardTile("syncAll", "device.sync", height: 2, width: 2, decoration: "flat") {
            state "syncAll", label: 'sync all', action: "syncAll", backgroundColor: "#ffffff", defaultState: true
        }
        standardTile("configure", "device.configure", height: 2, width: 2, decoration: "flat") {
            state "configure", label: 'configure', action: "configure", backgroundColor: "#ffffff", defaultState: true
        }
        standardTile("test", "device.test", height: 2, width: 2, decoration: "flat") {
            state "test", label: 'test', action: "test", backgroundColor: "#ffffff", defaultState: true
        }
        main(["motion", "temperature", "humidity", "illuminance", "ultravioletIndex"])
        details(["motion", "temperature", "humidity", "illuminance", "ultravioletIndex", "batteryStatus", "tamper", "syncPending", "logMessage", "syncAll", "configure", "test"])
    }

    preferences {
        if (configurationHandler()) input(type: 'paragraph', element: 'paragraph', title: 'GENERAL', description: 'Device handler settings.')

        if ('deviceUse' in configurationHandler()) {
            def uses = configurationUseStates().keySet().sort() as List
            def defaultUse = configurationUseStates().find { it.value.default }.key
            input(name: 'configDeviceUse', title: 'What type of sensor do you want to use this device for?', type: 'enum', options: uses, defaultValue: defaultUse, required: true, displayDuringSetup: true)
        }

        if ('autoResetTamperDelay' in configurationHandler()) input(name: 'configAutoResetTamperDelay', title: 'Auto-Reset Tamper Alarm:\n' + 'Automatically reset tamper alarms after this time delay.\n' + 'Values: 0 = Auto-reset Disabled\n' + '1-86400 = Delay (s)\n' + 'Default Value: 30s', type: 'number', defaultValue: '30', required: false)

        if ('loggingLevelIDE' in configurationHandler()) input(name: 'configLoggingLevelIDE', title: 'IDE Live Logging Level: Messages with this level and higher will be logged to the IDE.', type: 'enum', options: ['0' : 'None', '1' : 'Error', '2' : 'Warning', '3' : 'Info', '4' : 'Debug', '5' : 'Trace'], defaultValue: '3', required: true)

        if ('loggingLevelDevice' in configurationHandler()) input(name: 'configLoggingLevelDevice', title: 'Device Logging Level: Messages with this level and higher will be logged to the logMessage attribute.', type: 'enum', options: ['0' : 'None', '1' : 'Error', '2' : 'Warning'], defaultValue: '2', required: true)

        if ('wakeUpInterval' in configurationHandler()) input(name: 'configWakeUpInterval', title: 'WAKE UP INTERVAL:\n' + 'The device will wake up after each defined time interval to sync configuration parameters, ' + 'associations and settings.\n' + 'Values: 5-86399 = Interval (s)\n' + 'Default Value: 4000 (every 66 minutes)', type: 'number', defaultValue: '4000', required: false)

        if (configurationUser()) generatePrefsParams()
    }
}

/*****************************************************************************************************************
 *  SmartThings System Methods
 *****************************************************************************************************************/
def installed() {
    state.loggingLevelIDE = 5; state.loggingLevelDevice = 2
    logger('installed(): Performing initial setup', 'trace')
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
    logger('configure(): Configuring device', 'trace')
    device.updateSetting('autoResetTamperDelay', 30)
    device.updateSetting('configLoggingLevelIDE', '5') // set to 3 when finished debugging
    device.updateSetting('configLoggingLevelDevice', '2')

    if (!listening()) {
        def interval = (configurationIntervals().wakeUpIntervalSpecified) ?: configurationIntervals().wakeUpIntervalDefault
        state.wakeUpIntervalTarget = interval
        device.updateSetting('configWakeUpInterval', value: interval)
    }

    parametersMetadata().findAll( { !it.readonly } ).each {
        def sv = configurationSpecified().find { cs -> cs.id == it.id }?.specifiedValue
        if (sv) {
            state."paramTarget${it.id}" = sv
            device.updateSetting("configParam${it.id}", sv)

        } else {
            device.updateSetting("configParam${it.id}", it.defaultValue)
        }
    }
    state.syncAll = true
    state.configurationReport = [:]
    updateDataValue('serialNumber', null)
    updated()
}

def updated() {
    logger('updated(): Updating device', 'trace')
    if (!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 2000) {
        state.updatedLastRanAt = now()

        state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3
        state.loggingLevelDevice = (settings.configLoggingLevelDevice) ? settings.configLoggingLevelDevice.toInteger(): 2
        state.autoResetTamperDelay = (settings.configAutoResetTamperDelay) ? settings.configAutoResetTamperDelay.toInteger() : 30

        parametersMetadata().findAll( {!it.readonly} ).each {
            if (settings?."configParam${it.id}" || settings?.find { s -> s.key ==~ /configParam${it.id}[a-z]/ }) {
                switch(it.type) {
                    case "number":
                        state."param${it.id}target" = settings."configParam${it.id}"
                        break
                    case "enum":
                        state."param${it.id}target" = settings."configParam${it.id}".toInteger()
                        break
                    case "bool":
                        state."param${it.id}target" = (settings."configParam${it.id}") ? (it.trueValue ?: 1) : (it.falseValue ?: 0)
                        break
                    case "flags":
                        def target = 0
                        settings.findAll { st -> st.key ==~ /configParam${it.id}[a-z]/ }.each{ k, v -> if (v.toBoolean()) target += it.flags.find{ flag -> flag.id == "${k.reverse().take(1)}" }.flagValue }
                        state."param${it.id}target" = target
                        break
                }
            }
        }
        if (listening()) {
            sync()
        } else {
            if (settings.configWakeUpInterval) {
                state.wakeUpIntervalTarget = settings.configWakeUpInterval
            }
            state.queued = [] as Set
            state.queued.plus('sync()')
        }
    } else {
        logger('updated(): Ran within last 2 seconds so aborting update.', 'trace')
    }
}

def parse(String description) {
    logger("parse(): Parsing raw message: ${description}","trace")
    def result = []
    if (description.startsWith("Err")) {
        if (description.startsWith("Err 106")) {
            logger("parse() >> Err 106", "error")
            result = createEvent(name: "secureInclusion", value: "failed", isStateChange: true,
                    descriptionText: "This sensor failed to complete the network security key exchange. If you are unable to control it via SmartThings, you must remove it from your network and add it again.")
        } else {
            logger("parse(): Unknown Error. Raw message: ${description}","error")
        }
    }
    else if (description != "updated") {
        def cmd = zwave.parse(description, commandClassesVersions())
        if (cmd) {
            result = zwaveEvent(cmd)
            logger "After zwaveEvent(cmd) >> Parsed '${description}' to ${result.inspect()}", "trace"
            if (listening() && (device.latestValue('syncPending') > 0) && (cmd.commandClassId in commandClassesUnsolicited())) sync()
        } else {
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
    } else {
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

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) { // 0x70=2, // Configuration
    // *** to sort - use cached values?
    logger("ConfigurationReport: $cmd", 'trace')

    def param = cmd.parameterNumber.toString().padLeft(3, "0")
    def paramValue = cmd.scaledConfigurationValue.toString()
    logger("Processing Configuration Report: (Parameter: $param, Value: $paramValue)", 'trace')
    state.configurationReport << [(param): paramValue]
    if (state.configurationReport.size() == configurationParameters().size()) {
        logger('All Configuration Values Reported', 'trace')
        updateDataValue("configurationReport", state.configurationReport.collect {
            it
        }.join(","))
    }

    def result = []
    def value
    if (cmd.parameterNumber == 9 && cmd.configurationValue[0] == 0) {
        value = "dc"
        /*
        if (!configured()) {
            logger('ConfigurationReport: configuring device', 'trace')
            result << response(configure())
        }
        */
        result << createEvent(name: "batteryStatus", value: "USB Cable", displayed: false)
        result << createEvent(name: "powerSource", value: value, displayed: false)
    } else if (cmd.parameterNumber == 9 && cmd.configurationValue[0] == 1) {
        value = "battery"
        result << createEvent(name: "powerSource", value: value, displayed: false)
    }
    /* Think this condition is a bug as it seems to creates an infintie loop (since added request for parameter 101)
    	else if (cmd.parameterNumber == 101){
    		result << response(configure())
    	}
    */
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
    } else {
        map.value = cmd.batteryLevel
        map.isStateChange = true // force propogation of event
    }
    state.timeLastBatteryReport = new Date().time
    createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) { // 0x72: 2
    logger('zwaveEvent(deviceSpecificReport): Serial number report received.', 'trace')
    logger("zwaveEvent(deviceSpecificReport): Serial number raw report: $cmd", 'debug')
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

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionCommandClassReport cmd) { // 0x86=2, // Version
    def ccValue = Integer.toHexString(cmd.requestedCommandClass).toUpperCase()
    def ccVersion = cmd.commandClassVersion
    logger("Processing Command Class Version Report: (Command Class: $ccValue, Version: $ccVersion)", 'trace')
    state.commandClassVersions << [(ccValue): ccVersion]
    if (state.commandClassVersions.size() == commandClassesQuery().size()) {
        logger('All Command Class Versions Reported', 'debug')
        updateDataValue("commandClassVersions", state.commandClassVersions.findAll { it.value > 0 }.sort().collect { it }.join(","))
    }
    // createEvent(descriptionText: "${device.displayName} Command Class Versions Report", isStateChange: true, data: [name: 'Version Command Class Report', requestedCommandClass: ccValue, commandClassVersion: ccVersion])
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
    logger("Processing Powerlevel Report: $powerLevel dBm", 'trace')
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
    } else {
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
    if (listening()) {
        sendCommandSequence(zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 0x01))
    }
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
    sendEvent(name: "tamper", value: "clear", descriptionText: "Tamper alarm cleared", displayed: true)
}

def syncAll() {
    logger('syncAll() called', 'trace')
    state.syncAll = true
    (listening()) ? sync() : state.queued.plus('sync()')
}

def test() {
    logger('test() called', 'trace')
    (listening()) ? testNow() : state.queued.plus('testNow()')
}

private testNow() {
    logger('testRun() called', 'trace')
    def cmds = []

    logger('testRun(): Requesting Powerlevel Report.', 'trace')
    cmds << zwave.powerlevelV1.powerlevelGet()

    logger('testRun(): Requesting Command Class Report.', 'trace')
    state.commandClassVersions = [:]
    commandClassesQuery().each {
        cmds << zwave.versionV1.versionCommandClassGet(requestedCommandClass: it)
    }

    logger('testRun(): Requesting Configuration Report.', 'trace')
    state.configurationReport = [:]
    configurationParameters().each {
        cmds << zwave.configurationV1.configurationGet(parameterNumber: it)
    }

    if ('testNow()' in state.queued) state.queued.minus('testNow()')

    sendCommandSequence(cmds)
}

/*****************************************************************************************************************
 *  Generic Helper Methods
*****************************************************************************************************************/
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
        case "trace":
            if (state.loggingLevelIDE >= 5) log.trace msg; sendEvent descriptionText: "Trace: $msg", displayed: false, isStateChange: true
            break
        default:
            log.debug msg; sendEvent descriptionText: "Log: $msg", displayed: false, isStateChange: true
    }
}

private sync() {
    logger('sync(): Syncing configuration with the physical device.', 'trace')
    def cmds = []
    def syncPending = 0
    if (state.syncAll) {
        logger('sync(): Deleting all cached values.', 'trace')
        state.wakeUpIntervalCache = null
        parametersMetadata().findAll( {!it.readonly} ).each { state."paramCache${it.id}" = null }
        updateDataValue('serialNumber', null)
        state.syncAll = false
    }
    if (!listening() && (state.wakeUpIntervalTarget != null) && (state.wakeUpIntervalTarget != state.wakeUpIntervalCache)) {
        logger("sync(): Syncing Wake Up Interval: New Value: ${state.wakeUpIntervalTarget}", 'info')
        cmds << zwave.wakeUpV1.wakeUpIntervalSet(seconds: state.wakeUpIntervalTarget, nodeid: zwaveHubNodeId)
        cmds << zwave.wakeUpV1.wakeUpIntervalGet()
        syncPending++
    }
    parametersMetadata().findAll( {!it.readonly} ).each {
        if ((state."paramTarget${it.id}" != null) && (state."paramCache${it.id}" != state."paramTarget${it.id}")) {
            cmds << zwave.configurationV1.configurationSet(parameterNumber: it.id, size: it.size, scaledConfigurationValue: state."paramTarget${it.id}".toInteger())
            cmds << zwave.configurationV1.configurationGet(parameterNumber: it.id)
            logger("sync(): Syncing parameter #${it.id} [${it.name}]: New Value: " + state."paramTarget${it.id}", 'info')
            syncPending++
        }
    }

    parametersMetadata().findAll( {it.readonly} ).each {
        if (it in configurationParameters()) cmds << zwave.configurationV1.configurationGet(parameterNumber: it.id)
    }

    if (getDataValue('serialNumber') == null) {
        logger('sync(): Requesting device serial number.', 'trace')
        cmds << zwave.manufacturerSpecificV2.deviceSpecificGet(deviceIdType: 1)
        syncPending++
    }
    sendEvent(name: "syncPending", value: syncPending, displayed: false, descriptionText: "Change to syncPending.", isStateChange: true)
    logger('sync(): Sending sync commands.', 'trace')
    logger("sync(): Command sequence: $cmds", 'debug')
    sendCommandSequence(cmds)
}

private updateSyncPending() {
    def syncPending = 0
    def userConfig = 0
    if (state.syncAll) {
        state.wakeUpIntervalCache = null
        parametersMetadata().findAll( {!it.readonly} ).each { state."paramCache${it.id}" = null }
        updateDataValue('serialNumber', null)
        state.syncAll = false
    }
    if (!listening()) {
        def target = state.wakeUpIntervalTarget
        if ((target != null) && (target != state.wakeUpIntervalCache)) {
            syncPending++
        }
        if (target != configurationIntervals().wakeUpIntervalSpecified) {
            userConfig++
        }
    }
    parametersMetadata().findAll( {!it.readonly} ).each {
        def target = state."paramTarget${it.id}"
        if ((target != null) && (target != state."paramCache${it.id}")) {
            syncPending++
        }
        def sv = configurationSpecified().find { cs -> cs.id == it.id }?.specifiedValue
        if (sv && (target != sv)) {
            userConfig++
        } else if (target != it.defaultValue) {
            userConfig++
        }
    }
    if (getDataValue('serialNumber') == null) {
        syncPending++
    }
    logger("updateSyncPending(): syncPending: ${syncPending}", 'debug')
    if ((syncPending == 0) && (device.latestValue('syncPending') > 0)) {
        logger("Sync Complete.", "info")
        def ct = (userConfig > 0) ? 'user' : 'specified'
        updateDataValue('configurationType', ct)
    }
    sendEvent(name: "syncPending", value: syncPending, displayed: false)
}

private generatePrefsParams() {
    input (
        type: "paragraph",
        element: "paragraph",
        title: "DEVICE PARAMETERS:",
        description: "Device parameters are used to customise the physical device. " +
                     "Refer to the product documentation for a full description of each parameter."
    )
    parametersMetadata().findAll{ !it.readonly }.each{
        if (it.id in configurationUser()) {
            def lb = (it.description.length() > 0) ? "\n" : ""
            switch(it.type) {
                case "number":
                    input (
                        name: "configParam${it.id}",
                        title: "#${it.id}: ${it.name}: \n" + it.description + lb +"Default Value: ${it.defaultValue}",
                        type: it.type,
                        range: it.range,
                        defaultValue: it.defaultValue,
                        required: it.required
                    )
                    break
                case "enum":
                    input (
                        name: "configParam${it.id}",
                        title: "#${it.id}: ${it.name}: \n" + it.description + lb + "Default Value: ${it.defaultValue}",
                        type: it.type,
                        options: it.options,
                        defaultValue: it.defaultValue,
                        required: it.required
                    )
                    break
                case "bool":
                    input (
                        name: "configParam${it.id}",
                        title: "#${it.id}: ${it.name}: \n" + it.description + lb + "Default Value: ${it.defaultValue}",
                        type: it.type,
                        defaultValue: it.defaultValue,
                        required: it.required
                    )
                    break
                case "flags":
                    input (
                        title: "${it.id}: ${it.name}",
                        description: it.description,
                        type: "paragraph", element: "paragraph"
                    )
                    it.flags.each { flag ->
                        input (
                            name: "configParam${it.id}${flag.id}", // ? how best to reference? 1a or 1-a or 1-32 etc
                            title: "${flag.id}) ${flag.description}",
                            type: 'bool',
                            defaultValue: flag.default,
                            required: it.required
                        )
                    }
                    break
            }
        }
    }
}

private byteArrayToUInt(byteArray) {
    def i = 0
    byteArray.reverse().eachWithIndex { b, ix -> i += b * (0x100 ** ix) }
    return i
}

/*****************************************************************************************************************
 * Send Zwave Commands
*****************************************************************************************************************/
private sendCommandSequence(commands, delay = 1200) {
    logger('sendCommandSequence(): Assembling commands.', 'trace')
    // delayBetween(commands.collect { encapsulate(it) }, delay)
    if (!listening()) commands << zwave.wakeUpV1.wakeUpNoMoreInformation()
    sendHubCommand(commands.collect { response(it) }, delay)
    // sendHubCommand(commands.collect { selectEncapsulation(response(it)) }, delay) // not sure of this code // not sure of this code
}

// private selectEncapsulation(physicalgraph.zwave.Command cmd) {
private selectEncapsulation(cmd) {
    logger('selectEncapsulation(): Selecting encapsulation method.', 'trace')
    // if (zwaveInfo?.zw.endsWith("s") && (cmd.commandClassId in commandClassesSecure())) {
    if (zwaveInfo?.zw.endsWith('s')) {
        secureEncapsulate(cmd)
    } else if (zwaveInfo?.cc.contains('56')) {
        crc16Encapsulate(cmd)
    } else {
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

private sensorValueEvent(Short value) {
    def eventValue = null
    if (value == 0x00) {eventValue = "dry"}
    if (value == 0xFF) {eventValue = "wet"}
    def result = createEvent(name: "water", value: eventValue, displayed: true, isStateChange: true, descriptionText: "$device.displayName is $eventValue")
    return result
}

private motionEvent(value) {
    def map = [name: "motion"]
    if (value) {
        map.value = "active"
        map.descriptionText = "$device.displayName detected motion"
    } else {
        map.value = "inactive"
        map.descriptionText = "$device.displayName motion has stopped"
    }
    createEvent(map)
}

private getTimeOptionValueMap() {
    [
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
        "24 hours": 24 * 60 * 60,
    ]
}

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

private configurationParameters() { [
    2, 3, 4, 5, 8, 9, 40, 81, 101, 102, 103, 111, 112, 113
] }

private configurationSpecified() { [
    [id: 101, size: 4, specifiedValue: 240],
    [id: 102, size: 4, specifiedValue: 0],
    [id: 103, size: 4, specifiedValue: 0],
    [id: 111, size: 4, specifiedValue: 3600],
    [id: 2, size: 1, specifiedValue: 1],
    [id: 3, size: 2, specifiedValue: 10],
    [id: 4, size: 1, specifiedValue: 5],
    [id: 40, size: 1, specifiedValue: 0],
    [id: 81, size: 1, specifiedValue: 2]
] }

private configurationUser() { [
    3, 4
] }

private configurationHandler() { [
    // 'deviceUse',
    'autoResetTamperDelay', 'loggingLevelDevice', 'loggingLevelIDE'
    // , 'wakeUpInterval'
] }

private configurationIntervals() { [
    wakeUpIntervalDefault: 4_000,
    checkIntervalDefault: 8_500,
    wakeUpIntervalSpecified: 86_400,
    checkIntervalSpecified: 180_000,
    batteryRefresh: 604_800
] }

private configurationUseStates() { [
    Bed: [event: 'contact', inactive: 'empty', active: 'occupied'],
    Chair: [event: 'contact', inactive: 'vacant', active: 'occupied'],
    Toilet: [event: 'contact', inactive: 'full', active: 'flushing'],
    Water: [event: 'water', inactive: 'dry', active: 'wet', default: true]
] }

private parametersMetadata() { [
    [id: 2, size: 1, type: 'bool', defaultValue: 0, required: false, readonly: false, isSigned: false, name: 'Enable waking up for 10 minutes', description: 'when re-power on (battery mode) the MultiSensor', valueTrue: 3, valueFalse: 1],
    [id: 3, size: 2, type: 'number', range: '10..3600', defaultValue: 240, required: false, readonly: false, isSigned: false, name: 'PIR reset time', description: 'Reset time for PIR sensor'],
    [id: 4, size: 1, type: 'enum', defaultValue: '5', required: false, readonly: false, isSigned: false, name: 'a', description: 'a', options: ['0': 'Off', '1': 'level 1 (minimum)', '2': 'level 2', '3': 'level 3', '4': 'level 4', '5': 'level 5 (maximum)']],
    [id: 5, size: 1, type: 'enum', defaultValue: '1', required: false, readonly: false, isSigned: false, name: 'a', description: 'a', options: []],[id: 8, size: 1, type: 'number', range: '..', defaultValue: 15, required: false, readonly: false, isSigned: false, name: 'a', description: 'a'],
    [id: 9, size: 2, type: 'flags', defaultValue: 'a', required: false, readonly: true, isSigned: false, name: 'a', description: 'a', flags: []],
    [id: 40, size: 1, type: 'bool', defaultValue: 0, required: false, readonly: false, isSigned: false, name: 'a', description: 'a'],[id: 81, size: 1, type: 'enum', defaultValue: '0', required: false, readonly: false, isSigned: false, name: 'a', description: 'a', options: []],
    [id: 101, size: 4, type: 'flags', defaultValue: '241', required: false, readonly: false, isSigned: false, name: 'a', description: 'a', flags: [[id: 'a', description: 'enable battery', defaultValue: true, flagValue: 1], [id: 'b', description: 'enable ultraviolet', defaultValue: true, flagValue: 16], [id: 'c', description: 'enable temperature', defaultValue: true, flagValue: 32], [id: 'd', description: 'enable humidity', defaultValue: true, flagValue: 64], [id: 'e', description: 'enable luminance', defaultValue: true, flagValue: 128]]],
    [id: 102, size: 4, type: 'flags', defaultValue: '0', required: false, readonly: false, isSigned: false, name: 'a', description: 'a', flags: [[id: 'a', description: 'enable battery', defaultValue: true, flagValue: 1], [id: 'b', description: 'enable ultraviolet', defaultValue: true, flagValue: 16], [id: 'c', description: 'enable temperature', defaultValue: true, flagValue: 32], [id: 'd', description: 'enable humidity', defaultValue: true, flagValue: 64], [id: 'e', description: 'enable luminance', defaultValue: true, flagValue: 128]]],
    [id: 103, size: 4, type: 'flags', defaultValue: '0', required: false, readonly: false, isSigned: false, name: 'a', description: 'a', flags: [[id: 'a', description: 'enable battery', defaultValue: true, flagValue: 1], [id: 'b', description: 'enable ultraviolet', defaultValue: true, flagValue: 16], [id: 'c', description: 'enable temperature', defaultValue: true, flagValue: 32], [id: 'd', description: 'enable humidity', defaultValue: true, flagValue: 64], [id: 'e', description: 'enable luminance', defaultValue: true, flagValue: 128]]],
    [id: 111, size: 4, type: 'number', range: '..', defaultValue: 3600, required: false, readonly: false, isSigned: false, name: 'a', description: 'a'],
    [id: 112, size: 4, type: 'number', range: '..', defaultValue: 3600, required: false, readonly: false, isSigned: false, name: 'a', description: 'a'],
    [id: 113, size: 4, type: 'number', range: '..', defaultValue: 3600, required: false, readonly: false, isSigned: false, name: 'a', description: 'a']
] }