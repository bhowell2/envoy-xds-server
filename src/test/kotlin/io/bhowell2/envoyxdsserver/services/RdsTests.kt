package io.bhowell2.envoyxdsserver.services

import io.bhowell2.envoyxdsserver.utils.RdsTestUtils.Companion.generateAddIndividualRouteJson
import io.bhowell2.envoyxdsserver.utils.RdsTestUtils.Companion.generateAddRouteConfigJson
import io.bhowell2.envoyxdsserver.utils.RdsTestUtils.Companion.generateUpdateRdsJson
import io.bhowell2.envoyxdsserver.utils.RdsTestUtils.Companion.generateRemoveIndividualRouteJson
import io.bhowell2.envoyxdsserver.utils.RdsTestUtils.Companion.generateRouteJson
import io.bhowell2.envoyxdsserver.utils.RdsTestUtils.Companion.generateVirtualHostJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * @author Blake Howell
 */
class RdsTests {

  @Test
  fun `should add route configuration`() {
    val rds = Rds()
    assertEquals(0, rds.getResourcesResponse().size())
    val addRouteConfig =
        generateAddRouteConfigJson("rc1",
                                   listOf(generateVirtualHostJson("vh1", listOf("*"),
                                                                  listOf(generateRouteJson("/", "cluster1", "aroute")))
                                   )
        )
    rds.updateService(generateUpdateRdsJson(listOf(addRouteConfig), null, null, null))
    assertEquals(1, rds.getResourcesResponse().size())
    assertNotNull(rds.getResourcesResponse().getJsonObject("rc1"))
  }

  @Test
  fun `should add route to existing route configuration`() {
    val rds = Rds()
    assertEquals(0, rds.getResourcesResponse().size())
    val addRouteConfig =
        generateAddRouteConfigJson("rc1",
                                   listOf(generateVirtualHostJson("vh1", listOf("*"),
                                                                  listOf(generateRouteJson("/", "cluster1", "aroute")))
                                   )
        )
    rds.updateService(generateUpdateRdsJson(listOf(addRouteConfig), null, null, null))
    assertEquals(1, rds.getResourcesResponse().size())
    assertEquals(1, rds.getResourcesResponse().getJsonObject("rc1").getJsonArray("virtual_hosts").size())
    assertEquals(1, rds.getResourcesResponse().getJsonObject("rc1").getJsonArray("virtual_hosts").getJsonObject(0)
        .getJsonArray("routes").size())

    // update by adding a route
    val rds2 = Rds(rds)
    assertEquals(1, rds2.getResourcesResponse().size())
    val addRoute = generateAddIndividualRouteJson("rc1", "vh1", generateRouteJson("/hey", "cluster1", "aroute2"))
    rds2.updateService(generateUpdateRdsJson(null, listOf(addRoute), null, null))
    // should still be one, but the number of routes should be 2
    val rds2Json = rds2.getResourcesResponse()
    assertEquals(1, rds2Json.size())
    assertEquals(1, rds2Json.getJsonObject("rc1").getJsonArray("virtual_hosts").size())
    assertEquals(2, rds2Json.getJsonObject("rc1").getJsonArray("virtual_hosts").getJsonObject(0)
        .getJsonArray("routes").size())
  }

  @Test
  fun `should remove only route but not entire route configuration`() {
    val rds = Rds()
    assertEquals(0, rds.getResourcesResponse().size())
    val addRouteConfig =
        generateAddRouteConfigJson("rc1",
                                   listOf(generateVirtualHostJson("vh1", listOf("*"),
                                                                  listOf(generateRouteJson("/", "cluster1", "aroute"),
                                                                         generateRouteJson("/", "cluster1", "adiffroute")))
                                   )
        )
    rds.updateService(generateUpdateRdsJson(listOf(addRouteConfig), null, null, null))
    assertEquals(1, rds.getResourcesResponse().size())
    assertEquals(1, rds.getResourcesResponse().getJsonObject("rc1").getJsonArray("virtual_hosts").size())
    assertEquals(2, rds.getResourcesResponse().getJsonObject("rc1").getJsonArray("virtual_hosts").getJsonObject(0)
        .getJsonArray("routes").size())

    val rds2 = Rds(rds)
    // remove individual route
    rds2.updateService(generateUpdateRdsJson(null, null, null,
                                             listOf(generateRemoveIndividualRouteJson("rc1", "vh1", "aroute"))))
    val rds2Json = rds2.getResourcesResponse()
    assertEquals(1, rds2Json.size())
    assertEquals(1, rds2Json.getJsonObject("rc1").getJsonArray("virtual_hosts").size())
    assertEquals(1, rds2Json.getJsonObject("rc1").getJsonArray("virtual_hosts").getJsonObject(0)
        .getJsonArray("routes").size())
  }

  @Test
  fun `should remove entire route configuration`() {
    val rds = Rds()
    assertEquals(0, rds.getResourcesResponse().size())
    val addRouteConfig =
        generateAddRouteConfigJson("rc1",
                                   listOf(generateVirtualHostJson("vh1", listOf("*"),
                                                                  listOf(generateRouteJson("/", "cluster1", "aroute")))
                                   )
        )
    rds.updateService(generateUpdateRdsJson(listOf(addRouteConfig), null, null, null))
    assertEquals(1, rds.getResourcesResponse().size())
    assertEquals(1, rds.getResourcesResponse().getJsonObject("rc1").getJsonArray("virtual_hosts").size())
    assertEquals(1, rds.getResourcesResponse().getJsonObject("rc1").getJsonArray("virtual_hosts").getJsonObject(0)
        .getJsonArray("routes").size())

    val rds2 = Rds(rds)
    // remove individual route
    rds2.updateService(generateUpdateRdsJson(null, null, listOf("rc1"), null))
    assertEquals(0, rds2.getResourcesResponse().size())
  }

}