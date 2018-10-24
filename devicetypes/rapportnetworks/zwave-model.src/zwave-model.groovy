/*****************************************************************************************************************
 *  Copyright: David Lomas (codersaur)
 *  Name: Fibaro Flood Sensor Advanced
 *  Author: David Lomas (codersaur)
 *  Date: 2017-03-02
 *  Version: 1.00
 *  Source: https://github.com/codersaur/SmartThings/tree/master/devices/fibaro-flood-sensor
 *  Author: David Lomas (codersaur)
 *  Description: An advanced SmartThings device handler for the Fibaro Flood Sensor (FGFS-101) (EU),
 *   with firmware: 2.6 or older.
 *  For full information, including installation instructions, exmples, and version history, see:
 *   https://github.com/codersaur/SmartThings/tree/master/devices/fibaro-flood-sensor
 *  License:
 *   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License. You may obtain a copy of the License at:
 *      http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *   for the specific language governing permissions and limitations under the License.
 *****************************************************************************************************************/
metadata {
    definition (name: "Aeon Water Sensor 6", namespace: "robertvandervoort", author: "Robert Vandervoort", ocfDeviceType: "x.com.st.d.sensor.moisture") {
        capability "Battery"
        capability "Power Source"
        capability "Configuration"
        capability "Notification"
        capability "Sensor"
        capability "Switch"
        capability "Shock Sensor"
        capability "Temperature Measurement"
        capability "Water Sensor"
        capability "Zw Multichannel"

        /* Standard (Capability) Attributes:
        attribute "battery", "number"
        attribute "powerSource", "enum", ["battery", "dc", "mains", "unknown"]
        attribute "tamper", "enum", ["detected", "clear"]
        attribute "temperature", "number"
        attribute "water", "enum", ["dry", "wet"]
        */

        // Custom Attributes:
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

        // Fingerprints:
        fingerprint mfr: "0086", prod: "0102", model: "007A"
    }

    simulator {
      status "dry": "command: 3003, payload: 00"
      status "wet": "command: 3003, payload: FF"
    }

    tiles(scale: 2) {
        multiAttributeTile(name:"multiTile", type:"generic", width:6, height:4) {
            tileAttribute("device.water", key: "PRIMARY_CONTROL") {
                attributeState "dry", label:'', icon:"st.alarm.water.dry", backgroundColor:"#79b821"
                attributeState "wet", label:'', icon:"st.alarm.water.wet", backgroundColor:"#53a7c0"
            }
            tileAttribute("device.temperature", key: "SECONDARY_CONTROL") {
                attributeState "temperature", label:'Temperature: ${currentValue}°C'
            }
        }
        standardTile("water", "device.water", width: 2, height: 2, canChangeIcon: true) {
            state "dry", icon:"st.alarm.water.dry", backgroundColor:"#ffffff"
            state "wet", icon:"st.alarm.water.wet", backgroundColor:"#53a7c0"
        }
        valueTile("temperature", "device.temperature", width: 2, height: 2) {
            state "temperature", label:'${currentValue}°C'
        }
        standardTile("tamper", "device.tamper", decoration: "flat", width: 2, height: 2) {
            state("default", label:"tampered", icon:"st.security.alarm.alarm", backgroundColor:"#FF6600", action: "resetTamper")
            state("clear", label:"clear", icon:"st.security.alarm.clear", backgroundColor:"#ffffff")
        }
        valueTile("battery", "device.battery", width: 2, height: 2, decoration: "flat") {
            state "battery", label:'Battery: ${currentValue}%'
        }
        standardTile("powerSource", "device.powerSource", width: 2, height: 2, decoration: "flat") {
            state "powerSource", label:'${currentValue}-Powered'
        }
        valueTile("batteryStatus", "device.batteryStatus", width: 2, height: 2, decoration: "flat", inactiveLabel: false) {
            state "batteryStatus", label:'${currentValue}', unit:""
        }
        standardTile("syncPending", "device.syncPending", decoration: "flat", width: 2, height: 2) {
            state "default", label:'Sync Pending', backgroundColor:"#FF6600", action:"syncAll"
            state "0", label:'Synced', action:"", backgroundColor:"#79b821"
        }
        standardTile("test", "device.test", decoration: "flat", width: 2, height: 2) {
            state "default", label:'Test', action:"test"
        }
        // syncAll

        main(["water","temperature"])
        details([
            "multiTile",
            //"water", // Also in multiTile.
            //"temperature", // Also in multiTile.
            //"battery",
            //"powerSource",
            "batteryStatus",
            "tamper",
            "syncPending"
            //,"test"
        ])
    }

    preferences {
        section {
            input(
                type: "paragraph",
                element: "paragraph",
                title: "GENERAL:",
                description: "General device handler settings."
            )
            input(
                name: "configLoggingLevelIDE",
                title: "IDE Live Logging Level: Messages with this level and higher will be logged to the IDE.",
                type: "enum",
                options: [
                    "0" : "None",
                    "1" : "Error",
                    "2" : "Warning",
                    "3" : "Info",
                    "4" : "Debug",
                    "5" : "Trace"
                ],
                defaultValue: "3",
                required: true
            )
            input(
                name: "configLoggingLevelDevice",
                title: "Device Logging Level: Messages with this level and higher will be logged to the logMessage attribute.",
                type: "enum",
                options: [
                    "0" : "None",
                    "1" : "Error",
                    "2" : "Warning"
                ],
                defaultValue: "2",
                required: true
            )
            input(
                name: "configAutoResetTamperDelay",
                title: "Auto-Reset Tamper Alarm:\n" +
                "Automatically reset tamper alarms after this time delay.\n" +
                "Values: 0 = Auto-reset Disabled\n" +
                "1-86400 = Delay (s)\n" +
                "Default Value: 30s",
                type: "number",
                defaultValue: "30",
                required: false
            )
        }
        section {
            input(
                name: "configWakeUpInterval",
                title: "WAKE UP INTERVAL:\n" +
                "The device will wake up after each defined time interval to sync configuration parameters, " +
                "associations and settings.\n" +
                "Values: 5-86399 = Interval (s)\n" +
                "Default Value: 4000 (every 66 minutes)",
                type: "number",
                defaultValue: "4000",
                required: false
            )
        }
        section {
            input(
                name: "deviceUse",
                title: "What type of sensor do you want to use this device for?",
                description: "Tap to set",
                type: "enum",
                options: ["Bed", "Chair", "Toilet", "Water"],
                defaultValue: "Water",
                required: true,
                displayDuringSetup: true
            )
        }
        generatePrefsParams()
        // generatePrefsAssocGroups()
    }
}


    if (isListening() & Notsynced & commandClasses.unsolicited.find {} ) {
        sync()
    }


def parse(description) { // *** to sort
    logger("parse(): Parsing raw message: ${description}","trace")
    def result = []
    if (description.startsWith("Err")) {
        logger("parse(): Unknown Error. Raw message: ${description}","error")
    }
    else {
        def cmd = zwave.parse(description, getCommandClassVersions())
        if (cmd) {
            result += zwaveEvent(cmd)
        } else {
            logger("parse(): Could not parse raw message: ${description}","error")
        }
    }
    return result
}

/*****************************************************************************************************************
 *  Z-wave Event Handlers.
 *****************************************************************************************************************/

/**
 *  zwaveEvent( COMMAND_CLASS_BASIC V1 (0x20) : BASIC_SET )
 *  Note: If this command is received by the hub, the hub will be in Associatin Group 1, and parameter #5 set to 255.
 *   The hub should also receive a corresponding SensorAlarmReport anyway.
 *  Action: Log water event.
 *  cmd attributes:
 *    Short    value
 *      0x00       = Off       = Dry
 *      0x01..0x63 = 0..100%   = Wet
 *      0xFE       = Unknown
 *      0xFF       = On        = Wet
 **/
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
    logger("zwaveEvent(): Basic Set received: ${cmd}","trace")
    def map = [:]
    map.name = "water"
    map.value = cmd.value ? "wet" : "dry"
    map.descriptionText = "${device.displayName} is ${map.value}"
    return createEvent(map)
}

/**
 *  zwaveEvent( COMMAND_CLASS_SENSOR_BINARY V1 (0x30) : SENSOR_BINARY_REPORT (0x03) )
 *  The Sensor Binary Report command is used to advertise a sensor value.
 *   THIS COMMAND CLASS IS DEPRECIATED!
 *  Action: Do nothing, as we don't event know which sensor the value is from.
 **/
def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) {
    logger("zwaveEvent(): Sensor Binary Report received: ${cmd}","trace")
}

/**
 *  zwaveEvent( COMMAND_CLASS_SENSOR_MULTILEVEL V2 (0x31) : SENSOR_MULTILEVEL_REPORT (0x05) )
 *  The Multilevel Sensor Report Command is used by a multilevel sensor to advertise a sensor reading.
 *  Action: Raise appropriate type of event (and disp event) and log an info message.
 *  Note: SmartThings does not yet have capabilities corresponding to all possible sensor types, therefore
 *  some of the event types raised below are non-standard.
 **/
def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv2.SensorMultilevelReport cmd) {
    logger("zwaveEvent(): SensorMultilevelReport received: ${cmd}","trace")
    def result = []
    def map = [ displayed: true, value: cmd.scaledSensorValue.toString() ]
    def dispMap = [ displayed: false ]
    switch (cmd.sensorType) {
        case 1:
            map.name = "temperature"
            map.unit = (cmd.scale == 1) ? "F" : "C"
            break
        default:
            logger("zwaveEvent(): SensorMultilevelReport with unhandled sensorType: ${cmd}","warn")
            map.name = "unknown"
            map.unit = "unknown"
            break
    }
    logger("New sensor reading: Name: ${map.name}, Value: ${map.value}, Unit: ${map.unit}","info")
    result << createEvent(map)
    if (dispMap.name) { result << createEvent(dispMap) }
    return result
}

/**
 *  zwaveEvent( COMMAND_CLASS_MULTICHANNEL V4 (0x60) : MULTI_CHANNEL_CMD_ENCAP (0x0D))
 *  The Multi Channel Command Encapsulation command is used to encapsulate commands. Any command supported by
 *  a Multi Channel End Point may be encapsulated using this command.
 *  Action: Extract the encapsulated command and pass to the appropriate zwaveEvent() handler.
 *
 *  cmd attributes:
 *    Boolean      bitAddress           Set to true if multicast addressing is used.
 *    Short        command              Command identifier of the embedded command.
 *    Short        commandClass         Command Class identifier of the embedded command.
 *    Short        destinationEndPoint  Destination End Point.
 *    List<Short>  parameter            Carries the parameter(s) of the embedded command.
 *    Short        sourceEndPoint       Source End Point.
 *
 *  Example: MultiChannelCmdEncap(bitAddress: false, command: 1, commandClass: 32, destinationEndPoint: 0,
 *            parameter: [0], sourceEndPoint: 1)
 **/
def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
    logger("zwaveEvent(): Multi Channel Command Encapsulation command received: ${cmd}","trace")

    def encapsulatedCommand = cmd.encapsulatedCommand(getCommandClassVersions())
    if (!encapsulatedCommand) {
        logger("zwaveEvent(): Could not extract command from ${cmd}","error")
    } else {
        return zwaveEvent(encapsulatedCommand)
    }
}

/**
 *  zwaveEvent( COMMAND_CLASS_CONFIGURATION V1 (0x70) : CONFIGURATION_REPORT (0x06) )
 *  The Configuration Report Command is used to advertise the actual value of the advertised parameter.
 *  Action: Store the value in the parameter cache, update syncPending, and log an info message.
 *  Note: The Fibaro Flood Sensor documentation treats some parameter values as SIGNED and others as UNSIGNED!
 *   configurationValues are converted accordingly, using the isSigned attribute from getParamMd().
 *  Note: Ideally, we want to update the corresponding preference value shown on the Settings GUI, however this
 *  is not possible due to security restrictions in the SmartThings platform.
 *  cmd attributes:
 *    List<Short>  configurationValue        Value of parameter (byte array).
 *    Short        parameterNumber           Parameter ID.
 *    Integer      scaledConfigurationValue  Value of parameter (as signed int).
 *    Short        size                      Size of parameter's value (bytes).
 *  Example: ConfigurationReport(configurationValue: [0], parameterNumber: 14, reserved11: 0,
 *            scaledConfigurationValue: 0, size: 1)
 **/
def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
    logger("zwaveEvent(): Configuration Report received: ${cmd}","trace")

    def paramMd = getParametersMetadata().find( { it.id == cmd.parameterNumber })
    // Some values are treated as unsigned and some as signed, so we convert accordingly:
    def paramValue = (paramMd?.isSigned) ? cmd.scaledConfigurationValue : byteArrayToUInt(cmd.configurationValue)
    def signInfo = (paramMd?.isSigned) ? "SIGNED" : "UNSIGNED"

    state."paramCache${cmd.parameterNumber}" = paramValue
    logger("Parameter #${cmd.parameterNumber} [${paramMd?.name}] has value: ${paramValue} [${signInfo}]","info")
    updateSyncPending()
}

/**
 *  zwaveEvent( COMMAND_CLASS_NOTIFICATION V3 (0x71) : NOTIFICATION_REPORT (0x05) )
 *  The Notification Report Command is used to advertise notification information.
 *  Action: Raise appropriate type of event (e.g. fault, tamper, water) and log an info or warn message.
 *  Note: SmartThings does not yet have official capabilities definited for many types of notification. E.g. this
 *  handler raises 'fault' events, which is not part of any standard capability.
 *  cmd attributes:
 *    Short        event                  Event Type (see code below).
 *    List<Short>  eventParameter         Event Parameter(s) (depends on Event type).
 *    Short        eventParametersLength  Length of eventParameter.
 *    Short        notificationStatus     The notification reporting status of the device (depends on push or pull model).
 *    Short        notificationType       Notification Type (see code below).
 *    Boolean      sequence
 *    Short        v1AlarmLevel           Legacy Alarm Level from Alarm CC V1.
 *    Short        v1AlarmType            Legacy Alarm Type from Alarm CC V1.
 *    Short        zensorNetSourceNodeId  Source node ID
 *
 *  Example: NotificationReport(event: 8, eventParameter: [], eventParametersLength: 0, notificationStatus: 255,
 *    notificationType: 8, reserved61: 0, sequence: false, v1AlarmLevel: 0, v1AlarmType: 0, zensorNetSourceNodeId: 0)
 **/
def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) {
    logger("zwaveEvent(): Notification Report received: ${cmd}","trace")

    def result = []

    switch (cmd.notificationType) {
        case 4:  // Heat Alarm:
            switch (cmd.event) {
                case 0:  // Previous Events cleared:
                    // Do not send a fault clear event automatically.
                    logger("Heat Alarm Cleared","info")
                    break

                case 1:  // Overheat detected:
                case 2:  // Overheat detected, Unknown Location:
                    result << createEvent(name: "fault", value: "overheat", descriptionText: "Overheat detected!", displayed: true)
                    logger("Overheat detected!","warn")
                    break

                case 3:  // Rapid Temperature Rise:
                case 4:  // Rapid Temperature Rise, Unknown Location:
                    result << createEvent(name: "fault", value: "temperature", descriptionText: "Rapid temperature rise detected!", displayed: true)
                    logger("Rapid temperature rise detected!","warn")
                    break

                case 5:  // Underheat detected:
                case 6:  // Underheat detected, Unknown Location:
                    result << createEvent(name: "fault", value: "underheat", descriptionText: "Underheat detected!", displayed: true)
                    logger("Underheat detected!","warn")
                    break

                default:
                    logger("zwaveEvent(): Notification Report recieved with unhandled event: ${cmd}","warn")
                    break
            }
            break

        //case 5:  // Water Alarm: // Not Implemented yet. Should raise water/consumableStatus events etc...

        case 9:  // system:
            switch (cmd.event) {
                case 0:  // Previous Events cleared:
                    // Do not send a fault clear event automatically.
                    logger("Previous Events cleared","info")
                    break

                case 1:  // Harware Failure:
                case 3:  // Harware Failure (with manufacturer proprietary failure code):
                    result << createEvent(name: "fault", value: "hardware", descriptionText: "Hardware failure detected!", displayed: true)
                    logger("Hardware failure detected!","warn")
                    break

                case 2:  // Software Failure:
                case 4:  // Software Failure (with manufacturer proprietary failure code):
                    result << createEvent(name: "fault", value: "firmware", descriptionText: "Firmware failure detected!", displayed: true)
                    logger("Firmware failure detected!","warn")
                    break

                case 6:  // Tampering:
                    result << createEvent(name: "tamper", value: "detected", descriptionText: "Tampering: Product covering removed!", displayed: true)
                    logger("Tampering: Product covering removed!","warn")
                    if (state.autoResetTamperDelay > 0) runIn(state.autoResetTamperDelay, "resetTamper")
                    break

                default:
                    logger("zwaveEvent(): Notification Report recieved with unhandled event: ${cmd}","warn")
                    break
            }
            break

        default:
            logger("zwaveEvent(): Notification Report recieved with unhandled notificationType: ${cmd}","warn")
            break
    }

    return result
}

/**
 *  zwaveEvent( COMMAND_CLASS_FIRMWARE_UPDATE_MD V2 (0x7A) : FIRMWARE_MD_REPORT (0x02) )
 *  The Firmware Meta Data Report Command is used to advertise the status of the current firmware in the device.
 *  Action: Publish values as device 'data' and log an info message. No check is performed.
 *  cmd attributes:
 *    Integer  checksum        Checksum of the firmware image.
 *    Integer  firmwareId      Firware ID (this is not the firmware version).
 *    Integer  manufacturerId  Manufacturer ID.
 *
 *  Example: FirmwareMdReport(checksum: 50874, firmwareId: 274, manufacturerId: 271)
 **/
def zwaveEvent(physicalgraph.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd) {
    logger("zwaveEvent(): Firmware Metadata Report received: ${cmd}","trace")

    // Display as hex strings:
    def firmwareIdDisp = String.format("%04X",cmd.firmwareId)
    def checksumDisp = String.format("%04X",cmd.checksum)

    logger("Firmware Metadata Report: Firmware ID: ${firmwareIdDisp}, Checksum: ${checksumDisp}","info")

    updateDataValue("firmwareId","${firmwareIdDisp}")
    updateDataValue("firmwareChecksum","${checksumDisp}")
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
    logger("zwaveEvent(): Battery Report received: ${cmd}","trace")
    logger("Battery Level: ${cmd.batteryLevel}%","info")

    def result = []
    result << createEvent(name: "powerSource", value: "battery", descriptionText: "Device is using battery.")
    result << createEvent(name: "battery", value: cmd.batteryLevel, unit: "%", displayed: true)
    result << createEvent(name: "batteryStatus", value: "Battery: ${cmd.batteryLevel}%", displayed: false)

    return result
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpIntervalReport cmd) {
    logger("zwaveEvent(): Wakeup Interval Report received: ${cmd}","trace")

    state.wakeUpIntervalCache = cmd.seconds.toInteger()
    logger("Wake Up Interval is ${cmd.seconds} seconds.","info")
    updateSyncPending()
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd) { *** need to redo
    logger("zwaveEvent(): Wakeup Notification received: ${cmd}","trace")

    logger("Device Woke Up","info")

    def result = []

    result << response(zwave.batteryV1.batteryGet())
    result << response(zwave.firmwareUpdateMdV2.firmwareMdGet())
    result << response(zwave.manufacturerSpecificV2.manufacturerSpecificGet())
    result << response(zwave.versionV1.versionGet())

    // Send wakeUpNoMoreInformation command, but only if there is nothing more to sync:

    if (device.latestValue("syncPending").toInteger() == 0) result << response(zwave.wakeUpV1.wakeUpNoMoreInformation())
    if (state.testPending = true) testNow()

    return result
}

/**
 *  zwaveEvent( COMMAND_CLASS_SENSOR_ALARM V1 (0x9C) : SENSOR_ALARM_REPORT (0x02) )
 *  The Sensor Alarm Report command is used to advertise the alarm state.
 *   THIS COMMAND CLASS IS DEPRECIATED! But still used by the device.
 *  Action: Raies water or tamper event. Log info message.
 *  cmd attributes:
 *    Integer  seconds       Time the alarm has been active.
 *    Short    sensorState   Sensor state.
 *      0x00      = No Alarm
 *      0x01-0x64 = Alarm Severity
 *      0xFF      = Alarm.
 *    Short    sensorType    Sensor Type.
 *    Short    sourceNodeId  Z-Wave node ID of sending device.
 *
 *  Example: SensorAlarmReport(seconds: 0, sensorState: 255, sensorType: 0, sourceNodeId: 7)
 **/
def zwaveEvent(physicalgraph.zwave.commands.sensoralarmv1.SensorAlarmReport cmd) {
    logger("zwaveEvent(): Sensor Alarm Report received: ${cmd}","trace")

    def map = [:]

    switch (cmd.sensorType) {
        case 0:  // General Purpose Alarm
        case 1:  // Smoke Alarm (but used here as tamper)
            map.name = "tamper"
            map.isStateChange = true
            map.value = cmd.sensorState ? "detected" : "clear"
            map.descriptionText = "${device.displayName} has been tampered with."
            logger("Device has been tampered with!","info")
            if (state.autoResetTamperDelay > 0) runIn(state.autoResetTamperDelay, "resetTamper")
            break

        case 5:  // Water Leak Alarm
            map.name = "water"
            map.isStateChange = true
            map.value = cmd.sensorState ? "wet" : "dry"
            map.descriptionText = "${device.displayName} is ${map.value}."
            logger("Device is ${map.value}!","info")
        break

        default:
            logger("zwaveEvent(): SensorAlarmReport with unhandled sensorType: ${cmd}","warn")
            map.name = "unknown"
            map.value = cmd.sensorState
            break
    }

    return createEvent(map)
}

/**
 *  zwaveEvent( DEFAULT CATCHALL )
 *  Called for all commands that aren't handled above.
 **/
def zwaveEvent(physicalgraph.zwave.Command cmd) {
    logger("zwaveEvent(): No handler for command: ${cmd}","error")
}

/*****************************************************************************************************************
 *  Capability-related Commands: [None]
 *****************************************************************************************************************/
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

def ping() {

}

def refresh() {

}

def poll() { // depreciated

}

/*****************************************************************************************************************
 *  Custom Commands:
 *****************************************************************************************************************/
def resetTamper() {
    logger("resetTamper(): Resetting tamper alarm.","info")
    sendEvent(name: "tamper", value: "clear", descriptionText: "Tamper alarm cleared", displayed: true)
}

def syncAll() {
    state.syncAll = true
    (isListening()) ? sync() : updateSyncPending()
}

def test() {
    logger("test()","trace")
    (isListening()) ? testNow() : state.testPending = true
    }
}

private testNow() {
    logger("testRun()","trace")
    state.testPending = false
    def cmds = []
    sendEvent(descriptionText: "Requesting Command Class Report", displayed: false)
    state.commandClassVersions = [:]
    getCommandClasses.supported.each {
        cmds << zwave.versionV1.versionCommandClassGet(requestedCommandClass: it)
    }
    sendCommandSequence(cmds)
}

/*****************************************************************************************************************
* Send Commands
*****************************************************************************************************************/
private sendCommandSequence(commands, delay = 1200) {
    // delayBetween(commands.collect { encapsulate(it) }, delay)
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
 *  SmartThings System Commands:
 *****************************************************************************************************************/
def installed() {
    logger('Performing initial setup', 'info')
    state.loggingLevelIDE = 5; state.loggingLevelDevice = 2
    sendEvent(name: 'checkInterval', value: (2.1 * intervals.wakeUpIntervalDefault).round(), descriptionText: 'Default checkInterval')
    getDeviceUseStates()
    sendEvent(name: "${getDataValue('event')}", value: "${getDataValue('inactiveState')}", displayed: false)
    sendEvent(name: 'tamper', value: 'clear', descriptionText: 'Tamper cleared', displayed: false)
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
            updateSyncPending()
        }
    } else {
        logger('updated(): Ran within last 2 seconds so aborting.', 'debug')
    }
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/
def commandClasses = [
    supported: [0x20, 0x22, 0x25, 0x26, 0x27, 0x2B, 0x30, 0x31, 0x32, 0x33, 0x56, 0x59, 0x5A, 0x5E, 0x60, 0x70, 0x71, 0x72, 0x73, 0x75, 0x7A, 0x80, 0x84, 0x85, 0x86, 0x8E, 0x98, 0x9C],
    secure: [0x20, 0x2B, 0x30, 0x5A, 0x70, 0x71, 0x84, 0x85, 0x8E, 0x9C],
    unsolicited: [],
    versions: [0x20: 1, 0x22: 1, 0x2B: 1, 0x30: 2, 0x56: 1, 0x59: 1, 0x5A: 1, 0x5E: 2, 0x70: 2, 0x71: 3, 0x72: 2, 0x73: 1, 0x7A: 2, 0x80: 1, 0x84: 2, 0x85: 2, 0x86: 1, 0x8E: 2, 0x98: 1, 0x9C: 1]
]

private configurationSpecified() { [
    [id: 3, size: 1, specifiedValue: 0],
    [id: 50, size: 2, specifiedValue: 0]
] }

def configurationParameters = [1, 2, 3, 4, 10, 11, 12, 13, 14, 15, 20, 30, 31, 50, 51, 52, 53, 54, 55, 56, 70, 71, 72]

def intervals = [
    wakeUpIntervalDefault: 4_000,
    wakeUpIntervalSpecified: 64_800,
    checkInterval: 132_000,
    batteryRefresh:
]

private listening() {
    getZwaveInfo()?.zw?.startsWith("L")
}

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
 *  Static Matadata Functions
 *****************************************************************************************************************/
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

/*
private getAssociationGroupsMetadata() { [
    [id:  1, maxNodes: 5, name: "Device Status", // Water state?
        description : "Reports device state, sending BASIC SET or ALARM commands."],
    [id:  2, maxNodes: 5, name: "TMP Button and Tilt Sensor",
        description : "Sends ALARM commands to associated devices when TMP button is released or a tilt is triggered (depending on parameter 74)."],
    [id:  3, maxNodes: 0, name: "Device Status",
        description : "Reports device state. Main Z-Wave controller should be added to this group."]
] }
*/