## REPL
You will need a build of repl, currently the way to do this is to clone this repo

`git clone https://github.com/SevereOverfl0w/bukkurerepl.git`
and then build with [leiningen](http://leiningen.org/)
`lein uberjar`
The created standalone jar from the `target` directory should then be placed in your Spigot `plugins` directory.

Once installed connecting to it is simple, just `lein repl :connect 4005`.
The REPL can be configured from the `plugins/repl/config.yml` file.

Here's example usage lifted from cljminecraft.
```clojure
user=> (in-ns 'repl.core)
#<Namespace repl.core>

repl.core=> (ev/find-event "break")
("painting.painting-break-by-entity" "hanging.hanging-break" "painting.painting-break" "entity.entity-break-door" "hanging.hanging-break-by-entity" "player.player-item-break" "block.block-break")

;; block.block-break looks good.. lets see what we can get out of it
repl.core=> (ev/describe-event "block.block-break")
#{"setExpToDrop" "isCancelled" "getEventName" "setCancelled" "getExpToDrop" "getPlayer" "getBlock"}

;; Cool, getBlock looks like I can use it..
repl.core=> (defn mybreakfn [ev] {:msg (format "You broke a %s" (.getBlock ev))})
#'repl.core/mybreakfn

repl.core=> (ev/register-event @clj-plugin "block.block-break" #'mybreakfn)
nil

;; Test breaking a block, I get a crazy message, let's make that more sane
repl.core=> (defn mybreakfn [ev] {:msg (format "You broke a %s" (.getType (.getBlock ev)))})
#'repl.core/mybreakfn
```

