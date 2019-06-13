var http = require("https");
var fs = require("fs")

var postdata = JSON.stringify({
                                "update_groups": "master",
                                "rds": {
                                  "remove_route_configs": ["https_rds"],
                                  "add": [
                                    {
                                      "name": "https_rds",
                                      "virtual_hosts": [
                                        {
                                          "name": "first",
                                          "domains": ["*"],
                                          "routes": [
                                            {
                                              "match": {
                                                "prefix": "/",
                                                // "runtime_fraction": {
                                                //   "default_value": {
                                                //     "numerator": 50,
                                                //     "denominator": "HUNDRED"
                                                //   }
                                                // }
                                              },
                                              "route": {
                                                "cluster": "cluster1",
                                                "upgrade_configs": [
                                                  {
                                                    "upgrade_type": "websocket",
                                                    "enabled": true
                                                  }
                                                ]
                                              },
                                              "metadata": {
                                                "filter_metadata": {
                                                  "name": {
                                                    "name": "testserver1"
                                                  }
                                                }
                                              }
                                            },
                                            {
                                              "match": {
                                                "prefix": "/",
                                                // "runtime_fraction": {
                                                //   "default_value": {
                                                //     "numerator": 100,
                                                //     "denominator": "HUNDRED"
                                                //   }
                                                // }
                                              },
                                              "route": {
                                                "cluster": "cluster2",
                                                "upgrade_configs": [
                                                  {
                                                    "upgrade_type": "websocket",
                                                    "enabled": true
                                                  }
                                                ]
                                              },
                                              "metadata": {
                                                "filter_metadata": {
                                                  "name": {
                                                    "name": "testserver1"
                                                  }
                                                }
                                              }
                                            }
                                          ]
                                        }
                                      ]
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
