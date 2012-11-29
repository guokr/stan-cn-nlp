(ns com.guokr.nlp.ner
  (:require [com.guokr.nlp.seg :as seg]))

(def model (.getFile (clojure.java.io/resource "chinese.misc.distsim.crf.ser.gz")))

(def props (doto (java.util.Properties.)
               (.setProperty "inputEncoding" "utf-8")
               (.setProperty "outputEncoding" "utf-8")))
(def classifier (edu.stanford.nlp.ie.crf.CRFClassifier/getClassifier model props))

(defn ner [text] (.classifyToString classifier (seg/seg text)))
