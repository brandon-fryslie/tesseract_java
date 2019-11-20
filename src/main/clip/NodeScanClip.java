package clip;

import environment.PixelNode;

public class NodeScanClip extends AbstractClip {

    private int _scanNode;

    //constructor
    public NodeScanClip() {

    }

    public void init() {
        clipId = "node_scan";

        super.init();

        _scanNode = 0;
    }

    public void run() {
        _scanNode++;

        if(_scanNode >= _myMain.stage.getNodes().length){
            _scanNode = 0;
        }

    }

    public int[] drawNode(PixelNode pixelNode) {

        int[] nodestate = new int[3];

        if(pixelNode.getIndex() == _scanNode){
            nodestate[0] = 255;
            nodestate[1] = 255;
            nodestate[2] = 255;
        }

        return nodestate;
    }
}
