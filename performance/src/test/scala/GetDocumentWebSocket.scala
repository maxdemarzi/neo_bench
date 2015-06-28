import com.ning.http.util.Base64

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.concurrent.duration._

class GetDocumentWebSocket  extends Simulation {

  val httpConf = http
  .baseURL("http://localhost:7474")
  .wsBaseURL("ws://localhost:7474")
  .acceptHeader("application/json")

  // Use a data file for our requests and repeat values if we get to the end.
  val feeder = csv("ids100000.csv")
    .circular


val scn = scenario("Get Document WebSocket")
  .exec(ws("Connect WS")
    .open("/websocket"))
  .repeat(3125) {
    feed(feeder)
      .exec(ws("get document websocket")
    .sendText("""${key}""")
    .check(wsAwait.within(3).until(1).regex("hello").saveAs("profile")))
      .exec(session => {
      val profile = session.get("profile").asOption[String]
      println(profile.getOrElse("Profile not Found"))
      session
    })
   }

  .exec(ws("Close WS").close)

setUp(
scn.inject(rampUsers(32) over(0 seconds)).protocols(httpConf)
)

}

/*
    .repeat(3) {
      feed(feeder)
       .exec(ws("get document websocket")
        .sendText("""${key}""")
        .check(wsListen.within(30).until(1).regex("(.*?)").saveAs("profile"))
        /* uncomment to print */
        //.saveAs("profile")
        /**/
        ).exitHereIfFailed
    /* uncomment to print */
    .exec(session => {
    val profile = session.get("profile").asOption[String]
    println(profile.getOrElse("Profile not Found"))
    session
    }) /**/
    }
 */


/*
    .check(wsAwait.within(300).until(1).regex("(.*?)").saveAs("profile")))
      .exec(session => {
      val profile = session.get("profile").asOption[String]
      println(profile.getOrElse("Profile not Found"))
      session
    })
   }

 */