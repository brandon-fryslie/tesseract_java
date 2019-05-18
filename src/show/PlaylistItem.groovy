package show

// Playlist item: One scene + one duration.  and any other fields we decide we want
public class PlaylistItem {
  // This needs its own ID so we know which item we're playing on the frontend and backend (and can agree)
  // There can be more than one of the same scene per playlist, so we can't use the playlist id
  // This doesn't need to be persistent, but it needs to be unique per instance of the backend
  String id;
  Scene scene;
  Integer duration;

  public PlaylistItem(String id, Scene scene, Integer duration) {
    this.id = id;
    this.scene = scene;
    this.duration = duration;
  }
}
