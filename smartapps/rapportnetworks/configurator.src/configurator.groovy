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
}

def mainPage() {
    dynamicPage(name: 'mainPage', uninstall:true, install:true, refreshInterval: 5) {

        section('Options') {
            input (
                    name: 'configLoggingLevelIDE',
                    title: 'IDE Live Logging Level.',
                    type: 'enum',
                    options: [ '0': 'None', '1': 'Error', '2': 'Warning', '3': 'Info', '4': 'Debug', '5': 'Trace' ],
                    defaultValue: '3',
                    displayDuringSetup: true,
                    required: false
            )
        }

        section('Select devices to Configure') {
            input(name: "configurationPref", type: "capability.configuration", title: "Configuration:", multiple: true, required: false, submitOnChange: true) // hideWhenEmpty: true,
            paragraph(title: 'Selected Devices', "${createSummary(selectedDeviceNames)}")
        }
    }
}

private getSelectedDeviceIds() {
    settings?.configurationPref?.collect { it.id }
}

/*
private getSelectedDeviceNames() {
    try {
        selectedDevices?.collect {
            def sp = (it?.currentState('syncPending')?.date?.time > it?.currentState('configure')?.date?.time) ? it.currentState('syncPending')?.value : '?'
            "${it?.displayName} (${it?.getZwaveInfo()?.zw?.take(1)})\n ->${it?.currentState('configure')?.date?.format('yyyy/MM/dd-HH:mm')} [$sp]"
        }?.sort()
    }
    catch (e) {
        logger("Preferences: Error while getting selected device names: ${e.message}", 'warn')
        []
    }
}
*/

private getSelectedDeviceNames() {
    def devices = []
    settings?.configurationPref?.each {
        def sp = (it?.currentState('syncPending')?.date?.time > it?.currentState('configure')?.date?.time) ? it.currentState('syncPending')?.value : '?'
        devices << "${it?.displayName} (${it?.getZwaveInfo()?.zw?.take(1)})\n ->${it?.currentState('configure')?.date?.format('yyyy/MM/dd-HH:mm')} [$sp]"
    }
    devices
}

private getSelectedDevices() {
    def devices = []
    try {
        if (settings?.configurationPref) {
            devices << settings.configurationPref
        }
    }
    catch (e) {
        logger("Preferences: Error while getting selected devices for configuration capability: ${e.message}", 'warn')
    }
    devices?.flatten()?.unique { it.id }
}

private static createSummary(items) {
    def summary = ''
    items?.each {
        summary += summary ? '\n' : ''
        summary += "$it"
    }
    summary
}

/*****************************************************************************************************************
 *  SmartThings System Commands:
 *****************************************************************************************************************/
def installed() {
    logger("Installed App: ${app.label}. Installed with settings: ${settings}", 'trace')
    state.loggingLevelIDE = 5 // change to 3 when finished
}

def updated() {
    logger("Updated App: ${app.label}. Updated with settings: ${settings}", 'trace')

    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3

    subscribe(app, handleAppTouch)

    state.devicesSelected = getSelectedDeviceIds()

    state.sendCounter = 2
}

def uninstalled() {
    logger("Uninstalled App: ${app.label}.", 'trace')
}

/*****************************************************************************************************************
 *  Event Handlers:
 *****************************************************************************************************************/
def handleAppTouch(evt) { // SmartApp Touch event
    logger("handleAppTouch: Event triggered: $evt", 'trace')
    // app.updateSetting('configurationPref', [])
    controller()
    unsubscribe()
}

/*****************************************************************************************************************
 *  Main Commands:
 *****************************************************************************************************************/
def controller() {
    logger('controller: Called', 'trace')
    if (state.sendCounter > 0) {
        configureCommand()
        runIn(30, checkReceived)
        runIn(60, controller)
        state.sendCounter = state.sendCounter - 1
    }

}

def configureCommand() {
    logger('configureCommand: Called', 'trace')
    settings?.configurationPref?.each {
        if (it.id in state?.devicesSelected) {
            logger("configureCommand: Sending Configure Command to $it", 'info')
            it.configure()
        }
    }
}

def checkReceived() {
    def removalList = []
    settings?.configurationPref?.each {
        if (it.id in state?.devicesSelected) {
            if (it?.currentState('configure')?.value == 'received') {
                logger("checkReceived: Deselecting: ${it.id} : ${it.displayName}", 'info')
                removalList << it.id
            }
        }
    }
    if (removalList) state.devicesSelected = state.devicesSelected - removalList
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/
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