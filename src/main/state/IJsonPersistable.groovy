package state

import groovy.transform.CompileStatic

// Objects that can be persisted to a json object in our homebrewed persistence mechanism
// This is probably mostly pointless but you never know
@CompileStatic
interface IJsonPersistable {
  def asJsonObj()
}
