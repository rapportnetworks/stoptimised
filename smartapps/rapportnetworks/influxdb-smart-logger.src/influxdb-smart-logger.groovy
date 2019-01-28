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
                name: 'prefDatabaseRemote',
                type: 'bool',
                title: 'Use Remote Database',
                defaultValue: true,
                required: true
            )

            input(
                    name: 'prefDatabaseSecure',
                    type: 'bool',
                    title: 'Use Encrypted Connection',
                    defaultValue: true,
                    required: true
            )

            input(name: 'prefDatabaseHost', type: 'text', title: 'Host', defaultValue: 'data.sunnd.com', required: true)

            input(name: 'prefDatabasePort', type: 'text', title: 'Port', defaultValue: '443', required: true)

            input(name: 'prefDatabaseName', type: 'text', title: 'Database Name', defaultValue: '*', required: true)

            input(name: 'prefDatabaseUser', type: 'text', title: 'Username', defaultValue: '*', required: true)

            input(name: 'prefDatabasePass', type: 'text', title: 'Password', defaultValue: '*', required: true)
        }
        /*
        section('System Monitoring:') {
            input(
                    name: 'prefAdjustInactiveTimestamp',
                    type: 'bool',
                    title: 'Adjust Inactive status timestamp to compensate for PIR reset time?',
                    defaultValue: true,
                    required: true
            )
        }
        */
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
                input("${it.cap}Pref", "capability.${it.cap}", title: "${it.title}:", multiple: true, hideWhenEmpty: true, required: false, submitOnChange: true)
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
            input(name: 'allowedAttributes', type: 'enum', title: "Which events should be logged?", required: true, multiple: true, submitOnChange: true, options: supportedAttr)
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
                                input(name: "${attr}Exclusions", type: "enum", title: "Exclude ${attr} events:", required: false, multiple: true, options: attrDevices)
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
    state.headers = [HOST: "${settings.prefDatabaseHost}:${settings.prefDatabasePort}", "Content-Type": "application/x-www-form-urlencoded"]
    state.uri = "http${(settings.prefDatabaseSecure) ? 's' : ''}://${settings.prefDatabaseHost}:${settings.prefDatabasePort}"
    state.query = [db: "${settings.prefDatabaseName}", u: "${settings.prefDatabaseUser}", p: "${settings.prefDatabasePass}", precision: 'ms']
    state.path = '/write'

    // Set values for state variables
    state.groupNames = [:] // Initialise map of Group Ids and Group Names TODO - Is this needed here?

    state.houseType = 'House'

    state.installed = true

    if (settings?.allowedAttributes) {
        state.attributesConfigured = true
    } else {
        logger("Unconfigured - Choose Events", 'debug')
    }

    if (getSelectedDevices()) {
        state.devicesConfigured = true
    } else {
        logger("Unconfigured - Choose Devices", 'debug')
    }

    // Configure Subscriptions:
    manageSubscriptions()

    manageSchedules()

    // runIn(100, pollLocations)
    // runIn(300, pollDevices)
    // runIn(600, pollAttributes)
    // runIn(900, pollZwaves)
}

/*****************************************************************************************************************
 *  Event Handlers:
 *****************************************************************************************************************/

def handleAppTouch(evt) { // handleAppTouch(evt) - used for testing
    logger("handleAppTouch()", 'trace')
}

def handleEnumEvent(evt) {
    def measurementType = 'enum'
    def measurementName = 'states'
    def retentionPolicy = 'autogen'
    def multiple = false
    def superItem = false
    influxLineProtocol(evt, measurementName, measurementType, multiple, retentionPolicy, superItem)
}

def handleNumberEvent(evt) {
    def measurementType = 'number'
    def measurementName = 'values'
    def retentionPolicy = 'autogen'
    def multiple = false
    def superItem = false
    influxLineProtocol(evt, measurementName, measurementType, multiple, retentionPolicy, superItem)
}

def handleVector3Event(evt) {
    def measurementType = 'vector3'
    def measurementName = 'threeaxes'
    def retentionPolicy = 'autogen'
    def multiple = false
    def superItem = false
    influxLineProtocol(evt, measurementName, measurementType, multiple, retentionPolicy, superItem)
}

def handleStringEvent(evt) {
    def measurementType = 'string'
    def measurementName = 'statuses'
    def retentionPolicy = 'autogen'
    def multiple = false
    def superItem = false
    influxLineProtocol(evt, measurementName, measurementType, multiple, retentionPolicy, superItem)
}

def handleColorMapEvent() {
    def measurementType = 'colorMap'
    def measurementName = 'values'
    def retentionPolicy = 'autogen'
    def multiple = false
    def superItem = false
    influxLineProtocol(evt, measurementName, measurementType, multiple, retentionPolicy, superItem)
}

// def handleJsonObjectEvent() { } // TODO

def handleDaylight(evt) {
    def measurementType = 'enum'
    def measurementName = 'states'
    def retentionPolicy = 'autogen'
    def multiple = false
    def superItem = false
    influxLineProtocol(event, measurementName, measurementType, multiple, retentionPolicy, superItem)
}

def handleHubStatus(evt) {
    if (evt.value == 'active' || evt.value == 'disconnected') {
        def measurementType = 'hub'
        def measurementName = 'hubStatus'
        def retentionPolicy = 'autogen'
        def multiple = false
        def superItem = false
        influxLineProtocol(evt, measurementName, measurementType, multiple, retentionPolicy, superItem)
    }
}

def pollLocations() {
    logger('pollLocations:', 'trace')
    def measurementType = 'local'
    def measurementName = 'locations'
    def retentionPolicy = 'metadata'
    def multiple = false
    def superItem = false
    def items = ['dummy'] // make new object (dummy) TODO
    influxLineProtocol(items, measurementName, measurementType, multiple, retentionPolicy, superItem) // only 1 location currently accessible by SmartApp instance (injected property = location installed)
}

def pollDevices() {
    logger('pollDevices:', 'trace')
    def measurementType = 'device'
    def measurementName = 'devices'
    def retentionPolicy = 'autogen' // TODO Check should it be 'metadata'?
    def multiple = true
    def superItem = false
    def items = getSelectedDevices()?.findAll { !it.displayName.startsWith('~') } // ?Needs 'get' because private method?
    influxLineProtocol(items, measurementName, measurementType, multiple, retentionPolicy, superItem)
}

def pollAttributes() {
    logger('pollAttributes:', 'trace')
    def measurementType = 'attribute'
    def measurementName = 'attributes'
    def retentionPolicy = 'metadata'
    def multiple = true
    getSelectedDevices()?.findAll { !it.displayName.startsWith('~') }.each { dev ->
        def items = getDeviceAllowedAttrs(dev?.id)
        def superItem = dev
        if (items) influxLineProtocol(items, measurementName, measurementType, multiple, retentionPolicy, superItem)
    }
}

// TODO Check that this works and then see about creating a collection of attributes

def pollZwaves() {
    logger('pollZwaves:', 'trace')
    def measurementType = 'zwave'
    def measurementName = 'devicesZw' // need to check this
    def retentionPolicy = 'metadata'
    def multiple = true
    def superItem = false
    def items = getSelectedDevices()?.findAll { !it.displayName.startsWith('~') && it?.getZwaveInfo().containsKey('zw') }
    influxLineProtocol(items, measurementName, measurementType, retentionPolicy, multiple, superItem)
}

def influxLineProtocol(items, measurementName, measurementType, multiple = false, retentionPolicy = 'autogen', superItem) {
    logger("influxLP: items: ${items}", 'trace')
    def influxLP = new StringBuilder()
    items.each { item ->
        influxLP.append(measurementName)
        tags().each { tag ->
            if ('all' in tag.type || measurementType in tag.type) {
                influxLP.append(",${tag.name}=")
                switch(tag.arguments) {
                    case 0:
                        try { influxLP.append("$tag.closure"()) }
                        catch(e) { logger("influxLP: Error with tag closure: ${tag.closure}", 'trace') }
                        break
                    case 1:
                        try {
                            if (superItem && !tag.sub) {
                                influxLP.append("$tag.closure"(superItem))
                            } else {
                                influxLP.append("$tag.closure"(item))
                            }
                        }
                        catch(e) { logger("influxLP: Error with tag closure: ${tag.closure}", 'trace') }
                        break
                    case 2:
                        try { influxLP.append("$tag.closure"(superItem, item)) }
                        catch(e) { logger("influxLP: Error with tag closure: ${tag.closure}", 'trace') }
                        break
                 }
            }
        }
        influxLP.append(' ')
        def fieldCount = 0
        fields().each { field ->
            if ('all' in field.type || measurementType in field.type) {
                influxLP.append((fieldCount) ? ',' : '')
                if (field.name) influxLP.append("${field.name}=")
                switch(field.arguments) {
                    case 0:
                        try { influxLP.append("$field.closure"()) }
                        catch(e) { logger("influxLP: Error with field closure: ${field.closure}", 'trace') }
                        break
                    case 1:
                        try {
                            if (superItem && !field.sub) {
                                influxLP.append("$field.closure"(superItem))
                            } else {
                                influxLP.append("$field.closure"(item))
                            }
                        }
                        catch(e) { logger("influxLP: Error with field closure: ${field.closure}", 'trace') }
                        break
                    case 2:
                        try { influxLP.append("$field.closure"(superItem, item)) }
                        catch(e) { logger("influxLP: Error with field closure: ${field.closure}", 'trace') }
                        break
                }
                if (field.valueType == 'integer') influxLP.append('i')
                fieldCount++
            }
        }
        if (item?.respondsTo('isStateChange')) {
            influxLP.append(' ')
            influxLP.append(timestamp(item))
        }
        if (multiple) influxLP.append('\n')
    }
    logger ("${influxLP.toString()}", 'trace')
/*
    if (!(timeElapsed < 500 && evt.value == pEvent.value)) {
        // ignores repeated propagation of an event (time interval < 0.5 s)
        postToInfluxDB(tags.toString(), retentionPolicy)
    def location = (state.databaseRemote) ? 'Remote' : 'Local'
    "postToInfluxDB${location}"(data, retentionPolicy)
    } else {
        logger("handleEnumEvent(): Ignoring duplicate event $evt.displayName ($evt.name) $evt.value", 'warn')
    }
*/
}

def tags() { [
        [name: 'area', closure: 'locationName', arguments: 0, type: ['all']],
        [name: 'areaId', closure: 'locationId', arguments: 0, type: ['all']],
        [name: 'building', closure: 'hubName', arguments: 0, type: ['all']],
        [name: 'buildingId', closure: 'hubId', arguments: 0, type: ['all']],
        [name: 'chamber', closure: 'groupName', arguments: 1, type: ['attribute', 'colorMap', 'device', 'enum', 'number', 'string', 'vector3', 'zwave']],
        [name: 'chamberId', closure: 'groupId', arguments: 1, type: ['attribute', 'colorMap', 'device', 'enum', 'number', 'string', 'vector3', 'zwave']],
        [name: 'deviceCode', closure: 'deviceCode', arguments: 1, type: ['attribute', 'colorMap', 'device', 'enum', 'number', 'string', 'vector3', 'zwave']],
        [name: 'deviceId', closure: 'deviceId', arguments: 1, type: ['attribute', 'colorMap', 'device', 'enum', 'number', 'string', 'vector3', 'zwave']],
        [name: 'deviceLabel', closure: 'deviceLabel', arguments: 1, type: ['attribute', 'colorMap', 'device', 'enum', 'number', 'string', 'vector3', 'zwave']],
        [name: 'deviceType', closure: 'deviceType', arguments: 1, type: ['attribute', 'colorMap', 'device', 'zwave']],
        [name: 'event', closure: 'eventName', arguments: 1, type: ['attribute', 'colorMap', 'enum', 'number', 'string', 'vector3'], sub: true],
        [name: 'eventType', closure: 'eventType', arguments: 1, type: ['attribute', 'colorMap', 'enum', 'number', 'string', 'vector3', ], sub: true], // ? rename to eventClass ?
        [name: 'hubStatus', closure: 'hubStatus', arguments: 0, type: ['local']],
        [name: 'hubType', closure: 'hubType', arguments: 0, type: ['local']],
        [name: 'identifierGlobal', closure: 'identifierGlobal', arguments: 1, type: ['device', 'colorMap', 'enum', 'number', 'string', 'vector3', 'zwave']], // removed 'attribute' for now
        [name: 'identifierLocal', closure: 'identifierLocal', arguments: 1, type: ['attribute', 'colorMap', 'device', 'enum', 'number', 'string', 'vector3', 'zwave']],
        [name: 'isChange', closure: 'isChange', arguments: 1, type: ['colorMap', 'enum', 'number', 'string', 'vector3']], // ??Handle null values? or does it always have a value?
        [name: 'onBattery', closure: 'onBattery', arguments: 0, type: ['local']], // check this out
        [name: 'power', closure: 'power', arguments: 1, type: ['zwave']],
        [name: 'secure', closure: 'secure', arguments: 1, type: ['zwave']],
        [name: 'source', closure: 'source', arguments: 1, type: ['enum', 'number', 'vector3']],
        [name: 'status', closure: 'status', arguments: 1, type: ['attribute', 'device', 'zwave']], // TODO ?Included
        [name: 'type', closure: 'zwType', arguments: 0, type: ['zwave']],
        [name: 'timeElapsed', closure: 'daysElapsed', arguments: 2, type: ['attribute']],
        [name: 'timeZone', closure: 'timeZoneCode', arguments: 0, type: ['local']],
        [name: 'unit', closure: 'unit', arguments: 1, type: ['number', 'vector3']],
] }

def getLocationName() { return { -> location.name.replaceAll(' ', '\\\\ ') } }

def getLocationId() { return { -> location.id } }

def getHubName() { return { -> location.hubs[0].name.replaceAll(' ', '\\\\ ') } }

def getHubId() { return { -> location.hubs[0].id } }

def getGroupName() { return { (state?.groupNames?."${groupId(it)}".replaceAll(' ', '\\\\ ')) ?: state.houseType } }

def getGroupId() { return {
    if (it?.respondsTo('isStateChange')) {
        it?.device?.device?.groupId // ?: 'unassigned' for event objects TODO
    }
    else {
        it?.device?.groupId // ?: 'unassigned' for everything else TODO
    }
} }

def getDeviceCode() { return {
    if (it?.respondsTo('isStateChange')) {
        return (it?.device?.device?.name?.replaceAll(' ', '\\\\ ')) ?: 'unassigned' // for event objects
    }
    else {
        return (it?.name?.replaceAll(' ', '\\\\ ')) ?: 'unassigned' // for everything else
    }
} }

def getDeviceId() { return {
    if (it?.respondsTo('isStateChange')) {
        return it.deviceId // for event objects
    } else {
        return it?.id // for everything else
    }
} }

def getDeviceLabel() { return {
    if (it?.respondsTo('isStateChange')) {
        return (it?.device?.device?.label?.replaceAll(' ', '\\\\ ')) ?: 'unassigned' // for event objects
    } else {
        return (it?.label?.replaceAll(' ', '\\\\ ')) ?: 'unassigned' // for everything else
    }
} }

def getDeviceType() { return { it?.typeName.replaceAll(' ', '\\\\ ') } }

def getEventName() { return {
    if (it?.respondsTo('isStateChange')) {
        if (it?.name in ['sunrise', 'sunset']) {
            return 'daylight'
        } else {
            return it?.name
        }
    } else {
        return it
    }
} }

def getEventDetails() { return { getAttributeDetail().find { ad -> ad.key == eventName(it) }.value } }

def getEventType() { return { eventDetails(it).type } }

def getHubStatus() { return { -> location.hubs[0].status } }

def getHubType() { return { -> location.hubs[0].type } }

def getIdentifierGlobal() { return { "${locationName()}\\ .\\${hubName()}\\ .\\ ${identifierLocal(it)}\\ .\\ ${eventName(it)}" } }

def getIdentifierLocal() { return { "${groupName(it)}\\ .\\ ${deviceLabel(it)}" } }

def getIsChange() { return { it?.isStateChange } } // ??Handle null values? or does it always have a value?

def getOnBattery() { return { -> location.hubs[0].hub.getDataValue('batteryInUse') } }

def getPower() { return {
    def power = it?.getZwaveInfo()?.zw.take(1)
    switch(power) {
        case 'L':
            return 'Listening'; break
        case 'S':
            return 'Sleepy'; break
        case 'B':
            return 'Beamable'; break
    }
} }

def getSecure() { return { (it?.getZwaveInfo()?.zw.endsWith('s')) ? 'true' : 'false' } }

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

def getStatus() { return { it?.status } } // TODO - use switch to convert to lowercase (ONLINE/OFFLINE)

def getDaysElapsed() { return { dev, attr ->
    if (dev?.latestState(attr)) {
        def daysElapsed = ((new Date().time - dev.latestState(attr).date.time) / 86_400_000) / 30
        daysElapsed = daysElapsed.toDouble().trunc().round()
        return "${daysElapsed * 30}-${(daysElapsed + 1) * 30} days"
    } else {
        return null
    }
} }

def getTimeZoneCode() { return { -> "${location.timeZone.ID}" } }

def getUnit() { return {
    def unit = (it?.unit) ? it.unit : eventDetails(it).unit
    // threeaxes unit is 'g'
    if (it.name == 'temperature') unit.replaceAll('\u00B0', '') // remove circle from C unit
    unit
} }

def getZwInfo() { return { it?.getZwaveInfo().clone() } }

def getZwType() { return { 'zwave' } } // getZwType() is a valid ST method


def fields() { [
        [name: '', closure: 'configuredParameters', valueType: 'string', arguments: 1, type: ['zwave']],
        [name: 'checkInterval', closure: 'checkInterval', valueType: 'integer', arguments: 1, type: ['zwave']],
        [name: 'eventDescription', closure: 'eventDescription', valueType: 'string', arguments: 1, type: ['colorMap', 'enum', 'number', 'string', 'vector3']],
        [name: 'eventId', closure: 'eventId', valueType: 'string', arguments: 1, type: ['colorMap', 'enum', 'number', 'string', 'vector3']],
        [name: 'firmwareVersion', closure: 'firmware', valueType: 'string', arguments: 0, type: ['local']],
        [name: 'hubIP', closure: 'hubIP', valueType: 'string', arguments: 0, type: ['local']],
        [name: 'latitude', closure: 'latitude', valueType: 'string', arguments: 0, type: ['local']],
        [name: 'longitude', closure: 'longitude', valueType: 'string', arguments: 0, type: ['local']],
        [name: 'nBinary', closure: 'currentStateBinary', valueType: 'boolean', arguments: 1, type: ['day', 'hub', 'enum']],
        [name: 'nLevel', closure: 'currentStateLevel', valueType: 'integer', arguments: 1, type: ['day', 'hub', 'enum']],
        [name: 'nState', closure: 'currentState', valueType: 'string', arguments: 1, type: ['day', 'hub', 'enum']],
        [name: 'nString', closure: 'currentString', valueType: 'string', arguments: 1, type: ['string']],
        [name: 'nText', closure: 'currentStateDescription', valueType: 'string', arguments: 1, type: ['enum']],
        [name: 'nText', closure: 'currentValueDescription', valueType: 'string', arguments: 1, type: ['number']],
        [name: 'nValue', closure: 'currentValue', valueType: 'float', arguments: 1, type: ['number']],
        [name: 'nValueDisplay', closure: 'currentValueDisplay', valueType: 'float', arguments: 1, type: ['number']],
        [name: 'nValueHue', closure: 'currentValueHue', valueType: 'integer', arguments: 1, type: ['colorMap']],
        [name: 'nValueSat', closure: 'currentValueSat', valueType: 'integer', arguments: 1, type: ['colorMap']],
        [name: 'nValueX', closure: 'currentValueX', valueType: 'float', arguments: 1, type: ['vector3']],
        [name: 'nValueY', closure: 'currentValueY', valueType: 'float', arguments: 1, type: ['vector3']],
        [name: 'nValueZ', closure: 'currentValueZ', valueType: 'float', arguments: 1, type: ['vector3']],
        [name: 'pBinary', closure: 'previousStateBinary', valueType: 'boolean', arguments: 1, type: ['enum']],
        [name: 'pLevel', closure: 'previousStateLevel', valueType: 'integer', arguments: 1, type: ['enum']],
        [name: 'portTCP', closure: 'portTCP', valueType: 'integer', arguments: 0, type: ['local']],
        [name: 'pState', closure: 'previousState', valueType: 'string', arguments: 1, type: ['enum']],
        [name: 'pText', closure: 'previousStateDescription', valueType: 'string', arguments: 1, type: ['enum']],
        [name: 'pText', closure: 'previousValueDescription', valueType: 'string', arguments: 1, type: ['number']],
        [name: 'pValue', closure: 'previousValue', valueType: 'float', arguments: 1, type: ['number']],
        [name: 'rChange', closure: 'difference', valueType: 'float', arguments: 1, type: ['number']],
        [name: 'rChangeText', closure: 'differenceText', valueType: 'string', arguments: 1, type: ['number']],
        [name: 'statusLevel', closure: 'statusLevel', valueType: 'integer', arguments: 1, type: ['device']], // TODO Convert to a tag?
        [name: 'sunrise', closure: 'sunrise', valueType: 'string', arguments: 0, type: ['local']],
        [name: 'sunset', closure: 'sunset', valueType: 'string', arguments: 0, type: ['local']],
        [name: 'tDay', closure: 'timeOfDay', valueType: 'integer', arguments: 1, type: ['enum', 'number']],
        [name: 'tElapsed', closure: 'timeElapsed', valueType: 'integer', arguments: 1, type: ['enum', 'number']],
        [name: 'tElapsedText', closure: 'timeElapsedText', valueType: 'string', arguments: 1, type: ['enum', 'number']],
        [name: 'timeLastEvent', closure: 'timeLastEvent', valueType: 'integer', arguments: 2, type: ['attribute']],
        [name: 'timestamp', closure: 'timestamp', valueType: 'integer', arguments: 1, type: ['enum', 'number', 'vector3']],
        [name: 'tOffset', closure: 'currentTimeOffset', valueType: 'integer', arguments: 1, type: ['enum']],
        [name: 'tWrite', closure: 'timeWrite', valueType: 'integer', arguments: 0, type: ['enum', 'number', 'vector3']],
        [name: 'valueLastEvent', closure: 'valueLastEvent', valueType: 'string', arguments: 2, type: ['attribute']],
        [name: 'wLevel', closure: 'weightedLevel', valueType: 'integer', arguments: 1, type: ['enum']],
        [name: 'wValue', closure: 'weightedValue', valueType: 'float', arguments: 1, type: ['number']],
        [name: 'zigbeePowerLevel', closure: 'zigbeePowerLevel', valueType: 'integer', arguments: 0, type: ['local']],
        [name: 'zwavePowerLevel', closure: 'longitude', valueType: 'string', arguments: 0, type: ['local']],
        [name: '', closure: 'ccList', valueType: 'string', arguments: 1, type: ['zwave']],
] }

def getCcList() { return {
    def info = it?.getZwaveInfo().clone() // TODO rewrite this using collect filters? so as to avoid need for cloning
    def cc = info.cc
    cc?.addAll(info?.ccOut)
    cc?.addAll(info?.sec)
    def ccList = 'zz' + cc.sort().join('=true,zz') + '=true'
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

def getCurrentEventValue() { return {
    if (it?.name in ['sunrise', 'sunset']) {
        return it.name
    } else {
        return it.value
    }
} }

def getCurrentState() { return { "\"${getCurrentEventValue(it)}\"" } }

def getCurrentStateBinary() { return { (currentStateLevel(it) > 0) ? 'true' : 'false' } }

def getCurrentStateLevel() { return { attributeStates(it).find { level -> level.key == getCurrentEventValue(it) }.value } }

def getAttributeStates() { return { eventDetails(it).levels } } // Lookup array for event state levels

def getCurrentStateDescription() { return {
    def text = "\"At ${locationName()}, in ${hubName()}, ${deviceLabel(it)} is ${currentState(it)} in the ${groupName(it)}.\""
    text.replaceAll('\\\\', '')
} }

def getCurrentString() { return { "\"${it.value}\"" } }

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

def getCurrentColorMap() { return { parseJson(it) } }

def getCurrentValueHue() { return { getCurrentColorMap(it).hue } }
def getCurrentValueSat() { return { getCurrentColorMap(it).saturation } }

def getCurrentValueX() { return { it.xyzValue.x / gravityFactor() } }
def getCurrentValueY() { return { it.xyzValue.y / gravityFactor() } }
def getCurrentValueZ() { return { it.xyzValue.z / gravityFactor() } }

def getGravityFactor() { return { -> (1024) } }

def getFirmware() { return { -> "\"${location.hubs[0].firmwareVersionString}\"" } }

def getHub() { return { -> location.hubs[0] } }

def getHubIP() { return { -> "\"${location.hubs[0].localIP}\"" } }

def getLatitude() { return { -> "\"${location.latitude}\"" } }

def getLongitude() { return { -> "\"${location.longitude}\"" } }

def getPortTCP() { return { -> location.hubs[0].localSrvPortTCP } }

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
    def changeAbs = (differenceText(it) == 'unchanged') ? 'unchanged' : "${differenceText(it)} by ${difference(it).abs()} ${unit(it)}"
    "\"This is ${changeAbs} compared to ${timeElapsedText(it)}.\""
} }

def getDifferenceText() { return {
    def changeText = 'unchanged' // text description of change
    if (difference(it) > 0) changeText = 'increased'
    else if (difference(it) < 0) changeText = 'decreased'
    changeText
} }

def getStatusLevel() { return { (it?.status.toUpperCase() in ["ONLINE"]) ? 1 : 0 } }

def getSunrise() { return { -> "\"${getSunriseAndSunset().sunrise.format('HH:mm', location.timeZone)}\"" } }

def getSunset() { return { -> "\"${getSunriseAndSunset().sunset.format('HH:mm', location.timeZone)}\"" } }

def getTimestamp() { return { it.date.time - currentTimeOffset(it) } }

def getTimeOffsetAmount() { return { -> (1000 * 10 / 2) } }

def getCurrentTimeOffset() { return { (eventName(it) == 'motion' && currentState(it) == "\"inactive\"") ? timeOffsetAmount() : 0 } }

def getPreviousTimeOffset() { return { (eventName(it) == 'motion' && previousState(it) == "\"inactive\"") ? timeOffsetAmount() : 0 } }

def getTimeOfDay() { return { timestamp(it) - it.date.clone().clearTime().time } } // calculate time of day in elapsed milliseconds

def getTimeElapsed() { return { timestamp(it) - previousEvent(it).date.time - previousTimeOffset(it) } }

def getTimeElapsedText() { return {
    def time = timeElapsed(it) / 1000
    def phrase
    switch (time) {
        case { it < 60 } :
            phrase = Math.round(time) + ' seconds ago'; break
        case { it < 90 } :
            phrase = Math.round(time / 60) + ' minute ago'; break
        case { it < 3600 } :
            phrase = Math.round(time / 60) + ' minutes ago'; break
        case { it < 5400 } :
            phrase = Math.round(time / 3600) + ' hour ago'; break
        case { it < 86400 } :
            phrase = Math.round(time / 3600) + ' hours ago'; break
        case { it < 129600 } :
            phrase = Math.round(time / 86400) + ' day ago'; break
        default :
            phrase = Math.round(time / 86400) + ' days ago'; break
    }
    phrase
} }

def getTimeLastEvent() { return { dev, attr ->
    if (dev?.latestState(attr)) {
        return dev.latestState(attr).date.time
    } else {
        return 0
    }
} }

def getTimeWrite() { return { -> new Date().time } } // time of processing the event

def getValueLastEvent() { return { dev, attr ->
    if (dev?.latestState(attr)) {
        return "\"${dev.latestState(attr).value}\""
    } else {
        return 'null'
    }
} }

def getWeightedLevel() { return {  previousStateLevel(it) * timeElapsed(it) } }

def getWeightedValue() { return {  previousValue(it) * timeElapsed(it) } }

def getZigbeePowerLevel() { return { -> location.hubs[0].hub.getDataValue('zigbeePowerLevel') } }

def getZwavePowerLevel() { return { -> location.hubs[0].hub.getDataValue('zwavePowerLevel') } }

/*****************************************************************************************************************
 *  Main Commands:
 *****************************************************************************************************************/
def postToInfluxDBLocal(data, retentionPolicy = 'autogen') {
    try {
        def query = state.query.clone()
        query.rp = retentionPolicy
        def hubAction = new physicalgraph.device.HubAction([
            method : 'POST',
            headers: state.headers,
            path   : state.path,
            query  : query,
            body   : data],
            null,
            [callback: handleInfluxResponseLocal]
        )
        logger("postToInfluxDB(): Posting data to InfluxDB: Headers: ${state.headers}, Path: ${state.path}, Query: ${query}, Data: ${data}", 'info')
        sendHubCommand(hubAction)
    }
    catch (e) {
        logger("postToInfluxDB(): Exception ${e} on ${hubAction}", 'error')
    }
}

def handleInfluxResponseLocal(physicalgraph.device.HubResponse hubResponse) { // TODO - Check / tidy up
    if (hubResponse.status == 204) logger("postToInfluxDBLocal: Success! Response from InfluxDB: Status: ${hubResponse.status}, Headers: ${hubResponse.headers}", 'trace')
    if (hubResponse.status >= 400) logger("postToInfluxDBLocal: Something went wrong! Response from InfluxDB: Status: ${hubResponse.status}, Headers: ${hubResponse.headers}, Body: ${hubResponse.data}", 'error')
}

def postToInfluxDBRemote(data, retentionPolicy = 'autogen') {
    def query = state.query.clone()
    query.rp = retentionPolicy
    def params = [
        uri               : state.uri,
        path              : state.path,
        query             : query,
        contentType       : "application/x-www-form-urlencoded",
        requestContentType: "application/x-www-form-urlencoded",
        body              : data
    ]
    logger("postToInfluxDB(): Posting data to InfluxDB: Uri: ${state.uri}, Path: ${state.path}, Query: ${query}, Data: ${data}", 'info')
    asynchttp_v1.post(handleInfluxResponseRemote, params)
}

def handleInfluxResponseRemote(response, requestdata) { // TODO - Check / tidy up
    if (response.status == 204) logger("postToInfluxDBRemote: Success! Response from InfluxDB: Status: ${response.status}, Headers: ${response.headers}, Body: ${requestdata}", 'trace')
    if (response.status >= 400) logger("postToInfluxDBRemote: Something went wrong! Response from InfluxDB: Status: ${response.status}, Headers: ${response.headers}, Body: ${requestdata}", 'error')
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/
private manageSchedules() {
    logger('manageSchedules', 'trace')

    try { unschedule(pollLocations) }
    catch (e) { logger('manageSchedules: Unschedule pollLocation failed!', 'error') }
    // runEvery3Hours(pollLocations)
    runEvery15Minutes(pollLocations)

    try { unschedule(pollDevices) }
    catch (e) { logger('manageSchedules: Unschedule pollDevices failed!', 'error') }
    // runEvery3Hours(pollDevices)
    runEvery15Minutes(pollDevices)

    try { unschedule(pollAttributes) }
    catch (e) { logger('manageSchedules: Unschedule pollAttributes failed!', 'error') }
    // runEvery3Hours(pollAttributes)
    runEvery15Minutes(pollAttributes)

    try { unschedule(pollZwaves) }
    catch (e) { logger('manageSchedules: Unschedule pollZwaves failed!', 'error') }
    // runEvery3Hours(pollZwaves)
    runEvery15Minutes(pollZwaves)
}


private manageSubscriptions() { // Configures subscriptions
    logger('manageSubscriptions:', 'trace')
    unsubscribe()
    getSelectedDevices()?.each { dev ->
        if (!dev.displayName.startsWith("~")) {
            getDeviceAllowedAttrs(dev?.id)?.each { attr ->
                if (dev?.hasAttribute("${attr}")) { // select only attributes that exist
                    def type = getAttributeDetail().find { it.key == attr }.value.type
                    switch(type) {
                        case 'enum':
                        logger("manageSubscriptions: Subscribing 'handleEnumEvent' listener to attribute: ${attr}, for device: ${dev}", 'info')
                        subscribe(dev, attr, handleEnumEvent); break
                        case 'number':
                        logger("manageSubscriptions: Subscribing 'handleNumberEvent' listener to attribute: ${attr}, for device: ${dev}", 'info')
                        subscribe(dev, attr, handleNumberEvent); break
                        case 'vector3':
                        logger("manageSubscriptions: Subscribing 'handleVector3Event' listener to attribute: ${attr}, for device: ${dev}", 'info')
                        subscribe(dev, attr, handleVector3Event); break
                        case 'string':
                            logger("manageSubscriptions: Subscribing 'handleStringEvent' listener to attribute: ${attr}, for device: ${dev}", 'info')
                        subscribe(dev, attr, handleStringEvent); break
                        case 'colorMap':
                        logger("manageSubscriptions: Subscribing 'handleColorMapEvent' listener to attribute: ${attr}, for device: ${dev}", 'info')
                        subscribe(dev, attr, handleColorMapEvent); break
                        case 'json_object':
                        logger("manageSubscriptions: Subscribing 'handleJson_objectEvent' listener to attribute: ${attr}, for device: ${dev}", 'info')
                        // subscribe(dev, attr, handleJsonObjectEvent); break TODO - write handler (if needed)
                    }
                }
            }
        }
    }

    logger("manageSubscriptions: Subscribing 'handleDaylight' listener to 'Sunrise' and 'Sunset' events", 'info')
    subscribe(location, 'sunrise', handleDaylight)
    subscribe(location, 'sunset', handleDaylight)

    logger("manageSubscriptions: Subscribing 'handleHubStatus' listener to 'Hub Status' events", 'info')
    subscribe(location.hubs[0], 'hubStatus', handleHubStatus)

    logger('manageSubscriptions: Building state map of group Ids and group names', 'info')
    if (settings.bridgePref) {
        def groupId
        def groupName
        settings.bridgePref.each {
            if (it.name?.take(1) == '~') {
                groupId = it.device?.groupId
                groupName = it.name?.drop(1) // .replaceAll(' ', '\\\\ ') // stoppped replaceAll to generalise
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
    deviceAllowedAttrs
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
                logger("Error while finding supported devices for ${attr}: ${e.message}", 'warn')
            }
        }
    }
    supportedAttributes?.unique()?.sort()
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
            logger("Error while getting attributes for capability ${cap}: ${e.message}", 'warn')
        }
    }
    attributes
}

private getSelectedDeviceNames() {
    try {
        return getSelectedDevices()?.collect { it?.displayName }?.sort()
    }
    catch (e) {
        logger("Error while getting selected device names: ${e.message}", 'warn')
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
            logger("Error while getting selected devices for capability ${it}: ${e.message}", 'warn')
        }
    }
    devices?.flatten()?.unique { it.id }
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
            color                   : [type: 'colorMap'],
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