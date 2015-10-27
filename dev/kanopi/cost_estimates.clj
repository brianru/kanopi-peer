(ns kanopi.cost-estimates)

(def service-library
  {
   :circleci
   {:id :circleci
    :args [:circleci/num-instances]
    :price-fn (fn [n] (* (dec n) 50))
    }

   :github/personal
   {:id :github/personal
    :args [:github.personal/num-repositories]
    }

   :github/organization
   {:id :github/organization
    :args [:github.organization/num-repositories]
    }

   :clerky
   {:id :clerky}

   :aws/ec2
   {:id :aws/ec2}

   :datomic
   {:id :datomic}
   })

(defn select-services
  ([service-library]
   (vals service-library))
  ([service-library & services]
   (->> services
        (select-keys service-library)
        (vals))))

(def minimum-usage-estimates
  {:circleci/num-instances 1
   })

(defn estimate-costs
  ([services ests]
   (reduce
    (fn [subtotal service]
      (->> (get service :args [])
           (map #(or (get ests %) (get service %) 0))
           (apply (get service :price-fn (constantly 0)))
           (+ subtotal)))
    0
    services)))

(comment
 (estimate-costs (select-services service-library) minimum-usage-estimates)
 )


