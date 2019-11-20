package environment

class WifiPixelPanel {


  public static List<PixelNode> buildNodes() {
    List<PixelNode> nodes = (0..6).collect { int n ->
      new PixelNode(n, 1, 1, n + 1)
    }

    nodes
  }
}
