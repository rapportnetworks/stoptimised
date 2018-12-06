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
 *  Description: A SmartApp to Send Configure Command to selected SmartThings Devices with the Configuration Capability.
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
        name: 'Configurator',
        namespace: 'rapportnetworks',
        author: 'Alasdair Thin',
        description: 'Configure SmartThings Devices',
        category: 'My Apps',
        iconUrl: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png',
        iconX2Url: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png',
        iconX3Url: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png'
)

preferences {
    page(name: 'mainPage')
    page(name: 'devicesPage')
}

def mainPage() {
    dynamicPage(name: 'mainPage', uninstall:true, install:true) {

        section('Options') {
            input (
                    name: 'configLoggingLevelIDE',
                    title: 'IDE Live Logging Level. Messages with this level and higher will be logged to the IDE.',
                    type: 'enum',
                    options: [ '0': 'None', '1': 'Error', '2': 'Warning', '3': 'Info', '4': 'Debug', '5': 'Trace' ],
                    defaultValue: '3',
                    displayDuringSetup: true,
                    required: false
            )
            input(
                    name: 'clearSelections',
                    title: 'Clear device selections after sending Configure command.',
                    type: 'bool',
                    defaultValue: true,
                    displayDuringSetup: true,
                    required: false
            )
            input(
                    name: 'enableTouch',
                    title: 'Enable App Touch to send Configure command.',
                    type: 'bool',
                    defaultValue: false,
                    displayDuringSetup: true,
                    required: false
            )
        }

        if (state.devicesConfigured && !settings.clearSelections) {
            section('Selected Devices') {
                getPageLink('devicesPageLink', 'Tap to change', 'devicesPage', null, createSummary(selectedDeviceNames))
            }
        }
        else {
            devicesPageContent
        }
    }
}

def devicesPage() {
    dynamicPage(name: 'devicesPage') {
        devicesPageContent
    }
}

private getDevicesPageContent() {
    section('Choose Devices') {
        paragraph 'Select devices to send Configure command to. Only devices with Configuration capability will receive the command.'

        capabilities.each {
            try {
                input(
                        "${it.cap}Pref",
                        "capability.${it.cap}",
                        title: "${it.title}:",
                        multiple: true,
                        hideWhenEmpty: true,
                        required: false,
                        submitOnChange: true)
            }
            catch (e) {
                logger("Preferences: Failed to create input for ${it}: ${e.message}", 'trace')
            }
        }

    }
}

private getSelectedDeviceNames() {
    try {
        selectedDevices?.collect {
            "${(it.hasCapability('Configuration')) ? '+' : '-'}${(it.hasCommand('configure')) ? '+' : '-'}${it?.displayName}"
        }?.sort()
    }
    catch (e) {
        logger("Preferences: Error while getting selected device names: ${e.message}", 'warn')
        []
    }
}

private getSelectedDevices() {
    def devices = []
    capabilities?.each {
        try {
            if (settings?."${it.cap}Pref") {
                devices << settings?."${it.cap}Pref"
            }
        }
        catch (e) {
            logger("Preferences: Error while getting selected devices for capability ${it}: ${e.message}", 'warn')
        }
    }
    devices?.flatten()?.unique { it.id }
}

private getPageLink(linkName, linkText, pageName, args=null, desc='', image=null) {
    def map = [
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

private static createSummary(items) {
    def summary = ''
    items?.each {
        summary += summary ? '\n' : ''
        summary += "   $it"
    }
    summary
}

/*****************************************************************************************************************
 *  SmartThings System Commands:
 *****************************************************************************************************************/
def installed() {
    logger("Installed App: ${app.label}. Installed with settings: ${settings}", 'trace')
    state.loggingLevelIDE = 5
}

def updated() {
    logger("Updated App: ${app.label}. Updated with settings: ${settings}", 'trace')

    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3

    (settings.enableTouch) ? subscribe(app, handleAppTouch) : unsubscribe()

    (selectedDevices) ? state.devicesConfigured = true : logger('Unconfigured - Choose Devices', 'debug')

    if (state.devicesConfigured) configureCommand()

    if (settings.clearSelections) runIn(15, resetPrefs)
}

def uninstalled() {
    logger("Uninstalled App: ${app.label}.", 'trace')
}

/*****************************************************************************************************************
 *  Event Handlers:
 *****************************************************************************************************************/
def handleAppTouch(evt) { // SmartApp Touch event
    logger("handleAppTouch: Event triggered: $evt", 'trace')
    if (state.devicesConfigured) configureCommand()
}

/*****************************************************************************************************************
 *  Main Commands:
 *****************************************************************************************************************/
def configureCommand() {
    if (!state.configureLastSentAt || now() >= state.configureLastSentAt + 300_000) {
        state.configureLastSentAt = now()
        selectedDevices?.each  {
            if (it.hasCapability('Configuration')) {
                logger("${it.displayName} has Configuration Capability.", 'debug')
                if (it.hasCommand('configure')) {
                    logger("Configure Command sent to ${it.displayName}.", 'info')
                    it.configure()
                }
                else {
                    logger("${it.displayName} does not have Configure Command.", 'info')
                }
            }
            else {
                logger("${it.displayName} does not have Configuration Capability.", 'debug')
            }
        }
    }
    else {
        logger('configureCommand: Configure command sent within last 5 minutes so aborting.', 'trace')
    }
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/
def resetPrefs() {
    state.devicesConfigured = false
    capabilities?.each {
        if (settings?."${it.cap}Pref") {
            logger('resetPrefs: Resetting preferences.', 'info')
            app.updateSetting("${it.cap}Pref", [])
        }
    }
}

private logger(msg, level = 'debug') {
    switch (level) {
        case 'error':
            if (state.loggingLevelIDE >= 1) log.error msg
            break
        case 'warn':
            if (state.loggingLevelIDE >= 2) log.warn msg
            break
        case 'info':
            if (state.loggingLevelIDE >= 3) log.info msg
            break
        case 'debug':
            if (state.loggingLevelIDE >= 4) log.debug msg
            break
        case 'trace':
            if (state.loggingLevelIDE >= 5) log.trace msg
            break
        default:
            log.debug msg
            break
    }
}

private static getCapabilities() { [
        [title: 'Configurable', cap: 'configuration'],
        [title: 'Sensors', cap: 'sensor'],
        [title: 'Actuators', cap: 'actuator'],
] }