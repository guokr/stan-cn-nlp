(ns com.guokr.stancnnlp.ner
  (:require [com.guokr.stancnnlp.seg :as seg]))

(def props (doto (java.util.Properties.)
               (.setProperty "inputEncoding" "utf-8")
               (.setProperty "outputEncoding" "utf-8")))
(def classifier (edu.stanford.nlp.ie.crf.CRFClassifier/getClassifier "chinese.misc.distsim.crf.ser.gz" props))

(defn ner [text] (.classifyToString classifier (seg/seg text)))
