package output;


import app.TesseractMain
import groovy.transform.CompileStatic
import hardware.WledController
import network.HyperMediaUDP;

//import com.heroicrobot.dropbit.devices.pixelpusher.Pixel;

@CompileStatic
public class WledUdpServer {
  private HyperMediaUDP udp;

  int myPort = 7777
  int wledPort = 21324


  private int numColors = 3;
  private int[] c = new int[numColors * 3];

  List<WledController> controllers

  public WledUdpServer(List<WledController> controllers) {
    this.controllers = controllers

//    //red
//    c[0] = 200;//255 is max
//    c[1] = 0;
//    c[2] = 0;
//
//    //blue
//    c[3] = 0;
//    c[4] = 200;
//    c[5] = 0;
//
//    //green
//    c[6] = 0;
//    c[7] = 0;
//    c[8] = 200;

    // create a new datagram connection and wait for incoming message
    udp = new HyperMediaUDP(TesseractMain.getMain(), myPort);
    udp.setBuffer(10000);

    udp.log(false);     // <-- printout the connection activity, but performance is affected
    udp.listen(false);
  }

  public void send() {
    controllers.each { WledController controller ->
      byte[] frame = controller.getFrame()
      udp.send(frame, controller.ip, wledPort);
    }
  }

  public void sendWledPanel() {

  }
}
