package hardware

import environment.PixelNode
import environment.WledPanel

class WledController {
  String name
  String ip
  List<PixelNode> pixels = []
  int frameSize
  int numColors = 4

  int spacingFactor = 10

  public WledController(String name, String ip, int globalX, int globalY, List<List<Integer>> mapping) {
    this.name = name
    this.ip = ip

    pixels = mapping.withIndex().collect { List<Integer> coords, int index ->
      def (Integer x, Integer y, Integer z) = coords

      x += globalX
      y += globalY

      // flip these on the x axis for some reason
      x = -x

      new PixelNode(x * spacingFactor, y * spacingFactor, z * spacingFactor, index)
    }

    frameSize = pixels.size() * numColors + 2
  }

  public byte[] getFrame() {
    byte[] res = new byte[frameSize]

    res[0] = (byte) 3 // DRGBW protocol
    res[1] = (byte) 5 // wait 5 seconds after last udp data to reset to patterns

    pixels.withIndex().each { PixelNode pixelNode, int index ->
      res[2 + index * numColors] = (byte) pixelNode.getR()
      res[3 + index * numColors] = (byte) pixelNode.getG()
      res[4 + index * numColors] = (byte) pixelNode.getB()
      res[5 + index * numColors] = (byte) 0.1
    }

    res
  }
}
