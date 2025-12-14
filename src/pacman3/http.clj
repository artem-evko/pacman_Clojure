(ns pacman3.http
    (:require
      [compojure.core :refer [defroutes GET]]
      [compojure.route :as route]
      [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
      [clojure.java.io :as io]
      [pacman3.ws :as ws]))

(defroutes routes
           (GET "/" []
                {:status 200
                 :headers {"Content-Type" "text/html; charset=utf-8"}
                 :body (slurp (io/resource "public/index.html"))})

           ;; WebSocket endpoint
           (GET "/ws" req (ws/handler req))

           ;; Статические файлы из resources/public
           (route/resources "/")

           (route/not-found "Not found"))

(def app
  (wrap-defaults routes site-defaults))
