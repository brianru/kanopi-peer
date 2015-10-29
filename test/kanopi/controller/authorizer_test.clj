(ns kanopi.controller.authorizer-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [kanopi.test-util :as test-util]
            [kanopi.model.schema :as schema]
            [kanopi.model.storage.datomic :as datomic]
            [kanopi.controller.authenticator :as authenticator]
            [kanopi.controller.authorizer :refer :all]))

(defn- register-and-get-creds!
  [{:keys [authenticator] :as sys} username password]
  (authenticator/register! authenticator username password)
  (authenticator/credentials authenticator username))

(deftest hack-the-database
  (let [sys   (-> (test-util/system-excl-web)
                  (component/start))
        creds  (register-and-get-creds! sys "brian" "rubinton")
        creds2 (register-and-get-creds! sys "hannah" "rubinton")

        plain-db    (d/db (get-in sys [:datomic-peer :connection]))
        filtered-db (datomic/filtered-db* plain-db creds)
        ]
    ;; TODO: test authorized entities are available in both plain and
    ;; filtered db
    (testing "some data is available"
      (is (d/q '[:find ?e .
                 :in $
                 :where [?e :datum/label ?v]]
               filtered-db))
      (is (d/q '[:find ?e .
                 :in $ ?team
                 :where [?e :datum/team ?team]]
               filtered-db (schema/current-team creds)))
      (is (d/attribute filtered-db :datum/label)))

    (testing "other users' data is not available"
      (is (d/q '[:find ?e .
                 :in $ ?team
                 :where [?e _ ?team]]
               plain-db (schema/current-team creds2)))

      (is (not (d/q '[:find ?e .
                      :in $ ?team
                      :where [?e _ ?team]]
                    filtered-db (schema/current-team creds2)))))



    (component/stop sys)))

(deftest only-see-my-data
  (let [{:keys [authorizer] :as sys}
        (-> (test-util/system-excl-web)
            (component/start))
        
        creds1 (register-and-get-creds! sys "brian" "rubinton")
        teamname1 "i am on the team!"
        teamname2 "whoami???"
        teamname3 "the trees!"
        _ (create-team! authorizer creds1 teamname1)
        _ (create-team! authorizer creds1 teamname2)
        _ (create-team! authorizer creds1 teamname3)
        creds2 (register-and-get-creds! sys "hannah" "rubinton")
        ]

    (component/stop sys)))

(deftest cannot-add-user-to-personal-team
  (let [sys    (-> (test-util/system-excl-web)
                   (component/start))
        creds1 (register-and-get-creds! sys "brian"  "rubinton")
        creds2 (register-and-get-creds! sys "hannah" "rubinton")
        personal-team (get creds1 :team)]
    (is (thrown? java.lang.AssertionError
                 (add-to-team! (:authorizer sys) creds1 "brian" (get creds2 :username))))
    (component/stop sys)))

(deftest can-create-team
  (let [sys      (-> (test-util/system-excl-web)
                     (component/start))
        creds    (register-and-get-creds! sys "brian" "rubinton")
        teamname "bananafart"
        res      (create-team! (:authorizer sys) creds teamname)
        creds'   (authenticator/credentials (:authenticator sys) "brian")
        ]
    (is (contains? (->> (get creds' :teams)
                        (map :team/id)
                        (set))
                   teamname))
    (component/stop sys)))

(deftest cannot-add-self-to-team
  (let [sys      (-> (test-util/system-excl-web)
                     (component/start))
        creds1   (register-and-get-creds! sys "brian"  "rubinton")
        creds2   (register-and-get-creds! sys "hannah" "rubinton")
        teamname "foofoofoo"
        team3    (create-team! (:authorizer sys) creds1 teamname)
        ]
    (is (thrown? java.lang.AssertionError
                 (add-to-team! (:authorizer sys) creds2 teamname (get creds2 :username))))

    (component/stop sys)))

(deftest cannot-leave-personal-team
  (let [sys      (-> (test-util/system-excl-web)
                     (component/start))
        creds1   (register-and-get-creds! sys "brian" "rubinton")
        ]
    (is (thrown? java.lang.AssertionError
                 (leave-team! (:authorizer sys) creds1 "brian")))
    (component/stop sys)))

(deftest can-only-leave-current-teams
  (let [sys      (-> (test-util/system-excl-web)
                     (component/start))
        creds1   (register-and-get-creds! sys "brian"  "rubinton")
        creds2   (register-and-get-creds! sys "hannah" "rubinton")
        teamname "foofoofoo"
        team3    (create-team! (:authorizer sys) creds1 teamname)
        ]
    (is (thrown? java.lang.AssertionError
                 (leave-team! (:authorizer sys) creds2 teamname)))
    (component/stop sys)))

(deftest teams-are-uniquely-identified
  (let [{:keys [authorizer] :as sys}
        (-> (test-util/system-excl-web)
            (component/start))
        creds1    (register-and-get-creds! sys "brian"  "rubinton")
        creds2    (register-and-get-creds! sys "hannah" "rubinton")
        teamname3 "foofoofoo"
        teamname4 "applebottom"
        team3     (create-team! (:authorizer sys) creds1 teamname3)
        team4     (create-team! (:authorizer sys) creds2 teamname4)
        ]
    (testing "create duplicate team, creds already member of team with that name"
      (is (thrown? java.lang.AssertionError
                   (create-team! authorizer creds1 teamname3))))
    (testing "create dupe team, creds not member of team with that name"
      (is (thrown? java.lang.AssertionError
                   (create-team! authorizer creds1 teamname4))))
    (testing "create dupe team, with name of another user"
      (is (thrown? java.lang.AssertionError
                   (create-team! authorizer creds1 (get creds2 :username)))))
    (component/stop sys)))
