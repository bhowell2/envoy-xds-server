# Envoy xDS Management Server
This is a semi-opinionated xDS management server for Envoy to dynamically add and remove resources. Currently this is
built on top of the [java-control-plane](https://github.com/envoyproxy/java-control-plane) provided by Envoy.

TODO:
1. Clean up tests and API.
2. (Possibly) separate `XdsController` and anything below that as independent of vert.x and just provide `getState` and
`updateState` methods on the controller - everything there and below will use language classes and protobuf stuff. Allowing
the Xds Server / API to be embedded within the user's choice server.
2. Create web GUI for API.

## Top-level Update API
Currently all API work is done by `POST`ing JSON to the http server at `/update`. The posted JSON should be in the form:
```javascript
{
  // specify cluster groups for which these resources should be added/removed
  // can use "*" for all groups
  "update_groups": [],
  // remove groups is used to completely remove a node-group and all its resources
  "remove_groups": [],
  "cds": {
    ...
  },
  "eds": {
    ...
  },
  "lds": {
    ...
  },
  "rds": {
    ...
  },
  "sds": {
    ...
  }
}
```
It is not required to post empty resources. Each resource may have a custom body of its own - see below for info on each
resource's possible parameters for adding and removing resources.

It should be assumed if a resource already exists and it is added again, that it will be merged with the existing
resource. E.g., **cluster1** already exists and a post request "adds" **cluster1** again the fields will be merged,
with the latter addition overriding any conflicting fields of the former. If it is desired to ONLY set the resource's
fields to the values submitted then it should be removed first (if it exists) for good measure.
Each section below will describe which fields are merged for each resource.

**Note, remove operations are processed first. So you can remove and add the "same" resource in a single `POST` request.**

### State Response
Generally the state response (retrieved from a `GET` request to `/state`) will look like:
```javascript
{
  "cluster_group_name1": {
    "cds": [{}, {}, ..., {}]
    // all same form as above
    "eds": [...],
    "lds": [...],
    "rds": [...],
    "sds": [...]
  },
  "cluster_group_name2": {
    "cds": [...]
    ...
    "sds": [...]
  },
  ...
}
```

## CDS API
Provides capacity to add, remove, and modify clusters. If the same cluster (name) already exists, the results are
merged if it is not removed first.

### Add
Accepts envoy v2 cluster config. https://www.envoyproxy.io/docs/envoy/v1.10.0/api-v2/api/v2/cds.proto#envoy-api-field-cluster-type

```javascript
{
  "cds": {
    "add": [
      {
        "name": "cluster1",
        "connect_timeout": "0.25s",
        "lb_policy": "ROUND_ROBIN",
        // just example, not required to use EDS
        "type": "EDS",
        "eds_cluster_config": {
          "eds_config": {
            "api_config_source": {
              "api_type": "GRPC",
              "grpc_services": {
                "envoy_grpc": {
                  "cluster_name": "xds_cluster"   // i.e., this management server
                }
              }
            }
          }
        },
        "http2_protocol_options": {}    // can send empty JSON (this line would inform envoy to connect with cluster1 using HTTP2)
      },
      {
        "name": "cluster2",
        ...
      }
    ]
  }
}
```

### Remove
Just submit a list of the name of each cluster that should be removed.
```javascript
{
  "cds": {
    "remove": ["cluster1", ..., "clusterN"]
  }
}
```

## EDS
Allows for submitting endpoints to add and remove endpoints from `load_assignment` for a given cluster.

already exists (for a cluster name), it will be merged with the request's supplied endpoints (for that cluster name).
The endpoint will be referenced by its cluster name, ip address, and port which are separated by ':'.
E.g., `cluster_name:ip_address:port`.

### Add
Accepts Envoy v2 [ClusterLoadAssignment](https://www.envoyproxy.io/docs/envoy/v1.10.0/api-v2/api/v2/eds.proto#envoy-api-msg-clusterloadassignment)
config. This behaves a bit differently than simply submitting a ClusterLoadAssignment, because it merges all endpoints
supplied for a given `cluster_name`. This allows for dynamically adding more endpoints without re-submitting every
endpoint for an entire ClusterLoadAssignment. If the same endpoint is submitted multiple times, the last one will
override all previous submissions (this is the same for named_endpoints). There is no merging of the endpoint itself.
The policy object is merged with existing

```javascript
{
  "eds": {
    "add": [
      // this is the load_assignment for the cluster that matches the cluster_name supplied
      {
        "cluster_name": "cluster1",
        // list of LocalityLbEndpoints
        // https://www.envoyproxy.io/docs/envoy/v1.10.0/api-v2/api/v2/endpoint/endpoint.proto#envoy-api-msg-endpoint-localitylbendpoints
        "endpoints": [
          {
            "locality": {
              "zone": "service_zone",
              "region": "region_zone_belongs_to",
              "sub_zone": "whatever"
            },
            "lb_endpoints": [
              "endpoint": {
                "address": {
                  "socket_address": {
                    "address": "1.1.1.1",
                    "port_value": 443
                  }
                },
                "health_check_config": {
                  "port_value": 80
                }
              },
              "health_status": "HEALTHY"
            ],
            "load_balancing_weight": 1,
            "priority": 0
          },
        ],
        "named_endpoints": {
          // this is supposed to be a map to endpoints, where the key is the endpoint_name
          // and the value is an endpoint object
          "endpoint1": {
            ...
          }
        },
        // policy for the load assignment
        "policy": {
          ...
        }
      }
    ]
  }
}

// note, the above is equivalent to:

```

### Remove
To remove a single endpoint, the endpoint's reference must be supplied (i.e., `cluster_name:ip_address:port`).  If it is
desired to remove ALL endpoints for a cluster, just the cluster name should be supplied, followed by '!!' - requiring
that the request be followed by '!!' so that it is clear the user wants to delete all endpoints for the cluster and does
not accidentally supply only the cluster name. If all endpoints are removed from a `LocalityLBEndpoint` then that locality
endpoint will be removed.

```javascript
// remove endpoint with ip 1.1.1.1 and port 443 from cluster1
// delete all endpoints of cluster2 (does not remove the cluster, just the endpoints)
{
  "eds": {
    "remove": ["cluster1:1.1.1.1:443", "cluster2!"]
  }
}
```

## LDS
### Add

### Remove

## RDS
### Add

### Remove

## SDS
### Add

### Remove

## Testing
Some certificates are provided for testing.
