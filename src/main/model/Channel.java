package model;

import clip.*;
import environment.PixelNode;
import processing.core.PApplet;
import show.Scene;


public class Channel {
  private Scene _currentScene; //use getter setter
  private float _mix = 0.0f;

  //CONSTRUCTOR
  public Channel() {

  }

  // animation logic that runs per frame
  public void run() {
    if (_currentScene != null) {
      if (_currentScene.getClip() != null) {
        _currentScene.getClip().run();
      }
    }
  }

  //this is just a generic call that reaches down into clips to perform drawing unique to each clip
  public int[] drawNode(PixelNode pixelNode) {
    int[] result = new int[3];

    if (_currentScene != null) {
      result = _currentScene.getClip().drawNode(pixelNode);
    }

    return result;
  }


  public void drawUI(PApplet p, int x, int y) {
    //draw the state of this channel to processing
  }

  // this is not safe and caused an NPE exception at one point
  // need to have a proper way to 'stop' the scene, but this works for now
  public void unsetScene() {
    this._currentScene = null;
  }

  public void setScene(Scene scene) {
    this._currentScene = scene;
  }

  public Scene getScene() {
    return _currentScene;
  }

  // I need a reference to the 'active' clip, meaning the one we are playing or will be playing once the transition is finished
  // Returns _nextScene.getClip() if _nextScene is set, otherwise _currentScene.getClip().  if both are null, returns null
  public AbstractClip getActiveClip() {
    if (this._currentScene != null) {
      return this._currentScene.getClip();
    } else {
      return null;
    }
  }
}
