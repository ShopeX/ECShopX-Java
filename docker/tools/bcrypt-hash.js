#!/usr/bin/env node
// Generate a BCrypt hash compatible with Spring Security BCrypt.checkpw.
// Usage: INSTALL_PLAIN_PASSWORD='secret' node bcrypt-hash.js
//    or: node bcrypt-hash.js 'secret'
'use strict';

var bcrypt = require('./bcrypt.min.js');
var plain = process.env.INSTALL_PLAIN_PASSWORD;
if (plain == null || plain === '') {
  plain = process.argv[2];
}
if (plain == null || plain === '') {
  process.stderr.write('ERROR: missing password (env INSTALL_PLAIN_PASSWORD or argv[1])\n');
  process.exit(1);
}
process.stdout.write(bcrypt.hashSync(plain, 10));
