package io.bhowell2.envoyxdsserver.services

import com.google.common.collect.ImmutableMap
import com.google.protobuf.Message
import io.bhowell2.envoyxdsserver.api.DefaultUpdateServiceApi
import io.bhowell2.envoyxdsserver.exceptions.BadRequestException
import io.bhowell2.envoyxdsserver.jsonToMessageBuilder
import io.bhowell2.envoyxdsserver.messageToJsonObject
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

/**
 * All services should extend this. Provides some base functionality.
 */
abstract class ServiceBase<T: Message, B: Message.Builder>(val serviceName: String) {

  companion object {

    private fun checkIsJsonObjectOrThrowBadRequest(item: Any, badRequestMsg: String) {
      if (item !is JsonObject) {
        throw BadRequestException(badRequestMsg)
      }
    }

    /**
     * Checks whether or not the JsonElement is a string and throws a BadRequestException if it is not.
     */
    private fun checkIsStringThrowBadRequest(item: Any, badRequestMsg: String) {
      if (item !is String) {
        throw BadRequestException(badRequestMsg)
      }
    }

  }

  private var resources: ImmutableMap<String, T> = ImmutableMap.of()

  // use to track whether or not update has been called. once update has been called on this object it should
  // never be called again. a copy should be made.
  private var alreadyUpdated: Boolean = false

  /**
   * Copies the ResourceData object so that it can be modified without disrupting the original object.
   * This facilitates transactional operations for the ResourceData. When a rollback occurs the copied object is
   * disregarded.
   */
  constructor(copy: ServiceBase<T,B>): this(copy.serviceName) {
    this.resources = ImmutableMap.copyOf(copy.resources)
  }

  /**
   * Should return the particular services identifying name. This will most likely be what
   * [io.envoyproxy.controlplane.cache.Resources.getResourceName] returns for the given resource.
   * This is used to store and retrieve the resource from [resources].
   */
  protected abstract fun getResourceKeyFromBuilder(resourceBuilder: B): String

  /**
   * Returns the builder for this resource type.
   * (e.g., resource of Cluster type will return [io.envoyproxy.envoy.api.v2.Cluster.Builder]]
   */
  protected abstract fun getResourceBuilder(): B

  /**
   * Used to update the service's resources.
   */
  @Throws(Exception::class)
  @Synchronized
  fun updateService(updateServiceObj: JsonObject) {
    if (alreadyUpdated) {
      // LOG
      error("Should have never called updateService twice. A copy should have been made.")
    }
    val mutableResources = mutableMapOf<String, T>()
    mutableResources.putAll(this.resources)
    removeResources(updateServiceObj, mutableResources)
    addResources(updateServiceObj, mutableResources)
    this.resources = ImmutableMap.copyOf(mutableResources)
    // should not be updated again. if it is updated again a copy should be made. makes transactions easier.
    alreadyUpdated = true
  }

  /**
   * Default add resources implementation. Retrieves the "add" key as a JsonArray from the updateResourceObj and
   * creates (or merges if the resource key returned from [getResourceKeyFromBuilder] already exists) each position
   * in the array with builder returned from [getResourceBuilder].
   *
   * This should be overridden when more custom functionality is required for adding a resource.
   *
   * @param resourcesMap is mutable and thus changes are visible to the map wherever it is passed
   */
  protected open fun addResources(updateResourceObj: JsonObject, resourcesMap: MutableMap<String, T>) {
    addResourcesWithFullMerge(updateResourceObj, resourcesMap)
  }

  /**
   * Each entry in the
   */
  @Suppress("UNCHECKED_CAST")
  protected fun addResourcesWithFullMerge(updateResourceObj: JsonObject, resourcesMap: MutableMap<String, T>) {
    val addResources = updateResourceObj.getJsonArray(DefaultUpdateServiceApi.ADD)
    if (addResources == null || addResources.size() == 0) {
      return
    }
    for (jsonResource in addResources) {
      checkIsJsonObjectOrThrowBadRequest(jsonResource,
                                         "Add resource object (for $serviceName) is not a JSON object.")
      val resourceBuilder = getResourceBuilder()
      jsonToMessageBuilder(jsonResource as JsonObject, resourceBuilder as Message.Builder)
      val currentResourceForName = resourcesMap[getResourceKeyFromBuilder(resourceBuilder)]
      if (currentResourceForName != null) {
        val mergedResource = currentResourceForName.toBuilder().mergeFrom(resourceBuilder.buildPartial()).build()
        resourcesMap[getResourceKeyFromBuilder(resourceBuilder)] = mergedResource as T
      } else {
        resourcesMap[getResourceKeyFromBuilder(resourceBuilder)] = resourceBuilder.build() as T
      }
    }

  }

  /**
   * Default remove resource implementation. Retrieves a (param name = [DefaultUpdateServiceApi.REMOVE]) JsonArray
   * from [updateResourceObj]. This is useful when the key directly matches the resource name that needs to be removed,
   * This should be overridden by extending classes where necessary - i.e., when the key provided needs to be
   * processed to remove a resource. See the API in the README for the required key format for each resource type.
   *
   * @param resourcesMap is mutable and thus changes are visible to the map wherever it is passed
   */
  @Throws(Exception::class)
  @Synchronized
  protected open fun removeResources(updateResourceObj: JsonObject, resourcesMap: MutableMap<String, T>) {
    val removeKeys = updateResourceObj.getJsonArray(DefaultUpdateServiceApi.REMOVE)
    if (removeKeys == null || removeKeys.size() == 0) {
      return  // remove nothing
    }
    for (key in removeKeys) {
      checkIsStringThrowBadRequest(key, "Remove resources item for ($serviceName) should be an array of strings.")
      resourcesMap.remove(key)
    }
  }

  /**
   * Generates a response for each resource (GRPC message). This should be overridden if certain fields
   * should not be returned (e.g., for SDS should not return 'inline_string' or 'inline_bytes' for the certificate).
   */
  open fun generateResourceResponse(resource: T, includeDefaultValues: Boolean = false): JsonObject {
    return messageToJsonObject(resource, includeDefaultValues)
  }

  /**
   * Turns the service's resources into a JsonObject that can be returned to the user. This
   * JSON of form:
   * {
   *  "resource_name": {
   *    "name": "resource_name",
   *    ...
   *  },
   *  "resource_name2": {
   *    ...
   *  },
   *  ...
   * }
   */
  fun getResourcesResponse(includeDefaultValues: Boolean = false): JsonObject {
    val retJson = JsonObject()
    for (entry in resources) {
      retJson.put(entry.key, generateResourceResponse(entry.value))
    }
    return retJson
  }

  fun getSnapshotResources(): Collection<T> {
    return this.resources.values
  }

  /**
   * Returns a JsonObject to reset the current service's resources. This is simply a JsonObject with the
   * 'add' parameter, which is common to every service and is used to fully add a resource.
   */
  fun getResetServiceState(): JsonObject {
    val addResources = JsonArray()
    for (resource in resources) {
      addResources.add(messageToJsonObject(resource.value))
    }
    return JsonObject().put(DefaultUpdateServiceApi.ADD, addResources)
  }

}
