/*****************************************************************************************************************
 *  Copyright Alasdair Thin
 *
 *  Name: Configurator
 *
 *  Date: 2018-12-04
 *
 *  Version: 1.0
 *
 *  Source:
 *
 *  Author: Alasdair Thin
 *
 *  Description: A SmartApp to log SmartThings device states to an InfluxDB database.
 *
 *  Acknowledgements: Includes code originally developed by David Lomas (codersaur) and Kevin LaFramboise (krlaframboise).
 *
 *  Original Source: https://github.com/codersaur/SmartThings/tree/master/smartapps/influxdb-logger
 *
 *  License:
 *   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *   for the specific language governing permissions and limitations under the License.
 *****************************************************************************************************************/

definition (
        name: "Configurator",
        namespace: "rapportnetworks",
        author: "Alasdair Thin",
        description: "Configure SmartThings Devices",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)

preferences {
    page(name: "mainPage")
    page(name: "devicesPage")
}

def mainPage() {
    dynamicPage(name:"mainPage", uninstall:true, install:true, refreshInterval: 5) {

        section("General") {
            input (
                    name: "configLoggingLevelIDE",
                    title: "IDE Live Logging Level. Messages with this level and higher will be logged to the IDE.",
                    type: "enum",
                    options: [ "0" : "None", "1" : "Error", "2" : "Warning", "3" : "Info", "4" : "Debug", "5" : "Trace" ],
                    defaultValue: "3",
                    displayDuringSetup: true,
                    required: false,
            )
            input(
                    name: 'clearSelections',
                    title: 'Clear device selections after sending configure command.',
                    type: 'bool',
                    defaultValue: true,
                    displayDuringSetup: true,
                    required: false,
            )
        }


        if (state.devicesConfigured && !settings.clearSelections) {
            section("Selected Devices") {
                getPageLink("devicesPageLink", "Tap to change", "devicesPage", null, buildSummary(selectedDeviceNames))
            }
        }
        else {
            devicesPageContent
        }
    }
}

def devicesPage() {
    dynamicPage(name:"devicesPage") {
        devicesPageContent
    }
}

private getDevicesPageContent() {
    section("Choose Devices") {
        paragraph '''Each device only needs to be selected once and which field you select it from has no effect on which events will be logged for it. There's a field below for every capability, but you should be able to locate most of your devices in either the 'Actuators' or 'Sensors' fields at the top.'''

        capabilities.each {
            if (it.cap != 'bridge') {
                try {
                    input "${it.cap}Pref", "capability.${it.cap}",
                            title: "${it.title}:",
                            multiple: true,
                            hideWhenEmpty: true,
                            required: false,
                            submitOnChange: true
                }
                catch (final e) {
                    logTrace "Failed to create input for ${it}: ${e.message}"
                }
            }
        }

    }
}

private getSelectedDeviceNames() {
    try {
        return selectedDevices?.collect { "${it?.displayName}" + "${(it.hasCapability('Configuration')) ? '+' : '-'}" + "${(it.hasCommand('configure')) ? '+' : '-'}" }?.sort() // check this works first then add ${(it.hasCapability('Configuration')) ? '+' : '-'}
    }
    catch (final e) {
        logWarn "Error while getting selected device names: ${e.message}"
        return []
    }
}

private getSelectedDevices() {
    final def devices = []
    capabilities?.each {
        try {
            if (settings?."${it.cap}Pref") {
                devices << settings?."${it.cap}Pref"
            }
        }
        catch (final e) {
            logWarn "Error while getting selected devices for capability ${it}: ${e.message}"
        }
    }
    return devices?.flatten()?.unique { it.id }
}

private getPageLink(final linkName, final linkText, final pageName, final args=null, final desc="", final image=null) {
    final def map = [
            name: "$linkName",
            title: "$linkText",
            description: "$desc",
            page: "$pageName",
            required: false,
    ]
    if (args) map.params = args
    if (image) map.image = image
    href(map)
}

private static buildSummary(final items) {
    def summary = ""
    items?.each {
        summary += summary ? "\n" : ""
        summary += "   ${it}"
    }
    summary
}

/*****************************************************************************************************************
 *  SmartThings System Commands:
 *****************************************************************************************************************/
def installed() { // runs when the app is first installed
    state.loggingLevelIDE = 5
    log.debug "${app.label}: Installed with settings: ${settings}"
}

def uninstalled() { // runs when the app is uninstalled
    logger("uninstalled()","trace")
}

def updated() { // runs when app settings are changed
    logger("updated()","trace")

    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3

    if (selectedDevices) {
        state.devicesConfigured = true
    }
    else {
        logger('Unconfigured - Choose Devices', 'debug')
    }

    if (state.devicesConfigured) configureCommand()

    if (settings.clearSelections) {
        runIn(30, resetPrefs)
        state.devicesConfigured = false
    }
}

/*****************************************************************************************************************
 *  Event Handlers:
 *****************************************************************************************************************/
def handleAppTouch(final evt) { // handleAppTouch(evt) - used for testing
    logger("handleAppTouch()","trace")
}

/*****************************************************************************************************************
 *  Main Commands:
 *****************************************************************************************************************/
def configureCommand() {
    selectedDevices?.each  { final dev ->
        if (!dev.displayName.startsWith("~")) {
            if (dev.hasCapability("Configuration")) {
                log.debug "${dev.displayName} has Configuration Capability"
                if (dev.hasCommand("configure")) {
                    log.debug "${dev.displayName} has configure Command"
                    dev.configure()
                    log.debug "configure Command sent to ${dev.displayName}"
                }
                else {
                    log.debug "${dev.displayName} does not have configure Command"
                }
            }
            else {
                log.debug "${dev.displayName} does not have Configuration Capability"
            }
        }
    }
}


/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/
def resetPrefs() {
    // app.updateSetting(inputName, [type: type, value: value])
    capabilities?.each {
        if (settings?."${it.cap}Pref") {
            app.updateSetting("${it.cap}Pref", [])
            logger('Resetting preferences.', 'info')
        }
    }
}

private logger(final msg, final level = "debug") { // Wrapper function for all logging
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

private static getCapabilities() { [
        [title: 'Actuators', cap: 'actuator'],
        [title: 'Bridges', cap: 'bridge'],
        [title: 'Sensors', cap: 'sensor', attr: ['buttonClicks', 'current', 'pressure', 'reactiveEnergy', 'reactivePower', 'totalEnergy']],
        [title: 'Acceleration Sensors', cap: 'accelerationSensor', attr: 'acceleration'],
        [title: 'Alarms', cap: 'alarm', attr: 'alarm'],
        [title: 'Batteries', cap: 'battery', attr: 'battery'],
        [title: 'Beacons', cap: 'beacon', attr: 'presence'],
        [title: 'Bulbs', cap: 'bulb', attr: 'switch'],
        [title: 'Buttons', cap: 'button', attr: ['button', 'numberOfButtons']],
        [title: 'Carbon Dioxide Measurement Sensors', cap: 'carbonDioxideMeasurement', attr: 'carbonDioxide'],
        [title: 'Carbon Monoxide Detectors', cap: 'carbonMonoxideDetector', attr: 'carbonMonoxide'],
        [title: 'Color Control Devices', cap: 'colorControl', attr: ['color', 'hue', 'saturation']],
        [title: 'Color Temperature Devices', cap: 'colorTemperature', attr: 'colorTemperature'],
        [title: 'Consumable Devices', cap: 'consumable', attr: 'consumableStatus'],
        [title: 'Contact Sensors', cap: 'contactSensor', attr: 'contact'],
        [title: 'Doors', cap: 'doorControl', attr: 'door'],
        [title: 'Energy Meters', cap: 'energyMeter', attr: 'energy'],
        [title: 'Garage Doors', cap: 'garageDoorControl', attr: 'door'],
        [title: 'Holdable Buttons', cap: 'holdableButton', attr: ['button', 'numberOfButtons']],
        [title: 'Illuminance Measurement Sensors', cap: 'illuminanceMeasurement', attr: 'illuminance'],
        [title: 'Lights', cap: 'light', attr: 'switch'],
        [title: 'Locks', cap: 'lock', attr: 'lock'],
        [title: 'Motion Sensors', cap: 'motionSensor', attr: 'motion'],
        [title: 'Music Players', cap: 'musicPlayer', attr: ['level', 'mute', 'status', 'trackDescription']],
        [title: 'Outlets', cap: 'outlet', attr: 'switch'],
        [title: 'Power Meters', cap: 'powerMeter', attr: 'power'],
        [title: 'Power Sources', cap: 'powerSource', attr: 'powerSource'],
        [title: 'Power', cap: 'power', attr: 'powerSource'],
        [title: 'Presence Sensors', cap: 'presenceSensor', attr: 'presence'],
        [title: 'Relative Humidity Measurement Sensors', cap: 'relativeHumidityMeasurement', attr: 'humidity'],
        [title: 'Relay Switches', cap: 'relaySwitch', attr: 'switch'],
        [title: 'Shock Sensors', cap: 'shockSensor', attr: 'shock'],
        [title: 'Signal Strength Sensors', cap: 'signalStrength', attr: ['lqi', 'rssi']],
        [title: 'Sleep Sensors', cap: 'sleepSensor', attr: 'sleeping'],
        [title: 'Smoke Detectors', cap: 'smokeDetector', attr: 'smoke'],
        [title: 'Sound Pressure Level Sensors', cap: 'soundPressureLevel', attr: 'soundPressureLevel'],
        [title: 'Sound Sensors', cap: 'soundSensor', attr: 'sound'],
        [title: 'Switch Level Sensors', cap: 'switchLevel', attr: 'level'],
        [title: 'Switches', cap: 'switch', attr: 'switch'],
        [title: 'Tamper Alert Sensors', cap: 'tamperAlert', attr: 'tamper'],
        [title: 'Temperature Measurement Sensors', cap: 'temperatureMeasurement', attr: 'temperature'],
        [title: 'Thermostats', cap: 'thermostat', attr: ['heatingSetpoint', 'temperature', 'thermostatFanMode', 'thermostatMode', 'thermostatOperatingState', 'thermostatSetpoint']],
        [title: 'Three Axis Sensors', cap: 'threeAxis', attr: 'threeAxis'],
        [title: 'Touch Sensors', cap: 'touchSensor', attr: 'touch'],
        [title: 'Ultraviolet Index Sensors', cap: 'ultravioletIndex', attr: 'ultravioletIndex'],
        [title: 'Valves', cap: 'valve', attr: 'valve'],
        [title: 'Voltage Measurement Sensors', cap: 'voltageMeasurement', attr: 'voltage'],
        [title: 'Water Sensors', cap: 'waterSensor', attr: 'water'],
        [title: 'Window Shades', cap: 'windowShade', attr: 'windowShade'],
] }