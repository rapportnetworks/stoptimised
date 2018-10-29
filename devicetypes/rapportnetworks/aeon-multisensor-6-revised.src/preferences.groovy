    input (
        type: "paragraph",
        element: "paragraph",
        title: "DEVICE PARAMETERS:",
        description: "Device parameters are used to customise the physical device. " +
                 "Refer to the product documentation for a full description of each parameter."
    )

    preferences {
        section("paragraph") {
            paragraph "This is how you can make a paragraph element"
            paragraph image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
                      title: "paragraph title",
                      required: true,
                      "This is a long description that rambles on and on and on..."
        }
    }



metadata {
    definition(...) {...}
    tiles() {...}
    preferences {
        input "tempOffset", "number", title: "Degrees", description: "Adjust temperature by this many degrees", range: "*..*", displayDuringSetup: false
    }
}

def someCommandMethod() {
    if (tempOffset) {
        // handle offset value
    }
}

metadata {
    simulator {
        // TODO: define status and reply messages here
    }

    tiles {
        // TODO: define your main and details tiles here
    }

    preferences {
        input name: "email", type: "email", title: "Email", description: "Enter Email Address", required: true, displayDuringSetup: true
        input name: "text", type: "text", title: "Text", description: "Enter Text", required: true
        input name: "number", type: "number", title: "Number", description: "Enter number", required: true
        input name: "bool", type: "bool", title: "Bool", description: "Enter boolean", required: true
        input name: "password", type: "password", title: "password", description: "Enter password", required: true
        input name: "phone", type: "phone", title: "phone", description: "Enter phone", required: true
        input name: "decimal", type: "decimal", title: "decimal", description: "Enter decimal", required: true
        input name: "time", type: "time", title: "time", description: "Enter time", required: true
        input name: "options", type: "enum", title: "enum", options: ["Option 1", "Option 2"], description: "Enter enum", required: true
    }
}

def someCommand() {
    log.debug "email: $email"
    log.debug "text: $text"
    log.debug "bool: $bool"
    log.debug "password: $password"
    log.debug "phone: $phone"
    log.debug "decimal: $decimal"
    log.debug "time: $time"
    log.debug "options: $options"
}

// bool	A true or false value (value returned as a boolean).
// boolean	A "true" or "false" value (value returned as a string). It’s recommended that you use the “bool” input instead, since the simulator and mobile support for this type may not be consistent, and using “bool” will return you a boolean (instead of a string). The “boolean” input type may be removed in the near future.


// Setting a default value (defaultValue: "foobar") for an input may render that selection in the mobile app, but the user still needs to enter data in that field. It’s recommended to not use defaultValue to avoid confusion.

// capitalization (Note - this feature is currently only supported on iOS devices) String - if the input is a text field, this controls the behavior of the auto-capitalization on the mobile device. "none" specifies to not enable auto-capitalization for any word. "sentences" will capitlize the first letter of each sentence. "all" will use all caps. "words" will capitalize every word. The default is "words".
