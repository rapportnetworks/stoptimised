/*****************************************************************************************************************
 *  Copyright Alasdair Thin
 *
 *  Name: InfluxDB Smart Logger
 *
 *  Date: 2019-10-09
 *
 *  Version: 3.0
 *
 *  Author: Alasdair Thin
 *
 *  Description: A SmartApp to log SmartThings device states to an InfluxDB database.
 *
 *****************************************************************************************************************/

definition(
        name        : 'InfluxDB Smart Logger v3',
        namespace   : 'rapportnetworks',
        author      : 'Alasdair Thin',
        description : 'Log SmartThings device states to InfluxDB',
        category    : 'Health & Wellness',
        iconUrl     : 'https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-30DayGoals.png',
        iconX2Url   : 'https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-30DayGoals@2x.png',
        iconX3Url   : 'https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-30DayGoals@2x.png',
)

include 'asynchttp_v1'

preferences {
    page(name: 'mainPage')
    page(name: 'devicesPage')
    page(name: 'attributesPage')
}

def mainPage() {
    dynamicPage(
            name      : 'mainPage',
            uninstall : true,
            install   : true,
    ) {
        section('Logging settings') {
            input(
                    name: 'logLevelIDE',
                    title: 'IDE Logging Level',
                    type: 'enum',
                    options: [0: 'None', 1: 'Error', 2: 'Warning', 3: 'Info', 4: 'Debug', 5: 'Trace'],
                    defaultValue: 3,
                    displayDuringSetup: true,
                    required: false,
            )

            input(
                    name: 'logIdent',
                    title: 'Log Data Identifiers',
                    type: 'bool',
                    defaultValue: true,
                    required: false,
                    displayDuringSetup: true,
            )

            input(
                    name: 'logLevelDB',
                    title: 'Database Logging Level',
                    type: 'enum',
                    options: [1: 'Minimal', 2: 'Intermediate', 3: 'All'],
                    defaultValue: 1,
                    displayDuringSetup: true,
                    required: false,
            )
        }

        section('Measurements to Log') {
            input(
                    name         : 'logEvents',
                    title        : 'Events',
                    description  : '',
                    type         : 'bool',
                    defaultValue : false,
                    required     : false,
            )

            input(
                    name         : 'logMetadata',
                    title        : 'Metadata',
                    description  : '',
                    type         : 'bool',
                    defaultValue : false,
                    required     : false,
            )

            input(
                    name         : 'logStatuses',
                    title        : 'Statuses',
                    description  : '',
                    type         : 'bool',
                    defaultValue : false,
                    required     : false,
            )

            input(
                    name         : 'logConfigs',
                    title        : 'Configurations',
                    description  : '',
                    type         : 'bool',
                    defaultValue : false,
                    required     : false,
            )
        }

        section('Influx Database settings') {
            input(
                    name         : 'dbVersion',
                    type         : 'number',
                    title        : 'Database version',
                    range        : '1..2',
                    defaultValue : 2,
                    required     : false,
            )

            input(
                    name         : 'dbRemote',
                    type         : 'bool',
                    title        : 'Remote Database',
                    defaultValue : true,
                    required     : false,
            )

            input(
                    name         : 'dbSSL',
                    type         : 'bool',
                    title        : 'Encrypted Connection',
                    defaultValue : true,
                    required     : false,
            )

            input(
                    name           : 'dbHost',
                    type           : 'text',
                    title          : 'Host',
                    capitalization : 'none',
                    autoCorrect    : false,
                    required       : true,
            )

            input(
                    name         : 'dbPort',
                    type         : 'number',
                    title        : 'Port',
                    defaultValue : '443',
                    required     : false,
            )

            input(
                    name           : 'dbName',
                    type           : 'text',
                    title          : 'Database (v1)',
                    capitalization : 'none',
                    autoCorrect    : false,
                    required       : false,
            )

            input(
                    name           : 'dbUsername',
                    type           : 'text',
                    title          : 'Username (v1)',
                    capitalization : 'none',
                    autoCorrect    : false,
                    required       : false,
            )

            input(
                    name     : 'dbPassword',
                    type     : 'password',
                    title    : 'Password (v1)',
                    required : false,
            )

            input(
                    name           : 'dbOrganization',
                    type           : 'text',
                    title          : 'Organization (v2)',
                    capitalization : 'none',
                    autoCorrect    : false,
                    required       : false,
            )

            input(
                    name     : 'dbToken',
                    type     : 'password',
                    title    : 'Authorisation Token (v2)',
                    required : false,
            )
        }

        if (state.devicesConfigured) {
            section('User Selected Devices') {
                getPageLink(
                        'devicesPageLink',
                        'Tap to change',
                        'devicesPage',
                        null,
                        buildSummary(selectedDeviceNames),
                        null,
                )
            }
        } else {
            devicesPageContent
        }

        if (state.attributesConfigured) {
            section('User Selected Events') {
                getPageLink(
                        'attributesPageLink',
                        'Tap to change',
                        'attributesPage',
                        null,
                        buildSummary(settings?.allowedAttributes?.sort()),
                        null,
                )
            }
        } else {
            attributesPageContent
        }
    }
}

def devicesPage() {
    dynamicPage(name: 'devicesPage') {
        devicesPageContent
    }
}

private getDevicesPageContent() {
    section('Choose Devices') {
        paragraph(
                'Select devices to log. Each device only needs to be selected once and should be in either the Actuators or Sensors fields at the top.'
        )

        capabilities.each {
            try {
                input(
                        "${it.cap}Pref", // ?not name
                        "capability.${it.cap}", // ?not type
                        title          : "${it.title}",
                        multiple       : true,
                        hideWhenEmpty  : true,
                        required       : false,
                        submitOnChange : true,
                )
            }
            catch (e) {
                logger("preferences: Failed to create input for capability: ${it} - ${e.message}", 'error')
            }
        }
    }
}

def attributesPage() {
    dynamicPage(name: 'attributesPage') {
        attributesPageContent
    }
}

private getAttributesPageContent() {
    def supportedAttr = supportedAttributes?.sort()
    if (supportedAttr) {
        section('Choose Events') {
            paragraph(
                    'Select all the events that should get logged, depending on devices supporting them.'
            )

            input(
                    name           : 'allowedAttributes',
                    type           : 'enum',
                    title          : 'Events to log',
                    required       : true,
                    multiple       : true,
                    submitOnChange : true,
                    options        : supportedAttr,
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

private getPageLink(linkName, linkText, pageName, args = null, desc = "", image = null) {
    def map = [
            name        : "$linkName",
            title       : "$linkText",
            description : "$desc",
            page        : "$pageName",
            required    : false,
    ]
    if (args) map.params = args
    if (image) map.image = image
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
    state.logLevelIDE = 5
    logger("installed: ${app.label} installed with settings: ${settings}", 'trace')
}

def uninstalled() { // runs when the app is uninstalled
    logger("uninstalled:", 'trace')
}

def updated() { // runs when app settings are changed
    logger('updated: Setting IDE logging level', 'trace')
    state.logLevelIDE = (settings?.logLevelIDE) ? settings.logLevelIDE.toInteger() : 3

    /**
     * sets logging of identifiable data (true, false)
     */
    state.logIdent = settings?.logIdent ?: true

    /**
     * sets level of tags and fields logged to database (1: 'Minimal', 2: 'Intermediate', 3: 'All')
     */
    state.logLevelDB = (settings?.logLevelDB) ? settings.logLevelDB.toInteger() : 1

    /**
     * if all database logging options are off, then influx line protcol will still be assembled, but only shown in IDE
     */
    if (!settings?.logEvents && !settings?.logMetadata && !settings?.logStatuses && !settings?.logConfigs) {
        state.logToDB     = false
        state.logEvents   = true
        state.logMetadata = true
        state.logStatuses = true
        state.logConfigs  = true
    }
    else {
        state.logToDB     = true
        state.logEvents   = settings?.logEvents   ?: false
        state.logMetadata = settings?.logMetadata ?: false
        state.logStatuses = settings?.logStatuses ?: false
        state.logConfigs  = settings?.logConfigs  ?: false
    }

    /**
     * create uri depending on database (Local | Remote) and whether using SSL
     */
    if (settings?.dbRemote) {
        state.uri = "http${(settings?.dbSSL) ? 's' : ''}://${settings?.dbHost}:${settings?.dbPort}"
    }
    else {
        state.uri = "${settings?.dbHost}:${settings?.dbPort}"
    }

    /**
     * sets parameter for determining which post method to call
     */
    state.dbLocation = (settings?.dbRemote) ? 'Remote' : 'Local'

    logger('updated: Creating map of group Ids and group names', 'debug')
    state.dwellingType = 'House'
    state.groupNames = [:]
    if (settings?.bridgePref) {
        def groupId
        def groupName
        settings.bridgePref.each {
            if (it.name?.take(1) == '~') {
                groupId = it.device?.groupId
                groupName = it.name?.drop(1)
                if (groupId) state.groupNames << [(groupId): groupName]
            }
        }
    }

    if (getSelectedDevices()) {
        logger('updated: Configured - Devices Selected', 'trace')
        state.devicesConfigured = true
    } else {
        logger('updated: Unconfigured - Choose Devices', 'debug')
    }

    if (settings?.allowedAttributes) {
        logger('updated: Configured - Events Selected', 'trace')
        state.attributesConfigured = true
    } else {
        logger('updated: Unconfigured - Choose Events', 'debug')
    }

    manageSubscriptions()
    manageSchedules()

    logger('updated: Scheduling first run of poll methods', 'trace')
    def runInTime = 30
    def runInInterval = 30
    pollingMethods().each {
        runIn(runInTime, it.key)
        runInTime += runInInterval
    }

}

/*****************************************************************************************************************
 *  Event Handlers:
 *****************************************************************************************************************/
/*
def handleAppTouch(evt) { // Touch event on Smart App
    logger("handleAppTouch:", 'trace')
}
*/

def handleEnumEvent(evt) {
    if (state.logEvents) {
        def measurementType = 'enum'
        def measurementName = 'states'
        influxLineProtocol(evt, measurementName, measurementType)
    }
}

def handleNumberEvent(evt) {
    if (state.logEvents) {
        def measurementType = 'number'
        def measurementName = 'values'
        influxLineProtocol(evt, measurementName, measurementType)
    }
}

def handleVector3Event(evt) {
    if (state.logEvents) {
        def measurementType = 'vector3'
        def measurementName = 'threeaxes'
        influxLineProtocol(evt, measurementName, measurementType)
    }
}

def handleStringEvent(evt) {
    if (state.logEvents) {
        def measurementType = 'string'
        def measurementName = 'statuses'
        influxLineProtocol(evt, measurementName, measurementType)
    }
}

def handleColorMapEvent(evt) {
    if (state.logEvents) {
        def measurementType = 'colorMap'
        def measurementName = 'values'
        influxLineProtocol(evt, measurementName, measurementType)
    }
}

def handleJsonObjectEvent(evt) {
    // TODO - write handler if needed
}

def handleDaylight(evt) {
    if (state.logEvents) {
        def measurementType = 'day'
        def measurementName = 'daylight'
        influxLineProtocol(evt, measurementName, measurementType)
    }
}

def handleHubStatus(evt) {
    if (state.logEvents) {
        def measurementType = 'hub'
        def measurementName = 'hub'
        influxLineProtocol(evt, measurementName, measurementType)
    }
}

/*****************************************************************************************************************
 *  Poll Status:
 *****************************************************************************************************************/
def pollStatus() {
    pollStatusHubs()
    pollStatusDevices()
}

def pollStatusHubs() {
    logger('pollStatusHubs: running now', 'trace')
    if (state.logStatuses) {
        def measurementType = 'statHub'
        def measurementName = 'pollHubs'
        def bucket = 'statuses'
        def items = ['placeholder']
        influxLineProtocol(items, measurementName, measurementType, bucket)
    }
}

def pollStatusDevices() {
    logger('pollStatusDevices: running now', 'trace')
    if (state.logStatuses) {
        def measurementType = 'statDev'
        def measurementName = 'pollDevices'
        def bucket = 'statuses'
        def items = getSelectedDevices()?.findAll { !it.displayName.startsWith('~') }
        if (items) influxLineProtocol(items, measurementName, measurementType, bucket)
        // if (items) items.each { influxLineProtocol(it, measurementName, measurementType, bucket) }
    }
}

/*****************************************************************************************************************
 *  Poll Metadata:
 *****************************************************************************************************************/
def pollLocations() {
    logger('pollLocations: running now', 'trace')
    if (state.logMetadata) {
        def measurementType = 'local'
        def measurementName = 'areas'
        def retentionPolicy = 'metadata'
        def bucket = 'metadata'
        def items = ['placeholder'] // (only 1 location where Smart App is installed) so placeholder is needed)
        influxLineProtocol(items, measurementName, measurementType, bucket, retentionPolicy)
    }
}

def pollDevices() {
    logger('pollDevices: running now', 'trace')
    if (state.logMetadata) {
        def measurementType = 'device'
        def measurementName = 'devices'
        def retentionPolicy = 'metadata'
        def bucket          = 'metadata'
        def items = getSelectedDevices()?.findAll { !it.displayName.startsWith('~') }
        influxLineProtocol(items, measurementName, measurementType, bucket, retentionPolicy)
        // items.each { influxLineProtocol(it, measurementName, measurementType, bucket, retentionPolicy) }
    }
}

def pollAttributes() {
    logger('pollAttributes: running now', 'trace')
    if (state.logMetadata) {
        def measurementType = 'attribute'
        def measurementName = 'attributes'
        def retentionPolicy = 'metadata'
        def bucket          = 'metadata'
        getSelectedDevices()?.findAll { !it.displayName.startsWith('~') }.each { dev ->
            def parentItem = dev
            def items = getDeviceAllowedAttrs(dev) as List
            if (items) influxLineProtocol(items, measurementName, measurementType, bucket, retentionPolicy, parentItem)
            // if (items) { items.each { influxLineProtocol(["${it}"], measurementName, measurementType, bucket, retentionPolicy, parentItem) } }
        }
    }
}

def pollZwavesCCs() {
    logger('pollZwavesCCs: running now', 'trace')
    if (state.logConfigs) {
        def measurementType = 'zwCCs'
        def measurementName = 'zwaveCCs'
        def retentionPolicy = 'metadata'
        def bucket          = 'configs'
        def items = getSelectedDevices()?.findAll { !it.displayName.startsWith('~') && it?.getZwaveInfo().containsKey('zw') }
        if (items) influxLineProtocol(items, measurementName, measurementType, bucket, retentionPolicy)
        // if (items) items.each { influxLineProtocol(it, measurementName, measurementType, bucket, retentionPolicy) }
    }
}

def pollZwaves() {
    logger('pollZwaves: running now', 'trace')
    if (state.logConfigs) {
        def measurementType = 'zwave'
        def measurementName = 'zwave'
        def retentionPolicy = 'metadata'
        def bucket = 'configs'
        def items = getSelectedDevices()?.findAll { !it.displayName.startsWith('~') && it?.getZwaveInfo().containsKey('zw') }
        if (items) influxLineProtocol(items, measurementName, measurementType, bucket, retentionPolicy)
        // if (items) items.each { influxLineProtocol(it, measurementName, measurementType, bucket, retentionPolicy) }
    }
}

/*****************************************************************************************************************
 *  InfluxDB Line Protocol:
 *****************************************************************************************************************/
def influxLineProtocol(items, measurementName, measurementType, bucket = 'events', retentionPolicy = 'autogen', parentItem = false) {
    def influxLP = new StringBuilder()
    def eventId = 'notEvent'
    items.each { item ->
        influxLP.append(measurementName)
        tags().each { tag ->
            if (tag.level <= state.logLevelDB && (state.logIdent || !tag.ident) && ('all' in tag.type || measurementType in tag.type)) {
                switch (tag.args) {
                    case 0:
                        try {
                            tag.Value = "$tag.clos"()
                        }
                        catch (e) {
                            logger("influxLP: Error with tag closure 0 (${measurementType}): ${tag.clos}", 'error')
                        }
                        break
                    case 1:
                        try {
                            if (parentItem && tag.parent) {
                                tag.Value = "$tag.clos"(parentItem)
                            } else {
                                tag.Value = "$tag.clos"(item)
                            }
                        }
                        catch (e) {
                            logger("influxLP: Error with tag closure 1 (${measurementType}): ${tag.clos}", 'error')
                        }
                        break
                    case 2:
                        try {
                            tag.Value = "$tag.clos"(parentItem, item)
                        }
                        catch (e) {
                            logger("influxLP: Error with tag closure 2 (${measurementType}): ${tag.clos}", 'error')
                        }
                        break
                }
                if (tag.Value) {
                    influxLP.append(",${tag.name}=") // TODO - need to allow for "nameOld"
                    if (tag?.esc) {
                        influxLP.append("${tag.Value.replaceAll("'", '').replaceAll('"', '').replaceAll(',', '').replaceAll('=', '').replaceAll(' ', '\\\\ ')}")
                    } else {
                        influxLP.append(tag.Value)
                    }
                }
            }
        }
        influxLP.append(' ')
        def fieldCount = 0
        fields().each { field ->
            if (field.level <= state.logLevelDB && (state.logIdent || !field.ident) && ('all' in field.type || measurementType in field.type)) {
                switch (field.args) {
                    case 0:
                        try {
                            field.Value = "$field.clos"()
                        }
                        catch (e) {
                            logger("influxLP: Error with field closure 0 (${measurementType}): ${field.clos}", 'error')
                        }
                        break
                    case 1:
                        try {
                            if (parentItem && field.parent) {
                                field.Value = "$field.clos"(parentItem)
                            } else {
                                field.Value = "$field.clos"(item)
                            }
                        }
                        catch (e) {
                            logger("influxLP: Error with field closure 1 (${measurementType}): ${field.clos}", 'error')
                        }
                        break
                    case 2:
                        try {
                            field.Value = "$field.clos"(parentItem, item)
                        }
                        catch (e) {
                            logger("influxLP: Error with field closure 2 (${measurementType}): ${field.clos}", 'error')
                        }
                        break
                }
                if (field.Value || field.Value == 0) {

                    influxLP.append((fieldCount) ? ',' : '')

                    if (field.var != 'multiple') influxLP.append("${field.name}=") // TODO - need to allow for "nameOld"

                    if (field.var == 'string') {
                        influxLP.append('\"').append(field.Value).append('\"')
                    } else {
                        influxLP.append(field.Value)
                    }
                    if (field.var == 'integer') influxLP.append('i')

                    fieldCount++

                    if (field.name == 'eventId') eventId = "${field.Value}" // used to track logging to database and any associated errors
                }
            }
        }
        if (isEventObject(item)) {
            influxLP.append(' ').append(timestamp(item))
        }
        else {
            influxLP.append('\n')
        }
    }
/*
    if (!(timeElapsed < 500 && evt.value == pEvent.value)) {
        // ignores repeated propagation of an event (time interval < 0.5 s)
        postToInfluxDB...
    } else {
        logger("handleEnumEvent(): Ignoring duplicate event $evt.displayName ($evt.name) $evt.value", 'warn')
    }
*/
    if (state.logToDB) {
        "postToInfluxDB${state.dbLocation}"(influxLP.toString(), retentionPolicy, bucket, eventId)
    }
    else {
        logger("influxLineProtocol: ${influxLP.toString()}", 'debug')
    }
}

/*****************************************************************************************************************
 *  Tags Map:
 *****************************************************************************************************************/
def tags() { [
        [name: 'area',        level: 1, clos: 'locationName',              args: 0, type: ['all'], esc: true, ident: true,],
        [name: 'areaId',      level: 2, clos: 'locationId',                args: 1, type: ['all'], parent: true,],
        [name: 'building',    level: 1, clos: 'hubName',                   args: 0, type: ['all'], esc: true, ident: true,],
        [name: 'buildingId',  level: 2, clos: 'hubId',                     args: 1, type: ['all'], parent: true,],
        [name: 'chamber',     level: 1, clos: 'groupName',                 args: 1, type: ['attribute', 'colorMap', 'day', 'device', 'enum', 'hub', 'number', 'statDev', 'string', 'vector3', 'zwave', 'zwCCs'], parent: true, esc: true,],
        [name: 'chamberId',   level: 2, clos: 'groupId',                   args: 1, type: ['attribute', 'colorMap', 'device', 'enum', 'number', 'statDev', 'string', 'vector3', 'zwave', 'zwCCs'], parent: true,],
        [name: 'deviceCode',  level: 2, clos: 'deviceCode',                args: 1, type: ['attribute', 'colorMap', 'device', 'enum', 'number', 'statDev', 'string', 'vector3', 'zwave', 'zwCCs'], parent: true, esc: true, ident: true,],
        [name: 'deviceHand',  level: 1, clos: 'deviceHandlerName',         args: 1, type: ['attribute', 'device', 'zwave', 'zwCCs'], parent: true, esc: true,],
        [name: 'deviceId',    level: 2, clos: 'deviceId',                  args: 1, type: ['attribute', 'colorMap', 'device', 'enum', 'number', 'statDev', 'string', 'vector3', 'zwave', 'zwCCs'], parent: true,],
        [name: 'deviceLabel', level: 1, clos: 'deviceLabel',               args: 1, type: ['attribute', 'colorMap', 'device', 'enum', 'number', 'statDev', 'string', 'vector3', 'zwave', 'zwCCs'], parent: true, esc: true,],
        [name: 'deviceType',  level: 1, clos: 'deviceType',                args: 1, type: ['device','statDev'],],
        [name: 'event',       level: 1, clos: 'eventName',                 args: 1, type: ['attribute', 'colorMap', 'day', 'enum', 'hub', 'number', 'string', 'vector3'],],
        [name: 'eventType',   level: 1, clos: 'eventType',                 args: 1, type: ['attribute'],],
        [name: 'hubType',     level: 1, clos: 'hubType',                   args: 0, type: ['local', 'statHub'],],
        [name: 'identGlobal', level: 1, clos: 'identifierGlobalEvent',     args: 1, type: ['colorMap', 'day', 'enum', 'hub', 'number', 'string', 'vector3'], esc: true, ident: true,],
        [name: 'identGlobal', level: 1, clos: 'identifierGlobalHub',       args: 1, type: ['local', 'statHub'], esc: true, ident: true,],
        [name: 'identGlobal', level: 1, clos: 'identifierGlobalDevice',    args: 1, type: ['device', 'statDev', 'zwave', 'zwCCs'], esc: true, ident: true,],
        [name: 'identGlobal', level: 1, clos: 'identifierGlobalAttribute', args: 2, type: ['attribute'], esc: true, ident: true,],
        [name: 'identLocal',  level: 1, clos: 'identifierLocal',           args: 1, type: ['attribute', 'colorMap', 'device', 'enum', 'number', 'statDev', 'string', 'vector3'], parent: true, esc: true,  ident: true,],
        [name: 'listening',   level: 1, clos: 'zwaveListening',            args: 1, type: ['zwave', 'zwCCs'],],
        [name: 'power',       level: 1, clos: 'powerSource',               args: 1, type: ['device', 'statDev', 'zwave', 'zwCCs'],],
        [name: 'source',      level: 2, clos: 'source',                    args: 1, type: ['colorMap', 'enum', 'number', 'string', 'vector3'],],
        [name: 'status',      level: 1, clos: 'statusDevice',              args: 1, type: ['attribute', 'device', 'statDev', 'zwave', 'zwCCs'], parent: true,],
        [name: 'statusHub',   level: 1, clos: 'statusHub',                 args: 0, type: ['local', 'statDev', 'statHub'],],
        [name: 'tempScale',   level: 2, clos: 'tempScale',                 args: 0, type: ['local'],],
        [name: 'timeZone',    level: 2, clos: 'timeZoneName',              args: 0, type: ['local'],],
        [name: 'unit',        level: 1, clos: 'unit',                      args: 1, type: ['number', 'vector3'], esc: true,],
        // [name: 'isDigital',   level: 0, clos: 'isDigital',                 args: 1, type: ['colorMap', 'day', 'enum', 'hub', 'number', 'string', 'vector3'],], // unused?
        // [name: 'isPhysical',  level: 2, clos: 'isPhysical',                args: 1, type: ['colorMap', 'day', 'enum', 'hub', 'number', 'string', 'vector3'],], // unused?
] }

/*****************************************************************************************************************
 *  Fields Map:
 *****************************************************************************************************************/
def fields() { [
        [name: '',           level: 2, clos: 'configuredParametersList', var: 'multiple', args: 1, type: ['zwave'],],
        [name: 'battery',    level: 1, clos: 'battery',                  var: 'integer',  args: 1, type: ['device', 'statDev'],],
        [name: 'checkInt',   level: 1, clos: 'checkInterval',            var: 'integer',  args: 1, type: ['statDev','zwave'],],
        [name: 'configType', level: 1, clos: 'deviceConfigurationType',  var: 'string',   args: 1, type: ['zwave'],],
        [name: 'configure',  level: 1, clos: 'configure',                var: 'string',   args: 1, type: ['device', 'statDev', 'zwave'],],
        [name: 'deviceUse',  level: 1, clos: 'deviceUse',                var: 'string',   args: 1, type: ['zwave'],],
        [name: 'eventText',  level: 3, clos: 'eventDescription',         var: 'string',   args: 1, type: ['colorMap', 'day', 'enum', 'hub', 'number', 'string', 'vector3'],],
        [name: 'eventId',    level: 1, clos: 'eventId',                  var: 'string',   args: 1, type: ['colorMap', 'day', 'enum', 'hub', 'number', 'string', 'vector3'],],
        [name: 'firmware',   level: 1, clos: 'firmwareVersion',          var: 'string',   args: 0, type: ['local', 'statHub'],],
        [name: 'iP',         level: 1, clos: 'hubIPaddress',             var: 'string',   args: 0, type: ['local', 'statHub'],],
        [name: 'latitude',   level: 1, clos: 'latitude',                 var: 'float',    args: 0, type: ['local'], ident: true,],
        [name: 'longitude',  level: 1, clos: 'longitude',                var: 'float',    args: 0, type: ['local'], ident: true,],
        [name: 'messages',   level: 1, clos: 'messages',                 var: 'integer',  args: 1, type: ['statDev'],],
        [name: 'nBin',       level: 2, clos: 'currentStateBinary',       var: 'boolean',  args: 1, type: ['day', 'enum', 'hub'],],
        [name: 'nHue',       level: 1, clos: 'currentHue',               var: 'integer',  args: 1, type: ['colorMap'],],
        [name: 'nLevel',     level: 1, clos: 'currentStateLevel',        var: 'integer',  args: 1, type: ['day', 'enum', 'hub'],],
        [name: 'nSat',       level: 1, clos: 'currentSat',               var: 'integer',  args: 1, type: ['colorMap'],],
        [name: 'nState',     level: 1, clos: 'currentState',             var: 'string',   args: 1, type: ['day', 'enum', 'hub', 'string'],],
        [name: 'nText',      level: 3, clos: 'currentStateDescription',  var: 'string',   args: 1, type: ['enum'], ident: true,],
        [name: 'nText',      level: 3, clos: 'currentValueDescription',  var: 'string',   args: 1, type: ['number'], ident: true,],
        [name: 'nValue',     level: 1, clos: 'currentValue',             var: 'float',    args: 1, type: ['number'],],
        [name: 'nValueRd',   level: 3, clos: 'currentValueRounded',      var: 'string',   args: 1, type: ['number'],],
        [name: 'nX',         level: 1, clos: 'currentX',                 var: 'float',    args: 1, type: ['vector3'],],
        [name: 'nY',         level: 1, clos: 'currentY',                 var: 'float',    args: 1, type: ['vector3'],],
        [name: 'nZ',         level: 1, clos: 'currentZ',                 var: 'float',    args: 1, type: ['vector3'],],
        [name: 'onBattery',  level: 1, clos: 'onBattery',                var: 'boolean',  args: 0, type: ['statHub'],],
        [name: 'pBin',       level: 2, clos: 'previousStateBinary',      var: 'boolean',  args: 1, type: ['enum'],],
        [name: 'pLevel',     level: 1, clos: 'previousStateLevel',       var: 'integer',  args: 1, type: ['enum'],],
        [name: 'pState',     level: 1, clos: 'previousState',            var: 'string',   args: 1, type: ['enum'],],
        [name: 'pText',      level: 3, clos: 'previousStateDescription', var: 'string',   args: 1, type: ['enum'],],
        [name: 'pText',      level: 3, clos: 'previousValueDescription', var: 'string',   args: 1, type: ['number'],],
        [name: 'pValue',     level: 1, clos: 'previousValue',            var: 'float',    args: 1, type: ['number'],],
        [name: 'rDiff',      level: 1, clos: 'difference',               var: 'float',    args: 1, type: ['number'],],
        [name: 'rDiffText',  level: 3, clos: 'differenceText',           var: 'string',   args: 1, type: ['number'],],
        [name: 'sBin',       level: 2, clos: 'statusDeviceBinary',       var: 'boolean',  args: 1, type: ['device', 'statDev', 'zwave'],],
        [name: 'sBin',       level: 2, clos: 'statusHubBinary',          var: 'boolean',  args: 0, type: ['local', 'statHub'],],
        [name: 'sLevel',     level: 1, clos: 'statusDeviceLevel',        var: 'integer',  args: 1, type: ['device', 'statDev', 'zwave'],],
        [name: 'sLevel',     level: 1, clos: 'statusHubLevel',           var: 'integer',  args: 0, type: ['local', 'statHub'],],
        [name: 'secLevel',   level: 1, clos: 'networkSecurityLevel',     var: 'string',   args: 1, type: ['zwave'],],
        [name: 'secure',     level: 1, clos: 'zwaveSecure',              var: 'boolean',  args: 1, type: ['zwave', 'zwCCs'],],
        [name: 'sunrise',    level: 1, clos: 'sunrise',                  var: 'string',   args: 0, type: ['local'], ident: true,],
        [name: 'sunset',     level: 1, clos: 'sunset',                   var: 'string',   args: 0, type: ['local'], ident: true,],
        [name: 'tBattery',   level: 1, clos: 'batteryChangeDate',        var: 'integer',  args: 1, type: ['device', 'statDev'],],
        [name: 'tCP',        level: 1, clos: 'hubTCPport',               var: 'integer',  args: 0, type: ['local'],],
        [name: 'tDay',       level: 1, clos: 'timeOfDay',                var: 'integer',  args: 1, type: ['enum', 'number'],],
        [name: 'tElap',      level: 1, clos: 'timeElapsed',              var: 'integer',  args: 1, type: ['enum', 'number'],],
        [name: 'tElapText',  level: 3, clos: 'timeElapsedText',          var: 'string',   args: 1, type: ['enum', 'number'],],
        [name: 'tLastAct',   level: 1, clos: 'timeLastActivity',         var: 'integer',  args: 1, type: ['device'],],
        [name: 'tLastEvent', level: 1, clos: 'timeLastEvent',            var: 'integer',  args: 2, type: ['attribute'],],
        [name: 'tOffset',    level: 2, clos: 'currentTimeOffset',        var: 'integer',  args: 1, type: ['enum'],],
        [name: 'tStamp',     level: 2, clos: 'timestamp',                var: 'integer',  args: 1, type: ['day', 'enum', 'hub', 'number', 'vector3'],],
        [name: 'tWrite',     level: 2, clos: 'timeWrite',                var: 'integer',  args: 0, type: ['enum', 'number', 'vector3'],],
        [name: 'vLastEvent', level: 1, clos: 'valueLastEvent',           var: 'string',   args: 2, type: ['attribute'],],
        [name: 'wakeUpInt',  level: 1, clos: 'wakeUpInterval',           var: 'integer',  args: 1, type: ['statDev', 'zwave'],],
        [name: 'wLevel',     level: 1, clos: 'timeWeightedLevel',        var: 'integer',  args: 1, type: ['enum'],],
        [name: 'wValue',     level: 1, clos: 'timeWeightedValue',        var: 'float',    args: 1, type: ['number'],],
        [name: 'zigbeeP',    level: 2, clos: 'zigbeePowerLevel',         var: 'integer',  args: 0, type: ['local'],],
        [name: 'zwaveP',     level: 2, clos: 'zwavePowerLevel',          var: 'string',   args: 0, type: ['local'],],
        [name: '',           level: 3, clos: 'commandClassesList',       var: 'multiple', args: 1, type: ['zwCCs'],],
] }

/*****************************************************************************************************************
 *  Tags Location Details:
 *****************************************************************************************************************/
def getLocationName() { return { -> location.name } }

def getLocationId() { return { (isEventObject(it)) ? it.locationId : location.id } }

def getIsEventObject() { return { it?.respondsTo('isStateChange') } }

/*****************************************************************************************************************
 *  Tags Hub Details:
 *****************************************************************************************************************/
def getHubName() { return { -> hub().name } }

def getHub() { return { -> location.hubs[0] } } // note: device.hub can get a device's hub

def getHubId() { return { (isEventObject(it)) ? it.hubId : hub().id } }

/*****************************************************************************************************************
 *  Tags Group Details:
 *****************************************************************************************************************/
def getGroupName() { return { state?.groupNames?."${groupId(it)}" ?: state.dwellingType } } // gets group name from created state.groupNames map

def getGroupId() { return {
    if (isEventObject(it)) {
        it?.device?.device?.groupId ?: 'unassigned' // for event objects
    }
    else {
        it?.device?.groupId ?: 'unassigned' // for everything else
    }
} }

/*****************************************************************************************************************
 *  Tags Device Details:
 *****************************************************************************************************************/
def getDeviceCode() { return {
    if (isEventObject(it)) {
        it?.device?.device?.name ?: 'unassigned'
    }
    else {
        it?.name ?: 'unassigned'
    }
} }

def getDeviceHandlerName() { return { it?.typeName } }

def getDeviceId() { return { (isEventObject(it)) ? it.deviceId : it?.id } }

def getDeviceLabel() { return {
    if (isEventObject(it)) {
        if (eventName(it) == 'daylight') {
            'Day'
        } else if (eventName(it) == 'hubStatus') {
            'Hub'
        } else {
            it?.device?.device?.label ?: 'unassigned'
        }
    } else {
        it?.label ?: 'unassigned'
    }
} }

def getDeviceType() { return {
    if (zwInfo(it)) {
        'zwave'
    } else if (it?.device?.zigbeeId) {
        'zigbee'
    } else {
        'lan'
    }
} }

/*****************************************************************************************************************
 *  Tags Event Details:
 *****************************************************************************************************************/
def getEventName() { return {
    if (isEventObject(it)) {
        (it.name in ['sunrise', 'sunset']) ? 'daylight' : it.name // puts sunrise and sunset events into common 'daylight' event
    } else {
        it
    }
} }

def getEventType() { return { eventDetails(it).type } }

def getEventDetails() { return { getAttributeDetail().find { attr -> attr.key == eventName(it) }.value } }

def getIdentifierGlobalEvent() { return { "${locationName()} . ${hubName()} . ${identifierLocal(it)} . ${eventName(it).capitalize()}" } } // added capitalize()

def getIdentifierLocal() { return { "${groupName(it)} . ${deviceLabel(it)}" } }

def getIsChange() { return { it?.isStateChange } }

def getIsDigital() { return { it?.isDigital } }

def getIsPhysical() { return { it?.isPhysical } }

def getSource() { return { "${it?.source}".toLowerCase().replaceAll('_', '') } }

def getUnit() { return {
    def unit = it?.unit ?: eventDetails(it).unit
    if (it.name == 'temperature') unit.replaceAll('\u00B0', '') // remove circle from C unit
    unit
} }

/*****************************************************************************************************************
 *  Tags Metadata:
 *****************************************************************************************************************/
def getHubType() { return { -> "${hub().type}".toLowerCase() } }

def getIdentifierGlobalHub() { return { "${locationName()} . ${hubName()} . ${state.dwellingType} . Hub" } }

def getIdentifierGlobalDevice() { return { "${locationName()} . ${hubName()} . ${identifierLocal(it)}" } }

def getIdentifierGlobalAttribute() { return { dev, attr -> "${locationName()} . ${hubName()} . ${groupName(dev)} . ${deviceLabel(dev)} . ${attr.capitalize()}" } }

def getZwaveListening() { return {
    switch(zwInfo(it)?.zw.take(1)) {
        case 'L':
            return 'listening'; break
        case 'S':
            return 'sleepy'; break
        case 'B':
            return 'beamable'; break
        default:
            return 'unknown'; break
    }
} }

def getZwInfo() { return { it?.getZwaveInfo() } }

def getPowerSource() { return { it?.latestValue('powerSource').toLowerCase() ?: 'unknown' } }

def getTempScale() { return { -> location?.temperatureScale } }

def getTimeZoneName() { return { -> location?.timeZone.ID } }

/*****************************************************************************************************************
 *  Tags Statuses:
 *****************************************************************************************************************/
def getStatusDevice() { return { "${it?.status}".toLowerCase().replaceAll('_', '') } }

def getStatusHub() { return { -> "${hub()?.status}".toLowerCase().replaceAll('_', '') } }

/*****************************************************************************************************************
 *  Fields Event Details - Current:
 *****************************************************************************************************************/
def getEventDescription() { return { it?.descriptionText?.replaceAll('\u00B0', ' ').replace('{{ locationName }}', "${locationName()}").replace('{{ linkText }}', "${deviceLabel(it)}").replace('{{ value }}', "${it.value}").replace('{{ name }} ', '') } } // remove circle from C unit, tidy up description text by replacing placeholders

def getEventId() { return { it.id } }

def getCurrentStateBinary() { return { currentStateLevel(it) > 0 ? 't' : 'f' } }

def getCurrentStateLevel() { return { attributeStates(it).find { level -> level.key == currentState(it) }.value } }

def getAttributeStates() { return { eventDetails(it).levels } } // Lookup array for event state levels

def getCurrentState() { return { it?.name in ['sunrise', 'sunset'] ? it.name : it.value } }

def getCurrentStateDescription() { return { "At ${locationName()}, in ${hubName()}, ${deviceLabel(it)} is ${currentState(it)} in the ${groupName(it)}." } } // TODO - leave for now: 'sun has risen' / 'sun has set' for 'daylight' events

def getCurrentValueDescription() { return { "At ${locationName()}, in ${hubName()}, ${eventName(it)} is ${currentValueRounded(it)} ${unit(it)} in the ${groupName(it)}." } }

def getCurrentValue() { return { // done this way in case value is 0
    try {
        it.numberValue.toBigDecimal()
    } catch(e) {
        removeUnit(it)
    }
} }

def removeUnit() { return { // remove any units appending to end of event value property by device handler
    def length = it.value.length()
    def value
    def i = 2
    while (i < (length - 1)) {
        value = it.value.substring(0, length - i)
        if (value.isNumber()) break
        i++
    }
    if (i == length) {
        0
    } else {
        value.toBigDecimal()
    }
} }

def getCurrentValueRounded() { return { currentValue(it).setScale(decimalPlaces(it), BigDecimal.ROUND_HALF_EVEN) } }

def getDecimalPlaces() { return { eventDetails(it)?.decimalPlaces } }

def getCurrentHue() { return { currentColorMap(it).hue } }
def getCurrentSat() { return { currentColorMap(it).saturation } }
def getCurrentColorMap() { return { parseJson(it) } }

def getCurrentX() { return { it.xyzValue.x / gravityFactor() } }
def getCurrentY() { return { it.xyzValue.y / gravityFactor() } }
def getCurrentZ() { return { it.xyzValue.z / gravityFactor() } }
def getGravityFactor() { return { -> (1024) } }

/*****************************************************************************************************************
 *  Fields Event Details - Previous:
 *****************************************************************************************************************/
def getPreviousStateBinary() { return { previousStateLevel(it) > 0 ? 't' : 'f' } }

def getPreviousStateLevel() { return { attributeStates(it)?.find { level -> level.key == previousState(it) }?.value } }

def getPreviousState() { return { previousEvent(it)?.value } }

/**
 * get previous event
 * @return previous event or null if no previous event (i.e. first event for a given device.attribute)
 */
def getPreviousEvent() { return {
    def previousEvent
    def eventData = parseJson(it?.data)
    if (eventData?.previous) {
        previousEvent = [value: eventData?.previous?.value, date: it?.data?.previous?.date] // TODO - Check that date is the correct field
    } else {
        def history = it?.device?.statesSince("${it.name}", it.date - 7, [max: 5])
        if (history) {
            previousEvent = history.sort { a, b -> b.date.time <=> a.date.time }.find { previous -> previous.date.time < it.date.time }
        }
    }
    previousEvent
} }

def getPreviousStateDescription() { return { "This is a change from ${previousState(it)} ${timeElapsedText(it)}." } }

def getPreviousValueDescription() { return {
    def changeAbs = (differenceText(it) == 'unchanged') ? 'unchanged' : "${differenceText(it)} by ${difference(it).abs()} ${unit(it)}"
    "This is ${changeAbs} compared to ${timeElapsedText(it)}."
} }

def getPreviousValue() { return {
    try {
        previousEvent(it)?.numberValue.toBigDecimal()
    } catch(e) {
        removeUnit(previousEvent(it))
    }
} }

/*****************************************************************************************************************
 *  Fields Event Details - Value Difference:
 *****************************************************************************************************************/
def getDifferenceText() { return { (difference(it) > 0) ? 'increased' : (difference(it) < 0) ? 'decreased' : 'unchanged' } }

def getDifference() { return { (currentValue(it).setScale(decimalPlaces(it), BigDecimal.ROUND_HALF_EVEN) - previousValue(it).setScale(decimalPlaces(it), BigDecimal.ROUND_HALF_EVEN)).toBigDecimal().setScale(decimalPlaces(it), BigDecimal.ROUND_HALF_EVEN) } }

/*****************************************************************************************************************
 *  Fields Event Details - Time Difference:
 *****************************************************************************************************************/
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

def getTimeElapsed() { return { timestamp(it) - previousEvent(it).date.time - previousTimeOffset(it) } }

def getTimestamp() { return { it.date.time - currentTimeOffset(it) } }

def getCurrentTimeOffset() { return { (eventName(it) == 'motion' && currentState(it) == 'inactive') ? timeOffsetAmount() : 0 } }

def getTimeOffsetAmount() { return { -> (1000 * 10 / 2) } }

def getPreviousTimeOffset() { return { (eventName(it) == 'motion' && previousState(it) == 'inactive') ? timeOffsetAmount() : 0 } }

/*****************************************************************************************************************
 *  Fields Event Details - Time Values:
 *****************************************************************************************************************/
def getTimeOfDay() { return { timestamp(it) - it.date.clone().clearTime().time } } // calculates elapsed time of day in milliseconds

def getTimeWrite() { return { -> new Date().time } } // time of processing the event by Smart App

/*****************************************************************************************************************
 *  Fields Event Details - Weighted Values:
 *****************************************************************************************************************/
def getTimeWeightedLevel() { return {  previousStateLevel(it) * timeElapsed(it) } }

def getTimeWeightedValue() { return {  previousValue(it) * timeElapsed(it) } }

/*****************************************************************************************************************
 *  Fields Metadata:
 *****************************************************************************************************************/
def getConfiguredParametersList() { return {
    def params = it?.device?.getDataValue('configuredParameters')
    if (params) {
        params.replaceAll(',', 'i,') + 'i'
    } else {
        ''
    }
} }

def getBattery() { return {
    if (it?.hasAttribute('battery')) {
        it?.latestValue('battery') ?: 100 // in case battery is still 100 and no battery report has been sent
    } else {
        ''
    }
}}

def getCheckInterval() { return { it?.latestValue('checkInterval') ?: '' } }

def getDeviceConfigurationType() { return { it?.device?.getDataValue('configurationType') ?: '' } }

def getConfigure() { return { it?.latestValue('configure') ?: '' } }

def getDeviceUse() { return { it?.device?.getDataValue('deviceUse') ?: '' } }

def getFirmwareVersion() { return { -> hub().firmwareVersionString } }

def getLatitude() { return { -> location.latitude } }

def getLongitude() { return { -> location.longitude } }

def getMessages() { return { it?.device?.getDataValue('messages') ?: '' } }

def getNetworkSecurityLevel() { return { it?.device?.getDataValue('networkSecurityLevel')?.replace('ZWAVE_', '')?.replaceAll('_', ' ') ?: '' } }

def getZwaveSecure() { return { (zwInfo(it)?.zw.endsWith('s')) ? 't' : 'f' } }

def getSunrise() { return { -> daylight().sunrise.format('HH:mm', location.timeZone) } }
def getSunset() { return { -> daylight().sunset.format('HH:mm', location.timeZone) } }
def getDaylight() { return { -> getSunriseAndSunset() } }

def getHubTCPport() { return { -> hub().localSrvPortTCP } }

def getTimeLastActivity() { return { it?.lastActivity?.time ?: 0 } }

def getTimeLastEvent() { return { dev, attr -> dev?.latestState(attr)?.date?.time ?: 0 } }

def getValueLastEvent() { return { dev, attr -> "${dev?.latestValue(attr)}" ?: 'null' } }

def getWakeUpInterval() { return { it?.device?.getDataValue('wakeUpInterval') ?: '' } }

def getZigbeePowerLevel() { return { -> hub().hub.getDataValue('zigbeePowerLevel') } }

def getZwavePowerLevel() { return { -> hub().hub.getDataValue('zwavePowerLevel') } }

def getCommandClassesList() { return {
    def info = zwInfo(it).clone()
    def cc = info.cc
    if (info?.ccOut) cc.addAll(info.ccOut)
    if (info?.sec) cc.addAll(info.sec)
    def ccList = 'zz' + cc.sort().join('=t,zz') + '=t' // 't' is InfluxLP for 'true'
    info.remove('zw')
    info.remove('cc')
    if (info?.ccOut) info.remove('ccOut')
    if (info?.sec) info.remove('sec')
    // info.endpointInfo.replaceAll(',', '') // .replaceAll("'", '') TODO - Need to sort this out - leave for now until understand data format better
    info = info.sort()
    def toKeyValue = { it.collect { /$it.key="$it.value"/ } join ',' }
    info = toKeyValue(info) + ',' + "${ccList}"
    info
} }

/*****************************************************************************************************************
 *  Fields Statuses:
 *****************************************************************************************************************/
def getHubIPaddress() { return { -> hub().localIP } }

def getOnBattery() { return { ->
    def battery = hub()?.hub?.getDataValue('batteryInUse')
    if (battery) {
        (battery.contains('true')) ? 't' : 'f'
    }
    else {
        null
    }
} }

def getStatusDeviceBinary() { return { (statusDevice(it) in ['active', 'online']) ? 't' : 'f' } }

def getStatusHubBinary() { return { (statusHub() == 'active') ? 't' : 'f' } }

def getStatusDeviceLevel() { return { (statusDevice(it) in ['active', 'online']) ? 1 : -1 } }

def getStatusHubLevel() { return { (statusHub() == 'active') ? 1 : -1 } }

def getBatteryChangeDate() { return {
    if (it?.hasAttribute('batteryChange')) {
        it?.latestState('batteryChange')?.date?.time ?: 0
    } else {
        ''
    }
}}

/*****************************************************************************************************************
 *  Main Commands:
 *****************************************************************************************************************/
def postToInfluxDBLocal(data, retentionPolicy, bucket, eventId) {
    def headers = [
            HOST           : state.uri,
            'Content-Type' : 'application/octet-stream',
            'Accept'       : 'application/json'
    ]

    def query = [precision : 'ms']

    def params = [
            method   : 'POST',
            headers  : headers,
            query    : query,
            body     : data
    ]

    switch(settings.dbVersion) {
        case 1:
            query << [
                    db : settings?.dbName,
                    u  : settings?.dbUsername,
                    p  : settings?.dbPassword,
                    rp : retentionPolicy
            ]

            params << [path : '/write']
            break

        default:
            headers << [authorization : "Token ${settings?.dbToken}"]
            query   << [org : settings?.dbOrganization, bucket : bucket]
            params  << [path : '/api/v2/write']
            break
    }

    def options = [callback : handleInfluxDBResponseLocal]

    logger("postToInfluxDBhubAction: Posting data to InfluxDB: Host: ${state.uri}${params.path} Data: ${data}", 'info')
    // logger("postToInfluxDBhubAction: Posting data to InfluxDB: Headers: ${headers}, Host: ${state.uri}${params.path}, Query: ${query}, Data: ${data}", 'info')
    sendHubCommand(new physicalgraph.device.HubAction(params, null, options))
}

def handleInfluxDBResponseLocal(physicalgraph.device.HubResponse hubResponse) {
    if (hubResponse.status == 204) logger("postToInfluxDBLocal: Success! Code: ${hubResponse.status}", 'trace')
    if (hubResponse.status >= 400) logger("postToInfluxDBLocal: Something went wrong! Code: ${hubResponse.status} Message: ${hubResponse.json.error}", 'error')
}

def postToInfluxDBRemote(data, retentionPolicy, bucket, eventId) {
    def query = [precision : 'ms']

    def params = [
            uri                : state.uri,
            query              : query,
            requestContentType : 'application/octet-stream',  // 'Content-Type'
            contentType        : 'application/json', // 'Accept'
            body               : data
    ]

    switch(settings?.dbVersion) {
        case 1:
            query << [
                    db : settings?.dbName,
                    u  : settings?.dbUsername,
                    p  : settings?.dbPassword,
                    rp : retentionPolicy
            ]

            params << [path : '/write']
            break

        default:
            params << [headers : [authorization : "Token ${settings?.dbToken}"]]
            query  << [org : settings?.dbOrganization, bucket : bucket]
            params << [path : '/api/v2/write']
            break
    }

    def passData = [eventId : eventId]

    logger("postToInfluxDBasynchttp: Posting data to InfluxDB: Host: ${state.uri}${params.path} Data: ${data}", 'info')
    // logger("postToInfluxDBasynchttp: Posting data to InfluxDB: Host: ${state.uri}${params.path}, Query: ${query}, Data: ${data}", 'info')
    asynchttp_v1.post(handleInfluxDBResponseRemote, params, passData)
}

def handleInfluxDBResponseRemote(response, passData) {
    if (response.status == 204) logger("postToInfluxDBRemote: Success! Code: ${response.status} (id: ${passData.eventId})", 'trace')
    if (response.status >= 400) logger("postToInfluxDBRemote: Something went wrong! Code: ${response.status} Error: ${response.errorJson.error} (id: ${passData.eventId})", 'error')
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/
private manageSchedules() {
    logger('manageSchedules: Schedulling polling methods', 'trace')
    pollingMethods().each {
        try {
            unschedule(it.key)
        }
        catch (e) {
            logger("manageSchedules: Unschedule ${it.key} failed!", 'error')
        }
        "${it.value}"(it.key)
    }
}

def pollingMethods() { [
        pollStatus     : 'runEvery1Hour',
        pollLocations  : 'runEvery3Hours',
        pollDevices    : 'runEvery3Hours',
        pollAttributes : 'runEvery3Hours',
        pollZwavesCCs  : 'runEvery3Hours',
        pollZwaves     : 'runEvery3Hours'
] }

private manageSubscriptions() {
    logger('manageSubscriptions: Subscribing listeners to events', 'trace')
    unsubscribe()
    getSelectedDevices()?.each { dev ->
        if (!dev.displayName.startsWith("~")) {
            getDeviceAllowedAttrs(dev)?.each { attr ->
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
                        subscribe(dev, attr, handleJsonObjectEvent); break
                }
            }
        }
    }

    logger("manageSubscriptions: Subscribing 'handleDaylight' listener to 'Sunrise' and 'Sunset' events", 'info')
    subscribe(location, 'sunrise', handleDaylight)
    subscribe(location, 'sunset', handleDaylight)

    logger("manageSubscriptions: Subscribing 'handleHubStatus' listener to 'Hub Status' events", 'info')
    subscribe(location.hubs[0], 'hubStatus', handleHubStatus)
}

private logger(msg, level = 'debug') { // Wrapper method for all logging
    switch (level) {
        case 'error':
            if (state.logLevelIDE >= 1) log.error(msg); break
        case 'warn':
            if (state.logLevelIDE >= 2) log.warn(msg); break
        case 'info':
            if (state.logLevelIDE >= 3) log.info(msg); break
        case 'debug':
            if (state.logLevelIDE >= 4) log.debug(msg); break
        case 'trace':
            if (state.logLevelIDE >= 5) log.trace(msg); break
        default:
            log.debug(msg); break
    }
}

/**
 * creates a list of allowed attributes by for a given device by filtering list of user selected attributes
 * @param device
 * @return
 */
private getDeviceAllowedAttrs(device) {
    def deviceAllowedAttrs = []
    try {
        settings?.allowedAttributes?.each { attr ->
            try { if (device.hasAttribute(attr)) deviceAllowedAttrs << attr }
            catch (e) { logger("deviceAllowedAttrs: Error while getting device allowed attributes for ${device?.displayName} and attribute ${it}: ${e.message}", 'warn') }
        }
    }
    catch (e) { logger("deviceAllowedAttrs: Error while getting device allowed attributes for ${device?.displayName}: ${e.message}", 'warn') }
    deviceAllowedAttrs.sort()
}

/**
 * iterates through list of all potential attributes to find those belonging to selected devices
 * @return
 */
private getSupportedAttributes() {
    def supportedAttributes = []
    def devices = getSelectedDevices()
    if (devices) {
        getAllAttributes()?.each { attr ->
            try { if (devices?.find { dev -> dev?.hasAttribute(attr) }) supportedAttributes << attr }
            catch (e) { logger("supportedAttributes: Error while finding supported devices for ${attr}: ${e.message}", 'warn') }
        }
    }
    supportedAttributes?.unique()?.sort()
}

/**
 * iterates through Capabilites map and creates a list of all the potential attributes (so no custom attributes - could look at pulling them from device.supportedAttributes)
 * @return
 */
private getAllAttributes() {
    def attributes = []
    getCapabilities().each { cap ->
        try {
            if (cap?.attr) {
                if (cap.attr instanceof Collection) {
                    cap.attr.each {
                        attributes << it
                    }
                } else {
                    attributes << cap.attr
                }
            }
        }
        catch (e) {
            logger("allAttributes: Error while getting attributes for capability ${cap}: ${e.message}", 'warn')
        }
    }
    attributes?.unique()?.sort()
}

/**
 * creates list of device Names from list of device Objects selected by user
 * @return
 */
private getSelectedDeviceNames() {
    try {
        getSelectedDevices()?.collect { it?.displayName }?.sort() // TODO ? Could this be groupName . displayName to be more helpful?
    }
    catch (e) {
        logger("selectedDeviceNames: Error while getting selected device names: ${e.message}", 'warn')
        []
    }
}

/**
 * creates list of device Objects from devices selected by user
 * @return
 */
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

private getCapabilities() { [
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
] }

private getAttributeDetail() { [
        acceleration            : [type: 'enum', levels: [inactive: -1, active: 1]],
        alarm                   : [type: 'enum', levels: [off: -1, siren: 1, strobe: 2, both: 3]],
        battery                 : [type: 'number', decimalPlaces: 0, unit: '%'],
        button                  : [type: 'enum', levels: [released: -1, pushed: 1, double: 2, held: 3]],
        buttonClicks            : [type: 'enum', levels: ['hold start': -1, 'hold release': 0, 'one click': 1, 'two clicks': 2, 'three clicks': 3, 'four clicks': 4, 'five clicks': 5]],
        carbonDioxide           : [type: 'number', decimalPlaces: 0, unit: 'ppm'],
        carbonMonoxide          : [type: 'enum', levels: [clear: -1, detected: 1, tested: 4]],
        color                   : [type: 'string'],
        colorTemperature        : [type: 'number', decimalPlaces: 0, unit: 'K'],
        consumableStatus        : [type: 'enum', levels: [replace: -1, good: 1, order: 3, 'maintenance required': 4, missing: 5]],
        contact                 : [type: 'enum', levels: [closed: -1, empty: -1, full: -1, vacant: -1, flushing: 1, occupied: 1, open: 1]],
        current                 : [type: 'number', decimalPlaces: 2, unit: 'A'],
        daylight                : [type: 'daylight', levels: [ sunset: -1, sunrise: 1]],
        door                    : [type: 'enum', levels: [closing: -2, closed: -1, open: 1, opening: 2, unknown: 5]],
        energy                  : [type: 'number', decimalPlaces: 2, unit: 'kWh'],
        heatingSetpoint         : [type: 'number', decimalPlaces: 0, unit: 'C'],
        hubStatus               : [type: 'hub', levels: [inactive: -2, disconnected: -1, active: 1]],
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
] }