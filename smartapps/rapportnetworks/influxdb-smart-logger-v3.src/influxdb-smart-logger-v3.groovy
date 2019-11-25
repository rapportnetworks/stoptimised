/*****************************************************************************************************************
 *  Copyright Alasdair Thin
 *
 *  Name: InfluxDB Smart Logger
 *
 *  Date: 2019-11-15
 *
 *  Version: 3.1
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
    page(name : 'mainPage')
    page(name : 'devicesPage')
    page(name : 'attributesPage')
}

def mainPage() {
    dynamicPage(
            name      : 'mainPage',
            uninstall : true,
            install   : true,
    ) {
        section('Logger settings') {
            input(
                    name               : 'logLevelDB',
                    title              : 'Database Logging Level',
                    description        : '',
                    type               : 'enum',
                    options            : [1 : 'Minimal', 2 : 'Intermediate', 3 : 'All'],
                    defaultValue       : 1,
                    displayDuringSetup : true,
            )

            input(
                    name               : 'logIdent',
                    title              : 'Log Data Identifiers',
                    description        : '',
                    type               : 'bool',
                    defaultValue       : true,
                    displayDuringSetup : true,
            )

            input(
                    name               : 'logLevelIDE',
                    title              : 'IDE Logging Level',
                    description        : '',
                    type               : 'enum',
                    options            : [0 : 'None', 1 : 'Error', 2 : 'Warning', 3 : 'Info', 4 : 'Debug', 5 : 'Trace'],
                    defaultValue       : 3,
                    displayDuringSetup : true,
            )
        }

        section('Measurements to Log') {
            input(
                    name         : 'logEvents',
                    title        : 'Events',
                    description  : '',
                    type         : 'bool',
                    defaultValue : false,
            )

            input(
                    name         : 'logMetadata',
                    title        : 'Metadata',
                    description  : '',
                    type         : 'bool',
                    defaultValue : false,
            )

            input(
                    name         : 'logStatuses',
                    title        : 'Statuses',
                    description  : '',
                    type         : 'bool',
                    defaultValue : false,
            )

            input(
                    name         : 'logConfigs',
                    title        : 'Configurations',
                    description  : '',
                    type         : 'bool',
                    defaultValue : false,
            )
        }

        section('Influx Database settings') {
            input(
                    name         : 'dbVersion',
                    title        : 'Database version',
                    description  : '',
                    type         : 'number',
                    range        : '1..2',
                    defaultValue : 2,
            )

            input(
                    name         : 'dbRemote',
                    title        : 'Remote Database',
                    description  : '',
                    type         : 'bool',
                    defaultValue : true,
            )

            input(
                    name         : 'dbSSL',
                    title        : 'Encrypted Connection',
                    description  : '',
                    type         : 'bool',
                    defaultValue : true,
            )

            input(
                    name           : 'dbHost',
                    title          : 'Host',
                    description    : '',
                    type           : 'text',
                    capitalization : 'none',
                    autoCorrect    : false,
                    required       : false,
            )

            input(
                    name         : 'dbPort',
                    title        : 'Port',
                    description  : '',
                    type         : 'number',
                    defaultValue : '443',
            )

            input(
                    name           : 'dbName',
                    title          : 'Database (v1)',
                    description    : '',
                    type           : 'text',
                    capitalization : 'none',
                    autoCorrect    : false,
                    required       : false,
            )

            input(
                    name           : 'dbUsername',
                    title          : 'Username (v1)',
                    description    : '',
                    type           : 'text',
                    capitalization : 'none',
                    autoCorrect    : false,
                    required       : false,
            )

            input(
                    name        : 'dbPassword',
                    title       : 'Password (v1)',
                    description : '',
                    type        : 'password',
                    required    : false,
            )

            input(
                    name           : 'dbOrganization',
                    title          : 'Organization (v2)',
                    description    : '',
                    type           : 'text',
                    capitalization : 'none',
                    autoCorrect    : false,
                    required       : false,
            )

            input(
                    name        : 'dbToken',
                    title       : 'Authorisation Token (v2)',
                    description : '',
                    type        : 'password',
                    required    : false,
            )
        }

        if (state.devicesConfigured) {
            section('Selected Devices') {
                getPageLink(
                        'devicesPageLink',
                        'Tap to change',
                        'devicesPage',
                        null,
                        buildSummary(selectedDeviceNames),
                        null,
                )
            }
        }
        else {
            devicesPageContent
        }

        if (state.attributesConfigured) {
            section('Selected Events') {
                getPageLink(
                        'attributesPageLink',
                        'Tap to change',
                        'attributesPage',
                        null,
                        buildSummary(settings?.loggedAttributes?.sort()),
                        null,
                )
            }
        }
        else {
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
                        submitOnChange : true,
                        required       : false,
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
    def listAttributesSupported = attributesSupported
    if (listAttributesSupported) {
        section('Choose Events') {
            paragraph(
                    'Select all the events that should get logged, depending on the devices supporting them.'
            )
            input(
                    name           : 'loggedAttributes',
                    title          : 'Events to log',
                    description    : '',
                    type           : 'enum',
                    options        : listAttributesSupported,
                    multiple       : true,
                    hideWhenEmpty  : false,
                    submitOnChange : true,
                    required       : true,

            )
        }
    }
    else {
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
 *  SmartThings System Commands (installed, uninstalled, updated)
 *****************************************************************************************************************/
/**
 * installed - runs when the smart app is first installed
 * @return
 */
def installed() {
    state.logLevelIDE = 5
    logger("installed: ${app.label} installed with settings: ${settings}.", 'info')
}

/**
 * uninstalled - runs when the app is uninstalled
 * @return
 */
def uninstalled() {
    logger("uninstalled: ${app.label} has be removed.", 'info')
}

/**
 * updated - runs whenever app settings are changed
 * @return
 */
def updated() {
    logger("updated: ${app.label} has been updated.", 'info')

    logger('updated: Setting IDE logging level.', 'info')
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

    /**
     * check if devices and events selected by user
     */
    if (getSelectedDevices()) {
        logger('updated: Configured - Devices Selected.', 'info')
        state.devicesConfigured = true
    }
    else {
        logger('updated: Unconfigured - Choose Devices.', 'warn')
        state.devicesConfigured = false
    }

    if (settings?.loggedAttributes) {
        logger('updated: Configured - Events Selected.', 'info')
        state.attributesConfigured = true
    }
    else {
        logger('updated: Unconfigured - Choose Events.', 'warn')
        state.attributesConfigured = false
    }

    /**
     * Subscribe event handler methods to listen for events
     */
    manageSubscriptions()

    /**
     * Scheduling status and metadata polling methods
     */
    manageSchedules()

    /**
     * Sets a name for whole of dwelling events (eg energy)
     */
    state.dwellingType = 'House'

    /**
     * create a map of room (group) names
     */
    generateGroupNamesMap()

    logger('updated: Scheduling first run of poll methods.', 'info')
    def runInTime     = 30
    def runInInterval = 30
    pollingMethods().each {
        runIn(runInTime, it.key)
        runInTime += runInInterval
    }
}

/*****************************************************************************************************************
 *  Updated Helper Method (generateGroupNamesMap)
 *****************************************************************************************************************/
/**
 * generateGroupNamesMap
 * Scheduled to run every 3 hours so as to capture any updates to rooms and/or devices.
 * @return
 */
private generateGroupNamesMap() {
    logger('generateGroupNamesMap: Creating map of group Ids and group names.', 'info')
    state.groupNames = [:]
    if (settings?.bridgePref) {
        def groupId
        def groupName
        settings.bridgePref.each {
            if (it.displayName?.take(1) == '~') {
                groupId   = it.device?.groupId
                groupName = it.displayName?.drop(1)
                if (groupId) state.groupNames << [(groupId): groupName]
            }
        }
    }
}

/*****************************************************************************************************************
 *  Event Handler Methods (handleEnumEvent, handleNumberEvent, handleVector3Event, handleStringEvent, handleColorMapEvent, handleJsonObjectEvent, handleDaylight, handleHubStatus)
 *****************************************************************************************************************/
/*
def handleAppTouch(evt) { // Touch event on Smart App
    logger("handleAppTouch:", 'trace')
}
*/

/**
 * handleEnumEvent
 * @param evt
 * @return
 */
def handleEnumEvent(evt) {
    if (state?.logEvents) influxLineProtocol(evt, 'states', 'enum')
}

/**
 * handleNumberEvent
 * @param evt
 * @return
 */
def handleNumberEvent(evt) {
    if (state?.logEvents) influxLineProtocol(evt, 'values', 'number')
}

/**
 * handleVector3Event
 * @param evt
 * @return
 */
def handleVector3Event(evt) {
    if (state?.logEvents) influxLineProtocol(evt, 'threeaxes', 'vector3')
}

/**
 * handleStringEvent
 * @param evt
 * @return
 */
def handleStringEvent(evt) {
    if (state?.logEvents) influxLineProtocol(evt, 'statuses', 'string')
}

/**
 * handleColorMapEvent
 * @param evt
 * @return
 */
def handleColorMapEvent(evt) {
    if (state?.logEvents) influxLineProtocol(evt, 'values', 'colorMap')
}

/**
 * handleJsonObjectEvent
 * @param evt
 */
def handleJsonObjectEvent(evt) {
    // TODO - Write handler for json object events if needed.
}

/**
 * handleDaylight
 * @param evt
 * @return
 */
def handleDaylight(evt) {
    if (state?.logEvents) influxLineProtocol(evt, 'daylight', 'day')
}

/**
 * handleHubStatus
 * @param evt
 * @return
 */
def handleHubStatus(evt) {
    if (state?.logEvents) influxLineProtocol(evt, 'hub', 'hub')
}

/*****************************************************************************************************************
 *  Polling Status Methods (pollStatus, pollStatusHubs, pollStatusDevices)
 *****************************************************************************************************************/
/**
 * pollStatus
 * @return
 */
def pollStatus() {
    pollStatusHubs()
    pollStatusDevices()
}

/**
 * pollStatusHubs
 * @return
 */
def pollStatusHubs() {
    logger('pollStatusHubs: running now.', 'info')
    if (state?.logStatuses) {
        def items = ['placeholder'] // (only 1 location where Smart App is installed, so placeholder is needed)
        influxLineProtocol(items, 'pollHubs', 'statHub', 'statuses')
    }
}

/**
 * pollStatusDevices
 * @return
 */
def pollStatusDevices() {
    logger('pollStatusDevices: running now.', 'info')
    if (state?.logStatuses) {
        def items = getSelectedDevices()?.findAll { !it.displayName.startsWith('~') }
        if (items) influxLineProtocol(items, 'pollDevices', 'statDev', 'statuses')
    }
}

/*****************************************************************************************************************
 *  Polling Metadata Methods (pollLocations, pollDevices, pollAttributes, pollZwavesCCs, pollZwaves)
 *****************************************************************************************************************/
/**
 * pollLocations
 * @return
 */
def pollLocations() {
    logger('pollLocations: running now.', 'info')
    if (state?.logMetadata) {
        def items = ['placeholder'] // (only 1 location where Smart App is installed, so placeholder is needed)
        influxLineProtocol(items, 'areas', 'local', 'metadata', 'metadata')
    }
}

/**
 * pollDevices
 * @return
 */
def pollDevices() {
    logger('pollDevices: running now.', 'info')
    if (state?.logMetadata) {
        def items = getSelectedDevices()?.findAll { !it.displayName.startsWith('~') }
        if (items) influxLineProtocol(items, 'devices', 'device', 'metadata', 'metadata')
    }
}

/**
 * pollAttributes
 * @return
 */
def pollAttributes() {
    logger('pollAttributes: running now.', 'info')
    if (state?.logMetadata) {
        getSelectedDevices()?.findAll { !it.displayName.startsWith('~') }.each { dev ->
            def parentItem = dev
            def items = getDeviceAttributesSelected(dev)
            if (items) influxLineProtocol(items, 'attributes', 'attribute', 'metadata', 'metadata', parentItem)
        }
    }
}

/**
 * pollZwavesCCs
 * @return
 */
def pollZwavesCCs() {
    logger('pollZwavesCCs: running now', 'trace')
    if (state?.logConfigs) {
        def items = getSelectedDevices()?.findAll { !it.displayName.startsWith('~') && it?.getZwaveInfo().containsKey('zw') }
        if (items) influxLineProtocol(items, 'zwaveCCs', 'zwCCs', 'configs', 'metadata')
    }
}

/**
 * pollZwaves
 * @return
 */
def pollZwaves() {
    logger('pollZwaves: running now', 'info')
    if (state?.logConfigs) {
        def items = getSelectedDevices()?.findAll { !it.displayName.startsWith('~') && it?.getZwaveInfo().containsKey('zw') }
        if (items) influxLineProtocol(items, 'zwave', 'zwave', 'configs', 'metadata')
    }
}

/*****************************************************************************************************************
 *  InfluxDB Line Protocol:
 *****************************************************************************************************************/
def influxLineProtocol(items, measurementName, measurementType, bucket = 'events', retentionPolicy = 'autogen', parentItem = false) {
    def influxLP = new StringBuilder()
    def lpt      = lineProtocolTemplateMap()?."${measurementType}"
    logger("influxLineProtocol: measurementType - ${measurementType}, template - ${lpt}.", 'debug')
    def eventId  = 'notEvent'
    items.each { item ->
        /**
         * Appends measurement name to the line protocol.
         */
        influxLP.append(measurementName)

        /**
         * Appends each tag according to measurement item type and schema to the line protocol.
         */
        tags().each { tag ->
            if (tag.level <= state.logLevelDB && (state.logIdent || !tag.ident) && ('a' in tag.type || lpt in tag.type)) {
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
                            }
                            else {
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

                if (tag?.Value) {
                    influxLP.append(",${tag.name}=")
                    if (tag?.esc) {
                        influxLP.append("${tag.Value.replaceAll("'", '').replaceAll('"', '').replaceAll(',', '').replaceAll('=', '').replaceAll(' ', '\\\\ ')}")
                    }
                    else {
                        influxLP.append(tag.Value)
                    }
                }
            }
        }

        /**
         * Append space between tags and fields to the line protocol.
         */
        influxLP.append(' ')

        /**
         * Append each field according to measurement item type and schema to the line protocol.
         */
        def fieldCount = 0
        fields().each { field ->
            if (field.level <= state.logLevelDB && (state.logIdent || !field.ident) && ('a' in field.type || lpt in field.type)) {
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
                            }
                            else {
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

                if (field?.Value || field?.Value == 0) {

                    influxLP.append((fieldCount) ? ',' : '')

                    if (field.var != 'multiple') influxLP.append("${field.name}=")

                    if (field.var == 'string') {
                        influxLP.append('\"').append(field.Value).append('\"')
                    }
                    else {
                        influxLP.append(field.Value)
                    }

                    if (field.var == 'integer') influxLP.append('i')

                    fieldCount++

                    if (field.name == 'eventId') eventId = "${field.Value}" // used to track logging to database and any associated errors
                }
            }
        }

        /**
         * Append timestamp for event (single) items, line return for others as multiple items, to the line protocol.
         */
        if (isEventObject(item)) {
            influxLP.append(' ').append(timestamp(item))
        }
        else {
            influxLP.append('\n')
        }
    }

    /**
     * Check to exclude duplicate events occuring in a short space of time
     * Doesn't seem to be much of a problem now, so not used
     * TODO - Check for duplicate events in InfluxDB.
     */
/*
    if (!(timeElapsed < 500 && evt.value == pEvent.value)) {
        // ignores repeated propagation of an event (time interval < 0.5 s)
        postToInfluxDB...
    } else {
        logger("handleEnumEvent(): Ignoring duplicate event $evt.displayName ($evt.name) $evt.value", 'warn')
    }
*/
    /**
     * Allows for line protocol to be displayed in IDE rather than being logged to database.
     */
    if (state.logToDB) {
        "postToInfluxDB${state.dbLocation}"(influxLP.toString(), retentionPolicy, bucket, eventId)
    }
    else {
        logger("influxLineProtocol: ${influxLP.toString()}", 'info')
    }
}

private lineProtocolTemplateMap() { [
        all       : 'a', // all
        attribute : 'b', // metadata
        colorMap  : 'c', // event
        day       : 'd', // event
        enum      : 'e', // event
        device    : 'f', // metadata
        hub       : 'h', // event
        local     : 'l', // metadata
        number    : 'n', // event
        statHub   : 'p', // status
        statDev   : 'q', // status
        string    : 's', // event
        vector3   : 'v', // event
        zwCCs     : 'y', // config
        zwave     : 'z', // config
] }

/*****************************************************************************************************************
 *  Tags Map for Line Protocol:
 *****************************************************************************************************************/
def tags() { [
    [name: 'area',          level: 1, clos: 'locationName',          args: 0, type: ['a'], esc: true, ident: true],
    [name: 'areaId',        level: 2, clos: 'locationId',            args: 1, type: ['a'], parent: true],
    [name: 'building',      level: 1, clos: 'hubName',               args: 0, type: ['a'], esc: true, ident: true],
    [name: 'buildingId',    level: 2, clos: 'hubId',                 args: 1, type: ['a'], parent: true],
    [name: 'chamber',       level: 1, clos: 'groupName',             args: 1, type: ['b','c','d','e','f','h','n','q','s','v','y','z'], esc: true, parent: true],
    [name: 'chamberId',     level: 2, clos: 'groupId',               args: 1, type: ['b','c','e','f','n','q','s','v','y','z'], parent: true],
    [name: 'deviceCode',    level: 2, clos: 'deviceCode',            args: 1, type: ['b','c','e','f','n','q','s','v','y','z'], esc: true, ident: true, parent: true],
    [name: 'deviceHand',    level: 1, clos: 'deviceHandlerName',     args: 1, type: ['b','f','q','y','z'], esc: true, parent: true],
    [name: 'deviceId',      level: 2, clos: 'deviceId',              args: 1, type: ['b','c','e','f','n','q','s','v','y','z'], parent: true],
    [name: 'deviceLabel',   level: 1, clos: 'deviceLabel',           args: 1, type: ['b','c','e','f','n','q','s','v','y','z'], esc: true, parent: true],
    [name: 'deviceType',    level: 1, clos: 'deviceType',            args: 1, type: ['f','q']],
    [name: 'event',         level: 1, clos: 'eventName',             args: 1, type: ['b','c','d','e','h','n','s','v']],
    [name: 'eventType',     level: 1, clos: 'eventType',             args: 1, type: ['b']],
    [name: 'hubType',       level: 1, clos: 'hubType',               args: 0, type: ['l','p']],
    [name: 'identGlobal',   level: 1, clos: 'identGlobalEvent',      args: 1, type: ['c','d','e','h','n','s','v'], esc: true, ident: true],
    [name: 'identGlobal',   level: 1, clos: 'identGlobalHub',        args: 1, type: ['l','p'], esc: true, ident: true],
    [name: 'identGlobal',   level: 1, clos: 'identGlobalDevice',     args: 1, type: ['f','q','y','z'], esc: true, ident: true],
    [name: 'identGlobal',   level: 1, clos: 'identGlobalAttribute',  args: 2, type: ['b'], esc: true, ident: true],
    [name: 'identLocal',    level: 1, clos: 'identLocal',            args: 1, type: ['b','c','e','f','n','q','s','v'], esc: true,  ident: true, parent: true],
 // [name: 'isChange',      level: 2, clos: 'isChange',              args: 1, type: ['c', 'd', 'e', 'h', 'n', 's', 'v']],
 // [name: 'isDigital',     level: 2, clos: 'isDigital',             args: 1, type: ['c', 'd', 'e', 'h', 'n', 's', 'v']],
 // [name: 'isPhysical',    level: 2, clos: 'isPhysical',            args: 1, type: ['c', 'd', 'e', 'h', 'n', 's', 'v']],
    [name: 'listening',     level: 1, clos: 'listeningZwave',        args: 1, type: ['y','z']],
    [name: 'mdDeviceNo',    level: 2, clos: 'metadataDeviceNumber',  args: 1, type: ['f']],
    [name: 'mdDeviceType',  level: 2, clos: 'metadataDeviceType',    args: 1, type: ['f'], esc: true],
    [name: 'mdInventCode',  level: 2, clos: 'metadataInventoryCode', args: 1, type: ['f'], esc: true],
    [name: 'mdLocation',    level: 2, clos: 'metadataLocation',      args: 1, type: ['f'], esc: true],
    [name: 'mdRoom',        level: 2, clos: 'metadataRoom',          args: 1, type: ['f'], esc: true],
    [name: 'mdRoomNo',      level: 2, clos: 'metadataRoomNumber',    args: 1, type: ['f']],
    [name: 'mdSubLocation', level: 2, clos: 'metadataSubLocation',   args: 1, type: ['f'], esc: true],
    [name: 'power',         level: 1, clos: 'powerSource',           args: 1, type: ['f','q','y','z']],
    [name: 'source',        level: 2, clos: 'eventSource',           args: 1, type: ['c','e','n','s','v']],
    [name: 'status',        level: 1, clos: 'statusDevice',          args: 1, type: ['b','f','q','y','z'], parent: true],
    [name: 'statusHub',     level: 1, clos: 'statusHub',             args: 0, type: ['l','p','q']],
    [name: 'tempScale',     level: 2, clos: 'tempScale',             args: 0, type: ['l']],
    [name: 'timeZone',      level: 2, clos: 'timeZoneName',          args: 0, type: ['l']],
    [name: 'unit',          level: 1, clos: 'unit',                  args: 1, type: ['n','v'], esc: true],
] }

/*****************************************************************************************************************
 *  Fields Map for Line Protocol:
 *****************************************************************************************************************/
def fields() { [
    [name: '',              level: 2, clos: 'configuredParametersList', var: 'multiple', args: 1, type: ['z']],
    [name: 'battery',       level: 1, clos: 'battery',                  var: 'integer',  args: 1, type: ['f','q']],
    [name: 'checkInt',      level: 1, clos: 'checkInterval',            var: 'integer',  args: 1, type: ['q','z']],
    [name: 'configProfile', level: 1, clos: 'deviceConfiguredProfile',  var: 'string',   args: 1, type: ['z']],
    [name: 'configure',     level: 1, clos: 'configure',                var: 'string',   args: 1, type: ['f','q','z']],
    [name: 'deviceUse',     level: 1, clos: 'deviceUse',                var: 'string',   args: 1, type: ['z']],
    [name: 'eventText',     level: 3, clos: 'eventDescription',         var: 'string',   args: 1, type: ['c','d','e','h','n','s','v']],
    [name: 'eventId',       level: 1, clos: 'eventId',                  var: 'string',   args: 1, type: ['c','d','e','h','n','s','v']],
    [name: 'firmware',      level: 1, clos: 'firmwareVersion',          var: 'string',   args: 0, type: ['l','p']],
    [name: 'iP',            level: 1, clos: 'hubIPaddress',             var: 'string',   args: 0, type: ['l','p']],
    [name: 'latitude',      level: 1, clos: 'latitude',                 var: 'float',    args: 0, type: ['l'], ident: true],
    [name: 'longitude',     level: 1, clos: 'longitude',                var: 'float',    args: 0, type: ['l'], ident: true],
    [name: 'mdNotes',       level: 2, clos: 'metadataNotes',            var: 'string',   args: 1, type: ['f']],
    [name: 'mReceived',     level: 1, clos: 'messagesReceived',         var: 'integer',  args: 1, type: ['q']],
    [name: 'mSent',         level: 1, clos: 'messagesSent',             var: 'integer',  args: 1, type: ['q']],
    [name: 'nBin',          level: 2, clos: 'stateBinaryCurrent',       var: 'boolean',  args: 1, type: ['d','e','h']],
    [name: 'nHue',          level: 1, clos: 'hueCurrent',               var: 'integer',  args: 1, type: ['c']],
    [name: 'nLevel',        level: 1, clos: 'stateLevelCurrent',        var: 'integer',  args: 1, type: ['d','e','h']],
    [name: 'nSat',          level: 1, clos: 'saturationCurrent',        var: 'integer',  args: 1, type: ['c']],
    [name: 'nState',        level: 1, clos: 'stateCurrent',             var: 'string',   args: 1, type: ['d','e','h','s']],
    [name: 'nText',         level: 3, clos: 'stateDescriptionCurrent',  var: 'string',   args: 1, type: ['e'], ident: true],
    [name: 'nText',         level: 3, clos: 'valueDescriptionCurrent',  var: 'string',   args: 1, type: ['n'], ident: true],
    [name: 'nValue',        level: 1, clos: 'valueCurrent',             var: 'float',    args: 1, type: ['n']],
    [name: 'nValueRd',      level: 3, clos: 'valueRoundedCurrent',      var: 'string',   args: 1, type: ['n']],
    [name: 'nX',            level: 1, clos: 'xxxCurrent',               var: 'float',    args: 1, type: ['v']],
    [name: 'nY',            level: 1, clos: 'yyyCurrent',               var: 'float',    args: 1, type: ['v']],
    [name: 'nZ',            level: 1, clos: 'zzzCurrent',               var: 'float',    args: 1, type: ['v']],
    [name: 'onBattery',     level: 1, clos: 'batteryPower',             var: 'boolean',  args: 0, type: ['p']],
    [name: 'pBin',          level: 2, clos: 'stateBinaryPrevious',      var: 'boolean',  args: 1, type: ['e']],
    [name: 'pLevel',        level: 1, clos: 'stateLevelPrevious',       var: 'integer',  args: 1, type: ['e']],
    [name: 'pState',        level: 1, clos: 'statePrevious',            var: 'string',   args: 1, type: ['e']],
    [name: 'pText',         level: 3, clos: 'stateDescriptionPrevious', var: 'string',   args: 1, type: ['e']],
    [name: 'pText',         level: 3, clos: 'valueDescriptionPrevious', var: 'string',   args: 1, type: ['n']],
    [name: 'pValue',        level: 1, clos: 'valuePrevious',            var: 'float',    args: 1, type: ['n']],
    [name: 'rDiff',         level: 1, clos: 'difference',               var: 'float',    args: 1, type: ['n']],
    [name: 'rDiffText',     level: 3, clos: 'differenceText',           var: 'string',   args: 1, type: ['n']],
    [name: 'sBin',          level: 2, clos: 'statusDeviceBinary',       var: 'boolean',  args: 1, type: ['f','q','z']],
    [name: 'sBin',          level: 2, clos: 'statusHubBinary',          var: 'boolean',  args: 0, type: ['l','p']],
    [name: 'sLevel',        level: 1, clos: 'statusDeviceLevel',        var: 'integer',  args: 1, type: ['f','q','z']],
    [name: 'sLevel',        level: 1, clos: 'statusHubLevel',           var: 'integer',  args: 0, type: ['l','p']],
    [name: 'secLevel',      level: 1, clos: 'networkSecurityLevel',     var: 'string',   args: 1, type: ['z']],
    [name: 'secure',        level: 1, clos: 'zwaveSecure',              var: 'boolean',  args: 1, type: ['y','z']],
    [name: 'sunrise',       level: 1, clos: 'sunrise',                  var: 'string',   args: 0, type: ['l'], ident: true],
    [name: 'sunset',        level: 1, clos: 'sunset',                   var: 'string',   args: 0, type: ['l'], ident: true],
    [name: 'tBattery',      level: 1, clos: 'batteryChangeDate',        var: 'integer',  args: 1, type: ['f','q']],
    [name: 'tCP',           level: 1, clos: 'hubTCPport',               var: 'integer',  args: 0, type: ['l']],
    [name: 'tDay',          level: 1, clos: 'timeOfDay',                var: 'integer',  args: 1, type: ['e','n']],
    [name: 'tElap',         level: 1, clos: 'timeElapsed',              var: 'integer',  args: 1, type: ['e','n']],
    [name: 'tElapText',     level: 3, clos: 'timeElapsedText',          var: 'string',   args: 1, type: ['e','n']],
    [name: 'tLastAct',      level: 1, clos: 'timeLastActivity',         var: 'integer',  args: 1, type: ['f']],
    [name: 'tLastEvent',    level: 1, clos: 'timeLastEvent',            var: 'integer',  args: 2, type: ['b']],
    [name: 'tOffset',       level: 2, clos: 'timeOffsetCurrent',        var: 'integer',  args: 1, type: ['e']],
    [name: 'tStamp',        level: 2, clos: 'timestamp',                var: 'integer',  args: 1, type: ['d','e','h','n','v']],
    [name: 'tWrite',        level: 2, clos: 'timeWrite',                var: 'integer',  args: 0, type: ['e','n','v']],
    [name: 'vLastEvent',    level: 1, clos: 'valueLastEvent',           var: 'string',   args: 2, type: ['b']],
    [name: 'wakeUpInt',     level: 1, clos: 'wakeUpInterval',           var: 'integer',  args: 1, type: ['q','z']],
    [name: 'wLevel',        level: 1, clos: 'timeWeightedLevel',        var: 'integer',  args: 1, type: ['e']],
    [name: 'wValue',        level: 1, clos: 'timeWeightedValue',        var: 'float',    args: 1, type: ['n']],
    [name: 'zigbeeP',       level: 2, clos: 'zigbeePowerLevel',         var: 'integer',  args: 0, type: ['l']],
    [name: 'zwaveP',        level: 2, clos: 'zwavePowerLevel',          var: 'string',   args: 0, type: ['l']],
    [name: '',              level: 3, clos: 'commandClassesList',       var: 'multiple', args: 1, type: ['y']],
] }

/*****************************************************************************************************************
 *  Tag and Field Closures (alphabetical listing)
 *****************************************************************************************************************/
/**
 * getAttributeStates - helper - gets attribute levels from getEventDetails helper
 * @return map of attribute states and levels
 */
def getAttributeStates() { return { eventDetails(it).levels } }

/**
 * getBattery - gets latest value of 'battery' attribute for the device
 * @return value of 'battery' attribute
 */
def getBattery() { return { it?.latestValue('battery') ?: '' } }

/**
 * getBatteryChangeDate - gets value of 'batteryChange' attribute used to log a battery change in the modile app
 * @return timestamp
 */
def getBatteryChangeDate() { return { it?.latestState('batteryChange')?.date?.time ?: '' } }

/**
 * getBatteryPower - gets whether or not hub is running on batteries (v2 hub)
 * TODO - Drop this as no longer much use (v2 hubs only)?
 * @return true/false
 */
def getBatteryPower() { return { ->
    def battery = hub()?.hub?.getDataValue('batteryInUse')
    if (battery) {
        (battery.contains('true')) ? 't' : 'f'
    }
    else {
        null
    }
} }

/**
 * getCheckInterval - gets latest value of 'checkInterval' attribute for the device
 * This is set at twice the device Wake Up Interval and is used to establish whether or not a device is online.
 * @return value of 'checkInterval'
 */
def getCheckInterval() { return { it?.latestValue('checkInterval') ?: '' } }

/**
 * getColorMapCurrent - helper - parses json color map event value
 * @return parsed color map
 */
def getColorMapCurrent() { return { parseJson(it) } }

/**
 * getCommandClassesList - for Zwave devices, gets report of Command Classes from infoZwave
 * @return zwave report
 */
def getCommandClassesList() { return {
    def info = infoZwave(it).clone()
    def cc = info.cc
    if (info?.ccOut) cc.addAll(info.ccOut)
    if (info?.sec) cc.addAll(info.sec)
    def ccList = 'zz' + cc.sort().join('=t,zz') + '=t' // 't' is InfluxLP for 'true'
    info.remove('zw')
    info.remove('cc')
    info.remove('endpointInfo')
    if (info?.ccOut) info.remove('ccOut')
    if (info?.sec) info.remove('sec')
    info = info.sort()
    def toKeyValue = { it.collect { /$it.key="$it.value"/ } join ',' }
    info = toKeyValue(info) + ',' + "${ccList}"
    info
} }

/**
 * getConfigure - gets latest value of 'configure' attribute to report configuration state of device
 * @return configuration state of the device
 */
def getConfigure() { return { it?.latestValue('configure') ?: '' } }

/**
 * getConfiguredParametersList - gets the data value 'configuredParameters' reported by the device handler for the device
 * Series of configuration parameter numbers and their values reported for the device.
 * Prefixed 'c' in front for compatibility with Grafana.
 * @return comma-separated series of configuration values
 */
def getConfiguredParametersList() { return {
    def params = it?.device?.getDataValue('configuredParameters')
    if (params && params != 'null') {
        'c' + params.replaceAll(',', 'i,c') + 'i'
    }
    else {
        ''
    }
} }

/**
 * getDaylight - helper gets Sunrise and Sunset time object from SmartThings system
 * @return sunrise and sunset object
 */
def getDaylight() { return { -> getSunriseAndSunset() } }

/**
 * getDecimalPlaces - helper - gets decimal places to round to from getEventDetails
 * @return decimal places to round to
 */
def getDecimalPlaces() { return { eventDetails(it)?.decimalPlaces } }

/**
 * getDeviceCode - code allocated to device in IDE
 * Initially set to join name assigned in device handler.
 * Accessed via an event object, otherwise get via device object.
 * @return device code (name)
 */
def getDeviceCode() { return { (isEventObject(it)) ? (it?.device?.device?.name ?: 'unassigned') : (it?.name ?: 'unassigned') } }

/**
 * getDeviceConfiguredProfile - gets the data value 'configuredProfile' reported by the device handler for the device
 * Device handler may configure the device with a particular configuration profile.
 * @return name of configuration profile
 */
def getDeviceConfiguredProfile() { return { it?.device?.getDataValue('configuredProfile') ?: '' } }

/**
 * getDeviceHandlerName - name of device handler given in handler metadata
 * @return name of device handler
 */
def getDeviceHandlerName() { return { it?.typeName } }

/**
 * getDeviceId - id of device
 * Contained within an event object, otherwise get via device object.
 * @return device id
 */
def getDeviceId() { return { (isEventObject(it)) ? it?.deviceId : it?.id } }

/**
 * getDeviceLabel - label of device (used in mobile app)
 * Contained within an event object (except for daylight and hubStatus events), otherwise get via device object.
 * Uses .displayName instead of .label (in case label not set).
 * @return device label
 */
def getDeviceLabel() { return {
    if (isEventObject(it)) {
        if (eventName(it) == 'daylight') {
            'Day'
        }
        else if (eventName(it) == 'hubStatus') {
            'Hub'
        }
        else {
            it?.device?.device?.displayName ?: 'unassigned'
        }
    }
    else {
        it?.displayName ?: 'unassigned'
    }
} }

/**
 * getDeviceType - type of device according to communication protocol
 * @return device communication protocol type
 */
def getDeviceType() { return {
    if (infoZwave(it)) {
        'zwave'
    }
    else if (it?.device?.zigbeeId) {
        'zigbee'
    }
    else {
        'lan'
    }
} }

/**
 * getDeviceUse - gets data value 'deviceUse' which indicates what device is being used for
 * TODO - Drop this as no longer used in new device handlers?
 * @return device use name
 */
def getDeviceUse() { return { it?.device?.getDataValue('deviceUse') ?: '' } }

/**
 * getDifference - calculates the difference between current and previous event values.
 * Used in text description, so rounded values are used for consistency.
 * @return change in value
 */
def getDifference() { return { (valueCurrent(it).setScale(decimalPlaces(it), BigDecimal.ROUND_HALF_EVEN) - valuePrevious(it).setScale(decimalPlaces(it), BigDecimal.ROUND_HALF_EVEN)).toBigDecimal().setScale(decimalPlaces(it), BigDecimal.ROUND_HALF_EVEN) } }

/**
 * getDifferenceText - gets text term for change in value from previous to current event value
 * @return text term for change in value
 */
def getDifferenceText() { return { (difference(it) > 0) ? 'increased' : (difference(it) < 0) ? 'decreased' : 'unchanged' } }

/**
 * getEventDescription - tidies up event description generated by SmartThings system where there is metadata missing
 * @return event description text
 */
def getEventDescription() { return { it?.descriptionText?.replaceAll('\u00B0', ' ').replace('{{ locationName }}', "${locationName()}").replace('{{ linkText }}', "${deviceLabel(it)}").replace('{{ value }}', "${it.value}").replace('{{ name }} ', '') } }

/**
 * getEventDetails - helper - looks up details of attribute from map
 * @return attribute details
 */
def getEventDetails() { return { getAttributeDetail().find { attr -> attr.key == eventName(it) }.value } }

/**
 * getEventId - gets unique id of event object
 * @return event id
 */
def getEventId() { return { it.id } }

/**
 * getEventName - name of event or attribute
 * Puts sunrise and sunset events into common 'daylight' event.
 * If not an event object, returns it (attribute metadata).
 * @return event or attribute name
 */
def getEventName() { return {
    if (isEventObject(it)) {
        (it.name in ['sunrise', 'sunset']) ? 'daylight' : it.name
    }
    else {
        it
    }
} }

/**
 * getEventPrevious - helper
 * TODO - Check that previous event pulled from data field correctly (not implemented by handler yet).
 * @return previous event or null if no previous event (i.e. first event for a given device.attribute)
 */
def getEventPrevious() { return {
    def eventPrevious
    def eventData = parseJson(it?.data)
    if (eventData?.previous) {
        eventPrevious = [value: eventData?.previous?.value, date: it?.data?.previous?.date]
    }
    else {
        def history = it?.device?.statesSince("${it.name}", it.date - 7, [max: 5])
        if (history) {
            eventPrevious = history.sort { a, b -> b.date.time <=> a.date.time }.find { previous -> previous.date.time < it.date.time }
        }
    }
    eventPrevious
} }

/**
 * getEventSource - indicates where event orignates from
 * @return even event source
 */
def getEventSource() { return { "${it?.source}".toLowerCase().replaceAll('_', '') } }

/**
 * getEventType - gets type of event (eg number, string etc)
 * @return event type
 */
def getEventType() { return { eventDetails(it).type } }

/**
 * getFirmwareVersion - gets firmware of device as reported by the device handler
 * @return device firmware version
 */
def getFirmwareVersion() { return { -> hub().firmwareVersionString } }

/**
 * getGroupId - gets group id of device
 * Contained within an event object, otherwise get via device object.
 * @return
 */
def getGroupId() { return {
    if (isEventObject(it)) {
        it?.device?.device?.groupId ?: 'unassigned'
    }
    else {
        it?.device?.groupId ?: 'unassigned'
    }
} }

/**
 * getGroupName - gets name of group (room) device is allocated to via map lookup
 * If room metadata is available, uses it.
 * Else uses an assigned group or if not assigned to a group, returns the value of state.dwellingType
 * @return name of group (room)
 */
def getGroupName() { return {
    if (metadataRoom(it)) {
        "${metadataRoom(it)}" + ((metadataRoomNumber(it)) ? " ${metadataRoomNumber(it)}" : '')
    }
    else {
        state?.groupNames?."${groupId(it)}" ?: state.dwellingType
    }
} }

/**
 * getHub - helper - potentially more than one hub at a given location, but currently restricted to one.
 * Note device.hub can get a device's hub
 * @return hub object
 */
def getHub() { return { -> location.hubs[0] } }

/**
 * getHubId - Id of hub.
 * Contained within an event object, otherwise get via hub object.
 * @return hub id
 */
def getHubId() { return { (isEventObject(it)) ? it?.hubId : hub()?.id } }

/**
 * getHubIPaddress - gets IP address of hub on local network
 * @return ip address
 */
def getHubIPaddress() { return { -> hub().localIP } }

/**
 * getHubName - Name assigned to SmartThings hub
 * @return name of hub
 */
def getHubName() { return { -> hub().name } }

/**
 * getHubTCPport - gets tcp port of hub on local network
 * @return tcp port
 */
def getHubTCPport() { return { -> hub().localSrvPortTCP } }

/**
 * getHubType - distinguishes between physical and virtual hubs
 * @return hub type
 */
def getHubType() { return { -> "${hub().type}".toLowerCase() } }

/**
 * getHueCurrent - gets hue value from color map event value
 * @return hue value
 */
def getHueCurrent() { return { colorMapCurrent(it).hue } }

/**
 * getIdentGlobalAttribute
 * @return
 */
def getIdentGlobalAttribute() { return { dev, attr -> "${locationName()} . ${hubName()} . ${identLocal(dev)} . ${attr.capitalize()}" } }

/**
 * getIdentGlobalDevice
 * @return
 */
def getIdentGlobalDevice() { return { "${locationName()} . ${hubName()} . ${identLocal(it)}" } }

/**
 * getIdentGlobalEvent
 * @return
 */
def getIdentGlobalEvent() { return { "${locationName()} . ${hubName()} . ${identLocal(it)} . ${eventName(it).capitalize()}" } }

/**
 * getIdentGlobalHub
 * @return
 */
def getIdentGlobalHub() { return { "${locationName()} . ${hubName()} . ${state.dwellingType} . Hub" } }

/**
 * getIdentLocal - creates local identifier for device
 * If there is metadata entered in device handler, uses metadata.
 * Otherwise uses details from Group Names.
 * @return
 */
def getIdentLocal() { return {
    if (metadataDeviceType(it)) {
        def label = "${metadataRoom(it)} . ${metadataDeviceType(it)}"
        label += metadataDeviceNumber(it) ? " ${metadataDeviceNumber(it)} " : ''
        label += (metadataLocation(it) || metadataSubLocation(it)) ? ' .' : ''
        label += metadataSubLocation(it) ? " ${metadataSubLocation(it)}" : ''
        label += metadataLocation(it) ? " ${metadataLocation(it)}" : ''
        label
    }
    else {
        "${groupName(it)} . ${deviceLabelShort(it)}"
    }
} }

/**
 * getInfoZwave - helper to get Zwave information about the device
 * @return zwave information object
 */
def getInfoZwave() { return { it?.getZwaveInfo() } }

/**
 * getIsChange - indicates whether or not attribute value has changed/forced report
 * Unused.
 * @return
 */
def getIsChange() { return { it?.isStateChange } }

/**
 * getIsDigital - used to distinguish between virtual and physical events
 * Unused.
 * @return true if event is not physical
 */
def getIsDigital() { return { it?.isDigital } }

/**
 * getIsEventObject - helper - checks to see if object is an event
 * @return true if event object
 */
def getIsEventObject() { return { it?.respondsTo('isStateChange') } }

/**
 * getIsPhysical - used to distinguish between virtual and physical events
 * Unused.
 * @return true if event is physical
 */
def getIsPhysical() { return { it?.isPhysical } }

/**
 * getListeningZwave - gets type of Zwave device
 * @return zwave device type
 */
def getListeningZwave() { return {
    switch(infoZwave(it)?.zw.take(1)) {
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

/**
 * getLatitude - gets latitude for hub according to location set in mobile app
 * @return latitude
 */
def getLatitude() { return { -> location?.latitude ?: '' } }

/**
 * getLocationId - SmartThings account location id
 * Contained within an event object, otherwise get via location object.
 * @return location id
 */
def getLocationId() { return { (isEventObject(it)) ? it?.locationId : location?.id } }

/**
 * getLocationName - SmartThings account set location
 * @return name of location
 */
def getLocationName() { return { -> location?.name  ?: '' } }

/**
 * getLongitude - gets longitude for hub according to location set in mobile app
 * @return longitude
 */
def getLongitude() { return { -> location?.longitude ?: '' } }

/**
 * getMessagesReceived - number of messages sent to (received by) device
 * @return received messages
 */
def getMessagesReceived() { return { it?.device?.getDataValue('messagesReceived') ?: '' } }

/**
 * getMessagesSent - number of messages sent by (received from) device
 * @return sent messages
 */
def getMessagesSent() { return { it?.device?.getDataValue('messagesSent') ?: '' } }

/**
 * getMetadataDeviceNumber - gets Device Number (if more than one in a room) entered via mobile app
 * @return device number
 */
def getMetadataDeviceNumber() { return {
    if (isEventObject(it)) {
        it?.device?.device?.getDataValue('metadataDeviceNumber') ?: ''
    }
    else {
        it?.device?.getDataValue('metadataDeviceNumber') ?: ''
    }
} }

/**
 * getMetadataDeviceType - gets Device Type entered via mobile app
 * @return device type
 */
def getMetadataDeviceType() { return {
    if (isEventObject(it)) {
        it?.device?.device?.getDataValue('metadataDeviceType') ?: ''
    }
    else {
        it?.device?.getDataValue('metadataDeviceType') ?: ''
    }
} }

/**
 * getMetadataInventoryCode - gets device Inventory Code entered via mobile app
 * @return inventory code
 */
def getMetadataInventoryCode() { return {
    if (isEventObject(it)) {
        it?.device?.device?.getDataValue('metadataInventoryCode') ?: ''
    }
    else {
        it?.device?.getDataValue('metadataInventoryCode') ?: ''
    }
} }

/**
 * getMetadataLocation - gets device location within room entered via mobile app
 * @return device location
 */
def getMetadataLocation() { return {
    if (isEventObject(it)) {
        it?.device?.device?.getDataValue('metadataLocation') ?: ''
    }
    else {
        it?.device?.getDataValue('metadataLocation') ?: ''
    }
} }

/**
 * getMetadataNotes - gets any metadata notes regarding device installation entered via mobile app
 * @return notes
 */
def getMetadataNotes() { return {
    if (isEventObject(it)) {
        it?.device?.device?.getDataValue('metadataNotes') ?: ''
    }
    else {
        it?.device?.getDataValue('metadataNotes') ?: ''
    }
} }

/**
 * getMetadataRoom - gets name of room device is installed in entered via mobile app
 * @return room name
 */
def getMetadataRoom() { return {
    if (isEventObject(it)) {
        it?.device?.device?.getDataValue('metadataRoom') ?: ''
    }
    else {
        it?.device?.getDataValue('metadataRoom') ?: ''
    }
} }

/**
 * getMetadataRoomNumber - gets number of room (if more than one of same type) device is installed in entered via mobile app
 * @return room number
 */
def getMetadataRoomNumber() { return {
    if (isEventObject(it)) {
        it?.device?.device?.getDataValue('metadataRoomNumber') ?: ''
    }
    else {
        it?.device?.getDataValue('metadataRoomNumber') ?: ''
    }
} }

/**
 * getMetadataSubLocation - gets device sublocation within room entered via mobile app
 * @return device sublocation
 */
def getMetadataSubLocation() { return {
    if (isEventObject(it)) {
        it?.device?.device?.getDataValue('metadataSubLocation') ?: ''
    }
    else {
        it?.device?.getDataValue('metadataSubLocation') ?: ''
    }
} }

/**
 * getNetworkSecurityLevel - gets network security level for the device as reported by SmartThings system
 * @return network security level description
 */
def getNetworkSecurityLevel() { return { it?.device?.getDataValue('networkSecurityLevel')?.replace('ZWAVE_', '')?.replaceAll('_', ' ') ?: '' } }

/**
 * getPowerSource - gets latest value of powerSource attribute for device
 * @return powerSource attribute
 */
def getPowerSource() { return { it?.latestValue('powerSource')?.toLowerCase() ?: 'unknown' } }

/**
 * getSaturationCurrent - gets saturation value from color map event value
 * @return saturation value
 */
def getSaturationCurrent() { return { colorMapCurrent(it).saturation } }

/**
 * getStateBinaryCurrent - converts attribute level to a binary flag based on level value
 * @return attribute true/false
 */
def getStateBinaryCurrent() { return { stateLevelCurrent(it) > 0 ? 't' : 'f' } }

/**
 * getStateBinaryPrevious - converts attribute level to a binary flag based on level value
 * @return previous event attribute true/false
 */
def getStateBinaryPrevious() { return { stateLevelPrevious(it) > 0 ? 't' : 'f' } }

/**
 * getStateCurrent - current state of attribute with adjustment for sunrise and sunset events
 * @return attribute state
 */
def getStateCurrent() { return { it?.name in ['sunrise', 'sunset'] ? it.name : it.value } }

/**
 * getStateDescriptionCurrent - compiles a textual description for state events
 * TODO - 'sun has risen' / 'sun has set' for 'daylight' events - leave for now.
 * @return textual description for state events
 */
def getStateDescriptionCurrent() { return { "At ${locationName()}, in ${hubName()}, ${deviceLabel(it)} is ${stateCurrent(it)} in the ${groupName(it)}." } }

/**
 * getStateDescriptionPrevious - compiles a textual description of change from previous to current event
 * @return textual description of change for state events
 */
def getStateDescriptionPrevious() { return { "This is a change from ${statePrevious(it)} ${timeElapsedText(it)}." } }

/**
 * getStateLevelCurrent - gets level corresponding to current state of attribute
 * @return attribute level
 */
def getStateLevelCurrent() { return { attributeStates(it).find { level -> level.key == stateCurrent(it) }.value } }

/**
 * getStateLevelPrevious - gets level corresponding to current state of attribute
 * @return previous event attribute level
 */
def getStateLevelPrevious() { return { attributeStates(it)?.find { level -> level.key == statePrevious(it) }?.value } }

/**
 * getStatePrevious - previous state of attribute
 * TODO - Check sunrise/sunset conversion needed? - Don't think so.
 * @return previous event attribute state
 */
def getStatePrevious() { return { eventPrevious(it)?.value } }

/**
 * getStatusDevice - gets status of device
 * TODO - Convert active/inactive to online/offline? - Leave for now.
 * @return device status
 */
def getStatusDevice() { return { "${it?.status}".toLowerCase().replaceAll('_', '') } }

/**
 * getStatusDeviceBinary - reports if device is active/online as a binary flag
 * @return true/false
 */
def getStatusDeviceBinary() { return { (statusDevice(it) in ['active', 'online']) ? 't' : 'f' } }

/**
 * getStatusDeviceLevel - reports if device is active/online as a level
 * @return
 */
def getStatusDeviceLevel() { return { (statusDevice(it) in ['active', 'online']) ? 1 : -1 } }

/**
 * getStatusHub - gets status of hub
 * @return hub status
 */
def getStatusHub() { return { -> "${hub()?.status}".toLowerCase().replaceAll('_', '') } }

/**
 * getStatusHubBinary - reports if hub is active as a binary flag
 * @return true/false
 */
def getStatusHubBinary() { return { (statusHub() == 'active') ? 't' : 'f' } }

/**
 * getStatusHubLevel - reports if hub is active as a level
 * @return
 */
def getStatusHubLevel() { return { (statusHub() == 'active') ? 1 : -1 } }

/**
 * getSunrise - gets time of sunrise depending on hub location set in mobile app
 * @return time of sunrise
 */
def getSunrise() { return { -> daylight().sunrise.format('HH:mm', location.timeZone) } }

/**
 * getSunset - gets time of sunset depending on hub location set in mobile app
 * @return time of sunset
 */
def getSunset() { return { -> daylight().sunset.format('HH:mm', location.timeZone) } }

/**
 * getTempScale - gets temperature scale set in mobile app
 * @return temperature scale
 */
def getTempScale() { return { -> location?.temperatureScale } }

/**
 * getTimeElapsed - gets the elapsed time (in milliseconds) between current and previous events
 * @return time difference in milliseconds
 */
def getTimeElapsed() { return { timestamp(it) - eventPrevious(it).date.time - timeOffsetPrevious(it) } }

/**
 * getTimeElapsedText - converts the elapsed time between current and previous events to an appropriate text description
 * @return text description of elapsed time
 */
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

/**
 * getTimeLastActivity - gets timestamp of last activity reported by a device
 * @return timestamp
 */
def getTimeLastActivity() { return { it?.lastActivity?.time ?: 0 } }

/**
 * getTimeLastEvent - gets timestamp of latest value for each attribute reported by a device
 * Reports 0 if null, to indicate that attribute has never been reported by device (may indicate a potential fault)
 * @return timestamp
 */
def getTimeLastEvent() { return { dev, attr -> dev?.latestState(attr)?.date?.time ?: 0 } }

/**
 * getTimeOfDay - calculates elapsed time of day in milliseconds
 * Used in time-weighted-average calculations.
 * @return time of day in milliseconds
 */
def getTimeOfDay() { return { timestamp(it) - it.date.clone().clearTime().time } }

/**
 * getTimeOffsetCurrent - helper - calculates any time offset for current event
 * @return time offset for current event
 */
def getTimeOffsetCurrent() { return { (eventName(it) == 'motion' && stateCurrent(it) == 'inactive') ? timeOffsetAmount() : 0 } }

/**
 * getTimeOffsetAmount - helper - returns motion sensor timestamp offset period
 * @return time period
 */
def getTimeOffsetAmount() { return { -> (1000 * 10 / 2) } }

/**
 * getTimeOffsetPrevious - helper - calculates any time offset for previous event
 * @return
 */
def getTimeOffsetPrevious() { return { (eventName(it) == 'motion' && statePrevious(it) == 'inactive') ? timeOffsetAmount() : 0 } }

/**
 * getTimestamp - gets timestamp (in milliseconds) of event
 * The timestamps of motion events are adjusted by an offset amount to account for the response time of the sensor.
 * @return
 */
def getTimestamp() { return { it.date.time - timeOffsetCurrent(it) } }

/**
 * getTimeWrite - gets timestamp of when an event is processed by logger app
 * @return processing timestamp
 */
def getTimeWrite() { return { -> new Date().time } }

/**
 * getTimeWeightedLevel - calculates time-weighted value for state events using state level
 * @return time-weighted state level
 */
def getTimeWeightedLevel() { return {  stateLevelPrevious(it) * timeElapsed(it) } }

/**
 * getTimeWeightedValue - calculates time-weighted value for value events
 * @return time-weighted value
 */
def getTimeWeightedValue() { return {  valuePrevious(it) * timeElapsed(it) } }

/**
 * getTimeZoneName - gets timezone according to hub location set in mobile app
 * @return name of timezone
 */
def getTimeZoneName() { return { -> location?.timeZone.ID } }

/**
 * getUnit - unit for events that have them
 * Removes circle from C unit - causes formatting problems.
 * @return name of unit
 */
def getUnit() { return {
    def unit = it?.unit ?: eventDetails(it).unit
    if (it.name == 'temperature') unit.replaceAll('\u00B0', '')
    unit
} }

/**
 * getValueCurrent - some device handlers append unit to number, so any units will be removed
 * @return event value as a number
 */
def getValueCurrent() { return {
    try {
        it.numberValue.toBigDecimal()
    }
    catch(e) {
        removeUnit(it)
    }
} }

/**
 * getValueDescriptionCurrent - compiles a textual description for value events
 * @return textual description for value events
 */
def getValueDescriptionCurrent() { return { "At ${locationName()}, in ${hubName()}, ${eventName(it)} is ${valueRoundedCurrent(it)} ${unit(it)} in the ${groupName(it)}." } }

/**
 * getValueDescriptionPrevious - compiles a textual description of change from previous to current event
 * @return textual description of change for value events
 */
def getValueDescriptionPrevious() { return {
    def changeAbs = (differenceText(it) == 'unchanged') ? 'unchanged' : "${differenceText(it)} by ${difference(it).abs()} ${unit(it)}"
    "This is ${changeAbs} compared to ${timeElapsedText(it)}."
} }

/**
 * getValueLastEvent - gets latest value for each attribute reported by a device
 * @return value
 */
def getValueLastEvent() { return { dev, attr -> "${dev?.latestValue(attr)}" ?: 'null' } }

/**
 * getValuePrevious - some device handlers append unit to number, so any units will be removed
 * @return previous event value as a number
 */
def getValuePrevious() { return {
    try {
        eventPrevious(it)?.numberValue.toBigDecimal()
    }
    catch(e) {
        removeUnit(eventPrevious(it))
    }
} }

/**
 * getValueRoundedCurrent - helper - rounds current event value
 * @return rounder number
 */
def getValueRoundedCurrent() { return { valueCurrent(it).setScale(decimalPlaces(it), BigDecimal.ROUND_HALF_EVEN) } }

/**
 * getWakeUpInterval - wake up interval of the device
 * @return duration (seconds)
 */
def getWakeUpInterval() { return { it?.device?.getDataValue('wakeUpInterval') ?: '' } }

/**
 * getXCurrent - gets x value for 3-axis events, converted to g unit
 * @return x value
 */
def getXxxCurrent() { return { it.xyzValue?.x / gravityFactor() ?: 0 } }

/**
 * getYCurrent - gets y value for 3-axis events, converted to g unit
 * @return y value
 */
def getYyyCurrent() { return { it.xyzValue?.y / gravityFactor() ?: 0 } }

/**
 * getZCurrent - gets z value for 3-axis events, converted to g unit
 * @return z value
 */
def getZzzCurrent() { return { it.xyzValue?.z / gravityFactor() ?: 0 } }

/**
 * getZigbeePowerLevel - power level for Zigbee devices
 * @return power level
 */
def getZigbeePowerLevel() { return { -> hub().hub.getDataValue('zigbeePowerLevel') } }

/**
 * getZwavePowerLevel - power level for Zwave devices
 * @return power level
 */
def getZwavePowerLevel() { return { -> hub().hub.getDataValue('zwavePowerLevel') } }

/**
 * getZwaveSecure - gets whether device is securely included or not
 * @return true/false
 */
def getZwaveSecure() { return { (infoZwave(it)?.zw.endsWith('s')) ? 't' : 'f' } }

/**
 * getDeviceLabelShort - removes room name at start of device label
 * (Up to space after dot.) eg Kitchen . Drawer 1
 * @return
 */
def getDeviceLabelShort() { return { deviceLabel(it) - ~/.+\s?+\.+\s?+/ } }

/**
 * getGravityFactor - helper - returns conversion factor to g unit (for Smartsense 3-axis sensor).
 * @return g unit conversion factor value
 */
def getGravityFactor() { return { -> (1024) } }

/**
 * removeUnit - helper - removes any units appending to end of event value by a device handler
 * @return number
 */
def removeUnit() { return {
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
    }
    else {
        value.toBigDecimal()
    }
} }

/*****************************************************************************************************************
 *  Post Data to InfluxDB (postToInfluxDBLocal, handleInfluxDBResponseLocal, postToInfluxDBRemote, handleInfluxDBResponseRemote)
 *****************************************************************************************************************/
/**
 * postToInfluxDBLocal - posts data to InfluxDB server on local network using HubAction method
 * @param data
 * @param retentionPolicy
 * @param bucket
 * @param eventId
 * @return
 */
private postToInfluxDBLocal(data, retentionPolicy, bucket, eventId) {
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

    logger("postToInfluxDBhubAction: Posting data to InfluxDB: Host: ${state.uri}${params.path} Data: ${data}", 'trace')
    sendHubCommand(new physicalgraph.device.HubAction(params, null, options))
}

/**
 * handleInfluxDBResponseLocal - handles response from InfluxDB server on local network
 * @param hubResponse
 * @return
 */
def handleInfluxDBResponseLocal(physicalgraph.device.HubResponse hubResponse) {
    if (hubResponse.status == 204) {
        logger("postToInfluxDBLocal: Success! Code: ${hubResponse.status}", 'trace')
    }
    if (hubResponse.status >= 400) {
        logger("postToInfluxDBLocal: Something went wrong! Code: ${hubResponse.status} Message: ${hubResponse.json.error}", 'error')
    }
}

/**
 * postToInfluxDBRemote - posts data to InfluxDB server on remote network using asynchronous http method
 * @param data
 * @param retentionPolicy
 * @param bucket
 * @param eventId
 * @return
 */
private postToInfluxDBRemote(data, retentionPolicy, bucket, eventId) {
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

    logger("postToInfluxDBasynchttp: Posting data to InfluxDB: Host: ${state.uri}${params.path} Data: ${data}", 'trace')
    asynchttp_v1.post(handleInfluxDBResponseRemote, params, passData)
}

/**
 * handleInfluxDBResponseRemote - handles response from InfluxDB server on remote network
 * @param response
 * @param passData
 * @return
 */
def handleInfluxDBResponseRemote(response, passData) {
    if (response.status == 204) {
        logger("postToInfluxDBRemote: Success! Code: ${response.status} (id: ${passData.eventId})", 'trace')
    }
    if (response.status >= 400) {
        logger("postToInfluxDBRemote: Something went wrong! Code: ${response.status} Error: ${response.errorJson.error} (id: ${passData.eventId})", 'error')
    }
}

/*****************************************************************************************************************
 *  Subscription and Scheduling Methods (manageSubscriptions, manageSchedules, pollingMethods)
 *****************************************************************************************************************/
/**
 * manageSubscriptions - subscribes event handlers to listen for events
 * @return
 */
private manageSubscriptions() {
    logger('manageSubscriptions: Subscribing listeners to events.', 'info')
    unsubscribe()
    getSelectedDevices()?.each { dev ->
        if (!dev.displayName.startsWith("~")) {
            getDeviceAttributesSelected(dev)?.each { attr ->
                def type = getAttributeDetail().find { it.key == attr }.value.type
                switch(type) {
                    case 'enum':
                        logger("manageSubscriptions: Subscribing 'handleEnumEvent' listener to attribute: ${attr}, for device: ${dev}.", 'trace')
                        subscribe(dev, attr, handleEnumEvent)
                        break

                    case 'number':
                        logger("manageSubscriptions: Subscribing 'handleNumberEvent' listener to attribute: ${attr}, for device: ${dev}.", 'trace')
                        subscribe(dev, attr, handleNumberEvent)
                        break

                    case 'vector3':
                        logger("manageSubscriptions: Subscribing 'handleVector3Event' listener to attribute: ${attr}, for device: ${dev}.", 'trace')
                        subscribe(dev, attr, handleVector3Event)
                        break

                    case 'string':
                        logger("manageSubscriptions: Subscribing 'handleStringEvent' listener to attribute: ${attr}, for device: ${dev}.", 'trace')
                        subscribe(dev, attr, handleStringEvent)
                        break

                    case 'colorMap':
                        logger("manageSubscriptions: Subscribing 'handleColorMapEvent' listener to attribute: ${attr}, for device: ${dev}.", 'trace')
                        subscribe(dev, attr, handleColorMapEvent)
                        break

                    case 'json_object':
                        logger("manageSubscriptions: Subscribing 'handleJson_objectEvent' listener to attribute: ${attr}, for device: ${dev}.", 'trace')
                        subscribe(dev, attr, handleJsonObjectEvent)
                        break

                    default:
                        logger("manageSubscriptions: Unhandled attribute: ${attr}, for device: ${dev}.", 'error')
                        break
                }
            }
        }
    }

    logger("manageSubscriptions: Subscribing 'handleDaylight' listener to 'Sunrise' and 'Sunset' events.", 'info')
    subscribe(location, 'sunrise', handleDaylight)
    subscribe(location, 'sunset', handleDaylight)

    logger("manageSubscriptions: Subscribing 'handleHubStatus' listener to 'Hub Status' events.", 'info')
    subscribe(location.hubs[0], 'hubStatus', handleHubStatus)
}

/**
 * manageSchedules - manages scheduling of each polling method
 * @return
 */
private manageSchedules() {
    logger('manageSchedules: Schedulling polling methods.', 'info')
    pollingMethods().each {
        try {
            unschedule(it.key)
        }
        catch (e) {
            logger("manageSchedules: Unschedule ${it.key} failed!", 'error')
        }
        logger("manageSchedules: Scheduling ${it.key} to ${it.value}.", 'debug')
        "${it.value}"(it.key)
    }
}

/**
 * pollingMethods - map of polling methods to schedule with the given frequency
 * @return
 */
private pollingMethods() {
    [
        pollStatus            : 'runEvery1Hour',
        pollLocations         : 'runEvery3Hours',
        pollDevices           : 'runEvery3Hours',
        pollAttributes        : 'runEvery3Hours',
        pollZwavesCCs         : 'runEvery3Hours',
        pollZwaves            : 'runEvery3Hours',
        generateGroupNamesMap : 'runEvery3Hours',
    ]
}

/*****************************************************************************************************************
 *  General Helper Method (logger)
 *****************************************************************************************************************/
/**
 * logger - logs messages to IDE and creates events
 * @param msg
 * @param level
 * @return
 */
private logger(String msg, String level = 'debug') {
    switch (level) {
        case 'error':
            if (!state?.logLevelIDE || state?.logLevelIDE >= 1) {
                log.error(msg)
            }
            break

        case 'warn':
            if (!state?.logLevelIDE || state?.logLevelIDE >= 2) {
                log.warn(msg)
            }
            break

        case 'info':
            if (!state?.logLevelIDE || state?.logLevelIDE >= 3) {
                log.info(msg)
            }
            break

        case 'debug':
            if (!state?.logLevelIDE || state?.logLevelIDE >= 4) {
                log.debug(msg)
            }
            break

        case 'trace':
            if (!state?.logLevelIDE || state?.logLevelIDE >= 5) {
                log.trace(msg)
            }
            break

        default:
            log.error(msg)
            break
    }
}

/*****************************************************************************************************************
 *  Device and Attribute Selection Methods (getSelectedDeviceNames, getSelectedDevices, getAttributesSupported, getAttributesAll, getDeviceAttributesSelected)
 *****************************************************************************************************************/
/**
 * getSelectedDeviceNames - creates list of device names from list of device objects selected by user
 * @return
 */
private getSelectedDeviceNames() {
    def listSelectedDeviceNames = []
    try {
        // listSelectedDeviceNames = getSelectedDevices()?.collect { it?.displayName }?.sort()
        // listSelectedDeviceNames = getSelectedDevices()?.collect { deviceLabel(it) }?.sort()
        listSelectedDeviceNames = getSelectedDevices()?.collect { identLocal(it) }?.sort()
    }
    catch (e) {
        logger("selectedDeviceNames: Error while getting selected device names: ${e.message}.", 'warn')
    }
    listSelectedDeviceNames
}

/**
 * getSelectedDevices - creates list of device objects from devices selected by user
 * @return
 */
private getSelectedDevices() {
    def listSelectedDevices = []
    getCapabilities()?.each {
        try {
            if (settings?."${it.cap}Pref") {
                listSelectedDevices << settings?."${it.cap}Pref"
            }
        }
        catch (e) {
            logger("Error while getting selected devices for capability ${it}: ${e.message}", 'warn')
        }
    }
    listSelectedDevices?.flatten()?.unique { it.id }
}

/**
 * getAttributesSupported - iterates through list of all attributes to find those belonging to devices selected by the user
 * @return
 */
private getAttributesSupported() {
    def listAttributesSupported = []
    def devices = getSelectedDevices()
    if (devices) {
        getAttributesAll()?.each { attr ->
            try {
                if (devices?.find { dev -> dev?.hasAttribute(attr) } ) {
                    listAttributesSupported << attr
                }
            }
            catch (e) {
                logger("AttributesSupported: Error while finding supported devices for ${attr}: ${e.message}", 'warn')
            }
        }
    }
    listAttributesSupported?.unique()?.sort()
}

/**
 * getAttributesAll - iterates through capabilites map and creates a list of all standard attributes
 * no custom attributes - could get them from device.supportedAttributes
 * @return
 */
private getAttributesAll() {
    def listAttributesAll = []
    getCapabilities().each { cap ->
        try {
            if (cap?.attr) {
                if (cap.attr instanceof Collection) {
                    cap.attr.each {
                        listAttributesAll << it
                    }
                }
                else {
                    listAttributesAll << cap.attr
                }
            }
        }
        catch (e) {
            logger("getAttributesAll: Error while getting attributes for capability ${cap}: ${e.message}", 'warn')
        }
    }
    listAttributesAll?.unique()?.sort()
}

/**
 * getDeviceAttributesSelected - creates a list of selected attributes by for a given device by filtering list of all user selected attributes
 * @param device
 * @return
 */
private getDeviceAttributesSelected(device) {
    def listSelectedAttributes = []
    try {
        settings?.loggedAttributes?.each { attr ->
            try {
                if (device.hasAttribute(attr)) {
                    listSelectedAttributes << attr
                }
            }
            catch (e) {
                logger("getDeviceAttributesSelected: Error while getting selected attributes for ${device?.displayName} and attribute ${it}: ${e.message}", 'warn')
            }
        }
    }
    catch (e) {
        logger("getDeviceAttributesSelected: Error while getting device allowed attributes for ${device?.displayName}: ${e.message}", 'warn')
    }
    listSelectedAttributes.sort()
}

/*****************************************************************************************************************
 *  Metadata (getCapabilities, getAttributeDetail)
 *****************************************************************************************************************/
/**
 * getCapabilities
 * @return
 */
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

/**
 * getAttributeDetail
 * @return
 */
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