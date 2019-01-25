/*****************************************************************************************************************
 *  Copyright Alasdair Thin
 *
 *  Name: InfluxDB Smart Logger
 *
 *  Date: 2019-01-14
 *
 *  Version: 2.0
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

definition(
        name: 'InfluxDB Smart Logger V2',
        namespace: 'rapportnetworks',
        author: 'Alasdair Thin',
        description: 'Log SmartThings device states to InfluxDB',
        category: 'My Apps',
        iconUrl: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png',
        iconX2Url: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png',
        iconX3Url: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png'
)

include 'asynchttp_v1'

preferences {
    page(name: 'mainPage')
    page(name: 'devicesPage')
    page(name: 'attributesPage')
    page(name: 'attributeExclusionsPage')
}

def mainPage() {
    dynamicPage(name: 'mainPage', uninstall: true, install: true) {
        section('General:') {
            input(
                    name: 'configLoggingLevelIDE',
                    title: "IDE Live Logging Level:\nMessages with this level and higher will be logged to the IDE.",
                    type: 'enum',
                    options: [0: 'None', 1: 'Error', '2': 'Warning', '3': 'Info', '4': 'Debug', '5': 'Trace'],
                    defaultValue: '3',
                    displayDuringSetup: true,
                    required: false
            )
        }
        section('InfluxDB Database:') {
            input(
                    name: 'prefDatabaseHost',
                    type: 'text',
                    title: 'Host',
                    defaultValue: 'data.sunnd.com',
                    required: true
            )
            input(
                    name: 'prefDatabasePort',
                    type: 'text',
                    title: 'Port',
                    defaultValue: '443',
                    required: true
            )
            input(
                    name: 'prefDatabaseName',
                    type: 'text',
                    title: 'Database Name',
                    defaultValue: '*',
                    required: true
            )
            input(
                    name: 'prefDatabaseUser',
                    type: 'text',
                    title: 'Username',
                    defaultValue: '*',
                    required: true
            )
            input(
                    name: 'prefDatabasePass',
                    type: 'text',
                    title: 'Password',
                    defaultValue: '*',
                    required: true
            )
        }
        section('System Monitoring:') {
            input(
                    name: 'prefAdjustInactiveTimestamp',
                    type: 'bool',
                    title: 'Adjust Inactive status timestamp to compensate for PIR reset time?',
                    defaultValue: true,
                    required: true
            )
            input(
                    name: 'prefRoomNameCapture',
                    type: 'bool',
                    title: 'Use Virtual Devices to Capture Room Names?',
                    defaultValue: true,
                    required: true
            )
        }

        if (state.devicesConfigured) {
            section('Selected Devices') {
                getPageLink('devicesPageLink', 'Tap to change', 'devicesPage', null, buildSummary(getSelectedDeviceNames()))
            }
        } else {
            getDevicesPageContent()
        }

        if (state.attributesConfigured) {
            section('Selected Events') {
                getPageLink('attributesPageLink', 'Tap to change', 'attributesPage', null, buildSummary(settings?.allowedAttributes?.sort()))
            }
            section('Event Device Exclusions') {
                getPageLink('attributeExclusionsPageLink', 'Select devices to exclude for specific events.', 'attributeExclusionsPage')
            }
        } else {
            getattributesPageContent()
        }

    }
}

def devicesPage() {
    dynamicPage(name: 'devicesPage') {
        getDevicesPageContent()
    }
}

private getDevicesPageContent() {
    section("Choose Devices") {
        paragraph(
                "Selecting a device from one of the fields below lets the SmartApp know that the device should be included in the logging process.\nEach device only needs to be selected once and which field you select it from has no effect on which events will be logged for it.\nThere's a field below for every capability, but you should be able to locate most of your devices in either the Actuators or Sensors fields at the top."
        )

        getCapabilities().each {
            try {
                input(
                        "${it.cap}Pref", "capability.${it.cap}",
                        title: "${it.title}:",
                        multiple: true,
                        hideWhenEmpty: true,
                        required: false,
                        submitOnChange: true
                )
            }
            catch (e) {
                logger("Failed to create input for ${it}: ${e.message}", 'trace')
            }
        }

    }
}

def attributesPage() {
    dynamicPage(name: 'attributesPage') {
        getAttributesPageContent()
    }
}

private getAttributesPageContent() {
    def supportedAttr = getSupportedAttributes()?.sort()
    if (supportedAttr) {
        section('Choose Events') {
            paragraph(
                    "Select all the events that should get logged for all devices that support them.\nIf the event you want to log isn't shown, verify that you've selected a device that supports it because only supported events are included."
            )
            input(
                    name: 'allowedAttributes',
                    type: 'enum',
                    title: "Which events should be logged?",
                    required: true,
                    multiple: true,
                    submitOnChange: true,
                    options: supportedAttr
            )
        }
    } else {
        section('Choose Events') {
            paragraph(
                    'You need to select devices before you can choose events.'
            )
        }
    }
}

def attributeExclusionsPage() {
    dynamicPage(name: 'attributeExclusionsPage') {
        section('Device Exclusions (Optional)') {
            def startTime = new Date().time
            if (settings?.allowedAttributes) {
                paragraph(
                        "If there are some events that should't be logged for specific devices, use the corresponding event fields below to exclude them.\nYou can also use the fields below to see which devices support each event."
                )
                settings?.allowedAttributes?.sort()?.each { attr ->
                    if (startTime && (new Date().time - startTime) > 15000) {
                        paragraph(
                                "The SmartApp was able to load all the fields within the allowed time.  If the event you're looking for didn't get loaded, select less devices or attributes."
                        )
                        startTime = null
                    } else if (startTime) {
                        try {
                            def attrDevices = getSelectedDevices()?.findAll { device ->
                                device.hasAttribute("${attr}")
                            }?.collect { it.id }?.unique()?.sort()
                            if (attrDevices) {
                                input(
                                        name: "${attr}Exclusions",
                                        type: "enum",
                                        title: "Exclude ${attr} events:",
                                        required: false,
                                        multiple: true,
                                        options: attrDevices
                                )
                            }
                        }
                        catch (e) {
                            logger("Error while getting device exclusion list for attribute ${attr}: ${e.message}", 'warn')
                        }
                    }
                }
            }
        }
    }
}

private getPageLink(linkName, linkText, pageName, args = null, desc = "", image = null) {
    def map = [
            name       : "$linkName",
            title      : "$linkText",
            description: "$desc",
            page       : "$pageName",
            required   : false
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
    def summary = ''
    items?.each {
        summary += summary ? '\n' : ''
        summary += "   ${it}"
    }
    summary
}

/*****************************************************************************************************************
 *  SmartThings System Commands:
 *****************************************************************************************************************/

def installed() { // runs when the app is first installed
    state.installedAt = now()
    state.loggingLevelIDE = 5
    logger("${app.label}: Installed with settings: ${settings}", 'trace')
    state.installed = true
}

def uninstalled() { // runs when the app is uninstalled
    logger("uninstalled()", 'trace')
}

def updated() { // runs when app settings are changed
    logger("updated()", 'trace')

    // Update internal state:
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3

    // Database config:
    state.databaseHost = settings.prefDatabaseHost
    state.databasePort = settings.prefDatabasePort
    state.databaseName = settings.prefDatabaseName
    state.databaseUser = settings.prefDatabaseUser
    state.databasePass = settings.prefDatabasePass

    state.headers = [HOST: "${state.databaseHost}:${state.databasePort}", "Content-Type": "application/x-www-form-urlencoded"]
    state.uri = "https://${state.databaseHost}:${state.databasePort}"
    state.path = "/write"
    state.query = [db: "${state.databaseName}", rp: 'autogen', precision: 'ms', u: "${state.databaseUser}", p: "${state.databasePass}"]

    state.adjustInactiveTimestamp = settings.prefAdjustInactiveTimestamp
    state.roomNameCapture = settings.prefRoomNameCapture

    state.groupNames = [:] // Initialise map of Group Ids and Group Names
    state.hubLocationDetails = "" // Define state variable to hold location and hub details
    state.hubLocationText = ""

    state.houseType = 'House'

    // hubLocationDetails() // generate hub location details

    state.installed = true

    if (settings?.allowedAttributes) {
        state.attributesConfigured = true
    } else {
        logDebug "Unconfigured - Choose Events"
    }

    if (getSelectedDevices()) {
        state.devicesConfigured = true
    } else {
        logDebug "Unconfigured - Choose Devices"
    }

    // Configure Subscriptions:
    manageSubscriptions()

    // manageSchedules()

    // runIn(100, pollLocation)
    // runIn(300, pollDevices)
    // runIn(600, pollAttributes)
    // runIn(900, pollDeviceChecks)
}

/*****************************************************************************************************************
 *  Event Handlers:
 *****************************************************************************************************************/

def handleAppTouch(evt) { // handleAppTouch(evt) - used for testing
    logger("handleAppTouch()", 'trace')
}

def handleEnumEvent(evt) {
    def measurement = 'enum' // 'state'
    def retentionPolicy = 'autogen'
    def multiple = false
    influxLineProtocol(evt, measurement, retentionPolicy, multiple)
}

def handleNumberEvent(evt) {
    def measurement = 'number'
    def retentionPolicy = 'autogen'
    def multiple = false
    influxLineProtocol(evt, measurement, retentionPolicy, multiple)
}

def handleNumberThreeaxes(evt) {
    def measurement = 'threeaxes'
    def retentionPolicy = 'autogen'
    def multiple = false
    influxLineProtocol(evt, measurement, retentionPolicy, multiple)
}

def handleDaylight(evt) {
    def measurement = 'daylight'
    def retentionPolicy = 'autogen'
    def multiple = false
    def event = evt.clone()
    def value = "${event.name}"
    event.name = 'daylight'
    event.value = value
    influxLineProtocol(event, measurement, retentionPolicy, multiple)
}

def handleHubStatus(evt) {
    if (evt.value == 'active' || evt.value == 'disconnected') {
        def measurement = 'hubStatus'
        def retentionPolicy = 'autogen'
        def multiple = false
        influxLineProtocol(evt, measurement, retentionPolicy, multiple)
    }
}

def pollLocation() {
    def measurement = 'location'
    def retentionPolicy = 'metadata'
    def multiple = false
    influxLineProtocol(evt, measurement, retentionPolicy, multiple)
}

def pollAttributes() {
    logger("pollAttributes()", 'trace')
    getSelectedDevices()?.each { dev ->
        def data = new StringBuilder()
        getDeviceAllowedAttrs(dev?.id)?.each { attr ->
            if (dev.latestState(attr)?.value != null) {
                logger("pollAttributes(): Polling device ${dev} for attribute: ${attr}", 'info')
                data.append(",event=${attr}") // TODO need to work out how to handle this
                def ending = '\n'
                def rp = 'metadata'
            }
        }
    }
    influxLineProtocol(evt, measurement, retentionPolicy, multiple)
}

def pollDevices() {
    logger("pollDevices()", 'trace')
    def data = new StringBuilder()
    def info
    getSelectedDevices()?.each { dev ->
        if (info.containsKey("zw")) {
            logger("pollDevices(): zWave report for device ${dev}", 'info')
            data.append('devices') // measurement name
        }
    }
    influxLineProtocol(evt, measurement, retentionPolicy, multiple)
}

def pollDeviceChecks() {
    getSelectedDevices()?.each { dev ->
        if (!dev.displayName.startsWith("~")) {
            logger("pollDeviceChecks(): device health check report for device ${dev}", 'info')
            data.append('deviceChecks') // measurement name
            data.append('\n')
            def rp = 'autogen'
        }
    }
    influxLineProtocol(evt, measurement, retentionPolicy, multiple)
}

def influxLineProtocol(items, measurement, retentionPolicy, multiple) {
    logger("influxLineProtocol(): ", 'trace')

    def influxLP = new StringBuilder()

    items.each { item ->

        influxLP.append(measurement)

        tags().each { tag ->
            if ('all' in tag.type || measurement in tag.type) {
                influxLP.append(",${tag.name}=")
                if (tag.arguments) {
                    influxLP.append("$tag.closure"(item))
                } else {
                   influxLP.append("$tag.closure"())
                }
            }
        }

        influxLP.append(' ')

        def fieldCount = 0
        fields().each { field ->
            if ('all' in field.type || eventType in field.type) {
                influxLP.append((fieldCount) ? ',' : '')
                if (field.name) influxLP.append("${field.name}=")
                if (field.arguments) {
                    influxLP.append("$field.closure"(item))
                } else {
                    influxLP.append("$field.closure"())
                }
                if (field.valueType == 'integer') influxLP.append('i')
                fieldCount++
            }
        }

        if (timestamp(item)) {
            influxLP.append(' ')
            influxLP.append(timestamp(item))
        }

        if (multiple) influxLP.append('\n')
    }

    logger ("${influxLP.toString()}", 'trace')
/*
    def rp = 'autogen' // set retention policy
    if (!(timeElapsed < 500 && evt.value == pEvent.value)) {
        // ignores repeated propagation of an event (time interval < 0.5 s)
        postToInfluxDB(tags.toString(), rp)
    } else {
        logger("handleEnumEvent(): Ignoring duplicate event $evt.displayName ($evt.name) $evt.value", 'warn')
    }
*/
}

def tags() { [
        [name: 'area', type: ['all'], closure: 'locationName', arguments: 0],
        [name: 'areaId', type: ['all'], closure: 'locationId', arguments: 0],
        [name: 'building', type: ['all'], closure: 'hubName', arguments: 0],
        [name: 'buildingId', type: ['all'], closure: 'hubId', arguments: 0],
        [name: 'chamber', type: ['all'], closure: 'groupName', arguments: 1],
        [name: 'chamberId', type: ['enum', 'number', 'vector3', 'device', 'attr', 'deviceZw'], closure: 'groupId', arguments: 1],
        [name: 'deviceCode', type: ['enum', 'number', 'vector3', 'device', 'attr', 'deviceZw'], closure: 'deviceCode', arguments: 1],
        [name: 'deviceId', type: ['enum', 'number', 'vector3', 'device', 'attr', 'deviceZw'], closure: 'deviceId', arguments: 1],
        [name: 'deviceLabel', type: ['enum', 'number', 'vector3', 'device', 'attr', 'deviceZw'], closure: 'deviceLabel', arguments: 1],
        [name: 'deviceType', type: ['device', 'attr', 'deviceZw'], closure: 'deviceType', arguments: 1],
        [name: 'event', type: ['enum', 'number', 'vector3', 'attr'], closure: 'eventName', arguments: 1],
        [name: 'eventType', type: ['enum', 'number', 'vector3', 'attr'], closure: 'eventType', arguments: 1], // ? rename to eventClass ?
        [name: 'hubStatus', type: ['location'], closure: 'hubStatus', arguments: 0],
        [name: 'hubType', type: ['location'], closure: 'hubType', arguments: 0],
        [name: 'identifierGlobal', type: ['enum', 'number', 'threeaxes', 'device', 'attr', 'deviceZw'], closure: 'identifierGlobal', arguments: 1],
        [name: 'identifierLocal', type: ['enum', 'number', 'threeaxes', 'device', 'attr', 'deviceZw'], closure: 'identifierLocal', arguments: 1],
        [name: 'isChange', type: ['all'], closure: 'isChange', arguments: 1], // ??Handle null values? or does it always have a value?
        [name: 'onBattery', type: ['location'], closure: 'onBattery', arguments: 0],
        [name: 'power', type: ['deviceZw'], closure: 'power', arguments: 1],
        [name: 'secure', type: ['deviceZw'], closure: 'secure', arguments: 1],
        [name: 'source', type: ['all'], closure: 'source', arguments: 1],
        [name: 'status', type: ['device', 'attr', 'deviceZw'], closure: 'status', arguments: 1], // TODO ?Included
        [name: 'type', type: ['deviceZw'], closure: 'zwType', arguments: 0],
        [name: 'timeElapsed', type: ['attr'], closure: 'daysElapsed', arguments: 1],
        [name: 'timeZone', type: ['location'], closure: 'timeZone', arguments: 0],
        [name: 'unit', type: ['number', 'vector3'], closure: 'unit', arguments: 1],
]}


def getLocationName() { return { -> location.name.replaceAll(' ', '\\\\ ') }.memoizeAtMost(1) }

def getLocationId() { return { -> location.id }.memoizeAtMost(1) }

def getHubName() { return { -> location.hubs[0].name.replaceAll(' ', '\\\\ ') }.memoizeAtMost(1) }

def getHubId() { return { -> location.hubs[0].id }.memoizeAtMost(1) }

def getGroupName() { return { (state?.groupNames?."${groupId(it)}") ?: state.houseType }.memoizeAtMost(1) } // not assigned for hub and daylight events

def getGroupId() { return { it?.device?.device?.groupId }.memoizeAtMost(1) }

def getDeviceCode() { return { (it?.device?.device?.name?.replaceAll(' ', '\\\\ ')) ?: 'unassigned' } }

def getDeviceId() { return { it.deviceId } }

def getDeviceLabel() { return { (it?.device?.device?.label?.replaceAll(' ', '\\\\ ')) ?: 'unassigned' }.memoizeAtMost(1) }

def getDeviceType() { return { it?.typeName.replaceAll(' ', '\\\\ ') } }

def getEventName() { return { it.name }.memoizeAtMost(1) }

def getEventDetails() { return { getAttributeDetail().find { ad -> ad.key == eventName(it) }.value } } // needs 'get' prefix

def getEventType() { return { eventDetails(it).type } }

def getHubStatus() { return { -> hub().status } }

def getHubType() { return { -> hub().type } }

def getIdentifierGlobal() { return { "${locationName()}\\ .\\${hubName()}\\ .\\ ${identifierLocal(it)}\\ .\\ ${eventName(it)}" } }

def getIdentifierLocal() { return { "${groupName(it)}\\ .\\ ${deviceLabel(it)}" }.memoizeAtMost(1) }

def getIsChange() { return { it?.isStateChange } } // ??Handle null values? or does it always have a value?

def getOnBattery() { return { -> hub().hub.getDataValue('batteryInUse') } }

def getPower() { return {
    switch (zwInfo(it)?.zw.take(1)) {
        case 'L':
            return 'Listening'; break
        case 'S':
            return 'Sleepy'; break
        case 'B':
            return 'Beamable'; break
    }
} }

def getSecure() { return { (zwInfo(it)?.zw.endsWith('s')) ? 'true' : 'false' } }

def getSource() { return {
    switch(it.source) {
        case 'DEVICE':
            return 'device'; break
        case 'LOCATION':
            return 'location'; break
        case 'HUB':
            return 'hub'; break
        default:
            return it.source; break
    }
} }

def getStatus() { return { it?.status } }

def getDaysElapsed() { return {
    def daysElapsed = ((new Date().time - it.latestState(attr).date.time) / 86_400_000) / 30
    daysElapsed = daysElapsed.toDouble().trunc().round()
    "${daysElapsed * 30}-${(daysElapsed + 1) * 30} days"
} }

def getTimeZone() { return { -> location.timeZone.ID } }

def getUnit() { return {
    def unit = (it?.unit) ?: eventDetails(it).unit
    // threeaxes unit is 'g'
    unit = (it.name != 'temperature') ?: unit.replaceAll('\u00B0', '') // remove circle from C unit
} }

def getZwInfo() { return { it?.zwaveInfo() } }

def getZwType() { return { 'zwave' } }


def fields() { [
        [name: '', type: ['deviceZw'], closure: 'configuredParameters', valueType: 'string', arguments: 1],
        [name: 'checkInterval', type: ['deviceZw'], closure: 'checkInterval', valueType: 'integer', arguments: 1],
        [name: 'eventDescription', type: ['all'], closure: 'eventDescription', valueType: 'string', arguments: 1],
        [name: 'eventId', type: ['all'], closure: 'eventId', valueType: 'string', arguments: 1],
        [name: 'firmwareVersion', type: ['location'], closure: 'firmware', valueType: 'string', arguments: 0],
        [name: 'hubIP', type: ['location'], closure: 'hubIP', valueType: 'string', arguments: 0],
        [name: 'latitude', type: ['location'], closure: 'latitude', valueType: 'string', arguments: 0],
        [name: 'longitude', type: ['location'], closure: 'longitude', valueType: 'string', arguments: 0],
        [name: 'nBinary', type: ['day', 'hub', 'enum'], closure: 'currentStateBinary', valueType: 'boolean', arguments: 1],
        [name: 'nLevel', type: ['day', 'hub', 'enum'], closure: 'currentStateLevel', valueType: 'integer', arguments: 1],
        [name: 'nState', type: ['day', 'hub', 'enum'], closure: 'currentState', valueType: 'string', arguments: 1],
        [name: 'nText', type: ['enum'], closure: 'currentStateDescription', valueType: 'string', arguments: 1],
        [name: 'nText', type: ['number'], closure: 'currentValueDescription', valueType: 'string', arguments: 1],
        [name: 'nValue', type: ['number'], closure: 'currentValue', valueType: 'float', arguments: 1],
        [name: 'nValueDisplay', type: ['number'], closure: 'currentValueDisplay', valueType: 'float', arguments: 1],
        [name: 'nValueX', type: ['vector3'], closure: 'currentValueX', valueType: 'float', arguments: 1],
        [name: 'nValueY', type: ['vector3'], closure: 'currentValueY', valueType: 'float', arguments: 1],
        [name: 'nValueZ', type: ['vector3'], closure: 'currentValueZ', valueType: 'float', arguments: 1],
        [name: 'pBinary', type: ['enum'], closure: 'previousStateBinary', valueType: 'boolean', arguments: 1],
        [name: 'pLevel', type: ['enum'], closure: 'previousStateLevel', valueType: 'integer', arguments: 1],
        [name: 'portTCP', type: ['location'], closure: 'portTCP', valueType: 'integer', arguments: 0],
        [name: 'pState', type: ['enum'], closure: 'previousState', valueType: 'string', arguments: 1],
        [name: 'pText', type: ['enum'], closure: 'previousStateDescription', valueType: 'string', arguments: 1],
        [name: 'pText', type: ['number'], closure: 'previousValueDescription', valueType: 'string', arguments: 1],
        [name: 'pValue', type: ['number'], closure: 'previousValue', valueType: 'float', arguments: 1],
        [name: 'rChange', type: ['number'], closure: 'difference', valueType: 'float', arguments: 1],
        [name: 'rChangeText', type: ['number'], closure: 'differenceText', valueType: 'string', arguments: 1],
        [name: 'statusLevel', type: ['device'], closure: 'statusLevel', valueType: 'integer', arguments: 1], // TODO Convert to a tag?
        [name: 'sunrise', type: ['location'], closure: 'sunrise', valueType: 'string', arguments: 0],
        [name: 'sunset', type: ['location'], closure: 'sunset', valueType: 'string', arguments: 0],
        [name: 'tDay', type: ['enum', 'number'], closure: 'timeOfDay', valueType: 'integer', arguments: 1],
        [name: 'tElapsed', type: ['enum', 'number'], closure: 'timeElapsed', valueType: 'integer', arguments: 1],
        [name: 'tElapsedText', type: ['enum', 'number'], closure: 'timeElapsedText', valueType: 'string', arguments: 1],
        [name: 'timeLastEvent', type: ['attr'], closure: 'timeLastEvent', valueType: 'integer', arguments: 1],
        [name: 'timestamp', type: ['all'], closure: 'timestamp', valueType: 'integer', arguments: 1],
        [name: 'tOffset', type: ['enum'], closure: 'currentTimeOffset', valueType: 'integer', arguments: 1],
        [name: 'tWrite', type: ['enum', 'number', 'vector3'], closure: 'timeWrite', valueType: 'integer', arguments: 0],
        [name: 'valueLastEvent', type: ['attr'], closure: 'valueLastEvent', valueType: 'string', arguments: 1],
        [name: 'wLevel', type: ['enum'], closure: 'weightedLevel', valueType: 'integer', arguments: 1],
        [name: 'wValue', type: ['number'], closure: 'weightedValue', valueType: 'float', arguments: 1],
        [name: 'zigbeePowerLevel', type: ['location'], closure: 'zigbeePowerLevel', valueType: 'integer', arguments: 0],
        [name: 'zwavePowerLevel', type: ['location'], closure: 'longitude', valueType: 'string', arguments: 0],
        [name: '', type: ['deviceZw'], closure: 'ccList', valueType: 'string', arguments: 1],
] }

def getCcList() { return {
    def info = zwInfo(it).clone() // TODO rewrite this using collect filters? so as to avoid need for cloning
    def cc = info.cc
    if (info?.ccOut) cc.addAll(info.ccOut)
    if (info?.sec) cc.addAll(info.sec)
    def ccList = 'zz' + cc.sort().join("=true,zz") + '=true'
    info.remove('zw')
    info.remove('cc')
    info.remove('ccOut')
    info.remove('sec')
    info = info.sort()
    def toKeyValue = { it.collect { /$it.key="$it.value"/ } join "," }
    info = toKeyValue(info) + ',' + "${ccList}"
    info
} }

def getCheckInterval() { return { it?.latestState('checkInterval')?.value } }

def getConfiguredParameters() { return { (it?.device?.getDataValue('configuredParameters')) ?: '' } }

def getDaylight() { return { -> getSunriseAndSunset() } }

def getEventDescription() { return { "\"${it?.descriptionText}\"" } }

def getEventId() { return { "\"${it.id}\"" } }

def getCurrentState() { return { "\"${it.value}\"" } }

def getCurrentStateBinary() { return { (currentStateLevel(it) > 0) ? 'true' : 'false' } }

def getCurrentStateLevel() { return { attributeStates(it).find { level -> level.key == it.value }.value } }

def getAttributeStates() { return { eventDetails(it).levels }.memoizeAtMost(1) } // Lookup array for event state levels

def getCurrentStateDescription() { return {
    def text = "\"At ${locationName()}, in ${hubName()}, ${deviceLabel(it)} is ${currentState(it)} in the ${groupName(it)}.\""
    text.replaceAll('\\\\', '')
} }

def getCurrentValue() { return { (it?.numberValue?.toBigDecimal()) ?: removeUnit(it) } }

def removeUnit() { return { // remove any units appending to end of event value
    def length = it.value.length()
    def value
    def i = 2
    while (i < (length - 1)) {
        value = it.value.substring(0, length - i)
        if (value.isNumber()) break
        i++
    }
    if (i == length) {
        return 0
    } else {
        return value.toBigDecimal()
    }
} }

def getCurrentValueDisplay() { return { "${currentValue(it).setScale(decimalPlaces(it), BigDecimal.ROUND_HALF_EVEN)}" } }

    def getDecimalPlaces() { return { eventDetails(it)?.decimalPlaces } }

def getCurrentValueDescription() { return {
    def text = "\"At ${locationName()} ${eventName(it)} is ${currentValueDisplay(it)} in the ${groupName(it)}.\""
    text.replaceAll('\\\\', '')
} }

def getCurrentValueX() { return { it.xyzValue.x / gravityFactor() } }
def getCurrentValueY() { return { it.xyzValue.y / gravityFactor() } }
def getCurrentValueZ() { return { it.xyzValue.z / gravityFactor() } }

    def getGravityFactor() { return { -> (1024) }.memoizeAtMost(1) }

def getFirmware() { return { -> "\"${hub().firmwareVersionString}\"" } }

def getHub() { return { -> location.hubs[0] }.memoizeAtMost(1) }

def getHubIP() { return { -> "\"${hub().localIP}\"" } }

def getLatitude() { return { -> "\"${hub().latitude}\"" } }

def getLongitude() { return { -> "\"${hub().longitude}\"" } }

def getPortTCP() { return { -> hub().localSrvPortTCP } }

def getPreviousEvent() { return {
    def eventData = parseJson(it?.data)
    if (eventData?.previous) {
        [value: eventData?.previous?.value, date: it?.data?.previous?.date] // TODO - Check that date is the correct field
    }
    else {
        def history = it.device.statesSince("${it.name}", it.date - 7, [max: 5])
        def historySorted = (history) ? history.sort { a, b -> b.date.time <=> a.date.time } : it.device.latestState("${it.name}")
        historySorted.find { previous -> previous.date.time < it.date.time }
    }
} }

def getPreviousState() { return { "\"${previousEvent(it).value}\"" } }

def getPreviousStateBinary() { return { (previousStateLevel(it) > 0) ? 'true' : 'false' } }

    def getPreviousStateLevel() { return { attributeStates(it).find { level -> level.key == previousEvent(it).value }.value } }

def getPreviousStateDescription() { return { "\"This is a change from ${previousState(it)} ${timeElapsedText(it)}.\"" } } // Has got quotes round previousState(it)

def getPreviousValue() { return { (previousEvent(it)?.numberValue?.toBigDecimal()) ?: removeUnit(previousEvent(it)) } }

def getDifference() { return { (currentValue(it).setScale(decimalPlaces(it), BigDecimal.ROUND_HALF_EVEN) - previousValue(it).setScale(decimalPlaces(it), BigDecimal.ROUND_HALF_EVEN)).toBigDecimal().setScale(decimalPlaces(it), BigDecimal.ROUND_HALF_EVEN) } }

def getPreviousValueDescription() { return {
    def changeAbs = (differenceText(it) == 'unchanged') ?: "${differenceText(it)} by ${difference(it).abs()} ${unit(it)}"
    "\"This is ${changeAbs} compared to ${timeElapsedText(it)}.\""
} }

def getDifferenceText() { return {
    def changeText = 'unchanged' // text description of change
    if (difference(it) > 0) changeText = 'increased'
    else if (difference(it) < 0) changeText = 'decreased'
    changeText
} }

def getStatusLevel() { return { (it?.status.toUpperCase() in ["ONLINE"]) ? 1 : 0 } }

def getSunrise() { return { -> "\"${daylight.sunrise.format('HH:mm', timeZone())}\"" } }

def getSunset() { return { -> "\"${daylight.sunset.format('HH:mm', timeZone())}\"" } }

def getTimestamp() { return { it.date.time - currentTimeOffset(it) } }

    def getTimeOffsetAmount() { return { -> (1000 * 10 / 2) }.memoizeAtMost(1) }

    def getCurrentTimeOffset() { return { (eventName(it) == 'motion' && currentState(it) == "\"inactive\"") ? timeOffsetAmount() : 0 } }

    def getPreviousTimeOffset() { return { (eventName(it) == 'motion' && previousState(it) == "\"inactive\"") ? timeOffsetAmount() : 0 } }

def getTimeOfDay() { return { timestamp(it) - it.date.clone().clearTime().time } } // calculate time of day in elapsed milliseconds

def getTimeElapsed() { return { timestamp(it) - previousEvent(it).date.time - previousTimeOffset(it) } }

def getTimeElapsedText() { return {
    def time = timeElapsed(it) / 1000
    def phrase
    if (time < 60) phrase = Math.round(time) + ' seconds ago'
    else if (time < 90) phrase = Math.round(time / 60) + ' minute ago'
    else if (time < 3600) phrase = Math.round(time / 60) + ' minutes ago'
    else if (time < 5400) phrase = Math.round(time / 3600) + ' hour ago'
    else if (time < 86400) phrase = Math.round(time / 3600) + ' hours ago'
    else if (time < 129600) phrase = Math.round(time / 86400) + ' day ago'
    else phrase = Math.round(time / 86400) + ' days ago'
    "\"${phrase}\""
    }
}

def getTimeLastEvent() { return { it.latestState(attr).date.time } }

def getTimeWrite() { return { -> new Date().time } } // time of processing the event

def getValueLastEvent() { return { "\"${it.latestState(attr).value}\"" } }

def getWeightedLevel() { return {  previousStateLevel(it) * timeElapsed(it) } }

def getWeightedValue() { return {  previousValue(it) * timeElapsed(it) } }

def getZigbeePowerLevel() { return { -> hub().hub.getDataValue('zigbeePowerLevel') } }

def getZwavePowerLevel() { return { -> hub().hub.getDataValue('zwavePowerLevel') } }


/*
def tagsPollAttributes() { [
        [name: 'area', type: ['all'], closure: 'locationName', arguments: 0],
        [name: 'areaId', type: ['all'], closure: 'locationId', arguments: 0],
        [name: 'building', type: ['all'], closure: 'hubName', arguments: 0],
        [name: 'buildingId', type: ['all'], closure: 'hubId', arguments: 0],
        [name: 'chamber', type: ['all'], closure: 'groupName', arguments: 1],
        [name: 'chamberId', type: ['enum', 'number', 'vector3'], closure: 'groupId', arguments: 1],
        [name: 'deviceCode', type: ['enum', 'number', 'vector3'], closure: 'deviceCode', arguments: 1],
        [name: 'deviceId', type: ['enum', 'number', 'vector3'], closure: 'deviceId', arguments: 1],
        [name: 'deviceLabel', type: ['enum', 'number', 'vector3'], closure: 'deviceLabel', arguments: 1],
        [name: 'deviceType', type: ['enum', 'number', 'vector3'], closure: 'deviceType', arguments: 1],
        [name: 'eventType', type: ['enum', 'number', 'vector3'], closure: 'eventType', arguments: 1],
        [name: 'identifierGlobal', type: ['enum', 'number', 'threeaxes'], closure: 'identifierGlobal', arguments: 1],
        [name: 'identifierLocal', type: ['enum', 'number', 'threeaxes'], closure: 'identifierLocal', arguments: 1],
        [name: 'status', type: ['number', 'vector3'], closure: 'status', arguments: 1], // TODO ?Included
        [name: 'timeElapsed', type: ['number', 'vector3'], closure: 'timeElapsed', arguments: 1],
] }
def fieldsPollAttributes() { [
        [name: 'timeLastEvent', type: ['attr'], closure: 'timeLastEvent', valueType: 'integer', arguments: 1],
        [name: 'valueLastEvent', type: ['attr'], closure: 'valueLastEvent', valueType: 'string', arguments: 1],
] }

def tagsDeviceChecks() { [
        [name: 'area', type: ['all'], closure: 'locationName', arguments: 0],
        [name: 'areaId', type: ['all'], closure: 'locationId', arguments: 0],
        [name: 'building', type: ['all'], closure: 'hubName', arguments: 0],
        [name: 'buildingId', type: ['all'], closure: 'hubId', arguments: 0],
        [name: 'chamber', type: ['all'], closure: 'groupName', arguments: 1],
        [name: 'chamberId', type: ['enum', 'number', 'vector3'], closure: 'groupId', arguments: 1],
        [name: 'deviceCode', type: ['enum', 'number', 'vector3'], closure: 'deviceCode', arguments: 1],
        [name: 'deviceId', type: ['enum', 'number', 'vector3'], closure: 'deviceId', arguments: 1],
        [name: 'deviceLabel', type: ['enum', 'number', 'vector3'], closure: 'deviceLabel', arguments: 1],
        [name: 'deviceType', type: ['enum', 'number', 'vector3'], closure: 'deviceType', arguments: 1],
        [name: 'identifierGlobal', type: ['enum', 'number', 'threeaxes'], closure: 'identifierGlobal', arguments: 1],
        [name: 'identifierLocal', type: ['enum', 'number', 'threeaxes'], closure: 'identifierLocal', arguments: 1],
        [name: 'power', type: ['enum', 'number', 'threeaxes'], closure: 'power', arguments: 1],
        [name: 'secure', type: ['enum', 'number', 'threeaxes'], closure: 'secure', arguments: 1],
        [name: 'status', type: ['number', 'vector3'], closure: 'status', arguments: 1],
        [name: 'type', type: ['location'], closure: 'zwType', arguments: 0],
] }
def fieldsPollDevices() { [
        [name: '', type: ['location'], closure: 'configuredParameters', valueType: 'string', arguments: 1],
        [name: 'checkInterval', type: ['location'], closure: 'checkInterval', valueType: 'integer', arguments: 1],
        [name: '', type: ['location'], closure: 'ccList', valueType: 'string', arguments: 1],
] }

def tagsLocation() { [
        [name: 'area', type: ['location'], closure: 'locationName', arguments: 0],
        [name: 'areaId', type: ['location'], closure: 'locationId', arguments: 0],
        [name: 'building', type: ['location'], closure: 'hubName', arguments: 0],
        [name: 'buildingId', type: ['location'], closure: 'hubId', arguments: 0],
        [name: 'hubStatus', type: ['location'], closure: 'hubStatus', arguments: 0],
        [name: 'hubType', type: ['location'], closure: 'hubType', arguments: 0],
        [name: 'onBattery', type: ['location'], closure: 'onBattery', arguments: 0],
        [name: 'timeZone', type: ['location'], closure: 'timeZone', arguments: 0],
] }
def fieldsLocation() { [
        [name: 'firmwareVersion', type: ['location'], closure: 'firmware', valueType: 'string', arguments: 0],
        [name: 'hubIP', type: ['location'], closure: 'hubIP', valueType: 'string', arguments: 0],
        [name: 'latitude', type: ['location'], closure: 'latitude', valueType: 'string', arguments: 0],
        [name: 'longitude', type: ['location'], closure: 'longitude', valueType: 'string', arguments: 0],
        [name: 'portTCP', type: ['location'], closure: 'portTCP', valueType: 'integer', arguments: 0],
        [name: 'sunrise', type: ['location'], closure: 'sunrise', valueType: 'string', arguments: 0],
        [name: 'sunset', type: ['location'], closure: 'sunset', valueType: 'string', arguments: 0],
        [name: 'zigbeePowerLevel', type: ['location'], closure: 'zigbeePowerLevel', valueType: 'integer', arguments: 0],
        [name: 'zwavePowerLevel', type: ['location'], closure: 'longitude', valueType: 'string', arguments: 0],
] }

def tagsDeviceChecks() { [
        [name: 'area', type: ['all'], closure: 'locationName', arguments: 0],
        [name: 'areaId', type: ['all'], closure: 'locationId', arguments: 0],
        [name: 'building', type: ['all'], closure: 'hubName', arguments: 0],
        [name: 'buildingId', type: ['all'], closure: 'hubId', arguments: 0],
        [name: 'chamber', type: ['all'], closure: 'groupName', arguments: 1],
        [name: 'chamberId', type: ['enum', 'number', 'vector3'], closure: 'groupId', arguments: 1],
        [name: 'deviceCode', type: ['enum', 'number', 'vector3'], closure: 'deviceCode', arguments: 1],
        [name: 'deviceId', type: ['enum', 'number', 'vector3'], closure: 'deviceId', arguments: 1],
        [name: 'deviceLabel', type: ['enum', 'number', 'vector3'], closure: 'deviceLabel', arguments: 1],
        [name: 'deviceType', type: ['enum', 'number', 'vector3'], closure: 'deviceType', arguments: 1],
        [name: 'identifierGlobal', type: ['enum', 'number', 'threeaxes'], closure: 'identifierGlobal', arguments: 1],
        [name: 'identifierLocal', type: ['enum', 'number', 'threeaxes'], closure: 'identifierLocal', arguments: 1],
        [name: 'status', type: ['number', 'vector3'], closure: 'status', arguments: 1],
] }

def fieldsDeviceChecks() { [
        [name: 'statusLevel', type: ['location'], closure: 'statusLevel', valueType: 'integer', arguments: 1],
] }
*/

/*****************************************************************************************************************
 *  Main Commands:
 *****************************************************************************************************************/
/*
def hubLocationDetails() {
    // state.hubLocationDetails = ",area=${location.name.replaceAll(' ', '\\\\ ')},areaId=${location.id},building=${location.hubs[0].name.replaceAll(' ', '\\\\ ')},buildingId=${location.hubs[0].id}"
    // tags: area,areaId,building,buildingId
    // state.hubLocationIdentifier = "${location.name.replaceAll(' ', '\\\\ ')}\\ .\\ ${location.hubs[0].name.replaceAll(' ', '\\\\ ')}"
    // state.hubLocationText = "At ${location.name}, in ${location.hubs[0].name},"
}
*/

def postToInfluxDB(data, rp = 'autogen') {
// need to update hubAction state variables and rewrite the hubAction function
    if (state.databaseHost.take(3) == "192") {
        try {
            def query = state.query.clone()
            query.rp = rp
            def hubAction = new physicalgraph.device.HubAction([
                    method : "POST",
                    headers: state.headers,
                    path   : state.path,
                    query  : query,
                    body   : data
            ],
                    null,
                    [callback: handleInfluxResponseHubAction]
            )
            logger("postToInfluxDB(): Posting data to InfluxDB: Headers: ${state.headers}, Path: ${state.path}, Query: ${query}, Data: ${data}", 'info')
            sendHubCommand(hubAction)
        }
        catch (Exception e) {
            logger("postToInfluxDB(): Exception ${e} on ${hubAction}", 'error')
        }
    } else {
        def query = state.query.clone()
        query.rp = rp
        def params = [
                uri               : state.uri,
                path              : state.path,
                query             : query,
                contentType       : "application/x-www-form-urlencoded",
                requestContentType: "application/x-www-form-urlencoded",
                body              : data
        ]
        logger("postToInfluxDB(): Posting data to InfluxDB: Uri: ${state.uri}, Path: ${state.path}, Query: ${query}, Data: ${data}", 'info')
        asynchttp_v1.post(handleInfluxResponse, params)
    }
}

def handleInfluxResponseHubAction(physicalgraph.device.HubResponse hubResponse) {
    def status = hubResponse.status
    if (status == 204) logger("postToInfluxDB(): Success! Response from InfluxDB: Status: ${status}, Headers: ${hubResponse.headers}", 'trace')
    if (status >= 400) logger("postToInfluxDB(): Something went wrong! Response from InfluxDB: Status: ${status}, Headers: ${hubResponse.headers}, Body: ${hubResponse.data}", 'error')
}

def handleInfluxResponse(response, requestdata) {
    def status = response.status
    if (status == 204) logger("postToInfluxDB(): Success! Response from InfluxDB: Status: ${status}, Headers: ${response.headers}, Body: ${response.data}", 'trace')
    if (status >= 400) logger("postToInfluxDB(): Something went wrong! Response from InfluxDB: Status: ${status}, Headers: ${response.headers}, Body: ${response.data}", 'error')
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/
/*
private manageSchedules() {
    logger("manageSchedules()", 'trace')

    try {
        unschedule(hubLocationDetails)
    }
    catch (e) {
        logger("manageSchedules(): Unschedule hubLocationDetails failed!", 'error')
    }
    runEvery3Hours(hubLocationDetails)

    try {
        unschedule(pollLocation)
    }
    catch (e) {
        logger("manageSchedules(): Unschedule pollLocation failed!", 'error')
    }
    runEvery3Hours(pollLocation)

    try {
        unschedule(pollDevices)
    }
    catch (e) {
        logger("manageSchedules(): Unschedule pollDevices failed!", 'error')
    }
    runEvery3Hours(pollDevices)

    try {
        unschedule(pollAttributes)
    }
    catch (e) {
        logger("manageSchedules(): Unschedule pollAttributes failed!", 'error')
    }
    runEvery3Hours(pollAttributes)

    try {
        unschedule(pollDeviceChecks)
    }
    catch (e) {
        logger("manageSchedules(): Unschedule pollDeviceChecks failed!", 'error')
    }
    runEvery3Hours(pollDeviceChecks)
}
*/

private manageSubscriptions() { // Configures subscriptions
    logger("manageSubscriptions()", 'trace')
    unsubscribe()

    getSelectedDevices()?.each { dev ->

        if (!dev.displayName.startsWith("~")) {

            getDeviceAllowedAttrs(dev?.id)?.each { attr ->

                if (dev?.hasAttribute("${attr}")) { // select only attributes that exist

                    def type = getAttributeDetail().find { it.key == attr }.value.type

                    if (type == 'enum') {
                        logger("manageSubscriptions(): Subscribing 'handleEnumEvent' listener to attribute: ${attr}, for device: ${dev}", 'info')
                        subscribe(dev, attr, handleEnumEvent)
                    } else if (type == 'number') {
                        logger("manageSubscriptions(): Subscribing 'handleNumberEvent' listener to attribute: ${attr}, for device: ${dev}", 'info')
                        subscribe(dev, attr, handleNumberEvent)
                    } else if (type == 'vector3') {
                        logger("manageSubscriptions(): Subscribing 'handleVector3Event' listener to attribute: ${attr}, for device: ${dev}", 'info')
                        subscribe(dev, attr, handleVector3Event)
                    } else if (type == 'color_map') {
                        logger("manageSubscriptions(): Subscribing 'handleColor_mapEvent' listener to attribute: ${attr}, for device: ${dev}", 'info')
                        //    subscribe(dev, attr, handleColor_mapEvent) *** TO DO - write handler
                    } else if (type == 'json_object') {
                        logger("manageSubscriptions(): Subscribing 'handleJson_objectEvent' listener to attribute: ${attr}, for device: ${dev}", 'info')
                        //    subscribe(dev, attr, handleJson_objectEvent) *** TO DO - write handler (if needed)
                    }
                }
            }
        }
    }

    /*
    // subscribe to Sunrise and Sunset events
    subscribe(location, "sunrise", handleDaylight)
    subscribe(location, "sunset", handleDaylight)
    logger("manageSubscriptions(): Subscribing 'handleDaylight' listener to 'Sunrise' and 'Sunset' events", 'info')
    */
    /*
    // subscribe to Hub status
    def hub = location.hubs[0]
    subscribe(hub, "hubStatus", handleHubStatus)
    logger("manageSubscriptions(): Subscribing 'handleHubStatus' listener to 'Hub Status' events", 'info')
    */

    // build state maps of group Ids and group names
    if (state.roomNameCapture) {
        def groupId
        def groupName
        settings.bridgePref.each {
            if (it.name?.take(1) == '~') {
                groupId = it.device?.groupId
                groupName = it.name?.drop(1).replaceAll(' ', '\\\\ ')
                if (groupId) state.groupNames << [(groupId): groupName]
            }
        }
    }
}

private logger(msg, level = 'debug') { // Wrapper function for all logging
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
                logger("Error while getting device allowed attributes for ${device?.displayName} and attribute ${attr}: ${e.message}", 'warn')
                // need to check device.displayName - should it be deviceName.displayName or dev.displayName ??
            }
        }
    }
    catch (e) {
        logger("Error while getting device allowed attributes for ${device.displayName}: ${e.message}", 'warn')
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
                } else {
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

private getCapabilities() {
    [
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
            [title: 'Window Shades', cap: 'windowShade', attr: 'windowShade']
    ]
}

private getAttributeDetail() {
    [
            acceleration            : [type: 'enum', levels: [inactive: -1, active: 1]],
            alarm                   : [type: 'enum', levels: [off: -1, siren: 1, strobe: 2, both: 3]],
            battery                 : [type: 'number', decimalPlaces: 0, unit: '%'],
            button                  : [type: 'enum', levels: [released: -1, pushed: 1, double: 2, held: 3]],
            buttonClicks            : [type: 'enum', levels: ['hold start': -1, 'hold release': 0, 'one click': 1, 'two clicks': 2, 'three clicks': 3, 'four clicks': 4, 'five clicks': 5]],
            carbonDioxide           : [type: 'number', decimalPlaces: 0, unit: 'ppm'],
            carbonMonoxide          : [type: 'enum', levels: [clear: -1, detected: 1, tested: 4]],
            color                   : [type: 'color_map'],
            colorTemperature        : [type: 'number', decimalPlaces: 0, unit: 'K'],
            consumableStatus        : [type: 'enum', levels: [replace: -1, good: 1, order: 3, 'maintenance required': 4, missing: 5]],
            contact                 : [type: 'enum', levels: [closed: -1, empty: -1, full: -1, vacant: -1, flushing: 1, occupied: 1, open: 1]],
            current                 : [type: 'number', decimalPlaces: 2, unit: 'A'],
            daylight                : [type: 'enum', levels: [ sunset: -1, sunrise: 1]],
            door                    : [type: 'enum', levels: [closing: -2, closed: -1, open: 1, opening: 2, unknown: 5]],
            energy                  : [type: 'number', decimalPlaces: 2, unit: 'kWh'],
            heatingSetpoint         : [type: 'number', decimalPlaces: 0, unit: 'C'],
            hubStatus               : [type: 'enum', levels: [disconnected: -1, active: 1]],
            hue                     : [type: 'number', decimalPlaces: 0, unit: '%'],
            humidity                : [type: 'number', decimalPlaces: 0, unit: '%'],
            illuminance             : [type: 'number', decimalPlaces: 0, unit: 'lux'],
            level                   : [type: 'number', decimalPlaces: 0, unit: ''],
            lock                    : [type: 'enum', levels: [locked: -1, unlocked: 1, 'unlocked with timeout': 2, unknown: 5]],
            lqi                     : [type: 'number', decimalPlaces: 2, unit: 'dB'],
            motion                  : [type: 'enum', levels: [inactive: -1, active: 1]],
            mute                    : [type: 'enum', levels: [muted: -1, unmuted: 1]],
            numberOfButtons         : [type: 'number', decimalPlaces: 0, unit: ''],
            pH                      : [type: 'number', decimalPlaces: 1, unit: ''],
            power                   : [type: 'number', decimalPlaces: 0, unit: 'W'],
            powerSource             : [type: 'enum', levels: [mains: -2, dc: -1, battery: 1, unknown: 5]],
            presence                : [type: 'enum', levels: ['not present': -1, present: 1]],
            pressure                : [type: 'number', decimalPlaces: 1, unit: 'mbar'],
            reactiveEnergy          : [type: 'number', decimalPlaces: 2, unit: 'kVarh'],
            reactivePower           : [type: 'number', decimalPlaces: 3, unit: 'kVar'],
            rssi                    : [type: 'number', decimalPlaces: 2, unit: 'dB'],
            saturation              : [type: 'number', decimalPlaces: 0, unit: '%'],
            shock                   : [type: 'enum', levels: [clear: -1, detected: 1]],
            sleeping                : [type: 'enum', levels: [sleeping: -1, 'not sleeping': 1]],
            smoke                   : [type: 'enum', levels: [clear: -1, detected: 1, tested: 4]],
            sound                   : [type: 'enum', levels: ['not detected': -1, detected: 1]],
            soundPressureLevel      : [type: 'number', decimalPlaces: 0, unit: 'dB'],
            status                  : [type: 'string'],
            switch                  : [type: 'enum', levels: [off: -1, on: 1]],
            tamper                  : [type: 'enum', levels: [clear: -1, detected: 1]],
            temperature             : [type: 'number', decimalPlaces: 0, unit: 'C'],
            thermostatFanMode       : [type: 'enum', levels: [on: 1, circulate: 2, auto: 3, followschedule: 4]],
            thermostatMode          : [type: 'enum', levels: ['rush hour': -4, cool: -3, off: -1, heat: 1, 'emergency heat': 2, auto: 3]],
            thermostatOperatingState: [type: 'enum', levels: [cooling: -3, 'pending cool': -2, idle: -1, heating: 1, 'pending heat': 2, 'fan only': 3]],
            thermostatSetpoint      : [type: 'number', decimalPlaces: 0, unit: 'C'],
            threeAxis               : [type: 'vector3', decimalPlaces: 2, unit: 'g'],
            totalEnergy             : [type: 'number', decimalPlaces: 2, unit: 'kVAh'],
            touch                   : [type: 'enum', levels: [touched: 1]],
            trackDescription        : [type: 'string'],
            ultravioletIndex        : [type: 'number', decimalPlaces: 0, unit: ''],
            valve                   : [type: 'enum', levels: [closed: -1, open: 1]],
            voltage                 : [type: 'number', decimalPlaces: 0, unit: 'V'],
            water                   : [type: 'enum', levels: [dry: -1, wet: 1]],
            windowShade             : [type: 'enum', levels: [closing: -2, closed: -1, opening: 2, 'partially open': 3, unknown: 5]]
    ]
}