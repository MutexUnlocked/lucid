// Copyright (C) 2015 the V8 project authors. All rights reserved.
// This code is governed by the BSD license found in the LICENSE file.
/*---
esid: sec-weakmap.prototype.set
description: >
  Throws TypeError if `this` doesn't have a [[WeakMapData]] internal slot.
info: |
  WeakMap.prototype.set ( key, value )

  ...
  3. If M does not have a [[WeakMapData]] internal slot, throw a TypeError
  exception.
  ...
features: [Set]
---*/

assert.throws(TypeError, function() {
  WeakMap.prototype.set.call(new Set(), {}, 1);
});

assert.throws(TypeError, function() {
  var map = new WeakMap();
  map.set.call(new Set(), {}, 1);
});

reportCompare(0, 0);