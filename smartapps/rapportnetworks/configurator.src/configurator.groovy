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
    page(name: 'mainPage', install: true, uninstall: true)

}

def mainPage() {
    dynamicPage(name: 'mainPage') {
        section {
            input(name: 'configurationPref', type: 'capability.configuration', title: 'Select Devices with Configuration capability:', multiple: true, required: false, submitOnChange: true)
        }
        section {
            paragraph(title: 'Details of Selected Devices:', "${createSummary(selectedDeviceNames)}")
        }
        section {
            input(name: 'loggingPref', title: 'Log Smart App activity.', type: 'bool', defaultValue: false, required: false)
        }
    }
}

private getSelectedDeviceNames() {
    settings?.configurationPref?.collect {
        def items = (it?.currentState('syncPending')?.date?.time > it?.currentState('configure')?.date?.time) ? it.currentState('syncPending')?.value : '?'
        "${it?.displayName}(${it?.getZwaveInfo()?.zw?.take(1)}) >${it?.currentState('configure')?.date?.format('yyyy/MM/dd-HH:mm')}[$items]"
    }
}

private createSummary(items) {
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
    logger("installed: ${app.label} installed with settings: ${settings}.", 'trace')
}

def updated() {
    logger("updated: ${app.label} updated with settings: ${settings}.", 'trace')
    state.selectedDevices = selectedDeviceIds
    subscribe(app, handleAppTouch)
    state.sendCounter = 2
}

def uninstalled() {
    logger("uninstalled: ${app.label} uninstalled.", 'trace')
}

/*****************************************************************************************************************
 *  Event Handlers:
 *****************************************************************************************************************/
def handleAppTouch(event) { // SmartApp Touch event
    logger("handleAppTouch: App trigger event: $event", 'trace')
    unsubscribe()
    controller()
}

/*****************************************************************************************************************
 *  Main Commands:
 *****************************************************************************************************************/
def controller() {
    logger("controller: Called. sendCounter = $state.sendCounter", 'trace')
    if (state.sendCounter > 0) {
        sendCommand()
        runIn(30, checkReceived)
        runIn(60, controller)
        state.sendCounter = state.sendCounter - 1
    }

}

def sendCommand() {
    logger('sendCommand: Called.', 'trace')
    settings?.configurationPref?.each {
        if (it.id in state?.selectedDevices) {
            logger("sendCommand: Sending Configure command to ${it?.displayName} [${it.id}]", 'info')
            it.configure()
        }
    }
}

def checkReceived() {
    def removalList = []
    settings?.configurationPref?.each {
        if (it.id in state?.selectedDevices) {
            if (it?.currentState('configure')?.value == 'received') {
                logger("checkReceived: Deselecting device ${it?.displayName} [${it.id}].", 'info')
                removalList << it.id
            }
            else {
                logger("checkReceived: Configure command not received by ${it?.displayName} [${it.id}].", 'info')
            }
        }
    }
    if (removalList) state.selectedDevices = state.selectedDevices - removalList
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/
private getSelectedDeviceIds() {
    settings?.configurationPref?.collect { it.id }
}

private logger(message, level = 'debug') {
    if (loggingPref) log."$level" message
}