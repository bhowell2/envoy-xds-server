var http = require("http")
var webSocket = require('ws');

process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";

var counter = {};

// var runTimes = 50
// var i = 10
// for (var i = 0; i <= runTimes; i++) {
//   var cap = function(i) {
//     var req = http.request({
//                              host: "0.0.0.0",
//                              port: 8080,
//                              method: "GET",
//                              rejectUnauthorized: false,
//                              path: "/"
//                            }, function (res) {
//       res.on('data', function (d) {
//         // var data = d.toString()
//         // var index = d.toString().match("ID:").index;
//         // if (counter[data.substr(index + 4)] === undefined) {
//         //   counter[data.substr(index + 4)] = 0
//         // }
//         // counter[data.substr(index + 4)] = counter[data.substr(index + 4)] + 1
//         console.log(d)
//       })
//       res.on('error', function(e) {
//         console.log(e)
//       })
//     });
//     req.end()
//   };
//   cap(i);
// }

// setTimeout(function() {
//   console.log(counter)
// }, 1500)

const ws = new webSocket('ws://0.0.0.0:8080/', {
  rejectUnauthorized: false
});

ws.onopen = (open) => {
  console.log(open)
}

ws.onmessage = (msg) => {
  console.log(msg)
};

ws.onerror = (err) => {
  console.log(err)
}

ws.onclose = (close) => {
  console.log(close)
}

setTimeout(() => {
  console.log("Timeout ended.")
}, 20000);
