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

def installed() {
    sendEvent(name: "tamper", value: "clear", displayed: false)
    createEvent checkInterval // initial value twice default Wake Up Interval
    setConfigured("false")
}

def updated() {
    setConfigured("false")

    def tamperValue = device.latestValue("tamper")

    if (tamperValue == "active") {
        sendEvent(name: "tamper", value: "detected", displayed: false)
    } else if (tamperValue == "inactive") {
        sendEvent(name: "tamper", value: "clear", displayed: false)
    }
}

def configure() {
    log.debug "Executing configure"
    def request = []

    log.debug "Setting 1st Association Group"  // create anonymous event - no point in logging
    request << zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: [zwaveHubNodeId])

    log.debug "Setting Configuration Parameters"
    request << zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: 0)
    request << zwave.configurationV1.configurationSet(parameterNumber: 50, size: 2, scaledConfigurationValue: 0)

    log.debug "Setting Wake Up Interval"
    request << zwave.wakeUpV2.wakeUpIntervalSet(seconds: 18 * 60 * 60, nodeid: zwaveHubNodeId)
    sendEvent(name: "checkInterval", value: 2 * 18 * 60 * 60 + 1 * 60 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])

    if (!getDataValue("commandClassVersions")) {
        log.debug "Requesting Command Class Report"
        state.commandClassVersions = [:]
        getCommandClasses().each {
            request << zwave.versionV1.versionCommandClassGet(requestedCommandClass: it)
        }
    }

    log.debug "Requesting Configuration Report"
    state.configurationReport = [:]
    getConfigurationParameters().each {
        request << zwave.configurationV1.configurationGet(parameterNumber: it)
    }

    if (!state.timeLastBatteryReport || (new Date().time) - state.timeLastBatteryReport > 7 * 24 * 60 * 60 * 1000) {
        log.debug "Requesting Powerlevel Report"
        request << zwave.powerlevelV1.powerlevelGet()
    }

    if (!state.timeLastBatteryReport || (new Date().time) - state.timeLastBatteryReport > 7 * 24 * 60 * 60 * 1000) {
        log.debug "Requesting Battery Report"
        request << zwave.batteryV1.batteryGet()
    }

    request << zwave.manufacturerSpecificV2.manufacturerSpecificGet()
    request << zwave.manufacturerSpecificV2.deviceSpecificGet()

    log.debug "Request Binary Sensor Report"
    request << zwave.sensorBinaryV2.sensorBinaryGet()

    // zwave.associationGrpInfoV1.associationGroupInfoGet
    // zwave.securityV1.securityCommandsSupportedGet
    // zwavePlusInfo ?

    request << zwave.wakeUpV2.wakeUpNoMoreInformation()
    setConfigured("true")
    encapSequence(request, 1200)
}

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
                displayed: true,
            )
        }
    } else if (description == "updated") {
        return null
    } else {
        def cmd = zwave.parse(description, [0x56: 1, 0x71: 3, 0x72: 2, 0x80: 1, 0x84: 2, 0x85: 2, 0x86: 1, 0x98: 1])

        if (cmd) {
            log.debug "Parsed '${cmd}'"
            zwaveEvent(cmd)
        }
    }
}


// Application Events
def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd) {
    log.debug "Sensor Binary Report: $cmd"
    def map = [:]
    map.name = "contact"
    map.value = cmd.sensorValue ? "full" : "flushing"
    if (map.value == "full") {
        map.descriptionText = "${device.displayName} is full"
    } else {
        map.descriptionText = "${device.displayName} is flushing"
    }
    createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) {
    log.debug "Notification Report: $cmd"
    // assumed that default notification events are used (i.e. parameter 20 = 0)
    def map = [:]
    if (cmd.notificationType == 6) {
        switch (cmd.event) {
            case 22:
                map.name = "contact"
                map.value = "full"
                map.descriptionText = "${device.displayName} is full"
                break
            case 23:
                map.name = "contact"
                map.value = "flushing"
                map.descriptionText = "${device.displayName} is flushing"
                break
        }
    } else if (cmd.notificationType == 7) {
        switch (cmd.event) {
            case 0:
                map.name = "tamper"
                map.value = "clear"
                map.descriptionText = "Tamper alert cleared"
                break
            case 3:
                map.name = "tamper"
                map.value = "detected"
                map.descriptionText = "Tamper alert: sensor removed or covering opened"
                break
        }
    }
    createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
    def result = [createEvent(descriptionText: "${device.displayName} Configuration Report Received", isStateChange: false, data: cmd)]
    log.debug "Configuration Report: $cmd" // create anonymous event ? set data: cmd
    def param = cmd.parameterNumber.toString().padLeft(3, "0")
    def paramValue = cmd.scaledConfigurationValue.toString()
    log.debug "Processing Configuration Report: (Parameter: $param, Value: $paramValue)"
    def untransformed = state.configurationReport << [(param): paramValue]
    if (untransformed.size() == getConfigurationParameters().size()) {
        log.debug "All Configuration Values Reported"
        updateDataValue("configurationReport", state.configurationReport.collect {
            it
        }.join(","))
    }
/*
    Could I do this? NEW
    def report = String new state.configurationReport.collect { it }.join(","))
*/
    state.configurationReport = untransformed
    result
}

// Management Events
def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd) {
    def result = [createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)]
    if (!isConfigured()) {
        log.debug "late configure"
        result << response(configure())
    } else if (!state.timeLastBatteryReport || (new Date().time) - state.timeLastBatteryReport > 7 * 24 * 60 * 60 * 1000) {
        result << response(zwave.batteryV1.batteryGet().format())
        result << response("delay 1200")
        result << response(zwave.wakeUpV1.wakeUpNoMoreInformation().format())
    } else {
        result << createEvent(name: 'battery', value: device.latestValue("battery"), unit: '%', isStateChange: true, displayed: false)
        result << response(zwave.wakeUpV1.wakeUpNoMoreInformation().format())
    }
    result
}

// physicalgraph.zwave.commands.applicationstatusv1.ApplicationBusy
// physicalgraph.zwave.commands.applicationstatusv1.ApplicationRejectedRequest

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
    log.debug "Battery Report: $cmd" // create anonymous event
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

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
// No point in logging - because will only appear when device wakes up!
// Integer	manufacturerId
// Integer	productId
// Integer	productTypeId
// List<Short>	payload
    log.debug "Manufacturer Specific Report: $cmd"
    log.debug "manufacturerId:   ${cmd.manufacturerId}"
    log.debug "manufacturerName: ${cmd.manufacturerName}"
    log.debug "productId:        ${cmd.productId}"
    log.debug "productTypeId:    ${cmd.productTypeId}"
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) {
// create anonymouse event

//    List<Short>	deviceIdData
//    Short	deviceIdDataFormat
//    Short	deviceIdDataLengthIndicator
//    Short	deviceIdType
//    List<Short>	payload

//    Short	DEVICE_ID_DATA_FORMAT_RESERVED0	= 0
//    Short	DEVICE_ID_TYPE_RESERVED0	= 0
//    Short	DEVICE_ID_TYPE_SERIAL_NUMBER	= 1
//    Short	DEVICE_ID_DATA_FORMAT_BINARY	= 1
    log.debug "Device Specific Report: $cmd"
    log.debug "deviceIdData:                ${cmd.deviceIdData}"
    log.debug "deviceIdDataFormat:          ${cmd.deviceIdDataFormat}"
    log.debug "deviceIdDataLengthIndicator: ${cmd.deviceIdDataLengthIndicator}"
    log.debug "deviceIdType:                ${cmd.deviceIdType}"

    if (cmd.deviceIdType == 1 && cmd.deviceIdDataFormat == 1) { //serial number in binary format
        String serialNumber = "h'"

        cmd.deviceIdData.each {
            data - >
                serialNumber += "${String.format(" % 02 X ", data)}"
        }

        updateDataValue("serialNumber", serialNumber)
        log.debug "${device.displayName} - serial number: ${serialNumber}"
    }
}

// Management Events
def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
// not sure this is needed - ST Hub already has this
    updateDataValue("version", "${cmd.applicationVersion}.${cmd.applicationSubVersion}")
    log.debug "applicationVersion:      ${cmd.applicationVersion}"
    log.debug "applicationSubVersion:   ${cmd.applicationSubVersion}"
    log.debug "zWaveLibraryType:        ${cmd.zWaveLibraryType}"
    log.debug "zWaveProtocolVersion:    ${cmd.zWaveProtocolVersion}"
    log.debug "zWaveProtocolSubVersion: ${cmd.zWaveProtocolSubVersion}"
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionCommandClassReport cmd) {
    log.debug "Command Class Versions Report: $cmd" // create anonymous event
    def ccValue = Integer.toHexString(cmd.requestedCommandClass).toUpperCase()
    def ccVersion = cmd.commandClassVersion
    log.debug "Processing Command Class Version Report: (Command Class: $ccValue, Version: $ccVersion)"
    def untransformed = state.commandClassVersions << [(ccValue): ccVersion]
    if (untransformed.size() == getCommandClasses().size()) {
        log.debug "All Command Class Versions Reported"
        updateDataValue("commandClassVersions", state.commandClassVersions.findAll {
            it.value > 0
        }.sort().collect {
            it
        }.join(","))
    }
    state.commandClassVersions = untransformed
    return state.commandClassVersions
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) {
    log.debug "${device.displayName}: received command: $cmd - device has reset itself"
}

// Network Protocol Events
def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd) {
    log.debug "Powerlevel Report: $cmd"
    def powerLevel = -1 * cmd.powerLevel // omitted cmd.timeout (1-255 s)
    log.debug "Processing Powerlevel Report: (Powerlevel: $powerLevel dBm)"
    updateDataValue("powerLevelReport", "powerLevel=${powerLevel}")
}

// Transport Encapsulation Events
def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
    def versions = [0x72: 2, 0x80: 1, 0x86: 1]
    def version = versions[cmd.commandClass as Integer]
    def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
    def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
    if (!encapsulatedCommand) {
        log.debug "Could not extract command from $cmd"
    } else {
        zwaveEvent(encapsulatedCommand)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand([0x71: 3, 0x84: 2, 0x85: 2, 0x98: 1])
    if (encapsulatedCommand) {
        return zwaveEvent(encapsulatedCommand)
    } else {
        log.warn "Unable to extract encapsulated cmd from $cmd"
        createEvent(descriptionText: cmd.toString())
    }
}

// Catch Event
def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.debug "Catchall reached for cmd: $cmd"
}

// Send Commands
private secure(physicalgraph.zwave.Command cmd) {
    zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crc16(physicalgraph.zwave.Command cmd) {
    //zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
    "5601${cmd.format()}0000"
}

private encapSequence(commands, delay = 1200) {
    delayBetween(commands.collect {
        encap(it)
    }, delay)
}

private encap(physicalgraph.zwave.Command cmd) {
    def secureClasses = [0x20, 0x2B, 0x30, 0x5A, 0x70, 0x71, 0x84, 0x85, 0x8E, 0x9C]

    //todo: check if secure inclusion was successful
    //if not do not send security-encapsulated command
    if (secureClasses.find {
            it == cmd.commandClassId
        }) {
        secure(cmd)
    } else {
        crc16(cmd)
    }
}

// Helper Functions
private getCommandClasses() {
    [
        0x20, 0x22, 0x25, 0x26, 0x27, 0x2B, 0x30, 0x31, 0x32, 0x33, 0x56, 0x59, 0x5A, 0x5E, 0x60, 0x70, 0x71, 0x72, 0x73, 0x75, 0x7A, 0x80, 0x84, 0x85, 0x86, 0x8E, 0x98, 0x9C
    ]
}

private getConfigurationParameters() {
    [
        1, 2, 3, 4, 10, 11, 12, 13, 14, 15, 20, 30, 31, 50, 51, 52, 53, 54, 55, 56, 70, 71, 72
    ]
}

private setConfigured(configure) {
    updateDataValue("configured", configure)
}

private isConfigured() {
    getDataValue("configured") == "true"
}