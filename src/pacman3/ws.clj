(ns pacman3.ws
  (:require
    [org.httpkit.server :as http]
    [cheshire.core :as json]
    [pacman3.game.state :as gs]))

(defonce clients (atom #{}))
(defonce channel->player (atom {}))

(defn- send! [ch msg]
  (http/send! ch (json/generate-string msg)))

(defn- public-state [state]
  (-> state
      (update :status name)
      (update :winner #(when % (name %)))
      (update :players
              (fn [m]
                (into {}
                      (map (fn [[id p]]
                             [id (-> p
                                     (update :role name)
                                     (update :dir #(when % (name %))))]))
                      m)))))

(defn broadcast-state! [state]
  (let [payload (public-state state)
        msg {:type "state" :payload payload}]
    (doseq [ch @clients]
      (send! ch msg))))

(defn- on-message! [ch raw]
  (try
    (let [msg (json/parse-string raw true)]
      (case (:type msg)
        "join"
        (let [nickname (get-in msg [:payload :nickname] "Anon")
              {:keys [player state]} (gs/join! {:nickname nickname})]
          (swap! channel->player assoc ch (:id player))
          (send! ch {:type "joined"
                     :payload {:you (-> player (update :role name) (update :dir #(when % (name %))))
                               :state (public-state state)}})
          (broadcast-state! state))

        "dir"
        (let [pid (get @channel->player ch)
              dir-str (get-in msg [:payload :dir])
              dir-kw (some-> dir-str keyword)]
          (when pid
            (let [state (gs/change-dir! pid dir-kw)]
              (broadcast-state! state))))

        "restart"
        (let [state (gs/restart!)]
          (broadcast-state! state))

        "ping"
        (send! ch {:type "pong" :payload {:t (System/currentTimeMillis)}})

        (send! ch {:type "error" :payload {:msg "Unknown message type"}})))
    (catch Exception e
      (send! ch {:type "error"
                 :payload {:msg (str "Bad JSON: " (.getMessage e))}}))))

(defn handler [req]
  (http/with-channel req ch
    (swap! clients conj ch)
    (send! ch {:type "state" :payload (public-state (gs/snapshot))})

    (http/on-receive ch (fn [data] (on-message! ch data)))

    (http/on-close ch
                   (fn [_]
                     (swap! clients disj ch)
                     (when-let [pid (get @channel->player ch)]
                       (swap! channel->player dissoc ch)
                       (let [new-state (gs/leave! pid)]
                         (broadcast-state! new-state)))))))
