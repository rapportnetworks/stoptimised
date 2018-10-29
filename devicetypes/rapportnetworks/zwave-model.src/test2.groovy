def settings = [:]
settings << [configParam2a: "true"]
settings << [configParam2b: "true"]
settings << [configParam2c: "true"]
settings << [configParam2d: "true"]
settings << [configParam3: "true"]
// settings << [configParam2: "true"]

private parametersMetadata() { [
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
//        trueValue: 7
         ]
] }

parametersMetadata().findAll( {!it.readonly} ).each {
    if (settings?."configParam${it.id}" || settings?.find { s -> s.key ==~ /configParam${it.id}[a-z]/ }) {
        println "settings.configParam${it.id}"
        switch(it.type) {
            case "number":
                state."param${it.id}target" = settings."configParam${it.id}"
                break
            case "enum":
                state."param${it.id}target" = settings."configParam${it.id}".toInteger()
                break
            case "bool":
                println "bool"
                println it.id
                def value = (settings."configParam${it.id}".toBoolean()) ? (it.trueValue ?: 1) : (it.falseValue ?: 0)
                println value
                break
            case "flags":
                println "flags"
                println it.id
                def target = 0
                settings.findAll { st -> st.key ==~ /configParam${it.id}[a-z]/ }.each{ k, v ->
                   if (v.toBoolean()) target += it.flags.find{ flag -> flag.id == "${k.reverse().take(1)}" }.flagValue
                      println "${k.reverse().take(1)}"
                    println target
                  }
                break
        }
    }
}
println settings