package environment

import groovy.transform.CompileStatic
import hardware.*

@CompileStatic
public class PixelNode {
  int x
  int y
  int z

  float screenX
  float screenY

  int index //0-143 usually relative to the fixture

  int r
  int g
  int b

  //constructor
  public PixelNode(int x, int y, int z, int index) {
    this.x = x
    this.y = y
    this.z = z

    this.index = index
  }

  public void setRGB(int r, int g, int b) {
    this.r = r
    this.g = g
    this.b = b
  }
}
