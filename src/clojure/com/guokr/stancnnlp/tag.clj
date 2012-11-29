(ns com.guokr.stancnnlp.tag
  (:require [clojure.stanfordnlp.seg :as seg]))

(def tagger (edu.stanford.nlp.tagger.maxent.MaxentTagger. "chinese-distsim.tagger"))

(defn tag [text] (.tagString tagger (seg/seg text)))
