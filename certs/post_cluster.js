var http = require("https");
var fs = require("fs")

var postdata = JSON.stringify({
                                "update_groups": "master",
                                "cds": {
                                  // "remove": ["cluster1"],
                                  "add": [
                                    {
                                      "name": "cluster2",
                                      "type": "EDS",
                                      "connect_timeout": "0.25s",
                                      "http2_protocol_options": {},
                                      "upstream_connection_options": {
                                        "tcp_keepalive": {}
                                      },
                                      "eds_cluster_config": {
                                        "eds_config": {
                                          "api_config_source": {
                                            "api_type": "GRPC",
                                            "grpc_services": [
                                              {
                                                "envoy_grpc": {
                                                  "cluster_name": "xds_cluster"
                                                }
                                              }
                                            ]
                                          }
                                        }
                                      }
                                    }
                                  ]
                                }
                              });

var req = http.request({
                         host: "0.0.0.0",
                         port: 8888,
                         method: "POST",
                         path: "/update",
                         rejectUnauthorized: false,
                         headers: {
                           'Content-Type': "application/json",
                           'Content-Length': postdata.length
                         }
                       }, function(res) {
  res.on('data', function(d) {
    console.log(String(d))
  })
});

req.write(postdata);
req.end();
