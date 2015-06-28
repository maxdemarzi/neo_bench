import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.concurrent.duration._

class GetDocumentExt extends Simulation {

  val httpConf = http
    .baseURL("http://localhost:7474")
    .acceptHeader("application/json")
    /* Uncomment to see the response of each request.
    .extraInfoExtractor(extraInfo => {
    println(extraInfo.response.body.string)
    Nil
  })
    .disableResponseChunksDiscarding
  */

  // Use a data file for our requests and repeat values if we get to the end.
  val feeder = csv("ids100000.csv").circular


  val scn = scenario("Get Document Ext")
    .repeat(3125) {
    feed(feeder)
      .exec(
        http("get document ext")
          .get("/v1/service/getDocument/${key}")
          .basicAuth("neo4j", "swordfish")
          .check(status.is(200))
      )
  }

  setUp(
    scn.inject(rampUsers(32) over(0 seconds)).protocols(httpConf)
  )

}