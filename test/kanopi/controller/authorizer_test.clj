(ns kanopi.controller.authorizer-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [kanopi.test-util :as test-util]
            [kanopi.model.storage.datomic :as datomic]
            [kanopi.controller.authenticator :as authenticator]
            [kanopi.controller.authorizer :refer :all]))

(defn- register-and-get-creds!
  [{:keys [authenticator] :as sys} username password]
  (authenticator/register! authenticator username password)
  (authenticator/credentials authenticator username))

(deftest only-see-my-data
  (let [{:keys [authorizer] :as sys}
        (-> (test-util/system-excl-web)
            (component/start))
        
        creds1 (register-and-get-creds! sys "brian" "rubinton")
        teamname1 "i am on the team!"
        teamname2 "whoami???"
        teamname3 "the trees!"
        _ (authorizer/create-team! authorizer creds1 teamname1)
        _ (authorizer/create-team! authorizer creds1 teamname2)
        _ (authorizer/create-team! authorizer creds1 teamname3)
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
                 (add-to-team! (:authorizer sys)
                               creds1 (get creds1 :team) (get creds2 :username))))
    (component/stop sys)))

(deftest can-create-team
  (let [sys      (-> (test-util/system-excl-web)
                     (component/start))
        creds    (register-and-get-creds! sys "brian" "rubinton")
        teamname "bananafart"
        res      (create-team! (:authorizer sys) creds teamname)
        creds'   (authenticator/credentials (:authenticator sys) "brian")
        ]
    (is (contains? (get creds' :teams) teamname))
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
