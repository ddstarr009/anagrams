package anagram


import grails.test.mixin.integration.Integration
import grails.transaction.*
import static grails.web.http.HttpHeaders.*
import static org.springframework.http.HttpStatus.*
import spock.lang.*
import geb.spock.*
import grails.plugins.rest.client.RestBuilder

@Integration
@Rollback
class AnagramAPISpec extends GebSpec {
    def redisService

    def setup() {
    }

    def cleanup() {
    }

    // NOTE: the order of this test suite matters.  We are verifying Redis operations and app flow with this series of tests
    // if you change certain tests, e.g., the POST below, make sure you adjust the subsequent tests

    void "returns a count of words in the corpus after dictionary loaded on bootstrap"() {
        when:"we make a GET req to the /words/stats endpoint after dictionary has been loaded"
            def resp = restBuilder().get("$baseUrl/api/v1/words/stats")

        then:"The resp is OK and the dictionary was loaded"
            resp.status == 200
            //235,885 is currently the size of the dictionary
            def wordCount = resp.json.wordCount
            def withoutCommas = wordCount.replaceAll(",", "")
            int intCount = Integer.parseInt(withoutCommas)
            intCount >= 235885
            
    }

    void "Test adding words to the data store"() {
		redisService.flushDB() // we want to flush everything before the rest of the suite begins
        when:"we call a POST with JSON data to add words to our data store"
            def resp = restBuilder().post("$baseUrl/api/v1/words") {
                contentType "application/json"
                json {
                    words = ["Ader", "read", "dear", "dare", "listen", "silent"]
                }
            }

        then:"The words are added to the DB and we receive a 201 for created"
            resp.status == 201
    }

    void "Test that returns all anagram groups of size >= x, 1 group returned"() {
        when:"we make a GET req to the /api/v1/anagrams/groups/min/:minSize endpoint passing a min size of 4"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/groups/min/4")

        then:"resp is OK and anagram groups only include the read group since the listen group < min"
            resp.status == 200
            def respMap = resp.json
            respMap.group1 != null
            respMap.group2 == null
            respMap.group1.size == 4
            List anagramList = new ArrayList(respMap.group1.members)
            def list = anagramList.findAll { it == 'Ader' }
            list.size() == 1
    }

    void "Test that returns all anagram groups of size >= x, 2 groups returned "() {
        when:"we make a GET req to the /api/v1/anagrams/groups/min/:minSize endpoint passing a min size of 2"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/groups/min/2")

        then:"resp is OK and anagram groups only include the read group since the listen group < min"
            resp.status == 200
            def respMap = resp.json
            respMap.group1 != null
            respMap.group2 != null
            respMap.group3 == null
    }


    void "Test that returns words with the most anagrams"() {
        when:"we make a GET req to the anagrams/most endpoint"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/most")

        then:"The resp is OK and the returned anagrams are correct and from the read group"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.wordsWithMostAnagrams)
            def list = anagramList.findAll { it == 'Ader' }
            list.size() == 1
            resp.json.countOfAnagramGroup == 4
    }

    void "Test that returns a count of words in the corpus and min/max/median/average word length"() {
        when:"we make a GET req to the /words/stats endpoint"
            def resp = restBuilder().get("$baseUrl/api/v1/words/stats")

        then:"The resp is OK and the returned stats are correct"
            resp.status == 200
            resp.json.wordCount == "6"
            resp.json.averageWordLength == "4.6666666667"
            resp.json.minimumWordLength == 4
            resp.json.maximumWordLength == 6
            resp.json.medianWordLength == 4
    }

    void "Test finding anagrams for the given word with no proper nouns in the response"() {
        when:"we call a GET with a specific token and specify we do not want proper nouns"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/read?proper=false")

        then:"The resp is OK and the size is only 1"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.anagrams)
            def list = anagramList.findAll { it == 'Ader' }
            list.size() == 0
    }

    // adding this to ensure silent exists before deleting it
    void "Test finding anagrams for the given word"() {
        given: "our expected anagram for the word listen"
            List expectedAnagrams = new ArrayList()
            expectedAnagrams.add("silent")
        when:"we call a GET with a specific token and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/listen")

        then:"The json includes the expected anagram defined above"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.anagrams)
            expectedAnagrams.equals(anagramList)
    }

    void "Test deleting a word and its anagrams"() {
        when:"we call a DELETE with a specific token"
            def resp = restBuilder().delete("$baseUrl/api/v1/anagrams/listen")

        then:"The response is a 204 and the anagram family will be deleted"
            resp.status == 204
    }

    void "Test to verify that the listen anagram family has been deleted in previous test"() {
        when:"we call a GET with a specific token and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/silent")

        then:"the returned json will be empty"
            resp.status == 200
            resp.json.isEmpty() == true
    }

    void "returns a count of words in the corpus and min/max/median/average word length after anagram family was deleted"() {
        when:"we make a GET req to the /words/stats endpoint after deleting the listen family"
            def resp = restBuilder().get("$baseUrl/api/v1/words/stats")

        then:"The resp is OK and the wordCount is now 4"
            resp.status == 200
            def wordCount = resp.json.wordCount
            def withoutCommas = wordCount.replaceAll(",", "")
            int intCount = Integer.parseInt(withoutCommas)
            intCount == 4
    }

    void "Test finding anagrams for the given word"() {
        given: "our sorted anagrams for the word read"
            List expectedAnagrams = new ArrayList()
            expectedAnagrams.add("Ader")
            expectedAnagrams.add("dare")
            expectedAnagrams.add("dear")
        when:"we call a GET with a specific token and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/read")

        then:"The json includes the 2 expected anagrams defined above"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.anagrams)
            Collections.sort(anagramList)
            expectedAnagrams.equals(anagramList)
    }

    void "Test finding anagrams for the given word with a limit passed in"() {
        when:"we call a GET with a specific token and limit and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/read?limit=1")

        then:"The resp is OK and the size is only 1"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.anagrams)
            anagramList.size() == 1
    }

    void "Test deleting a single word from data store"() {
        when:"we call a DELETE with a specific token"
            def resp = restBuilder().delete("$baseUrl/api/v1/words/dare")

        then:"The response is a 204"
            resp.status == 204
    }

    void "Test finding anagrams for the given word after deleting 'dare' in previous test"() {
        given: "our sorted anagrams for the word read"
            List expectedAnagrams = new ArrayList()
            expectedAnagrams.add("Ader")
            expectedAnagrams.add("dear")
        when:"we call a GET with a specific token and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/read")

        then:"the returned json will only have 'dear' in the results"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.anagrams)
            Collections.sort(anagramList)
            expectedAnagrams.equals(anagramList)
    }

    void "Test deleting all words from data store"() {
        when:"we call a DELETE for all words"
            def resp = restBuilder().delete("$baseUrl/api/v1/words")

        then:"the status will be a 204"
            resp.status == 204
    }

    void "Test finding anagrams for a given word after deleting all words in the data store"() {
        when:"we call a GET with a specific token and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/read")

        then:"the returned json will be empty"
            resp.status == 200
            resp.json.isEmpty() == true
    }

    // *********************************************************************
    // let's POST again since we have no words in the data store right now
    void "Test adding other words to the data store"() {
        when:"we call a POST with JSON data to add words to our data store"
            def resp = restBuilder().post("$baseUrl/api/v1/words") {
                contentType "application/json"
                json {
                    words = ["fruit", "listen", "silent", "noarg", "GRoan", "groan", "organ"]
                }
            }

        then:"The words are added to the DB and we receive a 201 for created"
            resp.status == 201
    }

    void "another Test that returns words with the most anagrams"() {
        when:"we make a GET req to the anagrams/most endpoint"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/most")

        then:"The resp is OK and the returned anagrams are correct and from the organ group"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.wordsWithMostAnagrams)
            def list = anagramList.findAll { it == 'organ' }
            list.size() == 1
            resp.json.countOfAnagramGroup == 4
    }

    void "Test deleting a word and its anagrams"() {
        when:"we call a DELETE with a specific token"
            def resp = restBuilder().delete("$baseUrl/api/v1/anagrams/organ")

        then:"The response is a 204 and the anagram family will be deleted"
            resp.status == 204
    }

    void "another Test that returns words with the most anagrams after deleting organ group"() {
        when:"we make a GET req to the anagrams/most endpoint"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/most")

        then:"The resp is OK and the returned anagrams are correct and from the organ group"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.wordsWithMostAnagrams)
            def list = anagramList.findAll { it == 'silent' }
            list.size() == 1
            resp.json.countOfAnagramGroup == 2
    }

    void "Test GET that takes a set of words and returns whether or not they are all anagrams of each other"() {
        when:"we call a GET with a csv of words as a query param"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/checker?words=read,dare,dear")

        then:"the returned value should be true for read,dare,dear"
            resp.status == 200
            resp.text == "true"
    }

    void "Test GET that takes a set of words and returns whether or not they are all anagrams of each other, should be false"() {
        when:"we call a GET with a csv of words as a query param"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/checker?words=read,dare,dear,fruit")

        then:"the returned value should be false for read,dare,dear,fruit"
            resp.status == 200
            resp.text == "false"
    }




    RestBuilder restBuilder() {
        new RestBuilder()
    }
}
