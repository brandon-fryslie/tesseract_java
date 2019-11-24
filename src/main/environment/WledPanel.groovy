package environment

import groovy.transform.CompileStatic

@CompileStatic
class WledPanel {
  public static List<PixelNode> buildNodes() {
    int scaleFactor = 10

    List<PixelNode> nodes = (0..6).collect { int n ->
      new PixelNode(n * scaleFactor, 1 * scaleFactor, 1 * scaleFactor, n + 1)
    }

    nodes
  }
}
