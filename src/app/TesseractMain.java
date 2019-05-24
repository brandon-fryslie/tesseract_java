package app;


import processing.core.*;

import output.*;
import environment.*;
import model.*;
import state.StateManager;
import stores.PlaylistStore;
import stores.SceneStore;
import util.Util;
import show.*;
import websocket.WebsocketInterface;


import java.util.*;


public class TesseractMain extends PApplet {

  //single global instance
  private static TesseractMain _main;


  //CLIP CLASS ENUM
  public static final int NODESCAN = 0;
  public static final int SOLID = 1;
  public static final int COLORWASH = 2;
  public static final int STRIPE = 3;

  public static String[] clipNames = {
      "nodescan",
      "solid",
      "colorwash"
  };

  private UDPModel udpModel;
  private OnScreen onScreen;


  public Stage stage;

  public Channel channel1;
  public Channel channel2;

  public WebsocketInterface ws;
  public StateManager stateManager;
  public SceneStore sceneStore;
  public PlaylistStore playlistStore;

  public Playlist currentPlaylist;

  //Click the arrow on the line below to run the program in .idea
  public static void main(String[] args) {
    PApplet.main("app.TesseractMain", args);
  }

  public static TesseractMain getMain() {
    return _main;
  }

  @Override
  public void settings() {

    size(1100, 800, P3D);

    //looks nice, but runs slower, one reason to put UI in browser
    //pixelDensity(displayDensity()); //for mac retna displays
    //pixelDensity(2);
  }

  @Override
  public void setup() {
    Util.enableColorization();

    _main = this;

    // Persistence / state update stuff
    ws = WebsocketInterface.get();
    stateManager = StateManager.get();
    sceneStore = SceneStore.get();
    // Doesn't make sense to fully implement this until we know more about how playlists are gonna work
    playlistStore = PlaylistStore.get();

    clear();

    udpModel = new UDPModel(this);

    onScreen = new OnScreen(this);

    stage = new Stage(this);

    //pass in an XML stage definition, eventually we might load a saved project which is a playlist and environment together
    stage.buildStage();

    //create channels
    channel1 = new Channel(1);
    //channel2 = new Channel(2);

    //make a dummy clip, one way to use direct control and load a clip directly into a channel, no scene necessary
    //channel1.constructNewClip(SOLID);

    // Make some dummy data in the stores
    this.createBuiltInScenes();
    this.createBuiltInPlaylists();

    // Save the created data to disk so we persist our manually created scenes/playlists
    // This also has the effect of resetting any changes we make to them in the UI once we start the backend
    this.sceneStore.saveDataToDisk();
    this.playlistStore.saveDataToDisk();

    // Set the channel on the playlist manager
    PlaylistManager.get().setChannel(this.channel1);

    // Play the playlist with id = 1, play the first item in the playlist, and start in the 'looping' state
    PlaylistManager.get().play(1, null, Playlist.PlayState.LOOP_SCENE);

    // The shutdown hook will let us clean up when the application is killed
    createShutdownHook();
  }

  @Override
  public void draw() {
    clear();

    //call run() on the current clips inside channels
    channel1.run();
    //channel2.run();


    //get the full list of hardware nodes
    int l = stage.nodes.length;

    Node[] nextNodes = stage.nodes;
    stage.prevNodes = stage.nodes;

    for (int i = 0; i < l; i++) {
      Node n = nextNodes[i];
      int[] rgb = renderNode(n); //does the blending between the channels, apply master FX

      //now store that color on the node so we can send it as UDP data to the lights
      n.r = rgb[0];
      n.g = rgb[1];
      n.b = rgb[2];

      nextNodes[i] = n;
    }

    stage.nodes = nextNodes;

    onScreen.draw();
  }

  @Override
  public void mousePressed() {
    onScreen.mousePressed();
  }

  @Override
  public void mouseReleased() {
    onScreen.mouseReleased();
  }

  //determine final color for each node each frame
  public int[] renderNode(Node node) {
    //return
    int[] rgb1 = channel1.drawNode(node);


    //apply channel brightness
    //rgb1[0] = int(rgb1[0] * (channelCanvas1.channelBrightness/100) * (channelCanvas1.rBrightness/100));
    //rgb1[1] = int(rgb1[1] * (channelCanvas1.channelBrightness/100) * (channelCanvas1.gBrightness/100));
    //rgb1[2] = int(rgb1[2] * (channelCanvas1.channelBrightness/100) * (channelCanvas1.bBrightness/100));

    //mix the 2 channels together

    return rgb1;

  }//end render node

  private void createBuiltInScenes() {
    // These are hydrated from the json now.  creating them here will update the existing data in the store, but this can be commented out and it will load entirely from disk
    // If we specify the id in the constructor and it matches an existing Scene, it will update the data.  omitting the ID from the constructor will use the max id + 1 for the new scene
    Scene sWash = new Scene(4, "Color Wash", TesseractMain.COLORWASH, new float[]{0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f});
    this.sceneStore.addOrUpdate(sWash);

    Scene sYellow = new Scene(1, "Yellow", TesseractMain.SOLID, new float[]{0, 0, 0, 1, 1, 0, 0});
    this.sceneStore.addOrUpdate(sYellow);

    Scene sPurple = new Scene(2, "Purple", TesseractMain.SOLID, new float[]{0, 0, 0, 1, 0, 1, 0});
    this.sceneStore.addOrUpdate(sPurple);

    Scene sRed = new Scene(3, "Red", TesseractMain.SOLID, new float[]{0, 0, 0, 1, 0, 0, 0});
    this.sceneStore.addOrUpdate(sRed);
  }

  private void createBuiltInPlaylists() {
    // have to be a bit smarter about how we create the initial playlist so these uuids don't change all the time, its annoying

    List<PlaylistItem> playlistItems = Arrays.asList(
        new PlaylistItem(UUID.randomUUID().toString(), this.sceneStore.find("id", 4), 4),
        new PlaylistItem(UUID.randomUUID().toString(), this.sceneStore.find("id", 1), 3),
        new PlaylistItem(UUID.randomUUID().toString(), this.sceneStore.find("id", 4), 1),
        new PlaylistItem(UUID.randomUUID().toString(), this.sceneStore.find("id", 2), 4),
        new PlaylistItem(UUID.randomUUID().toString(), this.sceneStore.find("id", 3), 3),
        new PlaylistItem(UUID.randomUUID().toString(), this.sceneStore.find("id", 4), 5),
        new PlaylistItem(UUID.randomUUID().toString(), this.sceneStore.find("id", 1), 1)
    );

    // Kinda hacky but what we do here is see if we've already got a playlist w/ the id we want to create, and if so, we
    // loop through the existing playlist items and use those UUIDs on the PlaylistItems we're using in 'addOrUpdate'
    // This prevents the playlist.json file from changing every time we launch the application
    Playlist existingPlaylist = PlaylistStore.get().find("id", 1);
    if (existingPlaylist != null) {
      for (int i = 0; i < existingPlaylist.getItems().size() && i < playlistItems.size(); i++) {
        playlistItems.get(i).setId(existingPlaylist.getItems().get(i).getId());
      }
    }

    Playlist playlist1 = new Playlist(1, "Cubotron", 60, playlistItems);

    this.playlistStore.addOrUpdate(playlist1);
  }

  private void createShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        WebsocketInterface.get().shutdownServer();
      }
    });
  }
}