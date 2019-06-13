package io.bhowell2.envoyxdsserver.services

import io.bhowell2.envoyxdsserver.utils.LdsTestUtils.Companion.generateAddListenerJson
import io.bhowell2.envoyxdsserver.utils.LdsTestUtils.Companion.generateUpdateLdsJson
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Blake Howell
 */

class LdsTests {

  @Test
  fun `should add listener`() {
    val updateLds = generateUpdateLdsJson(listOf(generateAddListenerJson("yo", "1.1.1.1", 9999)), null)
    val lds = Lds()
    assertEquals(0, lds.getResourcesResponse().size())
    lds.updateService(updateLds)
    assertEquals(1, lds.getResourcesResponse().size())
    val updateLds2 = generateUpdateLdsJson(listOf(generateAddListenerJson("new", "1.1.1.1", 9999)), null)
    val lds2 = Lds(lds)
    assertEquals(1, lds2.getResourcesResponse().size())
    lds2.updateService(updateLds2)
    assertEquals(2, lds2.getResourcesResponse().size())
  }

  @Test
  fun `should remove listener`() {
    val updateLds = generateUpdateLdsJson(listOf(generateAddListenerJson("yo", "1.1.1.1", 9999)), null)
    val lds = Lds()
    assertEquals(0, lds.getResourcesResponse().size())
    lds.updateService(updateLds)
    assertEquals(1, lds.getResourcesResponse().size())
    val updateLdsWithRemove = generateUpdateLdsJson(null, listOf("yo"))
    val lds2 = Lds(lds)
    assertEquals(1, lds2.getResourcesResponse().size())
    lds2.updateService(updateLdsWithRemove)
    assertEquals(0, lds2.getResourcesResponse().size())
  }

}