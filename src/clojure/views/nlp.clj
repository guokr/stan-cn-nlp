(ns clojure.views.nlp
  (:require [clojure.stanfordnlp.seg :as seg])
  (:require [clojure.stanfordnlp.tag :as tag])
  (:require [clojure.stanfordnlp.ner :as ner])
  (:require [noir.response :as response])
  (:use [noir.core :only [defpage]]))

(defpage "/seg/:text" {text :text}
         (response/json {:seg (seg/seg text)}))

(defpage "/tag/:text" {text :text}
         (response/json {:seg (tag/tag text)}))

(defpage "/ner/:text" {text :text}
         (response/json {:seg (ner/ner text)}))

