private generatePrefsParams() {
    def prefs = []
    parametersMetadata().findAll{ !it.readonly }.each{
        if (!configurationUser() || it.id in configurationUser()) prefs << generatePref(it)
    }
    prefs
}

private generatePref(it) {
    def pref = []
    def lb = (it.description.length() > 0) ? "\n" : ""
    switch(it.type) {
        case "number":
            pref << [
                name: "configParam${it.id}",
                title: "#${it.id}: ${it.name}: \n" + it.description + lb +"Default Value: ${it.defaultValue}",
                type: it.type,
                range: it.range,
                defaultValue: it.defaultValue,
                required: it.required
            ]
            break
        case "enum":
            pref << [
                name: "configParam${it.id}",
                title: "#${it.id}: ${it.name}: \n" + it.description + lb + "Default Value: ${it.defaultValue}",
                type: it.type,
                options: it.options,
                defaultValue: it.defaultValue,
                required: it.required
            ]
            break
        case "bool":
            pref << [
                name: "configParam${it.id}",
                title: "#${it.id}: ${it.name}: \n" + it.description + lb + "Default Value: ${it.defaultValue}",
                type: it.type,
                defaultValue: it.defaultValue,
                required: it.required
            ]
            break
        case "flags":
            pref << [
                title: "${it.id}: ${it.name}: \n" + it.description,
                // description: "",
                type: "paragraph", element: "paragraph"
            ]
            it.flags.each { flag ->
                pref << [
                    name: "configParam${it.id}${flag.id}", // ? how best to reference? 1a or 1-a or 1-32 etc
                    title: "${flag.description}",
                    type: 'bool',
                    defaultValue: flag.default,
                    required: it.required
                ]
            }
            break
    }
    pref
}

private parametersMetadata() { [
    [id: 1, size: 1, type: "bool", defaultValue: false, required: false, readonly: false,
        isSigned: false,
        name: "Test Parameter",
        description : "Test Parameter.",
        falseValue: 0,
        trueValue: 3
         ],
    [id: 2, size: 1, type: "flags", defaultValue: 15, required: false, readonly: false,
        isSigned: false,
        name: "Acoustic and Visual Alarms",
        description : "Disable/enable LED indicator and acoustic alarm for flooding detection.",
        flags: [
            [id: 'a', description: 'enable temperature', defaultValue: true, flagValue: 1],
            [id: 'b', description: 'enable illuminance', defaultValue: true, flagValue: 2],
            [id: 'c', description: 'enable ultraviolet', defaultValue: false, flagValue: 4],
            [id: 'd', description: 'enable humidity', defaultValue: false, flagValue: 8]
            ] ],
    [id: 3, size: 1, type: "bool", defaultValue: false, required: false, readonly: false,
        isSigned: false,
        name: "Acoustic and Visual Alarms",
        description : "Enable LED indicator and acoustic alarm for flooding detection.",
        falseValue: 0,
        trueValue: 7
         ]
] }

private configurationUser() { [
    3
] }

prefs = generatePrefsParams()
println prefs