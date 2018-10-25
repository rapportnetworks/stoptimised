getParametersMetadata().findAll( { !it.readonly } ).each {
    def specifiedValue = configurationParameterValues().find { spec -> spec.id == it.id }?.specifiedValue
    if (specifiedValue) {
        println specifiedValue
    }
}


private configurationParameterValues() { [
    [id: 1, specifiedValue: 100],
    [id: 2, specifiedValue: "2"]
] }

private getParametersMetadata() { [
        [id:  1, size: 2, type: "number", range: "0..3600", defaultValue: 0, required: false, readonly: false,
         isSigned: true,
         name: "Alarm Cancellation Delay",
         description: "The time for which the device will retain the flood state after flooding has ceased.\n" +
         "Values: 0-3600 = Time Delay (s)"],
        [id: 2, size: 1, type: "enum", defaultValue: "3", required: false, readonly: false,
         isSigned: true,
         name: "Acoustic and Visual Alarms",
         description : "Disable/enable LED indicator and acoustic alarm for flooding detection.",
         options: ["0" : "0: Acoustic alarm INACTIVE. Visual alarm INACVTIVE",
                   "1" : "1: Acoustic alarm INACTIVE. Visual alarm ACTIVE",
                   "2" : "2: Acoustic alarm ACTIVE. Visual alarm INACTIVE",
                   "3" : "3: Acoustic alarm ACTIVE. Visual alarm ACTIVE"] ],
        [id: 5, size: 1, type: "enum", defaultValue: "255", required: false, readonly: false,
         isSigned: false,
         name: "Type of Alarm sent to Association Group 1",
         description : "",
         options: ["0" : "0: ALARM WATER command",
                   "255" : "255: BASIC_SET command"] ]
] }
