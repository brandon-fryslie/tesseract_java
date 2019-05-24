package state

import app.TesseractMain
import clip.AbstractClip
import clip.ClipMetadata
import org.java_websocket.WebSocketImpl
import show.Playlist
import stores.PlaylistStore
import stores.SceneStore
import show.PlaylistManager
import websocket.WebsocketInterface

// State manager is responsible for managing application state and synchronizing state
// between client(s) and server
class StateManager {
  public static StateManager instance;

  WebsocketInterface ws = WebsocketInterface.get()

  public StateManager() {
    this.registerHandlers()
  }

  // Singleton
  public static StateManager get() {
    if (instance == null) {
      instance = new StateManager()
    }

    instance
  }

  public registerHandlers() {
    ws.registerActionHandler('requestInitialState', this.&sendInitialState)
    ws.registerActionHandler('stateUpdate', this.&handleStateUpdate)
  }

  // it would be get to get the current values of the controls here too
  // This gets the 'active state' of the application
  // Things like which playlist / scene are we playing, stuff like that
  public Map getActiveState() {
    Map activeState = [
        playlistItemId               : PlaylistManager.get().getCurrentPlaylist().getCurrentItem().getId(),
        playlistId                   : PlaylistManager.get().getCurrentPlaylist().getId(),
        currentSceneDurationRemaining: PlaylistManager.get().getCurrentSceneDurationRemaining(),
        playlistPlayState            : PlaylistManager.get().getCurrentPlaylist().getCurrentPlayState().name(),
    ]

    return activeState
  }

  public void sendActiveState() {
    this.sendStateUpdate("activeState", this.getActiveState());
  }

  // Sends the state of the relevant objects to the front end for initial hydration
  public void sendInitialState(WebSocketImpl conn, Map inData) {
    // Send objects in this order:
    // clips (clips I'll leave hardcoded for now)
    // scenes
    // playlists

    println "[StateManager] Sending initial state to Client".cyan()

    Map data = [
        clipData    : ClipMetadata.getClipMetadata(),
        sceneData   : SceneStore.get().asJsonObj(),
        playlistData: PlaylistStore.get().asJsonObj(),
        activeState : this.getActiveState(),
    ]

    ws.sendMessage(conn, 'sendInitialState', data);
  }

  // A 'stateUpdate' event means something changed in the
  // state on the backend and we need to update the frontend to reflect the change
  // Send this to all clients for now
  // In the future, we will want something like 'send to all clients except one'
  public void sendStateUpdate(String stateKey, value) {
    println "[StateManager] Sending stateUpdate event: ${stateKey} ${value}".cyan()

    def data = [
        key  : stateKey,
        value: value,
    ]

    ws.broadcastMessage('stateUpdate', data)
  }

  // Handle receiving a stateUpdate event from a client
  public void handleStateUpdate(conn, inData) {
//    println "Got a state update event!!!"
//    println inData

    if (inData.stateKey == "activeControls") {
      this.handleActiveControlsUpdate(inData.value);
    } else if (inData.stateKey == "playlist") {
      this.handlePlaylistUpdate(inData.value);
    } else if (inData.stateKey == "playState") {
      this.handlePlayStateUpdate(inData.value);
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

    String fieldName = inData.fieldName
    float newValue = inData.newValue

    // Set the field in 'fieldName' to the value in 'newValue'
    // e.g., this will set 'p1' to '0.589378' or whatever the Control on the frontend is set to do
    // Groovy is cool because we can do stuff like this: obj."${variableHoldingFieldName}" to dynamically set a property on an object
    clip."${fieldName}" = newValue

//    println "Set clip field '${fieldName}' to value '${newValue}'"
  }

  // Create a new playlist object and shove it into the store, then write data to disk
  public void handlePlaylistUpdate(Map inData) {
    Playlist p = PlaylistStore.get().createPlaylistFromJson(inData)
    PlaylistStore.get().addOrUpdate(p)
    PlaylistStore.get().saveDataToDisk()
  }

  // Handles updates to the 'play state'
  // The playState is: whether we are playing, looping the current scene, or stopped, and the current playlistId and sceneId
  public void handlePlayStateUpdate(Map inData) {
    int playlistId = inData.activePlaylistId
    String playlistItemId = inData.activePlaylistItemId
    Playlist.PlayState playState = inData.playState as Playlist.PlayState

    // if we're already playing the correct playlist and item and we're in the correct playstate, don't do anything
    // this should prevent the playlist from restarting if we click it again in the UI and we're already on it
    if (playlistId == PlaylistManager.get().getCurrentPlaylist().getId()
        && playlistItemId == PlaylistManager.get().getCurrentPlaylist().getCurrentItem()?.getId()
        && playState == PlaylistManager.get().getCurrentPlayState()) {
      println "[StateManager] Already in the correct state, don't do anything"
      return
    }

    // check the current values and react accordingly
    // if playState changed, but activePlaylist didn't, we need to be able to update the playState without restarting the playlist
    if (playState == Playlist.PlayState.STOPPED) {
      // here we should just stop the playlist, doesn't matter
      println "[StateManager] playState updated to STOPPED"
      PlaylistManager.get().stop(playlistId, playlistItemId)
      return
    }

    // If we made it this far, we should play the incoming playlist, item, and playstate
    PlaylistManager.get().play(playlistId, playlistItemId, playState)
  }
}