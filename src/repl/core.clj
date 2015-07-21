;; TODO: Check this file manually
(ns bukkurerepl.core 
  (:require [bukkure.bukkit :as bk]
            [bukkure.blocks :as blocks]
            [bukkure.events :as ev]
            [bukkure.entity :as ent]
            [bukkure.player :as plr]
            [bukkure.util :as util]
            [bukkure.logging :as log]
            [bukkure.config :as cfg]
            [bukkure.commands :as cmd]
            [bukkure.recipes :as r]
            [bukkure.items :as i]
            [bukkure.files]
            [clojure.tools.nrepl.server :refer (start-server stop-server)]))

(def repl-handle (atom nil))

(defn start-repl [host port]
  (log/info "Starting repl on host: %s, port %s" host port)
  (cond
   @repl-handle
   {:msg "you tried to start a(nother) repl while one was already started"}
   (util/port-in-use? port host)
   {:msg (format "REPL already started or port %s:%s is in use" host port)}
   :else
   (do
     (reset! repl-handle (start-server :host host :port port))
     {:msg (format "Started repl on host: %s, port %s" host port)})))

(defn stop-repl
  []
  (cond
   (nil? @repl-handle) {:msg "you tried to stop REPL when it was not running"}
   :else
   (try
     (do
       (stop-server @repl-handle)
       {:msg "REPL stopped"})
     (finally
      (reset! repl-handle nil)))))

(defn start-repl-if-needed [plugin]
  (let [repl-host (cfg/get-string plugin "repl.host")
        repl-port (cfg/get-int plugin "repl.port")]
    (cond
     (not (cfg/get-boolean plugin "repl.enabled"))
     (log/info "REPL Disabled")
     :else
     (let [{:keys [msg] :as response} (start-repl repl-host repl-port)]
       (log/info "Repl options: %s %s" repl-host repl-port)
       (if msg (log/info msg))))))

(defonce clj-plugin (atom nil))

(defn repl-command [sender cmd & [port]]
  (log/info "Running repl command with %s %s" cmd port)
  (case cmd
    :start (start-repl "0.0.0.0" (or port (cfg/get-int @clj-plugin "repl.port")))
    :stop (stop-repl)))

;; bukkure basic permission system
(defn permission-command [sender player permission allow-type]
  (plr/set-permission player permission allow-type))

(defn player-permission-attach [ev]
  (plr/permission-attach-player! @clj-plugin ev))

(defn player-permission-detach [ev]
  (plr/permission-detach-player! ev))

(defn setup-permission-system
  [plugin]
  (ev/register-eventlist
   plugin
   [(ev/event "player.player-join" #'player-permission-attach)
    (ev/event "player.player-quit" #'player-permission-detach)
    (ev/event "player.player-kick" #'player-permission-detach)])
  (plr/permission-attach-all! plugin))

(defn disable-permission-system
  []
  (plr/permission-detach-all!))

;; bukkure specific setup
(defn start
  "onEnable bukkure"
  [plugin]
  (reset! clj-plugin plugin)
  (cmd/register-command @clj-plugin "clj.repl" #'repl-command [:keyword [:start :stop]] [:int [(cfg/get-int plugin "repl.port")]])
  (cmd/register-command @clj-plugin "clj.tabtest" #'tabtest-command :player :material [:keyword [:start :stop]] [:string #'tabcomplete-reverse-first])
  (cmd/register-command @clj-plugin "clj.addevent" #'addevent-command :event :string)
  (cmd/register-command @clj-plugin "clj.spawnentity" #'spawn-command :entity)
  (cmd/register-command @clj-plugin "clj.permission" #'permission-command :player :permission [:keyword [:allow :disallow :release]])
  (setup-permission-system plugin)
  (start-repl-if-needed plugin))

(defn stop
  "onDisable bukkure"
  [plugin]
  (stop-repl)
  (disable-permission-system))

(defn on-enable
  "to enable self or any child plugins"
  [plugin]
  (cfg/config-defaults plugin)
  (start)
  (log/info "Clojure Repl options: %s %s %s" (cfg/get-string plugin "repl.host") (cfg/get-int plugin "repl.port") (cfg/get-boolean plugin "repl.enabled"))
  )

(defn on-disable
  "to disable self or any child plugins"
  [plugin]
  (stop)
  (log/info "Clojure stopped - %s" plugin)
  )
