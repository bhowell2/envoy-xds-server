package io.bhowell2.envoyxdsserver.services

import io.bhowell2.envoyxdsserver.utils.CdsTestUtils.Companion.generateClusterLoadAssignmentJson
import io.bhowell2.envoyxdsserver.utils.CdsTestUtils.Companion.generateUpdateCdsJson
import io.vertx.core.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Unit tests for CdsData. Ensures that clusters are added and removed as expected.
 * @author Blake Howell
 */
class CdsTests {

  @Test
  fun `should return default value fields`() {
    val addCluster1 = JsonObject ().put("name", "test1")
        // use something other than STRICT here, because STRICT won't be returned because it's a default value,
        // so cannot simply use json.equals(respJson)
        .put("type", "STATIC")
        .put("drain_connections_on_host_removal", true)
        .put("load_assignment", generateClusterLoadAssignmentJson("whatever", "1.1.1.1", 9999))

    val updateCds = generateUpdateCdsJson(listOf(addCluster1), null)

    val cds = Cds()
    cds.updateService(updateCds)

    assertEquals(1, cds.getResourcesResponse().size())

    // check
    val addedClusterTest1 = cds.getResourcesResponse(true).getJsonObject("test1")
    assertEquals("test1", addedClusterTest1.getString("name"))
    assertEquals("STATIC", addCluster1.getString("type"))
  }

  @Test
  fun `should add cluster`() {
    val addCluster1 = JsonObject ().put("name", "test1")
        // use something other than STRICT here, because STRICT won't be returned because it's a default value,
        // so cannot simply use json.equals(respJson)
        .put("type", "STRICT_DNS")
        .put("drain_connections_on_host_removal", true)
        .put("load_assignment", generateClusterLoadAssignmentJson("whatever", "1.1.1.1", 9999))

    val updateCds = generateUpdateCdsJson(listOf(addCluster1), null)

    val cds = Cds()
    cds.updateService(updateCds)

    assertEquals(1, cds.getResourcesResponse().size())

    // check
    val addedClusterTest1 = cds.getResourcesResponse().getJsonObject("test1")
    assertEquals(addCluster1, addedClusterTest1)
    val loadAssmEndpoints = addedClusterTest1.getJsonObject("load_assignment").getJsonArray("endpoints")
    assertEquals(1, loadAssmEndpoints.size())
    val lbEndpoints = loadAssmEndpoints.getJsonObject(0).getJsonArray("lb_endpoints")
    assertEquals(1, lbEndpoints.size())

    // add another cluster
    val addCluster2 = JsonObject().put("name", "test2")
        .put("type", "LOGICAL_DNS")
        .put("drain_connections_on_host_removal", true)
        .put("load_assignment", generateClusterLoadAssignmentJson("whatever", "1.2.3.4", 9999))

    val updateCds2 = generateUpdateCdsJson(listOf(addCluster2), null)

    val cds2 = Cds(cds)
    cds2.updateService(updateCds2)

    assertEquals(2, cds2.getResourcesResponse().size())

    // check
    val addedClusterTest2 = cds2.getResourcesResponse().getJsonObject("test2")
    assertEquals(addCluster2, addedClusterTest2)
    val loadAssmEndpoints2 = addedClusterTest2.getJsonObject("load_assignment").getJsonArray("endpoints")
    assertEquals(1, loadAssmEndpoints2.size())
    val lbEndpoints2 = loadAssmEndpoints2.getJsonObject(0).getJsonArray("lb_endpoints")
    assertEquals(1, lbEndpoints2.size())
  }

  @Test
  fun `should merge cluster`() {
    val addCluster1 = JsonObject ().put("name", "test1")
        // use something other than STRICT here, because STRICT won't be returned because it's a default value,
        // so cannot simply use json.equals(respJson)
        .put("type", "STRICT_DNS")
        .put("drain_connections_on_host_removal", true)
        .put("load_assignment", generateClusterLoadAssignmentJson("whatever", "1.1.1.1", 9999))

    val updateCds = generateUpdateCdsJson(listOf(addCluster1), null)

    val cds = Cds()
    cds.updateService(updateCds)

    assertEquals(1, cds.getResourcesResponse().size())

    // check
    val addedClusterTest1 = cds.getResourcesResponse().getJsonObject("test1")
    assertEquals(addCluster1, addedClusterTest1)
    assertEquals("STRICT_DNS", addedClusterTest1.getString("type"))
    val loadAssmEndpoints = addedClusterTest1.getJsonObject("load_assignment").getJsonArray("endpoints")
    assertEquals(1, loadAssmEndpoints.size())
    val lbEndpoints = loadAssmEndpoints.getJsonObject(0).getJsonArray("lb_endpoints")
    assertEquals(1, lbEndpoints.size())


    // add cluster with the same name, expecting merge to happen. this means the load assignment should have two
    val addCluster2 = JsonObject().put("name", "test1")
        .put("type", "LOGICAL_DNS")
        .put("drain_connections_on_host_removal", true)
        .put("load_assignment", generateClusterLoadAssignmentJson("whatever", "1.2.3.4", 9999))

    val updateCds2 = generateUpdateCdsJson(listOf(addCluster2), null)

    val cds2 = Cds(cds)
    cds2.updateService(updateCds2)

    // should be size of 1 here, because clusters were merged
    assertEquals(1, cds2.getResourcesResponse().size())

    // check
    val addedClusterTest2 = cds2.getResourcesResponse().getJsonObject("test1")
    // cannot test for equality here as it has been merged and contains extra fields
    assertNotEquals(addCluster2, addedClusterTest2)
    // should have changed.
    assertEquals("LOGICAL_DNS", addedClusterTest2.getString("type"))
    // make sure that endpoints now has size 2
    val loadAssmEndpoints2 = addedClusterTest2.getJsonObject("load_assignment").getJsonArray("endpoints")
    assertEquals(2, loadAssmEndpoints2.size())
    // when adding a second load assignment, the endpoints are merged, but this creates 2 lb_endpoints
    // if it is desired to only have one LocalityLbEndpoint then will have to remove the cluster and re-add it with all
    // lb_endpoints in the LocalityLbEndpoint
  }


  @Test
  fun `should remove cluster`() {
    val addCluster1 = JsonObject ().put("name", "test1")
        // use something other than STRICT here, because STRICT won't be returned because it's a default value,
        // so cannot simply use json.equals(respJson)
        .put("type", "STRICT_DNS")
        .put("drain_connections_on_host_removal", true)
        .put("load_assignment", generateClusterLoadAssignmentJson("whatever", "1.1.1.1", 9999))

    val updateCds = generateUpdateCdsJson(listOf(addCluster1), null)

    val cds = Cds()
    cds.updateService(updateCds)
    assertEquals(1, cds.getResourcesResponse().size())

    val updateCdsWithRemove = generateUpdateCdsJson(null, listOf("test1"))

    val cds2 = Cds(cds)
    cds2.updateService(updateCdsWithRemove)

    assertEquals(0, cds2.getResourcesResponse().size())
  }


  @Test
  fun `should add and remove in same request`() {
    val addCluster1 = JsonObject ().put("name", "test1")
        // use something other than STRICT here, because STRICT won't be returned because it's a default value,
        // so cannot simply use json.equals(respJson)
        .put("type", "STRICT_DNS")
        .put("drain_connections_on_host_removal", true)
        .put("load_assignment", generateClusterLoadAssignmentJson("whatever", "1.1.1.1", 9999))

    val updateCds = generateUpdateCdsJson(listOf(addCluster1), null)

    val cds = Cds()
    cds.updateService(updateCds)

    assertEquals(1, cds.getResourcesResponse().size())

    // check
    val addedClusterTest1 = cds.getResourcesResponse().getJsonObject("test1")
    assertEquals(addCluster1, addedClusterTest1)
    val loadAssmEndpoints = addedClusterTest1.getJsonObject("load_assignment").getJsonArray("endpoints")
    assertEquals(1, loadAssmEndpoints.size())
    val lbEndpoints = loadAssmEndpoints.getJsonObject(0).getJsonArray("lb_endpoints")
    assertEquals(1, lbEndpoints.size())

    // add another cluster
    val addCluster2 = JsonObject().put("name", "test2")
        .put("type", "LOGICAL_DNS")
        .put("drain_connections_on_host_removal", true)
        .put("load_assignment", generateClusterLoadAssignmentJson("whatever", "1.2.3.4", 9999))

    val updateCds2 = generateUpdateCdsJson(listOf(addCluster2), listOf("test1"))

    val cds2 = Cds(cds)
    cds2.updateService(updateCds2)

    assertEquals(1, cds2.getResourcesResponse().size())

    // check
    val addedClusterTest2 = cds2.getResourcesResponse().getJsonObject("test2")
    assertEquals(addCluster2, addedClusterTest2)
    val loadAssmEndpoints2 = addedClusterTest2.getJsonObject("load_assignment").getJsonArray("endpoints")
    assertEquals(1, loadAssmEndpoints2.size())
    val lbEndpoints2 = loadAssmEndpoints2.getJsonObject(0).getJsonArray("lb_endpoints")
    assertEquals(1, lbEndpoints2.size())
  }

  @Test
  fun `should remove and add same cluster`() {
    val addCluster1 = JsonObject ().put("name", "test1")
        // use something other than STRICT here, because STRICT won't be returned because it's a default value,
        // so cannot simply use json.equals(respJson)
        .put("type", "STRICT_DNS")
        .put("drain_connections_on_host_removal", true)
        .put("load_assignment", generateClusterLoadAssignmentJson("whatever", "1.1.1.1", 9999))

    val updateCds = generateUpdateCdsJson(listOf(addCluster1), null)

    val cds = Cds()
    cds.updateService(updateCds)

    assertEquals(1, cds.getResourcesResponse().size())

    // check
    val addedClusterTest1 = cds.getResourcesResponse().getJsonObject("test1")
    assertEquals(addCluster1, addedClusterTest1)
    val loadAssmEndpoints = addedClusterTest1.getJsonObject("load_assignment").getJsonArray("endpoints")
    assertEquals(1, loadAssmEndpoints.size())
    val lbEndpoints = loadAssmEndpoints.getJsonObject(0).getJsonArray("lb_endpoints")
    assertEquals(1, lbEndpoints.size())

    val addCluster2 = addCluster1.copy()
    // same as above, but changed type and load assignment
    addCluster2.put("type", "LOGICAL_DNS")
        .put("load_assignment", generateClusterLoadAssignmentJson("whatever", listOf("1.1.1.1", "2.2.2.2"), listOf(9999, 9999)))

    val updateCds2 = generateUpdateCdsJson(listOf(addCluster2), listOf("test1"))

    val cds2 = Cds(cds)
    cds2.updateService(updateCds2)

    val addedClusterTest2 = cds2.getResourcesResponse().getJsonObject("test1")
    assertEquals(addCluster2, addedClusterTest2)
    val loadAssmEndpoints2 = addedClusterTest2.getJsonObject("load_assignment").getJsonArray("endpoints")
    assertEquals(1, loadAssmEndpoints2.size())
    val lbEndpoints2 = loadAssmEndpoints2.getJsonObject(0).getJsonArray("lb_endpoints")
    assertEquals(2, lbEndpoints2.size())

  }

}