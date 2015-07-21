;; TODO: Check this file manually
(ns repl.core 
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

(defn on-enable
  "onEnable REPL"
  [plugin]
  (cfg/config-defaults plugin)
  (reset! clj-plugin plugin)
  (cmd/register-command @clj-plugin "clj.repl" #'repl-command [:keyword [:start :stop]] [:int [(cfg/get-int plugin "repl.port")]])
  (start-repl-if-needed plugin)
  (log/info "Clojure Repl options: %s %s %s" (cfg/get-string plugin "repl.host") (cfg/get-int plugin "repl.port") (cfg/get-boolean plugin "repl.enabled")))

(defn on-disable
  "onDisable REPL"
  [plugin]
  (stop-repl)
  (log/info "Clojure Repl stopped - %s" plugin))
