var http = require("https");
var fs = require("fs")

var postdata = JSON.stringify({
  "update_groups": "master",
  "sds": {
    "add": [
      {
        "name": "justiceop_all_certs",
        "tls_certificate": {
          "certificate_chain": {
            "inline_string": String(fs.readFileSync("xdscert.pem"))
          },
          "private_key": {
            "inline_string": String(fs.readFileSync("xdsprivkey.pem"))
          }
        }
      }
    ]
  }
})

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
