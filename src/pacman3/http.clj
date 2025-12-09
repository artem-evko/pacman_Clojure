(ns pacman3.http
    (:require
      [compojure.core :refer [defroutes GET]]
      [compojure.route :as route]
      [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
      [clojure.java.io :as io]))

;; Маршруты HTTP
(defroutes routes
           ;; Главная страница игры
           (GET "/" []
                {:status 200
                 :headers {"Content-Type" "text/html; charset=utf-8"}
                 :body (slurp (io/resource "public/index.html"))})

           ;; Статика из resources/public (картинки, css, js)
           (route/resources "/")

           ;; 404
           (route/not-found "Not found"))

;; Обёртка middleware (cookies, заголовки и т.д.)
(def app
  (wrap-defaults routes site-defaults))