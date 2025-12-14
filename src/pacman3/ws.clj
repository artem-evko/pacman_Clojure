(ns pacman3.ws
  (:require
    [org.httpkit.server :as http]
    [cheshire.core :as json]))

;; Пока оставляем атом для списка сокетов (STM подключим на следующем этапе)
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

        ;; default
        (send! ch {:type "error"
                   :payload {:msg "Unknown message type"}})))
    (catch Exception e
      (send! ch {:type "error"
                 :payload {:msg (str "Bad JSON: " (.getMessage e))}}))))

(defn handler [req]
  (http/with-channel req ch
    ;; register
    (swap! clients conj ch)

    ;; hello
    (send! ch {:type "system" :payload {:msg "Соединение установлено"}})

    ;; receive
    (http/on-receive ch (fn [data] (on-message! ch data)))

    ;; close
    (http/on-close ch (fn [_status]
                        (swap! clients disj ch)))))
