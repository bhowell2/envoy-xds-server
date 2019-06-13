package io.bhowell2.envoyxdsserver.services

import io.bhowell2.envoyxdsserver.api.EdsApi
import io.bhowell2.envoyxdsserver.utils.CdsTestUtils.Companion.generateClusterLoadAssignmentJson
import io.bhowell2.envoyxdsserver.utils.EdsTestUtils.Companion.generateEdsUpdateJson
import io.bhowell2.envoyxdsserver.utils.EdsTestUtils.Companion.generateRemoveEndpointJson
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlin.test.*

/**
 * @author Blake Howell
 */

class EdsTests {

  @Test
  fun `should add endpoint`() {
    val loadAssm = generateClusterLoadAssignmentJson("test1", "1.2.3.4", 9999)
    val addCla = listOf(loadAssm)
    val updateEds = generateEdsUpdateJson(addCla, null, null, null)

    val eds = Eds()
    eds.updateService(updateEds)

    assertEquals(1, eds.getResourcesResponse().size())

    // check that endpoint exist and is of designated ip and port
    val edsResourcesJson = eds.getResourcesResponse().getJsonObject("test1")
    val lbEndpoints = edsResourcesJson.getJsonArray("endpoints").getJsonObject(0).getJsonArray("lb_endpoints")
    assertEquals(1, lbEndpoints.size())
    val socketAddress = lbEndpoints.getJsonObject(0).getJsonObject("endpoint")
        .getJsonObject("address").getJsonObject("socket_address")
    assertEquals("1.2.3.4", socketAddress.getString("address"))
    assertEquals(9999, socketAddress.getInteger("port_value"))
  }

  @Test
  fun `should add multiple endpoints`() {
    val loadAssm = generateClusterLoadAssignmentJson("test1", listOf("1.2.3.4", "5.6.7.8"), listOf(9999, 9999))
    val updateEds = generateEdsUpdateJson(listOf(loadAssm), null, null, null)

    val eds = Eds()
    eds.updateService(updateEds)

    assertEquals(1, eds.getResourcesResponse().size())
    val edsResourcesJson = eds.getResourcesResponse().getJsonObject("test1")
    val lbEndpoints = edsResourcesJson.getJsonArray("endpoints").getJsonObject(0).getJsonArray("lb_endpoints")
    assertEquals(2, lbEndpoints.size())
    val socketAddress = lbEndpoints.getJsonObject(0).getJsonObject("endpoint")
        .getJsonObject("address").getJsonObject("socket_address")
    assertEquals("1.2.3.4", socketAddress.getString("address"))
    assertEquals(9999, socketAddress.getInteger("port_value"))

    val socketAddress2 = lbEndpoints.getJsonObject(1).getJsonObject("endpoint")
        .getJsonObject("address").getJsonObject("socket_address")
    assertEquals("5.6.7.8", socketAddress2.getString("address"))
    assertEquals(9999, socketAddress2.getInteger("port_value"))
  }

  @Test
  fun `should remove entire cluster load assignment`() {
    val loadAssm = generateClusterLoadAssignmentJson("test1", "1.2.3.4", 9999)
    val updateEds = generateEdsUpdateJson(listOf(loadAssm), null, null, null)

    val eds = Eds()
    eds.updateService(updateEds)

    assertEquals(1, eds.getResourcesResponse().size())

    val eds2 = Eds(eds)

    // now remove it
    val updateRemoveAddressEds = generateEdsUpdateJson(null, null, listOf("test1"), null)
    eds2.updateService(updateRemoveAddressEds)

    assertEquals(0, eds2.getResourcesResponse().size())
  }

  @Test
  fun `should only remove one endpoint`() {
    val loadAssm = generateClusterLoadAssignmentJson("test1", listOf("1.2.3.4", "5.6.7.8"), listOf(9999, 9999))
    val updateEds = generateEdsUpdateJson(listOf(loadAssm), null, null, null)

    val eds = Eds()
    eds.updateService(updateEds)

    assertEquals(1, eds.getResourcesResponse().size())
    val edsResourcesJson = eds.getResourcesResponse().getJsonObject("test1")
    val lbEndpoints = edsResourcesJson.getJsonArray("endpoints").getJsonObject(0).getJsonArray("lb_endpoints")
    assertEquals(2, lbEndpoints.size())
    val socketAddress = lbEndpoints.getJsonObject(0).getJsonObject("endpoint")
        .getJsonObject("address").getJsonObject("socket_address")
    assertEquals("1.2.3.4", socketAddress.getString("address"))
    assertEquals(9999, socketAddress.getInteger("port_value"))

    val eds2 = Eds(eds)

    // now remove it
    val updateRemoveAddressEds =
        generateEdsUpdateJson(null, null, null,
                              listOf(generateRemoveEndpointJson("test1", "1.2.3.4", 9999)))
    eds2.updateService(updateRemoveAddressEds)


    assertEquals(1, eds2.getResourcesResponse().size())

    val edsResourcesJson2 = eds2.getResourcesResponse().getJsonObject("test1")
    val lbEndpoints2 = edsResourcesJson2.getJsonArray("endpoints").getJsonObject(0).getJsonArray("lb_endpoints")
    assertEquals(1, lbEndpoints2.size())
    val socketAddress2 = lbEndpoints2.getJsonObject(0).getJsonObject("endpoint")
        .getJsonObject("address").getJsonObject("socket_address")
    assertEquals("5.6.7.8", socketAddress2.getString("address"))
    assertEquals(9999, socketAddress2.getInteger("port_value"))
  }

  @Test
  fun `should remove endpoint but leave cla`() {
    val loadAssm = generateClusterLoadAssignmentJson("test1", listOf("1.2.3.4"), listOf(9999))
    val updateEds = generateEdsUpdateJson(listOf(loadAssm), null, null, null)

    val eds = Eds()
    eds.updateService(updateEds)

    assertEquals(1, eds.getResourcesResponse().size())
    val edsResourcesJson = eds.getResourcesResponse().getJsonObject("test1")
    val lbEndpoints = edsResourcesJson.getJsonArray("endpoints").getJsonObject(0).getJsonArray("lb_endpoints")
    assertEquals(1, lbEndpoints.size())
    val socketAddress = lbEndpoints.getJsonObject(0).getJsonObject("endpoint")
        .getJsonObject("address").getJsonObject("socket_address")
    assertEquals("1.2.3.4", socketAddress.getString("address"))
    assertEquals(9999, socketAddress.getInteger("port_value"))

    val eds2 = Eds(eds)

    // now remove it
    val updateRemoveAddressEds =
        generateEdsUpdateJson(null, null, null,
                                                  listOf(generateRemoveEndpointJson("test1", "1.2.3.4", 9999)))
    eds2.updateService(updateRemoveAddressEds)

    assertEquals(1, eds2.getResourcesResponse().size())

    val edsResourcesJson2 = eds2.getResourcesResponse().getJsonObject("test1")
    // cla should still exist
    assertNotNull(edsResourcesJson2)
    // should contain no endpoints since the only one has been removed.
    assertNull(edsResourcesJson2.getJsonArray("endpoints"))
  }

}