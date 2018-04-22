/*****************************************************************************************************************
 *  Copyright Alasdair Thin
 *
 *  Name: InfluxDB Smart Logger
 *
 *  Date: 2018-01-20
 *
 *  Version: 1.1
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
    name: "InfluxDB Smart Logger",
    namespace: "rapportnetworks",
    author: "Alasdair Thin",
    description: "Log SmartThings device states to InfluxDB",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
    )

include 'asynchttp_v1'

preferences {
	page(name: "mainPage")
	page(name: "devicesPage")
	page(name: "attributesPage")
	page(name: "attributeExclusionsPage")
}

def mainPage() {
	dynamicPage(name:"mainPage", uninstall:true, install:true) {

        section("General:") {
            input (
                name: "configLoggingLevelIDE",
                title: "IDE Live Logging Level:\nMessages with this level and higher will be logged to the IDE.",
                type: "enum",
                options: [ "0" : "None", "1" : "Error", "2" : "Warning", "3" : "Info", "4" : "Debug", "5" : "Trace" ],
                defaultValue: "3",
                displayDuringSetup: true,
                required: false
                )
            }

        section ("InfluxDB Database:") {
            input "prefDatabaseHost", "text", title: "Host", defaultValue: "data.sunnd.com", required: true
            input "prefDatabasePort", "text", title: "Port", defaultValue: "443", required: true
            input "prefDatabaseName", "text", title: "Database Name", defaultValue: "rndemo2", required: true
            input "prefDatabaseUser", "text", title: "Username", defaultValue: "thing", required: true
            input "prefDatabasePass", "text", title: "Password", defaultValue: "wildfirepinkdog", required: true
        }

        section("System Monitoring:") {
            input "prefAdjustInactiveTimestamp", "bool", title:"Adjust 'Inactive' status timestamp to compensate for PIR reset time?", defaultValue: true, required: true
            input "prefRoomNameCapture", "bool", title:"Use Virtual Devices to Capture Room Names?", defaultValue: true, required: true
        }

		if (state.devicesConfigured) {
			section("Selected Devices") {
				getPageLink("devicesPageLink", "Tap to change", "devicesPage", null, buildSummary(getSelectedDeviceNames()))
			}
		}
		else {
			getDevicesPageContent()
		}

		if (state.attributesConfigured) {
			section("Selected Events") {
				getPageLink("attributesPageLink", "Tap to change", "attributesPage", null, buildSummary(settings?.allowedAttributes?.sort()))
			}
			section ("Event Device Exclusions") {
				getPageLink("attributeExclusionsPageLink", "Select devices to exclude for specific events.", "attributeExclusionsPage")
			}
		}
		else {
			getAttributesPageContent()
		}

	}
}

def devicesPage() {
	dynamicPage(name:"devicesPage") {
		getDevicesPageContent()
	}
}

private getDevicesPageContent() {
	section("Choose Devices") {
		paragraph "Selecting a device from one of the fields below lets the SmartApp know that the device should be included in the logging process."
		paragraph "Each device only needs to be selected once and which field you select it from has no effect on which events will be logged for it."
		paragraph "There's a field below for every capability, but you should be able to locate most of your devices in either the 'Actuators' or 'Sensors' fields at the top."

		getCapabilities().each {
			try {
				input "${it.cap}Pref", "capability.${it.cap}",
					title: "${it.title}:",
					multiple: true,
					hideWhenEmpty: true,
					required: false,
					submitOnChange: true
			}
			catch (e) {
				logTrace "Failed to create input for ${it}: ${e.message}"
			}
		}

	}
}

def attributesPage() {
	dynamicPage(name:"attributesPage") {
		getAttributesPageContent()
	}
}

private getAttributesPageContent() {
	def supportedAttr = getSupportedAttributes()?.sort()
	if (supportedAttr) {
		section("Choose Events") {
			paragraph "Select all the events that should get logged for all devices that support them."
			paragraph "If the event you want to log isn't shown, verify that you've selected a device that supports it because only supported events are included."
			input "allowedAttributes", "enum",
				title: "Which events should be logged?",
				required: true,
				multiple: true,
				submitOnChange: true,
				options: supportedAttr
		}
	}
	else {
		section("Choose Events") {
			paragraph "You need to select devices before you can choose events."
		}
	}
}

def attributeExclusionsPage() {
	dynamicPage(name:"attributeExclusionsPage") {
		section ("Device Exclusions (Optional)") {
			def startTime = new Date().time
			if (settings?.allowedAttributes) {
				paragraph "If there are some events that should't be logged for specific devices, use the corresponding event fields below to exclude them."
				paragraph "You can also use the fields below to see which devices support each event."
				settings?.allowedAttributes?.sort()?.each { attr ->
					if (startTime && (new Date().time - startTime) > 15000) {
						paragraph "The SmartApp was able to load all the fields within the allowed time.  If the event you're looking for didn't get loaded, select less devices or attributes."
						startTime = null
					}
					else if (startTime) {
						try {
							def attrDevices = getSelectedDevices()?.findAll{ device ->
								device.hasAttribute("${attr}")
							}?.collect { it.id }?.unique()?.sort()
							if (attrDevices) {
								input "${attr}Exclusions", "enum",
									title: "Exclude ${attr} events:",
									required: false,
									multiple: true,
									options: attrDevices
							}
						}
						catch (e) {
							logWarn "Error while getting device exclusion list for attribute ${attr}: ${e.message}"
						}
					}
				}
			}
		}
	}
}

private getPageLink(linkName, linkText, pageName, args=null,desc="",image=null) {
	def map = [
		name: "$linkName",
		title: "$linkText",
		description: "$desc",
		page: "$pageName",
		required: false
	]
	if (args) {
		map.params = args
	}
	if (image) {
		map.image = image
	}
	href(map)
}

private buildSummary(items) {
	def summary = ""
	items?.each {
		summary += summary ? "\n" : ""
		summary += "   ${it}"
	}
	return summary
}

/*****************************************************************************************************************
 *  SmartThings System Commands:
 *****************************************************************************************************************/

def installed() { // runs when the app is first installed
    state.installedAt = now()
    state.loggingLevelIDE = 5
    log.debug "${app.label}: Installed with settings: ${settings}"
    state.installed = true
}

def uninstalled() { // runs when the app is uninstalled
    logger("uninstalled()","trace")
}

def updated() { // runs when app settings are changed
    logger("updated()","trace")

    // Update internal state:
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3

    // Database config:
    state.databaseHost = settings.prefDatabaseHost
    state.databasePort = settings.prefDatabasePort
    state.databaseName = settings.prefDatabaseName
    state.databaseUser = settings.prefDatabaseUser
    state.databasePass = settings.prefDatabasePass

    state.uri = "https://${state.databaseHost}:${state.databasePort}"
    state.path = "/write"
    state.query = [db: "${state.databaseName}", rp: 'autogen', precision: 'ms', u: "${state.databaseUser}", p: "${state.databasePass}"]

    state.headers = [:]
    state.headers.put("HOST", "${state.databaseHost}:${state.databasePort}")
    state.headers.put("Content-Type", "application/x-www-form-urlencoded")
    if (state.databaseUser && state.databasePass) {
        state.headers.put("Authorization", encodeCredentialsBasic(state.databaseUser, state.databasePass))
    }

    state.adjustInactiveTimestamp = settings.prefAdjustInactiveTimestamp
    state.roomNameCapture = settings.prefRoomNameCapture

    state.groupNames = [:] // Initialise map of Group Ids and Group Names
    state.deviceGroup = [:] // Initialise map of Device Ids and Group Names

    state.hubLocationRef = "" // Define state variable to hold location and hub details
    state.hubLocationDetails = ""
    state.hubLocationText = ""
    hubLocationDetails() // generate hub location details

	state.installed = true

    if (settings?.allowedAttributes) {
		state.attributesConfigured = true
	}
	else {
		logDebug "Unconfigured - Choose Events"
	}

	if (getSelectedDevices()) {
		state.devicesConfigured = true
	}
	else {
		logDebug "Unconfigured - Choose Devices"
	}

    // Configure Subscriptions:
    manageSubscriptions()
    manageSchedules()
    runIn(60, softPoll)
    runIn(90, zwaveReport)
}

/*****************************************************************************************************************
 *  Event Handlers:
 *****************************************************************************************************************/

def handleAppTouch(evt) { // handleAppTouch(evt) - used for testing
    logger("handleAppTouch()","trace")
}

def handleStateEvent(evt) {
    def eventType = 'state'
    handleEvent(evt, eventType)
}

def handleValueEvent(evt) {
    def eventType = 'value'
    handleEvent(evt, eventType)
}

def handleThreeAxisEvent(evt) {
    def eventType = 'threeAxis'
    handleEvent(evt, eventType)
}

def handleHubStatus(evt) {
    if (evt.value != 'zb_radio_on' && evt.value != 'zw_radio_on') {
        def eventType = 'hubStatus'
        handleEvent(evt, eventType)
    }
}

def handleDaylight(evt) {
    def eventType = 'daylight'
    handleEvent(evt, eventType)
}

def handleEvent(evt, eventType) {
    logger("handleEvent(): $eventType event $evt.displayName ($evt.name) $evt.value","info")

    def rp = 'autogen' // set retention policy

    def eventTime = evt.date.time // get event time
    def midnight // epoch time at start of day
    def offsetTime // offset to compensate for PIR reset Time
    def writeTime = new Date() // time of processing event

    def measurement
    def deviceName
    def deviceGroup
    def prevEvents
    def prevEvent
    def prevEventTime
    def prevTime
    def prevTimeText

    if (eventType == 'state' || eventType == 'value' || eventType == 'threeAxis') {

        deviceName = (evt?.device.device.name) ? evt.device.device.name : 'unassigned'

        deviceGroup = (evt.device.device?.groupId) ? state?.groupNames?.(evt.device.device.groupId) : 'unassigned'

        // get previous event
        prevEvents = evt.device.statesSince("${evt.name}", writeTime - 7, [max: 3])
        prevEvent = (eventTime > prevEvents[1].date.time) ? prevEvents[1] : prevEvents[2]
        prevEventTime = prevEvent.date.time

        // adjust timestamp of "inactive" status to compensate for PIRresetTime
        if (state.adjustInactiveTimestamp && evt.name == 'motion' && evt?.data) {
            def eventData = parseJson(evt.data)
            if (eventData?.PIRresetTime) {
                offsetTime = 1000 * eventData.PIRresetTime / 2
                if (evt.value == 'inactive') eventTime -= offsetTime
                if (prevEvent.value == 'inactive') prevEventTime -= offsetTime
            }
        }
        prevTime = (eventTime - prevEventTime)
        prevTimeText = timeElapsedText(prevTime)
        midnight = evt.date.clone().clearTime().time // get epoch time at start of day
    }

    // specifically for value events
    def nowValue
    def prevValue
    def change
    def changeText
    def unit // variable for event measurement unit
    def rounding // number of decimal places to round measurement value etc

    def fieldsSB = new StringBuilder() // populate initial fields set

    def description = "${evt?.descriptionText}"
    if (evt.name == 'temperature' && description) description = description.replaceAll('\u00B0', ' ') // remove circle from C unit
    fieldsSB.append('eventDescription="').append(description).append('",')
    fieldsSB.append('eventId="').append(evt.id).append('"')

    // for state events
    if (eventType == 'state') {
        measurement = 'states'

        def states = getAttributeDetail().find { it.key == evt.name }.value.levels // Lookup array for event state levels

        // append current (now:n) state values
        def nStateLevel = states.find { it.key == evt.value }.value
        def nStateBinary = (stateLevel > 0) ? 'true' : 'false'
        // nStateLevel += 'i' // append 'i' for InfluxDB line protocol
        fieldsSB.append(',nBinary=').append(nStateBinary).append(',nLevel=').append(nStateLevel).append('i').append(',nState="').append(evt.value).append('"')
        fieldsSB.append(',nText="').append(state.hubLocationText).append("${evt.displayName} is ${evt.value} in ${deviceGroup}.").append('"')
        // append previous (p) state values
        def prevStateLevel = states.find { it.key == prevEvent.value }.value
        def prevStateBinary = (prevStateLevel > 0) ? 'true' : 'false'
        // prevStateLevel += 'i'
        fieldsSB.append(',pBinary=').append(prevStateBinary).append(',pLevel=').append(prevStateLevel).append('i').append(',pState="').append(prevEvent.value).append('"')
        fieldsSB.append(',pText="').append("This is a change from ${prevEvent.value} ${prevTimeText}.").append('"')
        // calculate time of day in elapsed milliseconds
        fieldsSB.append(',tDay=').append(eventTime - midnight).append('i')
        // append time of previous(p) state values
        fieldsSB.append(',tElapsed=').append(prevTime).append('i').append(',tElapsedText="').append(prevTimeText).append('"')
        fieldsSB.append(',timestamp=').append(eventTime).append('i')
        // append offsetTime for motion sensor
        if (offsetTime) fieldsSB.append(',tOffset=').append(offsetTime).append('i')
        // time of writing event to databaseHost
        fieldsSB.append(',tWrite=').append(writeTime.time).append('i')
        // append time (seconds) weighted value - to facilate calculating mean value
        fieldsSB.append(',wLevel=').append(prevStateLevel * prevTime).append('i')
    }

    // for value events
    else if (eventType == 'value') {
        measurement = 'values'

        unit = (evt?.unit) ? evt.unit : getAttributeDetail().find { it.key == evt.name }.value.unit // set here, but included in tag set

        def trimLength

        if (evt.value.isNumber()) {
            nowValue = evt.floatValue
            prevValue = prevEvent.floatValue
        }
        else {
            trimLength = removeUnit(evt.value)
            def lengthNow = evt.value.length()
            def lengthPrev = prevEvent.value.length()
            nowValue = evt.value.substring(0, lengthNow - trimLength).toFloat()
            prevValue = prevEvent.value.substring(0, lengthPrev - trimLength).toFloat()
        }

        change =  nowValue - prevValue // calculate change from previous value

        rounding = getAttributeDetail().find { it.key == evt.name }?.value.decimalPlaces

        if (rounding > 0) {
            nowValue = nowValue.round(rounding)
            prevValue = prevValue.round(rounding)
            change = change.round(rounding)
        }
        else if (rounding == 0) {
            nowValue = nowValue.round()
            prevValue = prevValue.round()
            change = change.round()
        }

        // get text description of change
        changeText = 'unchanged'
        if (change > 0) changeText = 'increased'
        else if (change < 0) changeText = 'decreased'
        // append current (now:n) event value
        fieldsSB.append(',nText="').append(state.hubLocationText).append("${evt.name} is ${nowValue} ${unit} in ${deviceGroup}.").append('"')
        fieldsSB.append(',nValue=').append(nowValue)
        // append previous(p) event value
        fieldsSB.append(',pText="').append("This is ${changeText}")
        if (changeText != 'unchanged') fieldsSB.append(" by ${Math.abs(change)} ${unit}")
        fieldsSB.append(" compared to ${prevTimeText}.").append('"')
        fieldsSB.append(',pValue=').append(prevValue)
        // append change compared to previous(p) event value
        fieldsSB.append(',rChange=').append(change).append(',rChangeText="').append(changeText).append('"')
        // calculate time of day in elapsed milliseconds
        fieldsSB.append(',tDay=').append(eventTime - midnight).append('i')
        // append time of previous event value
        fieldsSB.append(',tElapsed=').append(prevTime).append('i').append(',tElapsedText="').append(prevTimeText).append('"')
        fieldsSB.append(',timestamp=').append(eventTime).append('i')
        // time of writing event to databaseHost
        fieldsSB.append(',tWrite=').append(writeTime.time).append('i')
        // append time (seconds) weighted value - to facilate calculating mean value
        fieldsSB.append(',wValue=').append(prevValue * prevTime)
    }

    // for theeAxis events
    else if (eventType == 'threeAxis') {
        measurement = 'threeaxes'
        fieldsSB.append(',nText="').append('threeAxis event').append('"')
        unit = 'g'
        def factor = 1024 // convert to g's
        fieldsSB.append(',nValueX=').append(evt.xyzValue.x/factor).append(',nValueY=').append(evt.xyzValue.y/factor).append(',nValueZ=').append(evt.xyzValue.z/factor)
        fieldsSB.append(',timestamp=').append(eventTime).append('i')
        // time of writing event to databaseHost
        fieldsSB.append(',tWrite=').append(writeTime.time).append('i')
    }

    // for hubStatus events
    else if (eventType == 'hubStatus') {
        measurement = 'states'
        def nStateBinary = 'true'
        def nStateLevel = '1i'
        if (evt.value == 'disconnected') {
            nStateBinary = 'false'
            nStateLevel = '-1i'
        }
        fieldsSB.append(',nBinary=').append(nStateBinary).append(',nLevel=').append(nStateLevel).append(',nState="').append(evt.value).append('"')
        fieldsSB.append(',nText="').append(state.hubLocationText).append("hub is ${evt.value}.").append('"')
        fieldsSB.append(',timestamp=').append(eventTime).append('i')
    }

    // for daylight events
    else if (eventType == 'daylight') {
        measurement = 'states'
        if (evt.name == 'sunrise') {
            fieldsSB.append(',nBinary=true,nLevel=1i,nState="Sunrise"').append(',nText="').append("At ${location.name}, building ${location.hubs[0].name}, sun has risen.").append('"')
        }
        else if (evt.name == 'sunset') {
            fieldsSB.append(',nBinary=false,nLevel=-1i,nState="Sunset"').append(',nText="').append("At ${location.name}, building ${location.hubs[0].name}, sun has set.").append('"')
        }
        fieldsSB.append(',timestamp=').append(eventTime).append('i')
    }

    def dataSB = new StringBuilder() // Create InfluxDB line protocol

    dataSB.append(state.hubLocationDetails) // Add hub tags

    if (eventType == 'state' || eventType == 'value' || eventType == 'threeAxis') {
        dataSB.append(',chamber=')
        dataSB.append(deviceGroup.replaceAll(' ', '\\\\ '))
        dataSB.append(',chamberId=')
        dataSB.append( evt.device.device?.groupId ? evt.device.device.groupId : 'unassigned' )

        dataSB.append(',deviceCode=').append(deviceName.replaceAll(' ', '\\\\ ')).append(',deviceId=').append(evt.deviceId).append(',deviceLabel=').append(evt.displayName.replaceAll(' ', '\\\\ '))

        dataSB.append(',event=').append(evt.name)
        dataSB.append(',eventType=').append(eventType) // Add type (state|value|threeAxis) of measurement tag

        dataSB.append(',identifier=').append(deviceGroup.replaceAll(' ', '\\\\ ')).append('\\ .\\ ').append(evt.displayName.replaceAll(' ', '\\\\ ')) // Create composite identifier

        dataSB.append(',isChange=').append(evt?.isStateChange)
    }

    else if (eventType == 'hubStatus' || eventType == 'daylight') {
        dataSB.append(',chamber=').append('House')

        dataSB.append(',deviceLabel=').append( eventType == 'hubStatus' ? 'hub' : 'day' )

        dataSB.append(',event=').append( eventType == 'hubStatus' ? evt.name : 'daylight' )

        dataSB.append(',eventType=state')

        dataSB.append(',identifier=').append('House').append('\\ .\\ ').append(evt.displayName.replaceAll(' ', '\\\\ '))

        dataSB.append(',isChange=').append(evt?.isStateChange)
    }
    dataSB.append(',source=').append(evt.source)

    if (unit) dataSB.append(',unit=').append(unit) // Add unit tag

    dataSB.append(' ').append(fieldsSB).append(' ').append(eventTime) // Add field set and timestamp

    dataSB.insert(0, measurement)
    postToInfluxDB(dataSB.toString(), rp)
}


def softPoll() {
    logger("softPoll()","trace")

    def dataSB = new StringBuilder()
    def rp = 'metadata'
    def now = new Date()

    getSelectedDevices()?.each  { dev ->

        getDeviceAllowedAttrs(dev?.id)?.each { attr ->

            if (dev.latestState(attr)?.value != null) {
                logger("softPoll(): Softpolling device ${dev} for attribute: ${attr}","info")
                dataSB.append('attributes')

                dataSB.append(state.hubLocationDetails) // Add hub tags

                dataSB.append(',chamber=')
                dataSB.append( state?.groupNames.(dev.device?.groupId) ? state.groupNames.(dev.device.groupId).replaceAll(' ', '\\\\ ') : 'unassigned' )
                dataSB.append(',chamberId=')
                dataSB.append( dev.device?.groupId ? dev.device.groupId : 'unassigned' )

                dataSB.append(',deviceCode=').append(dev.name.replaceAll(' ', '\\\\ '))
                dataSB.append(',deviceId=').append(dev.id)
                dataSB.append(',deviceLabel=').append(dev.label.replaceAll(' ', '\\\\ '))
                dataSB.append(',deviceType=').append(dev.typeName.replaceAll(' ', '\\\\ '))

                dataSB.append(',event=').append(attr)
                def type = getAttributeDetail().find { it.key == attr }.value.type
                dataSB.append(',eventType=').append(type)

                if (state?.groupNames.(dev.device?.groupId)) dataSB.append(',identifier=').append(state?.groupNames.(dev.device?.groupId).replaceAll(' ', '\\\\ ')).append('\\ .\\ ').append(dev.label.replaceAll(' ', '\\\\ ')) // Create unique composite identifier

                def daysElapsed = ((now.time - dev.latestState(attr).date.time) / 86_400_000) / 30
                daysElapsed = daysElapsed.toDouble().trunc().round()
                dataSB.append(',timeElapsed=').append(daysElapsed * 30).append('-').append((daysElapsed + 1) * 30).append('days')
                dataSB.append(' ').append('timeLastEvent=').append(dev.latestState(attr).date.time).append('i')
                dataSB.append(',valueLastEvent="').append(dev.latestState(attr).value).append('"')
                dataSB.append('\n')
            }
        }
    }
    postToInfluxDB(dataSB.toString(), rp)
}


def zwaveReport() {
    logger("zwaveReport()","trace")

    def dataSB = new StringBuilder()
    def rp = 'metadata'
    def info

    getSelectedDevices()?.each  { dev ->

        info = dev?.getZwaveInfo().clone()

            if (info.containsKey("zw")) {

                logger("zwaveReport(): zWave report for device ${dev}","info")

                dataSB.append('zwave')

                dataSB.append(state.hubLocationDetails) // Add hub tags

                dataSB.append(',chamber=')
                dataSB.append( state?.groupNames.(dev.device?.groupId) ? state.groupNames.(dev.device.groupId).replaceAll(' ', '\\\\ ') : 'unassigned' )
                dataSB.append(',chamberId=')
                dataSB.append( dev.device?.groupId ? dev.device.groupId : 'unassigned' )

                dataSB.append(',deviceCode=').append(dev.name.replaceAll(' ', '\\\\ '))
                dataSB.append(',deviceId=').append(dev.id)
                dataSB.append(',deviceLabel=').append(dev.label.replaceAll(' ', '\\\\ '))
                dataSB.append(',deviceType=').append(dev.typeName.replaceAll(' ', '\\\\ '))

                if (state.groupNames.(dev?.device.groupId)) dataSB.append(',identifier=').append(state.groupNames.(dev.device.groupId).replaceAll(' ', '\\\\ ')).append('\\ .\\ ').append(dev.label.replaceAll(' ', '\\\\ ')) // Create unique composite identifier

                dataSB.append(',type=zwave')

                def power = info.zw.take(1)
                switch(power) {
                    case "L":
                        power = 'Listening'
                    break
                    case "S":
                        power = 'Sleepy'
                    break
                    case "B":
                        power = 'Beamable'
                    break
                }
                def secure = (info.zw.endsWith("s")) ? 'true' : 'false'
                def cc = info.cc
                if (info?.sec) cc.addAll(info.sec)
                def ccSec = 'zz' + cc.sort().join("=true,zz") + '=true'
                info.remove('cc')
                info.remove('sec')
                info.remove('zw')
                info = info.sort()
                def toKeyValue = { it.collect { /$it.key="$it.value"/ } join "," }
                info = toKeyValue(info) + ',' + "${ccSec}"
                dataSB.append(',power=').append("${power}").append(',secure=').append("${secure}") // set as tag values to enable filtering
                dataSB.append(' ')
                if (dev?.device.getDataValue("configuredParameters")) dataSB.append(dev.device.getDataValue("configuredParameters")).append(',')
                dataSB.append(info)
                dataSB.append('\n')
            }
        }
    postToInfluxDB(dataSB.toString(), rp)
}

// converted elapsed time to textual description
def timeElapsedText(time) {
    def phrase
    time = time / 1000
    if (time < 60) phrase = Math.round(time / 1) + ' seconds previously'
    else if (time < 90) phrase = Math.round(time / 60) + ' minute previously'
    else if (time < 3600) phrase = Math.round(time / 60) + ' minutes previously'
    else if (time < 5400) phrase = Math.round(time / 3600) + ' hour previously'
    else if (time < 86400) phrase = Math.round(time / 3600) + ' hours previously'
    else if (time < 129600) phrase = Math.round(time / 86400) + ' day previously'
    else phrase = Math.round(time / 86400) + ' days previously'
    return phrase
}

// remove any units appending to end of event value
def removeUnit(stringUnit) {
    def valueString = stringUnit
    def length = valueString.length()
    def i = 0
    while (i < length) {
        if (valueString.substring(0, length - i).isNumber()) break
        i++
    }
    return i
}

/*****************************************************************************************************************
 *  Main Commands:
 *****************************************************************************************************************/

def hubLocationDetails() {
    def hubLocationRefSB = new StringBuilder()
    hubLocationRefSB.append(location.name.replaceAll(' ', '\\\\ ')).append('.').append(location.hubs[0].name.replaceAll(' ', '\\\\ '))
    state.hubLocationRef = hubLocationRefSB.toString()

    def hubLocationDetailsSB = new StringBuilder()
    hubLocationDetailsSB.append(',area=').append(location.name.replaceAll(' ', '\\\\ ')).append(',areaId=').append(location.id)
    hubLocationDetailsSB.append(',building=').append(location.hubs[0].name.replaceAll(' ', '\\\\ ')).append(',buildingId=').append(location.hubs[0].id)
    state.hubLocationDetails = hubLocationDetailsSB.toString()

    def hubLocationTextSB = new StringBuilder()
    hubLocationTextSB.append('At ').append(location.name).append(', in building ').append(location.hubs[0].name).append(', ')
    state.hubLocationText = hubLocationTextSB.toString()
}

def postToInfluxDB(data, rp) {
// need to update hubAction state variables and rewrite the hubAction function
    if (state.databaseHost.take(3) == "192") {
        try {
            def hubAction = new physicalgraph.device.HubAction([
                method: "POST",
                path: state.path,
                body: data,
                headers: state.headers
            ],
            null,
            [ callback: handleInfluxResponse ]
            )

        sendHubCommand(hubAction)
        }
        catch (Exception e) {
        logger("postToInfluxDB(): Exception ${e} on ${hubAction}","error")
        }
    }

    else {
        def query = state.query.clone()
        query.rp = rp
        def params = [
            uri: state.uri,
            path: state.path,
            query: query,
            contentType: "application/x-www-form-urlencoded",
            requestContentType: "application/x-www-form-urlencoded",
            body: data
            ]
        logger("postToInfluxDB(): Posting data to InfluxDB: Uri: ${state.uri}, Path: ${state.path}, Query: ${query}, Data: ${data}","info")
        asynchttp_v1.post(handleInfluxResponse, params)
    }
}

def handleInfluxResponse(response, requestdata) {
    def status = response.status
    if (status == 204) {
        logger("postToInfluxDB(): Success! Response from InfluxDB: Status: ${status}, Headers: ${response.headers}, Body: ${response.data}","trace")
    }
    if (status >= 400) {
        logger("postToInfluxDB(): Something went wrong! Response from InfluxDB: Status: ${status}, Headers: ${response.headers}, Body: ${response.data}","error")
    }
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/

private manageSchedules() {
    logger("manageSchedules()","trace")

    try { unschedule(hubLocationDetails) }
    catch(e) { logger("manageSchedules(): Unschedule hubLocationDetails failed!","error") }
    runEvery3Hours(hubLocationDetails)

    try { unschedule(softPoll) }
    catch(e) { logger("manageSchedules(): Unschedule softPoll failed!","error") }
    runEvery3Hours(softPoll)

    try { unschedule(zwaveReport) }
    catch(e) { logger("manageSchedules(): Unschedule zwaveReport failed!","error") }
    runEvery3Hours(zwaveReport)
}

private manageSubscriptions() { // Configures subscriptions
    logger("manageSubscriptions()","trace")
    unsubscribe()

    getSelectedDevices()?.each  { dev ->

        if (!dev.displayName.startsWith("~")) {

        getDeviceAllowedAttrs(dev?.id)?.each { attr ->

            if (dev?.hasAttribute("${attr}")) { // select only attributes that exist

                def type = getAttributeDetail().find { it.key == attr }.value.type

                    if (type == 'state') {
                        logger("manageSubscriptions(): Subscribing 'handleStateEvent' to attribute: ${attr}, for device: ${dev}","info")
                        subscribe(dev, attr, handleStateEvent)
                    }
                    else if (type == 'value') {
                        logger("manageSubscriptions(): Subscribing 'handleValueEvent' to attribute: ${attr}, for device: ${dev}","info")
                        subscribe(dev, attr, handleValueEvent)
                    }
                    else if (type == 'threeAxis') {
                        logger("manageSubscriptions(): Subscribing 'handleThreeAxisEvent' to attribute: ${attr}, for device: ${dev}","info")
                        subscribe(dev, attr, handleThreeAxisEvent)
                    }
                }
            }
        }
    }

    // subscribe to Sunrise and Sunset events
    subscribe(location, "sunrise", handleDaylight)
    subscribe(location, "sunset", handleDaylight)
    logger("manageSubscriptions(): Subscribing to 'Sunrise' and 'Sunset' events","info")

    // subscribe to Hub status
    def hub = location.hubs[0]
    subscribe(hub, "hubStatus", handleHubStatus)
    logger("manageSubscriptions(): Subscribing to 'Hub Status' events","info")

    // build state maps of group Ids and group names
    if (state.roomNameCapture) {
        def groupId
        def groupName
        settings.bridgePref.each {
            groupId = it.device?.groupId
            groupName = it.name?.drop(1)
            if (groupId) state.groupNames << [(groupId): groupName]
        }

        def deviceId
        def deviceName
        def deviceGroupId
        def deviceGroup
        getSelectedDevices()?.each  {
            deviceId = it.id
            deviceName = it.name
            deviceGroupId = it.device?.groupId
            deviceGroup = state.groupNames?."${deviceGroupId}"
            if (deviceGroupId) state.deviceGroup << [(deviceId): [deviceName: deviceName, deviceGroup: deviceGroup, deviceGroupId: deviceGroupId]]
            else state.deviceGroup << [(deviceId): [deviceName: deviceName, deviceGroup: 'unassigned', deviceGroupId: 'unassigned']]
        }
    }
}

private logger(msg, level = "debug") { // Wrapper function for all logging
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

// Encode credentials for HTTP Basic authentication.
private encodeCredentialsBasic(username, password) {
    return "Basic " + "${username}:${password}".encodeAsBase64().toString()
}

private getDeviceAllowedAttrs(deviceName) {
	def deviceAllowedAttrs = []
	try {
		settings?.allowedAttributes?.each { attr ->
			try {
				def attrExcludedDevices = settings?."${attr}Exclusions"
				if (!attrExcludedDevices?.find { it?.toLowerCase() == deviceName?.toLowerCase() }) {
					deviceAllowedAttrs << "${attr}"
				}
			}
			catch (e) {
				logWarn "Error while getting device allowed attributes for ${device?.displayName} and attribute ${attr}: ${e.message}"
			}
		}
	}
	catch (e) {
		logWarn "Error while getting device allowed attributes for ${device.displayName}: ${e.message}"
	}
	return deviceAllowedAttrs
}

private getSupportedAttributes() {
	def supportedAttributes = []
	def devices = getSelectedDevices()
	if (devices) {
		getAllAttributes()?.each { attr ->
			try {
				if (devices?.find { it?.hasAttribute("${attr}") }) {
					supportedAttributes << "${attr}"
				}
			}
			catch (e) {
				logWarn "Error while finding supported devices for ${attr}: ${e.message}"
			}
		}
	}
	return supportedAttributes?.unique()?.sort()
}

private getAllAttributes() {
	def attributes = []
	getCapabilities().each { cap ->
		try {
			if (cap?.attr) {
				if (cap.attr instanceof Collection) {
					cap.attr.each { attr ->
						attributes << "${attr}"
					}
				}
				else {
					attributes << "${cap?.attr}"
				}
			}
		}
		catch (e) {
			logWarn "Error while getting attributes for capability ${cap}: ${e.message}"
		}
	}
	return attributes
}

private getSelectedDeviceNames() {
	try {
		return getSelectedDevices()?.collect { it?.displayName }?.sort()
	}
	catch (e) {
		logWarn "Error while getting selected device names: ${e.message}"
		return []
	}
}

private getSelectedDevices() {
	def devices = []
	getCapabilities()?.each {
		try {
			if (settings?."${it.cap}Pref") {
				devices << settings?."${it.cap}Pref"
			}
		}
		catch (e) {
			logWarn "Error while getting selected devices for capability ${it}: ${e.message}"
		}
	}
	return devices?.flatten()?.unique { it.id }
}

private getCapabilities() { [
		[title: "Actuators", cap: "actuator"],
		[title: "Sensors", cap: "sensor"],
        [title: "Room Name Virtual Devices", cap: "bridge"], // added in
		[title: "Acceleration Sensors", cap: "accelerationSensor", attr: "acceleration"],
		[title: "Alarms", cap: "alarm", attr: "alarm"],
		[title: "Batteries", cap: "battery", attr: "battery"],
		[title: "Beacons", cap: "beacon", attr: "presence"],
		[title: "Bulbs", cap: "bulb", attr: "switch"],
		[title: "Buttons", cap: "button", attr: ["button", "numberOfButtons"]],
		[title: "Carbon Dioxide Measurement Sensors", cap: "carbonDioxideMeasurement", attr: "carbonDioxide"],
		[title: "Carbon Monoxide Detectors", cap: "carbonMonoxideDetector", attr: "carbonMonoxide"],
		[title: "Color Control Devices", cap: "colorControl", attr: ["color", "hue", "saturation"]],
		[title: "Color Temperature Devices", cap: "colorTemperature", attr: "colorTemperature"],
		[title: "Consumable Devices", cap: "consumable", attr: "consumableStatus"],
		[title: "Contact Sensors", cap: "contactSensor", attr: "contact"],
		[title: "Doors", cap: "doorControl", attr: "door"],
		[title: "Energy Meters", cap: "energyMeter", attr: "energy"],
		[title: "Garage Doors", cap: "garageDoorControl", attr: "door"],
		[title: "Illuminance Measurement Sensors", cap: "illuminanceMeasurement", attr: "illuminance"],
		[title: "Image Capture Devices", cap: "imageCapture", attr: "image"],
		[title: "Indicators", cap: "indicator", attr: "indicatorStatus"],
		[title: "Lights", cap: "light", attr: "switch"],
		[title: "Locks", cap: "lock", attr: "lock"],
		[title: "Media Controllers", cap: "mediaController", attr: "currentActivity"],
		[title: "Motion Sensors", cap: "motionSensor", attr: "motion"],
		[title: "Music Players", cap: "musicPlayer", attr: ["level", "mute", "status", "trackDescription"]],
		[title: "Outlets", cap: "outlet", attr: "switch"],
		[title: "pH Measurement Sensors", cap: "phMeasurement", attr: "pH"],
		[title: "Power Meters", cap: "powerMeter", attr: "power"],
		[title: "Power Sources", cap: "powerSource", attr: "powerSource"],
		[title: "Presence Sensors", cap: "presenceSensor", attr: "presence"],
		[title: "Relative Humidity Measurement Sensors", cap: "relativeHumidityMeasurement", attr: "humidity"],
		[title: "Relay Switches", cap: "relaySwitch", attr: "switch"],
		[title: "Shock Sensors", cap: "shockSensor", attr: "shock"],
		[title: "Signal Strength Sensors", cap: "signalStrength", attr: ["lqi", "rssi"]],
		[title: "Sleep Sensors", cap: "sleepSensor", attr: "sleeping"],
		[title: "Smoke Detectors", cap: "smokeDetector", attr: "smoke"],
		[title: "Sound Pressure Level Sensors", cap: "soundPressureLevel", attr: "soundPressureLevel"],
		[title: "Sound Sensors", cap: "soundSensor", attr: "sound"],
		[title: "Speech Recognition Sensors", cap: "speechRecognition", attr: "phraseSpoken"],
		[title: "Switches", cap: "switch", attr: "switch"],
		[title: "Switch Level Sensors", cap: "switchLevel", attr: "level"],
		[title: "Tamper Alert Sensors", cap: "tamperAlert", attr: "tamper"],
		[title: "Temperature Measurement Sensors", cap: "temperatureMeasurement", attr: "temperature"],
		[title: "Thermostats", cap: "thermostat", attr: ["coolingSetpoint", "heatingSetpoint", "temperature", "thermostatFanMode", "thermostatMode", "thermostatOperatingState", "thermostatSetpoint"]],
		[title: "Three Axis Sensors", cap: "threeAxis", attr: "threeAxis"],
		[title: "Touch Sensors", cap: "touchSensor", attr: "touch"],
		[title: "Ultraviolet Index Sensors", cap: "ultravioletIndex", attr: "ultravioletIndex"],
		[title: "Valves", cap: "valve", attr: "valve"],
		[title: "Voltage Measurement Sensors", cap: "voltageMeasurement", attr: "voltage"],
		[title: "Water Sensors", cap: "waterSensor", attr: "water"],
		[title: "Window Shades", cap: "windowShade", attr: "windowShade"]
] }

private getAttributeDetail() { [
    acceleration: [type: 'state', levels: [inactive: -1, active: 1]],
    alarm: [type: 'state', levels: [off: -1, siren: 1, strobe: 2, both: 3]],
    battery: [type: 'value', decimalPlaces: 0, unit: '%'],
    button: [type: 'state', levels: [pushed: 1, held: 2]],
    carbonDioxide: [type: 'value', decimalPlaces: 0, unit: ''],
    carbonMonoxide: [type: 'state', levels: [clear: -1, detected: 1, tested: 4]],
    color: [type: 'value', decimalPlaces: 0, unit: ''],
    consumableStatus: [type: 'state', levels: [replace: -1, good: 1, order: 3, 'maintenance required': 4, missing: 5]],
    contact: [type: 'state', levels: [closed: -1, open: 1, full: -1, flushing: 1]],
    coolingSetpoint: [type: 'state'],
    current: [type: 'value', decimalPlaces: 1, unit: 'A'],
    door: [type: 'state', levels: [closed: -1, open: 1, opening: 2, closing: -2, unknown: 5]],
    energy: [type: 'value', decimalPlaces: 2, unit: 'kWh'],
    goal: [type: 'value', decimalPlaces: 0, unit: ''],
    heatingSetpoint: [type: 'state'],
    hue: [type: 'value', decimalPlaces: 0, unit: ''],
    humidity: [type: 'value', decimalPlaces: 0, unit: '%'],
    illuminance: [type: 'value', decimalPlaces: 0, unit: 'lux'],
    level: [type: 'value', decimalPlaces: 0, unit: ''],
    lock: [type: 'state', levels: [locked: -1, unlocked: 1, 'unlocked with timeout': 2, unknown: 5]],
    lqi: [type: 'value', decimalPlaces: 2, unit: ''],
    motion: [type: 'state', levels: [inactive: -1, active: 1]],
    mute: [type: 'state', levels: [muted: -1, unmuted: 1]],
    optimisation: [type: 'state', levels: [inactive: -1, active: 1]],
    pH: [type: 'value', decimalPlaces: 1, unit: ''],
    power: [type: 'value', decimalPlaces: 1, unit: 'W'],
    powerFactor: [type: 'value', decimalPlaces: 2, unit: ''],
    powerSource: [type: 'state', levels: [mains: -1, battery: 1, dc: 2,unknown: 5]], // added in
    presence: [type: 'state', levels: ['not present': -1, present: 1]],
    rssi: [type: 'value', decimalPlaces: 2, unit: ''],
    saturation: [type: 'value', decimalPlaces: 0, unit: ''],
    scheduledSetpoint: [type: 'state'],
    shock: [type: 'state', levels: [clear: -1, detected: 1]],
    sleeping: [type: 'state', levels: [sleeping: -1, 'not sleeping': 1]],
    smoke: [type: 'state', levels: [clear: -1, detected: 1, tested: 4]],
    sound: [type: 'state', levels: ['not detected': -1, detected: 1]],
    soundPressureLevel: [type: 'value', decimalPlaces: 2, unit: ''],
    status: [type: 'state'],
    steps: [type: 'value', decimalPlaces: 0, unit: ''],
    switch: [type: 'state', levels: [off: -1, on: 1]],
    tamper: [type: 'state', levels: [clear: -1, detected: 1]],
    temperature: [type: 'value', decimalPlaces: 1, unit: 'C'],
    thermostatFanMode: [type: 'state', levels: [auto: -1, on: 1, circulate: 2]],
    thermostatMode: [type: 'state', levels: [off: -1, heat: 1, 'emergency heat': 2, auto: 3, cool: -3]],
    thermostatOperatingState: [type: 'state', levels: [idle: -1, heating: 1, 'pending heat': 2, 'fan only': 3, cooling: -1, 'pending cool': -2]],
    thermostatSetpoint: [type: 'state'],
    thermostatSetpointMode: [type: 'state'],
    threeAxis: [type: 'threeAxis', decimalPlaces: 2, unit: ''],
    touch: [type: 'state', levels: [touched: 1]],
    trackData: [type: 'state'],
    trackDescription: [type: 'state'],
    ultravioletIndex: [type: 'value', decimalPlaces: 0, unit: ''],
    voltage: [type: 'value', decimalPlaces: 1, unit: 'V'],
    water: [type: 'state', levels: [dry: -1, wet: 1]],
    windowFunction: [type: 'state'],
    windowShade: [type: 'state', levels: [closed: -1, opening: 1, 'partially open': 2, open: 3, closing: -2, unknown: 5]]
] }