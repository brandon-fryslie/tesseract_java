package hardware;

import environment.DracoPanel;

import java.util.ArrayList;


public class DracoController extends Controller {


    public ArrayList<DracoPanel> dracoPanelArray;


    //constructor
    public DracoController(String theIp, int theId, String theMac) {
        ip = theIp;
        id = theId;
        mac = theMac;

        dracoPanelArray = new ArrayList<DracoPanel>();
    }


    public void addStrandPanel(DracoPanel dracoPanel){
        dracoPanelArray.add(dracoPanel);

    }



}
