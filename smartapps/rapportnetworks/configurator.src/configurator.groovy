

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