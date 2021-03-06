package state

import app.TesseractMain
import clip.AbstractClip
import clip.ClipMetadata
import stores.PlaylistStore
import stores.SceneStore
import websocket.WebsocketInterface

// State manager is responsible for managing application state and synchronizing state
// between client(s) and server
class StateManager {

  WebsocketInterface ws = WebsocketInterface.get()

  public StateManager() {
    this.registerHandlers()
  }

  public registerHandlers() {
    ws.registerActionHandler('requestInitialState', this.&sendInitialState)
    ws.registerActionHandler('stateUpdate', this.&handleStateUpdate)
  }

  // Sends the state of the relevant objects to the front end for initial hydration
  public sendInitialState(conn, inData) {
    // Send objects in this order:
    // clips (clips I'll leave hardcoded for now)
    // scenes
    // playlists

    println "Sending initial state to Client".cyan()

    def data = [
        clipData: ClipMetadata.getClipMetadata(),
        sceneData: SceneStore.get().asJsonObj(),
        playlistData: PlaylistStore.get().asJsonObj(),
        activeScene: TesseractMain.getMain().channel1.getScene().id, // Send the active scene ID on initial load
    ]

    ws.sendMessage(conn, 'sendInitialState', data);
  }

  // A 'stateUpdate' event means something changed in the
  // state on the backend and we need to update the frontend to reflect the change
  // Send this to all clients for now
  // In the future, we will want something like 'send to all clients except one'
  public void sendStateUpdate(String stateKey, value) {
    println "Sending stateUpdate event: ${stateKey} ${value}".cyan()

    def data = [
        key: stateKey,
        value: value,
    ]

    ws.broadcastMessage('stateUpdate', data)
  }

  // Handle receiving a stateUpdate event from a client
  public void handleStateUpdate(conn, inData) {
    println "Got a state update event!!!"
    println inData

    if (inData.stateKey == "activeControls") {
      this.handleActiveControlsUpdate(inData);
    } else {
      // The reason I use RuntimeException is because they can't be caught (by Processing), so you are always guaranteed to see the stack trace
      throw new RuntimeException("Error: No handler for state key '${inData.stateKey}'")
    }

    // todo: here is where I would determine if the stateUpdate should be broadcast to other clients and send the data
  }

  // Handle an update to one of the active controls
  public void handleActiveControlsUpdate(Map inData) {
    // find the active clip.  this is gonna be kinda hacky for now
    AbstractClip clip = TesseractMain.getMain().channel1.getScene().clip

    String fieldName = inData.value.fieldName
    float newValue = inData.value.newValue

    // Set the field in 'fieldName' to the value in 'newValue'
    // e.g., this will set 'p1' to '0.589378' or whatever the Control on the frontend is set to do
    // Groovy is cool because we can do stuff like this: obj."${variableHoldingFieldName}" to dynamically set a property on an object
    clip."${fieldName}" = newValue

    println "Set clip field '${fieldName}' to value '${newValue}'"
  }
}
