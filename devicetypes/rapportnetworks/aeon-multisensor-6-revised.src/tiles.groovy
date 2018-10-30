
    single-attribute tiles
    multi-attribute tiles

    width: 2, height:2 // 2 x 2 squares on grid (6 wide x unlimited height)

    standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) // main tile - allows user to change device icon

    // standard tiles (single attribute)
    standardTile("tileName", "device.switch", width: 2, height: 2) {...}

    // defaultState: true - uses this to render if not state available - including valueTiles
    // icon: *** - can be rendered for specific state

    // decoration: "flat" - can be used to remove ring appearance

    // background color can change with state/value
    valueTile("temperature", "device.temperature", width: 2, height: 2) {
        state("temperature", label:'${currentValue}', unit:"dF",
            backgroundColors:[
                [value: 31, color: "#153591"],
                [value: 44, color: "#1e9cbb"],
                [value: 59, color: "#90d2a7"],
                [value: 74, color: "#44b621"],
                [value: 84, color: "#f1d801"],
                [value: 95, color: "#d04e00"],
                [value: 96, color: "#bc2323"]
            ]
        )
    }

    multiAttributeTile // must be given width: 6, height: 4
    // type: "lighting" | "thermostat" | "mediaPlayer" | "generic"
    // key:

    COLOR_CONTROL	Displays a color palette for the user to select a color from.
    COOLING_SETPOINT	Used by the Thermostat Multi-Attribute Tile.
    HEATING_SETPOINT	Used by the Thermostat Multi-Attribute Tile.
    MARQUEE	Displays a rotating marquee message beneath the PRIMARY_CONTROL.
    MEDIA_MUTED	Allows the user to press the volume icon to mute on a Multimedia Multi-Attribute Tile.
    MEDIA_STATUS	Used to display and control the current play status (playing, paused, stopped) on a Multimedia Multi-Attribute Tile.
    NEXT_TRACK	Renders a control for going to the next track on a Multimedia Multi-Attribute Tile.
    OPERATING_STATE	Used by the Thermostat Multi-Attribute Tile.
    PREVIOUS_TRACK	Renders a control for going to the previous track on a Multimedia Multi-Attribute Tile.
    PRIMARY_CONTROL	All tiles must define a PRIMARY_CONTROL. Controls the background color of tile (except for the Thermostat Multi-Attribute Tile), and specifies the attribute to show on the Device list views.
    SECONDARY_CONTROL	Used to display textual information below the PRIMARY_CONTROL.
    SLIDER_CONTROL	Displays a slider input; typically useful for attributes like bulb level or volume.
    THERMOSTAT_MODE	Used by the Thermostat Multi-Attribute Tile.
    VALUE_CONTROL	Renders Up and Down controls for increasing and decreasing an attribute’s value by 1.

    Generic multiAttributeTile controls available
    PRIMARY_CONTROL	The primary control tile for this device, controls the background color
    SECONDARY_CONTROL	Displays textual data below the primary control
    VALUE_CONTROL	Renders Up and Down buttons for increasing or decreasing values
    SLIDER_CONTROL	Renders a slider control for selecting a value along a range
    COLOR_CONTROL	Renders the color picker that allows users to select a color (useful for Color Control devices)


    Color Standards
    Color	Hex code   Description
    Blue	#00a0dc	   Represents “on”-like device states
    White	#ffffff	   Represents “off”-like device states
    Orange	#e86d13	   Represents device states that require the user’s attention
    Gray	#cccccc	   Represents “inactive” or “offline” device states


    temperature
    Temperature value (Fahrenheit)	Hex code
    31	 #153591
    44	 #1e9cbb
    59	 #90d2a7
    74	 #44b621
    84	 #f1d801
    95	 #d04e00
    96	 #bc2323


    Attribute state	Color
    Switch on	Blue–#00a0dc
    Switch off	White–#ffffff
    Motion active	Blue–#00a0dc
    Motion inactive	White–#ffffff
    Contact sensor open	Orange–##e86d13
    Contact sensor closed	Blue–#00a0dc
    Lock locked	Blue–#00a0dc
    Lock unlocked	White–#ffffff
    Presence present	Blue–#00a0dc
    Presence away	Gray–#cccccc
    Thermostat cool	Blue–#00a0dc
    Thermostat heat	Orange–##e86d13
    Siren on	Orange–##e86d13
    Siren off	White–#ffffff
    Water sensor dry	White–#ffffff
    Water sensor wet	Blue–#00a0dc
    Smoke detector clear	White–#ffffff
    Smoke detector detected	Orange–#e86d13
    Smoke detector tested	Orange–#e86d13



    tiles(scale: 2) { // 6 wide x Unlimited height grid
        // standard tile with actions named
        // use '${name}' for discrete states
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "off", label: '${name}', action: "switch.on",
                  icon: "st.switches.switch.off", backgroundColor: "#ffffff"
            state "on", label: '${name}', action: "switch.off",
                  icon: "st.switches.switch.on", backgroundColor: "#00a0dc"
        }

        // value tile (read only) - single "state"
        valueTile("power", "device.power", decoration: "flat", width: 2, height: 2) {
            state "power", label:'${currentValue} Watts'
        }

        // the "switch" tile will appear in the Things view
        main("switch")

        // the "switch" and "power" tiles will appear in the Device Details - view (order is left-to-right, top-to-bottom)
        details(["switch", "power"])
    }




