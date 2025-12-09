(ns pacman3.core
  (:gen-class)
  (:require
    [org.httpkit.server :as http]
    [pacman3.http :as http-app]))

;; Здесь будем хранить ссылку на запущенный сервер,
;; чтобы можно было его остановить из REPL при необходимости.
(defonce server (atom nil))

(defn start! []
  (when-not @server
    (println "Starting Pacman server on http://localhost:3000")
    (reset! server
            (http/run-server http-app/app {:port 3000}))))

(defn stop! []
  (when-let [s @server]
    (s) ;; http-kit возвращает функцию-остановщик
    (reset! server nil)
    (println "Server stopped")))

(defn -main
  "Точка входа при запуске `clj -M -m pacman3.core`"
  [& _]
  (start!))
