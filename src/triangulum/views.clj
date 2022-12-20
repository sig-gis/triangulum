(ns triangulum.views
  (:import java.io.ByteArrayOutputStream)
  (:require [clojure.data.json   :as json]
            [clojure.edn         :as edn]
            [clojure.java.io     :as io]
            [clojure.string      :as str]
            [clj-http.client     :as client]
            [cognitect.transit   :as transit]
            [hiccup.page         :refer [html5 include-css include-js]]
            [triangulum.config   :refer [get-config]]
            [triangulum.git      :refer [current-version]]
            [triangulum.errors   :refer [nil-on-error]]
            [triangulum.utils    :refer [resolve-foreign-symbol kebab->snake kebab->camel]]))

(defn find-cljs-app-js
  "Pull "
  []
  (as-> (slurp "target/public/cljs/manifest.edn") app
    (edn/read-string app)
    (get app "target/public/cljs/app.js")
    (str/split app #"/")
    (last app)
    (str "/cljs/" app)))

(defn find-manifest
  "Returns the manifest.json"
  []
  (if (= "dev" (get-config :server :mode))
    (loop [resp    (nil-on-error
                    (client/get "http://localhost:5173/manifest.json"
                                {:accept :json}))
           counter 0]
      (cond
        resp
        (-> resp
            :body
            (json/read-str))

        (> counter 50)
        (throw (ex-info "can't find manifest.json" {}))

        :else
        (do (Thread/sleep 200)
            (recur resp (inc counter)))))
    (-> (slurp "dist/public/manifest.json")
        (json/read-str))))

(defn cljs-project?
  "Check if current project is a ClojureScript project"
  []
  (get-config :app :cljs-init))

(defn find-bundle-asset-files
  "Returns a map of JS and CSS asset paths for React/Vite JS projects. Return nil
  for CLJS projects."
  [page]
  (when-not (cljs-project?)
    (let [src-file           (format "src/js/%s.jsx" page)
          manifest           (find-manifest)
          js-entrypoint-file (str "/" (get-in manifest [src-file "file"]))
          js-asset-files     (mapv #(str/replace % #"^_" "/assets/")
                                   (get-in manifest [src-file "imports"]))
          css-asset-files    (reduce-kv (fn [acc k v]
                                          (if (str/includes? k ".css")
                                            (conj acc (str "/" (get v "file")))
                                            acc))
                                        []
                                        manifest)]
      {:js-files  (conj js-asset-files js-entrypoint-file)
       :css-files css-asset-files})))

(defn head
  "Produces the head tag of the index page"
  [{:keys [bundle-js-files bundle-css-files lang]}]
  (let [{:keys [title description keywords extra-head-tags gtag-id static-css-files static-js-files]} (get-config :app)]
    [:head
     [:title (get title lang "")]
     [:meta {:charset "utf-8"}]
     [:meta {:name "description" :content (get description lang "")}]
     [:meta {:name "keywords" :content (get keywords lang "")}]
     (seq extra-head-tags)
     (when gtag-id
       (list [:script {:async true :src (str "https://www.googletagmanager.com/gtag/js?id=" gtag-id)}]
             [:script (str "window.dataLayer = window.dataLayer || [];"
                           "function gtag() { dataLayer.push(arguments); }"
                           "gtag('js', new Date());"
                           "gtag('config', '" gtag-id "', {'page_location': location.host + location.pathname});")]))
     (apply include-css
            (concat static-css-files
                    bundle-css-files))
     (apply include-js static-js-files)
     (cond
       ;; CLJS app
       (cljs-project?)
       (include-js (find-cljs-app-js))

       ;; JS app/prod
       (= "prod" (get-config :server :mode))
       (map (fn [f] [:script {:type "module" :src f}])
            (butlast bundle-js-files))

       ;; JS app/dev
       :else
       [:script {:type "module"
                       :src "http://localhost:5173/index.html?html-proxy&index=0.js"}])]))

(defn uri->page
  "Returns the JavaScript file home page"
  [uri]
  (->> (str/split uri #"/")
       (remove str/blank?)
       (first)
       ((fnil kebab->camel "home"))))

(defn client-init
  "Returns the script tag necessary to for the browser to load the app"
  [entry-file params]
  (let [js-params (-> params
                      (assoc :versionDeployed (current-version))
                      (merge (get-config :app :client-keys))
                      (json/write-str))]
    (if-let [cljs-init (get-config :app :cljs-init)]
      ;; CLJS app
      [:script {:type "text/javascript"}
       (format "window.onload = function () { %s(%s); };" (-> cljs-init name kebab->snake) js-params)]
      ;; JS app
      [:script {:type "module" :src "http://localhost:5173/src/js/index.jsx"} ]
      #_[:script {:type "module"}
       (format "// import { pageInit } from \"%s\"; window.onload = function () { pageInit(%s); };"
               (if (= "prod" (get-config :server :mode))
                 entry-file
                 (str "http://localhost:5173" "/src/js/index.jsx"))
               js-params)])))

(defn- announcement-banner []
  (let [announcement (-> (slurp "announcement.txt")
                         (str/split #"\n"))]            ; TODO This will be moved to the front end for better UX.
    (when-not (empty? (first announcement))
      [:div#banner {:style {:background-color "#f96841"
                            :box-shadow       "3px 1px 4px 0 rgb(0, 0, 0, 0.25)"
                            :color            "#ffffff"
                            :display          (if (pos? (count announcement)) "block" "none")
                            :margin           "0px"
                            :padding          "5px"
                            :position         "fixed"
                            :text-align       "center"
                            :top              "0"
                            :right            "0"
                            :left             "0"
                            :width            "100vw"
                            :z-index          "10000"}}
       [:script {:type "text/javascript"}
        "setTimeout (function () {document.getElementById ('banner') .style.display='none'}, 10000);"]
       (map (fn [line]
              [:p {:style {:font-size   "18px"
                           :font-weight "bold"
                           :margin      "0 30px 0 0"}
                   :key   line}
               line])
            announcement)
       [:button {:style   {:align-items      "center"
                           :background-color "transparent"
                           :border-color     "#ffffff"
                           :border-radius    "50%"
                           :border-style     "solid"
                           :border-width     "2px"
                           :cursor           "pointer"
                           :display          "flex"
                           :height           "25px"
                           :padding          "0"
                           :position         "fixed"
                           :right            "10px"
                           :top              "5px"
                           :width            "25px"}
                 :onClick "document.getElementById('banner').style.display='none'"}
        [:svg {:width "24px" :height "24px" :viewBox "0 0 48 48" :fill "#ffffff"}
         [:path {:d "M38 12.83l-2.83-2.83-11.17 11.17-11.17-11.17-2.83 2.83 11.17 11.17-11.17 11.17 2.83 2.83
                     11.17-11.17 11.17 11.17 2.83-2.83-11.17-11.17z"}]]]])))

(defn get-response-params
  "Prepares the necessary dynamic assets and values needed to render the page"
  [uri request]
  (let [page          (uri->page uri)
        asset-files   (find-bundle-asset-files page)
        get-user-lang (some-> (get-config :app :get-user-lang) resolve-foreign-symbol)
        lang          (if get-user-lang
                        (get-user-lang request)
                        :en)]
    {:bundle-js-files  (:js-files asset-files)
     :bundle-css-files (:css-files asset-files)
     :lang             lang}))

(defn render-page
  "Returns the page's html"
  [uri]
  (fn [request]
    (let [response-params (when (= "prod" (get-config :server :mode))
                            (get-response-params uri request))]
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (html5
                 (head response-params)
                 [:body
                  [:section
                   ;; TODO These will be moved to the front end for better UX.
                   (when-let [flash-message (get-in request [:params :flash_message])]
                     ;; FIXME: We need to write class definition in css file and serve it by default
                     [:p {:class "alert"} flash-message])
                   (when (.exists (io/as-file "announcement.txt"))
                     (announcement-banner))
                   [:div#app]]
                  (client-init (-> response-params :bundle-js-files last)
                               (:params request))])})))

#_(defn render-page
  [uri]
  (fn [request]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (html5
            [:head
             #_[:script {:type "module"}
                    "import { injectIntoGlobalHook } from \"/@react-refresh\";
                     injectIntoGlobalHook(window);
                     window.$RefreshReg$ = () => {};
                     window.$RefreshSig$ = () => (type) => type;"]
             [:script {:type "module"
                       :src "http://localhost:5173/index.html?html-proxy&index=0.js"}]
             #_[:script {:type "module" :src "http://localhost:5173/@vite/client"}]
             #_[:meta {:charset "UTF-8"}] [:link {:rel "icon" :type "image/svg+xml" :href "/vite.svg"}]
             #_[:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
             #_[:title "Vite + React + TS + Emotion"]]
            [:body
             [:div#app]
             (client-init nil (:params request))])}))

(defn not-found-page
  "Produces a not found response"
  [request]
  (-> request
      ((render-page "/page-not-found"))
      (assoc :status 404)))

(defn body->transit
  "Produces a transit response body"
  [body]
  (let [out    (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer body)
    (.toString out)))
