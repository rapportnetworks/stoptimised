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
        capability "Battery"
        capability "Configuration"
        capability "Health Check"
        capability "Illuminance Measurement"
        capability "Motion Sensor"
        capability "Power Source"
        capability "Relative Humidity Measurement"
        capability "Tamper Alert"
        capability "Temperature Measurement"
        capability "Ultraviolet Index"

        /* Standard (Capability) Attributes
        attribute "battery", "number"
        attribute "powerSource", "enum", ["battery", "dc", "mains", "unknown"]
        attribute "tamper", "enum", ["detected", "clear"]
        attribute "temperature", "number"
        attribute "water", "enum", ["dry", "wet"]
        */

        // Custom Attributes
        attribute "batteryStatus", "string"
        attribute "batteryStatus", "string"     // Indicates DC-power or battery %.
        attribute "logMessage", "string"        // Important log messages.
        attribute "syncPending", "number"       // Number of config items that need to be synced with the physical device.
        attribute "switch1", "enum", ["on", "off"]
        attribute "switch2", "enum", ["on", "off"]

        // Custom Commands:
        command "resetTamper"
        command "sync"
        command "test"

        command "configure"
        command "getTemp"
        command "getPosition"
        command "getBattery"
        command "clearShock"

        fingerprint mfr: "0086", prod: "0102", model: "0064", deviceJoinName: "Aeotec MultiSensor 6"
    }

    preferences {
        input description: "Please consult AEOTEC MULTISENSOR 6 operating manual for advanced setting options. You can skip this configuration to use default settings", title: "Advanced Configuration", displayDuringSetup: true, type: "paragraph", element: "paragraph"

        input "motionDelayTime", "enum", title: "Motion Sensor Delay Time", options: ["10 seconds", "20 seconds", "40 seconds", "1 minute", "2 minutes", "3 minutes", "4 minutes"], defaultValue: "10 seconds", displayDuringSetup: true

        input "motionSensitivity", "enum", title: "Motion Sensor Sensitivity", options: ["Off", "Minimum", "Normal", "Maximum"], defaultValue: "Maximum", displayDuringSetup: true

        input "reportInterval", "enum", title: "Sensors Report Interval", options: ["8 minutes", "15 minutes", "30 minutes", "1 hour", "6 hours", "12 hours", "18 hours", "24 hours"], defaultValue: "1 hour", displayDuringSetup: true

        input "ledBlinking", "enum", title: "LED Blinking Setting", options: ["Enabled", "Only Motion", "Disabled"], defaultValue: "Disabled", displayDuringSetup: true
    }

    tiles(scale: 2) {
        multiAttributeTile(name: "motion", type: "generic", width: 6, height: 4) {
            tileAttribute("device.motion", key: "PRIMARY_CONTROL") {
                attributeState "active", label: 'motion', icon: "st.motion.motion.active", backgroundColor: "#00A0DC"
                attributeState "inactive", label: 'no motion', icon: "st.motion.motion.inactive", backgroundColor: "#cccccc"
            }
        }
        valueTile("temperature", "device.temperature", inactiveLabel: false, width: 2, height: 2) {
            state "temperature", label: '${currentValue}°',
                backgroundColors: [
                    [value: 32, color: "#153591"],
                    [value: 44, color: "#1e9cbb"],
                    [value: 59, color: "#90d2a7"],
                    [value: 74, color: "#44b621"],
                    [value: 84, color: "#f1d801"],
                    [value: 92, color: "#d04e00"],
                    [value: 98, color: "#bc2323"]
                ]
        }
        valueTile("humidity", "device.humidity", inactiveLabel: false, width: 2, height: 2) {
            state "humidity", label: '${currentValue}% humidity', unit: ""
        }
        valueTile("illuminance", "device.illuminance", inactiveLabel: false, width: 2, height: 2) {
            state "illuminance", label: '${currentValue} lux', unit: ""
        }
        valueTile("ultravioletIndex", "device.ultravioletIndex", inactiveLabel: false, width: 2, height: 2) {
            state "ultravioletIndex", label: '${currentValue} UV index', unit: ""
        }
        valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "battery", label: '${currentValue}% battery', unit: ""
        }
        valueTile("batteryStatus", "device.batteryStatus", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "batteryStatus", label: '${currentValue}', unit: ""
        }
        valueTile("powerSource", "device.powerSource", height: 2, width: 2, decoration: "flat") {
            state "powerSource", label: '${currentValue} powered', backgroundColor: "#ffffff"
        }
        valueTile("tamper", "device.tamper", height: 2, width: 2, decoration: "flat") {
            state "clear", label: 'tamper clear', backgroundColor: "#ffffff"
            state "detected", label: 'tampered', backgroundColor: "#ff0000"
        }
        main(["motion", "temperature", "humidity", "illuminance", "ultravioletIndex"])
        details(["motion", "temperature", "humidity", "illuminance", "ultravioletIndex", "batteryStatus", "tamper"])
    }
}


/*****************************************************************************************************************
 *  SmartThings System Methods
 *****************************************************************************************************************/
def installed() {
    logger('Performing initial setup', 'info')
    state.loggingLevelIDE = 5; state.loggingLevelDevice = 2
    sendEvent(name: 'checkInterval', value: intervals.checkIntervalDefault, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID], descriptionText: 'Default checkInterval')
    sendEvent(name: 'tamper', value: 'clear', descriptionText: 'Tamper cleared', displayed: false)
    getDeviceUseStates()
    sendEvent(name: "${getDataValue('event')}", value: "${getDataValue('inactiveState')}", displayed: false)
    if (isListening()) {
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
    logger('configure()', 'trace')
    device.updateSetting('configLoggingLevelIDE', value: '3')
    device.updateSetting('configLoggingLevelDevice', value: '2')
    device.updateSetting('configLoggingLevelDevice', value: 30)

    if (!isListening()) {
        def interval = (intervals.wakeUpIntervalSpecified) ?: intervals.wakeUpIntervalDefault
        state.wakeUpIntervalTarget = interval
        device.updateSetting('configWakeUpInterval', value: interval)
    }

    getParametersMetadata().findAll( { !it.readonly } ).each {
        def sv = configurationSpecified().find { sv -> sv.id == it.id }?.specifiedValue
        if (sv) {
            state."paramTarget${it.id}" = sv.toInteger()
            device.updateSetting("configParam${it.id}", sv)

        } else {
            device.updateSetting("configParam${it.id}", it.defaultValue)
        }
    }
    updateDataValue('serialNumber', null)
    state.syncAll = true
    updated()
}

def updated() {
    logger('updated()', 'trace')
    if (!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 2000) {
        state.updatedLastRanAt = now()

        state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3
        state.loggingLevelDevice = (settings.configLoggingLevelDevice) ? settings.configLoggingLevelDevice.toInteger(): 2
        state.autoResetTamperDelay = (settings.configAutoResetTamperDelay) ? settings.configAutoResetTamperDelay.toInteger() : 30

        getParametersMetadata().findAll( {!it.readonly} ).each {
            if (settings?."configParam${it.id}") {
                state."param${it.id}target" = settings."configParam${it.id}".toInteger()
            }
        }
        if (isListening()) {
            sync()
        } else {
            if (settings.configWakeUpInterval) {
                state.wakeUpIntervalTarget = settings.configWakeUpInterval
            }
            state.queued = [] as Set
            state.queued.plus('sync()')
        }
    } else {
        logger('updated(): Ran within last 2 seconds so aborting.', 'debug')
    }
}

def parse(description) {
    logger("parse(): Parsing raw message: ${description}","trace")
    def result = []
    if (description.startsWith("Err")) {
        if (description.startsWith("Err 106")) {
            logger("parse() >> Err 106", "error")
            result = createEvent( name: "secureInclusion", value: "failed", isStateChange: true,
                    descriptionText: "This sensor failed to complete the network security key exchange. If you are unable to control it via SmartThings, you must remove it from your network and add it again.")
        } else {
            logger("parse(): Unknown Error. Raw message: ${description}","error")
        }
    }
    else if (description != "updated") {
        def cmd = zwave.parse(description, commandClasses.versions)
        if (cmd) {
            result << zwaveEvent(cmd)
            if (isListening() & (device.latestValue('syncPending') > 0) & commandClasses.unsolicited.find { it = cmd.commandClassId } ) {
                sync()
            }
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
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
    logger("BasicSet(): Creating Motion event", "info")
    motionEvent(cmd.value) // responding to BasicSet - 2001 value FF
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd) {
    logger("SensorBinaryReport(): Creating Motion event", "info")
    motionEvent(cmd.sensorValue)
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) {
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
        log.warn "Need to handle this cmd.notificationType: ${cmd.notificationType}"
        result << createEvent(descriptionText: cmd.toString(), isStateChange: false)
    }
    result
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) {
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

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) { // *** to sort - use cached values?
    log.debug "ConfigurationReport: $cmd"

    def param = cmd.parameterNumber.toString().padLeft(3, "0")
    def paramValue = cmd.scaledConfigurationValue.toString()
    log.debug "Processing Configuration Report: (Parameter: $param, Value: $paramValue)"
    state.configurationReport << [(param): paramValue]
    if (state.configurationReport.size() == getConfigurationParameters().size()) {
        log.debug "All Configuration Values Reported"
        updateDataValue("configurationReport", state.configurationReport.collect {
            it
        }.join(","))
    }

    def result = []
    def value
    if (cmd.parameterNumber == 9 && cmd.configurationValue[0] == 0) {
        value = "dc"
        if (!isConfigured()) {
            log.debug("ConfigurationReport: configuring device")
            result << response(configure())
        }
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
def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
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

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    log.info "Executing zwaveEvent 72 (ManufacturerSpecificV2) : 05 (ManufacturerSpecificReport) with cmd: $cmd"
    log.debug "manufacturerId:   ${cmd.manufacturerId}"
    log.debug "manufacturerName: ${cmd.manufacturerName}"
    log.debug "productId:        ${cmd.productId}"
    log.debug "productTypeId:    ${cmd.productTypeId}"
    def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
    updateDataValue("MSR", msr)
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionCommandClassReport cmd) {
    def ccValue = Integer.toHexString(cmd.requestedCommandClass).toUpperCase()
    def ccVersion = cmd.commandClassVersion
    log.debug "Processing Command Class Version Report: (Command Class: $ccValue, Version: $ccVersion)"
    state.commandClassVersions << [(ccValue): ccVersion]
    if (state.commandClassVersions.size() == getCommandClasses().size()) {
        log.debug "All Command Class Versions Reported"
        updateDataValue("commandClassVersions", state.commandClassVersions.findAll {
            it.value > 0
        }.sort().collect {
            it
        }.join(","))
    }
    createEvent(descriptionText: "${device.displayName} Command Class Versions Report", isStateChange: true, data: [name: 'Version Command Class Report', requestedCommandClass: ccValue, commandClassVersion: ccVersion])
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpIntervalReport cmd) {
    def result = []
    def wakeupInterval = cmd.seconds
    log.debug "wakeupInterval: $wakeupInterval"
    updateDataValue("wakeupInterval", "$wakeupInterval")
    result << createEvent(descriptionText: "$device.displayName wakeupInterval: $wakeupInterval", isStateChange: false)
    // *** need to create checkInterval event
    result
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd) {
    def result = [createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)]
    def cmds = []
    if (!isConfigured()) {
        log.debug("late configure")
        result << response(configure())
    } else {
        log.debug("Device has been configured sending >> wakeUpNoMoreInformation()")
        cmds << zwave.wakeUpV1.wakeUpNoMoreInformation().format()
        result << response(cmds)
    }
    result
}

/*****************************************************************************************************************
 *  Zwave Network Protocol Events Handlers
*****************************************************************************************************************/
def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd) {
    log.debug "Powerlevel Report: $cmd"
    def powerLevel = -1 * cmd.powerLevel //	def timeout = cmd.timeout (1-255 s) - omit
    log.debug "Processing Powerlevel Report: (Powerlevel: $powerLevel dBm)"
    updateDataValue("powerLevel", powerLevel)
}

/*****************************************************************************************************************
 *  Zwave Transport Encapsulation Events Handlers
*****************************************************************************************************************/
def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand([0x31: 5, 0x30: 2, 0x84: 1])
    state.sec = 1
    log.debug "encapsulated: ${encapsulatedCommand}"
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand)
    } else {
        log.warn "Unable to extract encapsulated cmd from $cmd"
        createEvent(descriptionText: cmd.toString())
    }
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.NetworkKeyVerify cmd) {
    state.sec = 1
    log.info "Executing zwaveEvent 98 (SecurityV1): 07 (NetworkKeyVerify) with cmd: $cmd (node is securely included)"
    def result = [createEvent(name: "secureInclusion", value: "success", descriptionText: "Secure inclusion was successful", isStateChange: true)]
    result
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityCommandsSupportedReport cmd) {
    log.info "Executing zwaveEvent 98 (SecurityV1): 03 (SecurityCommandsSupportedReport) with cmd: $cmd"
    state.sec = 1
}

/*****************************************************************************************************************
 *  Zwave General Event Handler
*****************************************************************************************************************/
def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.debug "General zwaveEvent cmd: ${cmd}"
    createEvent(descriptionText: cmd.toString(), isStateChange: false)
}

/*****************************************************************************************************************
 *  Capability-related Commands
*****************************************************************************************************************/
def ping() {
    if (isListening()) {
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
    logger("resetTamper(): Resetting tamper alarm.","info")
    sendEvent(name: "tamper", value: "clear", descriptionText: "Tamper alarm cleared", displayed: true)
}

def syncAll() {
    state.syncAll = true
    (isListening()) ? sync() : state.queued.plus('sync()')
}

def test() {
    logger("test()","trace")
    (isListening()) ? testNow() : state.queued.plus('testNow()')
    }
}

private testNow() {
    logger("testRun()","trace")
    state.queued.minus('testNow()')
    def cmds = []

    sendEvent(descriptionText: "Requesting Powerlevel Report", displayed: false)
    cmds << zwave.powerlevelV1.powerlevelGet()

    sendEvent(descriptionText: "Requesting Command Class Report", displayed: false)
    state.commandClassVersions = [:]
    commandClasses.versions.each {
        cmds << zwave.versionV1.versionCommandClassGet(requestedCommandClass: it)
    }
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
            if (state.loggingLevelIDE >= 1) log.error msg
            if (state.loggingLevelDevice >= 1) sendEvent(name: "logMessage", value: "ERROR: ${msg}", displayed: false, isStateChange: true)
        break
        case "warn":
            if (state.loggingLevelIDE >= 2) log.warn msg
            if (state.loggingLevelDevice >= 2) sendEvent(name: "logMessage", value: "WARNING: ${msg}", displayed: false, isStateChange: true)
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

private sync() {
    logger("sync(): Syncing configuration with the physical device.","info")
    def cmds = []
    def syncPending = 0
    if (state.syncAll) {
        state.wakeUpIntervalCache = null
        getParametersMetadata().findAll( {!it.readonly} ).each { state."paramCache${it.id}" = null }
        setDataValue('serialNumber', null)
        state.syncAll = false
    }
    if ((!isListening() & (state.wakeUpIntervalTarget != null) & (state.wakeUpIntervalTarget != state.wakeUpIntervalCache)) {
        cmds << zwave.wakeUpV1.wakeUpIntervalSet(seconds: state.wakeUpIntervalTarget, nodeid: zwaveHubNodeId)
        cmds << zwave.wakeUpV1.wakeUpIntervalGet()
        logger("sync(): Syncing Wake Up Interval: New Value: ${state.wakeUpIntervalTarget}","info")
        syncPending++
    }
    getParametersMetadata().findAll( {!it.readonly} ).each {
        if ((state."paramTarget${it.id}" != null) & (state."paramCache${it.id}" != state."paramTarget${it.id}")) {
            cmds << zwave.configurationV1.configurationSet(parameterNumber: it.id, size: it.size, scaledConfigurationValue: state."paramTarget${it.id}".toInteger())
            cmds << zwave.configurationV1.configurationGet(parameterNumber: it.id)
            logger("sync(): Syncing parameter #${it.id} [${it.name}]: New Value: " + state."paramTarget${it.id}","info")
            syncPending++
        }
    }
    if (getDataValue("serialNumber") = null) {
        cmds << zwave.manufacturerSpecificV2.deviceSpecificGet(deviceIdType: 1)
        syncPending++
    }
    sendEvent(name: "syncPending", value: syncPending, displayed: false)
    sendCommandSequence(cmds)
}

private updateSyncPending() {
    def syncPending = 0
    def userConfig = 0
    if (state.syncAll) {
        state.wakeUpIntervalCache = null
        getParametersMetadata().findAll( {!it.readonly} ).each { state."paramCache${it.id}" = null }
        setDataValue('serialNumber', null)
        state.syncAll = false
    }
    if (!isListening() {
        def target = state.wakeUpIntervalTarget
        if ((target != null) & (target != state.wakeUpIntervalCache)) {
            syncPending++
        }
        if (target != intervals.wakeUpIntervalSpecified) {
            userConfig++
        }
    }
    getParametersMetadata().findAll( {!it.readonly} ).each {
        def target = state."paramTarget${it.id}"
        if ((target != null) & (target != state."paramCache${it.id}")) {
            syncPending++
        }
        def sv = configurationSpecified().find { sv -> sv.id == it.id }?.specifiedValue
        if (sv & (target != sv)) {
            userConfig++
        } else if (target != it.defaultValue) {
            userConfig++
        }
    }
    if (getDataValue('serialNumber') = null) {
        syncPending++
    }
    logger("updateSyncPending(): syncPending: ${syncPending}", "debug")
    if ((syncPending == 0) & (device.latestValue('syncPending') > 0)) {
        logger("Sync Complete.", "info")
        def ct = (userConfig > 0) 'user' : 'specified'
        updateDataValue('configurationType', ct)
    }
    sendEvent(name: "syncPending", value: syncPending, displayed: false)
}

private generatePrefsParams() {
    section {
        input (
            type: "paragraph",
            element: "paragraph",
            title: "DEVICE PARAMETERS:",
            description: "Device parameters are used to customise the physical device. " +
                         "Refer to the product documentation for a full description of each parameter."
        )
        getParametersMetadata().findAll( {!it.readonly} ).each { // Exclude readonly parameters.
            def lb = (it.description.length() > 0) ? "\n" : ""
            switch(it.type) {
                case "number":
                    input (
                        name: "configParam${it.id}",
                        title: "#${it.id}: ${it.name}: \n" + it.description + lb +"Default Value: ${it.defaultValue}",
                        type: it.type,
                        range: it.range,
                        // defaultValue: it.defaultValue, // iPhone users can uncomment these lines!
                        required: it.required
                    )
                break
                case "enum":
                    input (
                        name: "configParam${it.id}",
                        title: "#${it.id}: ${it.name}: \n" + it.description + lb + "Default Value: ${it.defaultValue}",
                        type: it.type,
                        options: it.options,
                        // defaultValue: it.defaultValue, // iPhone users can uncomment these lines!
                        required: it.required
                    )
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
    // delayBetween(commands.collect { encapsulate(it) }, delay)
    if (!isListening()) commands << zwave.wakeUpV1.wakeUpNoMoreInformation()
    sendHubCommand(commands.collect { encapsulate(response(it)) }, delay) // not sure of this code
}

private encapsulate(physicalgraph.zwave.Command cmd) {
    if (zwaveInfo.zw.endsWith("s") && getSecureClasses.find{ it == cmd.commandClassId }) { secureEncapsulate(cmd) }
    else if (zwaveInfo.cc.contains("56")){ crc16Encapsulate(cmd) }
    else { cmd.format() }
}

private secureEncapsulate(physicalgraph.zwave.Command cmd) {
    zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crc16Encapsulate(physicalgraph.zwave.Command cmd) {
    zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

/*****************************************************************************************************************
 *  Specific Helper Methods
*****************************************************************************************************************/
private deviceUseStates() {
    def useStates = [
        Bed: [event: 'contact', inactive: 'empty', active: 'occupied'],
        Chair: [event: 'contact', inactive: 'vacant', active: 'occupied'],
        Toilet: [event: 'contact', inactive: 'full', active: 'flushing'],
        Water: [event: 'water', inactive: 'dry', active: 'wet']
    ]
    def event = (settings.deviceUse) ? useStates."${settings.deviceUse}".event : 'water'
    def inactiveState = (settings.deviceUse) ? useStates."${settings.deviceUse}".inactive : 'dry'
    def activeState = (settings.deviceUse) ? useStates."${settings.deviceUse}".active : 'wet'
    updateDataValue('deviceUse', settings.deviceUse)
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

def motionEvent(value) {
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







private def getTimeOptionValueMap() {
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
def commandClasses = [
    supported: [0x20, 0x22, 0x25, 0x26, 0x27, 0x2B, 0x30, 0x31, 0x32, 0x33, 0x56, 0x59, 0x5A, 0x5E, 0x60, 0x70, 0x71, 0x72, 0x73, 0x75, 0x7A, 0x80, 0x84, 0x85, 0x86, 0x8E, 0x98, 0x9C],
    secure: [0x20, 0x2B, 0x30, 0x5A, 0x70, 0x71, 0x84, 0x85, 0x8E, 0x9C],
    unsolicited: [0x20, 0x30, 0x31, 0x60, 0x71, 0x9C],
    versions: [0x20: 1, 0x22: 1, 0x2B: 1, 0x30: 2, 0x56: 1, 0x59: 1, 0x5A: 1, 0x5E: 2, 0x70: 2, 0x71: 3, 0x72: 2, 0x73: 1, 0x7A: 2, 0x80: 1, 0x84: 2, 0x85: 2, 0x86: 1, 0x8E: 2, 0x98: 1, 0x9C: 1]
]

private configurationSpecified() { [
    [id: 3, size: 2, specifiedValue: 20], // motion delay
    [id: 4, size: 1, specifiedValue: 5], // maximum motion sensitivity
    [id: 8, size: 1, specifiedValue: 30], // increase duration of wakeup from 15 to 30 seconds
    [id: 111, size: 4, specifiedValue: 3600], // report every 1 hour
    [id: 40, size: 1, specifiedValue: 0], // disable report automatically on threshold change
    [id: 81, size: 1, specifiedValue: 2], // disable LED
    [id: 101, size: 4, specifiedValue: 240] // all except battery
] }

def configurationParameters = [1, 2, 3, 4, 10, 11, 12, 13, 14, 15, 20, 30, 31, 50, 51, 52, 53, 54, 55, 56, 70, 71, 72]

def intervals = [
    wakeUpIntervalDefault: 4_000,
    checkIntervalDefault: ,
    wakeUpIntervalSpecified: 86_400,
    checkIntervalSpecified: 180_000,
    batteryRefresh: 604_800
]

private getCommandClassVersions() { [
    0x20: 1, // Basic V1
    0x30: 1, // Sensor Binary V1 (not even v2).
    0x31: 2, // Sensor Multilevel V?
    0x60: 3, // Multi Channel V?
    0x70: 1, // Configuration V1
    0x71: 1, // Alarm (Notification) V1
    0x72: 2, // Manufacturer Specific V2
    0x7A: 2, // Firmware Update MD V2
    0x80: 1, // Battery V1
    0x84: 1, // Wake Up V1
    0x85: 2, // Association V2
    0x86: 1, // Version V1
    0x8E: 2, // Multi Channel Association V2
    0x9C: 1  // Sensor Alarm V1
] }

private parametersMetadata() { [
    [id:  1, size: 2, type: "number", range: "0..3600", defaultValue: 0, required: false, readonly: false,
        isSigned: true,
        name: "Alarm Cancellation Delay",
        description: "The time for which the device will retain the flood state after flooding has ceased.\n" +
        "Values: 0-3600 = Time Delay (s)"],
    [id: 2, size: 1, type: "enum", defaultValue: "3", required: false, readonly: false,
        isSigned: true,
        name: "Acoustic and Visual Alarms",
        description : "Disable/enable LED indicator and acoustic alarm for flooding detection.",
        options: ["0" : "0: Acoustic alarm INACTIVE. Visual alarm INACVTIVE",
                    "1" : "1: Acoustic alarm INACTIVE. Visual alarm ACTIVE",
                    "2" : "2: Acoustic alarm ACTIVE. Visual alarm INACTIVE",
                    "3" : "3: Acoustic alarm ACTIVE. Visual alarm ACTIVE"] ],
    [id: 5, size: 1, type: "enum", defaultValue: "255", required: false, readonly: false,
        isSigned: false,
        name: "Type of Alarm sent to Association Group 1",
        description : "",
        options: ["0" : "0: ALARM WATER command",
                    "255" : "255: BASIC_SET command"] ],
    [id: 7, size: 1, type: "number", range: "1..255", defaultValue: 255, required: false, readonly: false,
        isSigned: false,
        name: "Level sent to Association Group 1",
        description : "Determines the level sent (BASIC_SET) to Association Group 1 on alarm.\n" +
        "Values: 1-99 = Level\n255 = Last memorised state"],
    [id: 9, size: 1, type: "enum", defaultValue: "1", required: false, readonly: false,
        isSigned: true,
        name: "Alarm Cancelling",
        description : "",
        options: ["0" : "0: Alarm cancellation INACTIVE",
                    "1" : "1: Alarm cancellation ACTIVE"] ],
    [id: 10, size: 2, type: "number", range: "1..65535", defaultValue: 300, required: false, readonly: false,
        isSigned: false,
        name: "Temperature Measurement Interval",
        description : "Time between consecutive temperature measurements. New temperature value is reported to " +
        "the main controller only if it differs from the previously measured by hysteresis (parameter #12).\n" +
        "Values: 1-65535 = Time (s)"],
    [id: 12, size: 2, type: "number", range: "1..1000", defaultValue: 50, required: false, readonly: false,
        isSigned: true,
        name: "Temperature Measurement Hysteresis",
        description : "Determines the minimum temperature change resulting in a temperature report being " +
        "sent to the main controller.\n" +
        "Values: 1-1000 = Temp change (in 0.01C steps)"],
    [id: 13, size: 1, type: "enum", defaultValue: "0", required: false, readonly: false,
        isSigned: true,
        name: "Alarm Broadcasts",
        description : "Determines if flood and tamper alarms are broadcast to all devices.",
        options: ["0" : "0: Flood alarm broadcast INACTIVE. Tamper alarm broadcast INACTIVE",
                    "1" : "1: Flood alarm broadcast ACTIVE. Tamper alarm broadcast INACTIVE",
                    "2" : "2: Flood alarm broadcast INACTIVE. Tamper alarm broadcast ACTIVE",
                    "3" : "3: Flood alarm broadcast ACTIVE. Tamper alarm broadcast ACTIVE"] ],
    [id: 50, size: 2, type: "number", range: "-10000..10000", defaultValue : 1500, required: false, readonly: false,
        isSigned: true,
        name: "Low Temperature Alarm Threshold",
        description : "Temperature below which LED indicator blinks (with a colour determined by Parameter #61).\n" +
        "Values: -10000-10000 = Temp (-100C to +100C in 0.01C steps)"],
    [id: 51, size: 2, type: "number", range: "-10000..10000", defaultValue : 3500, required: false, readonly: false,
        isSigned: true,
        name: "High Temperature Alarm Threshold",
        description : "Temperature above which LED indicator blinks (with a colour determined by Parameter #62).\n" +
        "Values: -10000-10000 = Temp (-100C to +100C in 0.01C steps)"],
    [id: 61, size: 4, type: "number", range: "0..16777215", defaultValue : 255, required: false, readonly: false,
        isSigned: false,
        name: "Low Temperature Alarm indicator Colour",
        description : "Indicated colour = 65536 * RED value + 256 * GREEN value + BLUE value.\n" +
        "Values: 0-16777215"],
    [id: 62, size: 4, type: "number", range: "0..16777215", defaultValue : 16711680, required: false, readonly: false,
        isSigned: false,
        name: "High Temperature Alarm indicator Colour",
        description : "Indicated colour = 65536 * RED value + 256 * GREEN value + BLUE value.\n" +
        "Values: 0-16777215"],
    [id: 63, size: 1, type: "enum", defaultValue: "2", required: false, readonly: false,
        isSigned: true,
        name: "LED Indicator Operation",
        description : "LED Indicator can be turned off to save battery.",
        options: ["0" : "0: OFF",
                    "1" : "1: BLINK (every temperature measurement)",
                    "2" : "2: CONTINUOUS (constant power only)"] ],
    [id: 73, size: 2, type: "number", range: "-10000..10000", defaultValue : 0, required: false, readonly: false,
        isSigned: true,
        name: "Temperature Measurement Compensation",
        description : "Temperature value to be added to or deducted to compensate for the difference between air " +
        "temperature and temperature at the floor level.\n" +
        "Values: -10000-10000 = Temp (-100C to +100C in 0.01C steps)"],
    [id: 74, size: 1, type: "enum", defaultValue: "2", required: false, readonly: false,
        isSigned: true,
        name: "Alarm Frame Sent to Association Group #2",
        description : "Turn on alarms resulting from movement and/or the TMP button released.",
        options: ["0" : "0: TMP Button INACTIVE. Movement INACTIVE",
                    "1" : "1: TMP Button ACTIVE. Movement INACTIVE",
                    "2" : "2: TMP Button INACTIVE. Movement ACTIVE",
                    "3" : "3: TMP Button ACTIVE. Movement ACTIVE"] ],
    [id: 75, size: 2, type: "number", range: "0..65535", defaultValue : 0, required: false, readonly: false,
        isSigned: false,
        name: "Visual and Audible Alarms Duration",
        description : "Time period after which the LED and audible alarm the will become quiet. ignored when parameter #2 is 0.\n" +
        "Values: 0 = Active indefinitely\n" +
        "1-65535 = Time (s)"],
    [id: 76, size: 2, type: "number", range: "0..65535", defaultValue : 0, required: false, readonly: false,
        isSigned: false,
        name: "Alarm Retransmission Time",
        description : "Time period after which an alarm frame will be retransmitted.\n" +
        "Values: 0 = No retransmission\n" +
        "1-65535 = Time (s)"],
    [id: 77, size: 1, type: "enum", defaultValue: "0", required: false, readonly: false,
        isSigned: true,
        name: "Flood Sensor Functionality",
        description : "Allows for turning off the internal flood sensor. Tamper and temperature sensor will remain active.",
        options: ["0" : "0: Flood sensor ACTIVE",
                    "1" : "1: Flood sensor INACTIVE"] ]
] }
