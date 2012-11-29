(ns com.guokr.nlp.tag
  (:require [com.guokr.nlp.seg :as seg]))

(def model (.getFile (clojure.java.io/resource "chinese-distsim.tagger")))

(def tagger (edu.stanford.nlp.tagger.maxent.MaxentTagger. model))

(defn tag [text] (.tagString tagger (seg/seg text)))
