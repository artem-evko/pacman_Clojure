(ns pacman3.core
    (:gen-class)
    (:require
      [org.httpkit.server :as http]
      [pacman3.http :as http-app]
      [pacman3.game.loop :as loop]))

(defonce server (atom nil))

(defn start! []
      (when-not @server
                (println "Starting Pacman server on http://localhost:3001")
                (reset! server (http/run-server http-app/app {:port 3001}))
                (loop/start!)))

(defn stop! []
      (loop/stop!)
      (when-let [s @server]
                (s)
                (reset! server nil)
                (println "Server stopped")))

(defn -main [& _]
      (start!))
