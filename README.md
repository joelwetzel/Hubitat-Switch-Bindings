# Switch Bindings app for Hubitat
An app for Hubitat that binds two (or more) switches/dimmers together.  When bound, if either one turns on or off, the binding will make the other one also turn on/off.  It will also sync dimmer levels, if any of the devices are dimmers. (It works a lot like a z-wave association, but it happens in the Hubitat hub, so that the hub can know/display the updated device states.  Because the Hubitat is local, the binding can happen effectively as fast as z-wave messages can travel.)

## Examples of Usage
- Suppose you have a smart wall switch in your living room that controls the overhead can lights.  Now you add a lamp in the room, and you either put a smart bulb in it, or plug it into a smart outlet.  You can use this app to bind the wall switch and the lamp together, so that all your lights will always turn on and off together.  They will stay synced whether you control them using the wall switch, an app, or Alexa or Google Home.
- 3-way and 4-way lighting controlled by software, not wiring in the walls
- Sync a fan and a light switch
- Sync levels across a roomful of dimmers.
- With a 5 button scene controller keypad.  My driver exposes the buttons/lights as virtual switches.  By using Switch Bindings alongside this, it becomes easy to keep lights synced with the scene controller.  See my keypad driver here: [https://github.com/joelwetzel/Hubitat-Cooper-Aspire-Scene-Controller](https://github.com/joelwetzel/Hubitat-Cooper-Aspire-Scene-Controller)

## Installation

The best way to install this code is by using [Hubitat Package Manager](https://community.hubitat.com/t/beta-hubitat-package-manager).

However, if you must install manually:

1. On the Hubitat hub, go to the "Apps Code" page
2. Click "+ New App"
3. Copy in the contents of SwitchBindings.groovy
4. Click "Save"
5. Click "+ New App" again
6. Copy in the contents of SwitchBindingInstance.groovy
7. Click "Save"
8. Go to the "Apps" page
9. Click "Add User App"
10. Click on Switch Bindings
11. Click "Done"
12. Click on Switch Bindings in your apps list
13. Click on "Add a new binding"
14. Choose your switches
15. Click "Done"
