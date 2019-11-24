/*****************************************************************************************************************
 *  Copyright Alasdair Thin
 *
 *  Name: Configurator
 *
 *  Date: 2019-11-23
 *
 *  Version: 2-0-0
 *
 *  Author: Alasdair Thin
 *
 *  Description: A SmartApp to Send Configure Command to selected SmartThings Devices with the Configuration Capability.
 *
 *****************************************************************************************************************/

definition (
        name        : 'Configurator',
        namespace   : 'rapportnetwork',
        author      : 'Alasdair Thin',
        description : 'Configure SmartThings Devices',
        category    : 'My Apps',
        iconUrl     : 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png',
        iconX2Url   : 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png',
        iconX3Url   : 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png',
)

preferences {
    page(
            name      : 'mainPage',
            install   : true,
            uninstall : true,
    )

}

def mainPage() {
    dynamicPage(name : 'mainPage') {
        section {
            input(
                    name           : 'configurationPref',
                    title          : 'Select Devices with Configuration capability:',
                    type           : 'capability.configuration',
                    multiple       : true,
                    required       : false,
                    submitOnChange : true,
            )
        }

        section {
            paragraph('Select Configuration Profiles for Devices')
            settings?.configurationPref?.each {
                input(
                        // TODO see about pulling metadata in from device to be able to distinguish between devices with the same name
                        // TODO rework as a paragraph for each item - use dynamic methods to get info
                        name           : "profile-${it}",
                        type           : 'enum',
                        title          : "${it}\n(${it?.typeName})\n[${it?.device?.getDataValue('configuredProfile') ?: 'unknown'}]",
                        options        : getConfigureCommands(it),
                        required       : false,
                )
            }
        }

        section {
            paragraph(
                    title : 'Details of Selected Devices',
                    "${createSummary(selectedDeviceNames)}"
            )
        }
        section {
            input(
                    name         : 'loggingPref',
                    title        : 'Log Smart App activity.',
                    type         : 'bool',
                    defaultValue : false,
                    required     : false,
            )
        }
    }
}

def getConfigureCommands(device) {
    device?.supportedCommands?.findAll {
        command -> command?.name?.matches("configure(.+)") // .startsWith('configure')
        }?.collectEntries {
        configureCommand -> [(configureCommand.name) : (configureCommand.name - 'configure')]
        }
}

private getSelectedDeviceNames() {
    settings?.configurationPref?.collect {
        def items
        if (it?.currentState('syncPending')?.date?.time > it?.currentState('configure')?.date?.time) {
            items = it.currentState('syncPending')?.value
        }
        else {
            items = '?' // TODO Show configured profile instead
        }

        // "${it?.displayName}(${it?.getZwaveInfo()?.zw?.take(1)}) > ${it?.currentState('configure')?.date?.format('yyyy/MM/dd-HH:mm')} [$items]"
        // GString
        // "${it?.displayName} (${it?.getZwaveInfo()?.zw?.take(1)}) > ${it?.currentState('configure')?.date?.format('yyyy/MM/dd-HH:mm')} [${items}] ${it?.supportedCommands.findAll { command -> command.name.startsWith('configure') } }"
        def name = it?.displayName
        def profile = settings?."profile-${name}"
        "${name} (${it?.getZwaveInfo()?.zw?.take(1)}) > ${it?.currentState('configure')?.date?.format('yyyy/MM/dd-HH:mm')} [${items}] -> ${profile}"
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
    state.selectedDevices = selectedDeviceIds
    subscribe(app, handleAppTouch)
    state.sendCounter = 3
}

def updated() {
    logger("updated: ${app.label} updated with settings: ${settings}.", 'trace')
    state.selectedDevices = selectedDeviceIds
    subscribe(app, handleAppTouch)
    state.sendCounter = 3
}

def uninstalled() {
    logger("uninstalled: ${app.label} uninstalled.", 'trace')
}

/*****************************************************************************************************************
 *  Event Handlers:
 *****************************************************************************************************************/
def handleAppTouch(event) { // SmartApp Touch event
    logger("handleAppTouch: App trigger event: $event", 'trace')
    state.startTime = now() - 200_000
    unsubscribe()
    controller()
}

/*****************************************************************************************************************
 *  Main Commands:
 *****************************************************************************************************************/
def controller() {
    logger("controller: Called. sendCounter = $state.sendCounter", 'trace')
    if (state.sendCounter > 0) {
        checkReceived()
        sendCommand()
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

def checkReceived() { // need to put a time limit on checking currentState - so that isn't long time ago
    def removalList = []
    settings?.configurationPref?.each {
        if (it.id in state?.selectedDevices) {
            def configure = it?.currentState('configure')
            if (configure.value in ['received', 'queued', 'completed'] && configure.date.time >= state.startTime) {
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
    settings?.configurationPref?.findResults { it?.hasAttribute('configure') ? it.id : null }
}

private logger(message, level = 'debug') {
    if (loggingPref) log."${level}" message
}

/*
def configureCommand() {
    getSelectedDevices()?.each  { dev ->
        if (!dev.displayName.startsWith("~")) {
            if (dev.hasCapability("Configuration")) {
                log.debug "${device.deviceLabel} has Configuration Capability"
                if (dev.hasCommand("configure")) {
                    log.debug "${device.deviceLabel} has configure Command"
                    dev.configure()
                    log.debug "configure Command sent to ${device.deviceLabel}"
                }
                else {
                    log.debug "${device.deviceLabel} does not have configure Command"
                }
            }
            else {
                log.debug "${device.deviceLabel} does not have Configuration Capability"
            }
        }
    }
}
*/