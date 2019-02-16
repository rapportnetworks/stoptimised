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
        capability "Battery"                       // attribute 'battery' (number)
        capability "Configuration"                 // command   'configure'
        capability "Health Check"                  // attribute 'checkInterval' (number); attribute 'DeviceWatch-DeviceStatus' (string); attribute 'healthStatus' (?); command 'ping'
        capability "Illuminance Measurement"       // attribute 'illuminance' (number)
        capability "Motion Sensor"                 // attribute 'motion' (enum: ['inactive', 'active'])
        capability "Power Source"                  // attribute 'powerSource' (enum: ['battery', 'dc', 'mains', 'unknown']
        capability "Relative Humidity Measurement" // attribute 'humidity' (number)
        capability "Tamper Alert"                  // attribute 'tamper' (enum: ['detected', 'clear'])
        capability "Temperature Measurement"       // attribute 'temperature' (number)
        capability "Ultraviolet Index"             // attribute 'ultravioletIndex' (number)

        // Custom Attributes
        attribute "batteryChange", "string"        // Used to log chaging of battery.
        attribute "batteryStatus", "string"        // Indicates DC-power or battery %.
        attribute "configure", "string"            // Reports on configuration command status.
        attribute "logMessage", "string"           // Important log messages.
        attribute "secureInclusion", "string"      // Indicates secure inclusion success/failed.
        attribute "syncPending", "number"          // Number of config items that need to be synced with the physical device.

        // Custom Commands
        command "batteryChange"                    // Manually logs change of battery.
        command "profile"                          // Manually initiates profiling of the device (power level, command class versions)
        command "resetLog"                         // Manually clears logMessage attribute
        command "resetTamper"                      // Manually resets tamper attribute to 'clear'
        command "syncAll"                          // Manually triggers the syncing of all device parameters
        command "syncRemaining"                    // Manually triggers the syncing of any unsynched parameters (or queues them is a sleepy device)

        // Data values
        // deviceUse:            what device (e.g. contact sensor) is being used for (optional configuration)
        // event:                attribute for device event (based on optional configuration)
        // inactiveState:        state when inactive (based on optional configuration)
        // activeState:          state when active (based on optional configuration)
        // commandClassVersions: list of all command classes supported by the device and their version numbers
        // configurationType:    indicator of way device is configured [default, specified, user]
        // configurationReport:  report of all configured parameter values (omits values that are not configured - i.e. remain at default values)
        // messages:             counter of all messages received by parse method (i.e. sent by device)
        // powerLevel:           device power level
        // wakeUpInterval:       device wake up interval
        // MSR:                  manufacturer specific report
        // serialNumber:         device unique serial number (if it has one)

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
        /**
         * tile displaying value of motion attribute
         */
        standardTile("motion", "device.motion", inactiveLabel: false, width: 2, height: 2) {
            state "active", label: 'motion', icon: "st.motion.motion.active", backgroundColor: "#00A0DC"
            state "inactive", label: 'no motion', icon: "st.motion.motion.inactive", backgroundColor: "#cccccc"
        }
        /**
         * tile displaying value of temperature attribute
         */
        valueTile("temperature", "device.temperature", inactiveLabel: false, width: 2, height: 2) {
            state "temperature", label: '${currentValue}°C', unit: "°C", backgroundColors: [[value: 0, color: "#153591"], [value: 7, color: "#1e9cbb"], [value: 15, color: "#90d2a7"], [value: 23, color: "#44b621"], [value: 29, color: "#f1d801"], [value: 33, color: "#d04e00"], [value: 37, color: "#bc2323"]],  defaultState: true
        }
        /**
         * tile displaying value of humidity attribute
         */
        valueTile("humidity", "device.humidity", inactiveLabel: false, width: 2, height: 2) {
            state "humidity", label: '${currentValue} % humidity', unit: "% humidity", defaultState: true
        }
        /**
         * tile displaying value of illuminance atribute
         */
        valueTile("illuminance", "device.illuminance", inactiveLabel: false, width: 2, height: 2) {
            state "illuminance", label: '${currentValue} lux', unit: "", defaultState: true
        }
        /**
         * tile displaying value of ultraviolet index attribute
         */
        valueTile("ultravioletIndex", "device.ultravioletIndex", inactiveLabel: false, width: 2, height: 2) {
            state "ultravioletIndex", label: '${currentValue} UV index', unit: "", defaultState: true
        }
        /**
         * tile displaying value of battery attribute
         */
        valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "battery", label: '${currentValue}% battery', unit: "", defaultState: true
        }
        /**
         * tile displaying value of battery status attribute
         */
        valueTile("batteryStatus", "device.batteryStatus", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "batteryStatus", label: '${currentValue}', unit: "", defaultState: true
        }
        /**
         * tile displaying value of power source attribute
         */
        valueTile("powerSource", "device.powerSource", height: 2, width: 2, decoration: "flat") {
            state "powerSource", label: '${currentValue} powered', backgroundColor: "#ffffff", defaultState: true
        }
        /**
         * tile displaying value of tamper attribute
         * on tap triggers the resetTamper command
         */
        standardTile("tamper", "device.tamper", height: 2, width: 2, decoration: "flat") {
            state "clear", label: 'tamper clear', backgroundColor: "#ffffff", defaultState: true, action: "resetTamper" // *** remove action after testing
            state "detected", label: 'tampered', icon: 'st.secondary.tools', action: "resetTamper", backgroundColor: "#ff0000"
        }
        /**
         * tile displaying value of syncPending attribute - number of configuration parameters that remain to be synched with the device
         * on tap triggers the syncRemaining command
         */
        valueTile("syncPending", "device.syncPending", height: 2, width: 2, decoration: "flat") {
            state "syncPending", label: '${currentValue} to sync', action: "syncRemaining", backgroundColor: "#ffffff", defaultState: true
        }
        /**
         * on tap triggers the syncAll command
         */
        standardTile("syncAll", "device.syncAll", height: 2, width: 2, decoration: "flat") {
            state "syncAll", label: 'Sync All', icon: 'st.secondary.tools', action: "syncAll", backgroundColor: "#ffffff", defaultState: true
        }
        /**
         * tile displaying value of logMessage attribute
         * on tap triggers the resetLog command
         */
        valueTile("logMessage", "device.logMessage", height: 2, width: 4, decoration: "flat") {
            state "clear", label: '${currentValue}', action: "resetLog", backgroundColor: "#ffffff", defaultState: true
        }
        /**
         * on tap triggers the batteryChange command (used to record a change of device battery)
         */
        standardTile("batteryChange", "device.batteryChange", height: 2, width: 2, decoration: "flat") {
            state "batteryChange", label: 'Battery Change', icon: 'st.secondary.tools', action: "batteryChange", backgroundColor: "#ffffff", defaultState: true
        }
        /**
         * on tap triggers the configure command (sets/resets device configuration parameters to default/specified values)
         */
        standardTile("configure", "device.configure", height: 2, width: 2, decoration: "flat") {
            state "configure", label: 'configure', icon: 'st.secondary.tools', action: "configure", backgroundColor: "#ffffff", defaultState: true
        }
        /**
         * on tap triggers the profile command (requests power level and command class versions reports from the device)
         */
        standardTile("profile", "device.profile", height: 2, width: 2, decoration: "flat") {
            state "profile", label: 'profile', icon: 'st.secondary.tools', action: "profile", backgroundColor: "#ffffff", defaultState: true
        }

        main(["motion", "temperature", "humidity", "illuminance", "ultravioletIndex"])
        details(["motion", "temperature", "humidity", "illuminance", "ultravioletIndex", "batteryStatus", "tamper", "syncPending", "syncAll", "logMessage", "batteryChange", "configure", "profile"])
    }

    simulator {
        status "no motion": "command: 9881, payload: 00300300"
        status "motion": "command: 9881, payload: 003003FF"

        for (int i = 0; i <= 100; i += 20) {
            status "temperature ${i}F": new physicalgraph.zwave.Zwave().securityV1.securityMessageEncapsulation().encapsulate(new physicalgraph.zwave.Zwave().sensorMultilevelV2.sensorMultilevelReport(scaledSensorValue: i, precision: 1, sensorType: 1, scale: 1)).incomingMessage()
        }

        for (int i = 0; i <= 100; i += 20) {
            status "humidity ${i}%": new physicalgraph.zwave.Zwave().securityV1.securityMessageEncapsulation().encapsulate(new physicalgraph.zwave.Zwave().sensorMultilevelV2.sensorMultilevelReport(scaledSensorValue: i, sensorType: 5)).incomingMessage()
        }

        for (int i in [0, 20, 89, 100, 200, 500, 1000]) {
            status "illuminance ${i} lux": new physicalgraph.zwave.Zwave().securityV1.securityMessageEncapsulation().encapsulate(new physicalgraph.zwave.Zwave().sensorMultilevelV2.sensorMultilevelReport(scaledSensorValue: i, sensorType: 3)).incomingMessage()
        }

        for (int i in [0, 5, 10, 15, 50, 99, 100]) {
            status "battery ${i}%": new physicalgraph.zwave.Zwave().securityV1.securityMessageEncapsulation().encapsulate(new physicalgraph.zwave.Zwave().batteryV1.batteryReport(batteryLevel: i)).incomingMessage()
        }

        status "low battery alert": new physicalgraph.zwave.Zwave().securityV1.securityMessageEncapsulation().encapsulate(new physicalgraph.zwave.Zwave().batteryV1.batteryReport(batteryLevel: 255)).incomingMessage()

        status "wake up": "command: 8407, payload: "

        // reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
        // reply "200100,delay 100,2502": "command: 2503, payload: 00"
    }

    preferences {
        if (configDevice()) {
            input(
                    name: 'paraSettings',
                    title: 'DEVICE SETTINGS',
                    description: 'Tap each item to set.',
                    type: 'paragraph',
                    element: 'paragraph'
            )

            if ('deviceUse' in configDevice()) {
                def uses = configUseStateOptions().collectEntries { [(it.item): it.use] }
                def defaultUse = configUseStateOptions().find { it.default }.item
                def defaultUseName = configUseStateOptions().find { it.default }.use
                input(
                        name: 'configDeviceUse',
                        title: "What type of sensor do you want to use this device for? (default: ${defaultUseName})",
                        type: 'enum',
                        options: uses,
                        defaultValue: defaultUse,
                        required: true,
                        displayDuringSetup: true
                )
            }

            if ('autoResetTamperDelay' in configDevice()) {
                input(
                        name: 'configAutoResetTamperDelay',
                        title: 'Auto-Reset Tamper Alarm after this time delay. (default: 30 seconds)',
                        type: 'enum',
                        options: [30: '30 seconds', 60: '1 minute', 120: '2 minutes', 300: '5 minutes', 3600: '1 hour'],
                        defaultValue: 30,
                        required: false
                )
            }

            if ('wakeUpInterval' in configDevice()) {
                def intervals = configWakeIntervalOptions().collectEntries { [(it.item): it.interval] }
                def defaultInterval = (configWakeIntervalOptions()?.find { it.specified }?.item) ?: configWakeIntervalOptions().find { it.default }.item
                def defaultIntervalName = (configWakeIntervalOptions()?.find { it.specified }?.interval) ?: configWakeIntervalOptions().find { it.default }.interval
                input(
                        name: 'configWakeUpInterval',
                        title: "Device Wake Up Interval. (default: ${defaultIntervalName})",
                        type: 'enum',
                        options: intervals,
                        defaultValue: defaultInterval,
                        required: false
                )
            }
        }

        if (configUser()) generatePrefsParams()

        if (configLogger()) {
            input(
                    name: 'paraLogger',
                    title: 'LOGGER SETTINGS',
                    description: 'Tap each item to set.',
                    type: 'paragraph',
                    element: 'paragraph'
            )

            if ('logLevelIDE' in configLogger()) {
                input(
                        name: 'configLogLevelIDE',
                        title: 'IDE Live Logging Level for messages with this level and higher.',
                        type: 'enum',
                        options: [0: 'None', 1: 'Error', 2: 'Warning', 3: 'Info', 4: 'Debug', 5: 'Trace'],
                        defaultValue: 3,
                        required: false
                )
            }

            if ('logLevelDevice' in configLogger()) {
                input(
                        name: 'configLogLevelDevice',
                        title: 'Device Logging Level for messages with this level and higher.',
                        type: 'enum',
                        options: [0: 'None', 1: 'Error', 2: 'Warning'],
                        defaultValue: 2,
                        required: false
                )
            }
        }
    }
}

/***********************************************************************************************************************
 * Preferences Helper Methods (generatePrefsParams, getTimeOptionValueMap)
 **********************************************************************************************************************/
/**
 * generatePrefsParams
 * @return
 */
private generatePrefsParams() {
    input (name: 'paraParameters', title: 'DEVICE PARAMETERS', description: 'These are used to customise the operation of the device. Refer to the product documentation for a full description of each parameter.', type: 'paragraph', element: 'paragraph')
    paramsMetadata().findAll{ !it.readonly }.each{

        if (configUser()[0] == 0 || it.id in configUser()) {

            def id = it.id.toString().padLeft(3, '0')

            def specified = configSpecified()?.find { cs -> cs.id == it.id }

            def prefDefaultValue = (specified) ? specified.specifiedValue : it.defaultValue

            switch(it.type) {
                case 'number':
                    input(
                            name: "configParam${id}",
                            title: "${it.id}. ${it.name} (default: ${prefDefaultValue})",
                            description: "${it.description} (default: ${prefDefaultValue})",
                            type: it.type,
                            range: it.range,
                            defaultValue: prefDefaultValue,
                            required: it.required
                    )
                    break

                case 'enum':
                    input(
                            name: "configParam${id}",
                            title: "${it.id}. ${it.name} (default: ${it.options?.find { op -> op.key == prefDefaultValue}.value})",
                            description: "${it.description} (default: ${it.options?.find { op -> op.key == prefDefaultValue}.value})",
                            type: it.type,
                            options: it.options,
                            defaultValue: prefDefaultValue,
                            required: it.required
                    )
                    break

                case 'bool':
                    input(
                            name: "configParam${id}",
                            title: "${it.id}. ${it.name} (default: ${(prefDefaultValue) ? 'on' : 'off'})",
                            description: "${it.description} (default: ${(prefDefaultValue) ? 'on' : 'off'})",
                            type: it.type,
                            defaultValue: prefDefaultValue,
                            required: it.required
                    )
                    break

                case 'flags':
                    input(
                            name: "paraFlags${id}",
                            title: "${it.id}. ${it.name}",
                            description: it.description,
                            type: 'paragraph',
                            element: 'paragraph'
                    )
                    it.flags.each { f ->
                        def specifiedFlagValue = (specified) ? specified?.flags.find { sf -> sf.id == f.id }?.specifiedValue : f.defaultValue

                        def prefDefaultFlagValue = (specifiedFlagValue == f.flagValue) ? true : false
                        input(
                                name: "configParam${id}${f.id}",
                                title: "${f.id}) ${f.description} (default: ${(prefDefaultFlagValue) ? 'on' : 'off'})",
                                type: 'bool',
                                defaultValue: prefDefaultFlagValue,
                                required: it.required
                        )
                    }
                    break

                default:
                    logger('preferences: Unhandled preference type.', 'warning')
            }
        }
    }
}

/**
 * getTimeOptionValueMap
 * @return
 */
private getTimeOptionValueMap() { [ // TODO - create a generalised lookup list
    '10 seconds': 10,
    '20 seconds': 20,
    '40 seconds': 40,
    '1 minute': 60,
    '2 minutes': 2 * 60,
    '3 minutes': 3 * 60,
    '4 minutes': 4 * 60,
    '5 minutes': 5 * 60,
    '8 minutes': 8 * 60,
    '15 minutes': 15 * 60,
    '30 minutes': 30 * 60,
    '1 hour': 60 * 60,
    '6 hours': 6 * 60 * 60,
    '12 hours': 12 * 60 * 60,
    '18 hours': 18 * 60 * 60,
    '24 hours': 24 * 60 * 60
] }

/***********************************************************************************************************************
 * Main System Methods (installed, configure, updated)
 * Note: configure is in fact a command
 **********************************************************************************************************************/
/**
 * installed is called on first installation of device and sets up initial device states
 */
def installed() {
    /**
     * sets initial logging levels
     */
    state.logLevelIDE = 5; state.logLevelDevice = 2

    logger('installed: setting initial state of device attributes', 'info')

    /**
     * sends event to set checkInterval default value TODO - need to sort defaultCheckInterval - calculate? based on default/specified option?
     */
    sendEvent(name: 'checkInterval', value: configIntervals().defaultCheckInterval, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID], descriptionText: 'Default checkInterval')

    /**
     * sends event to clear any tamper due to installation
     */
    sendEvent(name: 'tamper', value: 'clear', descriptionText: 'Tamper cleared', displayed: false)

    /**
     * sets device use (if an option) and sends event to set inactive state
     */
    if ('deviceUse' in configDevice()) {
        logger('installed: setting device use states', 'debug')
        deviceUseStates()
        sendEvent(name: "${getDataValue('event')}", value: "${getDataValue('inactiveState')}", displayed: false)
    }

    /**
     * checks whether device is listening or sleepy (on battery) and sends events to set appropriate states
     */
    if (listening()) {
        logger('Device is in listening mode (powered).', 'debug')
        sendEvent(name: 'powerSource', value: 'dc', descriptionText: 'Device is connected to DC power supply.')
        sendEvent(name: 'batteryStatus', value: 'DC-power', displayed: false)
    } else {
        logger('Device is in sleepy mode (battery).', 'debug')
        sendEvent(name: 'powerSource', value: 'battery', descriptionText: 'Device is using battery.')
    }

    /**
     * sets up counter to track number of messages sent by device
     */
    state.messages = 0
}

/**
 * configure() is called by ST system after installed() and also in response to configure command sent to device (via mobile app or smart app)
 * Sets/resets device by setting configuration values to default/specified values.
 */
def configure() {
    logger('configure: Setting/Resetting device configuration targets to default/specified values.', 'info')

    sendEvent(name: 'configure', value: 'received', descriptionText: 'Configuration command received by device.', isStateChange: true, displayed: false) // custom attribute to report status to Configurator SmartApp

    state.configuring = true

    def autoResetTamperDelayDefault = 30
    logger("configure: Resetting autoResetTamperDelay preference to ${autoResetTamperDelayDefault}.", 'trace')
    state.autoResetTamperDelay = autoResetTamperDelayDefault
    try { device.updateSetting('configAutoResetTamperDelay', autoResetTamperDelayDefault) }
    catch(e) {}

    def logLevelIDEDefault = 5
    logger("configure: Resetting configLogLevelIDE preference to ${logLevelIDEDefault}.", 'trace') // TODO - set to 3 when finished debugging
    state.logLevelIDE = logLevelIDEDefault
    try { device.updateSetting('configLogLevelIDE', logLevelIDEDefault) }
    catch(e) {}

    def logLevelDeviceDefault = 2
    logger("configure: Resetting configLogLevelDevice preference to ${logLevelDeviceDefault}.", 'trace')
    state.logLevelDevice = logLevelDeviceDefault
    try { device.updateSetting('configLogLevelDevice', logLevelDeviceDefault) }
    catch(e) {}

    if (commandClassesVersions().containsKey(0x84)) {
        def wakeUpIntervalDefault = (configWakeIntervalOptions()?.find { it.specified }?.item) ?: configWakeIntervalOptions().find { it.default }.item
        logger("configure: Resetting configWakeUpInterval preference to ${wakeUpIntervalDefault}.", 'trace')
        state.wakeUpIntervalTarget = wakeUpIntervalDefault
        try { device.updateSetting('configWakeUpInterval', wakeUpIntervalDefault) }
        catch(e) {}
    }

    logger('configure: getting default/specified values and resetting any existing preferences', 'trace')
    paramsMetadata().findAll( { it.id in configParameters() && !it.readonly } ).each {

        def specified = configSpecified()?.find { cs -> cs.id == it.id }
        def defaultValue = (specified) ? specified.specifiedValue : it.defaultValue
        def resetType = (specified) ? 'specified' : 'default'
        state."paramTarget$it.id" = defaultValue

        def id = it.id.toString().padLeft(3, "0")

        switch(it.type) {
            case 'number':
                logger("configure: Parameter $id, resetting number preference to ($resetType): $defaultValue", 'debug')
                try { device.updateSetting("configParam$id", defaultValue) }
                catch(e) {}
                break

            case 'enum':
                logger("configure: Parameter $id, resetting enum preference to ($resetType): $defaultValue", 'debug')
                try { device.updateSetting("configParam$id", defaultValue) }
                catch(e) {}
                break

            case 'bool':
                def resetBool = (defaultValue == it.trueValue)
                logger("configure: Parameter: $id, resetting bool preference to ($resetType): $resetBool", 'debug')
                try { device.updateSetting("configParam$id", resetBool) }
                catch(e) {}
                break

            case 'flags':
                def defaultFlags = (specified) ? specified.flags : it.flags
                defaultFlags.each { rf ->
                    def defaultFlagValue = (rf?.specifiedValue != null) ? rf.specifiedValue : rf.defaultValue
                    def resetBool = (defaultFlagValue == rf.flagValue)
                    logger("configure: Parameter: $id$rf.id, resetting flag preference to ($resetType): $resetBool", 'debug')
                    try { device.updateSetting("configParam$id$rf.id", resetBool) }
                    catch(e) {}
                }
                break

            default:
                logger('configure: Unhandled preference type.', 'warn')
        }
    }

    state.syncAll = true
    state.configReportBuffer = [:]
    updateDataValue('serialNumber', null)
    updated()
}

/**
 * updated() sets configuration to user preferences selected in mobile app if state.configuring = false
 * If called after configure(), any selected user preferences will be ignored (and may have already been reset to defaults).
 * Due to a ST system bug, updated() is called twice in immediate succession. As a result, there is a check to abort the second call.
 */
def updated() {
    logger('updated: Updating configuration targets to match any user preferences.', 'info')
    if (!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 2000) {
        state.updatedLastRanAt = now()

        if (!state.configuring) {
            if (settings?.configAutoResetTamperDelay != null) {
                state.autoResetTamperDelay = settings.configAutoResetTamperDelay.toInteger()
                logger("updated: Updating autoResetTamperDelay value to $state.autoResetTamperDelay", 'debug')
            }

            if (settings?.configLogLevelIDE != null) {
                state.logLevelIDE = settings.configLogLevelIDE.toInteger()
                logger("updated: Updating logLevelIDE value to $state.logLevelIDE", 'debug')
            }

            if (settings?.configLogLevelDevice != null) {
                state.logLevelDevice = settings.configLogLevelDevice.toInteger()
                logger("updated: Updating logLevelDevice value to $state.logLevelDevice", 'debug')
            }

            if (settings?.configWakeUpInterval != null) {
                state.wakeUpIntervalTarget = settings.configWakeUpInterval.toInteger()
                logger("updated: Updating wakeUpIntervalTarget value to $state.wakeUpIntervalTarget", 'debug')
            }

            paramsMetadata().findAll({ it.id in configParameters() && !it.readonly }).each {

                def id = it.id.toString().padLeft(3, "0")
                if (settings?."configParam$id" != null || settings?."configParam${id}a" != null) {

                    switch (it.type) {
                        case 'number':
                            def setting = settings."configParam$id"
                            logger("updated: Parameter $id set to match number preference value: $setting", 'debug')
                            state."paramTarget$it.id" = setting
                            break

                        case 'enum':
                            def setting = settings."configParam$id".toInteger()
                            logger("updated: Parameter $id set to match enum preference value: $setting", 'debug')
                            state."paramTarget$it.id" = setting
                            break

                        case 'bool':
                            def setting = (settings."configParam$id") ? it.trueValue : it.falseValue
                            logger("updated: Parameter $id set to match bool preference value: $setting", 'debug')
                            state."paramTarget$it.id" = setting
                            break

                        case 'flags':
                            def target = 0
                            settings.findAll { set -> set.key ==~ /configParam${id}[a-z]/ }.each { k, v ->
                                if (v) target += it.flags.find { f -> f.id == "${k.reverse().take(1)}" }.flagValue
                            }
                            logger("updated: Parameter $it.id set to match sum of flag preference values: $target", 'debug')
                            state."paramTarget$it.id" = target
                            break

                        default:
                            logger('updated: Unhandled configuration type.', 'warn')
                    }
                }
            }
        }

        state.configuring = false

        if ('deviceUse' in configDevice()) {
            logger('updateded: setting device use states', 'debug')
            deviceUseStates()
            sendEvent(name: "${getDataValue('event')}", value: "${getDataValue('inactiveState')}", displayed: false)
        }

        /**
         * If the device is mains powered (i.e. listening), the configuration commands can be sent immediately.
         * If the device is battery powered (i.e. sleepy), the configuration commands are queued (state.queued) until the device next wakes up.
         */
        if (listening()) {
            def result = response(sendCommandSequence(sync()))
            logger("updated: Result '$result'", 'info')
            result
        } else {
            logger('updated: Sleepy device, queuing sync().', 'info')
            sendEvent(name: 'configure', value: 'queued', descriptionText: 'Device reports Configuration queued.', isStateChange: true, displayed: false)
            if (state.queued) {
                state.queued << 'sync'
            } else {
                state.queued = ['sync']
            }
        }
    } else {
        logger('updated: Ran within last 2 seconds so aborting update.', 'trace')
    }
}

/***********************************************************************************************************************
 * System Methods Helper Methods (sync, updateSyncPending, listening, byteArrayToUInt, deviceUseStates, logger)
 **********************************************************************************************************************/
/**
 * sync() compares target and cached values for each configuration parameter. When there is a difference it assembles the relevant command to update the configuration setting in the device.
 * It then requests the configuration value back from the device to check that it has been successfully updated
 */
private sync() {
    def cmds = []
    def syncPending = 0

    if (state.syncAll) {
        logger('sync: Deleting all cached configuration values.', 'debug')
        state.wakeUpIntervalCache = null
        paramsMetadata().findAll( { it.id in configParameters() && !it.readonly } ).each { state."paramCache${it.id}" = null }
        updateDataValue('serialNumber', null)
        state.syncAll = false
    }

    if (state.wakeUpIntervalTarget != null && state.wakeUpIntervalTarget != state.wakeUpIntervalCache) {
        logger("sync: Syncing Wake Up Interval with new value: ${state.wakeUpIntervalTarget}", 'debug')
        cmds += wakeUpIntervalSet(state.wakeUpIntervalTarget, zwaveHubNodeId)
        cmds += wakeUpIntervalGet()
        syncPending++
    }

    paramsMetadata().each {
        if (it.id in configParameters() && !it.readonly && state."paramTarget${it.id}" != null && state."paramTarget${it.id}" != state."paramCache${it.id}") {
            logger("sync: Syncing parameter ${it.id} with new value: " + state."paramTarget${it.id}", 'debug')
            cmds += configurationSet(it.id, it.size, state."paramTarget${it.id}")
            cmds += configurationGet(it.id)
            syncPending++
        }
        else if (state.syncAll && it.id in configParameters()) {
            cmds += configurationGet(it.id)
        }
    }

    if (getDataValue('serialNumber') == null) { // TODO - check if this is needed? - What if device doesn't have a serial number?
        logger('sync: Requesting device serial number.', 'debug')
        cmds += deviceSpecificGet()
        syncPending++
    }

    sendEvent(name: 'syncPending', value: syncPending, displayed: false, descriptionText: 'Change to syncPending.', isStateChange: true) // TODO - check this
    logger("sync: Returning '$cmds'", 'debug')
    cmds
}

/**
 * updateSyncPending() called when a report is received back from the device in response to check a configuration parameter value
 * keeps track of the number of outstanding configuration parameters that have yet to be successfully updated
 */
private updateSyncPending() {
    def syncPending = 0
    def userConfig = 0

    if (state.syncAll) {
        logger('updateSyncPending: Deleting all cached values.', 'debug')
        state.wakeUpIntervalCache = null
        paramsMetadata().findAll( { it.id in configParameters() && !it.readonly } ).each { state."paramCache${it.id}" = null }
        updateDataValue('serialNumber', null)
        state.syncAll = false
    }

    if (!listening()) {
        def target = state.wakeUpIntervalTarget
        if (target != null && target != state.wakeUpIntervalCache) syncPending++
        if (target != configIntervals().specifiedWakeUpInterval) userConfig++
    }

    paramsMetadata().findAll( { it.id in configParameters() && !it.readonly} ).each {
        if (state."paramTarget${it.id}" != null) {
            if (state."paramCache${it.id}" != state."paramTarget${it.id}") {
                syncPending++
            } else if (state."paramCache${it.id}" != it.defaultValue) {
                def sv = configSpecified()?.find { cs -> cs.id == it.id }?.specifiedValue
                if (state."paramCache${it.id}"!= sv) {
                    userConfig++
                }
            }
        }
    }

    if (getDataValue('serialNumber') == null) syncPending++
    logger("updateSyncPending: $syncPending item(s) remaining", 'trace')
    // if (syncPending == 0 && device.latestValue('syncPending') > 0) { // ??? is this needed to stop this triggering when not needed?

    if (syncPending == 0) {
        def ct = (userConfig > 0) ? 'user' : (configSpecified()) ? 'specified' : 'default'
        logger("updateSyncPending: Sync Complete. Configuration type: $ct", 'info')
        updateDataValue('configurationType', ct)
        sendEvent(name: 'configure', value: 'completed', descriptionText: 'Device reports Configuration completed.', isStateChange: true, displayed: false)

        // TODO - create configReport here
        /*
        def paramReport = cmd.parameterNumber.toString().padLeft(3, "0")
        def paramValueReport = paramValue.toString()
        state.configReportBuffer << [(paramReport): paramValueReport]
        if (state.configReportBuffer.size() == configParameters().size()) {
            logger('ConfigurationReport: All Configuration Values Reported.', 'info')
            updateDataValue("configurationReport", state.configReportBuffer.sort().collect { it }.join(","))
        }
        */
    }

    sendEvent(name: 'syncPending', value: syncPending, displayed: false)
}

/**
 * checks whether or not device is a 'Listening' device i.e. powered by mains
 * @return
 */
private listening() {
    // getZwaveInfo()?.zw?.startsWith('S')
    getZwaveInfo()?.zw?.startsWith('L')
}

/**
 * converts unsigned integers
 * @param byteArray
 * @return
 */
private byteArrayToUInt(byteArray) {
    def i = 0; byteArray.reverse().eachWithIndex { b, ix -> i += b * (0x100 ** ix) }; i
}

/**
 * deviceUseStates() configures a contact sensor for different type of use and sets the event and state values that are reported
 * uses settings.ConfigDeviceUse to set datavalues for event value and states (default is a water sensor)
 */
void deviceUseStates() {
    def useStates
    if (settings?.configDeviceUse) {
        useStates = configUseStateOptions()?.find { it.item == settings.configDeviceUse.toInteger() }
    } else {
        useStates = configUseStateOptions()?.find { it.default == true }
    }
    def deviceUse = (useStates) ? useStates.use : 'Water'
    def event = (useStates) ? useStates.event : 'water'
    def inactiveState = (useStates) ? useStates.inactive : 'dry'
    def activeState = (useStates) ? useStates.active : 'wet'
    updateDataValue('deviceUse', deviceUse)
    updateDataValue('event', event)
    updateDataValue('inactiveState', inactiveState)
    updateDataValue('activeState', activeState)
}

/**
 * logger method determines whether or not to pass on message to log, depending on the configured message severity level
 * @param msg
 * @param level
 * @return
 */
private logger(msg, level = 'debug') {
    switch(level) {
        case 'error':
            if (state.logLevelIDE >= 1) log.error(msg); sendEvent(descriptionText: "Error: $msg", displayed: false, isStateChange: true)
            if (state.logLevelDevice >= 1) sendEvent(name: 'logMessage', value: "Error: $msg", displayed: false, isStateChange: true)
            break

        case 'warn':
            if (state.logLevelIDE >= 2) log.warn(msg); sendEvent(descriptionText: "Warning: $msg", displayed: false, isStateChange: true)
            if (state.logLevelDevice >= 2) sendEvent(name: 'logMessage', value: "Warning: $msg", displayed: false, isStateChange: true)
            break

        case 'info':
            if (state.logLevelIDE >= 3) log.info(msg); sendEvent(descriptionText: "Info: $msg", displayed: false, isStateChange: true)
            break

        case 'debug':
            if (state.logLevelIDE >= 4) log.debug(msg); sendEvent(descriptionText: "Debug: $msg", displayed: false, isStateChange: true)
            break

        case 'trace':
            if (state.logLevelIDE >= 5) log.trace(msg)
            break

        default:
            log.debug(msg); sendEvent(descriptionText: "Log: $msg", displayed: false, isStateChange: true)
    }
}

/***********************************************************************************************************************
 * Capability-related Commands (ping, poll, refresh)
 * Note: configure is in Main System Methods
 **********************************************************************************************************************/
/**
 * ping
 * @return
 */
def ping() {
    if (listening()) sendCommandSequence(zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 0x01))
}

/**
 * poll
 */
def poll() { // depreciated
}

/**
 * refresh
 */
def refresh() {
}

/***********************************************************************************************************************
 *  Device Handler Custom Commands (resetLog, resetTamper, syncAll, syncRemaining, profile)
 **********************************************************************************************************************/
/**
 * batteryChange - sends an event to log change of battery TODO - check details of attribute, add to logger app and database
 */
void batteryChange() {
    sendEvent(name: 'batteryChange', value: 'changed', descriptionText: 'Device battery was changed.', isStateChange: true, displayed: false)
}

/**
 * profile - initiates profiling of theh device (profileNow())
 * @return
 */
def profile() {
    logger('profile: Called', 'info')
    (listening()) ? sendCommandSequence(profileNow()) : state.queued << 'profileNow'
}

/**
 * profileNow - gets reports from device on transmission power level and which versions of commands classes that it supports
 * @return
 */
private profileNow() {
    logger('profileNow: Called', 'info')
    def cmds = []
    cmds += powerlevelGet()
    cmds += versionCommandClassGet()
    cmds
}

/**
 * resetLog - resets log messages displatey in mobile app
 * @return
 */
def resetLog() {
    sendEvent(name: 'logMessage', value: 'log clear', displayed: false, isStateChange: true)
}

/**
 * resetTamper - resets tamper (after configured period as there is not zwave tamper clear message)
 * @return
 */
def resetTamper() {
    logger('resetTamper: Resetting tamper alarm.', 'info')
    sendEvent(name: 'tamper', value: 'clear', descriptionText: 'Tamper alarm cleared', displayed: true, isStateChange: true)
}

/**
 * syncAll - initiates a resynching of all device configuration parameters
 * @return
 */
def syncAll() {
    logger('syncAll: Called', 'info')
    state.syncAll = true
    state.configReportBuffer = [:]
    (listening()) ? sendCommandSequence(sync()) : state.queued << 'sync'
}

/**
 * syncRemaining - initiates a resynching of unsynched device configuration parameters
 * @return
 */
def syncRemaining() {
    logger('syncRemaining: Called', 'info')
    (listening()) ? sendCommandSequence(sync()) : state.queued << 'sync'
}

/***********************************************************************************************************************
 * Zwave Command Helpers
 * Generic zwave commands that are common to most, if not all devices.
 **********************************************************************************************************************/
/*
private association(commands) {
    sendEvent(descriptionText: "Setting 1st Association Group", displayed: false)
    commands << zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: [zwaveHubNodeId])
}
*/

private batteryGet() {
    if (commandClassesVersions()?.containsKey(0x80)) {
        logger('batteryGet: Requesting Battery report', 'debug')
        zwave.batteryV1.batteryGet()
    }
}

private configurationGet(id) {
    zwave.configurationV1.configurationGet(parameterNumber: id)
}

private configurationSet(id, size, target) {
    zwave.configurationV1.configurationSet(parameterNumber: id, size: size, scaledConfigurationValue: target)
}

private deviceSpecificGet() {
    zwave.manufacturerSpecificV2.deviceSpecificGet(deviceIdType: 1)
}

private powerlevelGet() {
    if (commandClassesVersions()?.containsKey(0x73)) {
        logger('powerlevelGet: Requesting Powerlevel report.', 'debug')
        zwave.powerlevelV1.powerlevelGet()
    }
}

/*
private sensorBinary(commands) {
    sendEvent(descriptionText: "Requesting Binary Sensor Report", displayed: false)
    commands << zwave.sensorBinaryV2.sensorBinaryGet()
}
*/

private versionCommandClassGet() {
    logger('versionCommandClassGet: Requesting Command Class report.', 'debug')
    state.commandClassVersionsBuffer = [:]
    def cmds = []
    commandClassesQuery().each {
        cmds += zwave.versionV1.versionCommandClassGet(requestedCommandClass: it)
    }
    cmds
}

/**
 * wakeUpIntervalGet - requests Wake Up Interval Report from device
 * called by sync
 * @return wakeUpIntervalGet getter
 */
private wakeUpIntervalGet() {
    zwave.wakeUpV1.wakeUpIntervalGet()
}

/**
 * wakeUpIntervalSet - sets Wake Up Interval for device
 * called by sync
 * @param seconds - duration of Wake Up Interval
 * @param nodeid - id of node to send Device Woke Up Notification to (i.e. hub node id)
 * @return wakeUpIntervalSet setter
 */
private wakeUpIntervalSet(seconds, nodeid) {
    zwave.wakeUpV1.wakeUpIntervalSet(seconds: seconds, nodeid: nodeid)
}

/**
 * wakeUpNoMoreInformation - tells device to go back to sleep
 * @return wakeUpNoMoreInformation command
 */
private wakeUpNoMoreInformation() {
    logger('wakeUpNoMoreInformation: Sending Wake Up No More Information.', 'debug')
    zwave.wakeUpV1.wakeUpNoMoreInformation()
}

/***********************************************************************************************************************
 * Send Zwave Commands to Device
 **********************************************************************************************************************/
private sendCommandSequence(cmds, delay = 1200) {
    if (!listening()) {
        cmds += wakeUpNoMoreInformation()
    }
    delayBetween(cmds.collect { selectEncapsulation(it) }, delay)
    // sendHubCommand(commands.collect { response(it) }, delay)
}
/**
 * selectEncapsulation - depends on device capabilites
 * @param cmd
 * @return cmd with or without appropriate encapsulation
 */
private selectEncapsulation(physicalgraph.zwave.Command cmd) {
    if (zwaveInfo?.zw?.endsWith('s') && zwaveInfo?.sec?.contains(Integer.toHexString(cmd.commandClassId)?.toUpperCase())) {
        secureEncapsulate(cmd)
    }
    else if (zwaveInfo?.cc?.contains('56')) {
        crc16Encapsulate(cmd)
    }
    else {
        logger("noEncapsulation: '$cmd'", 'trace')
        cmd.format()
    }
}

/**
 * secureEncapsulate
 * @param cmd
 * @return cmd securely encapsulated
 */
private secureEncapsulate(physicalgraph.zwave.Command cmd) {
    logger("secureEncapsulation: '$cmd'", 'trace')
    zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

/**
 * crc16Encapsulate
 * @param cmd
 * @return cmd crc16 encapsulated
 */
private crc16Encapsulate(physicalgraph.zwave.Command cmd) {
    logger("crc16Encapsulation: '$cmd'", 'trace')
    zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

/***********************************************************************************************************************
 *  Parse Method
 *  Receives raw Zwave commands from devices and passes them to the appropriate handler
 *  @param description - raw Zwave command
 *  @return list of resultant events and/or commands
 **********************************************************************************************************************/
def parse(String description) {
    logger("parse: raw message '$description'", 'trace')

    def messages = state?.messages ?: 0
    updateDataValue('messages', "${state.messages}")
    state.messages = messages + 1

    def result = []

    if (description.startsWith('Err')) {
        if (description.startsWith('Err 106')) {
            logger('parse: Error 106', 'error')
            result += createEvent(name: 'secureInclusion', value: 'failed', isStateChange: true, descriptionText: 'Sensor failed to complete the network security key exchange. You must remove it from your network and add it again.')
        } else {
            logger("parse: Unknown Error. raw message '$description'", 'error')
        }
    }
    else if (description != 'updated') {
        def cmd = zwave.parse(description, commandClassesVersions())
        if (cmd) {
            result += zwaveEvent(cmd)
            if (listening() && device.latestValue('syncPending') > 0 && cmd.commandClassId in commandClassesUnsolicited()) {
                logger('parse: sync() called', 'debug')
                result += response(sendCommandSequence(sync()))
            }
        } else {
            logger("parse: Could not parse.  raw message '$description'", 'error')
        }
    }
    logger("parse: Result '$result'", 'trace')
    result
}

/***********************************************************************************************************************
 *  Zwave Application Events Handlers
 **********************************************************************************************************************/
/**
 * 0x20=1, Basic
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
    logger("BasicSet: Ignoring '$cmd'", 'info')
    // motionEvent(cmd.value)
}

/**
 * 0x30=2, Sensor Binary
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd) {
    logger("SensorBinaryReport: Ignoring '$cmd'", 'info')
    // motionEvent(cmd.sensorValue)
}

/**
 * 0x71=5, Notification
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) {
    logger("NotificationReport: '$cmd'", 'debug')
    def result = []

    if (cmd.notificationType == 0x07) {
        switch (cmd.event) {
            case 0x00:
                result += motionEvent(0)
                // result << createEvent(name: "tamper", value: "clear") // is this needed - check other handlers
                break

            case 0x03:
                result += createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName was tampered", displayed: true, isStateChange: true)
                if (state.autoResetTamperDelay > 0) {
                    unschedule(resetTamper)
                    runIn(state.autoResetTamperDelay, resetTamper)
                }
                break

            case 0x07:
                result += motionEvent(1)
                break

            case 0x08:
                result += motionEvent(1)
                break

            default:
                logger("NotificationReport: Unhandled notification event '$cmd.event", 'warn')
        }
        logger("NotificationReport: '$result'", 'info')
    } else {
        logger("NotificationReport: Unhandled notification type '$cmd.notificationType'", 'warn')
    }
    result
}

/**
 * 0x31: 5, Sensor Multilevel
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) {
    logger("SensorMultilevelReport: $cmd", 'debug')
    def map = [displayed: true, isStateChange: true]

    switch (cmd.sensorType) {
        case 0x01:
            map.name = 'temperature'
            def cmdScale = (cmd.scale == 1) ? 'F' : 'C'
            map.value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmdScale, cmd.precision)
            map.unit = getTemperatureScale()
            map.descriptionText = "$device.displayName temperature is $map.value $map.unit"
            break

        case 0x03:
            map.name = 'illuminance'
            map.value = cmd.scaledSensorValue.toInteger()
            map.unit = 'lux'
            map.descriptionText = "$device.displayName illuminance is $map.value $map.unit"
            break

        case 0x05:
            map.name = 'humidity'
            map.value = cmd.scaledSensorValue.toInteger()
            map.unit = '%'
            map.descriptionText = "$device.displayName humidity is $map.value $map.unit"
            break

        case 0x1B:
            map.name = 'ultravioletIndex'
            map.value = cmd.scaledSensorValue.toInteger()
            map.descriptionText = "$device.displayName ultravioletIndex is $map.value"
            break

        default:
            logger("SensorMultilevelReport: Unhandled sensor report '$cmd'", 'warn')
            break
    }
    logger("SensorMultilevelReport: '$map'", 'info')
    createEvent(map)
}

/**
 * 0x70: 2, Configuration
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
    logger("ConfigurationReport: '$cmd'", 'trace')
    def signed = paramsMetadata()?.find { it.id == cmd.parameterNumber }?.isSigned
    def paramValue = (signed) ? cmd.scaledConfigurationValue : byteArrayToUInt(cmd.configurationValue)

    logger("ConfigurationReport: Parameter $cmd.parameterNumber has been set to value (${(signed) ? 'signed' : 'unsigned'}) $paramValue", 'debug')
    state."paramCache${cmd.parameterNumber}" = paramValue
    if (paramsMetadata().find { !it.readonly } ) updateSyncPending()

    def result = []
    if (cmd.parameterNumber == 9) {
        if (cmd.configurationValue[0] == 0) {
            result += createEvent(name: 'powerSource', value: 'dc', displayed: false)
            result += createEvent(name: 'batteryStatus', value: 'USB Cable', displayed: false) // ??is this needed??
        }
        else if (cmd.configurationValue[0] == 1) {
            result += createEvent(name: 'powerSource', value: 'battery', displayed: false)
        }
    }
    result
}

/***********************************************************************************************************************
 * Zwave Management Events Handlers
 **********************************************************************************************************************/
/**
 * 0x80:1, Battery
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
    def map = [name: "battery", unit: "%"]
    if (cmd.batteryLevel == 0xFF) {
        map.value = 1
        map.descriptionText = "${device.displayName} battery is low"
        map.isStateChange = true
    }
    else {
        map.value = cmd.batteryLevel
        map.isStateChange = true
    }
    state.timeLastBatteryReport = new Date().time
    createEvent(map)
}

/**
 * 0x72: 2, Device Specific
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) {
    logger('DeviceSpecificReport: Serial number report received.', 'info')
    logger("DeviceSpecificReport: Serial number raw report: $cmd", 'debug')
    def serialNumber = "0"
    // serialNumber = (cmd.deviceIdType == 1 && cmd.deviceIdDataFormat == 1) ? (cmd.deviceIdData.each { data -> serialNumber += "${String.format(" % 02 X ", data)}" }) : "0"
    updateDataValue('serialNumber', serialNumber)
    updateSyncPending()
}

/**
 * 0x72=2, Manufacturer Specific
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    log.info "Executing zwaveEvent 72 (ManufacturerSpecificV2) : 05 (ManufacturerSpecificReport) with cmd: $cmd"
    log.debug "manufacturerId:   ${cmd.manufacturerId}"
    log.debug "manufacturerName: ${cmd.manufacturerName}"
    log.debug "productId:        ${cmd.productId}"
    log.debug "productTypeId:    ${cmd.productTypeId}"
    def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
    updateDataValue("MSR", msr)
}

/**
 * 0x86: 2, Version
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionCommandClassReport cmd) {
    def ccValue = Integer.toHexString(cmd.requestedCommandClass).toUpperCase()
    def ccVersion = cmd.commandClassVersion
    logger("VersionCommandClassReport: Command Class: $ccValue, version: $ccVersion", 'debug')
    state.commandClassVersionsBuffer << [(ccValue): ccVersion]
    if (state.commandClassVersionsBuffer.size() == commandClassesQuery().size()) {
        logger('VersionCommandClassReport: All Command Class Versions Reported.', 'info')
        updateDataValue("commandClassVersions", state.commandClassVersionsBuffer.findAll { it.value > 0 }.sort().collect { it }.join(","))
    }
}

/**
 * 0x84: 2, Wake Up Interval Report
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpIntervalReport cmd) {
    logger("WakeUpIntervalReport: '$cmd'", 'debug')
    def wakeUpInterval = cmd.seconds
    state.wakeUpIntervalCache = wakeUpInterval
    updateDataValue('wakeUpInterval', "$wakeUpInterval")
    updateSyncPending()
    def checkInterval = wakeUpInterval.toDouble().multiply(2.2).round()
    createEvent(name: 'checkInterval', value: checkInterval, displayed: false, data: [protocol: 'zwave', hubHardwareId: device.hub.hardwareID], descriptionText: 'Configured checkInterval')
}

/**
 * 0x84: 2, Wake Up Notification
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd) {
    logger('WakeUpNotification: Device woke up.', 'info')
    def cmds = []
    if (listening()) {
        cmds += powerlevelGet()
        if (device.latestValue('syncPending') > 0) cmds += sync()
        // ?? send mock battery event as a device "pulse"??
    } else {
        if (state.queued) {
            def queue = state.queued as Set
            logger("WakeUpNotification: Queue '$queue'", 'trace')
            // queue.each { "$it"().each { qc -> cmds << qc } }
            queue.each { cmds += "$it"() }
            state.queued = [] // TODO - Not sure should reset the queue here, rather remove items when reports received back?
        }
        else if (device.latestValue('syncPending') > 0) {
            logger("WakeUpNotification: syncPending > 0", 'trace')
            // sync().each { cmds << it }
            cmds += sync()
        }
        if (!state?.timeLastBatteryReport || now() > state.timeLastBatteryReport + configIntervals().batteryRefreshInterval) {
            logger("WakeUpNotification: Requesting Battery report.", 'trace')
            cmds += batteryGet()
            cmds += powerlevelGet()
        } else {
            sendEvent(name: 'battery', value: device.latestValue('battery'), unit: '%', isStateChange: true, displayed: false)
        }
    }
    logger("WakeUpNotification: Returning '$cmds'", 'debug')
    def report = response(sendCommandSequence(cmds))
    logger("WakeUpNotification: Result '$report'", 'info')
    report
}

/***********************************************************************************************************************
 * Zwave Network Protocol Events Handlers
 **********************************************************************************************************************/
/**
 * 0x73=1, // Powerlevel Report
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd) {
    logger("PowerlevelReport: '$cmd'", 'debug')
    def powerLevel = -1 * cmd.powerLevel //	def timeout = cmd.timeout (1-255 s) - omit
    logger("PowerlevelReport: $powerLevel dBm", 'info')
    updateDataValue('powerLevel', "$powerLevel")
    // ??? could create event - so that have a "pulse" for listening devices
}

/***********************************************************************************************************************
 * Zwave Transport Encapsulation Events Handlers
 **********************************************************************************************************************/
/**
 * 0x98=1, Security Message Encapsulation
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    logger("security: raw '$cmd'", 'trace')
    def encapsulatedCommand = cmd.encapsulatedCommand(commandClassesVersions())
    logger("security: encapsulated '$encapsulatedCommand'", 'trace')
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand)
    }
    else {
        logger("security: unable to extract '$cmd'", 'warn')
    }
}

// TODO - ??? need crc16 processing???

/**
 * 0x98=1, Security Network Key Verify
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.securityv1.NetworkKeyVerify cmd) {
    logger('networkKey: Device is securely included.', 'info')
    def result = [createEvent(name: 'secureInclusion', value: 'success', descriptionText: 'Secure inclusion was successful', isStateChange: true)]
    result
}

/**
 * 0x98=1, Security Commands Supported Report
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityCommandsSupportedReport cmd) {
    logger("securityCommands: report '$cmd'", 'info')
}

/**
 * Zwave General Event Handler (catch all)
 */
def zwaveEvent(physicalgraph.zwave.Command cmd) {
    logger("General zwaveEvent cmd: ${cmd}", 'warn')
    createEvent(descriptionText: cmd.toString(), isStateChange: false)
}

/***********************************************************************************************************************
 * Device Specific Helper Methods
 * Methods that are only applicable to certain devices
 **********************************************************************************************************************/
/**
 * motionEvent() creates a motion event (may be triggered by several different zwave messages)
 * @param value
 * @return
 */
private motionEvent(value) {
    logger('motionEvent: Creating motion event', 'debug')
    def map = [name: 'motion', displayed: true, isStateChange: true]
    if (value) {
        map.value = 'active'
        map.descriptionText = "$device.displayName detected motion"
    }
    else {
        map.value = 'inactive'
        map.descriptionText = "$device.displayName motion has stopped"
    }
    createEvent(map)
}

/**
 * sensorValueEvent()
 * @param value
 * @return
 */
private sensorValueEvent(Short value) {
    def eventValue = null
    if (value == 0x00) {eventValue = "dry"}
    if (value == 0xFF) {eventValue = "wet"}
    def result = createEvent(name: "water", value: eventValue, displayed: true, isStateChange: true, descriptionText: "$device.displayName is $eventValue")
    return result
}

/***********************************************************************************************************************
 *  Metadata Methods - Specific to Device and Specified Configuration
 **********************************************************************************************************************/
/**
 * commandClassesQuery() - list of all potential command classes to query device to see if it supports them
 * @return
 */
private commandClassesQuery() { [0x20, 0x22, 0x25, 0x26, 0x27, 0x2B, 0x30, 0x31, 0x32, 0x33, 0x56, 0x59, 0x5A, 0x5E, 0x60, 0x70, 0x71, 0x72, 0x73, 0x75, 0x7A, 0x80, 0x84, 0x85, 0x86, 0x8E, 0x98, 0x9C] }

/**
 * commandClassesSecure() -  *** don't think this is needed
 * @return
 */
private commandClassesSecure() { [0x20, 0x2B, 0x30, 0x5A, 0x70, 0x71, 0x84, 0x85, 0x8E, 0x9C] }

/**
 * commandClassesUnsolicited() - list of command classes of sensor reports (i.e. unsolicited) - can be used to trigger sync if device on battery
 * @return
 */
private commandClassesUnsolicited() { [0x20, 0x30, 0x31, 0x60, 0x71, 0x9C] }

/**
 * commandClassesVersions() - versions of command classes to use so that functionality is correctly supported
 * @return
 */
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

/**
 * configDevice() - menu items available in mobile app to be configured by user
 * @return
 */
private configDevice() { ['deviceUse', 'autoResetTamperDelay', 'wakeUpInterval'] }

/**
 * configLogger() - set logging level of device handler
 */
private configLogger() { ['logLevelDevice', 'logLevelIDE'] }

/**
 * configUseStateOptions() - map of states for contact sensor depending on use
 * @return
 */
/*
private configUseStateOptions() { [
        Bed: [event: 'contact', inactive: 'empty', active: 'occupied'],
        Chair: [event: 'contact', inactive: 'vacant', active: 'occupied'],
        Toilet: [event: 'contact', inactive: 'full', active: 'flushing'],
        Water: [event: 'water', inactive: 'dry', active: 'wet', default: true]
] }
*/
private configUseStateOptions() { [
    [item: 0, use: 'Bed', event: 'contact', inactive: 'empty', active: 'occupied'],
    [item: 1, use: 'Chair', event: 'contact', inactive: 'vacant', active: 'occupied'],
    [item: 2, use: 'Toilet', event: 'contact', inactive: 'full', active: 'flushing'],
    [item: 3, use: 'Water', event: 'water', inactive: 'dry', active: 'wet', default: true],
] }

/**
 * configIntervals() - list of values for intervals both device defaults and specified optimal values
 * @return
 */
private configIntervals() { [
        defaultWakeUpInterval: 4_000, defaultCheckInterval: 8_500,
        specifiedWakeUpInterval: 86_400, specifiedCheckInterval: 180_000,
        batteryRefreshInterval: 604_800
] }

/**
 * configWakeIntervalOptions
 * @return map of preference options for configuring device wake interval
 */
private configWakeIntervalOptions() { [
    [item: 3_600, interval: '1 hour', specified: true],
    [item: 7_200, interval: '2 hours', default: true],
    [item: 34_200, interval: '12 hours'],
] }

/**
 * configParameters() - set of device configuration parameters with a specified value required for optimal operation
 * @return list of device parameters
 */
private configParameters() { [2, 3, 4, 5, 8, 9, 40, 81, 101, 102, 103, 111, 112, 113] }

/**
 * configSpecified - map of specified configuration values for optimal operation (some may be same as default) - means device configuration can be set/reset
 * @return map of specified/default values for device parameters
 */
private configSpecified() { [
    [id:2,size:1,defaultValue:0,specifiedValue:1],
    [id:3,size:2,defaultValue:240,specifiedValue:30],
    [id:4,size:1,defaultValue:5,specifiedValue:5],
    [id:5,size:1,defaultValue:1,specifiedValue:2],
    [id:40,size:1,defaultValue:0,specifiedValue:0],
    [id:81,size:1,defaultValue:0,specifiedValue:2],
    [id:101,size:4,defaultValue:241,specifiedValue:240,flags:[[id:'a',flagValue:1,defaultValue:1,specifiedValue:0],[id:'b',flagValue:16,defaultValue:16,specifiedValue:16],[id:'c',flagValue:32,defaultValue:32,specifiedValue:32],[id:'d',flagValue:64,defaultValue:64,specifiedValue:64],[id:'e',flagValue:128,defaultValue:128,specifiedValue:128]]],
    [id:102,size:4,defaultValue:0,specifiedValue:0,flags:[[id:'a',flagValue:1,defaultValue:0,specifiedValue:0],[id:'b',flagValue:16,defaultValue:0,specifiedValue:0],[id:'c',flagValue:32,defaultValue:0,specifiedValue:0],[id:'d',flagValue:64,defaultValue:0,specifiedValue:0],[id:'e',flagValue:128,defaultValue:0,specifiedValue:0]]],
    [id:103,size:4,defaultValue:0,specifiedValue:0,flags:[[id:'a',flagValue:1,defaultValue:0,specifiedValue:0],[id:'b',flagValue:16,defaultValue:0,specifiedValue:0],[id:'c',flagValue:32,defaultValue:0,specifiedValue:0],[id:'d',flagValue:64,defaultValue:0,specifiedValue:0],[id:'e',flagValue:128,defaultValue:0,specifiedValue:0]]],
    [id:111,size:4,defaultValue:3600,specifiedValue:3600]
] }

/**
 * configUser - subset of device configuration parameters that are made available for configuration by the user in the mobile app
 * @return list of device parameters
 */
private configUser() { [2, 3, 4, 5, 8, 81, 101, 102, 103, 111, 112, 113] }

/**
 * paramsMetadata - complete map of all device configuration parameters
 * @return map of all device configuration parameters
 */
private paramsMetadata() { [
    [id:2,size:1,type:'bool',defaultValue:0,required:false,readonly:false,isSigned:false,name:'Enable waking up for 10 minutes',description:'when re-power on (battery mode) the MultiSensor',falseValue:0,trueValue:1],
    [id:3,size:2,type:'number',range: '10..3600',defaultValue:240,required:false,readonly:false,isSigned:false,name: 'PIR reset time',description:'Reset time for PIR sensor'],
    [id:4,size:1,type:'enum',defaultValue:5,required:false,readonly:false,isSigned:false,name:'PIR Sensitivity',description:'Set the sensitivity of motion sensor',options:[0:'Off',1:'level 1 (minimum)',2:'level 2',3:'level 3',4:'level 4',5:'level 5 (maximum)']],
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