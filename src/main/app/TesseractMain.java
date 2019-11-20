package app;

import environment.PixelNode;
import environment.Stage;
import model.Channel;
import output.UDPModel;
import processing.core.PApplet;
import processing.video.Movie;
import show.Playlist;
import show.PlaylistManager;
import stores.ConfigStore;
import stores.PlaylistStore;
import stores.SceneStore;
import util.Util;
import websocket.WebsocketInterface;

public class TesseractMain extends PApplet {

  //single global instance
  private static TesseractMain _main;


  //CLIP CLASS ENUM
  public static final int NODESCAN = 0;
  public static final int SOLID = 1;
  public static final int COLORWASH = 2;
  public static final int VIDEO = 3;
  public static final int PARTICLE = 4;
  public static final int PERLINNOISE = 5;
  public static final int LINESCLIP = 6;

  private OnScreen onScreen;//only the main class gets to draw
  public UDPModel udpModel;
  public Stage stage;
  public Channel channel1;

  //Click the arrow on the line below to run the program in .idea
  public static void main(String[] args) {
    PApplet.main("app.TesseractMain", args);
  }

  public static TesseractMain getMain() {
    return _main;
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

    // Clear screen
    clear();

    // Start listening for UDP messages.  Handles sending/receiving all UDP data
    udpModel = new UDPModel(this);

    // Draw the on-screen visualization
    onScreen = new OnScreen(this);

    // The stage is the LED mapping
    stage = new Stage();

    // Get the configured stage value.  Controlled via configuration option
    String stageType = ConfigStore.get().getString("stageType");

    // eventually we might load a saved project which is a playlist and environment together
    stage.buildStage(stageType);

    // create channel
    channel1 = new Channel(1);

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

    //call run() on the current clips inside channels
    channel1.run();
    //channel2.run();


    //get the full list of hardware nodes
    int l = stage.getNodes().length;

    PixelNode[] nextPixelNodes = stage.getNodes();

    // something for transitions
//    stage.prevNodes = stage.getNodes();

    for (int i = 0; i < l; i++) {
      PixelNode node = nextPixelNodes[i];
      int[] rgb = renderNode(node); //does the blending between the channels, apply master FX



      //now store that color on the node so we can send it as UDP data to the lights
      node.setRGB(rgb[0], rgb[1], rgb[2]);

      nextPixelNodes[i] = node;
    }

//    stage.nodes = nextPixelNodes;

    onScreen.draw();

    //push dummy packets out to LEDS
    //udpModel.sendRabbitTest();


    //PUT BACK
    udpModel.send();
  }


  //determine final color for each pixelNode each frame
  public int[] renderNode(PixelNode pixelNode) {
    //return
    int[] rgb1 = channel1.drawNode(pixelNode);


    //apply channel brightness
    rgb1[0] = (int)rgb1[0];
    rgb1[1] = (int)rgb1[1];
    rgb1[2] = (int)rgb1[2];

    //TODO mix the 2 channels together

    return rgb1;

  }//end render pixelNode


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
    udpModel.sendFlameTest(4, 1);

    onScreen.mousePressed();
  }

  @Override
  public void mouseReleased() {
    udpModel.sendFlameTest(4, 0);

    onScreen.mouseReleased();
  }


  //Custom event handler on pApplet for video library
  public void movieEvent(Movie movie) { movie.read(); }
}
