package environment;

import hardware.*;

public class Node  {
  public int x;
  public int y;
  public int z;

  public int index; //0-143 usually relative to the fixture
  public int port;  //1-8 on pixel pusher
  public Fixture fixture;

  public int nodeType;
  
  public int r;
  public int g;
  public int b;


  //constructor
  Node(int theX, int theY, int theZ, int theIndex, Tile theFixture) {
    x = theX;
    y = theY;
    z = theZ;

    index = theIndex;
    fixture = theFixture;
  }    

} 
