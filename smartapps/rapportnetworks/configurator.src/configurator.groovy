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
        name        : 'Configurator v2',
        namespace   : 'rapportnetwork',
        author      : 'Alasdair Thin',
        description : 'Configure SmartThings Devices',
        category    : 'My Apps',
        iconUrl     : 'https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-30DayGoals.png',
        iconX2Url   : 'https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-30DayGoals@2x.png',
        iconX3Url   : 'https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-30DayGoals@2x.png',
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
                    title          : 'Select Devices with Configuration capability',
                    type           : 'capability.configuration',
                    multiple       : true,
                    required       : true,
                    submitOnChange : true,
            )
        }

        section {
            paragraph('Select Configuration Profiles for Selected Devices')
            settings?.configurationPref?.each {
                def configureCommands = getConfigureCommands(it)
                def configured = it?.device?.getDataValue('configuredProfile') ?: 'unknown'
                input(
                        name           : "device-${it.id}",
                        type           : 'enum',
                        title          : "${it}\n(${it?.typeName})\n${configureCommands ?: '[unknown]'}\n[${configured}]",
                        options        : getConfigureCommandsOptions(configureCommands, configured),
                        required       : false,
                )
            }
        }

        /*
        section {
            paragraph(
                    title : 'Details of Selected Devices',
                    "${createSummary(selectedDeviceNames)}"
            )
        }
        */

        section {
            input(
                    name         : 'loggingPref',
                    title        : 'Log Smart App activity',
                    type         : 'bool',
                    defaultValue : false,
                    required     : false,
            )
        }
    }
}

def getConfigureCommands(device) {
    device?.supportedCommands?.findAll {
        it?.name?.matches("configure(.+)")
        }?.collect {
        command -> (command.name - 'configure')
        }
}

def getConfigureCommandsOptions(configureCommands, configured) {
    configureCommands?.findAll {
        it != configured
        }?.collectEntries {
        command -> [('configure' + command) : command]
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

/*
def sendCommand() {
    logger('sendCommand: Called.', 'trace')
    settings?.configurationPref?.each {
        if (it.id in state?.selectedDevices) {
            logger("sendCommand: Sending Configure command to ${it?.displayName} [${it.id}]", 'info')
            it.configure()
        }
    }
}
*/

def sendCommand() {
    logger('sendCommand: Called.', 'trace')
    settings?.configurationPref?.each {
        if (it.id in state?.selectedDevices) { // key
        logger("sendCommand: Sending Configure command ${it?.value} to device ${it?.key}.", 'info')
        "configure"
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
    // settings?.configurationPref?.findResults { it?.hasAttribute('configure') ? it.id : null }
    settings?.findAll {
        it?.key?.matches("device-(.+)") && it?.value != null
        }?.collectEntries {
        [(it?.key - 'device-') : it?.value]
        }
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