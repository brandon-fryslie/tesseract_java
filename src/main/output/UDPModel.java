package output;


import environment.DracoPanel;
import environment.PixelNode;
//import com.heroicrobot.dropbit.devices.pixelpusher.Pixel;

import network.HyperMediaUDP;
import processing.core.PApplet;

import hardware.*;
import stores.ConfigStore;


public class UDPModel {

    private PApplet p;
    private HyperMediaUDP hyperMediaUdp;

    public DracoController[] teensies;

    public int myPort      = 7777; //6000 also works
    public int rabbitPort  = 7;
    public int teensyPort  = 1337;


    private int numTiles    = 9;
    private String broadcastIp = "255.255.255.255";  // the remote IP address, rabbit uses DHCP, so you might have to check the router or use the driver app to get the IP


    private int numColors = 3;
    private int[] c = new int[numColors*3];
    private int currentNode = 0;
    private int count   = 0;
    private int numNodes = 12*12;

    private int[][] nodeMap = new int[12][12];


    public UDPModel(PApplet pApplet) {
        p = pApplet;

//        //red
//        c[0] = 200;//255 is max
//        c[1] = 0;
//        c[2] = 0;
//
//        //blue
//        c[3] = 0;
//        c[4] = 200;
//        c[5] = 0;
//
//        //green
//        c[6] = 0;
//        c[7] = 0;
//        c[8] = 200;

        //fill a hash map with hardware node positions
        createNodeMap();

        // create a new datagram connection on 6000
        // and wait for incoming message
        hyperMediaUdp = new HyperMediaUDP( p, myPort );
        hyperMediaUdp.setBuffer(10000);

        hyperMediaUdp.log( false );     // <-- printout the connection activity, but performance is affected
        hyperMediaUdp.listen( false );
    }

    private void createNodeMap(){
        for (int k=0; k<12; k++){//y
            for (int j=0; j<12; j++){//x

                int serial_port = (int) k/4; //what serial port does this coordinate belong to?
                //println(serial_port);

                int chipRow = (k%4)/2;// either 0 or 1
                int chipCol = (int) ((1-chipRow)*4);
                int chip_address = (int) chipCol+ (int)(j/3);
                //println(chip_address);

                int node_number = 0;

                if(k%2==0){//calculate the node number now
                    if (j%3==0){
                        node_number=4;
                    }else if (j%3==1){
                        node_number=5;
                    }else{
                        node_number=0;
                    }

                }else{
                    //node_number=(j%3)+2;
                    if (j%3==0){
                        node_number=3;
                    }else if (j%3==1){
                        node_number=2;
                    }else{
                        node_number=1;
                    }
                }

                //we now have serialport chipadress and node number
                //now we need to calculate the corresponding address in the outbuff
                int OUTBUFF_position = ((chip_address*6*9)+node_number*9);//this is the base chip position
                OUTBUFF_position = OUTBUFF_position+serial_port;// offset by the corrct amount for the correct serial port.
                nodeMap[k][j] = OUTBUFF_position;

            }
        }

    /*
     for (int y=0; y<12; y++){
       for (int x=0; x<12; x++){
         println(nodeMap[y][x]);
       }
     }
    */
    }

    public void send() {
        for (DracoController dracoController : teensies) {
            //sendTeensyNodesAsPanels(dracoController);

            sendPanelFrame(dracoController);

            //swap command, makes all the tiles change at once
            byte[] data = new byte[1];
            data[0] = (byte) ('s');
            hyperMediaUdp.send( data, dracoController.ip, teensyPort );
        }

    }


    // Send tile to Tesseract
    public void sendTileFrame(Object tile){
        //data for one tile, one frame
        byte[] data = new byte[(432+4)]; //144 nodes * 3 channels per node = 432

        data[0] = (byte) (p.unhex("ff")); //listen for command
        data[1] = (byte) (p.unhex("ff")); //chip command
        data[2] = (byte) (p.unhex("00")); //start command
//        data[3] = (byte) (tile.id-1); //tile address
        data[3] = (byte) (1); //tile address

        //node map for one tile
        for (int y=0; y<12; y++){
            for (int x=0; x<12; x++){
                //one pixelNode, set each channel
//                PixelNode pixelNode = tile.tileNodeArray[x][y];
                PixelNode pixelNode = new PixelNode(1,2,3, 4);

                data[nodeMap[y][x]+4] = (byte) pixelNode.getG();
                data[nodeMap[y][x]+3+4] = (byte) pixelNode.getR();
                data[nodeMap[y][x]+6+4] = (byte) pixelNode.getB();

                //hack for using v1 tiles
//                if(tile.channelSwap){
//                    data[nodeMap[y][x]+4] = (byte)pixelNode.b;
//                    data[nodeMap[y][x]+6+4] = (byte)pixelNode.g;
//                }
            }
        }
        // send the message for 1 tile
        String ip = "tile.parentRabbit.ip";

        //if(app.BROADCAST && ip!="X.X.X.X"){
            hyperMediaUdp.send( data, ip, rabbitPort );
        //}

    }//end sendTileFrame


    // Send data to the Draco panels via DracoController
    public void sendFlameTest(int pin, int on) {

        for (DracoController dracoController : teensies) {
            System.out.printf("FIRE TEENSY IP: %s \n", dracoController.ip);

            //fire command
            byte[] data = new byte[3];
            data[0] = (byte) ('f');
            data[1] = (byte) pin;
            data[2] = (byte) on;
            hyperMediaUdp.send(data, dracoController.ip, teensyPort);
        }
    }


    // Send data to the Draco panels via DracoController
    public void sendPanelFrame(DracoController dracoController) {

        //octo pin order is orange, blue, green, brown

        for(DracoPanel dracoPanel : dracoController.dracoPanelArray) {

            int l = dracoPanel.strandNodeArray.length;

            byte[] data = new byte[(l*3) + 2];

            data[0] = (byte) ('l'); //LIGHTS command
            data[1] = (byte) dracoPanel.pinNum; //pin address, once again we are doing one node per pin

            for (int i=0; i<l; i++){
                PixelNode pixelNode = dracoPanel.strandNodeArray[i];

                data[(i*3) + 0 +2] = (byte) pixelNode.getR();
                data[(i*3) + 1 +2] = (byte) pixelNode.getG();
                data[(i*3) + 2 +2] = (byte) pixelNode.getB();

                //for the love of god, something please just happen on the lights so I know my life isn't a complete sham.
                //data[(i*3) + 0 +2] = (byte) (PApplet.unhex("FF"));
                //data[(i*3) + 1 +2] = (byte) (PApplet.unhex("FF"));
                //data[(i*3) + 2 +2] = (byte) (PApplet.unhex("FF"));
            }

            // send the bytes for each tile separately
            hyperMediaUdp.send( data, dracoController.ip, teensyPort );
        }
    }//end sendPanelFrame

    /*
    // Send data to the Draco panels via DracoController
    public void sendTeensyNodesAsPanels(DracoController teensy) {

        int length = teensy.nodeArray.length;

        //basically a node is an entire panel or tile
        for (int t=0; t<length; t++){

            PixelNode node =  teensy.nodeArray[t];
            int ledPerPin = 100;

            byte[] data = new byte[(ledPerPin*3) + 2];

            data[0] = (byte) ('l'); //LIGHTS command
            data[1] = (byte) t; //pin address, once again we are doing one node per pin

            for (int i=0; i<ledPerPin; i++){
                data[(i*3) + 0 +2] = (byte) node.r;
                data[(i*3) + 1 +2] = (byte) node.g;
                data[(i*3) + 2 +2] = (byte) node.b;

                //for the love of god, something please just happen on the lights so I know my life isn't a complete sham.
                //data[(i*3) + 0 +2] = (byte) (PApplet.unhex("FF"));
                //data[(i*3) + 1 +2] = (byte) (PApplet.unhex("FF"));
                //data[(i*3) + 2 +2] = (byte) (PApplet.unhex("FF"));
            }

            // send the bytes for each tile separately
            hyperMediaUdp.send( data, teensy.ip, wledPort );
        }
    }
    */



    public void sendRabbitTest() {

        for (int t=0; t<numTiles; t++){
            int nodeCount = 0;

            byte[] data = new byte[(432+4)];

            data[0] = (byte) (p.unhex("ff"));
            data[1] = (byte) (p.unhex("ff")); //chip command
            data[2] = (byte) (p.unhex("00")); //start command
            data[3] = (byte) t; //tile address

            for (int y=0; y<12; y++){
                for (int x=0; x<12; x++){
                    if(nodeCount != currentNode){

                        data[nodeMap[y][x]+4] = (byte) (PApplet.unhex("00"));
                        data[nodeMap[y][x]+3+4] = (byte) (PApplet.unhex("00"));
                        data[nodeMap[y][x]+6+4] = (byte) (PApplet.unhex("00"));

                    }else{

                        data[nodeMap[y][x]+4] = (byte) c[count + 1]; //b
                        data[nodeMap[y][x]+3+4] = (byte) c[count + 0]; //r
                        data[nodeMap[y][x]+6+4] = (byte) c[count + 2]; //g
                    }

                    nodeCount++;
                }
            }

            // send the message for each tile
            //hyperMediaUdp.send( data, broadcastIp, rabbitPort );
            hyperMediaUdp.send( data, "192.168.1.105", rabbitPort );


        }//end for num tiles

        //swap command
        byte[] data = new byte[2];
        data[0] = (byte) (p.unhex("FF"));
        data[1] = (byte) (p.unhex("FE"));
        hyperMediaUdp.send( data, broadcastIp, rabbitPort );


        currentNode++;//increment node
        if(currentNode >= numNodes){
            currentNode = 0;

            count+=3;//increment color
            if(count >= (numColors*3)){
                count = 0;
            }
        }

    }

}
