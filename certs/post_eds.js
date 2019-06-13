var http = require("https");
var fs = require("fs")


var postdata = JSON.stringify({
                                "update_groups": "master",
                                "eds": {
                                  "add_clas": [
                                    {
                                      "cluster_name": "cluster2",
                                      "endpoints": [
                                        {
                                          "lb_endpoints": [
                                            {
                                              "endpoint": {
                                                "address": {
                                                  "socket_address": {
                                                    "address": "172.17.0.3",
                                                    "port_value": 8080
                                                  }
                                                }
                                              }
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                  ]
                                  // "remove_endpoints": [
                                  //   {
                                  //     "cluster_name": "cluster1",
                                  //     "address": "172.17.0.3",
                                  //     "port_value": 8080
                                  //   }
                                  // ]
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
