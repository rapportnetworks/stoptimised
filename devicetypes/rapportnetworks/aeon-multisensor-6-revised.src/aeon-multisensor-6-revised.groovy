/**
 *  Copyright 2018 Alasdair Thin
 *
 *  Licensed under the Apache License, Version 2.0 (the 'License'); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	Based on ***
 */

metadata {
    definition(name: 'Aeon Multisensor 6 Revised', namespace: 'rapportnetworks', author: 'Alasdair Thin') {
        /**
         * ST Capabilities
         */
        capability 'Sensor'
        capability 'Battery'                       // attribute: 'battery' (number)
        capability 'Configuration'                 // command:   'configure'
        capability 'Health Check'                  // attribute: 'checkInterval' (number), attribute: 'DeviceWatch-DeviceStatus' (string), attribute: 'healthStatus' (?), command: 'ping'
        capability 'Illuminance Measurement'       // attribute: 'illuminance' (number)
        capability 'Motion Sensor'                 // attribute: 'motion' (enum: ['inactive', 'active'])
        capability 'Power Source'                  // attribute: 'powerSource' (enum: ['battery', 'dc', 'mains', 'unknown']
        capability 'Relative Humidity Measurement' // attribute: 'humidity' (number)
        capability 'Tamper Alert'                  // attribute: 'tamper' (enum: ['detected', 'clear'])
        capability 'Temperature Measurement'       // attribute: 'temperature' (number)
        capability 'Ultraviolet Index'             // attribute: 'ultravioletIndex' (number)
        // TODO - What about deviceUseOptions (contact and water attributes)?

        /**
         * Custom Attributes
         */
        attribute 'batteryChange', 'string'        // Used to log chaging of battery.
        attribute 'configure', 'string'            // Reports on configuration command status. (enum: ['completed', 'queued', 'received', 'syncing'])
        attribute 'secureInclusion', 'string'      // Indicates secure inclusion success/failed.
        attribute 'syncPending', 'number'          // Number of config items that need to be synced with the physical device.

        /**
         * Custom Commands
         */
        command 'batteryChange'                    // Manually logs change of battery. (enum: ['changed'])
        command 'profile'                          // Manually initiates profiling of the device (power level, command class versions)
        command 'resetTamper'                      // Manually resets tamper attribute to 'clear'
        command 'syncAll'                          // Manually triggers the syncing of all device parameters
        command 'syncRemaining'                    // Manually triggers the syncing of any unsynched parameters (or queues them is a sleepy device)

        /**
         * Data values
         * deviceUse:            what device (e.g. contact sensor) is being used for (optional configuration)
         * event:                attribute for device event (based on optional configuration)
         * inactiveState:        state when inactive (based on optional configuration)
         * activeState:          state when active (based on optional configuration)
         * commandClassVersions: list of all command classes supported by the device and their version numbers
         * configurationType:    indicator of way device is configured [default, specified, user]
         * configuredParameters: list of all configured parameter values (omits values that are not configured - i.e. remain at default values)
         * messages:             counter of all messages received by parse method (i.e. sent by device)
         * MSR:                  manufacturer specific report
         * powerLevel:           device power level
         * serialNumber:         device unique serial number (if it has one)
         * wakeUpInterval:       device wake up interval
         */

        /**
         * Preferences
         * configAutoResetTamperDelay -> state.autoResetTamperDelay
         * configDeviceUse            -> deviceUse, event, inactiveState, activeState
         * configLogLevelIDE          -> state.logLevelIDE
         * configLogLevelDevice       -> state.logLevelDevice
         * configParam${id}           -> state."paramTarget${it.id}" -> state."paramCache${it.id}"
         * configWakeUpInterval       -> state.wakeUpIntervalTarget -> state.wakeUpIntervalCache
         * paraDeviceParameters
         * paraDeviceSettings
         * paraLoggerSettings
         */

        /**
         * State variables
         * autoResetTamperDelay
         * commandClassVersionsBuffer
         * configuringDevice
         * logLevelIDE
         * messageCounter
         * "paramTarget${it.id}", "paramCache${it.id}"
         * queued
         * syncAll
         * timeLastBatteryReport
         * updatedLastRanAt
         * wakeUpIntervalTarget, wakeUpIntervalCache
         */

        fingerprint mfr: '0086', prod: '0102', model: '0064', deviceJoinName: 'Aeotec MultiSensor 6'
    }

    tiles(scale: 2) {
        /**
         * attribute: motion
         */
        standardTile('motion', 'device.motion', canChangeIcon: false, width: 2, height: 2) {
            state('inactive', label: 'no motion', backgroundColor: '#ffffff', defaultState: true, icon: 'st.motion.motion.inactive')
            state('active', label: 'motion', backgroundColor: '#00a0dc', icon: 'st.motion.motion.active')
        }
        /**
         * attribute: temperature
         * needs Fahrenheit values to work (some internal conversion going on?)
         */
        valueTile('temperature', 'device.temperature', width: 2, height: 2) {
            state('temperature', label: '${currentValue}°C', unit: '°C', defaultState: true,
                    backgroundColors: [
                            // Celsius
                            [value:  0, color: '#153591'],
                            [value:  7, color: '#1e9cbb'],
                            [value: 15, color: '#90d2a7'],
                            [value: 23, color: '#44b621'],
                            [value: 29, color: '#f1d801'],
                            [value: 33, color: '#d04e00'],
                            [value: 37, color: '#bc2323'],
                            // Fahrenheit
                            [value: 40, color: "#153591"],
                            [value: 44, color: "#1e9cbb"],
                            [value: 59, color: "#90d2a7"],
                            [value: 74, color: "#44b621"],
                            [value: 84, color: "#f1d801"],
                            [value: 95, color: "#d04e00"],
                            [value: 96, color: "#bc2323"]
                    ]
            )
        }
        /**
         * attribute: humidity
         */
        valueTile('humidity', 'device.humidity', width: 2, height: 2) {
            state 'humidity', label: '${currentValue} %\nhumidity', unit: '%humidity', defaultState: true
        }
        /**
         * attribute: illuminance
         */
        valueTile('illuminance', 'device.illuminance', width: 2, height: 2) {
            state 'illuminance', label: '${currentValue}\nlux', unit: 'lux', defaultState: true
        }
        /**
         * attribute: ultraviolet index
         */
        valueTile('ultravioletIndex', 'device.ultravioletIndex', width: 2, height: 2) {
            state 'ultravioletIndex', label: '${currentValue} UV\nindex', unit: 'UV index', defaultState: true
        }
        /**
         * attribute: tamper
         * command:   resetTamper
         */
        standardTile('tamper', 'device.tamper', height: 2, width: 2, decoration: 'flat') {
            state 'clear', label: 'TAMPER CLEAR', backgroundColor: '#ffffff', action: 'resetTamper', defaultState: true, icon: 'https://github.com/rapportnetworks/stoptimised/raw/master/devicetypes/icons/completed@2x.png'
            state 'detected', label: 'TAMPERED', backgroundColor: '#ff0000', action: 'resetTamper', icon: 'https://github.com/rapportnetworks/stoptimised/raw/master/devicetypes/icons/tamper@2x.png'
        }
        /**
         * attribute: powerSource
         */
        standardTile('powerSource', 'device.powerSource', height: 2, width: 2, decoration: 'flat') {
            state 'battery', label: '${name}', backgroundColor: '#ffffff', icon: 'https://github.com/rapportnetworks/stoptimised/raw/master/devicetypes/icons/battery@2x.png'
            state 'dc', label: '${name}', backgroundColor: '#ffffff', icon: 'https://github.com/rapportnetworks/stoptimised/raw/master/devicetypes/icons/dc@2x.png'
            state 'mains', label: '${name}', backgroundColor: '#ffffff', icon: 'https://github.com/rapportnetworks/stoptimised/raw/master/devicetypes/icons/mains@2x.png'
            state 'unknown', label: '${name}', backgroundColor: '#ffffff', defaultState: true, icon: 'https://github.com/rapportnetworks/stoptimised/raw/master/devicetypes/icons/unknown@2x.png'
        }
        /**
         * attribute: battery
         */
        valueTile('battery', 'device.battery', decoration: 'flat', width: 2, height: 2) {
            state 'battery', label: '${currentValue} %\nbattery', unit: '%battery', defaultState: true
        }
        /**
         * command: batteryChange (used to record a change of device battery)
         */
        standardTile('batteryChange', 'device.batteryChange', height: 2, width: 2, decoration: 'flat') {
            state 'batteryChange', label: '${currentValue}', action: 'batteryChange', defaultState: true, icon: 'https://github.com/rapportnetworks/stoptimised/raw/master/devicetypes/icons/battery@2x.png'
        }
        /**
         * attribute: syncPending (number of configuration parameters that remain to be synched with the device)
         * command:   syncRemaining
         */
        valueTile('syncPending', 'device.syncPending', height: 2, width: 2, decoration: 'flat') {
            state 'syncPending', label: '${currentValue}\nto sync', unit: 'to sync', backgroundColor: '#ffffff', action: 'syncRemaining', defaultState: true
        }
        /**
         * command: syncAll
         */
        standardTile('syncAll', 'device.syncAll', height: 2, width: 2, decoration: 'flat') {
            state 'syncAll', label: 'SYNC ALL', action: 'syncAll', defaultState: true, icon: 'https://github.com/rapportnetworks/stoptimised/raw/master/devicetypes/icons/syncall@2x.png'
        }
        /**
         * command: configure (sets/resets device configuration parameters to default/specified values)
         */
        standardTile('configure', 'device.configure', height: 2, width: 2, decoration: 'flat') {
            state 'completed', label: 'CONFIGURE', action: 'configure', defaultState: true, icon: 'https://github.com/rapportnetworks/stoptimised/raw/master/devicetypes/icons/completed@2x.png' // , backgroundColor: '#ffffff'
            state 'received', label: 'RECEIVED', action: 'configure', icon: 'https://github.com/rapportnetworks/stoptimised/raw/master/devicetypes/icons/received@2x.png' // , backgroundColor: '#90d2a7'
            state 'queued', label: 'QUEUED', action: 'configure', icon: 'https://github.com/rapportnetworks/stoptimised/raw/master/devicetypes/icons/queued@2x.png' // , backgroundColor: '#90d2a7'
            state 'syncing', label: 'SYNCING', action: 'configure', icon: 'https://github.com/rapportnetworks/stoptimised/raw/master/devicetypes/icons/syncing@2x.png' //, backgroundColor: '#44b621'
        }
        /**
         * command: profile (requests power level and command class versions reports from the device)
         */
        standardTile('profile', 'device.profile', height: 2, width: 2, decoration: 'flat') {
            state 'profile', label: 'PROFILE', action: 'profile', defaultState: true, icon: 'https://github.com/rapportnetworks/stoptimised/raw/master/devicetypes/icons/profile@2x.png'
        }
        main('motion')
        details(['motion', 'temperature', 'humidity', 'illuminance', 'ultravioletIndex', 'tamper', 'powerSource', 'battery', 'batteryChange', 'syncPending', 'syncAll', 'configure', 'profile'])
    }

    simulator {
        status 'no motion': 'command: 9881, payload: 00300300'
        status 'motion': 'command: 9881, payload: 003003FF'

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

        status 'low battery alert': new physicalgraph.zwave.Zwave().securityV1.securityMessageEncapsulation().encapsulate(new physicalgraph.zwave.Zwave().batteryV1.batteryReport(batteryLevel: 255)).incomingMessage()

        status 'wake up': 'command: 8407, payload: '

        // reply '2001FF,delay 100,2502': 'command: 2503, payload: FF'
        // reply '200100,delay 100,2502': 'command: 2503, payload: 00'
    }

    preferences {
        if (configDeviceSettings()) {
            input(
                    name: 'paraDeviceSettings',
                    title: 'DEVICE SETTINGS',
                    description: 'Tap each item to set.',
                    type: 'paragraph',
                    element: 'paragraph'
            )

            if ('deviceUse' in configDeviceSettings()) {
                def uses           = deviceUseOptions().collectEntries { [(it.item): it.use] }
                def defaultUse     = deviceUseOptions().find { it.default }.item
                def defaultUseName = deviceUseOptions().find { it.default }.use
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

            if ('autoResetTamperDelay' in configDeviceSettings()) {
                input(
                        name: 'configAutoResetTamperDelay',
                        title: 'Auto-Reset Tamper Alarm after this time delay. (default: 30 seconds)',
                        type: 'enum',
                        options: [30: '30 seconds', 60: '1 minute', 120: '2 minutes', 300: '5 minutes', 3600: '1 hour'],
                        defaultValue: 30,
                        required: false
                )
            }

            if ('wakeUpInterval' in configDeviceSettings()) {
                def intervals           = wakeUpIntervalOptions().collectEntries { [(it.item): it.interval] }
                def defaultInterval     = (wakeUpIntervalOptions()?.find { it.specified }?.item)     ?: wakeUpIntervalOptions().find { it.default }.item
                def defaultIntervalName = (wakeUpIntervalOptions()?.find { it.specified }?.interval) ?: wakeUpIntervalOptions().find { it.default }.interval
                input(
                        name: 'configWakeUpInterval',
                        title: "Device Wake Up Interval. (default: ${defaultIntervalName})",
                        type: 'enum',
                        options: intervals,
                        defaultValue: defaultInterval,
                        required: false
                )
            }

            if ('logLevelIDE' in configDeviceSettings()) {
                input(
                        name: 'configLogLevelIDE',
                        title: 'IDE Live Logging Level for messages with this level and higher.',
                        type: 'enum',
                        options: [0: 'None', 1: 'Error', 2: 'Warning', 3: 'Info', 4: 'Debug', 5: 'Trace'],
                        defaultValue: 3,
                        required: false
                )
            }
        }

        if (configParametersUser()) generateParametersPreferences()
    }
}

/***********************************************************************************************************************
 * Preferences Helper Methods (generateParametersPreferences, getTimeOptionValueMap)
 **********************************************************************************************************************/
/**
 * generateParametersPreferences
 * @return selected parameter preference inputs
 */
private generateParametersPreferences() {
    input(
            name: 'paraDeviceParameters',
            title: 'DEVICE PARAMETERS',
            description: 'These are used to customise the operation of the device. Refer to the product documentation for a full description of each parameter.',
            type: 'paragraph',
            element: 'paragraph'
    )

    parametersMetadata().findAll{ it.id in configParameters() && !it.readonly }.each{
        /**
         * Gets list of parameters available to user to configure.
         * If the list is [0], all parameters in configParameters will be made available to the user.
         */
        if (configParametersUser()[0] == 0 || it.id in configParametersUser()) {

            def id = it.id.toString().padLeft(3, '0')

            def specific = parametersSpecifiedValues()?.find { spec -> spec.id == it.id }

            def prefDefault = (specific) ? specific.specified : it.default

            switch(it.type) {
                case 'number':
                    input(
                            name: "configParam${id}",
                            title: "${it.id}. ${it.name} (default: ${prefDefault})",
                            description: "${it.description} (default: ${prefDefault})",
                            type: it.type,
                            range: it.range,
                            defaultValue: prefDefault,
                            required: it.required
                    )
                    break

                case 'enum':
                    input(
                            name: "configParam${id}",
                            title: "${it.id}. ${it.name} (default: ${it.options?.find { op -> op.key == prefDefault}.value})",
                            description: "${it.description} (default: ${it.options?.find { op -> op.key == prefDefault}.value})",
                            type: it.type,
                            options: it.options,
                            defaultValue: prefDefault,
                            required: it.required
                    )
                    break

                case 'bool':
                    input(
                            name: "configParam${id}",
                            title: "${it.id}. ${it.name} (default: ${(prefDefault) ? 'on' : 'off'})",
                            description: "${it.description} (default: ${(prefDefault) ? 'on' : 'off'})",
                            type: it.type,
                            defaultValue: prefDefault,
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
                    it.flags.each { flag ->
                        def defaultOrSpecified = (specific) ? specific?.flags.find { specflag -> specflag.id == flag.id }?.specified : flag.default
                        def prefDefaultFlag = (flag.flag == defaultOrSpecified) ? true : false
                        input(
                                name: "configParam${id}${flag.id}",
                                title: "${flag.id}) ${flag.description} (default: ${(prefDefaultFlag) ? 'on' : 'off'})",
                                type: 'bool',
                                defaultValue: prefDefaultFlag,
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
private getTimeOptionValueMap() { [ // TODO - create a generalised lookup list - not currently used!
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
 * installed - sets up initial device states
 * called on first installation of device
 */
def installed() {
    logger('installed: setting initial state of device attributes', 'info')

    /**
     * sets initial logging level to IDE
     */
    state.logLevelIDE = 5

    /**
     * sends event to set checkInterval default value
     */
    def interval = wakeUpIntervalOptions().find { it.default }.item.multiply(2.2).trunc()
    sendEvent(name: 'checkInterval', value: interval, displayed: false, data: [protocol: 'zwave', hubHardwareId: device.hub.hardwareID], descriptionText: 'Default checkInterval')

    /**
     * sends event to clear any tamper due to installation
     */
    sendEvent(name: 'tamper', value: 'clear', descriptionText: 'Tamper cleared', displayed: false)

    /**
     * sets device use (if an option) and sends event to set inactive state TODO - check how this is reset/specified
     */
    if ('deviceUse' in configDeviceSettings()) {
        logger('installed: Setting device use states.', 'debug')
        deviceUseStates()
        sendEvent(name: "${getDataValue('event')}", value: "${getDataValue('inactiveState')}", displayed: false)
    }

    /**
     * set device power source (if only one) or 'unknown' if more than one option
     */
    def powerSource = powerSourceDefault()
    logger("installed: Device power source is ${powerSource}.", 'debug')
    sendEvent(name: 'powerSource', value: powerSource, descriptionText: "Device power source is ${powerSource}.")

    /**
     * sets up counter to track number of messages sent by device
     */
    state.messageCounter = 0
}

/**
 * configure - sets/resets device by setting configuration values to default/specified values
 * called by ST system after installed
 * called in response to configure command sent to device (via mobile app or smart app)
 */
def configure() {
    logger('configure: Setting/Resetting device configuration targets to default/specified values.', 'info')

    sendEvent(name: 'configure', value: 'received', descriptionText: 'Configuration command received by device.', isStateChange: true, displayed: false)

    state.configuringDevice = true

    def autoResetTamperDelayDefault = 30
    state.autoResetTamperDelay = autoResetTamperDelayDefault
    try {
        device.updateSetting('configAutoResetTamperDelay', autoResetTamperDelayDefault)
        logger("configure: Resetting autoResetTamperDelay preference to ${autoResetTamperDelayDefault}.", 'trace')
    }
    catch(e) {}

    def logLevelIDEDefault = 5 // TODO - set to 3 when finished debugging
    state.logLevelIDE = logLevelIDEDefault
    try {
        device.updateSetting('configLogLevelIDE', logLevelIDEDefault)
        logger("configure: Resetting configLogLevelIDE preference to ${logLevelIDEDefault}.", 'trace')
    }
    catch(e) {}

    if (commandClassesVersions().containsKey(0x84)) {
        def wakeUpIntervalDefault = (wakeUpIntervalOptions()?.find { it.specified }?.item) ?: wakeUpIntervalOptions().find { it.default }.item
        state.wakeUpIntervalTarget = wakeUpIntervalDefault
        try {
            device.updateSetting('configWakeUpInterval', wakeUpIntervalDefault)
            logger("configure: Resetting configWakeUpInterval preference to ${wakeUpIntervalDefault}.", 'trace')
        }
        catch(e) {}
    }

    logger('configure: getting default/specified values and resetting any existing preferences', 'trace')
    parametersMetadata().findAll( { it.id in configParameters() && !it.readonly } ).each {

        def specific = parametersSpecifiedValues()?.find { paramSV -> paramSV.id == it.id }
        def defaultValue = (specific) ? specific.specified : it.default
        def resetType = (specific) ? 'specified' : 'default'
        state."paramTarget${it.id}" = defaultValue

        def id = it.id.toString().padLeft(3, '0')

        switch(it.type) {
            case 'number':
                try {
                    device.updateSetting("configParam$id", defaultValue)
                    logger("configure: Parameter $id, resetting number preference to ($resetType): $defaultValue", 'debug')
                }
                catch(e) {}
                break

            case 'enum':
                try {
                    device.updateSetting("configParam$id", defaultValue)
                    logger("configure: Parameter $id, resetting enum preference to ($resetType): $defaultValue", 'debug')
                }
                catch(e) {}
                break

            case 'bool':
                def resetBool = (defaultValue == it.true) ? true : false
                try {
                    device.updateSetting("configParam$id", resetBool)
                    logger("configure: Parameter: $id, resetting bool preference to ($resetType): $resetBool", 'debug')
                }
                catch(e) {}
                break

            case 'flags':
                def defaultOrSpecified = (specific) ? specific.flags : it.flags
                defaultOrSpecified.each { flag ->
                    def defaultValueFlag = (flag?.specified != null) ? flag.specified : flag.default
                    def resetBool = (flag.flag == defaultValueFlag) ? true : false
                    try {
                        device.updateSetting("configParam$id${flag.id}", resetBool)
                        logger("configure: Parameter: $id${flag.id}, resetting flag preference to ($resetType): $resetBool", 'debug')
                    }
                    catch(e) {}
                }
                break

            default:
                logger('configure: Unhandled preference type.', 'warn')
        }
    }

    state.syncAll = true

    updateDataValue('serialNumber', null) // TODO - is this needed here?

    updated()
}

/**
 * updated - sets device configuration to user preferences selected in mobile app  only if state.configuringDevice = false
 * If called after configure, any selected user preferences will be ignored (and may have already been reset to defaults).
 * Due to a ST system bug, updated() is called twice in immediate succession. As a result, there is a check to abort the second call.
 */
def updated() {
    logger('updated: Updating configuration targets to match any user selected preferences.', 'info')
    if (!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 2000) {
        state.updatedLastRanAt = now()

        if (!state.configuringDevice) {
            if (settings?.configAutoResetTamperDelay != null) {
                state.autoResetTamperDelay = settings.configAutoResetTamperDelay.toInteger()
                logger("updated: autoResetTamperDelay set to value $state.autoResetTamperDelay", 'debug')
            }

            if (settings?.configLogLevelIDE != null) {
                state.logLevelIDE = settings.configLogLevelIDE.toInteger()
                logger("updated: logLevelIDE set to value $state.logLevelIDE", 'debug')
            }

            if (settings?.configWakeUpInterval != null) {
                state.wakeUpIntervalTarget = settings.configWakeUpInterval.toInteger()
                logger("updated: wakeUpIntervalTarget set to value $state.wakeUpIntervalTarget", 'debug')
            }

            parametersMetadata().findAll({ it.id in configParameters() && !it.readonly }).each {

                def id = it.id.toString().padLeft(3, '0')
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
                            def setting = (settings."configParam$id") ? it.true : it.false
                            logger("updated: Parameter $id set to match bool preference value: $setting", 'debug')
                            state."paramTarget$it.id" = setting
                            break

                        case 'flags':
                            def target = 0
                            settings.findAll { set -> set.key ==~ /configParam${id}[a-z]/ }.each { key, value ->
                                if (value) target += it.flags.find { flag -> flag.id == "${key.reverse().take(1)}" }.flag
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

        if ('deviceUse' in configDeviceSettings()) {
            logger('updateded: setting device use states', 'debug')
            deviceUseStates()
            sendEvent(name: "${getDataValue('event')}", value: "${getDataValue('inactiveState')}", displayed: false)
        }

        /**
         * If a listening device, the configuration commands can be sent immediately.
         * If a sleepy device, the configuration commands are queued (state.queued) until the device next wakes up.
         */
        if (listening()) {
            logger('updated: Listening device, calling sync now.', 'info')
            def cmds = sync()
            if (cmds) {
                response(sendCommandSequence(cmds))  // response wrapper needed in updated()
            }
            else {
                null
            }
        } else {
            logger('updated: Sleepy device, queuing sync.', 'info')
            sendEvent(name: 'configure', value: 'queued', descriptionText: 'Device reports Configuration queued.', isStateChange: true, displayed: false)
            if (state.queued) {
                state.queued << 'sync'
            } else {
                state.queued = ['sync']
            }
        }
    } else {
        logger('updated: Ran within last 2 seconds, so aborting update.', 'trace')
    }
}

/***********************************************************************************************************************
 * System Methods Helper Methods (sync, updateSyncPending, listening, byteArrayToUInt, deviceUseStates, logger)
 **********************************************************************************************************************/
/**
 * sync compares target and cached values for each configuration parameter. When there is a difference it assembles the relevant command to update the configuration setting in the device.
 * It then requests the configuration value back from the device to check that it has been successfully updated.
 * @return cmds - sequence of zwave commands
 */
private sync() {
    sendEvent(name: 'configure', value: 'syncing', descriptionText: 'Device is syncing.', isStateChange: true, displayed: false)
    def cmds = []
    def syncPending = 0

    if (state.syncAll) {
        logger('sync: Syncing all - deleting all cached configuration values.', 'debug')
        state.wakeUpIntervalCache = null
        parametersMetadata().findAll( { it.id in configParameters() && !it.readonly } ).each { state."paramCache${it.id}" = null }
        updateDataValue('serialNumber', null)
        state.syncAll = false
    }

    if (state.wakeUpIntervalTarget != null && state.wakeUpIntervalTarget != state.wakeUpIntervalCache) {
        logger("sync: Syncing Wake Up Interval with new value: ${state.wakeUpIntervalTarget}", 'debug')
        cmds += wakeUpIntervalSet(state.wakeUpIntervalTarget, zwaveHubNodeId)
        cmds += wakeUpIntervalGet()
        syncPending++
    }

    parametersMetadata().findAll( { it.id in configParameters() && !it.readonly } ).each {
        if (state."paramTarget${it.id}" != null && state."paramTarget${it.id}" != state."paramCache${it.id}") {
            def target = state."paramTarget${it.id}"
            logger("sync: Syncing parameter ${it.id.toString().padLeft(3, '0')} with new value: ${target}", 'debug')
            cmds += configurationSet(it.id, it.size, target)
            cmds += configurationGet(it.id)
            syncPending++
        }
    }

    if (getDataValue('serialNumber') == null) { // TODO - check if this is needed? - What if device doesn't have a serial number?
        logger('sync: Requesting device serial number.', 'debug')
        cmds += deviceSpecificGet()
        syncPending++
    }

    sendEvent(name: 'syncPending', value: syncPending, displayed: false, descriptionText: 'Change to syncPending.', isStateChange: true) // TODO - check this
    logger("sync: Commands: '$cmds'", 'trace')
    cmds
}

/**
 * updateSyncPending called when a report is received back from the device in response to check a configuration parameter value
 * keeps track of the number of outstanding configuration parameters that have yet to be successfully updated
 */
private updateSyncPending() {
    def syncPending = 0
    def userConfig = 0

    // if (!listening()) { TODO - check if wakeup interval applies to all listening devices? - need to be consistent with sync()
        def target = state?.wakeUpIntervalTarget
        if (target != null && target != state.wakeUpIntervalCache) syncPending++
        def specificInterval = (wakeUpIntervalOptions()?.find { it.specified }?.item) ?: wakeUpIntervalOptions().find { it.default }.item
        if (target != specificInterval) userConfig++
    // }

    parametersMetadata().findAll({ it.id in configParameters() && !it.readonly }).each {
        if (state."paramTarget${it.id}" != null) {
            if (state."paramCache${it.id}" != state."paramTarget${it.id}") {
                syncPending++
            } else if (state."paramCache${it.id}" != it.defaultValue) {
                def specific = parametersSpecifiedValues()?.find { paramSV -> paramSV.id == it.id }?.specifiedValue
                if (state."paramCache${it.id}" != specific) {
                    userConfig++
                }
            }
        }
    }

    if (getDataValue('serialNumber') == null) { // TODO - change this - make it optional or if set serialNumber to 0 if there isn't one?
        syncPending++
    }

    logger("updateSyncPending: $syncPending item(s) remain to be synced.", 'trace')
    sendEvent(name: 'syncPending', value: syncPending, displayed: false)

    if (syncPending == 0) {
        state.configuringDevice = false

        state?.queued?.removeAll { it == 'sync' }

        def configurationType = (userConfig > 0) ? 'user' : (parametersSpecifiedValues()) ? 'specified' : 'default'
        logger("updateSyncPending: Sync Complete. Configuration type: $configurationType", 'info')
        updateDataValue('configurationType', configurationType)
        sendEvent(name: 'configure', value: 'completed', descriptionText: "Device reports Configuration ($configurationType) completed.", isStateChange: true, displayed: false)

        def configurationReport = [:]
        parametersMetadata().findAll( { it.id in configParameters() && !it.readonly} ).each {
            def id = it.id.toString().padLeft(3, '0')
            configurationReport << [(id): state."paramCache${it.id}"]
        }

        logger('updateSyncPending: All Configuration Values Reported.', 'info')
        updateDataValue('configuredParameters', configurationReport.sort().collect { it }.join(','))
    }
    syncPending
}

/**
 * checks whether or not device is a 'Listening' device (usually powered by dc or mains)
 * @return
 */
private listening() {
    getZwaveInfo()?.zw?.startsWith('L')
    // getZwaveInfo()?.zw?.startsWith('S') // used for testing
}

/**
 * converts unsigned integers
 * @param byteArray
 * @return signed integer
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
        useStates = deviceUseOptions()?.find { it.item == settings.configDeviceUse.toInteger() }
    } else {
        useStates = deviceUseOptions()?.find { it.default == true }
    }
    def deviceUse     = (useStates) ? useStates.use      : 'Water'
    def event         = (useStates) ? useStates.event    : 'water'
    def inactiveState = (useStates) ? useStates.inactive : 'dry'
    def activeState   = (useStates) ? useStates.active   : 'wet'
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
private logger(msg, level = 'debug') { // TODO - Check whether want all the events sent
    switch(level) {
        case 'error':
            if (state.logLevelIDE >= 1) log.error(msg); sendEvent(descriptionText: "Error: ${msg}", displayed: false, isStateChange: true); break

        case 'warn':
            if (state.logLevelIDE >= 2) log.warn(msg); sendEvent(descriptionText: "Warning: ${msg}", displayed: false, isStateChange: true); break

        case 'info':
            if (state.logLevelIDE >= 3) log.info(msg); sendEvent(descriptionText: "Info: ${msg}", displayed: false, isStateChange: true); break

        case 'debug':
            if (state.logLevelIDE >= 4) log.debug(msg); break // sendEvent(descriptionText: "Debug: ${msg}", displayed: false, isStateChange: true);

        case 'trace':
            if (state.logLevelIDE >= 5) log.trace(msg); break

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
def refresh() { }

/***********************************************************************************************************************
 *  Device Handler Custom Commands (resetLog, resetTamper, syncAll, syncRemaining, profile)
 **********************************************************************************************************************/
/**
 * batteryChange - sends an event to log change of battery
 */
void batteryChange() {
    def date = new Date().format( 'yyyy-MM-dd' )
    sendEvent(name: 'batteryChange', value: date, descriptionText: 'Device battery was changed.', isStateChange: true, displayed: false)
}

/**
 * profile - initiates profiling of the device
 * @return
 */
def profile() {
    def profile = ['powerlevelGet', 'versionCommandClassGet']
    if (listening()) {
        logger('profile: Profiling now.', 'info')
        def cmds = []
        profile.each {
            cmds += "$it"()
        }
        sendCommandSequence(cmds)
    }
    else {
        logger('profile: Queuing profiling.', 'info')
        if (state.queued) {
            state.queued << profile
        }
        else {
            state.queued = profile
        }
    }
}

/**
 * resetTamper - resets tamper (after configured period as there is not a zwave tamper clear message)
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
    state.syncAll = true
    syncRemaining()
}

/**
 * syncRemaining - initiates a resynching of unsynched device configuration parameters
 * @return
 */
def syncRemaining() {
    def caller = (state.syncAll) ? 'syncAll' : 'syncRemaining'
    if (listening()) {
        logger("${caller}: Calling sync.", 'info')
        def cmds = sync()
        if (cmds) {
            sendCommandSequence(cmds)
        }
        else {
            null
        }
    } else {
        logger("${caller}: Queuing sync.", 'info')
        if (state.queued) {
            state.queued << 'sync'
        } else {
            state.queued = ['sync']
        }
    }
}

/***********************************************************************************************************************
 * Zwave Command Helpers
 * Generic zwave commands that are common to most, if not all devices.
 **********************************************************************************************************************/
/*
private association(commands) {
    sendEvent(descriptionText: 'Setting 1st Association Group', displayed: false)
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
    sendEvent(descriptionText: 'Requesting Binary Sensor Report', displayed: false)
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
        logger("noEncapsulation: Sending: '$cmd'", 'trace')
        cmd.format()
    }
}

/**
 * secureEncapsulate
 * @param cmd
 * @return cmd securely encapsulated
 */
private secureEncapsulate(physicalgraph.zwave.Command cmd) {
    logger("secureEncapsulation: Sending: '$cmd'", 'trace')
    zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

/**
 * crc16Encapsulate
 * @param cmd
 * @return cmd crc16 encapsulated
 */
private crc16Encapsulate(physicalgraph.zwave.Command cmd) {
    logger("crc16Encapsulation: Sending: '$cmd'", 'trace')
    zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

/***********************************************************************************************************************
 *  Parse Method
 *  Receives raw Zwave commands from devices and passes them to the appropriate handler
 *  @param description - raw Zwave command
 *  @return list of resultant events and/or commands
 **********************************************************************************************************************/
def parse(String description) {
    logger("parse: raw zwave message: '$description'", 'trace')

    def messages = state?.messageCounter ?: 0
    updateDataValue('messages', "${state.messageCounter}")
    state.messageCounter = messages + 1

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
            /**
             * unsolicited command class message acts as a trigger for listening devices to sync if any parameters remain unsynced
             * (otherwise reports requested via sync() would call sync() again)
             * (sleepy devices triggered via Wake Up report)
             */
            if (listening() && device.latestValue('syncPending') > 0 && cmd.commandClassId in commandClassesUnsolicited()) {
                logger('parse: sync called', 'debug')
                result += response(sendCommandSequence(sync())) // response wrapper needed in parse()
            }
        } else {
            logger("parse: Could not parse.  raw message '$description'", 'error')
        }
    }
    // logger("parse: Result '$result'", 'trace')
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
                // result << createEvent(name: 'tamper', value: 'clear') // is this needed - check other handlers
                break

            case 0x03:
                result += createEvent(name: 'tamper', value: 'detected', descriptionText: "$device.displayName was tampered", displayed: true, isStateChange: true)
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
    logger("configurationReport: '$cmd'", 'trace')
    def signed = parametersMetadata()?.find { it.id == cmd.parameterNumber }?.isSigned
    def paramValue = (signed) ? cmd.scaledConfigurationValue : byteArrayToUInt(cmd.configurationValue)

    logger("configurationReport: Parameter $cmd.parameterNumber has been set to value (${(signed) ? 'signed' : 'unsigned'}) $paramValue", 'debug')
    state."paramCache${cmd.parameterNumber}" = paramValue

    if (cmd.parameterNumber == powerSourceParameter()) powerSourceReport(cmd)

    if (parametersMetadata().find { !it.readonly } ) updateSyncPending()
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
    def map = [name: 'battery', unit: '%']
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
    def serialNumber = '0'
    // serialNumber = (cmd.deviceIdType == 1 && cmd.deviceIdDataFormat == 1) ? (cmd.deviceIdData.each { data -> serialNumber += "${String.format(' % 02 X ', data)}" }) : '0'
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
    def msr = String.format('%04X-%04X-%04X', cmd.manufacturerId, cmd.productTypeId, cmd.productId)
    updateDataValue('MSR', msr)
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
        updateDataValue('commandClassVersions', state.commandClassVersionsBuffer.findAll { it.value > 0 }.sort().collect { it }.join(','))
        state?.queued?.removeAll { it == 'versionCommandClassGet' }
    }
}

/**
 * 0x84: 2, Wake Up Interval Report
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpIntervalReport cmd) {
    logger("wakeUpIntervalReport: '$cmd'", 'debug')
    def wakeUpInterval = cmd.seconds
    state.wakeUpIntervalCache = wakeUpInterval
    updateDataValue('wakeUpInterval', "$wakeUpInterval")
    def interval = wakeUpInterval.toDouble().multiply(2.2).trunc()
    createEvent(name: 'checkInterval', value: interval, displayed: false, data: [protocol: 'zwave', hubHardwareId: device.hub.hardwareID], descriptionText: 'Configured checkInterval')
    updateSyncPending()
}

/**
 * 0x84: 2, Wake Up Notification
 * @param cmd
 * @return
 */
def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd) {
    logger('WakeUpNotification: Device woke up.', 'info')
    def cmds = []

    if (state.queued) {
        def queue = state.queued as Set
        queue.each {
            logger("WakeUpNotification: Calling $it", 'trace')
            cmds += "$it"()
        }
    }
    else if (device.latestValue('syncPending') > 0) {
        logger('WakeUpNotification: syncPending > 0, Calling sync', 'trace')
        cmds += sync()
    }

    if (!listening()) {
        if (!state?.timeLastBatteryReport || now() > state.timeLastBatteryReport + intervalsSpecifiedValues().batteryRefreshInterval) {
            logger('WakeUpNotification: Requesting Battery report.', 'trace')
            cmds += batteryGet()
            cmds += powerlevelGet()
        }
        else {
            sendEvent(name: 'battery', value: device.latestValue('battery'), unit: '%', isStateChange: true, displayed: false)
        }
    }
    if (cmds) {
        response(sendCommandSequence(cmds))
    }
    else {
        null
    }
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
    state?.queued?.removeAll { it == 'powerlevelGet' }
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
    logger("security: raw zwave message: '$cmd'", 'trace')
    def encapsulatedCommand = cmd.encapsulatedCommand(commandClassesVersions())
    logger("security: encapsulated zwave message: '$encapsulatedCommand'", 'trace')
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
    if (value == 0x00) {eventValue = 'dry'}
    if (value == 0xFF) {eventValue = 'wet'}
    def result = createEvent(name: 'water', value: eventValue, displayed: true, isStateChange: true, descriptionText: "$device.displayName is $eventValue")
    result
}

private powerSourceReport(cmd) {
    def result = []
    if (cmd.configurationValue[0] == 0) {
        result += createEvent(name: 'powerSource', value: 'dc', displayed: false)
    }
    else if (cmd.configurationValue[0] == 1) {
        result += createEvent(name: 'powerSource', value: 'battery', displayed: false)
    }
    result
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
 * commandClassesSecure() -  TODO - is this needed?
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
 * configDeviceSettings - menu items available in mobile app to be configured by user
 * @return list of items to configure
 */
private configDeviceSettings() { ['deviceUse', 'autoResetTamperDelay', 'wakeUpInterval', 'logLevelIDE'] }

/**
 * deviceUseOptions() - map of states for contact sensor depending on use
 * @return map of device use options
 */
private deviceUseOptions() { [
    [item: 0, use: 'Bed', event: 'contact', inactive: 'empty', active: 'occupied'],
    [item: 1, use: 'Chair', event: 'contact', inactive: 'vacant', active: 'occupied'],
    [item: 2, use: 'Toilet', event: 'contact', inactive: 'full', active: 'flushing'],
    [item: 3, use: 'Water', event: 'water', inactive: 'dry', active: 'wet', default: true],
] }

/**
 * wakeUpIntervalOptions
 * @return map of preference options for configuring device wake interval
 */
private wakeUpIntervalOptions() { [
        [item:  3_600, interval:  '1 hour', specified: true],
        [item:  7_200, interval:  '2 hours', default: true],
        [item: 34_200, interval: '12 hours'],
] }

/**
 * configParameters - set of device configuration parameters with a specified value required for optimal operation
 * 0 will make all items in parametersMetadata available
 * @return list of device parameters
 */
private configParameters() { [2, 3, 4, 5, 8, 9, 40, 81, 101, 102, 103, 111, 112, 113] }

/**
 * configParametersUser - subset of device configuration parameters that are made available for configuration by the user in the mobile app
 * set to [0] to make all configuration parameters available to user
 * @return list of device parameters
 */
private configParametersUser() { [2, 3, 4, 5, 8, 81, 101, 102, 103, 111, 112, 113] }

/**
 * powerSourceParameter - device configuration parameter number that relates to devices power source (if alternatives)
 * set to 0 if not applicable
 * @return power source parameter number
 */
private powerSourceParameter() { 9 }

/**
 * getPowerSourceDefault
 * @return name of default power source for device
 */
private getPowerSourceDefault() {
    // battery // for battery powered devices
    // dc // for usb powered devices
    // mains // for mains powered devices
    unknown // for devices with more than one power source
}

/**
 * batteryRefreshInterval
 * @return
 */
private batteryRefreshInterval() { 604_800 }

/**
 * parametersSpecifiedValues - map of specified configuration values for optimal operation (some may be same as default) - means device configuration can be set/reset
 * @return map of specified/default values for device parameters
 */
private parametersSpecifiedValues() { [
    [id:2,size:1,default:0,specified:1],
    [id:3,size:2,default:240,specified:30],
    [id:4,size:1,default:5,specified:5],
    [id:5,size:1,default:1,specified:2],
    [id:40,size:1,default:0,specified:0],
    [id:81,size:1,default:0,specified:2],
    [id:101,size:4,default:241,specified:240,flags:[[id:'a',flag:1,default:1,specified:0],[id:'b',flag:16,default:16,specified:16],[id:'c',flag:32,default:32,specified:32],[id:'d',flag:64,default:64,specified:64],[id:'e',flag:128,default:128,specified:128]]],
    [id:102,size:4,default:0,specified:0,flags:[[id:'a',flag:1,default:0,specified:0],[id:'b',flag:16,default:0,specified:0],[id:'c',flag:32,default:0,specified:0],[id:'d',flag:64,default:0,specified:0],[id:'e',flag:128,default:0,specified:0]]],
    [id:103,size:4,default:0,specified:0,flags:[[id:'a',flag:1,default:0,specified:0],[id:'b',flag:16,default:0,specified:0],[id:'c',flag:32,default:0,specified:0],[id:'d',flag:64,default:0,specified:0],[id:'e',flag:128,default:0,specified:0]]],
    [id:111,size:4,default:3600,specified:3600]
] }

/**
 * parametersMetadata - complete map of all (relevant) device configuration parameters
 * @return map of device configuration parameters
 */
private parametersMetadata() { [
    [id:2,size:1,type:'bool',default:0,required:false,readonly:false,isSigned:false,name:'Enable waking up for 10 minutes',description:'when re-power on (battery mode) the MultiSensor',false:0,true:1],
    [id:3,size:2,type:'number',range: '10..3600',default:240,required:false,readonly:false,isSigned:false,name: 'PIR reset time',description:'Reset time for PIR sensor'],
    [id:4,size:1,type:'enum',default:5,required:false,readonly:false,isSigned:false,name:'PIR Sensitivity',description:'Set the sensitivity of motion sensor',options:[0:'Off',1:'level 1 (minimum)',2:'level 2',3:'level 3',4:'level 4',5:'level 5 (maximum)']],
    [id:5,size:1,type:'enum',default:1,required:false,readonly:false,isSigned:false,name:'Which command?',description:'Command sent when the motion sensor triggered.',options:[1:'send Basic Set CC',2:'send Sensor Binary Report CC']],
    [id:8,size:1,type:'number',range: '15..60',default:15,required:false,readonly:false,isSigned:false,name: 'Timeout of after Wake Up',description:'Set the timeout of awake after the Wake Up CC is sent out'],
    [id:9,size:2,type:'flags',required:false,readonly:true,isSigned:false,name:'Report the current power mode and the product state for battery power mode',description:'Report the current power mode and the product state for battery power mode'],
    [id:40,size:1,type:'bool',default:0,required:false,readonly:false,isSigned:false,name:'Selective reporting',description:'Enable selective reporting',false:0,true:1],
    [id:81,size:1,type:'enum',default:0,required:false,readonly:false,isSigned:false,name:'Enable LED',description:'Enable/disable the LED blinking',options:[0:'Enable LED blinking',1:'Disable LED blinking only when the PIR is triggered',2:'Completely disable LED for motion; wakeup; and sensor report']],
    [id:101,size:4,type:'flags',default:241,required:false,readonly:false,isSigned:false,name:'Group 1 Report',description:'Which report needs to be sent in Report group 1',flags:[[id:'a',description:'enable battery',flag:1,default:1],[id:'b',description:'enable ultraviolet',flag:16,default:16],[id:'c',description:'enable temperature',flag:32,default:32],[id:'d',description:'enable humidity',flag:64,default:64],[id:'e',description:'enable luminance',flag:128,default:128]]],
    [id:102,size:4,type:'flags',default:0,required:false,readonly:false,isSigned:false,name:'Group 2 Report',description:'Which report needs to be sent in Report group 2',flags:[[id:'a',description:'enable battery',flag:1,default:0],[id:'b',description:'enable ultraviolet',flag:16,default:0],[id:'c',description:'enable temperature',flag:32,default:0],[id:'d',description:'enable humidity',flag:64,default:0],[id:'e',description:'enable luminance',flag:128,default:0]]],
    [id:103,size:4,type:'flags',default:0,required:false,readonly:false,isSigned:false,name:'Group 3 Report',description:'Which report needs to be sent in Report group 3',flags:[[id:'a',description:'enable battery',flag:1,default:0],[id:'b',description:'enable ultraviolet',flag:16,default:0],[id:'c',description:'enable temperature',flag:32,default:0],[id:'d',description:'enable humidity',flag:64,default:0],[id:'e',description:'enable luminance',flag:128,default:0]]],
    [id:111,size:4,type:'number',range: '300..12000',default:3600,required:false,readonly:false,isSigned:false,name: 'Time interval of group 1 report',description:'The interval time of sending reports in group 1'],
    [id:112,size:4,type:'number',range: '300..12000',default:3600,required:false,readonly:false,isSigned:false,name: 'Time interval of group 2 report',description:'The interval time of sending reports in group 2'],
    [id:113,size:4,type:'number',range: '300..12000',default:3600,required:false,readonly:false,isSigned:false,name: 'Time interval of group 3 report',description:'The interval time of sending reports in group 3']
] }