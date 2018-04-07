/*****************************************************************************************************************
 *  Copyright Alasdair Thin
 *
 *  Name: InfluxDB Smart Logger
 *
 *  Date: 2018-01-20
 *
 *  Version: 1.0
 *
 *  Source:
 *
 *  Author: Alasdair Thin
 *
 *  Description: A SmartApp to log SmartThings device states to an InfluxDB database.
 *
 *  Acknowledgements: Includes code originally developed by David Lomas (codersaur).
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
    name: "InfluxDB Logger",
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
    section("General:") {
        //input "prefDebugMode", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: true
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
        // input "prefLogModeEvents", "bool", title:"Log Mode Events?", defaultValue: false, required: true
        // input "prefDaylight", "bool", title:"Log times of Sunrise and Sunset?", defaultValue: true, required: true
        input "prefAdjustInactiveTimestamp", "bool", title:"Adjust 'Inactive' status timestamp to compensate for PIR reset time?", defaultValue: true, required: true
        input "prefRoomNameCapture", "bool", title:"Use Virtual Devices to Capture Room Names?", defaultValue: true, required: true
        // input "prefAllDevicesAttributes", "bool", title:"Subscribe to all devices and attributes?", defaultValue: true, required: true
    }

    section("Devices To Monitor:") {
        input "accelerometers", "capability.accelerationSensor", title: "Accelerometers", multiple: true, required: false
        input "alarms", "capability.alarm", title: "Alarms", multiple: true, required: false
        input "batteries", "capability.battery", title: "Batteries", multiple: true, required: false
        input "beacons", "capability.beacon", title: "Beacons", multiple: true, required: false
        input "bulbs", "capability.bulb", title: "Bulbs", multiple: true, required: false
        input "buttons", "capability.button", title: "Buttons", multiple: true, required: false
        input "cos", "capability.carbonMonoxideDetector", title: "Carbon Monoxide Detectors", multiple: true, required: false
        input "co2s", "capability.carbonDioxideMeasurement", title: "Carbon Dioxide Detectors", multiple: true, required: false
        input "colors", "capability.colorControl", title: "Color Controllers", multiple: true, required: false
        input "consumables", "capability.consumable", title: "Consumables", multiple: true, required: false
        input "contacts", "capability.contactSensor", title: "Contact Sensors", multiple: true, required: false
        input "doorsControllers", "capability.doorControl", title: "Door Controllers", multiple: true, required: false
        input "energyMeters", "capability.energyMeter", title: "Energy Meters", multiple: true, required: false
        input "holdables", "capability.holdableButton", title: "Holdable Buttons", multiple: true, required: false
        input "humidities", "capability.relativeHumidityMeasurement", title: "Humidity Meters", multiple: true, required: false
        input "illuminances", "capability.illuminanceMeasurement", title: "Illuminance Meters", multiple: true, required: false
        input "locks", "capability.lock", title: "Locks", multiple: true, required: false
        input "motions", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false
        input "musicPlayers", "capability.musicPlayer", title: "Music Players", multiple: true, required: false
        input "peds", "capability.stepSensor", title: "Pedometers", multiple: true, required: false
        input "phMeters", "capability.pHMeasurement", title: "pH Meters", multiple: true, required: false
        input "powerMeters", "capability.powerMeter", title: "Power Meters", multiple: true, required: false
        input "presences", "capability.presenceSensor", title: "Presence Sensors", multiple: true, required: false
        input "shockSensors", "capability.shockSensor", title: "Shock Sensors", multiple: true, required: false
        input "signalStrengthMeters", "capability.signalStrength", title: "Signal Strength Meters", multiple: true, required: false
        input "sleepSensors", "capability.sleepSensor", title: "Sleep Sensors", multiple: true, required: false
        input "smokeDetectors", "capability.smokeDetector", title: "Smoke Detectors", multiple: true, required: false
        input "soundSensors", "capability.soundSensor", title: "Sound Sensors", multiple: true, required: false
        input "spls", "capability.soundPressureLevel", title: "Sound Pressure Level Sensors", multiple: true, required: false
        input "switches", "capability.switch", title: "Switches", multiple: true, required: false
        input "switchLevels", "capability.switchLevel", title: "Switch Levels", multiple: true, required: false
        input "tamperAlerts", "capability.tamperAlert", title: "Tamper Alerts", multiple: true, required: false
        input "temperatures", "capability.temperatureMeasurement", title: "Temperature Sensors", multiple: true, required: false
        input "thermostats", "capability.thermostat", title: "Thermostats", multiple: true, required: false
        input "threeAxis", "capability.threeAxis", title: "Three-axis (Orientation) Sensors", multiple: true, required: false
        input "touchs", "capability.touchSensor", title: "Touch Sensors", multiple: true, required: false
        input "uvs", "capability.ultravioletIndex", title: "UV Sensors", multiple: true, required: false
        input "valves", "capability.valve", title: "Valves", multiple: true, required: false
        input "volts", "capability.voltageMeasurement", title: "Voltage Meters", multiple: true, required: false
        input "waterSensors", "capability.waterSensor", title: "Water Sensors", multiple: true, required: false
        input "windowShades", "capability.windowShade", title: "Window Shades", multiple: true, required: false
        input "bridges", "capability.bridge", title: "Virtual Devices to Capture Room Names", multiple: true, required: false
    }
}


/*****************************************************************************************************************
 *  SmartThings System Commands:
 *****************************************************************************************************************/

def installed() { // runs when the app is first installed
    state.installedAt = now()
    state.loggingLevelIDE = 5
    log.debug "${app.label}: Installed with settings: ${settings}"
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
    // state.path = "/write?db=${state.databaseName}&precision=ms"
    // state.pathExternal = "/write?db=${state.databaseName}&precision=ms&u=${state.databaseUser}&p=${state.databasePass}"
    state.path = "/write"
    state.query = [db: "${state.databaseName}", rp: 'autogen', precision: 'ms', u: "${state.databaseUser}", p: "${state.databasePass}"]

    state.headers = [:]
    state.headers.put("HOST", "${state.databaseHost}:${state.databasePort}")
    state.headers.put("Content-Type", "application/x-www-form-urlencoded")
    if (state.databaseUser && state.databasePass) {
        state.headers.put("Authorization", encodeCredentialsBasic(state.databaseUser, state.databasePass))
    }

    // System Monitoring settings:
    // state.daylight = settings.prefDaylight
    state.adjustInactiveTimestamp = settings.prefAdjustInactiveTimestamp
    state.roomNameCapture = settings.prefRoomNameCapture
    // state.allDevicesAttributes = settings.prefAllDevicesAttributes

    /* Build array of device collections and the attributes we want to report on for that collection: Note, the collection names are stored as strings. Adding references to the actual collection objects causes major issues (possibly memory issues?). */
    state.deviceAttributes = [
        [ devices: 'accelerometers', attributes: ['acceleration'], type: 'state'],
        [ devices: 'alarms', attributes: ['alarm'], type: 'state'],
        [ devices: 'batteries', attributes: ['battery'], type: 'value'],
        [ devices: 'beacons', attributes: ['presence'], type: 'state'],
        [ devices: 'bulbs', attributes: ['switch'], type: 'state'],
        [ devices: 'buttons', attributes: ['button'], type: 'state'],
        [ devices: 'cos', attributes: ['carbonMonoxide'], type: 'state'],
        [ devices: 'co2s', attributes: ['carbonDioxide'], type: 'state'],
        [ devices: 'colors', attributes: ['hue','saturation','color'], type: 'state'],
        [ devices: 'consumables', attributes: ['consumableStatus'], type: 'state'],
        [ devices: 'contacts', attributes: ['contact'], type: 'state'],
        [ devices: 'doorsControllers', attributes: ['door'], type: 'state'],
        [ devices: 'holdables', attributes: ['button'], type: 'state'],
        [ devices: 'energyMeters', attributes: ['energy'], type: 'value'],
        [ devices: 'humidities', attributes: ['humidity'], type: 'value'],
        [ devices: 'illuminances', attributes: ['illuminance'], type: 'value'],
        [ devices: 'locks', attributes: ['lock'], type: 'state'],
        [ devices: 'motions', attributes: ['motion'], type: 'state'],
        [ devices: 'musicPlayers', attributes: ['status','level','trackDescription','trackData','mute'], type: 'state'],
        [ devices: 'peds', attributes: ['steps','goal'], type: 'value'],
        [ devices: 'phMeters', attributes: ['pH'], type: 'value'],
        [ devices: 'powerMeters', attributes: ['power','voltage','current','powerFactor'], type: 'value'],
        [ devices: 'presences', attributes: ['presence'], type: 'state'],
        [ devices: 'shockSensors', attributes: ['shock'], type: 'state'],
        [ devices: 'signalStrengthMeters', attributes: ['lqi','rssi'], type: 'value'],
        [ devices: 'sleepSensors', attributes: ['sleeping'], type: 'state'],
        [ devices: 'smokeDetectors', attributes: ['smoke'], type: 'state'],
        [ devices: 'soundSensors', attributes: ['sound'], type: 'state'],
        [ devices: 'spls', attributes: ['soundPressureLevel'], type: 'value'],
        [ devices: 'switches', attributes: ['switch'], type: 'state'],
        [ devices: 'switchLevels', attributes: ['level'], type: 'value'],
        [ devices: 'tamperAlerts', attributes: ['tamper'], type: 'state'],
        [ devices: 'temperatures', attributes: ['temperature'], type: 'value'],
        [ devices: 'thermostats', attributes: ['temperature','heatingSetpoint','coolingSetpoint','thermostatSetpoint','thermostatMode','thermostatFanMode','thermostatOperatingState','thermostatSetpointMode','scheduledSetpoint','optimisation','windowFunction'], type: 'state'],
        [ devices: 'threeAxis', attributes: ['threeAxis'], type: 'threeAxis'],
        [ devices: 'touchs', attributes: ['touch'], type: 'state'],
        [ devices: 'uvs', attributes: ['ultravioletIndex'], type: 'value'],
        [ devices: 'valves', attributes: ['contact'], type: 'state'],
        [ devices: 'volts', attributes: ['voltage'], type: 'value'],
        [ devices: 'waterSensors', attributes: ['water'], type: 'state'],
        [ devices: 'windowShades', attributes: ['windowShade'], type: 'state']
    ]

    state.groupNames = [:] // Initialise map of Group Ids and Group Names
    state.deviceGroup = [:] // Initialise map of Device Ids and Group Names

    // Create a map of Attribute State Values
    state.attributeStateValues = [
        contact: [closed: -1, open: 1, full: -1, flushing: 1],
        door: [closed: -1, open: 1, opening: 2, closing: -2, unknown: 5],
        motion: [inactive: -1, active: 1],
        switch: [off: -1, on: 1],
        acceleration: [inactive: -1, active: 1],
        bulb: [off: -1, on: 1],
        lock: [locked: -1, unlocked: 1, 'unlocked with timeout': 2, unknown: 5],
        presence: ['not present': -1, present: 1],
        water: [dry: -1, wet: 1],
        button: [pushed: 1, held: 2],
        sound: ['not detected': -1, detected: 1],
        touch: [touched: 1],
        valve: [closed: -1, open: 1],
        alarm: [off: -1, siren: 1, strobe: 2, both: 3],
        carbonMonoxide: [clear: -1, detected: 1, tested: 4],
        shock: [clear: -1, detected: 1],
        sleeping: [sleeping: -1, 'not sleeping': 1],
        smoke: [clear: -1, detected: 1, tested: 4],
        consumableStatus: [replace: -1, good: 1, order: 3, 'maintenance required': 4, missing: 5],
        mute: [muted: -1, unmuted: 1],
        optimisation: [inactive: -1, active: 1],
        powerSource: [mains: -1, battery: 1, dc: 2,unknown: 5],
        tamper: [clear: -1, detected: 1],
        thermostatFanMode: [auto: -1, on: 1, circulate: 2],
        thermostatMode: [off: -1, heat: 1, 'emergency heat': 2, auto: 3, cool: -3],
        thermostatOperatingState: [idle: -1, heating: 1, 'pending heat': 2, 'fan only': 3, cooling: -1, 'pending cool': -2],
        windowShade: [closed: -1, opening: 1, 'partially open': 2, open: 3, closing: -2, unknown: 5],
        hubStatus: [disconnected: -1, active: 1]
    ]

    // Create an Array of Value rounding to decimal places
    state.attributeValueRounding = [
        battery: [decimalPlaces: 0, unit: '%'],
        current: [decimalPlaces: 1, unit: ''],
        energy: [decimalPlaces: 2, unit: ''],
        goal: [decimalPlaces: 0, unit: ''],
        humidity: [decimalPlaces: 0, unit: ''],
        illuminance: [decimalPlaces: 0, unit: ''],
        level: [decimalPlaces: 0, unit: ''],
        pH: [decimalPlaces: 1, unit: ''],
        power: [decimalPlaces: 0, unit: 'W'],
        powerFactor: [decimalPlaces: 2, unit: ''],
        lqi: [decimalPlaces: 2, unit: ''],
        rssi: [decimalPlaces: 2, unit: ''],
        soundPressureLevel: [decimalPlaces: 2, unit: ''],
        steps: [decimalPlaces: 0, unit: ''],
        temperature: [decimalPlaces: 1, unit: ''],
        threeAxis: [decimalPlaces: 2, unit: ''],
        ultravioletIndex: [decimalPlaces: 0, unit: ''],
        voltage: [decimalPlaces: 0, unit: '']
    ]

    // Define state variable to hold location and hub details
    state.hubLocationRef = ""
    state.hubLocationDetails = ""
    state.hubLocationText = ""
    hubLocationDetails()


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

/* def handleModeEvent(evt) { //Log Mode changes
    logger("handleModeEvent(): Mode changed to: ${evt.value}","info")

    def locationId = escapeStringForInfluxDB(location.id) // need to remove
    def locationName = escapeStringForInfluxDB(location.name)
    def mode = '"' + escapeStringForInfluxDB(evt.value) + '"'
    def data = "_stMode,locationId=${locationId},locationName=${locationName} mode=${mode}"
    postToInfluxDB(data)
} */


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

def handlePrefsReportEvent(evt) {
    def eventType = 'prefsReport'
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

    if (eventType == 'state' || eventType == 'value' || eventType == 'threeAxis' || eventType == 'prefsReport') {

        // deviceName = state.deviceGroup.(evt.deviceId).deviceName // lookup device name - need to change code
        deviceName = (evt?.device.device.name) ? evt.device.device.name : 'unassigned'

        // deviceGroup = state.deviceGroup.(evt.deviceId).deviceGroup // lookup device group name - need to change code
        deviceGroup = (evt?.device.device.groupId) ? state.groupNames.(evt.device.device.groupId) : 'unassigned'

        prevEvents = evt.device.statesSince("${evt.name}", writeTime - 7, [max: 3]) // get previous event
//        prevEvent = prevEvents.find { it -> it.date.time < eventTime}

//        prevEvents = evt.device.statesBetween("${evt.name}", evt.date - 7, evt.date, [max: 2]) // get previous event
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

    if (eventType != 'prefsReport') fieldsSB.append('eventDescription="').append(description).append('",')

    fieldsSB.append('eventId="').append(evt.id).append('"')


    // for state events
    if (eventType == 'state') {
        measurement = 'states'

        def states = state.attributeStateValues.find { it.key == evt.name }.value // Lookup array for event status values

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

        unit = (evt?.unit) ? evt.unit : state?.attributeValueRounding."${evt.name}".unit // set here, but included in tag set

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

        // calculate change from previous value
        change =  nowValue - prevValue
        rounding = state?.attributeValueRounding."${evt.name}".decimalPlaces
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


    // for prefsReport events
    else if (eventType == 'prefsReport') {
        rp = 'metadata'
        measurement = 'preferences'
        fieldsSB.append(',').append(evt.value)
    }


    // Create InfluxDB line protocol
    def dataSB = new StringBuilder()

    dataSB.append(state.hubLocationDetails) // Add hub tags

    if (eventType == 'state' || eventType == 'value' || eventType == 'threeAxis' || eventType == 'prefsReport') {

        dataSB.append(',chamber=').append(deviceGroup.replaceAll(' ', '\\\\ ')).append(',chamberId=').append(evt?.device.device.groupId)

        dataSB.append(',deviceCode=').append(deviceName.replaceAll(' ', '\\\\ ')).append(',deviceId=').append(evt.deviceId).append(',deviceLabel=').append(evt.displayName.replaceAll(' ', '\\\\ '))

        dataSB.append(',event=').append(evt.name)

        dataSB.append(',eventType=').append(eventType) // Add type (state|value|threeAxis) of measurement tag

        dataSB.append(',identifier=').append(deviceGroup.replaceAll(' ', '\\\\ ')).append('\\ .\\ ').append(evt.displayName.replaceAll(' ', '\\\\ ')) // Create composite identifier "chamber . deviceLabel"

        if (eventType != 'prefsReport') dataSB.append(',isChange=').append(evt?.isStateChange)
    }

    else if (eventType == 'hubStatus' || eventType == 'daylight') {

        dataSB.append(',chamber=').append('House')

        dataSB.append(',deviceLabel=')

        dataSB.append( eventType == 'hubStatus' ? 'hub' : 'day' )

        dataSB.append(',event=')

        dataSB.append( eventType == 'hubStatus' ? evt.name : 'daylight' )

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


def softPoll() {
    logger("softPoll()","trace")

    def dataSB = new StringBuilder()

    def devs // temp variable to hold device collection
    def type
    def now = new Date()

    state.deviceAttributes.each { da ->
        devs = settings."${da.devices}"
        type = da.type
        if (devs && (da.attributes)) {
            devs.each { d ->

//                def dataSB = new StringBuilder()

                da.attributes.each { attr ->

                    if (d.hasAttribute(attr) && d.latestState(attr)?.value != null) {
                        logger("softPoll(): Softpolling device ${d} for attribute: ${attr}","info")

                        dataSB.append('attributes')

                        dataSB.append(state.hubLocationDetails) // Add hub tags

                        dataSB.append(',chamber=').append(state?.groupNames.(d.device.groupId).replaceAll(' ', '\\\\ '))

                        dataSB.append(',chamberId=').append(d?.device.groupId)

                        dataSB.append(',deviceCode=').append(d.name.replaceAll(' ', '\\\\ '))

                        dataSB.append(',deviceId=').append(d.id)

                        dataSB.append(',deviceLabel=').append(d.label.replaceAll(' ', '\\\\ '))

                        dataSB.append(',event=').append(attr)

                        dataSB.append(',eventType=').append(type)

                        dataSB.append(',identifier=').append(state?.groupNames.(d.device.groupId).replaceAll(' ', '\\\\ ')).append('\\ .\\ ').append(d.label.replaceAll(' ', '\\\\ ')) // Create unique composite identifier

                        def daysElapsed = ((now.time - d.latestState(attr).date.time) / 86_400_000) / 30
                        daysElapsed = daysElapsed.toDouble().trunc().round()

                        dataSB.append(',timeElapsed=').append(daysElapsed * 30).append('-').append((daysElapsed + 1) * 30).append('days')

                        dataSB.append(' ').append('timeLastEvent=').append(d.latestState(attr).date.time).append('i')

                        dataSB.append(',valueLastEvent="').append(d.latestState(attr).value).append('"')

                        dataSB.append('\n')
                    }
                }
//                postToInfluxDB(dataSB.toString(), 'metadata')
            }
        }
    }
    def rp = 'metadata'
    postToInfluxDB(dataSB.toString(), rp) // will it still work here?
}


def zwaveReport() {

    def devs
    def devsList = []
    def dataSB = new StringBuilder()
    def info

    state.deviceAttributes.each { da ->
        devs = settings."${da.devices}"

        if (devs && (da.attributes)) {
            devs.each { d ->
                devsList.push(d)
            }
        }
    }

    devsList = devsList.unique { d -> d.id }

    devsList.each { d ->

        info = d?.getZwaveInfo().clone()

            if (info.containsKey("zw")) {

                logger("zwaveReport(): zWave report for device ${d}","info")

                dataSB.append('zwave')

                dataSB.append(state.hubLocationDetails) // Add hub tags

                dataSB.append(',chamber=').append(state?.groupNames.(d.device.groupId).replaceAll(' ', '\\\\ '))

                dataSB.append(',chamberId=').append(d?.device.groupId)

                dataSB.append(',deviceCode=').append(d.name.replaceAll(' ', '\\\\ '))

                dataSB.append(',deviceId=').append(d.id)

                dataSB.append(',deviceLabel=').append(d.label.replaceAll(' ', '\\\\ '))

                dataSB.append(',identifier=').append(state?.groupNames.(d.device.groupId).replaceAll(' ', '\\\\ ')).append('\\ .\\ ').append(d.label.replaceAll(' ', '\\\\ ')) // Create unique composite identifier

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

                if (d?.device.getDataValue("configuredParameters")) dataSB.append(d.device.getDataValue("configuredParameters")).append(',')

                dataSB.append(info)

                dataSB.append('\n')
            }
        }
    def rp = 'metadata'
    postToInfluxDB(dataSB.toString(), rp)
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


def postToInfluxDB(data, rp) { //Posts data to InfluxDB

//    def query = state.query.clone() as TreeMap

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
        /*
        if (rp != 'autogen') {
            query.rp = "${rp}"
            query.remove('precision')
        }
        */
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
        logger("postToInfluxDB(): Posting data to InfluxDB: Uri: ${state.uri}, Path: ${state.path}, Query: ${query}, Data: ${data}","debug")
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

    try {
        unschedule(hubLocationDetails)
    }
    catch(e) {
        // logger("manageSchedules(): Unschedule failed!","error")
    }
    // schedule update of state.hubLocationDetails
    runEvery3Hours(hubLocationDetails)
    // schedule("2018-01-01T03:10:00.000-0000", hubLocationDetails)

    try {
        unschedule(softPoll)
    }
    catch(e) {
        // logger("manageSchedules(): Unschedule failed!","error")
    }
    // schedule softPoll
    runEvery3Hours(softPoll)
    // schedule("2018-01-01T03:40:00.000-0000", softPoll)

    try {
        unschedule(zwaveReport)
    }
    catch(e) {
        // logger("manageSchedules(): Unschedule failed!","error")
    }
    // schedule softPoll
    runEvery3Hours(zwaveReport)
    // schedule("2018-01-01T03:40:00.000-0000", softPoll)
}


private manageSubscriptions() { // Configures subscriptions
    logger("manageSubscriptions()","trace")

    unsubscribe()

    // if (prefLogModeEvents) subscribe(location, "mode", handleModeEvent)

    // Subscribe to device attributes (iterate over each attribute for each device collection in state.deviceAttributes):
    def devs // dynamic variable holding device collection
    def type // dynamic variable holding type of attribute value
    state.deviceAttributes.each { da ->
        devs = settings."${da.devices}"
        type = da.type
        if (devs && (da.attributes)) {
            da.attributes.each { attr ->
                if (type == 'state') {
                    logger("manageSubscriptions(): Subscribing 'handleStateEvent' to attribute: ${attr}, for devices: ${da.devices}","info")
                    subscribe(devs, attr, handleStateEvent)
                }
                else if (type == 'value') {
                    logger("manageSubscriptions(): Subscribing 'handleValueEvent' to attribute: ${attr}, for devices: ${da.devices}","info")
                    subscribe(devs, attr, handleValueEvent)
                }
                else if (type == 'threeAxis') {
                    logger("manageSubscriptions(): Subscribing 'handleThreeAxisEvent' to attribute: ${attr}, for devices: ${da.devices}","info")
                    subscribe(devs, attr, handleThreeAxisEvent)
                }
                subscribe(devs, 'prefsReport', handlePrefsReportEvent)
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

    // build map of group Ids and group names
    if (state.roomNameCapture) {
        def groupId
        def groupName
        settings.bridges.each {
            groupId = it?.device.groupId
            groupName = it?.name.drop(1)
            if (groupId) state.groupNames << [(groupId): groupName]
        }
        def devices
        def deviceId
        def deviceName
        def deviceGroupId
        def deviceGroup
        state.deviceAttributes.each {
            devices = settings."${it.devices}"
            devices.each {
                deviceId = it.id
                deviceName = it.name
                deviceGroupId = it?.device.groupId
                deviceGroup = state.groupNames?."${deviceGroupId}"
                if (deviceGroupId) state.deviceGroup << [(deviceId): [deviceName: deviceName, deviceGroup: deviceGroup, deviceGroupId: deviceGroupId]]
                else state.deviceGroup << [(deviceId): [deviceName: deviceName, deviceGroup: 'unassigned', deviceGroupId: 'unassigned']]
            }
        }
    }

    else {
        def devices
        def deviceId
        def deviceName
        def code
        def groupName
        def rooms = [BD: 'Bedroom', BT: 'Bathroom', DN: 'Dining_room', KT: 'Kitchen', LV: 'Living_room', UT: 'Utility_room']
        state.deviceAttributes.each {
            devices = settings."${it.devices}"
            devices.each {
                deviceId = it.id
                deviceName = it.name
                code = deviceName.substring(4, 6)
                groupName = rooms.find { it.key == code}?.value
                state.deviceGroup << [(deviceId): [deviceName: deviceName, deviceGroup: groupName]]
            }
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


private escapeStringForInfluxDB(str) {
    if (str) {
        str = str.replaceAll(" ", "\\\\ ") // Escape spaces.
        str = str.replaceAll(",", "\\\\,") // Escape commas.
        str = str.replaceAll("=", "\\\\=") // Escape equal signs.
        str = str.replaceAll("\"", "\\\\\"") // Escape double quotes.
        //str = str.replaceAll("'", "_")  // Replace apostrophes with underscores.
    }
    else {
        str = 'null'
    }
    return str
}