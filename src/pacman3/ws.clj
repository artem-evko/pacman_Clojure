(ns pacman3.ws
  (:require
    [org.httpkit.server :as http]
    [cheshire.core :as json]))

;; Пока: список подключений (позже можно перейти на STM где нужно)
(defonce clients (atom #{}))

(defn- send! [ch msg]
  (http/send! ch (json/generate-string msg)))

(defn- broadcast! [msg]
  (doseq [ch @clients]
    (send! ch msg)))

(defn- on-message! [ch raw]
  (try
    (let [msg (json/parse-string raw true)]
      (case (:type msg)
        "join"
        (let [nickname (get-in msg [:payload :nickname] "Anon")]
          (send! ch {:type "joined"
                     :payload {:nickname nickname
                               :msg (str "Привет, " nickname "! WebSocket работает.")}})
          (broadcast! {:type "system"
                       :payload {:msg (str nickname " подключился(ась).")}}))

        "ping"
        (send! ch {:type "pong"
                   :payload {:t (System/currentTimeMillis)}})

        (send! ch {:type "error"
                   :payload {:msg "Unknown message type"}})))
    (catch Exception e
      (send! ch {:type "error"
                 :payload {:msg (str "Bad JSON: " (.getMessage e))}}))))

(defn handler [req]
  (http/with-channel req ch
    (swap! clients conj ch)

    (send! ch {:type "system" :payload {:msg "Соединение установлено"}})

    (http/on-receive ch (fn [data] (on-message! ch data)))

    (http/on-close ch (fn [_]
                        (swap! clients disj ch))))))
