package environment


import app.TesseractMain
import groovy.transform.CompileStatic
import hardware.DracoController
import hardware.WledController

@CompileStatic
public class Stage {

  //used to automatically define bounding box
  int maxX
  int maxY
  int maxZ

  int minX
  int minY
  int minZ

  float maxW
  float maxH
  float maxD

  //	An array of all the LEDs, used for render
  private PixelNode[] nodes
  private PixelNode[] prevNodes //last frames data

  private TesseractMain _myMain

  public Stage() {
    this.nodes = []
    _myMain = TesseractMain.getMain()
  }

  public PixelNode[] getNodes() {
    this.nodes
  }

  public PixelNode[] getPrevNodes() {
    this.prevNodes
  }

  // this return type will have to change but should work for now
  public List<WledController> buildStage(String stageType) {
    List<WledController> controllers

    if (stageType == 'BITTY') {
      controllers = buildBittyStage()
    } else if (stageType == 'CUBOTRON') {
      controllers = buildCubotron()
    } else {
      throw new RuntimeException("ERROR: Invalid stage type: ${stageType}")
    }

    PixelNode[] nodes = controllers.inject([] as List<PixelNode>) { List<PixelNode> result, WledController controller ->
      result + controller.getPixels()
    }

    nodes


    //set the boundaries of the stage
    // Clip each node to the maxX/Y/Z?
    for (PixelNode n : nodes) {
      if (n.x > maxX) maxX = n.x
      if (n.y > maxY) maxY = n.y
      if (n.z > maxZ) maxZ = n.z

      if (n.x < minX) minX = n.x
      if (n.y < minY) minY = n.y
      if (n.z < minZ) minZ = n.z
    }

    maxW = maxX + Math.abs(_myMain.getStage().minX)
    maxH = maxY + Math.abs(_myMain.getStage().minY)
    maxD = maxZ + Math.abs(_myMain.getStage().minZ)

    _myMain.println("maxW: " + maxW)
    _myMain.println("maxH: " + maxH)
    _myMain.println("maxD: " + maxD)

    this.nodes = nodes

    controllers

  }

  private List<WledController> buildCubotron() {
    List<List<Integer>> mapping = []

    // Initialize a crap-ton of nodes, just a big basic cubeotron
    for (int i = 0; i < 30; i++) {
      for (int j = 0; j < 30; j++) {
        for (int k = 0; k < 30; k++) {
          mapping << [10 * i, 10 * j, 10 * k]
        }
      }
    }
    [new WledController('CubotronController', 'x.x.x.x', 0, 0, mapping)]
  }



  private List<WledController> buildBittyStage() {

    // create controller

    Map config = [
        controllers: [
            [
                name: 'pixelCloud1',
                ip: 'pixelCloud1',
                globalX: 10,
                globalY: 0,
                mapping: [
                    [2, 0, 0],
                    [4, 0, 0],
                    [6, 0, 0],
                    [8, 0, 0],
                    [10, 0, 0],
                    [12, 0, 0],
                    [14, 0, 0],
                    [15, 1, 0],
                    [14, 2, 0],
                    [13, 3, 0],
                    [12, 4, 0],
                    [11, 5, 0],
                    [10, 6, 0],
                    [6, 6, 0],
                    [5, 5, 0],
                    [4, 4, 0],
                    [3, 3, 0],
                    [2, 2, 0],
                    [1, 1, 0],
                ]
            ],
            [
                name: 'pixelCloud2',
                ip: 'pixelCloud2',
                globalX: 0,
                globalY: 0,
                mapping: [
                    [0, 0, 0],
                    [1, 0, 0],
                    [2, 0, 0],
                    [3, 0, 0],
                    [4, 0, 0],
                    [5, 0, 0],
                    [5, 1, 0],
                    [4, 1, 0],
                    [3, 1, 0],
                    [2, 1, 0],
                    [1, 1, 0],
                    [0, 1, 0],
                    [0, 2, 0],
                    [1, 2, 0],
                    [2, 2, 0],
                    [3, 2, 0],
                    [4, 2, 0],
                    [5, 2, 0],
                    [5, 3, 0],
                    [4, 3, 0],
                    [3, 3, 0],
                    [2, 3, 0],
                    [1, 3, 0],
                    [0, 3, 0],
                    [0, 4, 0],
                    [1, 4, 0],
                    [2, 4, 0],
                    [3, 4, 0],
                    [4, 4, 0],
                    [5, 4, 0],
                ]
            ]
        ]
    ]

    List<Map> controllerMaps = config.controllers as List<Map>

    List<WledController> controllers = controllerMaps.collect { Map controllerMap ->
      new WledController(controllerMap.name as String, controllerMap.ip as String, controllerMap.globalX as int, controllerMap.globalY as int, controllerMap.mapping as List<List<Integer>>)
    }

    controllers
  }
}
