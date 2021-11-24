box.cfg {
  listen = 3301;
  log_level = 6;
}

require("schema").init()

require("api")

require('console').start()
