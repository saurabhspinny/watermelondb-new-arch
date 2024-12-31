"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");
exports.__esModule = true;
exports.default = void 0;
// var _reactNative = require("react-native");
var NativeWatermelonDB = require("../../../src/NativeWatermelonDB").default; // Adjust the path
var _randomId_v = _interopRequireDefault(require("./randomId_v2.native"));
var _fallback = _interopRequireDefault(require("./fallback"));
/* eslint-disable no-bitwise */
var alphabet = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
var randomNumbers = [];
var cur = 9999999;

// TODO: This is 3-5x slower than Math.random()-based implementation
// Should be migrated to JSI, or simply implemented fully in native
// (bridging is the bottleneck)
function nativeRandomId_v1() {
  var id = '';
  var len = 0;
  var v = 0;
  while (16 > len) {
    if (256 > cur) {
      v = randomNumbers[cur] >> 2;
      cur++;
      if (62 > v) {
        id += alphabet[v];
        len++;
      }
    } else {
      randomNumbers = NativeWatermelonDB.getRandomBytes(256);
      cur = 0;
    }
  }
  return id;
}
// Updated nativeRandomId function
var nativeRandomId = (() => {
  if (NativeWatermelonDB?.getRandomIds) {
    return _randomId_v.default; // Use randomId_v2.native if getRandomIds is available
  } else if (NativeWatermelonDB?.getRandomBytes) {
    return nativeRandomId_v1; // Use nativeRandomId_v1 if getRandomBytes is available
  }
  return _fallback.default; // Use fallback if neither method is available
})();
var _default = exports.default = nativeRandomId;