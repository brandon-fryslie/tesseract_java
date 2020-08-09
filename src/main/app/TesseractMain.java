package app;

import environment.PixelNode;
import environment.Stage;
import hardware.WledController;
import model.Channel;
import output.UDPModel;
import output.WledUdpServer;
import processing.core.PApplet;
import processing.video.Movie;
import show.Playlist;
import show.PlaylistManager;
import stores.ConfigStore;
import stores.PlaylistStore;
import stores.SceneStore;
import util.Util;
import websocket.WebsocketInterface;

import java.util.ArrayList;
import java.util.List;

public class TesseractMain extends PApplet {

  //single global instance
  private static TesseractMain _main;


  //CLIP CLASS ENUM
  public enum CLIPTYPES {
    NODESCAN, SOLID, COLORWASH, VIDEO, PARTICLE, PERLINNOISE, LINESCLIP
  }

  private OnScreen onScreen; //only the main class gets to draw
  private WledUdpServer wledUdpServer;
  private Stage stage;
  private Channel channel1;

  //Click the arrow on the line below to beforeFrame the program in .idea
  public static void main(String[] args) {
    PApplet.main("app.TesseractMain", args);
  }

  public static TesseractMain getMain() {
    return _main;
  }

  public Stage getStage() {
    return this.stage;
  }

  @Override
  public void settings() {

    size(1400, 800, P3D);

    // Required for the application to launch on Ubuntu Linux (Intel NUC w/ Intel integrated graphics)
    // It has something to do with the specific OS/packages/video drivers/moon cycles/etc
    https://github.com/processing/processing/issues/5476
    System.setProperty("jogl.disable.openglcore", "false");

    //looks nice, but runs slower, one reason to put UI in browser
    //pixelDensity(displayDensity()); //for mac retna displays
    //pixelDensity(2);
  }

  @Override
  public void setup() {
    frameRate( 30 );

    Util.enableColorization();

    _main = this;

    // Configure Data and Stores

    // Make some dummy data in the stores
    Util.createBuiltInScenes();
    Util.createBuiltInPlaylists();

    // Saves the default data
    SceneStore.get().saveDataToDisk();
    PlaylistStore.get().saveDataToDisk();

    // Load configuration from file.  This must happen AFTER we've created our initial playlists, or it will fail on a fresh install
    ConfigStore.get();

    // Initialize websocket connection
    WebsocketInterface.get();

    // The stage is the LED mapping
    stage = new Stage();

    // Get the configured stage value.  Controlled via configuration option
    String stageType = ConfigStore.get().getString("stageType");

    // eventually we might load a saved project which is a playlist and environment together
    List<WledController> controllers = stage.buildStage(stageType);

    // Start listening for UDP messages.  Handles sending/receiving all UDP data
    wledUdpServer = new WledUdpServer(controllers);

    // Clear screen
    clear();

    // Draw the on-screen visualization
    onScreen = new OnScreen(this);

    // create channel
    channel1 = new Channel();

    // Tell the PlaylistManager which channel to play playlists in
    PlaylistManager.get().setChannel(this.channel1);

    // Get initial playlist & playState from config
    Playlist initialPlaylist = PlaylistStore.get().find("displayName", ConfigStore.get().getString("initialPlaylist"));
    Playlist.PlayState initialPlayState = Util.getPlayState(ConfigStore.get().getString("initialPlayState"));

    // Play the playlist w/ the playState defined in our configuration
    PlaylistManager.get().play(initialPlaylist.getId(), null, initialPlayState);

    // The shutdown hook will let us clean up when the application is killed.  It is very important to clean up the websocket server so we don't leave the port in use
    createShutdownHook();
  }

  @Override
  public void draw() {
    clear();

    //call beforeFrame() on the current clips inside channels
    channel1.run();
    //channel2.beforeFrame();

    for (PixelNode node : stage.getNodes()) {
      int[] rgb = renderNode(node); //does the blending between the channels, apply master FX

      //now store that color on the node so we can send it as UDP data to the lights
      node.setRGB(rgb[0], rgb[1], rgb[2]);
    }

    // Draw onScreen representation
    onScreen.draw();

    // Send UDP Packets
    wledUdpServer.send();
  }


  // determine final color for each pixelNode each frame
  // This is where we would mix multiple channels of data
  public int[] renderNode(PixelNode pixelNode) {
    return channel1.drawNode(pixelNode);

  }

  private void createShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        WebsocketInterface.get().shutdownServer();
      }
    });
  }

  //EVENT HANDLERS
  //calls happen on pApplet, then can be routed to the proper place in our code
  @Override
  public void mousePressed() {
    onScreen.mousePressed();
  }

  @Override
  public void mouseReleased() {
    onScreen.mouseReleased();
  }


  //Custom event handler on pApplet for video library
  public void movieEvent(Movie movie) { movie.read(); }
}
