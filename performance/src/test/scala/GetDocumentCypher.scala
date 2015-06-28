import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.concurrent.duration._

class GetDocumentCypher extends Simulation {

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

  val query = """MATCH (f:PROFILES {_key:{key}) RETURN f"""
  val cypherQuery = """{"statements" : [{"statement" : "%s", "parameters" : { "key": "${key}" }}]}""".format(query)

  val scn = scenario("Get Document Cypher")
    .repeat(3125) {
    feed(feeder)
      .exec(
        http("get document")
          .post("/db/data/transaction/commit")
          .basicAuth("neo4j", "swordfish")
          .body(StringBody(cypherQuery))
          .asJSON
          .check(status.is(200))
      )
  }

  setUp(
    scn.inject(rampUsers(32) over(0 seconds)).protocols(httpConf)
  )

}
