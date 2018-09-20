class physicalgraph.zwave.commands.versionv1.VersionCommandClassGet
{
Short	requestedCommandClass
List<Short>	payload

String format()
}

(requestedCommandClass: 0x20)  - to send


class physicalgraph.zwave.commands.versionv1.VersionCommandClassReport
{
Short	commandClassVersion
Short	requestedCommandClass
List<Short>	payload

String format()
}

? convert to hex ? - try and see!




def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	log.debug "ConfigurationReport: $cmd"
	def nparam = "${cmd.parameterNumber}"
	def nvalue = "${cmd.scaledConfigurationValue}"
	log.debug "Processing Configuration Report: (Parameter: $nparam, Value: $nvalue)"
	def cP = [:]
	cP = state.configuredParameters
	cP.put("${nparam}", "${nvalue}")
	def cPReport = cP.collectEntries { key, value -> [key.padLeft(3,"0"), value] }
    cPReport = cPReport.sort()
    def toKeyValue = { it.collect { /$it.key=$it.value/ } join "," }
    cPReport = toKeyValue(cPReport)
	updateDataValue("configuredParameters", cPReport)
	state.configuredParameters = cP
}



class physicalgraph.zwave.commands.configurationv1.ConfigurationSet
{
List<Short>	configurationValue
Boolean	defaultValue
Short	parameterNumber
BigInteger	scaledConfigurationValue
Short	size
List<Short>	payload

String format()
}




zwave.configurationV1.configurationSet(parameterNumber: 2, size: 1, scaledConfigurationValue: 1)
