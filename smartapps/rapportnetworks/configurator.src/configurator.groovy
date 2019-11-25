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
                    name           : 'selectedDevices',
                    title          : 'Select Devices with Configuration capability',
                    type           : 'capability.configuration',
                    multiple       : true,
                    required       : true,
                    submitOnChange : true,
            )
        }

        section {
            paragraph('Select Configuration Profiles for Selected Devices')
            settings?.selectedDevices?.each {
                def commands = configureCommands(it)
                def configured = it?.device?.getDataValue('configuredProfile') ?: 'unknown'
                input(
                        name           : it.id,
                        type           : 'enum',
                        title          : "${it}\n(${it?.typeName})\n${commands ?: '[unknown]'}\n[${configured}]",
                        options        : configureOptions(commands, configured),
                        required       : false,
                )
            }
        }

        section {
            input(
                    name         : 'logging',
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

def getConfigureOptions(configureCommands, configured) {
    configureCommands?.findAll {
        it != configured
        }?.collectEntries {
        command -> [('configure' + command) : command]
        }
}

/*****************************************************************************************************************
 *  SmartThings System Commands:
 *****************************************************************************************************************/
def installed() {
    logger("installed: ${app.label} installed with settings: ${settings}.", 'trace')
    state.selectedDevicesIds = selectedDevicesIds
    logger("installed: selectedDevicesIds - ${state.selectedDevicesIds}", 'debug')
    state.selectedDevicesConfigueCommands = selectedDevicesConfigureCommands
    logger("installed: selectedDevicesConfigueCommands - ${state.selectedDevicesConfigueCommands}", 'debug')
    subscribe(app, handleAppTouch)
    state.sendCounter = 3
}

def updated() {
    logger("updated: ${app.label} updated with settings: ${settings}.", 'trace')
    state.selectedDevicesIds = selectedDevicesIds
    logger("updated: selectedDevicesIds - ${state.selectedDevicesIds}", 'debug')
    state.selectedDevicesConfigueCommands = selectedDevicesConfigureCommands
    logger("updated: selectedDevicesConfigueCommands - ${state.selectedDevicesConfigueCommands}", 'debug')
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
    logger("handleAppTouch: App trigger event: ${event}.", 'trace')
    state.startTime = now() - 200_000
    unsubscribe()
    controller()
}

/*****************************************************************************************************************
 *  Main Commands:
 *****************************************************************************************************************/
def controller() {
    logger("controller: Called. sendCounter = ${state.sendCounter}.", 'trace')
    if (state.sendCounter > 0) {
        checkReceived()
        sendCommand()
        runIn(60, controller)
        state.sendCounter = state.sendCounter - 1
    }

}

def sendCommand() {
    logger('sendCommand: Called.', 'trace')
    settings?.selectedDevices?.each {
        def id = it?.id
        if (id in state?.selectedDevicesConfigueCommands) {
            def command = state?.selectedDevicesConfigueCommands?."${id}"?.value
            logger("sendCommand: Sending Configure command ${command} to device ${id}.", 'info')
            it."configure${command}"()
        }
    }
}

def checkReceived() {
    def removalList = []
    settings?.selectedDevices?.each {
        def id = it?.id
        if (id in state?.selectedDevicesConfigueCommands) {
            def configure = it?.currentState('configure')
            // TODO Need to sort value logic
            if (configure.date.time >= state.startTime && configure.value in ['received', 'queued', 'completed']) {
                logger("checkReceived: Deselecting device ${it?.displayName} [${id}].", 'info')
                removalList << id
            }
            else {
                logger("checkReceived: Configure command not received by ${it?.displayName} [${id}].", 'info')
            }
        }
    }
    if (removalList) {
        removalList.each { removeId ->
            state?.selectedDevicesConfigueCommands.remove(removeId)
        }
    }
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/
private getSelectedDevicesIds() {
    settings?.selectedDevices?.collect { it.id }
}

private getSelectedDevicesConfigureCommands() {
    settings?.findAll {
        it?.key in state.selectedDevicesIds && it?.value != null
    }?.collectEntries {
        [(it?.key) : it?.value]
    }
}

private logger(message, level = 'debug') {
    if (settings?.logging) log."${level}" message
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