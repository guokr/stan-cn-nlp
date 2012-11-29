(ns views.nlp
  (:require [com.guokr.stancnnlp.seg :as seg])
  (:require [com.guokr.stancnnlp.tag :as tag])
  (:require [com.guokr.stancnnlp.ner :as ner])
  (:require [noir.response :as response])
  (:use [noir.core :only [defpage]]))

(defpage "/seg/:text" {text :text}
         (response/json {:seg (seg/seg text)}))

(defpage "/tag/:text" {text :text}
         (response/json {:seg (tag/tag text)}))

(defpage "/ner/:text" {text :text}
         (response/json {:seg (ner/ner text)}))

